export type ProductStatus = "LIVE" | "MVP" | "COMING SOON";

export type Product = {
  id: string;
  name: string;
  description: string;
  status: ProductStatus;
  /** Honest one-line clarification of availability and integration reality. */
  statusNote: string;
  url: string | null;
  category: string;
  capabilities: string[];
};

export const products: Product[] = [
  {
    id: "localboost",
    name: "Fixna LocalBoost",
    description: "AI-assisted marketing orchestration for local businesses.",
    status: "LIVE",
    statusNote:
      "Publicly available. Advertising-platform execution currently uses simulated (mock) adapters — no live ad spend occurs.",
    url: "https://localboost.fixna.in/",
    category: "Marketing intelligence",
    capabilities: ["Campaign orchestration", "AI recommendations", "Local targeting", "Performance tracking"],
  },
  {
    id: "consulting",
    name: "Fixna Consulting",
    description: "Technology consulting and client delivery platform for modern engineering teams.",
    status: "MVP",
    statusNote:
      "Available for early access. AI drafting features are advisory templates that always require human review.",
    url: "https://consulting.fixna.in/",
    category: "Technology consulting",
    capabilities: ["Architecture", "Cloud", "Kafka", "AI consulting"],
  },
  {
    id: "hospitality",
    name: "Fixna Hospitality",
    description: "Configurable technology platform for hotel and hospitality operations.",
    status: "COMING SOON",
    statusNote: "Planned product — not yet available.",
    url: null,
    category: "Hospitality operations",
    capabilities: ["Operations", "Guest experiences", "Service workflows", "Business configuration"],
  },
];
