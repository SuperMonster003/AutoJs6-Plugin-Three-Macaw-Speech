<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-macaw-speech-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>التعرف على الكلام دون اتصال, واكتشاف كلمة التنبيه, واكتشاف النشاط الصوتي, وتحويل النص إلى كلام بالشبكات العصبية, مع إمكانية استخدامه كمحرك TTS للنظام</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات (Languages)

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

******

### مقدمة

******

يستخدم 3-Macaw Speech مكتبة [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) لتشغيل التعرف على الكلام واكتشاف النشاط الصوتي واكتشاف كلمة التنبيه وتحويل النص إلى كلام بالشبكات العصبية على الجهاز دون اتصال. تجري كل المعالجة محليا ولا يُرفع أي صوت.

الإضافة ملف APK يُثبَّت بشكل منفصل. تستخدمها سكربتات AutoJs6 عبر الوحدة العامة `speech`, كما تُسجَّل قدرة تحويل النص إلى كلام كمحرك TTS لنظام Android, فيمكن لوحدة `tts` في AutoJs6 وللتطبيقات الأخرى اختيارها. لا تأتي نماذج التعرف والتركيب مع ملف APK, بل تُنزَّل عند الحاجة من شاشة الإضافة أو من سكربت, ولا يأتي مدمجا سوى نموذج اكتشاف النشاط الصوتي.

> هذا إصدار معاينة للتطوير: يتعرف AutoJs6 على الإضافة, وتحمّل الإضافة بيئة تشغيل الكلام وتنزّل النماذج وتديرها, لكن التعرف وكلمات التنبيه والتركيب وشاشات الإضافة ووحدة السكربت `speech` لم تُسلَّم بعد. يصف ما يلي أهداف تصميم الإصدار 1.0.0, وراجع خارطة الطريق لمعرفة التقدم.

******

### أبرز الميزات

******

- التعرف على الكلام: إملاء متدفق من الميكروفون (نتائج جزئية ونهائية في الوقت الفعلي) والتعرف على ملفات WAV, مع نماذج دون اتصال للصينية والإنجليزية والصينية المختلطة بالإنجليزية واليابانية والكورية والكانتونية.
- اكتشاف النشاط الصوتي: يقسم silero VAD v5 المدمج الصوت إلى مقاطع كلام بمفرده, ويُستخدم أيضا لإنهاء الجمل تلقائيا أثناء الإملاء.
- كلمات التنبيه: اكتب الكلمة المفتاحية نصا عاديا (مثل `小六小六` أو `hey macaw`) دون تدريب أي نموذج, ويمكن أن يبدأ الإملاء تلقائيا بعد التنبيه.
- تحويل النص إلى كلام: عدة أصوات عصبية صينية وإنجليزية, يمكن تركيبها في ملفات WAV أو استخدامها كمحرك TTS للنظام من خلال وحدة `tts` في AutoJs6 والتطبيقات الأخرى.
- إدارة النماذج: التنزيل من فهرس نماذج موقَّع, ولا يُفعَّل النموذج إلا بعد اجتياز كل ملف لفحص SHA-256, مع إمكانية استيراد النماذج المحلية, وتُعرض رخصة النماذج المقصورة على الاستخدام غير التجاري قبل تنزيلها.

******

### الاستخدام

******

1. حدّث AutoJs6 إلى الإصدار الداخلي 5451 (6.8.0) أو أحدث.
2. نزّل من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/releases) ملف APK المطابق لـ ABI الجهاز (اختر `universal` إن لم تكن متأكدا) وثبّته, ثم امنحه الإذن وفعّله في مركز الإضافات في AutoJs6.
3. افتح شاشة الإضافة لمنح إذن الميكروفون وتنزيل النماذج التي تحتاجها, ثم استخدم الوحدة `speech` في السكربتات.

> تدعم الإضافة الأجهزة التي تعمل بنظام Android 7.0 (API 24) أو أحدث. توجد حزمة لكل ABI, إضافة إلى حزمة `universal` تعمل على جميع الأجهزة.

