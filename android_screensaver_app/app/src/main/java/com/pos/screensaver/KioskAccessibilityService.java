package com.pos.screensaver;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility Service for POS Kiosks.
 * Listens for:
 * 1. Barcode/QR Scanner Keystrokes: When a customer scans a product, the scanner transmits
 *    keystrokes to Android. This service intercepts those events and resets the idle timer
 *    so the screensaver NEVER pops up while the customer is scanning!
 * 2. POS View Changes: Clicks, scrolls, and text updates in the shopping basket.
 */
public class KioskAccessibilityService extends AccessibilityService {

    private static final String TAG = "KioskAccessService";
    private static KioskAccessibilityService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Kiosk Accessibility Service connected: Barcode scanner monitoring ACTIVE.");
    }

    public static boolean isRunning() {
        return instance != null;
    }

    /**
     * Intercepts hardware barcode scanner keystrokes across the entire Android OS.
     * Crucial: Always returns false so the POS application receives the barcode numbers!
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            // Only dismiss for genuine barcode/input keys. Ignore media, volume, power, or unknown signals!
            if (isBarcodeOrUserInput(keyCode)) {
                AdActivity.dismissIfActive();
                IdleOverlayService.notifyActivity();
            }
        }
        // Return false so the barcode keystroke passes directly into the POS app unchanged
        return false;
    }

    private boolean isBarcodeOrUserInput(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_POWER
                || keyCode == KeyEvent.KEYCODE_SLEEP
                || keyCode == KeyEvent.KEYCODE_WAKEUP
                || keyCode == KeyEvent.KEYCODE_MUTE) {
            return false;
        }
        if (KeyEvent.isMediaSessionKey(keyCode)) {
            return false;
        }
        return true;
    }

    /**
     * Called on POS screen updates (clicks, scrolls, item added to basket)
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg != null && pkg.toString().equals(getPackageName())) {
            // Ignore events from our own ad screensaver app!
            return;
        }

        // If the screensaver is actively displaying, background POS events must not disturb it
        if (AdActivity.isActive) {
            return;
        }

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                || eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                || eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Active shopping or browsing detected on POS screen
            IdleOverlayService.notifyActivity();
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Kiosk Accessibility Service interrupted.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
