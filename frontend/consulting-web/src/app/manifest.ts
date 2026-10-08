import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Fixna Consulting",
    short_name: "Fixna",
    description: "Client relationship management for consulting firms.",
    start_url: "/",
    display: "standalone",
    background_color: "#f5f7f4",
    theme_color: "#163e32",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/apple-icon.svg", sizes: "any", type: "image/svg+xml", purpose: "apple" },
    ],
  };
}
