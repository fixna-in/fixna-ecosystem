const { chromium } = require('playwright');
const fs = require('fs');

async function captureScreenshots() {
  const browser = await chromium.launch();
  
  // Capture reference site
  const refPage = await browser.newPage();
  await refPage.setViewportSize({ width: 1440, height: 900 });
  await refPage.goto('https://www.aiwithhassan.com/', { waitUntil: 'networkidle', timeout: 60000 });
  await refPage.screenshot({ path: 'ref-desktop.png', fullPage: true });
  
  await refPage.setViewportSize({ width: 390, height: 844 });
  await refPage.goto('https://www.aiwithhassan.com/', { waitUntil: 'networkidle', timeout: 60000 });
  await refPage.screenshot({ path: 'ref-mobile.png', fullPage: true });
  
  await refPage.close();
  
  // Capture current fixna-web
  const fixnaPage = await browser.newPage();
  await fixnaPage.setViewportSize({ width: 1440, height: 900 });
  await fixnaPage.goto('http://localhost:3002', { waitUntil: 'networkidle', timeout: 60000 });
  await fixnaPage.screenshot({ path: 'fixna-desktop-before.png', fullPage: true });
  
  await fixnaPage.setViewportSize({ width: 390, height: 844 });
  await fixnaPage.goto('http://localhost:3002', { waitUntil: 'networkidle', timeout: 60000 });
  await fixnaPage.screenshot({ path: 'fixna-mobile-before.png', fullPage: true });
  
  // Also capture dark mode
  await fixnaPage.setViewportSize({ width: 1440, height: 900 });
  await fixnaPage.goto('http://localhost:3002', { waitUntil: 'networkidle', timeout: 60000 });
  await fixnaPage.evaluate(() => {
    document.documentElement.setAttribute('data-theme', 'dark');
    document.documentElement.style.colorScheme = 'dark';
  });
  await fixnaPage.waitForTimeout(500);
  await fixnaPage.screenshot({ path: 'fixna-desktop-dark-before.png', fullPage: true });
  
  await fixnaPage.setViewportSize({ width: 390, height: 844 });
  await fixnaPage.goto('http://localhost:3002', { waitUntil: 'networkidle', timeout: 60000 });
  await fixnaPage.evaluate(() => {
    document.documentElement.setAttribute('data-theme', 'dark');
    document.documentElement.style.colorScheme = 'dark';
  });
  await fixnaPage.waitForTimeout(500);
  await fixnaPage.screenshot({ path: 'fixna-mobile-dark-before.png', fullPage: true });
  
  await fixnaPage.close();
  await browser.close();
  
  console.log('Screenshots captured');
}

captureScreenshots().catch(console.error);