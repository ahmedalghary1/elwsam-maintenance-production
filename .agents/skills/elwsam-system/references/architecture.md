# المعمارية وحدود المجال

## الصورة العامة

```text
موقع الويب (قوالب Django) ─┐
تطبيق Android الإنتاج ─────┼──> web/config (Django) ──> قاعدة بيانات مشتركة
تطبيق Android الصيانة ─────┘
```

المضيف المعلن في إعدادات التطبيقين وREADME هو `elwsam.pythonanywhere.com`. الموقع والـAPI ليسا خدمتين منفصلتين داخل المستودع: `web/config/urls.py` يجمع صفحات الإدارة والمسارات تحت `/production/` وAPI تحت `/api/v1/`.

## التقنيات

- الخادم: Django 5.2 وDjango REST Framework وSimpleJWT وdrf-spectacular وSQLite افتراضياً. إعداد PostgreSQL متاح عبر متغيرات البيئة. المنطقة الزمنية `Africa/Cairo` و`USE_TZ=True`.
- الويب: Django Templates وCSS محلي في `web/static/`؛ واجهة عربية RTL. لوحة الإدارة التقنية Django Admin منفصلة عن الـDashboard.
- تطبيقا Android: Kotlin، Compose، Hilt، Retrofit/OkHttp، Kotlin Serialization، Room، DataStore، Gradle Wrapper 8.13، Android Gradle Plugin 8.13.2، SDK 36 وminSdk 23 وJDK 17.

## بيانات مشتركة

- `factories.Factory`: رمز واسم وحالة المصنع.
- `accounts.User`: تسجيل الدخول بـ`phone`، مع `factory` و`role` و`shift`. الأدوار: `ADMIN`, `MAINTENANCE_SUPERVISOR`, `PRODUCTION_SUPERVISOR`, `GENERAL_SUPERVISOR`. المشرف يحتاج مصنعاً.
- `assets.Asset`: ماكينة/أصل مشترك، نوعه `REGULAR_MACHINE`, `PRESS`, `SPRING_MACHINE`، له كود وترتيب وحقول نشاط/أرشفة. الأنواع المسموحة حسب رمز المصنع: F1 وF3 يدعمان العادي والمكبس؛ F2 يدعم العادي وماكينة السوستة (رغم أن تسمية display في enum تقول نفخ؛ راجع الاستعمال الفعلي قبل تغيير النوع).
- ربط المصنع هو حد العزل الأساسي للبيانات. لا تقبل `factory_id` المرسل من الهاتف بوصفه مصدراً موثوقاً؛ الخادم يستنتج المصنع من الجلسة/المستخدم، إلا في مسار اختيار مصنع المدير حيث يلزم التحقق.

## مجالا العمل المنفصلان

### الإنتاج

تطبيقه وAPI الخاص به يسجلان وردية لكل مصنع/تاريخ/shift. كيانات المجال في `web/production/models.py`: `Product`, `MachineOperator`, `MachineProductionDefault`, `ProductionShiftReport`, `MachineProductionEntry`, `ProductionStoppage`. إعداد المنتج/العامل/اللقم الافتراضي في `MachineProductionDefault` مرتبط بماكينة Asset مشتركة. بيانات الإنتاج وتوقفاته لا تتحول إلى تقارير الصيانة.

الحالات الخادمية: `DRAFT`, `PENDING_HANDOVER`, `CONFIRMED`. تقرير الوردية يحتوي المشرف والتاريخ والوردية، إدخالات الماكينات، التوقفات، الملاحظات، المشرف المستلم وتأكيد الاستلام. يوجد قيد فريد على `(factory, report_date, shift)`.

### الصيانة

تطبيقه وAPI الخاص به يسجلان تقرير فحص يومي لماكينة واحدة في كل يوم صيانة للمصنع. الكيانات في `web/maintenance/models.py`: قوالب الفحص وأقسامها وبنودها، `FactoryMaintenanceState`, `MaintenanceReport`, بنود الإجابة والصيانة الطارئة. منطق ترتيب الدور وحساب الماكينة المستحقة والتحقق والحفظ في `MaintenanceCycleService`، وليس في الـViews.

الجمعة عطلة. المصنع F1 وF3 يدور بين الماكينات العادية ثم المكابس، وF2 بين العادية وماكينة السوستة. لا تتقدم الدورة بمرور يوم فقط؛ لا بد من تقرير مكتمل. الأصل الحالي النشط يبقى هو نفسه عند تغيّر الترتيب، والأرشفة لا تمحو التاريخ.

## الموقع والـDashboard

- `/` و`/daily/`, `/reports/`, `/users/`, `/factories/`, `/assets/`, `/checklists/` في `web/dashboard/urls.py` و`web/dashboard/views.py`؛ تعرض وتشغّل مجال الصيانة والإعدادات المشتركة.
- `/production/` يعرض تقارير الإنتاج، إعدادات الماكينات، المنتجات، العمال، والتوقفات عبر `web/production/urls.py` و`views.py`.
- `/admin/` للإدارة التقنية. `/login/` و`/logout/` لواجهة الموقع.
- قوالب الصفحات في `web/templates/dashboard`, `web/templates/production`, `web/templates/registration`، والقالب العام `web/templates/base.html`.

## إعداد وتشغيل الخادم

راجع `web/README.md` و`web/requirements.txt` للتثبيت؛ الأوامر الأساسية: إنشاء venv، تثبيت المتطلبات، إعداد `.env`، `python manage.py migrate`, إنشاء مستخدم، ثم `runserver`. لا يوجد في الجرد `.env.example` رغم أن README يطلب نسخه؛ تحقق من الحالة المحلية قبل افتراض وجوده. التوثيق: `/api/schema/` و`/api/docs/`؛ الملف المحلي `web/schema.yml` قد يكون قديماً مقارنة بالتنفيذ.
