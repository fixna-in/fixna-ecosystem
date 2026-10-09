export type ProductStatus = "LIVE" | "MVP" | "COMING SOON";

export type Product = {
  id: string;
  name: string;
  description: string;
  status: ProductStatus;
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
    url: "https://localboost.fixna.in/",
    category: "Marketing intelligence",
    capabilities: ["Campaign orchestration", "AI recommendations", "Local targeting", "Performance tracking"],
  },
  {
    id: "consulting",
    name: "Fixna Consulting",
    description: "Technology consulting and client delivery platform for modern engineering teams.",
    status: "MVP",
    url: "https://consulting.fixna.in",
    category: "Technology consulting",
    capabilities: ["Architecture", "Cloud", "Kafka", "AI consulting"],
  },
  {
    id: "hospitality",
    name: "Fixna Hospitality",
    description: "Configurable technology platform for hotel and hospitality operations.",
    status: "COMING SOON",
    url: null,
    category: "Hospitality operations",
    capabilities: ["Operations", "Guest experiences", "Service workflows", "Business configuration"],
  },
];
