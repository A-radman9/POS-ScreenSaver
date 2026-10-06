# 🖥️ Android POS Kiosk Ad Screensaver Guide
### Solution for: **FY-3576SE (Rockchip RK3576 • Android 14 • 1080x1920)**

---

## 🔍 Why Does the Screen Stay on the POS App?

When a customer finishes payment and leaves, the screen stays stuck on your existing POS app because:
* Most POS applications have `FLAG_KEEP_SCREEN_ON` enabled in their code so the cash register screen doesn't turn black.
* Because the POS app prevents sleep, **Android's default system screensaver never turns on**.

To display ads when no one is using the kiosk, you need an application that **passively monitors idle time across the operating system** and launches an advertisement overlay directly over your existing POS app.

---

## 💡 Solution 1: Use the Custom Android Screensaver App (Included)

We have created a native Android project specifically for your device:
📁 **[`android_screensaver_app/`](file:///d:/screen_saver/android_screensaver_app/)**

### How It Works:
1. **Keeps your existing POS app running**: You don't need to change or touch your existing POS software.
2. **Passive Touch Detection**: [`IdleOverlayService.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/IdleOverlayService.java) monitors when customers touch the screen using `FLAG_WATCH_OUTSIDE_TOUCH`. It doesn't block or slow down your POS in any way.
3. **Automatic 60s Trigger**: If 60 seconds pass without any touch (customer walked away), it launches [`AdActivity.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/AdActivity.java) on top of the POS app.
4. **Mixed Media Player**: Loops images (`.jpg`, `.png`, `.webp`) and video commercials (`.mp4`) loaded from `/sdcard/Ads/`.
5. **Touch to Dismiss**: When the next customer touches the screen, the ad disappears instantly, revealing your POS app ready for checkout!
6. **Auto-Boot**: Automatically starts whenever the kiosk powers on ([`BootReceiver.java`](file:///d:/screen_saver/android_screensaver_app/app/src/main/java/com/pos/screensaver/BootReceiver.java)).

### How to Install and Run It:
1. Open the project folder `d:\screen_saver\android_screensaver_app` in **Android Studio**.
2. Build the APK (`Build -> Build Bundle(s) / APK(s) -> Build APK(s)`).
3. Copy the generated `.apk` to a USB flash drive and plug it into the FY-3576SE kiosk.
4. Open **Files** on the kiosk and tap the APK to install.
5. Launch **POS Ad Screensaver** once:
   - Tap **"Grant 'Display Over Other Apps'"** (الظهور فوق التطبيقات الأخرى) and toggle it ON.
   - Tap **"Start Inactivity Monitor"**.
6. Create an `Ads` folder in internal storage (`/sdcard/Ads/`) and copy your promotional posters and video ads there.

---

## ⚡ Solution 2: Ready-Made Kiosk App (No Coding Required - 2 Minute Setup)

If you don't want to compile an Android project, you can use **Fully Single App Kiosk** (the worldwide standard for Android POS kiosks):

### Steps:
1. On your PC, download the APK from: **https://www.fully-kiosk.com/** (choose Android 14 / RK3576 compatible APK).
2. Put the APK on a USB flash drive and install it on your FY-3576SE kiosk.
3. Open Fully Kiosk Settings:
   - **Kiosk Mode** ➔ **Single App Kiosk Mode**: Select your installed POS application as the target app.
   - **Device Management** ➔ **Keep Screen On**: Turn ON.
   - **Screensaver**:
     - **Screensaver Inactivity Timer**: Set to `60` seconds.
     - **Screensaver Wallpaper / Media Folder**: Point to `/sdcard/Ads/` (containing your images & videos).
     - **Screensaver dismiss on touch**: Turn ON.
4. Now, your POS app will be locked in the foreground. Whenever no one touches it for 60 seconds, your ads will play full-screen. As soon as someone touches the screen, it returns straight to the POS!

---

## 📂 Organizing Your Ads on the Device

Whether you use Solution 1 or Solution 2, create a folder named `Ads` on the device:
```text
/sdcard/Ads/
├── promo_sale_50.jpg
├── fresh_bakery_deal.jpg
├── loyalty_video.mp4
└── supermarket_commercial.mp4
```

> [!TIP]
> **Resolution Tip**: For the best visual quality on your FY-3576SE kiosk, make sure your images and videos are created in **portrait 1080x1920 (9:16 aspect ratio)**.
