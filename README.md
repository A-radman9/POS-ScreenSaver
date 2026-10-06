# 🖥️ Self-Service POS Screensaver & Ad Display System
### نظام شاشة التوقف وعرض الإعلانات لأجهزة نقاط البيع والخدمة الذاتية
##### Designed for **FY-3576SE (Rockchip RK3576 • Android 14/15 • 1080x1920 Portrait Display)** & Web POS Terminals

---

🌐 **Language Navigation / اختيار اللغة:**  
👉 **[النسخة العربية (Arabic Version)](#-النسخة-العربية)**  
👉 **[English Version (النسخة الإنجليزية)](#-english-version)**

---

# 🇸🇦 النسخة العربية

## 📋 نبذة عن المشروع
يقدم هذا المشروع نظاماً متكاملاً لشاشات التوقف وعرض الإعلانات التجارية لأجهزة الكشك ونقاط البيع الذاتية (مثل جهاز **FY-3576SE** المزود بماسح باركود وطابعة إيصالات وشاشة لمس رأسية).

في أجهزة الكاشير ونقاط البيع، تبقي التطبيقات الشاشة قيد التشغيل الدائم (`FLAG_KEEP_SCREEN_ON`)، مما يمنع شاشة التوقف الافتراضية للنظام من العمل. يوفر هذا المشروع حلاً ذكياً لمراقبة الخمول وإطلاق الإعلانات فورياً عند مغادرة العميل دون الحاجة لتعديل برنامج الكاشير الأساسي.

### ✨ المميزات الرئيسية:
* ⏱️ **مؤقت خمول قابل للضبط**: مضبوط افتراضياً على **60 ثانية** (دقيقة واحدة)، ويعيد ضبط نفسه تلقائياً عند أي لمسة للشاشة، تحريك للماوس، أو مسح باركود.
* 🎬 **دعم الوسائط المتعددة (صور وفيديو)**: تشغيل مستمر للبوسترات الإعلانية (`.jpg`, `.png`, `.webp`) ومقاطع الفيديو الدعائية (`.mp4`, `.webm`) في حلقة تكرار مع خلط عشوائي ذكي (Shuffle).
* 👆 **إيقاظ فوري بلمسة واحدة**: تختفي شاشة الإعلانات فور لمس العميل للشاشة ليعود فوراً إلى شاشة المحاسبة دون أي تأخير.
* 🛡️ **حماية ضد الضغطات العرضية (Anti-Ghost Click)**: اللمسة الأولى المخصصة لإيقاظ الشاشة تُعزل بأمان، وتمنع النقر غير المقصود على الأزرار الموجودة في تطبيق البيع خلف الإعلان (مثل زر "إلغاء" أو "دفع").
* 📦 **الاستجابة الفورية لقارئ الباركود**: عند مسح أي منتج عبر قارئ الباركود أثناء تشغيل الإعلانات، تستيقظ الشاشة فوراً ويُرسل رمز الباركود إلى شاشة البيع لإضافته للسلة مباشرة.
* 🔄 **التشغيل التلقائي عند إقلاع الجهاز**: تشغيل تلقائي في الخلفية عند فتح الجهاز أو إعادة تشغيله عبر [`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java).
* 📱 **مخصص للشاشات الرأسية 1080x1920**: مصمم ليتناسب تماماً مع نسبة العرض إلى الارتفاع (9:16) الرأسية لأجهزة الخدمة الذاتية.
* 🌐 **دعم كامل للغة العربية والإنجليزية**: دعم أصلي للاتجاه من اليمين إلى اليسار (RTL) والأرقام العربية والإنجليزية.

---

## 🚀 طرق التشغيل والتثبيت

### 1️⃣ الحل الأول: تطبيق أندرويد الأصلي المستقل (موصى به لأجهزة الكشك)
إذا كان لديك تطبيق كاشير مثبت بالفعل على جهاز الأندرويد، استخدم تطبيقنا الأصلي الموجود في المجلد:
📁 **[`android_screensaver_app/`](file:///d:/screen_saver/android_screensaver_app/)** (أو ثبت حزمة `POS-Ad-Screensaver.apk` مباشرة).

**خطوات التثبيت السريع:**
1. ثبّت الحزمة `POS-Ad-Screensaver.apk` على جهاز الكشك.
2. افتح تطبيق **POS Ad Screensaver** مرة واحدة:
   - اضغط **"Grant 'Display Over Other Apps'"** وفعّل صـلاحية الظهور فوق التطبيقات الأخرى.
   - اضغط **"Grant 'All Files Access'"** لمنح صـلاحية قراءة ملفات الإعلانات من الذاكرة.
   - اضغط **"Grant Barcode Scanner Access"** لتفعيل خدمة إمكانية الوصول لقارئ الباركود.
3. اضغط **"Start Inactivity Monitor"** لتبدأ الخدمة بالعمل في الخلفية.
4. أنشئ مجلداً باسم `Ads` في الذاكرة الداخلية (`/sdcard/Ads/`) وضع فيه الصور والفيديوهات الإعلانية.

---

### 2️⃣ الحل الثاني: التضمين المباشر في أنظمة نقاط البيع عبر الويب (Web POS)
إذا كان نظام البيع لديك مبنياً بتقنيات الويب (HTML/JS أو React أو Vue أو Next.js):

#### الخطوة 1: نسخ الملفات
انسخ الملفين الأساسيين إلى مشروعك:
- [`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js)
- [`pos-screensaver.css`](file:///d:/screen_saver/pos-screensaver.css)

#### الخطوة 2: ربط الملفات في صفحة `index.html`
```html
<link rel="stylesheet" href="pos-screensaver.css">
<script src="pos-screensaver.js"></script>
```

#### الخطوة 3: تشغيل وضبط شاشة التوقف
```javascript
const screensaver = new PosScreensaver({
  idleTimeout: 60, // مدة الخمول بالثواني (دقيقة واحدة)
  language: 'ar',  // اللغة ('ar' للعربية أو 'en' للإنجليزية)
  slides: [
    {
      type: 'image',
      src: 'assets/ads/flash_sale.jpg',
      title: 'عروض التوفير الكبرى',
      subtitle: 'خصومات تصل إلى 50% على آلاف المنتجات',
      badge: 'خصم 50%',
      duration: 8 // المدة بالثواني
    },
    {
      type: 'video',
      src: 'assets/ads/promo_video.webm', // أو .mp4
      title: 'عروض خاصة',
      subtitle: 'عروض حصرية لعملاء الخدمة الذاتية',
      badge: 'شاهد ووفر',
      duration: 12
    }
  ],
  onActivate: () => {
    console.log('تم تفعيل شاشة التوقف');
  },
  onDismiss: () => {
    console.log('لمس العميل الشاشة - العودة لشاشة البيع');
  }
});
```

---

### 3️⃣ الحل الثالث: استخدام متصفح Fully Kiosk Browser
1. ثبّت تطبيق **Fully Kiosk Browser** المرفق `Fully-Single-App-Kiosk.apk`.
2. في إعدادات التطبيق:
   - **Start URL**: أدخل رابط نظام البيع الخاص بك.
   - **Kiosk Mode**: فعّل وضع الكشك لقفل الشاشة.
   - **Screensaver Inactivity Timer**: اضبطه على `60` ثانية.
   - **Screensaver Wallpaper/Folder**: حدد المجلد `/sdcard/Ads/`.

---

## 🎬 أداة تحسين وضغط الفيديوهات للكشك
لتفادي أي مشاكل في ترميز الفيديو على معالجات Rockchip RK3576، يتضمن المشروع أداة آلية:
📁 [`tools/optimize_videos.py`](file:///d:/screen_saver/tools/optimize_videos.py) (أو شغل الملف المباشر [`OPTIMIZE_VIDEOS_FOR_KIOSK.bat`](file:///d:/screen_saver/OPTIMIZE_VIDEOS_FOR_KIOSK.bat)):
* تضع مقاطع الفيديو في مجلد `Input_Ads_Here/`.
* تشغل الأداة لتقوم تلقائياً بتحويل وتوسيط الفيديوهات إلى مقاس **1080x1920 رأسي** وترميز **H.264 High 4.1** المتوافق تماماً.
* تجد الفيديوهات الجاهزة في مجلد `Optimized_Ads_For_Kiosk/`.

---

## ⚙️ جدول خيارات الضبط (API Reference)

| الخيار | النوع | القيمة الافتراضية | الوصف |
| :--- | :--- | :--- | :--- |
| `idleTimeout` | `number` | `60` | مدة الخمول بالثواني قبل تفعيل شاشة الإعلانات |
| `slides` | `Array` | `[]` | قائمة الصور ومقاطع الفيديو الترويجية |
| `language` | `string` | `'ar'` | لغة العرض (`'ar'` للعربية أو `'en'` للإنجليزية) |
| `showProgress` | `boolean` | `true` | إظهار شريط التقدم المقسم في أعلى الشاشة |
| `enableKenBurns` | `boolean` | `true` | تفعيل تأثير التقريب البصري السلس للصور |
| `enableGhostClickProtection` | `boolean` | `true` | منع لمسة الإيقاظ من الضغط على أزرار البيع خلفها |
| `onActivate` | `Function` | `null` | دالة تُنفذ عند تشغيل شاشة التوقف |
| `onDismiss` | `Function` | `null` | دالة تُنفذ عند لمس الشاشة والعودة للبيع |
| `onSlideChange` | `Function` | `null` | دالة تُنفذ عند انتقال الشريحة الإعلانية |

---

<br>

---

# 🇬🇧 English Version

## 📋 Overview
This project provides an enterprise-grade screensaver and advertisement display system for self-service POS kiosks (specifically tailored for **FY-3576SE** devices equipped with barcode scanners, receipt printers, and vertical touchscreen displays).

In point-of-sale environments, POS software prevents standard OS sleep (`FLAG_KEEP_SCREEN_ON`). As a result, default Android screensavers never engage, leaving displays sitting idle on checkout screens. This system solves that problem by **passively monitoring idle time across the operating system without modifying or interrupting the core POS software**, displaying promotional media in a loop, and waking instantly when touched or when a barcode is scanned.

### 🚀 Key Features:
* ⏱️ **Configurable Inactivity Timer**: Defaults to **60 seconds** (1 minute). Resets automatically on touch, mouse movement, or barcode scan.
* 🖼️ **Mixed Media Support**: Seamlessly displays promotional posters (images) and video commercials (`.mp4`, `.webm`) in a continuous loop with smart shuffling.
* 👆 **Instant Touch-to-Dismiss**: As soon as a customer touches the screen, the screensaver immediately fades out and reveals the active POS checkout application.
* 🛡️ **Anti-Ghost Click Protection**: The wake-up tap is consumed safely without accidentally triggering underlying POS buttons (such as "Cancel" or "Pay").
* 📦 **Barcode Scanner Wake-Up**: Intercepts barcode/QR scanner keystrokes via [`KioskAccessibilityService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/KioskAccessibilityService.java) to wake up and pass the barcode directly into the POS cart.
* 🔄 **Auto-Start on Boot**: Launches automatically in the background on device reboot via [`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java).
* 📱 **Tailored for 1080x1920 Portrait Display**: Perfectly fits the vertical 9:16 aspect ratio of the FY-3576SE kiosk screen.
* 🌐 **Bilingual (Arabic & English)**: Complete Arabic (RTL) and English support matching your device locale (`١٣:٤٣`).

---

## 🛠️ Solutions & Setup Methods

### 1️⃣ Solution 1: Native Android Screensaver App (Recommended for Kiosks)
If you already have a native or web POS application running on Android:
📁 **[`android_screensaver_app/`](file:///d:/screen_saver/android_screensaver_app/)** (or install the pre-built `POS-Ad-Screensaver.apk`).

**Quick Setup:**
1. Install `POS-Ad-Screensaver.apk` on the kiosk.
2. Launch **POS Ad Screensaver** once:
   - Tap **"Grant 'Display Over Other Apps'"** and enable it.
   - Tap **"Grant 'All Files Access'"** to allow media playback from storage.
   - Tap **"Grant Barcode Scanner Access"** (Accessibility Service) to enable scanner wake-up.
3. Tap **"Start Inactivity Monitor"**. The setup window closes and your POS app remains on top.
4. Create an `Ads` folder in internal storage (`/sdcard/Ads/`) and copy your promotional posters and videos there.

---

### 2️⃣ Solution 2: Integration into Web-Based POS
If your POS is built with web technologies (HTML/JS, React, Vue, Next.js):

#### Step 1: Copy Files
Copy the two core files into your project:
- [`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js)
- [`pos-screensaver.css`](file:///d:/screen_saver/pos-screensaver.css)

#### Step 2: Include CSS & JS in `index.html`
```html
<link rel="stylesheet" href="pos-screensaver.css">
<script src="pos-screensaver.js"></script>
```

#### Step 3: Initialize the Screensaver
```javascript
const screensaver = new PosScreensaver({
  idleTimeout: 60, // Inactivity timeout in seconds
  language: 'ar',  // 'ar' for Arabic or 'en' for English
  slides: [
    {
      type: 'image',
      src: 'assets/ads/flash_sale.jpg',
      title: 'FLASH SALE • عروض التوفير الكبرى',
      subtitle: 'خصومات تصل إلى 50% على آلاف المنتجات',
      badge: '50% OFF',
      duration: 8
    },
    {
      type: 'video',
      src: 'assets/ads/promo_video.webm', // or .mp4
      title: 'SPECIAL OFFERS • عروض الفيديو',
      subtitle: 'عروض حصرية لعملاء الخدمة الذاتية',
      badge: 'WATCH & SAVE',
      duration: 12
    }
  ],
  onActivate: () => {
    console.log('Screensaver activated');
  },
  onDismiss: () => {
    console.log('Customer touched screen - POS ready');
  }
});
```

---

### 3️⃣ Solution 3: Fully Kiosk Browser Setup
1. Install **Fully Single App Kiosk** APK (`Fully-Single-App-Kiosk.apk`).
2. In Fully Kiosk Settings:
   - **Start URL**: Point to your POS web application.
   - **Kiosk Mode**: Enable single-app kiosk lockdown.
   - **Keep Screen On**: Always ON.
   - **Screensaver Inactivity Timer**: `60` seconds.
   - **Screensaver Wallpaper/Folder**: `/sdcard/Ads/`.

---

## 🎬 Video Optimization Tool for Kiosks
To ensure videos play reliably on Rockchip RK3576 hardware decoders without stutter or GPU crashes:
📁 [`tools/optimize_videos.py`](file:///d:/screen_saver/tools/optimize_videos.py) (or run [`OPTIMIZE_VIDEOS_FOR_KIOSK.bat`](file:///d:/screen_saver/OPTIMIZE_VIDEOS_FOR_KIOSK.bat)):
* Place raw video files in `Input_Ads_Here/`.
* Run the batch script; it automatically resizes, letterboxes, and re-encodes videos to **1080x1920 Portrait H.264 High 4.1 at 30fps**.
* Find the kiosk-ready files in `Optimized_Ads_For_Kiosk/`.

---

## ⚙️ Configuration Options Reference

| Option | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `idleTimeout` | `number` | `60` | Inactivity time in seconds before screensaver activates |
| `slides` | `Array` | `[]` | List of images and videos to display |
| `language` | `string` | `'ar'` | Display language (`'ar'` or `'en'`) |
| `showProgress` | `boolean` | `true` | Show Instagram-style segmented progress bars at top |
| `enableKenBurns` | `boolean` | `true` | Subtle panning/zooming animation on image slides |
| `enableGhostClickProtection` | `boolean` | `true` | Prevents the wake-up tap from clicking underlying POS buttons |
| `onActivate` | `Function` | `null` | Callback executed when screensaver opens |
| `onDismiss` | `Function` | `null` | Callback executed when user touches to close |
| `onSlideChange` | `Function` | `null` | Callback executed when the ad slide changes |
| `onTick` | `Function` | `null` | Callback executed every second with remaining idle countdown |

### API Methods:
- `screensaver.activate()`: Manually trigger the screensaver immediately.
- `screensaver.dismiss()`: Manually hide the screensaver.
- `screensaver.resetTimer()`: Reset the idle countdown.
- `screensaver.setIdleTimeout(seconds)`: Change timeout duration dynamically.
- `screensaver.setLanguage('ar' | 'en')`: Switch between Arabic and English.
- `screensaver.addSlide({...})`: Add a new advertisement dynamically.
- `screensaver.removeSlide(index)`: Remove an advertisement.
