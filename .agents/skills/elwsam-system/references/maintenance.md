# تطبيق الصيانة Android

## الغرض والبنية

`mobile_maintenance/` مشروع Android مستقل (package `com.maintenance.supervisor`). يسجل فحصاً يومياً لماكينة واحدة وفق checklist، مع ملاحظات وبنود صيانة طارئة وطاقم الصيانة. واجهته Compose وRTL، وRoom هو مصدر الحقيقة للواجهة بعد Bootstrap؛ لذلك يكمل العمل دون اتصال.

- `ui/screens/`: `LoginScreen`, `HomeScreen`, `InspectionScreen`؛ الحالة والتنقل في `ui/AppViewModels.kt` و`MainActivity.kt`.
- `domain/model/`: نماذج `Models.kt` ومنطق دوران بسيط `MaintenanceCycle.kt`.
- `domain/repository/Repositories.kt`: الواجهات ونتائج `AppResult`.
- `data/remote/`: `MaintenanceApi.kt`, `Dto.kt`, `AuthNetworking.kt`, `NetworkMonitor.kt`.
- `data/local/`: Room entities/relations/DAO و`AppDatabase.kt`؛ سجل migrations في `app/schemas/`.
- `data/repository/MaintenanceRepositoryImpl.kt`: Bootstrap والتحويل والحفظ المحلي والإكمال والمزامنة.
- `sync/SyncWorker.kt`: إرسال متصل بالشبكة مع exponential backoff؛ مهمة دورية كل 6 ساعات ومهمة فورية فريدة.
- `data/security/TokenStore.kt`: حفظ رموز الدخول مشفرة وفق README باستخدام Android Keystore/AES-GCM.

## دورة الفحص

1. تسجيل دخول برقم الهاتف وكلمة المرور؛ تحميل Bootstrap لتخزين المستخدم والمصنع والأصول النشطة والقوالب والحالة الحالية في Room.
2. `startOrLoadToday()` يستأنف مسودة اليوم أو التقرير المكتمل الموجود محلياً؛ إن لم توجد مسودة ينشئ UUID وتُنشأ إجابات افتراضية لكل بند.
3. كل checkbox/ملاحظة/بيانات الطاقم/صيانة طارئة تحفظ مباشرة في Transaction Room. التقرير المقفل لا يقبل تعديلاً.
4. الإكمال يخزن وقت الإكمال وتعديل الجهاز، يحول الحالة إلى `PENDING_SYNC`، ويضيفه إلى sync queue. SyncWorker يرسل التقارير الأقدم فالأحدث عند توفر الشبكة.
5. حالات النتائج الناجحة تزال من الصف وتوسم `SYNCED`; `conflict` و`rejected` يحافظان على التقرير والسبب ويزيلان المحاولة الآلية من الصف كي لا يعاد الخطأ بلا توقف. أخطاء الاتصال قابلة لإعادة المحاولة.

لا تمسح بيانات التقرير المحلي عند فشل الشبكة أو conflict أو تسجيل الخروج. تغيير الماكينة الحالية يجب أن يراعي أن تقريراً مكتملاً في اليوم يمنع التغيير.

## قواعد دورة الخادم

المنطق الحاكم في `web/maintenance/services/cycle.py`:

- الجمعة يوم عطلة؛ تاريخ اليوم حسب `Africa/Cairo`.
- F1/F3: الماكينات العادية ثم المكابس؛ F2: العادية ثم ماكينات السوستة.
- لا تتقدم الدورة حتى اكتمال تقرير صالح. عنصر مستحق يبقى عبر الأيام التي لم يكتمل فيها الفحص.
- الماكينة المختارة يدوياً تحفظ في `FactoryMaintenanceState`; لا تختَر عبر التطبيق ما لم يسمح الخادم، ولا تثق بترتيب الجهاز كمصدر للحقيقة.
- القائمة القياسية للمكن العادي والمكبس، وقائمة SPRING لماكينة السوستة. الخادم يتحقق من إرسال كل بنود القالب النشطة مرة واحدة ومن تبعية الأصل للمصنع.
- التقرير التاريخي مقفل؛ إعادة المزامنة للمعرف نفسه idempotent، ويستخدم `last_modified_at_device` لتحديد نسخة التعديل الأحدث.

## توقيت وقواعد محلية

يحسب التطبيق التاريخ بـ`LocalDate.now(clock.withZone(Africa/Cairo))` ويحفظ timestamps ISO-8601. الجمعة لا يبدأ فيها فحص. مسودة غير مكتملة تبقي الماكينة نفسها؛ التقرير المكتمل لا ينقلها إلى ماكينة أخرى في اليوم ذاته. حافظ على هذه القيود عند تعديل ViewModel أو آلية bootstrap.

## قاعدة البيانات والبناء

Room version الظاهر في `AppDatabase.kt`/Gradle مع ملفات export في `app/schemas/com.maintenance.supervisor.data.local.AppDatabase/`. أي تغيير schema يحتاج migration متوافقة وملف schema جديداً وفق سير المشروع؛ لا تمسح ملفات التاريخ. Wrapper و`BUILD_FINAL_APK.cmd` موجودان. README الصيانة يصف Windows build النهائي ومسار `release-output/maintenance-supervisor-release.apk` لكنه يحوي مساراً قديماً؛ افحص السكربت وGradle قبل الاعتماد عليه.

## تحقق العقد

استجابة `BootstrapDto` الفعلية ليست بالضرورة مساوية للمخطط المثال في README. راجع معاً `web/api/views.py:current_payload/BootstrapView`, `web/api/serializers.py`, `MaintenanceApi.kt`, `Dto.kt`, و`MaintenanceRepositoryImpl.kt`. المسار الدفعي في الخادم يعيد `results` بينما DTO/المستودع قد يستخدم تسمية Kotlin مختلفة عبر `SerialName`. لا تغيّر أحد الطرفين وحده.
