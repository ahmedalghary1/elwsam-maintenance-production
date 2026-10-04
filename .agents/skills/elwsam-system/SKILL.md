---
name: elwsam-system
description: Understand and change the Elwsam system: its shared Django website/API and the separate Android production and maintenance apps. Use for work in this repository; not for unrelated Django or Android projects.
metadata:
  short-description: خريطة مشروع الوسام
---

# نظام الوسام

هذا المستودع منتج واحد مكوّن من ثلاثة أجزاء: موقع/خلفية Django واحدة في `web/`، وتطبيق Android للإنتاج في `mobile_production/`، وتطبيق Android للصيانة في `mobile_maintenance/`. التطبيقان ليسا وحدتي Android داخل مشروع Gradle واحد؛ لكل منهما إعداد Gradle وRoom وواجهات API مستقلة. كلاهما يتصل بالخلفية نفسها على `https://elwsam.pythonanywhere.com/`.

## خريطة سريعة

- `web/`: Django 5.2، قوالب الموقع العربية RTL، إدارة المصانع والمستخدمين والأصول والصيانة، صفحات تقارير الإنتاج، REST API وOpenAPI. قاعدة البيانات الافتراضية SQLite، والمستخدم المخصص للدخول برقم الهاتف.
- `mobile_production/`: Kotlin/Jetpack Compose للإنتاج والأعطال وتسليم الورديات. اقرأ [مرجع الإنتاج](references/production.md) قبل تغيير رحلة الوردية أو المزامنة.
- `mobile_maintenance/`: Kotlin/Jetpack Compose لفحص ماكينة يومي، يعمل Offline First مع Room وWorkManager. اقرأ [مرجع الصيانة](references/maintenance.md) قبل تغيير دورة الفحص أو المزامنة.
- حدود المجال وملكية البيانات وتدفق الموقع موضحة في [المعمارية](references/architecture.md). عقد المسارات والفروقات الموثقة موضحة في [تكامل API](references/integration.md).

## قواعد مهمة عند العمل

1. حدد أي تطبيق وأي مجال يتأثر أولاً. الإنتاج والصيانة يشتركان في المستخدم والمصنع والأصول والخادم فقط؛ تقاريرهما وجداول Room وواجهاتهما منفصلة.
2. تتبع أي تغيير API عبر الطبقات الثلاث: View/Serializer في `web/`، ثم Retrofit DTO/service/repository في التطبيق المقابل، ثم الصفحة/الحالة. لا تعتمد على اسم endpoint وحده.
3. عند تعديل الأصول أو ترتيبها أو المستخدم/الدور، افحص أثر التغيير على المجالين وعلى صلاحيات الموقع والتطبيقين. `assets.Asset` مشترك بين الإنتاج والصيانة، وكذلك `accounts.User` و`factories.Factory`.
4. اعتبر التنفيذ الحالي مصدر الحقيقة عند وجود اختلاف مع README أو `FAWRY_WIZARD_SPEC.md` أو `schema.yml`. افحص السلوك الفعلي في الملفات المشار إليها قبل تغيير العقد؛ بعض الوثائق أو المخطط قد لا يعكسان كل التعديلات الحالية.
5. تقارير الهاتف تحفظ UUID محلياً في `client_report_id`. حافظ على هذا المعرّف عبر إعادة المحاولة لمنع ازدواج التقارير. راعِ التوقيت المحلي `Africa/Cairo` عند حساب يوم التقرير والورديات.
6. لا تضع أسراراً أو بيانات توقيع في المستودع. لا تفترض أن Debug signing في Gradle يصلح لتوزيع إنتاجي.

## نقاط بدء بحسب المهمة

| المهمة | ابدأ من |
|---|---|
| موقع/Dashboard أو صلاحيات الويب | `web/config/urls.py`, `web/dashboard/urls.py`, `web/dashboard/views.py`, `web/production/views.py` |
| مستخدمون ومصانع وأصول مشتركة | `web/accounts/models.py`, `web/factories/models.py`, `web/assets/models.py` |
| API الصيانة | `web/api/urls.py`, `web/api/views.py`, `web/api/serializers.py`, `web/maintenance/services/cycle.py` |
| API الإنتاج | `web/production/api_urls.py`, `web/production/api_views.py`, `web/production/serializers.py`, `web/production/models.py` |
| واجهة/تدفق إنتاج Android | `mobile_production/.../ui/screens/`, ثم `ProductionHomeViewModel.kt` و`ProductionRepository.kt` |
| فحص/مزامنة صيانة Android | `mobile_maintenance/.../ui/`, ثم `MaintenanceRepositoryImpl.kt`, `MaintenanceApi.kt`, `SyncWorker.kt` |

## طريقة تعديل آمنة

افهم أولاً الحالة الحالية من model/service، لا من نص الشاشة وحده. قبل إنهاء تغيير عقد، تأكد أن أسماء JSON والحقول الاختيارية وقيم الحالات متطابقة بين Django وKotlin. حافظ على العزل حسب المصنع، تاريخ التقرير، دور المستخدم، وسلامة التقرير المحلي غير المتزامن. راجع اختبارات الوحدة الموجودة ذات الصلة عند الحاجة إلى تعديل سلوك مغطى؛ لا تنشئ بيانات أو migrations أو تغييرات نشر لمجرد تسهيل التطوير.
