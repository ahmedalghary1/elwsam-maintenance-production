# تكامل التطبيقات مع Django API

## المصادقة المشتركة

كلا التطبيقين يستخدمان المسارات ذاتها:

```text
POST /api/v1/auth/login/       phone + password => access, refresh
POST /api/v1/auth/refresh/     refresh => access (+ refresh عند التدوير)
GET  /api/v1/auth/me/          بيانات المستخدم
```

يُرسل Access JWT في `Authorization: Bearer <token>`. تفاصيل التجديد في Authenticator كل عميل. افتراضات SimpleJWT بالخادم: access 30 دقيقة وrefresh 7 أيام مع تدوير refresh، قابلة للضبط بالبيئة.

## صيانة

معرّف المسارات الفعلي في `web/api/urls.py`:

```text
GET  /api/v1/mobile/bootstrap/
GET  /api/v1/mobile/current-maintenance/
POST /api/v1/mobile/current-maintenance/select/
POST /api/v1/mobile/reports/
PUT  /api/v1/mobile/reports/{server_pk}/
POST /api/v1/mobile/sync/reports/
```

`bootstrap` يضم payload الحالة الحالية (`asset`, `checklist_template`, `is_reported`, `report`, `order_version`, `is_maintenance_day`, `selection_mode`, `cycle_position`, `cycle_total`) ويضيف `user`, `active_assets`, `checklist_templates`. DTO الفعلي مرجع مطابقة الحقول: `mobile_maintenance/.../data/remote/Dto.kt`.

الحفظ الجماعي الفعلي يقبل `{ "reports": [...] }`، ويرجع `{ "results": [...], "current_maintenance": ..., "server_date": ... }`. كل نتيجة تتضمن `client_report_id`, `status`, `reason`, `report_id`. حالات الخدمة تشمل `synced`, `already_synced`, `conflict`, `rejected`. التطبيق يحتفظ بالإجابات محلياً؛ لا تعتبر `conflict` أو `rejected` نجاحاً.

لكل تقرير `client_report_id` UUID ثابت، مع `asset_id`, `report_date`, `started_at_device`, `completed_at_device`, `last_modified_at_device`, `answers`, ويمكن أن يضم `emergency_items` وبيانات طاقم الصيانة. المزامنة idempotent: التقرير القائم لا يعدل إذا كان مقفلاً، والتحديث لا يُقبل إن كان توقيت تعديل الجهاز ليس أحدث. يتحقق الخادم من المصنع، الماكينة المستحقة وتسلسل الدورة وقائمة الفحص.

## إنتاج

```text
GET  /api/v1/production/bootstrap/?factory=<id optional>
POST /api/v1/production/sync/shift-reports/
POST /api/v1/production/reports/confirm-handover/
```

مصدر تعريف العميل `mobile_production/.../data/remote/api/ProductionApiService.kt` وDTOs في `.../dto/ProductionDtos.kt`؛ مصدر الخادم `web/production/api_urls.py`, `api_views.py`, `serializers.py`.

Bootstrap يعيد `server_date`, `timezone`, المصنع المحدد وقائمة المصانع النشطة والمستخدم والماكينات النشطة مع إعدادات الإنتاج والمنتجات والعمال النشطين، بالإضافة إلى `pending_handover` و`today_reports`. طلب تحديد المصنع Query parameter. على الخادم صلاحية الإنتاج مطلوبة؛ للمشرف العادي المصنع من حسابه، ويمكن للمدير اختيار مصنع نشط.

تقرير المزامنة يرسل `client_report_id`, `shift`, `report_date`, أوقات الجهاز، status، `general_notes`, `entries`, `stoppages`. استخدم UUID نفسه لكل إعادة إرسال. الخادم ينشئ أو يحدّث تقريراً غير مؤكد، ويستنتج تغييرات المنتج/العامل من الافتراضي، ويحافظ على الإدخالات الموجودة عند ورود قائمة entries فارغة. تأكيد الاستلام يرسل `client_report_id` و`handover_notes`.

## نقاط اختلاف بين وصف README والتنفيذ

- README الصيانة يعرض شكلاً متوقعاً لـBootstrap يختلف عن مفاتيح التنفيذ الحالية؛ كما يصف استجابة المزامنة بمفتاح `reports`، بينما View الخادم يرجع `results`. قد يكون DTO العميل وسيطاً/قديم التسمية. قبل أي تعديل، اقرأ `Dto.kt` و`MaintenanceRepositoryImpl.kt` و`api/views.py` معاً واتبع العقد الذي يجري تشغيله فعلياً.
- README يقول إن التطبيقين يمنعان الاتصال غير المشفر، لكن `mobile_production/app/build.gradle.kts` يجعل `usesCleartextTraffic=true` في debug وrelease، بينما الصيانة false. خذ القيمة الفعلية من Gradle/Manifest ولا تكرر ادعاء README.
- `web/schema.yml` ملف محفوظ لا يُحدّث تلقائياً عند تعديل View؛ توليد المخطط والتحقق منه عملية منفصلة.
- قاعدة صلاحية صيانة API هي `IsMaintenanceSupervisor` وتطابق دور `MAINTENANCE_SUPERVISOR` فقط. لا تفترض أن `GENERAL_SUPERVISOR` المسموح له في property `can_access_maintenance` يمر من هذه الـpermission؛ راجعها قبل أي توسيع صلاحيات.

## عند تغيير العقد

حدّث Serializer/DTO وRetrofit method وRepository والتحويلات واختبارات العقد الموجودة. حافظ على أسماء JSON الصريحة بـ`SerialName`، القيم الافتراضية للحقول الاختيارية، حالات المزامنة، التواريخ بصيغة ISO، وحد المصنع. راجع OpenAPI الناتج، لا ملفاً قديماً فقط.
