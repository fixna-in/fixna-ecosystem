// Focused check: homepage hero alignment + contact textarea placeholder color in dark.
import { chromium } from "playwright";
const browser = await chromium.launch({ channel: "chrome", headless: true });

// Home hero alignment (desktop + mobile)
for (const theme of ["light", "dark"]) {
  for (const vp of [{ n: "desktop", w: 1440, h: 900 }, { n: "mobile", w: 390, h: 844 }]) {
    const ctx = await browser.newContext({ viewport: { width: vp.w, height: vp.h }, deviceScaleFactor: vp.n === "mobile" ? 2 : 1, isMobile: vp.n === "mobile", hasTouch: vp.n === "mobile" });
    await ctx.addInitScript((t) => { try { localStorage.setItem("fixna.theme", t); } catch {} }, theme);
    const page = await ctx.newPage();
    await page.goto("http://localhost:3002/", { waitUntil: "load" });
    await page.waitForTimeout(300);
    const info = await page.evaluate(() => {
      const hero = document.querySelector(".hero");
      const copy = document.querySelector(".hero-copy");
      const h1 = document.querySelector(".hero h1");
      const trust = document.querySelector(".hero-trust");
      const cs = (e) => e && getComputedStyle(e);
      return {
        heroCopyMaxWidth: copy ? cs(copy).maxWidth : null,
        heroCopyMarginInline: copy ? cs(copy).marginInline : null,
        h1TextAlign: h1 ? cs(h1).textAlign : null,
        h1MaxWidth: h1 ? cs(h1).maxWidth : null,
        trustTextAlign: trust ? cs(trust).textAlign : null,
        heroPaddingTop: hero ? cs(hero).paddingTop : null,
      };
    });
    console.log(`HOME ${theme}/${vp.n}:`, JSON.stringify(info));
    await ctx.close();
  }
}

// Contact form placeholder + a few text colors in dark
for (const theme of ["dark", "light"]) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript((t) => { try { localStorage.setItem("fixna.theme", t); } catch {} }, theme);
  const page = await ctx.newPage();
  await page.goto("http://localhost:3002/contact", { waitUntil: "load" });
  await page.waitForTimeout(300);
  const info = await page.evaluate(() => {
    const ta = document.querySelector("textarea[name=\"requirement\"]");
    const inp = document.querySelector("input[name=\"email\"]");
    const cs = (e, p) => e && getComputedStyle(e, p);
    return {
      taColor: cs(ta).color,
      taBg: cs(ta).backgroundColor,
      phColor: cs(ta, "::placeholder").color,
      phOpacity: cs(ta, "::placeholder").opacity,
      inpColor: cs(inp).color,
      inpBg: cs(inp).backgroundColor,
    };
  });
  console.log(`CONTACT ${theme}:`, JSON.stringify(info));
  await ctx.close();
}
await browser.close();
