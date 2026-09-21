# تطبيق مشرف الصيانة

تطبيق Android Native عربي (RTL) يعمل بأسلوب offline-first. الواجهة تقرأ دائمًا من Room، بينما تقوم الشبكة بتحديث النسخة المحلية ويرفع WorkManager التقارير المكتملة تلقائيًا بالترتيب الزمني.

## المتطلبات

- Android Studio بإصدار يدعم Android Gradle Plugin 8.13.2.
- JDK 17.
- Android SDK 36 (الحد الأدنى للجهاز Android 6 / API 23).
- لا حاجة لتثبيت Gradle منفصل؛ المشروع يحتوي على Gradle Wrapper 8.13.

## التشغيل

1. افتح مجلد المشروع في Android Studio.
2. عنوان الـ API مضبوط في نسختي Debug وRelease على `https://elwsam.pythonanywhere.com/` داخل `app/build.gradle.kts`.
3. نفّذ Gradle Sync ثم شغّل `app` على جهاز أو محاكي.

ينتهي Base URL بشرطة `/`، وتمنع نسختا Debug وRelease الاتصال غير المشفر.

## عقد الـ API

تم ربط المسارات التالية:

- `POST /api/v1/auth/login/`
- `POST /api/v1/auth/refresh/`
- `GET /api/v1/auth/me/`
- `GET /api/v1/mobile/bootstrap/`
- `GET /api/v1/mobile/current-maintenance/`
- `POST /api/v1/mobile/reports/`
- `PUT /api/v1/mobile/reports/{id}/`
- `POST /api/v1/mobile/sync/reports/`

لأن المخطط المرسل لم يتضمن شكل استجابة `bootstrap` و`sync/reports` كاملًا، يعتمد العميل العقد التالي القابل للتعديل في `data/remote/Dto.kt`:

```json
{
  "user": { "id": 1, "phone": "...", "role": "MAINTENANCE_SUPERVISOR", "factory": { "id": 1, "name": "...", "code": "..." } },
  "factory": { "id": 1, "name": "...", "code": "..." },
  "assets": [{ "id": 1, "code": "...", "name": "...", "type_name": "...", "sequence": 1, "checklist_template_id": 1, "is_active": true }],
  "checklist_templates": [{ "id": 1, "name": "...", "sections": [{ "id": 1, "template_id": 1, "title": "...", "sequence": 1, "items": [{ "id": 1, "section_id": 1, "text": "...", "sequence": 1 }] }] }],
  "current_maintenance": { "asset_id": 1, "report_date": "2026-09-17", "position": 1, "total": 12 }
}
```

استجابة المزامنة المتوقعة:

```json
{"reports":[{"client_report_id":"uuid","id":123,"status":"accepted","detail":null}]}
```

قيم النجاح المقبولة هي `accepted`, `created`, `updated`, `synced`, `success`. أي نتيجة أخرى تبقى محفوظة محليًا بحالة خطأ ويعيد WorkManager المحاولة. يجب أن يتعامل Django مع `client_report_id` كمفتاح idempotency لمنع التكرار.

## المعمارية والعمل دون إنترنت

- `data/local`: Room entities وDAO والعلاقات.
- `data/remote`: Retrofit DTOs وتعريف المسارات وAuthenticator.
- `data/repository`: مصدر الحقيقة الواحد وتحويل DTO → Entity → Domain.
- `domain`: النماذج وواجهات المستودعات ومنطق دورة الصيانة.
- `ui`: Compose، ViewModels، Login/Home/Inspection.
- `sync`: WorkManager مع شرط `NetworkType.CONNECTED` وexponential backoff.

بعد أول Bootstrap ناجح تظهر البيانات المحلية فورًا في كل تشغيل. كل Checkbox أو ملاحظة يُحفظ مباشرة في Room. عند إنهاء التقرير يتحول إلى `PENDING_SYNC` ويضاف إلى الطابور، ثم تُرفع التقارير من الأقدم إلى الأحدث. لا يحذف التطبيق التقرير المحلي عند فشل الشبكة أو تعارض الخادم أو تسجيل الخروج.

يُحسب يوم العمل حسب `Africa/Cairo`، بينما تُحفظ التوقيتات بصيغة ISO-8601. التقرير غير المكتمل يبقي نفس الماكينة، والتقرير المكتمل لا ينقل المستخدم إلى ماكينة أخرى في نفس اليوم.

Tokens مشفرة بمفتاح AES/GCM داخل Android Keystore قبل تخزينها في DataStore. لا يسجل التطبيق كلمات المرور أو Tokens، ويستخدم `FLAG_SECURE` لمنع لقطات الشاشة.

## البناء والاختبارات

### إنشاء APK نهائي موقّع بدون Android Studio

على جهاز Windows المجهز بـ JDK وAndroid SDK، شغّل من CMD:

```cmd
cd /d "C:\Users\m\Desktop\projects\الصيانة\mobile"
call BUILD_FINAL_APK.cmd
```

يضبط السكربت `JAVA_HOME` و`ANDROID_HOME` و`ANDROID_SDK_ROOT` و`GRADLE_USER_HOME` محليًا، ويبني Release محسّنًا ثم يقوم بعمل zipalign والتوقيع والتحقق. عند أول تشغيل فقط سيطلب إنشاء مفتاح التوقيع في `.signing\maintenance-release.jks`. يجب الاحتفاظ بهذا المفتاح وكلمة مروره لتوقيع كل تحديث لاحق.

النتيجة النهائية تكون في:

```text
release-output\maintenance-supervisor-release.apk
```

ملف APK مخصص لهاتف Android ولا يُفتح كبرنامج Windows.

من Terminal داخل المشروع:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

ملف Debug APK ينتج في `app/build/outputs/apk/debug/`. إصدار Release يحتاج إعداد توقيع خاص بالمؤسسة عبر إعدادات آمنة خارج المستودع؛ لا توجد مفاتيح توقيع ضمن المشروع.
