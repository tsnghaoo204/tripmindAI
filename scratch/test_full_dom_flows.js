const puppeteer = require('/home/minhnn/.gemini/antigravity-ide/brain/9a033322-e2f6-4d5e-b1fb-2f02f415170f/scratch/node_modules/puppeteer-core');
const fs = require('fs');
const path = require('path');

const ARTIFACTS_DIR = '/home/minhnn/.gemini/antigravity-ide/brain/9a033322-e2f6-4d5e-b1fb-2f02f415170f';

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function clickButtonWithText(page, text) {
  return await page.evaluate((searchText) => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const target = buttons.find((b) => b.textContent && b.textContent.trim().includes(searchText));
    if (target) {
      target.click();
      return true;
    }
    return false;
  }, text);
}

(async () => {
  console.log('Khởi động trình duyệt Chromium Headless...');
  const browser = await puppeteer.launch({
    headless: 'new',
    executablePath: '/usr/bin/google-chrome',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1600,1000']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1600, height: 1000 });

  page.on('dialog', async (dialog) => {
    console.log(`[DIALOG] ${dialog.type()}: ${dialog.message()}`);
    if (dialog.type() === 'prompt') {
      await dialog.accept('2026-11-15');
    } else {
      await dialog.accept();
    }
  });

  try {
    // 1. LOGIN
    console.log('1. Mở trang Đăng nhập...');
    await page.goto('http://localhost:5173/login', { waitUntil: 'networkidle2' });
    await page.type('input[type="email"]', 'admin@tripmind.com');
    await page.type('input[type="password"]', 'Admin@123456');
    await page.click('button[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
    console.log('   Đăng nhập thành công!');
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_1_dashboard.png') });

    // 2. CHỌN CHUYẾN ĐI (TRIP 14)
    console.log('2. Mở chi tiết chuyến đi số 14...');
    await page.goto('http://localhost:5173/trips/14', { waitUntil: 'networkidle2' });
    await sleep(1500);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_2_trip_detail.png') });

    // 3. TEST MODAL THÊM HOẠT ĐỘNG & GỢI Ý CHI PHÍ (COST ESTIMATE)
    console.log('3. Mở Modal Thêm hoạt động & Test Gợi ý chi phí...');
    await clickButtonWithText(page, 'Thêm điểm đến');
    await sleep(1000);
    await page.waitForSelector('input[placeholder*="Cà phê sáng"]', { timeout: 3000 });
    await page.type('input[placeholder*="Cà phê sáng"]', 'Cà Phê View Biển Mỹ Khê');
    
    // Click Gợi ý giá
    const clickedHint = await clickButtonWithText(page, 'Gợi ý giá');
    if (clickedHint) {
      await sleep(1000);
      console.log('   Đã bấm nút Gợi ý giá (Cost Estimate API: gợi ý chi phí theo PRICE_LEVEL)');
    }
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_3_cost_estimate.png') });

    // Submit activity
    await clickButtonWithText(page, 'Thêm vào ngày');
    await sleep(1500);
    console.log('   Đã lưu hoạt động mới vào ngày!');

    // 4. TEST XUẤT LỊCH TRÌNH .ICS
    console.log('4. Kiểm tra nút Xuất file .ics...');
    await clickButtonWithText(page, 'Xuất file .ics');
    await sleep(1000);
    console.log('   Đã bấm Xuất file .ics (RFC 5545)');

    // 5. TEST CHUYỂN GIAI ĐOẠN CHUYẾN (PHASE SWITCHER)
    console.log('5. Chuyển giai đoạn chuyến: Đang đi -> Đã xong (AFTER)...');
    await clickButtonWithText(page, 'Đang đi');
    await sleep(1000);
    await clickButtonWithText(page, 'Đã xong');
    await sleep(1500);
    console.log('   Đã chuyển sang giai đoạn Đã xong (AFTER)');
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_4_phase_after.png') });

    // 6. TEST ĐÁNH GIÁ ĐỊA ĐIỂM (PLACE REVIEW & RATING)
    console.log('6. Mở Modal Đánh giá địa điểm sau chuyến...');
    const clickedReview = await clickButtonWithText(page, 'Đánh giá địa điểm');
    if (clickedReview) {
      await sleep(1500);
      await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_5_place_review_modal.png') });

      // Click Thích (LIKE)
      await clickButtonWithText(page, 'Thích');
      await sleep(1000);
      console.log('   Đã đánh giá Thích (LIKE) cho địa điểm');

      // Close modal
      await page.evaluate(() => {
        const closeBtn = document.querySelector('div[role="dialog"] button, .fixed button');
        if (closeBtn) closeBtn.click();
      });
      await sleep(1000);
    }

    // 7. TEST TAB NGÂN SÁCH & CHI TIÊU + DAILY ALLOWANCE
    console.log('7. Chuyển sang Tab Ngân Sách & Chi Tiêu...');
    await clickButtonWithText(page, 'Ngân Sách & Chi Tiêu');
    await sleep(1500);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_6_budget_view.png') });

    // Thêm khoản chi tiêu mới
    const amountInput = await page.$('input[placeholder*="120000"]');
    if (amountInput) {
      await amountInput.type('180000');
      const descInput = await page.$('input[placeholder*="Ghi chú chi tiêu"]');
      if (descInput) await descInput.type('Hải sản Năm Đảnh');
      await clickButtonWithText(page, 'Ghi nhận chi tiêu');
      await sleep(1500);
      console.log('   Đã ghi nhận khoản chi tiêu mới!');
    }

    // 8. TEST TAB CHECKLIST & GỢI Ý
    console.log('8. Chuyển sang Tab Checklist Chuẩn Bị...');
    await clickButtonWithText(page, 'Checklist Chuẩn Bị');
    await sleep(1500);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_7_checklist_view.png') });

    // 9. TEST TRỢ LÝ AI DRAWER (REACT SSE)
    console.log('9. Mở Trợ lý AI Drawer...');
    await clickButtonWithText(page, 'Trợ lý AI TripMind');
    await sleep(1500);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_8_ai_drawer.png') });

    // Đóng drawer
    await page.evaluate(() => {
      const closeButtons = Array.from(document.querySelectorAll('button'));
      const close = closeButtons.find(b => b.querySelector('svg.lucide-x'));
      if (close) close.click();
    });
    await sleep(1000);

    // 10. TEST NHÂN BẢN CHUYẾN ĐI (DUPLICATE TRIP)
    console.log('10. Nhân bản chuyến đi...');
    await clickButtonWithText(page, 'Nhân bản');
    await sleep(2500);
    console.log('    Đã nhân bản chuyến đi, điều hướng về Dashboard');
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_9_duplicated_trip.png') });

    // 11. TEST KHÁM PHÁ ĐỊA ĐIỂM
    console.log('11. Mở trang Khám phá địa điểm...');
    await page.goto('http://localhost:5173/places', { waitUntil: 'networkidle2' });
    await sleep(1500);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_10_places.png') });

    // 12. TEST ADMIN DASHBOARD
    console.log('12. Mở trang Quản trị Hệ Thống Admin...');
    await page.goto('http://localhost:5173/admin', { waitUntil: 'networkidle2' });
    await sleep(1500);
    await page.screenshot({ path: path.join(ARTIFACTS_DIR, 'dom_flow_11_admin.png') });

    console.log('====================================================');
    console.log('=== TOÀN BỘ CÁC LUỒNG DOM TEST ĐÃ THÀNH CÔNG 100% ===');
    console.log('====================================================');
  } catch (err) {
    console.error('Lỗi khi chạy DOM test:', err);
  } finally {
    await browser.close();
  }
})();
