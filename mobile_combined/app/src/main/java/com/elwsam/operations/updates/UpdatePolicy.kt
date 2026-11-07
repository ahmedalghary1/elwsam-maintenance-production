package com.elwsam.operations.updates

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

object UpdatePolicy {
    const val MAX_BYTES = 200L * 1024 * 1024

    fun requireValidMetadata(installed: Long, offered: Long, size: Long, sha256: String) {
        require(offered > installed) { "نسخة التحديث ليست أحدث من النسخة المثبتة." }
        require(size in 1..MAX_BYTES) { "حجم التحديث غير صالح." }
        require(sha256.matches(Regex("[a-f0-9]{64}"))) { "بصمة ملف التحديث غير صالحة." }
    }

    fun copyVerified(input: InputStream, output: OutputStream, expectedSize: Long, expectedHash: String, progress: (Int) -> Unit) {
        require(expectedSize in 1..MAX_BYTES)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= expectedSize) { "حجم الملف لا يطابق النسخة المنشورة." }
            digest.update(buffer, 0, count)
            output.write(buffer, 0, count)
            progress((total * 100 / expectedSize).toInt())
        }
        require(total == expectedSize) { "لم يكتمل تنزيل ملف التحديث." }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        require(actual == expectedHash) { "تعذر التحقق من سلامة ملف التحديث. ستتم إعادة تنزيله." }
    }
}
