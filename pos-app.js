/**
 * POS Self-Checkout Kiosk Application Logic
 * Integrates with PosScreensaver
 */

document.addEventListener('DOMContentLoaded', () => {
  // POS Catalog Items
  const PRODUCTS = [
    { id: '101', nameEn: 'Organic Fresh Bananas', nameAr: 'موز عضوي طازج', price: 1.49, icon: '🍌', barcode: '880101' },
    { id: '102', nameEn: 'Sourdough Artisan Bread', nameAr: 'خبز ريفي بالخميرة الطبيعية', price: 3.99, icon: '🍞', barcode: '880102' },
    { id: '103', nameEn: 'Fresh Croissants (2pk)', nameAr: 'كرواسون طازج بالزبدة', price: 4.50, icon: '🥐', barcode: '880103' },
    { id: '104', nameEn: 'Avocados Hass (Pack of 3)', nameAr: 'أفوكادو هاس (3 حبات)', price: 2.99, icon: '🥑', barcode: '880104' },
    { id: '105', nameEn: 'Full Cream Organic Milk', nameAr: 'حليب كامل الدسم عضوي', price: 2.25, icon: '🥛', barcode: '880105' },
    { id: '106', nameEn: 'Premium Espresso Roast', nameAr: 'قهوة اسبريسو فاخرة', price: 8.90, icon: '☕', barcode: '880106' },
  ];

  // App State
  let currentLang = 'ar'; // Default Arabic matching the device locale in photo
  let cart = [];
  let posScreensaverInstance = null;

  // I18N Strings for POS Interface
  const TRANSLATIONS = {
    en: {
      brandTitle: 'Express Checkout',
      brandSubtitle: 'Self-Service Terminal #04',
      scanBanner: 'Scan your barcode or tap items below',
      idleCountdown: 'Idle timeout in: ',
      catalogTitle: 'Featured Items',
      unit: '/ each',
      cartTitle: 'My Basket',
      clearCart: 'Clear All',
      totalLabel: 'Total Amount:',
      payNow: 'Pay & Print Receipt',
      emptyCart: 'Your basket is empty. Scan an item to start.',
      currency: '$',
      modalSuccessTitle: 'Payment Approved!',
      modalSuccessDesc: 'Thank you for shopping with us. Please take your receipt below.',
      btnDone: 'Start New Customer'
    },
    ar: {
      brandTitle: 'الدفع السريع الذاتي',
      brandSubtitle: 'محطة الخدمة الذاتية رقم ٠٤',
      scanBanner: 'مرر الباركود من الماسح أو اختر من الشاشة',
      idleCountdown: 'شاشة التوقف بعد: ',
      catalogTitle: 'المنتجات المختارة',
      unit: '/ للقطعة',
      cartTitle: 'سلة المشتريات',
      clearCart: 'إفراغ السلة',
      totalLabel: 'الإجمالي المطلوب:',
      payNow: 'الدفع وطباعة الفاتورة',
      emptyCart: 'السلة فارغة. مرر الباركود للبدء.',
      currency: 'ر.س',
      modalSuccessTitle: 'تم الدفع بنجاح!',
      modalSuccessDesc: 'شكراً لتسوقكم معنا. يرجى استلام الإيصال من فتحة الطابعة بالأسفل.',
      btnDone: 'عميل جديد'
    }
  };

  // Initialize PosScreensaver instance
  posScreensaverInstance = new PosScreensaver({
    idleTimeout: 60, // 60 seconds as requested by user
    language: currentLang,
    slides: [
      {
        type: 'image',
        src: 'assets/ads/flash_sale.jpg',
        title: 'FLASH SALE • عروض التوفير الكبرى',
        subtitle: 'خصم حتى 50% على آلاف المنتجات المختارة في كافة الأقسام',
        badge: '50% OFF',
        duration: 8
      },
      {
        type: 'image',
        src: 'assets/ads/bakery_deal.jpg',
        title: 'THE GOLDEN CRUST • المخبز الطازج',
        subtitle: 'مخبوزات ساخنة وقهوة مختصة محضرة يومياً بأعلى معايير الجودة',
        badge: 'FRESH DAILY',
        duration: 8
      },
      {
        type: 'video',
        src: 'assets/ads/promo_video.webm',
        title: 'SPECIAL PROMO • عروض حصرية بالفيديو',
        subtitle: 'استمتع بأحدث التخفيضات والعروض الخاصة ببرنامج الولاء',
        badge: 'PROMO VIDEO',
        duration: 10
      }
    ],
    onActivate: () => {
      console.log('[Screensaver] Activated due to inactivity.');
      // Auto-clear success modal if open so next customer sees fresh screen
      closeCheckoutModal();
    },
    onDismiss: () => {
      console.log('[Screensaver] Customer touched screen to wake up POS.');
    },
    onTick: (secondsLeft) => {
      const countdownEl = document.getElementById('pos-idle-seconds');
      if (countdownEl) {
        countdownEl.textContent = `${secondsLeft}s`;
      }
    }
  });

  // Listen for barcode scan events from screensaver
  posScreensaverInstance.on('barcodeScanned', (data) => {
    console.log('[Barcode Scanned]', data.barcode);
    const matched = PRODUCTS.find(p => p.barcode === data.barcode);
    if (matched) {
      addToCart(matched);
      playBeep();
    }
  });

  // Render Product Catalog
  function renderCatalog() {
    const grid = document.getElementById('pos-products-grid');
    if (!grid) return;
    grid.innerHTML = '';

    PRODUCTS.forEach(p => {
      const card = document.createElement('div');
      card.className = 'product-card';
      card.innerHTML = `
        <div class="product-img-box">${p.icon}</div>
        <div class="product-info">
          <h3>${currentLang === 'ar' ? p.nameAr : p.nameEn}</h3>
          <div class="product-meta">
            <span class="product-price">${TRANSLATIONS[currentLang].currency} ${p.price.toFixed(2)}</span>
            <span class="product-unit">${TRANSLATIONS[currentLang].unit}</span>
          </div>
        </div>
        <button class="btn-add-item" aria-label="Add to basket">+</button>
      `;

      card.addEventListener('click', () => {
        addToCart(p);
        playBeep();
      });

      grid.appendChild(card);
    });
  }

  // Cart operations
  function addToCart(product) {
    const existing = cart.find(item => item.id === product.id);
    if (existing) {
      existing.qty += 1;
    } else {
      cart.push({ ...product, qty: 1 });
    }
    updateCartUI();
  }

  function changeQty(productId, delta) {
    const index = cart.findIndex(i => i.id === productId);
    if (index !== -1) {
      cart[index].qty += delta;
      if (cart[index].qty <= 0) {
        cart.splice(index, 1);
      }
    }
    updateCartUI();
  }

  function clearCart() {
    cart = [];
    updateCartUI();
  }

  function updateCartUI() {
    const listEl = document.getElementById('pos-cart-items');
    const badgeEl = document.getElementById('pos-cart-count');
    const totalEl = document.getElementById('pos-cart-total');
    const payBtn = document.getElementById('pos-pay-btn');

    if (!listEl) return;

    if (cart.length === 0) {
      listEl.innerHTML = `<div style="text-align: center; color: var(--text-muted); font-size: 0.85rem; padding: 12px 0;">${TRANSLATIONS[currentLang].emptyCart}</div>`;
      if (badgeEl) badgeEl.textContent = '0';
      if (totalEl) totalEl.textContent = `${TRANSLATIONS[currentLang].currency} 0.00`;
      if (payBtn) payBtn.disabled = true;
      return;
    }

    let total = 0;
    let itemCount = 0;
    listEl.innerHTML = '';

    cart.forEach(item => {
      const itemTotal = item.price * item.qty;
      total += itemTotal;
      itemCount += item.qty;

      const row = document.createElement('div');
      row.className = 'cart-item-row';
      row.innerHTML = `
        <span class="cart-item-name">${item.icon} ${currentLang === 'ar' ? item.nameAr : item.nameEn}</span>
        <div class="cart-item-qty-control">
          <button class="qty-btn btn-minus" data-id="${item.id}">-</button>
          <span>${item.qty}</span>
          <button class="qty-btn btn-plus" data-id="${item.id}">+</button>
          <strong style="margin-left: 8px; color: var(--primary);">${TRANSLATIONS[currentLang].currency}${itemTotal.toFixed(2)}</strong>
        </div>
      `;

      row.querySelector('.btn-minus').addEventListener('click', (e) => {
        e.stopPropagation();
        changeQty(item.id, -1);
      });

      row.querySelector('.btn-plus').addEventListener('click', (e) => {
        e.stopPropagation();
        changeQty(item.id, 1);
      });

      listEl.appendChild(row);
    });

    if (badgeEl) badgeEl.textContent = itemCount;
    if (totalEl) totalEl.textContent = `${TRANSLATIONS[currentLang].currency} ${total.toFixed(2)}`;
    if (payBtn) payBtn.disabled = false;
  }

  // Audio Beep for POS Touch / Barcode feedback
  function playBeep() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(1400, ctx.currentTime);
      gain.gain.setValueAtTime(0.12, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.1);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.1);
    } catch (e) {}
  }

  // Checkout Modal
  function openCheckoutModal() {
    const modal = document.getElementById('pos-checkout-modal');
    if (modal) modal.classList.add('open');
  }

  function closeCheckoutModal() {
    const modal = document.getElementById('pos-checkout-modal');
    if (modal) modal.classList.remove('open');
  }

  // Language Switcher
  function setLanguage(lang) {
    currentLang = lang;
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';

    // Update POS UI text
    document.getElementById('pos-brand-title').textContent = TRANSLATIONS[lang].brandTitle;
    document.getElementById('pos-brand-subtitle').textContent = TRANSLATIONS[lang].brandSubtitle;
    document.getElementById('pos-scan-text').textContent = TRANSLATIONS[lang].scanBanner;
    document.getElementById('pos-idle-label').textContent = TRANSLATIONS[lang].idleCountdown;
    document.getElementById('pos-catalog-heading').textContent = TRANSLATIONS[lang].catalogTitle;
    document.getElementById('pos-cart-heading').textContent = TRANSLATIONS[lang].cartTitle;
    document.getElementById('pos-clear-cart-btn').textContent = TRANSLATIONS[lang].clearCart;
    document.getElementById('pos-total-label').textContent = TRANSLATIONS[lang].totalLabel;
    document.getElementById('pos-pay-btn-text').textContent = TRANSLATIONS[lang].payNow;
    document.getElementById('btn-lang-toggle').textContent = lang === 'ar' ? '🌐 English' : '🌐 العربية';

    // Update modal
    document.getElementById('modal-title').textContent = TRANSLATIONS[lang].modalSuccessTitle;
    document.getElementById('modal-desc').textContent = TRANSLATIONS[lang].modalSuccessDesc;
    document.getElementById('modal-btn-done').textContent = TRANSLATIONS[lang].btnDone;

    // Update screensaver language
    if (posScreensaverInstance) {
      posScreensaverInstance.setLanguage(lang);
    }

    renderCatalog();
    updateCartUI();
  }

  // UI Event Listeners
  document.getElementById('btn-lang-toggle')?.addEventListener('click', () => {
    setLanguage(currentLang === 'ar' ? 'en' : 'ar');
  });

  document.getElementById('pos-clear-cart-btn')?.addEventListener('click', clearCart);

  document.getElementById('pos-pay-btn')?.addEventListener('click', () => {
    openCheckoutModal();
  });

  document.getElementById('modal-btn-done')?.addEventListener('click', () => {
    clearCart();
    closeCheckoutModal();
    posScreensaverInstance.resetTimer();
  });

  // Diagnostics & Testing Panel Bindings
  const diagPanel = document.getElementById('kiosk-diag-panel');
  const diagHeader = document.getElementById('diag-panel-header');
  const timeoutSlider = document.getElementById('diag-timeout-slider');
  const timeoutVal = document.getElementById('diag-timeout-val');
  const forceBtn = document.getElementById('btn-force-screensaver');
  const customFileInput = document.getElementById('diag-custom-file');

  if (diagHeader && diagPanel) {
    diagHeader.addEventListener('click', () => {
      diagPanel.classList.toggle('collapsed');
    });
  }

  if (timeoutSlider && timeoutVal) {
    timeoutSlider.addEventListener('input', (e) => {
      const val = parseInt(e.target.value, 10);
      timeoutVal.textContent = `${val}s`;
      posScreensaverInstance.setIdleTimeout(val);
    });
  }

  if (forceBtn) {
    forceBtn.addEventListener('click', () => {
      posScreensaverInstance.activate();
    });
  }

  // Custom Ad Media Uploader
  if (customFileInput) {
    customFileInput.addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;

      const fileUrl = URL.createObjectURL(file);
      const isVideo = file.type.startsWith('video');

      posScreensaverInstance.addSlide({
        type: isVideo ? 'video' : 'image',
        src: fileUrl,
        title: file.name.substring(0, 24),
        subtitle: isVideo ? 'Video Ad' : 'Image Ad',
        badge: 'NEW PROMO',
        duration: isVideo ? 12 : 8
      });

      alert(`Uploaded "${file.name}" to Screensaver ad carousel!`);
    });
  }

  // Initial Render
  setLanguage(currentLang);
});
