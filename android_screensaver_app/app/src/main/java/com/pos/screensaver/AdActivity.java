package com.pos.screensaver;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.ui.PlayerView;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fullscreen Advertising Activity powered by Media3 ExoPlayer.
 * - SurfaceView direct hardware overlay decoding (Rockchip RK3576 compatible, zero GPU SIGSEGV).
 * - Automatic software decoder fallback for unsupported/corrupted codecs.
 * - Shuffled ad rotation: plays images and videos in random order.
 * - Instant dismissal on touch or barcode scan: returns directly to the underlying POS app.
 */
public class AdActivity extends Activity {

    private static final String TAG = "AdActivity";
    private static final int DEFAULT_IMAGE_DURATION_MS = 8000; // 8 seconds per image

    public static volatile boolean isActive = false;
    private static AdActivity currentInstance = null;

    public static void dismissIfActive() {
        final AdActivity activity = currentInstance;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        activity.dismissScreensaver();
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private ImageView imageView;
    private PlayerView playerView;
    private ExoPlayer player;
    private Bitmap currentBitmap = null;

    private ObjectAnimator scaleX;
    private ObjectAnimator scaleY;

    private final List<File> mediaFiles = new ArrayList<>();
    private int currentIndex = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable nextSlideRunnable;
    private Runnable videoWatchdogRunnable;
    private boolean isTransitioning = false;
    private boolean isDismissed = false;

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private long launchTimeMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentInstance = this;
        isActive = true;
        launchTimeMs = System.currentTimeMillis();

        // Install crash logger to diagnose any unexpected issues in /sdcard/Ads/
        installCrashLogger();

        // Keep screen awake while displaying advertisements
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Request fullscreen without title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.activity_ad);
        hideSystemUI();

        // Get screen resolution for memory-safe image decoding
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = Math.max(metrics.widthPixels, 720);
        screenHeight = Math.max(metrics.heightPixels, 1280);

        imageView = findViewById(R.id.adImageView);
        playerView = findViewById(R.id.adPlayerView);

        // Initialize high-performance Media3 ExoPlayer with Rockchip hardware & software fallback
        setupExoPlayer();

