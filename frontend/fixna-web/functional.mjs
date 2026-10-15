// Functional checks: mobile nav, form focus ring, no-flash initial load.
import { chromium } from "playwright";
const browser = await chromium.launch({ channel: "chrome", headless: true });

// 1) No incorrect-theme flash on initial load (no stored preference) + OS default.
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  // Use the browser's own (default light) color-scheme preference; do not set localStorage.
  const page = await ctx.newPage();
  await page.goto("http://localhost:3002/", { waitUntil: "load" });
  await page.waitForTimeout(300);
  const d = await page.evaluate(() => document.documentElement.getAttribute("data-theme"));
  console.log("INITIAL LOAD data-theme (no pref, OS light):", d);
  await ctx.close();
}

// 2) Mobile menu toggles; 390x844 viewport.
{
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
  const page = await ctx.newPage();
  await page.goto("http://localhost:3002/", { waitUntil: "load" });
  await page.waitForTimeout(200);
  const menuVisibleBefore = await page.$eval(".mobile-menu-button", (el) => el.checkVisibility?.());
  const expandedBefore = await page.getAttribute(".mobile-menu-button", "aria-expanded");
  await page.click(".mobile-menu-button");
  await page.waitForTimeout(200);
  const expandedAfter = await page.getAttribute(".mobile-menu-button", "aria-expanded");
  const navOpen = await page.$eval(".mobile-nav", (el) => el.checkVisibility?.());
  console.log(`MOBILE MENU: buttonVisible=${menuVisibleBefore} expanded ${expandedBefore} -> ${expandedAfter} navVisible=${navOpen}`);
  await ctx.close();
}

// 3) Contact form focus ring + input/readability in both themes.
for (const theme of ["light", "dark"]) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript((t) => { try { localStorage.setItem("fixna.theme", t); } catch {} }, theme);
  const page = await ctx.newPage();
  await page.goto("http://localhost:3002/contact", { waitUntil: "load" });
  await page.waitForTimeout(200);
  await page.focus("input[name=\"email\"]");
  const info = await page.evaluate(() => {
    const input = document.querySelector("input[name=\"email\"]");
    const css = (el) => el && getComputedStyle(el);
    const ring = getComputedStyle(document.documentElement).getPropertyValue("--ring");
    return {
      inputBorder: css(input)?.borderColor,
      inputFocusOutline: css(input)?.outlineColor,
      inputFocusShadow: css(input)?.boxShadow,
      ringToken: ring,
    };
  });
  console.log(`CONTACT ${theme} focus:`, JSON.stringify(info));
  await ctx.close();
}
await browser.close();
