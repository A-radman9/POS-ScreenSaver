/**
 * POS Kiosk Screensaver Module
 * Robust, lightweight, touch-optimized screensaver with mixed media support (images & videos),
 * anti-ghost-click protection, and barcode scanner wake-up detection.
 * Designed for 1080x1920 portrait POS kiosks (Android 14 / Web POS / Chromium / Electron).
 */

(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    define([], factory);
  } else if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.PosScreensaver = factory();
  }
}(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  // Default localized strings
  const I18N = {
    en: {
      touchMain: 'Touch Screen to Start',
      touchSub: 'Tap anywhere to begin your checkout',
      unmute: 'Sound Off',
      muted: 'Sound On',
      kioskReady: 'Self-Checkout Kiosk'
    },
    ar: {
      touchMain: 'المس الشاشة للبدء',
      touchSub: 'اضغط في أي مكان لبدء الشراء والدفع',
      unmute: 'كتم الصوت',
      muted: 'تشغيل الصوت',
      kioskReady: 'جهاز الخدمة الذاتية'
    }
  };

  class PosScreensaver {
    /**
     * @param {Object} options
     * @param {number} [options.idleTimeout=60] - Idle timeout in seconds before screensaver activates
     * @param {Array<Object>} [options.slides] - Array of slides ({type: 'image'|'video', src: string, duration?: number, title?: string, subtitle?: string, badge?: string})
     * @param {string} [options.language='ar'] - Display language ('ar' or 'en')
     * @param {boolean} [options.showProgress=true] - Display top Instagram-style progress bars
     * @param {boolean} [options.enableKenBurns=true] - Subtle zoom-pan effect on images
     * @param {boolean} [options.autoStart=true] - Automatically start listening on initialization
     * @param {boolean} [options.enableGhostClickProtection=true] - Block accidental click on underlying POS elements upon wake-up
     * @param {Function} [options.onActivate] - Callback fired when screensaver opens
     * @param {Function} [options.onDismiss] - Callback fired when customer touches to dismiss
     * @param {Function} [options.onSlideChange] - Callback fired when slide switches
     * @param {Function} [options.onTick] - Callback fired every second with remaining idle seconds
     */
    constructor(options = {}) {
      this.options = Object.assign({
        idleTimeout: 60,
        slides: [],
        container: '.kiosk-viewport-wrapper', // attaches to kiosk container if present
        language: 'ar',
        showProgress: true,
        enableKenBurns: true,
        autoStart: true,
        enableGhostClickProtection: true,
        defaultSlideDuration: 7, // seconds
        onActivate: null,
        onDismiss: null,
        onSlideChange: null,
        onTick: null
      }, options);

      this.isActive = false;
      this.currentSlideIndex = 0;
      this.slides = [...(this.options.slides || [])];
      this.idleTimer = null;
      this.slideTimer = null;
      this.progressInterval = null;
      this.tickInterval = null;
      this.secondsRemaining = this.options.idleTimeout;
      this.eventListeners = {};
      this.lastActivityTime = Date.now();
      this.barcodeBuffer = '';
      this.barcodeTimer = null;
      this.isUnmuted = false;

      // DOM Elements
      this.overlayEl = null;
      this.slidesWrapperEl = null;
      this.progressContainerEl = null;
      this.touchPromptEl = null;
      this.soundToggleBtn = null;

      // Bound handlers
      this._handleUserActivity = this._handleUserActivity.bind(this);
      this._handleDismissTouch = this._handleDismissTouch.bind(this);
      this._handleKeyDown = this._handleKeyDown.bind(this);

      this._initDOM();

      if (this.options.autoStart) {
        this.start();
      }
    }

    /**
     * Build the screensaver DOM structure
     */
    _initDOM() {
      // Check if overlay already exists in DOM
      let overlay = document.getElementById('pos-screensaver-root');
      if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'pos-screensaver-root';
        overlay.className = 'pos-screensaver-overlay';
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');
        overlay.setAttribute('aria-label', 'Screensaver Advertisement Display');
        overlay.setAttribute('dir', this.options.language === 'ar' ? 'rtl' : 'ltr');

        overlay.innerHTML = `
          <div class="pos-ss-vignette"></div>
          <div class="pos-ss-kiosk-badge">
            <span class="pos-ss-live-dot"></span>
            <span id="pos-ss-kiosk-label">${I18N[this.options.language].kioskReady}</span>
          </div>
          <button type="button" class="pos-ss-sound-toggle" id="pos-ss-sound-btn" style="display:none;" aria-label="Toggle Sound">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27l4.73 4.73H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
            </svg>
            <span id="pos-ss-sound-text">${I18N[this.options.language].unmute}</span>
          </button>
          <div class="pos-ss-progress-bar-container" id="pos-ss-progress-bars"></div>
          <div class="pos-ss-slides-wrapper" id="pos-ss-slides-wrapper"></div>
          <div class="pos-ss-touch-prompt" id="pos-ss-touch-prompt">
            <div class="pos-ss-touch-icon">
              <svg viewBox="0 0 24 24">
                <path d="M9 11.24V7.5C9 6.12 10.12 5 11.5 5S14 6.12 14 7.5v3.74c1.21-.81 2-2.18 2-3.74C16 5.01 13.99 3 11.5 3S7 5.01 7 7.5c0 1.56.79 2.93 2 3.74zm9.84 4.63l-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6c0-.83-.67-1.5-1.5-1.5S10 6.67 10 7.5v10.74l-3.43-.72c-.08-.01-.15-.02-.24-.02-.31 0-.59.13-.79.33l-.79.8 4.94 4.94c.27.27.65.43 1.06.43h6.79c.75 0 1.33-.55 1.44-1.28l.75-5.27c.01-.06.01-.13.01-.2 0-.63-.39-1.18-.95-1.39z"/>
              </svg>
            </div>
            <div class="pos-ss-touch-text-wrap">
              <span class="pos-ss-touch-main" id="pos-ss-touch-main">${I18N[this.options.language].touchMain}</span>
              <span class="pos-ss-touch-sub" id="pos-ss-touch-sub">${I18N[this.options.language].touchSub}</span>
            </div>
          </div>
        `;
        const parentEl = (this.options.container && document.querySelector(this.options.container)) || document.body;
        parentEl.appendChild(overlay);
        if (parentEl !== document.body) {
          overlay.style.position = 'absolute';
        }
      }

      this.overlayEl = overlay;
      this.slidesWrapperEl = overlay.querySelector('#pos-ss-slides-wrapper');
      this.progressContainerEl = overlay.querySelector('#pos-ss-progress-bars');
      this.touchPromptEl = overlay.querySelector('#pos-ss-touch-prompt');
      this.soundToggleBtn = overlay.querySelector('#pos-ss-sound-btn');

      // Bind dismiss tap on entire overlay
      this.overlayEl.addEventListener('pointerdown', this._handleDismissTouch, { capture: true });

      // Sound toggle handler
      if (this.soundToggleBtn) {
        this.soundToggleBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          this._toggleSound();
        });
      }

      this._renderSlides();
    }

    /**
     * Render slide elements into DOM
     */
    _renderSlides() {
      if (!this.slidesWrapperEl) return;
      this.slidesWrapperEl.innerHTML = '';
      if (this.progressContainerEl) this.progressContainerEl.innerHTML = '';

      if (this.slides.length === 0) {
        // Fallback default promotional slide
        this.slides = [
          {
            type: 'image',
            src: 'assets/ads/flash_sale.jpg',
            title: 'Flash Sale • عروض حصرية',
            subtitle: 'خصومات تصل إلى 50% على تشكيلة واسعة من المنتجات',
            badge: '50% OFF',
            duration: 8
          },
          {
            type: 'image',
            src: 'assets/ads/bakery_deal.jpg',
            title: 'Fresh Bakery • مخبوزات طازجة',
            subtitle: 'قهوة مختصة ومخبوزات فرنسية طازجة يومياً',
            badge: 'DAILY FRESH',
            duration: 8
          }
        ];
      }

      this.slides.forEach((slide, index) => {
        // Progress item
        if (this.options.showProgress && this.progressContainerEl) {
          const progItem = document.createElement('div');
          progItem.className = 'pos-ss-progress-item';
          progItem.dataset.slideIndex = index;
          progItem.innerHTML = `<div class="pos-ss-progress-fill"></div>`;
          this.progressContainerEl.appendChild(progItem);
        }

        // Slide element
        const slideEl = document.createElement('div');
        slideEl.className = `pos-ss-slide ${index === 0 ? 'current' : ''} ${this.options.enableKenBurns && slide.type === 'image' ? 'ken-burns' : ''}`;
        slideEl.dataset.index = index;

        let mediaHtml = '';
        if (slide.type === 'video') {
          mediaHtml = `
            <video class="pos-ss-media" src="${slide.src}" preload="auto" playsinline muted loop></video>
          `;
        } else {
          mediaHtml = `
            <img class="pos-ss-media" src="${slide.src}" alt="${slide.title || 'Advertisement'}" loading="eager" />
          `;
        }

        const badgeHtml = slide.badge ? `<div class="pos-ss-badge">${slide.badge}</div>` : '';
        const titleHtml = slide.title ? `<h2 class="pos-ss-title">${slide.title}</h2>` : '';
        const subtitleHtml = slide.subtitle ? `<p class="pos-ss-subtitle">${slide.subtitle}</p>` : '';

        slideEl.innerHTML = `
          ${mediaHtml}
          ${(slide.title || slide.subtitle || slide.badge) ? `
            <div class="pos-ss-slide-content">
              ${badgeHtml}
              ${titleHtml}
              ${subtitleHtml}
            </div>
          ` : ''}
        `;

        this.slidesWrapperEl.appendChild(slideEl);
      });
    }

    /**
     * Start the screensaver idle detector
     */
    start() {
      this._attachActivityListeners();
      this.resetTimer();
    }

    /**
     * Stop the screensaver detector and clean up
     */
    stop() {
      this._detachActivityListeners();
      this._clearAllTimers();
      this.dismiss();
    }

    /**
     * Attach global activity listeners
     */
    _attachActivityListeners() {
      const events = ['pointerdown', 'touchstart', 'mousedown', 'mousemove', 'keydown', 'scroll', 'wheel'];
      events.forEach(evt => {
        window.addEventListener(evt, this._handleUserActivity, { passive: true });
      });
      window.addEventListener('keydown', this._handleKeyDown, { passive: false });
    }

    /**
     * Detach global activity listeners
     */
    _detachActivityListeners() {
      const events = ['pointerdown', 'touchstart', 'mousedown', 'mousemove', 'keydown', 'scroll', 'wheel'];
      events.forEach(evt => {
        window.removeEventListener(evt, this._handleUserActivity);
      });
      window.removeEventListener('keydown', this._handleKeyDown);
    }

    /**
     * User activity detected on POS screen
     */
    _handleUserActivity() {
      if (this.isActive) {
        // While active, touches are caught by overlay pointerdown
        return;
      }
      this.resetTimer();
    }

    /**
     * Special handler for Barcode Scanner and Keyboard input
     * Most barcode scanners simulate rapid keyboard typing followed by 'Enter'
     */
    _handleKeyDown(e) {
      if (this.isActive) {
        // If customer scans an item while screensaver is on, wake up!
        this.dismiss();
        return;
      }

      this.resetTimer();

      // Buffer characters to detect barcode scanner
      if (e.key === 'Enter') {
        if (this.barcodeBuffer.length >= 3) {
          this._emit('barcodeScanned', { barcode: this.barcodeBuffer });
        }
        this.barcodeBuffer = '';
      } else if (e.key && e.key.length === 1) {
        this.barcodeBuffer += e.key;
        clearTimeout(this.barcodeTimer);
        this.barcodeTimer = setTimeout(() => {
          this.barcodeBuffer = '';
        }, 150);
      }
    }

    /**
     * Reset the idle countdown
     */
    resetTimer() {
      this.lastActivityTime = Date.now();
      this.secondsRemaining = this.options.idleTimeout;
      clearTimeout(this.idleTimer);
      clearInterval(this.tickInterval);

      // Start countdown
      this.idleTimer = setTimeout(() => {
        this.activate();
      }, this.options.idleTimeout * 1000);

      // Optional tick interval for debug or countdown display
      this.tickInterval = setInterval(() => {
        if (this.isActive) return;
        this.secondsRemaining = Math.max(0, Math.ceil(this.options.idleTimeout - ((Date.now() - this.lastActivityTime) / 1000)));
        if (typeof this.options.onTick === 'function') {
          this.options.onTick(this.secondsRemaining);
        }
      }, 1000);
    }

    /**
     * Activate the Screensaver
     */
    activate() {
      if (this.isActive) return;
      this.isActive = true;
      clearTimeout(this.idleTimer);
      clearInterval(this.tickInterval);

      this.overlayEl.classList.add('active');
      this.currentSlideIndex = 0;
      this._goToSlide(0);

      if (typeof this.options.onActivate === 'function') {
        this.options.onActivate();
      }
      this._emit('activate');
    }

    /**
     * Dismiss screensaver when customer touches screen
     */
    _handleDismissTouch(e) {
      if (!this.isActive) return;

      // Anti-ghost-click: Stop propagation so the customer's initial tap doesn't
      // accidentally click checkout buttons behind the screensaver overlay
      if (this.options.enableGhostClickProtection) {
        e.preventDefault();
        e.stopPropagation();
      }

      // Visual touch ripple
      this._createTouchRipple(e.clientX, e.clientY);

      // Dismiss after short ripple effect
      setTimeout(() => {
        this.dismiss();
      }, 100);
    }

    /**
     * Dismiss the screensaver
     */
    dismiss() {
      if (!this.isActive) return;
      this.isActive = false;

      // Pause all media in slides to save memory and CPU
      this._pauseAllMedia();

      this.overlayEl.classList.remove('active');
      this._clearSlideTimer();

      // Reset progress bars
      if (this.progressContainerEl) {
        const fills = this.progressContainerEl.querySelectorAll('.pos-ss-progress-fill');
        fills.forEach(fill => { fill.style.width = '0%'; });
      }

      // Re-arm idle timer
      this.resetTimer();

      if (typeof this.options.onDismiss === 'function') {
        this.options.onDismiss();
      }
      this._emit('dismiss');
    }

    /**
     * Transition to a specific slide
     */
    _goToSlide(index) {
      if (!this.slides || this.slides.length === 0) return;
      this._clearSlideTimer();
      this._pauseAllMedia();

      this.currentSlideIndex = (index + this.slides.length) % this.slides.length;
      const currentSlideData = this.slides[this.currentSlideIndex];

      const allSlideEls = this.slidesWrapperEl.querySelectorAll('.pos-ss-slide');
      allSlideEls.forEach((slideEl, idx) => {
        slideEl.classList.remove('current', 'prev');
        if (idx === this.currentSlideIndex) {
          slideEl.classList.add('current');
        }
      });

      // Update sound button visibility if video
      if (this.soundToggleBtn) {
        if (currentSlideData.type === 'video') {
          this.soundToggleBtn.style.display = 'flex';
        } else {
          this.soundToggleBtn.style.display = 'none';
        }
      }

      // Play video if current slide is video
      const activeSlideEl = allSlideEls[this.currentSlideIndex];
      let durationSeconds = currentSlideData.duration || this.options.defaultSlideDuration;

      if (currentSlideData.type === 'video') {
        const videoEl = activeSlideEl.querySelector('video');
        if (videoEl) {
          videoEl.currentTime = 0;
          videoEl.muted = !this.isUnmuted;
          videoEl.play().catch(() => {
            // Autoplay with muted fallback if blocked by browser policy
            videoEl.muted = true;
            videoEl.play().catch(() => {});
          });

          // Use video duration if available and reasonable
          if (videoEl.duration && !isNaN(videoEl.duration) && videoEl.duration > 2) {
            durationSeconds = Math.min(videoEl.duration, 30);
          }
        }
      }

      // Animate progress bar
      this._animateProgressBar(this.currentSlideIndex, durationSeconds);

      // Schedule next slide
      this.slideTimer = setTimeout(() => {
        this._goToSlide(this.currentSlideIndex + 1);
      }, durationSeconds * 1000);

      if (typeof this.options.onSlideChange === 'function') {
        this.options.onSlideChange(this.currentSlideIndex, currentSlideData);
      }
      this._emit('slideChange', { index: this.currentSlideIndex, slide: currentSlideData });
    }

    /**
     * Animate segmented progress bar
     */
    _animateProgressBar(activeIndex, durationSeconds) {
      if (!this.progressContainerEl) return;
      clearInterval(this.progressInterval);

      const items = this.progressContainerEl.querySelectorAll('.pos-ss-progress-item');
      items.forEach((item, idx) => {
        const fill = item.querySelector('.pos-ss-progress-fill');
        if (idx < activeIndex) {
          fill.style.width = '100%';
        } else if (idx > activeIndex) {
          fill.style.width = '0%';
        } else {
          fill.style.width = '0%';
        }
      });

      const currentFill = items[activeIndex]?.querySelector('.pos-ss-progress-fill');
      if (!currentFill) return;

      const startTime = Date.now();
      const totalMs = durationSeconds * 1000;

      this.progressInterval = setInterval(() => {
        if (!this.isActive) {
          clearInterval(this.progressInterval);
          return;
        }
        const elapsed = Date.now() - startTime;
        const percent = Math.min(100, (elapsed / totalMs) * 100);
        currentFill.style.width = `${percent}%`;

        if (percent >= 100) {
          clearInterval(this.progressInterval);
        }
      }, 50);
    }

    /**
     * Pause all playing videos
     */
    _pauseAllMedia() {
      if (!this.slidesWrapperEl) return;
      const videos = this.slidesWrapperEl.querySelectorAll('video');
      videos.forEach(v => {
        try {
          v.pause();
        } catch (e) {}
      });
    }

    /**
     * Toggle video sound
     */
    _toggleSound() {
      this.isUnmuted = !this.isUnmuted;
      const currentSlideData = this.slides[this.currentSlideIndex];
      if (currentSlideData && currentSlideData.type === 'video') {
        const activeSlideEl = this.slidesWrapperEl.querySelector(`.pos-ss-slide[data-index="${this.currentSlideIndex}"]`);
        const videoEl = activeSlideEl?.querySelector('video');
        if (videoEl) {
          videoEl.muted = !this.isUnmuted;
        }
      }

      const textEl = document.getElementById('pos-ss-sound-text');
      if (textEl) {
        textEl.textContent = this.isUnmuted ? I18N[this.options.language].unmute : I18N[this.options.language].muted;
      }
    }

    /**
     * Create modern glowing ripple animation on tap
     */
    _createTouchRipple(x, y) {
      if (x === undefined || y === undefined) return;
      const ripple = document.createElement('div');
      ripple.className = 'pos-ss-ripple';
      const size = 180;
      ripple.style.width = `${size}px`;
      ripple.style.height = `${size}px`;
      ripple.style.left = `${x - size / 2}px`;
      ripple.style.top = `${y - size / 2}px`;
      this.overlayEl.appendChild(ripple);

      setTimeout(() => {
        ripple.remove();
      }, 650);
    }

    /**
     * Clear all slide timers
     */
    _clearSlideTimer() {
      clearTimeout(this.slideTimer);
      clearInterval(this.progressInterval);
    }

    /**
     * Clear all active timers
     */
    _clearAllTimers() {
      clearTimeout(this.idleTimer);
      clearInterval(this.tickInterval);
      this._clearSlideTimer();
      clearTimeout(this.barcodeTimer);
    }

    /**
     * Set idle timeout dynamically (in seconds)
     */
    setIdleTimeout(seconds) {
      this.options.idleTimeout = Math.max(3, parseInt(seconds, 10) || 60);
      this.resetTimer();
    }

    /**
     * Change display language
     */
    setLanguage(lang) {
      if (!I18N[lang]) return;
      this.options.language = lang;
      this.overlayEl.setAttribute('dir', lang === 'ar' ? 'rtl' : 'ltr');

      const mainEl = document.getElementById('pos-ss-touch-main');
      const subEl = document.getElementById('pos-ss-touch-sub');
      const kioskEl = document.getElementById('pos-ss-kiosk-label');
      const soundText = document.getElementById('pos-ss-sound-text');

      if (mainEl) mainEl.textContent = I18N[lang].touchMain;
      if (subEl) subEl.textContent = I18N[lang].touchSub;
      if (kioskEl) kioskEl.textContent = I18N[lang].kioskReady;
      if (soundText) soundText.textContent = this.isUnmuted ? I18N[lang].unmute : I18N[lang].muted;
    }

    /**
     * Add a new slide dynamically
     */
    addSlide(slide) {
      this.slides.push(slide);
      this._renderSlides();
    }

    /**
     * Remove slide by index
     */
    removeSlide(index) {
      if (index >= 0 && index < this.slides.length) {
        this.slides.splice(index, 1);
        this._renderSlides();
      }
    }

    /**
     * Event listener subscription
     */
    on(event, handler) {
      if (!this.eventListeners[event]) {
        this.eventListeners[event] = [];
      }
      this.eventListeners[event].push(handler);
      return this;
    }

    _emit(event, data) {
      const handlers = this.eventListeners[event];
      if (handlers) {
        handlers.forEach(fn => fn(data));
      }
    }
  }

  return PosScreensaver;
}));
