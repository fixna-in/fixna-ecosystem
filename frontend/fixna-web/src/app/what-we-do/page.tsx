import Link from "next/link";
import { capabilities } from "@/content/capabilities";
import { SectionHeading } from "@/components/section-heading";

export const metadata = {
  title: "What We Do",
  description: "Fixna designs and builds practical products, modernizes platforms, and applies AI where it creates measurable business value.",
};

const capabilityIcons = ["01", "02", "03", "04", "05", "06"];

export default function WhatWeDoPage() {
  return (
    <>
      <section className="page-hero page-hero--compact">
        <div className="container page-hero__inner">
          <div>
            <p className="page-kicker">What we do</p>
            <h1>Technology built around real business needs.</h1>
            <p>We combine product thinking, engineering leadership, cloud expertise, and AI delivery to help businesses move from problem to working software.</p>
          </div>
          <div className="hero-stat-card">
            <span>Practical</span>
            <strong>Product engineering</strong>
            <small>Architecture before unnecessary complexity.</small>
          </div>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <SectionHeading
            eyebrow="Capabilities"
            title="The work is shaped by the business, not by technology trends."
            description="Every engagement starts with the requirement, constraints, and outcome that matter."
          />
          <div className="capability-grid">
            {capabilities.map((capability, index) => (
              <article className="capability-card" key={capability.title}>
                <span className="capability-index">{capabilityIcons[index]}</span>
                <div>
                  <h3>{capability.title}</h3>
                  <p>{capability.description}</p>
                </div>
                <div className="capability-card__meta">
                  <strong>Typical outcome</strong>
                  <span>{capability.outcome}</span>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="page-section page-section--muted">
        <div className="container two-column">
          <div>
            <p className="eyebrow">How we work</p>
            <h2>Clear decisions. Measurable progress.</h2>
          </div>
          <ol className="process-list">
            {[
              ["01", "Understand", "Define the business problem, constraints, users, and measurable outcome."],
              ["02", "Assess", "Identify the architectural, operational, and delivery risks that matter."],
              ["03", "Design", "Select a practical technical approach without unnecessary complexity."],
              ["04", "Build", "Deliver production-quality software in focused, testable stages."],
              ["05", "Modernize", "Evolve existing systems without disrupting what already works."],
              ["06", "Optimize", "Use evidence to improve reliability, performance, and business value."],
            ].map(([number, title, description]) => (
              <li key={number}>
                <strong>{number}</strong>
                <div><h3>{title}</h3><p>{description}</p></div>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="cta-band">
        <div className="container cta-band__inner">
          <div>
            <p className="eyebrow">Need a practical technical partner?</p>
            <h2>Bring us the problem. We’ll help shape the path forward.</h2>
          </div>
          <Link className="button button-primary" href="/contact">Start a conversation</Link>
        </div>
      </section>
    </>
  );
}
