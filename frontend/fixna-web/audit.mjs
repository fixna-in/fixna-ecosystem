// Broader audit: centered text (incl. inherited through flex/grid), contrast < 4.5, and placeholder readability.
import { chromium } from "playwright";

const browser = await chromium.launch({ channel: "chrome", headless: true });
const routes = ["/", "/what-we-do", "/products", "/about", "/consulting", "/contact", "/technology", "/industries", "/capabilities", "/privacy-policy"];

for (const theme of ["light", "dark"]) {
  for (const vp of [{ n: "desktop", w: 1440, h: 900 }, { n: "mobile", w: 390, h: 844 }]) {
    const centered = [];
    const lowContrast = [];
    const placeholders = [];
    for (const route of routes) {
      const ctx = await browser.newContext({ viewport: { width: vp.w, height: vp.h }, deviceScaleFactor: vp.n === "mobile" ? 2 : 1, isMobile: vp.n === "mobile", hasTouch: vp.n === "mobile" });
      await ctx.addInitScript((t) => { try { localStorage.setItem("fixna.theme", t); } catch {} }, theme);
      const page = await ctx.newPage();
      try { await page.goto("http://localhost:3002" + route, { waitUntil: "load", timeout: 20000 }); } catch { await ctx.close(); continue; }
      await page.waitForTimeout(500);
      const res = await page.evaluate(() => {
        const lum = (rgb) => { const n = rgb.match(/\d+/g).map(Number); if (!n||n.length<3) return 0; const [r,g,b]=n.slice(0,3).map(v=>v/255); const f=c=>c<=0.03928?c/12.92:Math.pow((c+0.055)/1.055,2.4); return 0.2126*f(r)+0.7152*f(g)+0.0722*f(b); };
        const contrast = (fg,bg)=> { const a=lum(fg),b=lum(bg); const [hi,lo]=a>b?[a,b]:[b,a]; return (hi+0.05)/(lo+0.05); };
        const bgOf=(el)=>{let p=el,b;do{b=getComputedStyle(p).backgroundColor;if(b&&b!=="rgba(0, 0, 0, 0)"&&!b.includes("transparent"))break;p=p.parentElement;}while(p&&p!==document.documentElement);return b;};
        const hex=rgb=>{const n=rgb.match(/\d+/g);return n&&n.length>=3?"#"+n.slice(0,3).map(x=>x.toString(16).padStart(2,"0")).join(""):rgb;};
        const label=e=>`${e.tagName}${e.className?"."+e.className.split(" ")[0]:""}`;
        const out={centered:[],low:[],ph:[]};
        document.querySelectorAll("input, textarea").forEach(el=>{
          const ph=getComputedStyle(el,null).getPropertyValue("--placeholder-color"); // not standard; use pseudo
          let phColor=null, phBg=null;
          try{ const pseudo=getComputedStyle(el,"::placeholder"); phColor=pseudo.color; }catch(e){}
          const bg=bgOf(el);
          if(phColor){ const c=contrast(phColor,bg); out.ph.push(`${label(el)} phr=${hex(phColor)} bg=${hex(bg)} ratio=${c.toFixed(2)}`); }
        });
        document.querySelectorAll("*").forEach(el=>{
          const s=getComputedStyle(el); const t=(el.textContent||"").trim(); if(t.length<2) return;
          const r=contrast(s.color,bgOf(el));
          if(s.textAlign==="center") out.centered.push(`${label(el)} ta=center txt="${t.slice(0,18)}"`);
          if(r<4.5) out.low.push(`${label(el)} c=${hex(s.color)} bg=${hex(bgOf(el))} ratio=${r.toFixed(2)} txt="${t.slice(0,16)}"`);
        });
        return out;
      });
      if(res.centered.length) centered.push({route, samples: res.centered.slice(0,3)});
      const low = res.low.filter(l=>!l.includes("placeholder")); // avoid dup
      if(res.low.length) lowContrast.push({route, n:res.low.length, samples: res.low.slice(0,5)});
      if(res.ph.length) placeholders.push({route, samples: res.ph.slice(0,3)});
      await ctx.close();
    }
    console.log(`=== ${theme.toUpperCase()} ${vp.n.toUpperCase()} ===`);
    if(centered.length){console.log("CENTERED:", JSON.stringify(centered));}
    else{console.log("CENTERED: []");}
    if(lowContrast.length){console.log("CONTRAST<4.5:", JSON.stringify(lowContrast));}
    else{console.log("CONTRAST<4.5: []");}
    if(placeholders.length){console.log("PLACEHOLDER:", JSON.stringify(placeholders));}
    else{console.log("PLACEHOLDER: []");}
  }
}
await browser.close();
