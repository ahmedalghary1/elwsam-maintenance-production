# تطبيق الإنتاج Android

## الغرض والبنية

`mobile_production/` مشروع Android مستقل (package `com.production.supervisor`, applicationId نفسه). واجهة عربية Compose بأسلوب wizard سريع (الوضع الافتراضي `isFawryMode=true`) لتسجيل حالة كل ماكينة ووزن الإنتاج، وتسجيل التوقفات، ثم مراجعة الوردية وتسليمها للمشرف التالي. مرجع رحلة الاستخدام المكتوب `FAWRY_WIZARD_SPEC.md` يصف الهدف، لكن التنفيذ الحالي وViewModel هما المرجع.

- `ui/screens/home/ProductionHomeScreen.kt`, `ProductionHomeViewModel.kt`: حالة الصفحة وتدفق الماكينات.
- `ui/screens/wizard/`: `FawryWizardScreen`, `AtmNumpad`, `MachineExceptionDialog`.
- `ui/screens/entry/`: نموذج تفاصيل/استثناء الماكينة.
- `ui/screens/stoppage/`, `handover/`: التوقف والتسليم.
- `data/remote/`: API, DTOs, JWT Authenticator.
- `data/repository/ProductionRepository.kt`: Bootstrap وتخزين Room وبناء Payload وإرساله.
- `data/local/`: Entities وDAO و`ProductionDatabase.kt`.

## دورة الوردية

الحالة الأساسية في ViewModel تضم الوردية والتاريخ ومعرف التقرير والمصنع والماكينات والمنتجات والعمال والمدخلات والتوقفات والتسليم، وحالة wizard (`STATUS`, `WEIGHT`, `STOPPAGE`, `SUMMARY`). ينشئ Repository تقريراً محلياً عند عدم وجود `(date, shift)`، بمعرف UUID ثابت. كل تعديل آلة أو توقف يحفظ في Room ثم يحاول الإرسال مباشرة؛ نهاية الوردية تحفظ `completedAtDevice` والحالة `PENDING_HANDOVER` وترسل. يوجد وضع محلي عند تعذر جلب Bootstrap، لكن لا يوجد WorkManager في هذا المشروع؛ لا تفترض وجود مزامنة دورية أو retry تلقائي بعد رجوع الإنترنت.

افتراضات الوردية/المنطقة الزمنية تحتاج الانتباه: يستخدم ViewModel تاريخ الجهاز `LocalDate.now()`، ويختار shift المستخدم من Bootstrap إن كان مسنداً. الخادم يعمل على `Africa/Cairo`. عند تغيير حساب التاريخ أو توقيت التقرير، وحّد الطرفين صراحة.

## مصدر البيانات والإعدادات

Bootstrap الخادم يجلب الأصول النشطة وترتيبها وإعداداتها الافتراضية والمنتجات والعمال النشطين وتقارير اليوم والتسليم المعلق. Repository يخزن الأصول/المنتجات/العمال في Room ويحفظ المصنع المختار في TokenManager. شاشة الوردية تقرأ تدفقات Room؛ إعدادات الماكينة الافتراضية تملأ المنتج والعامل وعدد اللقم وأزمنة الدورة والتبريد.

إدخال `MachineProductionEntry` يحمل هوية التقرير والماكينة، العامل/المنتج الحالي والافتراضي ومؤشرات التغيير، اللقم والوضع `AUTO`/`MANUAL`، الوزن النهائي، الخامة والعبوة والملاحظات. لا تختزل حقول الإدخال إلى الوزن فقط إلا إذا كان هذا مقصوداً في واجهة wizard مع إبقاء payload/بيانات التفاصيل المطلوبة.

## المزامنة والتسليم

API مبين في [تكامل API](integration.md). الخادم يربط الوردية التالية بمستخدم نشط مسند للمصنع إن وجد. Bootstrap يحدد `pending_handover` حسب الوردية السابقة ثم يملك fallback لتقرير معلق آخر. التأكيد يغير الحالة إلى `CONFIRMED`.

مهم: التنفيذ الحالي يستدعي `syncReportNow` بعد حفظ المدخل والتوقف وكذلك بعد إنهاء الوردية، وليس عبر صف WorkManager. ادرس النتيجة الفعلية والإشارات المحلية قبل تغيير استراتيجية الإرسال. لا تحذف أو تبدل `client_report_id` في retries.

## البناء

متطلبات Gradle/JDK/SDK موضحة في ملف `app/build.gradle.kts`; Wrapper موجود في الجذر. `BUILD_FINAL_APK.cmd` هو مسار Windows لإنتاج APK وفق README الصيانة (قد يكون منسوخاً/متشابهاً بين المشروعين، افحص محتواه ومكان الخرج قبل استخدامه). إعداد release الحالي في Gradle يوقع بـdebug signing، و`usesCleartextTraffic` مضبوط true حتى release؛ لا تعتبر ذلك إعداد توزيع آمن أو مطابقاً لوثائق README من دون مراجعة واعتماد إعداد توقيع وسياسة الشبكة.
