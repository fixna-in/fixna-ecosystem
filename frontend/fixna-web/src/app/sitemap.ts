import type { MetadataRoute } from "next";
import { siteUrl } from "@/lib/site-url";

const routes = [
  "",
  "/about",
  "/what-we-do",
  "/products",
  "/consulting",
  "/technology",
  "/industries",
  "/contact",
  "/privacy-policy",
  "/terms",
];

// Stable per-route dates (R7): only bump a date when that page's content
// actually changes, so builds stop churning every lastModified timestamp.
const lastModifiedByRoute: Record<string, Date> = {
  "": new Date("2026-10-11"),
  "/about": new Date("2026-10-11"),
  "/what-we-do": new Date("2026-10-11"),
  "/products": new Date("2026-10-11"),
  "/consulting": new Date("2026-10-11"),
  "/technology": new Date("2026-10-11"),
  "/industries": new Date("2026-10-11"),
  "/contact": new Date("2026-10-11"),
  "/privacy-policy": new Date("2026-10-11"),
  "/terms": new Date("2026-10-11"),
};

export default function sitemap(): MetadataRoute.Sitemap {
  return routes.map((route) => ({
    url: `${siteUrl}${route}`,
    lastModified: lastModifiedByRoute[route],
    changeFrequency: "weekly",
    priority: route === "" ? 1 : 0.8,
  }));
}
