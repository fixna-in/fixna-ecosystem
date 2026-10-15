// Verify rendered styles for fixna-web in light/dark themes.
import { chromium } from "playwright";

const BASE = "http://localhost:3002";
const browser = await chromium.launch({ channel: "chrome", headless: true });

function lum(hex) {
  const m = hex.replace("#", "");
  const r = parseInt(m.slice(0, 2), 16) / 255;
  const g = parseInt(m.slice(2, 4), 16) / 255;
  const b = parseInt(m.slice(4, 6), 16) / 255;
  const f = (c) => (c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
}
function contrast(fg, bg) {
  const l1 = lum(fg), l2 = lum(bg);
  const [hi, lo] = l1 > l2 ? [l1, l2] : [l2, l1];
  return (hi + 0.05) / (lo + 0.05);
}
function toHex(rgb) {
  const nums = rgb.match(/\d+/g).map(Number);
  return "#" + nums.slice(0, 3).map((n) => n.toString(16).padStart(2, "0")).join("");
}

const routes = ["/", "/what-we-do", "/products", "/about", "/contact", "/technology", "/industries", "/consulting"];

for (const theme of ["light", "dark"]) {
  console.log(`\n===== THEME: ${theme.toUpperCase()} =====`);
  for (const route of routes) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await ctx.addInitScript((t) => { try { localStorage.setItem("fixna.theme", t); } catch {} }, theme);
    const page = await ctx.newPage();
    await page.goto(BASE + route, { waitUntil: "load", timeout: 30000 });
    await page.waitForTimeout(500);

    const data = await page.evaluate(() => {
      const cs = (el) => el ? getComputedStyle(el) : null;
      const body = document.body;
      const h1 = document.querySelector("h1");
      const header = document.querySelector(".site-header");
      const btn = document.querySelector(".button-primary");
      const card = document.querySelector(".card, .capability-card, .industry-card");
      const hero = document.querySelector(".hero, .page-hero");
      const muted = document.querySelector(".page-section--muted, .section--compact, .capability-index, .technology-group li");
      const navLink = document.querySelector(".main-nav a");
      return {
        themeAttr: document.documentElement.getAttribute("data-theme"),
        bodyBg: cs(body).backgroundColor,
        bodyColor: cs(body).color,
        h1FontSize: h1 ? cs(h1).fontSize : null,
        h1Color: h1 ? cs(h1).color : null,
        h1Weight: h1 ? cs(h1).fontWeight : null,
        h1LetterSpacing: h1 ? cs(h1).letterSpacing : null,
        headerBg: header ? cs(header).backgroundColor : null,
        headerHeight: header ? Math.round(header.getBoundingClientRect().height) : null,
        navLinkColor: navLink ? cs(navLink).color : null,
        primaryBtnBg: btn ? cs(btn).backgroundColor : null,
        primaryBtnShadow: btn ? cs(btn).boxShadow : "none",
        cardBg: card ? cs(card).backgroundColor : null,
        cardRadius: card ? cs(card).borderRadius : null,
        cardShadow: card ? cs(card).boxShadow : "none",
        mutedBg: muted ? cs(muted).backgroundColor : null,
        heroBg: hero ? cs(hero).backgroundColor : null,
        heroBefore: hero ? getComputedStyle(hero, "::before") : null,
        heroAfter: hero ? getComputedStyle(hero, "::after") : null,
      };
    });

    const bodyBg = toHex(data.bodyBg);
    const bodyColor = toHex(data.bodyColor);
    const h1Color = data.h1Color ? toHex(data.h1Color) : null;
    const btnBg = data.primaryBtnBg ? toHex(data.primaryBtnBg) : null;
    const btnFg = "#ffffff";
    const ratio = contrast(bodyColor, bodyBg);
    const btnRatio = btnBg ? contrast(btnFg, btnBg) : null;
    const h1Ratio = h1Color ? contrast(h1Color, bodyBg) : null;
    const deco = (data.heroBefore && data.heroBefore.content && data.heroBefore.content !== "none") || (data.heroAfter && data.heroAfter.content && data.heroAfter.content !== "none");

    console.log(`${route.padEnd(14)} attr=${data.themeAttr} body=${bodyBg}/${bodyColor} ${ratio.toFixed(1)}:1 | h1=${data.h1FontSize} w${data.h1Weight} ls=${data.h1LetterSpacing} ${h1Ratio ? h1Ratio.toFixed(1) : "-"}:1 | header=${data.headerHeight}px nav=${data.navLinkColor ? toHex(data.navLinkColor) : "-"} | btn=${btnBg} ${btnRatio ? btnRatio.toFixed(1) : "-"}:1 shadow=${data.primaryBtnShadow === "none" ? "none" : "YES"} | card=${data.cardBg ? toHex(data.cardBg) : "-"} r=${data.cardRadius} sh=${data.cardShadow === "none" ? "none" : "YES"} | muted=${data.mutedBg ? toHex(data.mutedBg) : "-"} | hero=${data.heroBg ? toHex(data.heroBg) : "-"} deco=${deco ? "PRESENT" : "none"}`);
    await ctx.close();
  }
}

// Theme toggle behavior: click toggles data-theme without reload.
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
await ctx.addInitScript(() => { try { localStorage.setItem("fixna.theme", "light"); } catch {} });
const page = await ctx.newPage();
await page.goto(BASE + "/", { waitUntil: "load" });
await page.waitForTimeout(200);
const before = await page.evaluate(() => document.documentElement.getAttribute("data-theme"));
await page.click(".theme-toggle");
await page.waitForTimeout(200);
const after = await page.evaluate(() => document.documentElement.getAttribute("data-theme"));
const stored = await page.evaluate(() => { try { return localStorage.getItem("fixna.theme"); } catch { return null; } });
console.log(`\nTHEME TOGGLE: before=${before} -> after=${after}, stored=${stored} (no reload: ${before !== after})`);
await browser.close();