******

### النماذج

******

يتراوح حجم كل نموذج من نماذج التعرف وكلمات التنبيه والتركيب بين 30 ميغابايت و350 ميغابايت, لذا لا تأتي مع ملف APK. تنزّل الإضافة النماذج من إصدارات ثابتة في مستودع الموارد المشتركة لـ AutoJs6, وتعود إلى عنوان sherpa-onnx الأصلي إذا تعذر الوصول إلى المرآة, وفي الحالتين يجب أن يجتاز كل ملف فحص SHA-256 قبل تفعيل النموذج.

يحمل كل نموذج رخصته. تعرض النماذج المقصورة على الاستخدام غير التجاري (مثل نموذج تركيب مدرَّب على مجموعة DataBaker) تنبيها قبل التنزيل, ويجب على السكربتات تمرير `acceptLicense: true` صراحة.

******

### واجهة برمجة النصوص

******

تستخدم السكربتات هذه القدرات عبر الوحدة العامة `speech` (ومتاحة أيضا باسم `$speech`, وهذه مسودة تصميم الإصدار 1.0.0, والمرجع هو توثيق الإصدار المنشور):

```javascript
let session = speech.listen({ language: "zh", silenceEndMillis: 1500 });
session.on("partial", (e) => console.log("...", e.text));
session.on("final", (e) => files.append("./dictation.txt", e.text + "\n"));
sleep(30000);
session.stop();

let wake = speech.wake(["hey macaw"], { thenListen: true });
wake.on("wake", (e) => e.listen.on("final", (r) => console.log(r.text)));

speech.synthesize("Task completed", "./done.wav");
tts("Task completed", { engine: "io.github.supermonster003.autojs6.plugin.three.macaw.speech" });
```

- تفتح `speech.listen(options)` جلسة إملاء تعيد النتائج عبر أحداث مثل `partial` و`final` و`end`, وتتعرف `speech.recognize(path)` على ملف WAV.
- تفتح `speech.wake(keywords, options)` جلسة كلمات تنبيه, ويبدأ الخيار `thenListen` الإملاء تلقائيا بعد التنبيه.
- تركّب `speech.synthesize(text, path)` الكلام في ملف WAV, وتنطق `speech.speak(text)` و`tts(text, {engine})` عبر محرك TTS للنظام, وتدير `speech.models` النماذج.

******

### الأسئلة الشائعة

******

#### هل يحتاج التعرف والتركيب إلى الإنترنت?

لا. بعد تنزيل النماذج تجري كل المعالجة على الجهاز ولا يُرفع أي صوت. لا تتصل الإضافة بالشبكة إلا لتنزيل النماذج وتحديث الفهرس.

#### لماذا تطلب الإضافة إذن الميكروفون بنفسها?

يُسجَّل الصوت داخل عملية الإضافة ولا يمر عبر AutoJs6 أبدا. يمنح AutoJs6 الإضافة قدراته في الواجهة الأمامية عند الربط, لذا يمكن للسكربتات الإملاء في الخلفية أيضا. امنح إذن الميكروفون من شاشة الإضافة قبل الاستخدام الأول.

#### أي حزمة يجب أن أثبّت?

تحتاج معظم الهواتف إلى `arm64-v8a`. إن لم تكن متأكدا من ABI الجهاز فثبّت `universal`, فهي تعمل على جميع الأجهزة لكن حجمها أكبر.

******

### الأذونات والأمان

******

تبقي الإضافة أذوناتها في أضيق نطاق ممكن:

