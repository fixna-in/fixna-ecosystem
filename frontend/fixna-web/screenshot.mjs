// Screenshot helper for fixna-web visual review.
// Usage: node screenshot.mjs <outDir> <route...>
import { chromium } from "playwright";
import { mkdirSync } from "node:fs";
import { join } from "node:path";

const BASE = "http://localhost:3002";
const outDir = process.argv[2] ?? "shots";
const routes = process.argv.slice(3).length ? process.argv.slice(3) : ["/"];
mkdirSync(outDir, { recursive: true });

const VIEWPORTS = [
  { name: "desktop", width: 1440, height: 900 },
  { name: "mobile", width: 390, height: 844 },
];
const THEMES = ["light", "dark"];

const browser = await chromium.launch({ channel: "chrome", headless: true });

for (const route of routes) {
  const slug = route === "/" ? "home" : route.replace(/^\//, "").replace(/\//g, "-");
  for (const vp of VIEWPORTS) {
    for (const theme of THEMES) {
      const ctx = await browser.newContext({
        viewport: { width: vp.width, height: vp.height },
        deviceScaleFactor: vp.name === "mobile" ? 2 : 1,
        isMobile: vp.name === "mobile",
        hasTouch: vp.name === "mobile",
      });
      // Force the persisted theme before any paint (mirrors the site's own init).
      await ctx.addInitScript((t) => {
        try { localStorage.setItem("fixna.theme", t); } catch {}
      }, theme);
      const page = await ctx.newPage();
      await page.goto(BASE + route, { waitUntil: "load", timeout: 30000 });
      await page.waitForTimeout(400);
      const file = join(outDir, `${slug}-${vp.name}-${theme}.png`);
      await page.screenshot({ path: file, fullPage: false });
      console.log("saved", file);
      await ctx.close();
    }
  }
}

await browser.close();
