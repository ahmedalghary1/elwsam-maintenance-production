package com.elwsam.operations.updates

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.elwsam.operations.BuildConfig
import com.elwsam.operations.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateState(val stage: String = "current", val progress: Int = 0, val message: String = "", val version: String = "")

@Singleton
class UpdateCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presence: DevicePresence
) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val reportMutex = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS).callTimeout(15, TimeUnit.MINUTES)
        .followRedirects(false).followSslRedirects(false).build()
    private val _state = MutableStateFlow(UpdateState(
        stage = prefs.getString("stage", "current") ?: "current",
        message = prefs.getString("stage_message", "").orEmpty(),
        version = prefs.getString("stage_version", "").orEmpty()
    ))
    val state = _state.asStateFlow()
    private var foreground = WeakReference<Activity>(null)
    private var progressReport: Job? = null
    @Volatile private var lastCheck = 0L
    @Volatile var confirmationIntent: Intent? = null
        private set
    @Volatile private var confirmationLaunched = false
    private val apkFile get() = File(context.filesDir, "app-update.apk")
    private val partFile get() = File(context.filesDir, "app-update.part")

    init {
        synchronized(prefs) {
            if (!prefs.contains("device_id")) {
                prefs.edit().putString("device_id", UUID.randomUUID().toString())
                    .putString("secret", UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                    .commit()
            }
        }
        scope.launch {
            while (isActive) {
                delay(60_000)
                if (foreground.get() != null) checkNow()
            }
        }
    }

    fun onResume(activity: Activity) {
        presence.onResume(activity)
        foreground = WeakReference(activity)
        check()
        showIfSafe()
    }

    fun onPause(activity: Activity) {
        presence.onPause(activity)
        if (foreground.get() === activity) foreground.clear()
    }

    fun check(force: Boolean = false) { scope.launch { checkNow(force) } }

    fun accountChanged(userId: Int) { presence.onLogin(userId); check(force = true) }
    fun accountLoggedOut() { presence.onLogout() }

    suspend fun checkNow(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext true
        try {
            val now = System.currentTimeMillis()
            if (force) prefs.edit().remove("install_failed_for").remove("defer_until").apply()
            if (!force && now - lastCheck < 60_000) return@withContext true
            lastCheck = now
            if (!register()) {
                setState("current", message = "سجّل الدخول مرة واحدة لربط الجهاز بخدمة التحديثات.")
                return@withContext true
            }
            val previous = prefs.getString("release", null)?.let { JSONObject(it) }
            if (previous != null && installedVersion() >= previous.getLong("version_code")) {
                prefs.edit().remove("release").remove("session_id").apply()
                apkFile.delete()
                setState("current", message = "التطبيق محدّث.")
            }
            heartbeat()
            val latest = request("latest/")
            val release = latest.optJSONObject("release")
            if (release == null) {
                // A withdrawn update must no longer be installed from the cached APK.
                abandonSession()
                prefs.edit().remove("release").apply()
                apkFile.delete()
                setState("current", message = if (latest.optBoolean("updates_enabled", true)) "التطبيق محدّث."
                    else "التحديثات موقوفة لهذا الجهاز بواسطة المسؤول.")
                return@withContext true
            }
            UpdatePolicy.requireValidMetadata(installedVersion(), release.getLong("version_code"), release.getLong("size_bytes"), release.getString("sha256"))
            if (!force && prefs.getLong("install_failed_for", -1) == release.getLong("version_code")) return@withContext true
            val sessionId = prefs.getInt("session_id", -1)
            val sessionAge = now - prefs.getLong("install_started", 0)
            if (sessionId >= 0 && sessionAge > 300_000 && prefs.getString("stage", "") == "installing") {
                abandonSession()
                markInstallFailed("استغرق Android وقتًا أطول من المتوقع. أعد المحاولة؛ تقاريرك محفوظة.")
                heartbeat()
                return@withContext true
            }
            if (!force && sessionId >= 0 && sessionAge in 0..300_000 &&
                prefs.getString("stage", "") == "confirmation" &&
                (confirmationIntent != null || confirmationLaunched) &&
                context.packageManager.packageInstaller.getSessionInfo(sessionId) != null) {
                setState("confirmation", message = "أكمل موافقة Android أو أعد المحاولة.")
                return@withContext true
            }
            if (sessionId >= 0 && sessionAge in 0..300_000 &&
                prefs.getString("stage", "") == "installing" &&
                context.packageManager.packageInstaller.getSessionInfo(sessionId) != null) {
                setState("installing")
                return@withContext true
            }
            if (sessionId >= 0 && (sessionAge > 300_000 || confirmationIntent == null)) abandonSession()
            prefs.edit().putString("release", release.toString()).commit()
            if (release.getInt("min_sdk") > Build.VERSION.SDK_INT) {
                setState("incompatible", message = "هذه النسخة تحتاج إصدار Android أحدث من الموجود على الهاتف.", version = release.getString("version_name"))
                heartbeat()
                return@withContext true
            }
            if (!apkFile.exists() || prefs.getString("verified_hash", "") != release.getString("sha256")) {
                abandonSession()
                download(release)
            } else {
                try {
                    verifyArchive(release)
                } catch (error: Exception) {
                    apkFile.delete()
                    prefs.edit().remove("verified_hash").apply()
                    throw error
                }
            }
            // Recover a pending confirmation after process death by creating a fresh session.
            if (_state.value.stage != "confirmation" || confirmationIntent == null) {
                setState("ready", version = release.getString("version_name"))
            }
            heartbeat()
            showIfSafe()
            true
        } catch (cancelled: CancellationException) {
            if (_state.value.stage == "downloading") {
                lastCheck = 0
                setState("failed", message = "توقف تنزيل التحديث. ستتم إعادة المحاولة عند عودة الإنترنت؛ تقاريرك محفوظة.")
                scope.launch { runCatching { heartbeat() } }
            }
            throw cancelled
        } catch (error: Exception) {
            lastCheck = 0
            if (force || prefs.contains("release") || _state.value.stage in setOf("downloading", "ready", "installing")) {
                setState("failed", message = error.message ?: "تعذر تحديث التطبيق. البيانات محفوظة؛ تحقق من الإنترنت وأعد المحاولة.")
                runCatching { heartbeat() }
            }
            false
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun register(): Boolean {
        return presence.ensureRegistered()
    }

    private fun deviceInfo(): JSONObject {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return JSONObject().put("version_code", PackageInfoCompat.getLongVersionCode(info))
            .put("version_name", info.versionName ?: "")
            .put("android_sdk", Build.VERSION.SDK_INT).put("label", "${Build.MANUFACTURER} ${Build.MODEL}".take(150))
    }

    private fun installedVersion() = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    private fun rawRequest(path: String, body: JSONObject? = null): okhttp3.Response {
        val builder = Request.Builder().url(BuildConfig.API_BASE_URL + "api/v1/updates/" + path)
        builder.header("X-Device-ID", prefs.getString("device_id", "")!!)
            .header("X-Device-Token", prefs.getString("secret", "")!!)
        if (body != null) builder.post(body.toString().toRequestBody("application/json".toMediaType()))
        return client.newCall(builder.build()).execute()
    }

    private fun request(path: String, body: JSONObject? = null): JSONObject = rawRequest(path, body).use {
        check(it.isSuccessful) { "تعذر الاتصال بخدمة التحديثات (${it.code})." }
        JSONObject(it.body?.string() ?: "{}")
    }

    private suspend fun heartbeat() = reportMutex.withLock {
        val value = _state.value
        val release = prefs.getString("release", null)?.let { JSONObject(it) }
        request("heartbeat/", deviceInfo().put("status", value.stage).put("progress", value.progress)
            .put("error", if (value.stage == "failed" || value.stage == "incompatible") value.message.take(1000) else "")
            .put("release_id", release?.optLong("id") ?: JSONObject.NULL))
    }

    private suspend fun download(release: JSONObject) {
        val downloadContext = currentCoroutineContext()
        setState("downloading", version = release.getString("version_name"))
        showIfSafe()
        heartbeat()
        // Only use our own authenticated download endpoint; never follow a server-supplied URL.
        val path = "releases/${release.getLong("id")}/download/"
        try {
            rawRequest(path).use { response ->
                check(response.isSuccessful) { "تعذر تنزيل ملف التحديث (${response.code})." }
                val body = response.body ?: error("ملف التحديث غير متاح.")
                partFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        var lastReported = -1
                        UpdatePolicy.copyVerified(input, output, release.getLong("size_bytes"), release.getString("sha256")) { progress ->
                            downloadContext.ensureActive()
                            _state.value = _state.value.copy(progress = progress)
                            if (progress / 10 != lastReported) {
                                lastReported = progress / 10
                                // Reporting failures cannot corrupt or interrupt a valid download.
                                if (progressReport?.isActive != true) progressReport = scope.launch { runCatching { heartbeat() } }
                            }
                        }
                    }
                }
            }
            apkFile.delete()
            check(partFile.renameTo(apkFile)) { "تعذر حفظ التحديث على الهاتف." }
            verifyArchive(release)
            prefs.edit().putString("verified_hash", release.getString("sha256")).commit()
        } catch (error: Exception) {
            partFile.delete()
            apkFile.delete()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun verifyArchive(release: JSONObject) {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: error("ملف APK غير صالح.")
        check(archive.packageName == context.packageName && PackageInfoCompat.getLongVersionCode(archive) == release.getLong("version_code")) { "نسخة APK لا تطابق التطبيق أو رقم البناء المنشور." }
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        check(signatures(archive).isNotEmpty() && signatures(archive) == signatures(installed)) { "توقيع التحديث مختلف عن النسخة المثبتة. تواصل مع المسؤول." }
        check(apkFile.length() == release.getLong("size_bytes")) { "حجم ملف التحديث غير صحيح." }
        val digest = MessageDigest.getInstance("SHA-256")
        apkFile.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        check(digest.digest().joinToString("") { "%02x".format(it) } == release.getString("sha256")) { "تغيّر ملف التحديث. أعد تنزيل النسخة." }
    }

    fun continueInstall() { scope.launch {
        mutex.withLock {
            if (_state.value.stage !in setOf("ready", "permission", "failed")) return@withLock
            try {
                val release = prefs.getString("release", null)?.let { JSONObject(it) } ?: return@withLock
                if (!apkFile.exists()) { check(force = true); return@withLock }
                if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                    setState("permission", version = release.getString("version_name"))
                    heartbeat()
                    return@withLock
                }
                verifyArchive(release)
                // A release can be withdrawn while it is downloading.
                val current = request("latest/").optJSONObject("release")
                check(current != null && current.getLong("id") == release.getLong("id")) { "تغيّرت النسخة المنشورة. أعد التحقق من التحديثات." }
                abandonSession()
                val installer = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    setSize(apkFile.length())
                    if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                val id = installer.createSession(params)
                prefs.edit().putInt("session_id", id).putLong("install_started", System.currentTimeMillis()).commit()
                installer.openSession(id).use { session ->
                    apkFile.inputStream().use { input -> session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    } }
                    setState("installing", version = release.getString("version_name"))
                    runCatching { heartbeat() }
                    val callback = Intent(context, InstallResultReceiver::class.java).setAction("com.elwsam.operations.UPDATE_RESULT")
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                    session.commit(PendingIntent.getBroadcast(context, id, callback, flags).intentSender)
                }
            } catch (error: Exception) {
                abandonSession()
                markInstallFailed(error.message ?: "تعذر تثبيت التحديث.")
                runCatching { heartbeat() }
            }
        }
    } }

    fun installResult(intent: Intent) {
        val id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (id != prefs.getInt("session_id", -2)) return
        confirmationLaunched = false
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmationIntent = confirm
                setState("confirmation", message = "يرجى إكمال موافقة Android لتثبيت التحديث.")
            }
            PackageInstaller.STATUS_SUCCESS -> { setState("current", message = "تم تثبيت التحديث."); check(force = true) }
            else -> {
                prefs.edit().remove("session_id").apply()
                markInstallFailed("لم يكتمل تثبيت التحديث. يمكنك إعادة المحاولة؛ تقاريرك محفوظة.")
            }
        }
        scope.launch { runCatching { heartbeat() } }
    }

    fun takeConfirmation(): Intent? = confirmationIntent.also {
        if (it != null) confirmationLaunched = true
        confirmationIntent = null
    }

    fun deferCurrent() {
        if (_state.value.stage != "current") prefs.edit().putLong("defer_until", System.currentTimeMillis() + 3_600_000).apply()
    }

    private fun markInstallFailed(message: String) {
        val code = prefs.getString("release", null)?.let { JSONObject(it).optLong("version_code", -1) } ?: -1L
        prefs.edit().putLong("install_failed_for", code).apply()
        setState("failed", message = message)
    }

    private fun abandonSession() {
        confirmationLaunched = false
        confirmationIntent = null
        val id = prefs.getInt("session_id", -1)
        if (id >= 0) runCatching { context.packageManager.packageInstaller.abandonSession(id) }
        prefs.edit().remove("session_id").apply()
    }

    private fun setState(stage: String, message: String = "", version: String = _state.value.version) {
        prefs.edit().putString("stage", stage).putString("stage_message", message).putString("stage_version", version).apply()
        _state.value = UpdateState(stage, if (stage == "ready") 100 else 0, message, version)
    }

    private fun showIfSafe() {
        if (System.currentTimeMillis() < prefs.getLong("defer_until", 0)) return
        val activity = foreground.get() ?: return
        // Never interrupt an open maintenance/production form or kill unsaved input.
        if (activity is MainActivity && _state.value.stage in setOf("ready", "downloading", "confirmation")) {
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed && foreground.get() === activity) {
                    foreground.clear()
                    activity.startActivity(Intent(activity, UpdateActivity::class.java))
                }
            }
        }
    }
}