        // Setup touch listeners so ANY click immediately dismisses the ad screensaver
        View.OnTouchListener touchDismissListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (System.currentTimeMillis() - launchTimeMs < 600) {
                        return true;
                    }
                    dismissScreensaver();
                    return true;
                }
                return false;
            }
        };

        View rootLayout = findViewById(R.id.adRootLayout);
        if (rootLayout != null) rootLayout.setOnTouchListener(touchDismissListener);
        if (playerView != null) playerView.setOnTouchListener(touchDismissListener);
        if (imageView != null) imageView.setOnTouchListener(touchDismissListener);

        View touchBadge = findViewById(R.id.touchPromptBadge);
        if (touchBadge != null) {
            touchBadge.setOnTouchListener(touchDismissListener);
            startBadgePulseAnimation(touchBadge);
        }

        loadMediaPlaylist();
        startPlayback();
    }

    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logCrash(e);
                Log.e(TAG, "Uncaught exception in " + t.getName(), e);
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(t, e);
                }
            }
        });
    }

    private static void logError(String message) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "Ads");
            if (!dir.exists()) dir.mkdirs();
            File logFile = new File(dir, "screensaver_log.txt");
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            pw.println("[" + time + "] " + message);
            pw.flush();
            pw.close();
        } catch (Exception ignored) {}
    }

    private static void logCrash(Throwable throwable) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "Ads");
            if (!dir.exists()) dir.mkdirs();
            File logFile = new File(dir, "screensaver_crash_log.txt");
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            pw.println("========== CRASH AT " + time + " ==========");
            throwable.printStackTrace(pw);
            pw.println("=========================================");
            pw.flush();
            pw.close();
        } catch (Exception ignored) {}
    }

    private void startBadgePulseAnimation(View badge) {
        stopBadgePulseAnimation();
        try {
            scaleX = ObjectAnimator.ofFloat(badge, "scaleX", 1.0f, 1.05f, 1.0f);
            scaleY = ObjectAnimator.ofFloat(badge, "scaleY", 1.0f, 1.05f, 1.0f);
            scaleX.setDuration(1600);
            scaleY.setDuration(1600);
            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleX.start();
            scaleY.start();
        } catch (Exception ignored) {}
    }

    private void stopBadgePulseAnimation() {
        if (scaleX != null) {
            try { scaleX.cancel(); } catch (Exception ignored) {}
            scaleX = null;
        }
        if (scaleY != null) {
            try { scaleY.cancel(); } catch (Exception ignored) {}
            scaleY = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - launchTimeMs < 600) {
                return true;
            }
            dismissScreensaver();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupExoPlayer() {
        try {
            releasePlayer();

            // Strict RAM-safe buffer control: prevents high-bitrate videos from allocating excessive RAM
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                            1500,   // minBufferMs: 1.5s
                            4000,   // maxBufferMs: 4s (strict RAM limit)
                            1000,   // bufferForPlaybackMs: 1s
                            1500    // bufferForPlaybackAfterRebufferMs: 1.5s
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build();

            // Google Pure User-Space Software Decoders ONLY:
            // c2.android.avc.decoder, c2.android.hevc.decoder, OMX.google.*
            // These run 100% in user-space CPU threads and NEVER call the Rockchip kernel VPU driver (/dev/mpp_service).
            // Completely excludes Rockchip hardware decoders (c2.rk.*, OMX.rk.*) to prevent kernel panics & kiosk reboots!
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setEnableDecoderFallback(true)
                    .setMediaCodecSelector(new MediaCodecSelector() {
                        @Override
                        public List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder)
                                throws MediaCodecUtil.DecoderQueryException {
                            List<MediaCodecInfo> allDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
                            List<MediaCodecInfo> safeSoftwareDecoders = new ArrayList<>();
                            List<MediaCodecInfo> nonRkDecoders = new ArrayList<>();

                            for (MediaCodecInfo info : allDecoders) {
                                String name = info.name.toLowerCase(Locale.ROOT);
                                // CRITICAL: NEVER invoke Rockchip VPU hardware decoders (c2.rk.* or omx.rk.*)
                                // They panic the Linux kernel (/dev/mpp_service) on high-res phone videos!
                                if (name.contains(".rk.") || name.startsWith("c2.rk.") || name.startsWith("omx.rk.")) {
                                    continue;
                                }

                                if (name.startsWith("c2.android.") || name.startsWith("omx.google.") || !info.hardwareAccelerated) {
                                    safeSoftwareDecoders.add(info);
                                } else {
                                    nonRkDecoders.add(info);
                                }
                            }

                            // Use safe software decoders first and foremost
                            if (!safeSoftwareDecoders.isEmpty()) {
                                return safeSoftwareDecoders;
                            }

                            if (!nonRkDecoders.isEmpty()) {
                                return nonRkDecoders;
                            }

                            return allDecoders;
                        }
                    });

            player = new ExoPlayer.Builder(this, renderersFactory)
                    .setLoadControl(loadControl)
                    .build();
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            player.setVolume(0f); // Default muted for retail POS environment

            if (playerView != null) {
                playerView.setPlayer(player);
                playerView.setUseController(false);
            }

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (isDismissed) return;

                    switch (playbackState) {
                        case Player.STATE_ENDED:
                            Log.d(TAG, "Video completed naturally. Advancing to next ad.");
                            advanceToNextSlide();
                            break;

                        case Player.STATE_READY:
                            long duration = (player != null) ? player.getDuration() : 0;
                            Log.d(TAG, "Video is ready and playing. Duration: " + duration + "ms");
                            long timeoutMs = (duration > 1000 && duration < 300000) ? (duration + 2000) : 30000;
                            armVideoWatchdog(timeoutMs);
                            break;

                        case Player.STATE_IDLE:
                        case Player.STATE_BUFFERING:
                        default:
                            break;
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "ExoPlayer playback error: " + error.getMessage(), error);
                    logError("Video playback error (" + error.getErrorCodeName() + "): " + error.getMessage());
                    releasePlayer();

                    // Temporarily show banner instead of black screen
                    if (playerView != null) playerView.setVisibility(View.GONE);
                    if (imageView != null) {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageResource(R.drawable.sample_promo_banner);
                    }

                    // 2-second cooldown prevents infinite looping when video format exceeds device capabilities
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            advanceToNextSlide();
                        }
                    }, 2000);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize ExoPlayer", t);
            logError("Failed to initialize ExoPlayer: " + t.getMessage());
        }
    }

    private void armVideoWatchdog(long timeoutMs) {
        if (videoWatchdogRunnable != null) {
            handler.removeCallbacks(videoWatchdogRunnable);
        }
        videoWatchdogRunnable = new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "Video watchdog timeout reached (" + timeoutMs + "ms). Advancing to next ad.");
                advanceToNextSlide();
            }
        };
        handler.postDelayed(videoWatchdogRunnable, timeoutMs);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (getWindow() == null) return;
        View decorView = getWindow().getDecorView();
        if (decorView == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    /**
     * Search for images and videos across internal memory and app directories
     */
    private void loadMediaPlaylist() {
        mediaFiles.clear();

        List<File> searchDirs = new ArrayList<>();

        // 1. Primary: /sdcard/Ads/
        searchDirs.add(new File(Environment.getExternalStorageDirectory(), "Ads"));

        // 2. /sdcard/Movies/Ads/ and /sdcard/Pictures/Ads/
        searchDirs.add(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Ads"));
        searchDirs.add(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Ads"));

        // 3. App-specific storage: Android/data/com.pos.screensaver/files/Ads
        File appAdsDir = new File(getExternalFilesDir(null), "Ads");
        searchDirs.add(appAdsDir);

        for (File dir : searchDirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                scanDirectory(dir);
            }
        }

        // Sort files alphabetically first
        Collections.sort(mediaFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        Log.d(TAG, "Loaded " + mediaFiles.size() + " media files from internal memory.");
    }

    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (isSupportedMedia(name)) {
                    if (!mediaFiles.contains(file)) {
                        mediaFiles.add(file);
                    }
                }
            }
        }
    }

    private boolean isSupportedMedia(String filename) {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".webp")
                || filename.endsWith(".bmp")
                || filename.endsWith(".mp4") || filename.endsWith(".mkv")
                || filename.endsWith(".webm") || filename.endsWith(".mov")
                || filename.endsWith(".3gp") || filename.endsWith(".ts");
    }

    private boolean isVideoFile(String filename) {
        return filename.endsWith(".mp4") || filename.endsWith(".mkv")
                || filename.endsWith(".webm") || filename.endsWith(".mov")
                || filename.endsWith(".3gp") || filename.endsWith(".ts");
    }

    private void startPlayback() {
        if (mediaFiles.isEmpty()) {
            if (imageView != null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(R.drawable.sample_promo_banner);
            }
            if (playerView != null) playerView.setVisibility(View.GONE);
            Log.w(TAG, "No ads found in /sdcard/Ads/. Displaying sample banner.");
            return;
        }

        // Shuffle all ads randomly so it starts on a different ad each time
        Collections.shuffle(mediaFiles);
        currentIndex = 0;

        showSlide(currentIndex);
    }

    private synchronized void advanceToNextSlide() {
        if (isDismissed) return;
        if (isTransitioning) return;
        isTransitioning = true;

        handler.removeCallbacks(nextSlideRunnable);
        if (videoWatchdogRunnable != null) {
            handler.removeCallbacks(videoWatchdogRunnable);
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                isTransitioning = false;
                showSlide(currentIndex + 1);
            }
        });
    }

    private void showSlide(int index) {
        if (isDismissed || mediaFiles.isEmpty()) return;

        // When playlist finishes, re-shuffle for endless varied playback
        if (index >= mediaFiles.size()) {
            Collections.shuffle(mediaFiles);
            currentIndex = 0;
        } else {
            currentIndex = index;
        }

        File currentFile = mediaFiles.get(currentIndex);
        String name = currentFile.getName().toLowerCase();
        boolean isVideo = isVideoFile(name);

        Log.d(TAG, "Showing ad [" + (currentIndex + 1) + "/" + mediaFiles.size() + "]: " + currentFile.getName());

        // Cancel previous timers
        handler.removeCallbacks(nextSlideRunnable);
        if (videoWatchdogRunnable != null) {
            handler.removeCallbacks(videoWatchdogRunnable);
        }

        if (isVideo) {
            displayVideoAd(currentFile);
        } else {
            displayImageAd(currentFile);
        }
    }

    private void displayVideoAd(File videoFile) {
        try {
            if (imageView != null) imageView.setVisibility(View.GONE);
            if (playerView != null) playerView.setVisibility(View.VISIBLE);

            Log.d(TAG, "Playing video " + videoFile.getName());

            if (player == null) {
                setupExoPlayer();
            }

            if (player != null) {
                player.stop();
                player.clearMediaItems();

                MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile));
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();

                armVideoWatchdog(15000);
            } else {
                advanceToNextSlide();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error playing video with ExoPlayer: " + videoFile.getName(), t);
            logError("Error playing video " + videoFile.getName() + ": " + t.getMessage());
            releasePlayer();
            advanceToNextSlide();
        }
    }

    private void displayImageAd(File imageFile) {
        try {
            if (player != null) {
                try {
                    player.stop();
                    player.clearMediaItems();
                } catch (Exception ignored) {}
            }

            if (playerView != null) playerView.setVisibility(View.GONE);
            if (imageView != null) {
                imageView.setVisibility(View.VISIBLE);

                Bitmap bitmap = decodeSampledBitmapFromFile(imageFile.getAbsolutePath(), screenWidth, screenHeight);
                if (bitmap != null) {
                    recycleCurrentBitmap();
                    currentBitmap = bitmap;
                    imageView.setImageBitmap(currentBitmap);
                } else {
                    imageView.setImageResource(R.drawable.sample_promo_banner);
                }
            }

            nextSlideRunnable = new Runnable() {
                @Override
                public void run() {
                    advanceToNextSlide();
                }
            };
            handler.postDelayed(nextSlideRunnable, DEFAULT_IMAGE_DURATION_MS);
        } catch (Throwable t) {
            Log.e(TAG, "Error displaying image: " + imageFile.getName(), t);
            advanceToNextSlide();
        }
    }

    /**
     * Memory-safe image decoding that prevents OutOfMemoryError and UI stutter.
     */
    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            int inSampleSize = 1;
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            options.inSampleSize = Math.max(1, inSampleSize);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, options);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to decode image: " + path, t);
            return null;
        }
    }

    private void recycleCurrentBitmap() {
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            try {
                currentBitmap.recycle();
            } catch (Exception ignored) {}
            currentBitmap = null;
        }
    }

    /**
     * Touch received: Dismiss screensaver and return directly to the POS app.
     * Uses finishAndRemoveTask() to cleanly reveal whatever app was running underneath.
     */
    private synchronized void dismissScreensaver() {
        if (isDismissed) return;
        isDismissed = true;
        isActive = false;
        if (currentInstance == this) {
            currentInstance = null;
        }

        handler.removeCallbacksAndMessages(null);
        stopBadgePulseAnimation();

        releasePlayer();
        recycleCurrentBitmap();

        // Notify IdleOverlayService to reset the idle countdown
        IdleOverlayService.notifyActivity();

        // Finish activity and remove task so it returns to the POS app
        finishAndRemoveTask();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
                player.clearMediaItems();
                player.release();
            } catch (Exception ignored) {}
            player = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
        hideSystemUI();
        if (player != null && !isDismissed) {
            try {
                player.play();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            try {
                player.pause();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            try {
                player.pause();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDismissed = true;
        isActive = false;
        if (currentInstance == this) {
            currentInstance = null;
        }
        handler.removeCallbacksAndMessages(null);
        stopBadgePulseAnimation();
        releasePlayer();
        recycleCurrentBitmap();
    }
}
