# 🖥️ Self-Service POS Screensaver & Ad Display System
### Specially Designed for **FY-3576SE (Rockchip RK3576, Android 14, 1080x1920 Portrait Display)**

> 📖 **Bilingual App Overview / الوصف الكامل للنظام باللغتين العربية والإنجليزية**: See [APP_DESCRIPTION.md](file:///d:/screen_saver/APP_DESCRIPTION.md)

---

## 📋 Overview
This project provides a screensaver and advertisement display system for self-service POS kiosks (like your **FY-3576SE** device with barcode scanner and receipt printer).

### Key Features:
* ⏱️ **Configurable Inactivity Timer**: Defaults to **60 seconds** (1 minute). Resets automatically whenever a customer touches the screen, moves a mouse, or scans a barcode.
* 🖼️ **Mixed Media Support**: Seamlessly displays high-resolution promotional posters (images) and promotional video commercials (`.mp4`, `.webm`) in a continuous loop.
* 👆 **Instant Touch-to-Dismiss**: As soon as a customer touches the screen, the screensaver immediately fades out and reveals the active POS checkout application.
* 🛡️ **Anti-Ghost Click Protection**: When a customer taps the screen to dismiss the screensaver, the touch event wakes the screen **without** accidentally clicking buttons behind it (e.g., preventing accidental clicks on "Cancel" or "Pay").
* 📦 **Barcode Scanner Wake-Up**: If a customer scans an item with the built-in barcode scanner while the screensaver is active, the kiosk wakes up and immediately adds the scanned item to their basket.
* 🌐 **Bilingual (Arabic & English)**: Complete Arabic (RTL) and English support matching your device locale (`١٣:٤٣`).
* 📱 **Tailored for 1080x1920 Portrait Display**: Perfectly fits the vertical 9:16 aspect ratio of the FY-3576SE kiosk screen.

---

## 🚀 Live Demo & Testing

A local HTTP server is already running on your machine:
```text
http://localhost:8080/index.html
```

### What You Can Test in the Demo:
1. **Interactive POS**: Add fresh grocery items to the basket, adjust quantities, view totals, and press "Pay & Print Receipt".
2. **Screensaver Activation**:
   - Wait for the countdown (60s default) without touching the screen.
   - Or open the **"Kiosk & Screensaver Controls"** panel on the bottom-right and click **"▶ Activate Screensaver Now"** or drag the slider to **5s** for instant testing.
3. **Touch to Dismiss**: Tap anywhere on the screensaver to watch the touch ripple effect and return smoothly to the POS.
4. **Custom Ad Media**: Upload your own image or video in the control panel to see it immediately included in the ad loop!

---

## 🛠️ How to Integrate into Your Existing Web POS

If you already have a Web POS application (HTML/JS, React, Vue, Next.js, etc.), you can drop this screensaver into your project in 3 simple steps:

### Step 1: Copy Files
Copy the two core files into your project:
- [`pos-screensaver.js`](file:///d:/screen_saver/pos-screensaver.js)
- [`pos-screensaver.css`](file:///d:/screen_saver/pos-screensaver.css)

### Step 2: Include the CSS and JS
In your `index.html`:
```html
<link rel="stylesheet" href="pos-screensaver.css">
<script src="pos-screensaver.js"></script>
```

### Step 3: Initialize the Screensaver
In your main JavaScript file:
```javascript
const screensaver = new PosScreensaver({
  idleTimeout: 60, // Inactivity timeout in seconds (1 minute)
  language: 'ar',  // 'ar' for Arabic or 'en' for English
  slides: [
    {
      type: 'image',
      src: 'assets/ads/flash_sale.jpg',
      title: 'FLASH SALE • عروض التوفير الكبرى',
      subtitle: 'خصومات تصل إلى 50% على آلاف المنتجات',
      badge: '50% OFF',
      duration: 8 // Duration in seconds
    },
    {
      type: 'image',
      src: 'assets/ads/bakery_deal.jpg',
      title: 'THE GOLDEN CRUST • المخبز الطازج',
      subtitle: 'مخبوزات طازجة وقهوة يومية',
      badge: 'FRESH DAILY',
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
    console.log('Screensaver started - pausing any active POS timers');
  },
  onDismiss: () => {
    console.log('Customer touched screen - ready for shopping');
  }
});
```

---

## 📱 Setting Up on the Android 14 Kiosk (FY-3576SE)

Your device (`FY-3576SE`) runs **Android 14** on a **Rockchip RK3576** processor. Here are the best ways to deploy this web app on the kiosk:

### Method A: Fully Kiosk Browser (Recommended for Android Kiosks)
1. Install **Fully Kiosk Browser** APK on your Android 14 kiosk.
2. In Fully Kiosk Settings:
   - **Start URL**: Set to your POS local or web URL (e.g., `http://localhost:8080` or `https://your-pos-url.com`).
   - **Kiosk Mode**: Enable to lock down the status bar and prevent customers from exiting.
   - **Keep Screen On**: Enable (`Always`).
   - **Webview Settings**: Hardware Acceleration `ON`.
3. The screensaver will run inside your web app automatically!

### Method B: Chrome Kiosk / Home Screen Shortcut
1. Open Google Chrome on the Android kiosk.
2. Navigate to your POS URL.
3. Tap Chrome Menu (⋮) ➔ **"Add to Home Screen"** or **"Install app"**.
4. Launch from the home screen icon — it runs in full-screen standalone mode without URL bars.

### Method C: Custom Android WebView App
If you are packaging the web app into an Android APK, configure your `MainActivity.java`:
```java
WebView webView = findViewById(R.id.webview);
webView.getSettings().setJavaScriptEnabled(true);
webView.getSettings().setDomStorageEnabled(true);
webView.getSettings().setMediaPlaybackRequiresUserGesture(false); // Allows video ad autoplay
webView.loadUrl("file:///android_asset/pos/index.html"); // or remote URL
```

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
| `onActivate` | `Function`| `null` | Callback executed when screensaver opens |
| `onDismiss` | `Function` | `null` | Callback executed when user touches to close |
| `onSlideChange` | `Function` | `null` | Callback executed when the ad slide changes |
| `onTick` | `Function` | `null` | Callback executed every second with remaining idle seconds |

### API Methods:
- `screensaver.activate()`: Manually trigger the screensaver immediately.
- `screensaver.dismiss()`: Manually hide the screensaver.
- `screensaver.resetTimer()`: Reset the idle countdown.
- `screensaver.setIdleTimeout(seconds)`: Change timeout duration dynamically.
- `screensaver.setLanguage('ar' | 'en')`: Switch between Arabic and English.
- `screensaver.addSlide({...})`: Add a new advertisement dynamically.
- `screensaver.removeSlide(index)`: Remove an advertisement.