- الميكروفون: لا يُسجَّل الصوت إلا للإملاء وكلمات التنبيه وشاشة التجربة, ويُحرَّر الميكروفون فور انتهاء الجلسة أو خروج السكربت.
- الشبكة: تُستخدم فقط لتنزيل النماذج وتحديث فهرس النماذج الموقَّع, ويعمل التعرف والتركيب دون اتصال.
- التخزين: تُحفظ النماذج في المجلد الخاص بالإضافة وتُستورد النماذج المحلية عبر منتقي الملفات في النظام, لذا لا يُطلب إذن التخزين.
- نقاط دخول محمية: تتطلب خدمة القدرات ونقطة التفعيل إذن إضافات AutoJs6 (`org.autojs.permission.PLUGIN`), فلا يمكن للتطبيقات الأخرى استدعاؤها مباشرة.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون والتفاوض على القدرات:

```text
application id: io.github.supermonster003.autojs6.plugin.three.macaw.speech
plugin id: three-macaw-speech
engine: speech
variant: default
info service: org.autojs.plugin.INFO (category three-macaw-speech)
capability service: org.autojs.plugin.SPEECH (category speech)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 5451
native libraries: libsherpa-onnx-jni.so, libonnxruntime.so (sherpa-onnx 1.13.8; arm64-v8a, armeabi-v7a, x86_64, x86)
```

******

### خارطة الطريق

******

تتابع الخطط وقرارات التصميم والتقدم في شكل قائمة تحقق في ROADMAP.md. العناصر غير المحددة خطط وليست ميزات في الإصدار الحالي.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/10/16_

- `تلميح` معاينة للتطوير: يتعرف AutoJs6 على الإضافة, وتحمّل الإضافة بيئة تشغيل الكلام وتنزّل النماذج وتديرها, ولم تُسلَّم بعد ميزات التعرف على الكلام وكلمات التنبيه وتحويل النص إلى كلام وشاشات الإضافة ووحدة السكربت speech
- `تلميح` يتطلب AutoJs6 6.8.0 (5451) أو أحدث (سيُحدَّد الحد الأدنى الدقيق للإصدار بعد اكتمال تغييرات المضيف)
- `ميزة` هيكل الإضافة: بروتوكول التفعيل Wake, وخدمة الهوية في مركز الإضافات, وخدمة قدرات الكلام
- `ميزة` بيئة تشغيل الكلام sherpa-onnx 1.13.8 ونموذج اكتشاف النشاط الصوتي silero VAD v5 مدمجان, مع حزمة لكل ABI من 4 أنواع
- `ميزة` إدارة النماذج: تنزيل نماذج الكلام دون اتصال المدرجة في كتالوج النماذج الموقّع من إصدارات الموارد المشتركة لـ AutoJs6, مع الرجوع إلى إصدارات sherpa-onnx الرسمية عند تعذرها; يُتحقق من كل ملف عبر SHA-256 قبل استخدامه, مع استئناف التنزيل والاستيراد المحلي (tar, tar.bz2, zip) والحذف والنماذج الافتراضية; وتتطلب النماذج المقصورة على الاستخدام غير التجاري أو غير المعلن ترخيصها قبول الترخيص أولا
- `ميزة` ملفات README وتعليمات مركز الإضافات وسجلات التغييرات بعشر لغات, مولَّدة من مصادر JSON
- `تبعية` إرفاق sherpa-onnx 1.13.8
- `تبعية` إرفاق Jetpack Compose BOM 2026.09.00
- `تبعية` إرفاق AndroidX Activity Compose 1.12.2
- `تبعية` إرفاق AndroidX Core KTX 1.15.0
- `تبعية` إرفاق Kotlin Coroutines 1.11.0

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

هذا القسم موجه للمطورين الراغبين في بناء المكون من المصدر.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\verify_native_alignment.py --check
py .python\check_repository.py
```

******

### الترخيص

******

يُنشر كود المشروع بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/LICENSE). تستخدم بيئة الكلام sherpa-onnx رخصة Apache-2.0, ويستخدم onnxruntime وsilero VAD رخصة MIT, وتُذكر رخصة كل نموذج في فهرس النماذج.

- [إشعارات الأطراف الثالثة (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech/blob/master/THIRD_PARTY_NOTICES.md)

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- مشروع sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
