# 🖥️ Self-Service POS Screensaver & Ad Display System
### نظام شاشة التوقف وعرض الإعلانات لأجهزة نقاط البيع والخدمة الذاتية

---

🌐 **Language / اللغة:** [English](#-english-version) | [العربية](#-النسخة-العربية)

---

# 🇬🇧 English Version

## 📌 Executive Summary
The **Self-Service POS Screensaver & Ad Display System** is an enterprise-grade solution built for self-service payment kiosks and POS (Point of Sale) terminals—specifically optimized for devices running Android 14/15 on portrait screens (1080x1920), such as the **FY-3576SE (Rockchip RK3576)**, as well as modern Web-based POS systems.

In high-traffic retail environments, cash register and kiosk software keep the screen awake permanently (`FLAG_KEEP_SCREEN_ON`). As a result, the standard Android screensaver never activates, leaving kiosks idle on completed transactions or payment screens. This solution solves that challenge by **passively monitoring idle time without altering or interrupting the underlying POS software**, displaying promotional video commercials and posters, and waking up immediately with zero latency when a customer touches the screen or scans a barcode.

---

## 🎯 The Problem & Our Solution

| Problem in Standard POS Kiosks | Our Solution |
| :--- | :--- |
| POS apps force screen on, preventing default OS screensavers from activating | Background service ([`IdleOverlayService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/IdleOverlayService.java)) monitors inactivity globally across Android |
| Idle kiosks display static checkout screens, wasting prime marketing real estate | High-impact full-screen looped video and image advertisements displayed automatically from `/sdcard/Ads/` |
| Touching the screen to wake up might trigger underlying POS buttons ("Ghost Clicks") | Anti-ghost click touch handling ensures wake-up taps dismiss the ad without clicking buttons behind it |
| Barcode scanner input does not wake up traditional screensavers | Hardware barcode scanner events intercepted by [`KioskAccessibilityService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/KioskAccessibilityService.java) to wake up and pass barcode directly to POS |
| High-bitrate 4K/landscape videos cause GPU crashes or black screens on kiosk chipsets | Automated FFmpeg optimizer script ([`optimize_videos.py`](file:///d:/screen_saver/tools/optimize_videos.py)) scales and pads video to 1080x1920 portrait H.264 High 4.1 |

---

## 🚀 Key Features

* ⏱️ **Configurable Inactivity Timer**: Defaults to **60 seconds** (1 minute). Automatically resets whenever a customer touches the screen or scans a barcode.
* 🎬 **Mixed Media Support**: Seamlessly rotates promotional images (`.jpg`, `.png`, `.webp`) and high-definition video ads (`.mp4`, `.webm`) in a continuous shuffled loop.
* ⚡ **Zero-Latency Touch Wake-Up**: Instantly hides the ad screen and returns to the active POS transaction window as soon as the customer taps the screen.
* 🛡️ **Anti-Ghost Click Protection**: The tap that dismisses the screensaver is consumed cleanly, preventing accidental clicks on underlying POS buttons (e.g., "Cancel", "Pay", or cart items).
* 📦 **Barcode Scanner Wake-Up**: Intercepts barcode/QR scanner keystrokes via Android Accessibility, waking the kiosk and feeding the barcode digits directly to the checkout software.
* 🔄 **Auto-Start on Boot**: Automatically boots in the background when the device powers on via [`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java).
* 📱 **Optimized for 1080x1920 Portrait**: Built natively for 9:16 vertical displays common in self-checkout and fast-food kiosk hardware.
* 🌐 **Bilingual (Arabic & English)**: Complete Arabic (RTL) and English support with native localization and localized numerical representations.
* 🔌 **Dual Architecture (Android Native + Web Drop-in)**: Can be deployed as a background Android APK on top of any existing POS app, or embedded directly inside Web POS systems via [`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js).

---

## 🏗️ System Architecture & Components

### 1. Native Android Application ([`android_screensaver_app/`](file:///d:/screen_saver/android_screensaver_app/))
* [`MainActivity.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/MainActivity.java): Fast setup activity that guides operators to grant required permissions:
  1. *Display Over Other Apps* (`SYSTEM_ALERT_WINDOW`)
  2. *Manage All Files* (`MANAGE_EXTERNAL_STORAGE` for Android 11–15)
  3. *Accessibility Service* (for barcode scanner keystroke detection)
* [`IdleOverlayService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/IdleOverlayService.java): Runs as a lightweight foreground background service using `FLAG_WATCH_OUTSIDE_TOUCH`. It observes touch interactions without consuming touches or interfering with the underlying POS application.
* [`KioskAccessibilityService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/KioskAccessibilityService.java): Listens for hardware barcode scanner keystrokes (`KeyEvent.ACTION_DOWN`), dismisses any active ad screen, and allows keystrokes to pass transparently to the POS input.
* [`AdActivity.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/AdActivity.java): Fullscreen advertising activity powered by AndroidX Media3 ExoPlayer with `SurfaceView` direct hardware decoding, customized for Rockchip RK3576 hardware decoder reliability, featuring smooth Ken Burns image animations and shuffled media rotation.
* [`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java): Listens for `BOOT_COMPLETED` and launches the inactivity monitor on reboot.

### 2. Web POS Screensaver Module ([`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js) & [`pos-screensaver.css`](file:///d:/screen_saver/pos-screensaver.css))
* A plug-and-play JavaScript class (`PosScreensaver`) that can be dropped into any Web POS application (React, Vue, Next.js, or vanilla HTML/JS).
* Features Instagram-style segmented progress bars, Ken Burns zoom effects, touch ripple animations, and customizable callbacks (`onActivate`, `onDismiss`, `onSlideChange`).

### 3. Media Optimization Tools ([`tools/optimize_videos.py`](file:///d:/screen_saver/tools/optimize_videos.py))
* Automated batch converter powered by FFmpeg.
* Takes commercial videos in any format, orientation, or framerate from [`Input_Ads_Here/`](file:///d:/screen_saver/Input_Ads_Here/) and converts them into [`Optimized_Ads_For_Kiosk/`](file:///d:/screen_saver/Optimized_Ads_For_Kiosk/):
  - Resolution: Exactly **1080x1920 (Portrait 9:16)** with letterbox/pillarbox padding.
  - Video Codec: **H.264 (AVC) High Profile Level 4.1**.
  - Frame rate: 30 FPS.
  - Container flags: `+faststart` (instant playback without buffering).

---

## 🛠️ Installation & Setup Guide

### Option A: Native Android APK Deployment (Recommended for Kiosks)
1. **Install the APK**: Install `POS-Ad-Screensaver.apk` on your kiosk device.
2. **Grant Permissions**:
   - Open **POS Ad Screensaver**.
   - Tap **"Grant 'Display Over Other Apps'"** and switch it ON.
   - Tap **"Grant 'All Files Access'"** to allow loading ads from storage.
   - Tap **"Grant Barcode Scanner Access"** (Accessibility Service) to enable scanner wake-up.
3. **Start Service**: Tap **"Start Inactivity Monitor"**. The setup window closes and your POS app remains active.
4. **Load Ad Media**:
   - Create a folder named `Ads` on internal storage (`/sdcard/Ads/`).
   - Copy your promotional images (`.jpg`, `.png`, `.webp`) and videos (`.mp4`) into this folder.

### Option B: Integration into Web-Based POS
Include the script and stylesheet in your HTML:
```html
<link rel="stylesheet" href="pos-screensaver.css">
<script src="pos-screensaver.js"></script>
```
Initialize the screensaver in JavaScript:
```javascript
const screensaver = new PosScreensaver({
  idleTimeout: 60, // Inactivity timeout in seconds
  language: 'ar',  // 'ar' or 'en'
  slides: [
    { type: 'image', src: 'ads/poster1.jpg', duration: 8 },
    { type: 'video', src: 'ads/promo.mp4', duration: 15 }
  ]
});
```

---

<br>

---

# 🇸🇦 النسخة العربية

## 📌 نبذة عامة عن النظام
**نظام شاشة التوقف وعرض الإعلانات لأجهزة نقاط البيع والخدمة الذاتية (Self-Service POS Screensaver & Ad Display System)** هو حل برمجي متكامل ومخصص لأجهزة الكشك ونقاط البيع الذاتية، صُمم خصيصاً للشاشات الرأسية بدقة **1080x1920** التي تعمل بنظام **Android 14 / Android 15** (مثل جهاز **FY-3576SE** المزود بمعالج **Rockchip RK3576**)، بالإضافة إلى دعمه الكامل للأنظمة السحابية وأنظمة الويب (Web POS).

في بيئات البيع بالتجزئة والمطاعم، تمنع تطبيقات الكاشف ونقاط البيع إيقاف تشغيل الشاشة أو خمولها افتراضياً عبر خاصية (`FLAG_KEEP_SCREEN_ON`)، مما يؤدي إلى عدم تشغيل شاشة التوقف الافتراضية للنظام أبداً وبقاء الشاشة متوقفة على واجهة المحاسبة أو السلة القديمة. يحل هذا النظام هذه المشكلة جذرياً عبر **مراقبة فترات الخمول وعدم الاستخدام في الخلفية دون أي تدخل أو تعديل على برنامج الكاشير الأساسي**، ثم إطلاق شاشة إعلانية تفاعلية تعرض مقاطع الفيديو الدعائية والبوسترات العالية الدقة، مع العودة الفورية لشاشة البيع بمجرد لمس الشاشة أو مسح باركود.

---

## 🎯 المشكلة والحل التقني

| التحدي في أجهزة الكشك التقليدية | الحل المقدم في هذا النظام |
| :--- | :--- |
| برامج نقاط البيع تبقي الشاشة تعمل دائماً وتمنع شاشة توقف أندرويد | خدمة خلفية ذكية ([`IdleOverlayService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/IdleOverlayService.java)) تراقب خمول الشاشة على مستوى النظام كاملاً |
| بقاء الشاشة على واجهة السلة أو الفاتورة السابقة وتفويت فرصة الترويج | تشغيل إعلانات ترويجية مستمرة (فيديو وصور) بكامل الشاشة تُحمّل من مجلد `/sdcard/Ads/` |
| لمس العميل للشاشة لإيقاظها قد يضغط أزراراً بالخطأ خلف الإعلان (Ghost Clicks) | عزل لمسة الإيقاظ برمجياً، بحيث تقوم بإغلاق الإعلان فوراً دون الضغط على أزرار الدفع أو الإلغاء |
| قارئ الباركود لا يوقظ الشاشة في الأنظمة العادية | ربط خدمة إمكانية الوصول ([`KioskAccessibilityService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/KioskAccessibilityService.java)) لإيقاظ الشاشة وتمرير رقم المنتج للسلة فوراً |
| مقاطع الفيديو بدقات مختلفة تسبب تشنج معالج الرسوميات وتوقف الشاشة | أداة معالجة وتحسين آلية ([`optimize_videos.py`](file:///d:/screen_saver/tools/optimize_videos.py)) تحول الفيديو إلى دقة 1080x1920 وترميز H.264 متوافق بنسبة 100% |

---

## ✨ المميزات الرئيسية

* ⏱️ **مؤقت خمول قابل للضبط**: مضبوط افتراضياً على **60 ثانية** (دقيقة واحدة)، ويعيد ضبط نفسه تلقائياً عند أي لمسة على الشاشة أو مسح باركود.
* 🎬 **دعم الوسائط المتعددة (فيديو وصور)**: يدعم تشغيل البوسترات الإعلانية (`.jpg`, `.png`, `.webp`) ومقاطع الفيديو الترويجية (`.mp4`, `.webm`) في حلقة تكرار متواصلة مع خلط عشوائي ذكي (Shuffle).
* ⚡ **إيقاظ فوري بمجرد اللمس**: تختفي شاشة الإعلانات فورياً عند لمس الزبون للشاشة ليظهر تطبيق الكاشير جاهزاً فوراً للاستخدام دون أي تأخير.
* 🛡️ **حماية ضد الضغطات العرضية (Anti-Ghost Click)**: اللمسة الأولى المخصصة لإغلاق شاشة التوقف لا تُمرّر إلى الأزرار الموجودة خلفها (مثل زر "إلغاء" أو "دفع") لتفادي أي أخطاء أثناء المحاسبة.
* 📦 **الاستجابة الفورية لقارئ الباركود**: إذا قام العميل بمسح سلعة عبر قارئ الباركود المتصل بجهاز الكشك، تستيقظ الشاشة فوراً ويُرسل رمز الباركود إلى شاشة البيع لإضافته للسلة مباشرة.
* 🔄 **التشغيل التلقائي عند بدء تشغيل الجهاز**: تبدأ الخدمة تلقائياً في الخلفية عند إعادة تشغيل جهاز الكشك أو انقطاع الكهرباء عبر [`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java).
* 📱 **مخصص للشاشات الرأسية 1080x1920**: مصمم ليتناسب تماماً مع النسبة الرأسية (9:16) لأجهزة الخدمة الذاتية وشاشات اللمس الرأسية.
* 🌐 **دعم ثنائي اللغة (العربية والإنجليزية)**: واجهات وعروض ثنائية اللغة تدعم اللغة العربية بالكامل مع اتجاه النصوص (RTL) والأرقام المترجمة.
* 🔌 **معمارية مزدوجة (تطبيق أندرويد مستقل + مكتبة ويب)**: يمكن تثبيته كتطبيق أندرويد مستقل يعمل فوق أي تطبيق POS قائم، أو تضمينه كملف JavaScript داخل برامج الويب عبر [`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js).

---

## 🏗️ مكونات النظام والمعمارية البرمجية

### 1. تطبيق أندرويد الأصلي ([`android_screensaver_app/`](file:///d:/screen_saver/android_screensaver_app/))
* [`MainActivity.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/MainActivity.java): واجهة الإعداد السريع التي تمكن المشرف من منح الصلاحيات المطلوبة بنقرة واحدة:
  1. صـلاحية الظهور فوق التطبيقات الأخرى (`Display Over Other Apps`).
  2. صـلاحية الوصول الكامل لملفات الوسائط (`Manage All Files Access`).
  3. صـلاحية خدمة إمكانية الوصول لقارئ الباركود (`Accessibility Service`).
* [`IdleOverlayService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/IdleOverlayService.java): خدمة تعمل في الخلفية (`Foreground Service`) وتستخدم طبقة لمس شفافة مع خاصية `FLAG_WATCH_OUTSIDE_TOUCH` لرصد خمول الشاشة بدون التأثير إطلاقاً على أداء تطبيق الكاشير الأساسي.
* [`KioskAccessibilityService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/KioskAccessibilityService.java): تلتقط إشارات أجهزة قراءة الباركود ورمز الاستجابة السريعة (QR)، وتقوم بإغلاق الإعلانات فورياً وتمرير رقم الباركود إلى برنامج البيع.
* [`AdActivity.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/AdActivity.java): مشغل الإعلانات المدمج المبني على مشغل جوجل المتقدم `Media3 ExoPlayer` وتقنية `SurfaceView` المتوافقة مع معالجات Rockchip RK3576، مع دعم حركة Ken Burns للصور وتشغيل مقاطع الفيديو عالية الدقة.
* [`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java): يستمع لحدث إقلاع الجهاز (`BOOT_COMPLETED`) ويبدأ المراقبة تلقائياً.

### 2. مكتبة الويب لشاشات التوقف ([`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js) و [`pos-screensaver.css`](file:///d:/screen_saver/pos-screensaver.css))
* مكتبة جافاسكربت جاهزة للتضمين المباشر في أي نظام POS مبني بتقنيات الويب (React, Vue, Next.js, أو HTML/JS العادي).
* تحتوي على شريط تقدم زمني مقسم بنمط القصص (Stories)، وتأثيرات بصرية مميزة وحماية متطورة ضد الضغطات الخاطئة.

### 3. أداة تحسين ومعالجة الفيديو ([`tools/optimize_videos.py`](file:///d:/screen_saver/tools/optimize_videos.py))
* أداة بايثون تلقائية تعتمد على مكتبة `FFmpeg`.
* تأخذ الفيديوهات الإعلانية من مجلد [`Input_Ads_Here/`](file:///d:/screen_saver/Input_Ads_Here/) بأي صيغة أو أبعاد، وتنتج في مجلد [`Optimized_Ads_For_Kiosk/`](file:///d:/screen_saver/Optimized_Ads_For_Kiosk/) فيديوهات محسنة بالمواصفات التالية:
  - الأبعاد: **1080x1920 (رأسي 9:16)** بدقة مطابقة مع خلفية سوداء أنيقة لمنع تمدد الصورة.
  - الترميز: **H.264 (AVC) High Profile Level 4.1**.
  - معدل الإطارات: 30 إطاراً في الثانية (30 FPS).
  - علامة التشغيل الفوري `+faststart` لمنع أي تأخير في بدء الفيديو.

---

## 🛠️ دليل التثبيت والتشغيل

### الطريقة الأولى: التثبيت كتطبيق أندرويد مستقل (الموصى بها لأجهزة الكشك)
1. **تثبيت ملف APK**: قم بتثبيت حزمة `POS-Ad-Screensaver.apk` على جهاز الكشك.
2. **منح الصلاحيات**:
   - افتح تطبيق **POS Ad Screensaver**.
   - اضغط على **"Grant 'Display Over Other Apps'"** (الظهور فوق التطبيقات الأخرى) وقم بتفعيلها.
   - اضغط على **"Grant 'All Files Access'"** (الوصول إلى جميع الملفات) للسماح بقراءة الإعلانات من الذاكرة.
   - اضغط على **"Grant Barcode Scanner Access"** (خدمة إمكانية الوصول) لتفعيل استشعار الباركود.
3. **بدء المراقبة**: اضغط على زر **"Start Inactivity Monitor"**؛ ستغلق الواجهة ويظهر تطبيق البيع الخاص بك فوراً.
4. **إضافة الإعلانات**:
   - أنشئ مجلداً باسم `Ads` في الذاكرة الداخلية للجهاز (`/sdcard/Ads/`).
   - انسخ الصور ومقاطع الفيديو الترويجية داخل هذا المجلد.

### الطريقة الثانية: التضمين المباشر في أنظمة الويب (Web POS)
أضف ملف التنسيق وملف الجافاسكربت إلى صفحة `index.html`:
```html
<link rel="stylesheet" href="pos-screensaver.css">
<script src="pos-screensaver.js"></script>
```
قم بتشغيل شاشة التوقف في كود الجافاسكربت:
```javascript
const screensaver = new PosScreensaver({
  idleTimeout: 60, // مدة الخمول بالثواني (دقيقة واحدة)
  language: 'ar',  // اللغة: 'ar' للعربية أو 'en' للإنجليزية
  slides: [
    { type: 'image', src: 'ads/poster1.jpg', duration: 8 },
    { type: 'video', src: 'ads/promo.mp4', duration: 15 }
  ]
});
```

---

## 📱 متطلبات الأجهزة والتوافق

* **الجهاز المستهدف**: جهاز الخدمة الذاتية **FY-3576SE** (معالج Rockchip RK3576، شاشة لمس 1080x1920 رأسية).
* **نظام التشغيل**: أندرويد 10 إلى أندرويد 15 (Android 10 - 15).
* **متصفحات الويب المدعومة**: Chrome، WebView، Edge، وأي متصفح كشك يدعم معايير HTML5 و ES6.
* **قارئات الباركود المدعومة**: جميع قارئات الباركود اللاسلكية والسلكية (USB HID / Keyboard Wedge).
