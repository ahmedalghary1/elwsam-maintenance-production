package com.maintenance.supervisor.data.remote

import kotlinx.serialization.json.*

/** Keep the server's permission/validation explanation instead of calling every HTTP error a network failure. */
fun maintenanceHttpError(code: Int, body: String?): String {
    fun flatten(value: JsonElement): String = when (value) {
        is JsonPrimitive -> value.contentOrNull.orEmpty()
        is JsonArray -> value.joinToString(" ") { flatten(it) }
        is JsonObject -> value.values.joinToString(" ") { flatten(it) }
    }
    val explanation = body?.let {
        if (!it.trimStart().startsWith("{") && !it.trimStart().startsWith("[")) null else runCatching {
            val value = Json.parseToJsonElement(it)
            flatten((value as? JsonObject)?.get("detail") ?: (value as? JsonObject)?.get("reason") ?: value)
        }.getOrNull()
    }?.trim()?.take(1000)?.takeIf { it.isNotBlank() }
    val fallback = when (code) {
        401 -> "انتهت جلسة الدخول. سجّل الدخول بالحساب صاحب التقرير."
        403 -> "رفض الخادم صلاحية الحساب لإرسال التقرير. تحقق من دور الحساب ومصنعه والوردية المخصصة له."
        409 -> "تعارض التقرير مع بيانات الخادم."
        else -> "لم يقبل الخادم التقرير. أعد المحاولة بعد التحقق من بيانات الحساب."
    }
    return "${explanation ?: fallback} (HTTP $code)"
}
