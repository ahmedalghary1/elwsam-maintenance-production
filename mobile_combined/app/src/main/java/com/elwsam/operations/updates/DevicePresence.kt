package com.elwsam.operations.updates

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.elwsam.operations.BuildConfig
import com.production.supervisor.data.local.datastore.TokenManager
import com.production.supervisor.data.remote.RequestOwner
import com.production.supervisor.data.remote.TokenAuthenticator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Presence is independent of APK download/install work and continues when updates are paused. */
@Singleton
class DevicePresence @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokens: TokenManager,
    authenticator: TokenAuthenticator
) {
    private val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var foreground = WeakReference<Activity>(null)
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    // The normal JWT refresh behavior, without logging device credentials.
    private val registrationClient = client.newBuilder().authenticator(authenticator).addInterceptor { chain ->
        val (access, userId) = runBlocking { tokens.requestIdentity() }
        val builder = chain.request().newBuilder().tag(RequestOwner::class.java, RequestOwner(userId))
        if (!access.isNullOrBlank()) builder.header("Authorization", "Bearer $access")
        chain.proceed(builder.build())
    }.build()

    init {
        scope.launch {
            while (isActive) {
                delay(30_000)
                if (foreground.get() != null) reportNow()
            }
        }
    }

    fun onResume(activity: Activity) {
        foreground = WeakReference(activity)
        scope.launch { reportNow() }
    }

    fun onPause(activity: Activity) {
        if (foreground.get() === activity) foreground.clear()
        // Avoid flickering "background" while navigating between our modules.
        scope.launch { delay(750); reportNow() }
    }

    fun onLogin(userId: Int) {
        synchronized(prefs) {
            prefs.edit().putInt("presence_user", userId).putString("presence_session", UUID.randomUUID().toString())
                .remove("presence_bound_session").remove("presence_logout_session").apply()
        }
        scope.launch { reportNow() }
    }

    fun onLogout() {
        retireAccount()
        scope.launch { reportNow() }
    }

    private fun retireAccount() = synchronized(prefs) {
        val session = prefs.getString("presence_session", null)
        val edit = prefs.edit().remove("presence_user").remove("presence_session").remove("presence_bound_session")
        if (session != null) edit.putString("presence_logout_session", session)
        edit.apply()
    }

    private fun nextSequence(): Long = synchronized(prefs) {
        val sequence = prefs.getLong("presence_sequence", 0) + 1
        check(prefs.edit().putLong("presence_sequence", sequence).commit())
        sequence
    }

    suspend fun ensureRegistered(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock { ensureRegisteredLocked() }
    }

    private suspend fun ensureRegisteredLocked(): Boolean {
        val userId = tokens.getUserId()
        if (userId == null) {
            retireAccount()
            return prefs.getBoolean("registered", false)
        }
        var session = prefs.getString("presence_session", null)
        if (session == null || prefs.getInt("presence_user", -1) != userId) {
            session = UUID.randomUUID().toString()
            prefs.edit().putString("presence_session", session).putInt("presence_user", userId)
                .remove("presence_bound_session").apply()
        }
        if (prefs.getString("presence_bound_session", null) != session) {
            if (prefs.getString("device_id", null) == null) return false
            val body = deviceInfo().put("device_id", prefs.getString("device_id", ""))
                .put("device_secret", prefs.getString("secret", ""))
                .put("account_session", session).put("sequence", nextSequence())
            registrationClient.newCall(request("register/", body, deviceAuth = false)).execute().use { response ->
                if (!response.isSuccessful) return false
            }
            // Login/logout can change while a request is in flight.
            if (tokens.getUserId() != userId || prefs.getString("presence_session", null) != session) return false
            prefs.edit().putBoolean("registered", true).putString("presence_bound_session", session).commit()
        }
        return true
    }

    suspend fun reportNow(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!ensureRegisteredLocked()) return@withLock false
                val userId = tokens.getUserId()
                val session = if (userId != null) prefs.getString("presence_session", null)
                    else prefs.getString("presence_logout_session", null)
                if (userId != null && (session == null || prefs.getInt("presence_user", -1) != userId ||
                        prefs.getString("presence_bound_session", null) != session)) return@withLock false
                val activity = foreground.get()?.javaClass?.name
                val body = JSONObject().put("sequence", nextSequence()).put("account_session", session ?: JSONObject.NULL)
                    .put("logged_in", userId != null).put("foreground", activity != null)
                    .put("screen", PresencePolicy.screen(activity, userId != null))
                client.newCall(request("presence/", body)).execute().use { response ->
                    if (response.code == 409) {
                        prefs.edit().remove("presence_bound_session").apply()
                        return@withLock false
                    }
                    if (!response.isSuccessful) return@withLock false
                }
                if (userId == null && prefs.getString("presence_logout_session", null) == session) {
                    prefs.edit().remove("presence_logout_session").apply()
                }
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not block startup, logout or report editing on connectivity.
                false
            }
        }
    }

    private fun request(path: String, body: JSONObject, deviceAuth: Boolean = true): Request {
        val builder = Request.Builder().url(BuildConfig.API_BASE_URL + "api/v1/updates/" + path)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (deviceAuth) builder.header("X-Device-ID", prefs.getString("device_id", "")!!)
            .header("X-Device-Token", prefs.getString("secret", "")!!)
        return builder.build()
    }

    private fun deviceInfo(): JSONObject {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return JSONObject().put("version_code", PackageInfoCompat.getLongVersionCode(info))
            .put("version_name", info.versionName ?: "").put("android_sdk", Build.VERSION.SDK_INT)
            .put("label", "${Build.MANUFACTURER} ${Build.MODEL}".take(150))
            .put("manufacturer", Build.MANUFACTURER.take(80)).put("model", Build.MODEL.take(120))
            .put("android_version", Build.VERSION.RELEASE.take(40))
    }
}
