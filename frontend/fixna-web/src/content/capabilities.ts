export type Capability = {
  title: string;
  description: string;
  problem: string;
  outcome: string;
};

export const capabilities: Capability[] = [
  {
    title: "Product Engineering",
    description: "Build SaaS platforms, internal tools, and business applications.",
    problem: "Ideas need a clear product direction and sustainable delivery path.",
    outcome: "A usable product that can evolve with the business.",
  },
  {
    title: "Solution Architecture",
    description: "Turn complex requirements into executable technical designs.",
    problem: "Business needs are difficult to translate into a dependable architecture.",
    outcome: "A design that balances clarity, delivery speed, and operational risk.",
  },
  {
    title: "Cloud & Modernization",
    description: "Modernize legacy systems and move workloads to cloud-native platforms.",
    problem: "Older systems can slow delivery, increase operational risk, and limit scale.",
    outcome: "A more maintainable platform that is easier to deploy and operate.",
  },
  {
    title: "Event-Driven Systems",
    description: "Design streaming, asynchronous workflows, and distributed integrations.",
    problem: "Independent services and systems need dependable coordination.",
    outcome: "A resilient architecture with clearer operational boundaries.",
  },
  {
    title: "AI & GenAI",
    description: "Apply AI to recommendations, workflows, and business automation.",
    problem: "AI can be added without a clear business or operational goal.",
    outcome: "Focused AI experiences that deliver measurable value.",
  },
  {
    title: "Integration & Automation",
    description: "Connect APIs, systems, and processes across the business.",
    problem: "Disconnected tools create manual work and inconsistent data.",
    outcome: "Cleaner workflows with fewer handoffs and less operational friction.",
  },
  {
    title: "Observability",
    description: "Make systems measurable, diagnosable, and reliable in production.",
    problem: "Teams cannot improve what they cannot see or understand.",
    outcome: "Clear operational insight and faster issue resolution.",
  },
  {
    title: "Technical Consulting",
    description: "Provide architecture reviews, technical leadership, and engineering advisory.",
    problem: "Technical decisions need strong business context and engineering judgment.",
    outcome: "A practical path to delivery with fewer avoidable mistakes.",
  },
];
