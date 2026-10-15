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
    description: "Technology consulting services — architecture, modernization, and engineering advisory.",
    status: "MVP",
    statusNote:
      "Available for early access. Delivered as professional services with support from the Fixna Consulting Workspace (early-access software). AI drafting features are advisory templates that always require human review.",
    url: "https://consulting.fixna.in/",
    category: "Technology consulting",
    capabilities: ["Architecture", "Cloud", "Kafka", "AI consulting"],
  },
  {
    id: "website-audit",
    name: "Fixna Website Audit",
    description: "Automated technical and on-page website audit with evidence-based findings and prioritized remediation.",
    status: "LIVE",
    statusNote: "Publicly available. Identifies technical SEO, performance, accessibility, and security issues.",
    url: "https://websiteaudit.fixna.in/",
    category: "Website intelligence",
    capabilities: ["Technical SEO audit", "Performance analysis", "Accessibility checks", "Security scanning", "Evidence-based findings", "Prioritized remediation"],
  },
  {
    id: "invoice-assistant",
    name: "Fixna Invoice Assistant",
    description: "Converts invoices and receipts into structured, reviewable records with field-level confidence and export.",
    status: "LIVE",
    statusNote: "Publicly available. Private PDF intake, offline OCR, confidence-gated review workflow, CSV/JSON export.",
    url: "https://invoice.fixna.in/",
    category: "Document intelligence",
    capabilities: ["PDF invoice extraction", "Offline OCR fallback", "Field-level confidence", "Review & approval workflow", "CSV/JSON export", "Cross-tenant search"],
  },
  {
    id: "hospitality",
    name: "Fixna Hospitality",
    description: "Configurable technology platform for hotel and hospitality operations.",
    status: "LIVE",
    statusNote: "Publicly available. Operations, guest experiences, service workflows, and business configuration.",
    url: "https://hospitality.fixna.in/",
    category: "Hospitality operations",
    capabilities: ["Operations", "Guest experiences", "Service workflows", "Business configuration"],
  },
];
