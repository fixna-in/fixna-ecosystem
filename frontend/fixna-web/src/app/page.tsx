import Link from "next/link";
import { capabilities } from "@/content/capabilities";
import { products } from "@/content/products";
import { technologyGroups } from "@/content/technology";
import { consultingExpertise } from "@/content/consulting";
import { productLinks } from "@/lib/product-links";

const approach = [
  ["01", "Understand", "Define the business problem, users, requirements, and constraints."],
  ["02", "Assess", "Identify the architecture, delivery, and operational risks that matter."],
  ["03", "Design", "Choose a practical approach without unnecessary complexity."],
  ["04", "Build", "Deliver production software in clear, testable stages."],
  ["05", "Modernize", "Evolve existing systems without disrupting what already works."],
  ["06", "Optimize", "Use evidence to improve reliability, performance, and business value."],
];

export default function HomePage() {
  return (
    <>
      <section className="hero" aria-labelledby="hero-title">
        <div className="container hero-grid">
          <div className="hero-copy">
            <p className="eyebrow">Technology • Product Engineering • AI</p>
            <h1 id="hero-title">Build software that solves real business problems.</h1>
            <p>We design and build practical digital products, modernize legacy systems, and apply AI where it creates meaningful business value.</p>
            <div className="hero-actions">
              <Link className="button button-primary" href="/what-we-do">Explore What We Do</Link>
              <Link className="button button-secondary" href="/products">View Our Products</Link>
            </div>
            <p className="hero-trust">Java • Spring Boot • Confluent Kafka • AWS • Azure • AI/GenAI</p>
          </div>

          <div className="hero-visual" aria-label="Fixna system architecture overview">
            <div className="architecture-card">
              <div className="architecture-flow">
                <div className="flow-node"><span className="icon">01</span><div><strong>Business</strong><span>Define the real need</span></div></div>
                <div className="flow-node"><span className="icon">02</span><div><strong>Architecture</strong><span>Build the right foundation</span></div></div>
                <div className="flow-node"><span className="icon">03</span><div><strong>Product</strong><span>Deliver working software</span></div></div>
                <div className="flow-node"><span className="icon">04</span><div><strong>Operate</strong><span>Measure and improve</span></div></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="capabilities-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">What Fixna does</p>
            <h2 id="capabilities-title">Technology built around real business needs.</h2>
            <p>Fixna is a technology and product engineering company helping businesses turn complex requirements into dependable software.</p>
          </div>
          <div className="capability-grid">
            {capabilities.slice(0, 6).map((capability, index) => (
              <article className="capability-card" key={capability.title}>
                <span className="capability-index">{String(index + 1).padStart(2, "0")}</span>
                <div>
                  <h3>{capability.title}</h3>
                  <p>{capability.description}</p>
                </div>
                <div className="capability-card__meta">
                  <strong>Outcome</strong>
                  <span>{capability.outcome}</span>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section section--compact" aria-labelledby="products-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Products</p>
            <h2 id="products-title">Built by Fixna.</h2>
            <p>Products designed around real business workflows.</p>
          </div>
          <div className="grid card-grid-3">
            {products.map((product) => (
              <article className="card product-card" key={product.id}>
                <span className={`status ${product.status === "LIVE" ? "live" : "coming-soon"}`}>{product.status}</span>
                <h3>{product.name}</h3>
                <p>{product.description}</p>
                <p><strong>{product.category}</strong></p>
                <div className="card-list">
                  {product.capabilities.map((capability) => <span key={capability}>{capability}</span>)}
                </div>
                {product.url ? (
                  <a className="button button-primary" href={product.url} target="_blank" rel="noreferrer">Explore {product.name === "Fixna LocalBoost" ? "LocalBoost" : product.name}</a>
                ) : (
                  <span className="button button-secondary" aria-disabled="true">Coming Soon</span>
                )}
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section page-section--muted" aria-labelledby="technology-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Engineering foundations</p>
            <h2 id="technology-title">Built on proven engineering foundations.</h2>
            <p>We select established tools for maintainability, clarity, and operational confidence.</p>
          </div>
          <div className="grid technology-grid">
            {technologyGroups.map((group) => (
              <article className="card technology-group" key={group.title}>
                <h3>{group.title}</h3>
                <ul>
                  {group.items.map((item) => <li key={item}>{item}</li>)}
                </ul>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="consulting-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Consulting</p>
            <h2 id="consulting-title">Architecture decisions that hold up in production.</h2>
            <p>From technical reviews to implementation support, we help teams make better engineering decisions.</p>
          </div>
          <div className="grid card-grid-4">
            {consultingExpertise.map((item) => <article className="card" key={item}><h3>{item}</h3></article>)}
          </div>
          <div className="cta-actions" style={{ marginTop: "1.5rem" }}>
            <Link className="button button-primary" href="/contact">Discuss a technology challenge</Link>
            <Link className="button button-secondary" href="/consulting">Explore Consulting</Link>
          </div>
        </div>
      </section>

      <section className="section page-section--muted" aria-labelledby="approach-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">How we work</p>
            <h2 id="approach-title">A practical path from problem to production.</h2>
          </div>
          <div className="grid process-grid">
            {approach.map(([number, title, description]) => (
              <article className="card process-card" key={number}>
                <strong>{number}</strong>
                <h3>{title}</h3>
                <p>{description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="industries-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Industries</p>
            <h2 id="industries-title">Software shaped by the realities of each business.</h2>
          </div>
          <div className="industry-grid">
            {[
              ["Local Business", "Clear workflows and measurable marketing performance."],
              ["Hospitality", "Coordinated operations, service, and guest experiences."],
              ["Professional Services", "Reliable delivery systems and transparent client operations."],
              ["Enterprise Technology", "Modernization support for complex platforms and teams."],
            ].map(([title, text]) => (
              <article className="industry-card" key={title}>
                <span className="industry-card__index">{title}</span>
                <h3>{title}</h3>
                <p>{text}</p>
              </article>
            ))}
          </div>
          <p style={{ marginTop: "1.25rem" }}><Link href="/industries">More vertical products are being developed within the Fixna ecosystem.</Link></p>
        </div>
      </section>

      <section className="section page-section--muted" aria-labelledby="statement-title">
        <div className="container two-column">
          <div>
            <p className="eyebrow">Our philosophy</p>
            <h2 id="statement-title">Technology should solve a business problem, not create another one.</h2>
          </div>
          <div className="content-list">
            <div className="card"><strong>Practical over fashionable</strong><p>Use the right technology for the problem and operational reality.</p></div>
            <div className="card"><strong>Architecture before unnecessary complexity</strong><p>Make decisions that support delivery and change.</p></div>
            <div className="card"><strong>AI with purpose</strong><p>Apply AI where it creates measurable value and understands the surrounding process.</p></div>
            <div className="card"><strong>Build for reality</strong><p>Design systems that people can operate, maintain, and improve.</p></div>
          </div>
        </div>
      </section>

      <section className="cta-band" aria-labelledby="cta-title">
        <div className="container cta-band__inner">
          <div>
            <p className="eyebrow">Start a conversation</p>
            <h2 id="cta-title">Have a business or technology problem to solve?</h2>
          </div>
          <div className="cta-actions">
            <Link className="button button-primary" href="/contact">Start a conversation</Link>
            <Link className="button button-ghost" href="/consulting">Explore consulting</Link>
          </div>
        </div>
      </section>
    </>
  );
}
