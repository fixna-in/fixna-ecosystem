import Link from "next/link";
import { capabilities } from "@/content/capabilities";
import { company } from "@/content/company";
import { products } from "@/content/products";
import { technologyGroups } from "@/content/technology";
import { consultingExpertise } from "@/content/consulting";
import { productLinks } from "@/lib/product-links";

const process = [
  { number: "01", title: "Understand", description: "Understand the business problem and the constraints that matter." },
  { number: "02", title: "Architect", description: "Design the right technical solution without unnecessary complexity." },
  { number: "03", title: "Build", description: "Deliver production-quality software incrementally." },
  { number: "04", title: "Validate", description: "Test, observe, and measure the result in real use." },
  { number: "05", title: "Improve", description: "Refine the system based on evidence and feedback." },
];

export default function HomePage() {
  return (
    <>
      <section className="hero" aria-labelledby="hero-title">
        <div className="container hero-grid">
          <div className="hero-copy">
            <p className="eyebrow">Fixna / technology + product engineering</p>
            <h1 id="hero-title">We build software that solves real business problems.</h1>
            <p>From cloud-native platforms and event-driven systems to AI-powered products and business automation.</p>
            <div className="hero-actions">
              <Link className="button button-primary" href="/contact">Work With Us</Link>
              <Link className="button button-secondary" href="/products">Explore Our Products</Link>
            </div>
            <p className="hero-trust">Product engineering · Cloud architecture · AI and automation</p>
          </div>

          <div className="hero-visual" aria-label="Fixna system architecture overview">
            <div className="architecture-card">
              <div className="architecture-flow">
                <div className="flow-node"><span className="icon">01</span><div><strong>Strategy</strong><span>Business problem</span></div></div>
                <div className="flow-node"><span className="icon">02</span><div><strong>Architecture</strong><span>Reliable technical design</span></div></div>
                <div className="flow-node"><span className="icon">03</span><div><strong>Delivery</strong><span>Production software</span></div></div>
                <div className="flow-node"><span className="icon">04</span><div><strong>Measure</strong><span>Observability and improvement</span></div></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="capabilities-title">
        <div className="container">
          <div className="section-intro">
            <div className="section-heading">
              <p className="eyebrow">What we do</p>
              <h2 id="capabilities-title">Practical engineering for the problems businesses actually face.</h2>
            </div>
          </div>
          <div className="grid card-grid-3">
            {capabilities.map((capability, index) => (
              <article className="card" key={capability.title}>
                <span className="card-index">0{index + 1}</span>
                <h3>{capability.title}</h3>
                <p>{capability.description}</p>
                <p><strong>Problem:</strong> {capability.problem}</p>
                <p><strong>Outcome:</strong> {capability.outcome}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="products-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Our products</p>
            <h2 id="products-title">Distinct products, built around clear business needs.</h2>
          </div>
          <div className="grid card-grid-3">
            {products.map((product) => (
              <article className="card product-card" key={product.id}>
                <span className={`status ${product.status === "LIVE" ? "live" : product.status === "COMING SOON" ? "coming-soon" : ""}`}>{product.status}</span>
                <h3>{product.name}</h3>
                <p>{product.description}</p>
                <p>{product.category}</p>
                <div className="card-list">
                  {product.capabilities.map((capability) => <span key={capability}>{capability}</span>)}
                </div>
                {product.url ? (
                  <a className="button button-primary" href={product.url} target="_blank" rel="noreferrer">Explore {product.name.split(" ").slice(1).join(" ")}</a>
                ) : (
                  <span className="button button-secondary" aria-disabled="true">Coming Soon</span>
                )}
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="technology-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Our technology</p>
            <h2 id="technology-title">The tools we use are chosen for the job, not for decoration.</h2>
          </div>
          <div className="grid technology-grid">
            {technologyGroups.map((group) => (
              <div className="card technology-group" key={group.title}>
                <h3>{group.title}</h3>
                <ul>
                  {group.items.map((item) => <li key={item}>{item}</li>)}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="process-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">How we work</p>
            <h2 id="process-title">We prefer practical architecture over unnecessary complexity.</h2>
          </div>
          <div className="grid process-grid">
            {process.map((step) => (
              <article className="card process-card" key={step.number}>
                <strong>{step.number}</strong>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="consulting-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Consulting expertise</p>
            <h2 id="consulting-title">Architecture and engineering guidance that turns complexity into an executable plan.</h2>
          </div>
          <div className="grid card-grid-4">
            {consultingExpertise.map((item) => (
              <div className="card" key={item}><h3>{item}</h3></div>
            ))}
          </div>
          <div className="cta-actions" style={{ marginTop: "1.5rem" }}>
            <Link className="button button-primary" href="/contact">Discuss Your Architecture</Link>
            <Link className="button button-secondary" href="/consulting">Explore Consulting</Link>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="who-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Who we work with</p>
            <h2 id="who-title">Partners with the ambition to build and modernize.</h2>
            <p>Whether you are building a new product or modernizing an existing platform, we help turn technical complexity into an executable engineering plan.</p>
          </div>
          <div className="grid client-grid">
            {[
              { title: "Startups", text: "Product strategy, architecture, and rapid delivery." },
              { title: "SMBs", text: "Practical systems that support business growth." },
              { title: "Product Companies", text: "Scalable platforms and maintainable engineering foundations." },
              { title: "Enterprises", text: "Complex modernization with clear operational boundaries." },
              { title: "GCCs", text: "Technology delivery and capability development." },
              { title: "Digital Businesses", text: "Automation, integration, and AI-enabled workflows." },
            ].map((item) => (
              <article className="card client-card" key={item.title}><h3>{item.title}</h3><p>{item.text}</p></article>
            ))}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="industries-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Industries & solutions</p>
            <h2 id="industries-title">Flexible software for the industries and workflows that need it.</h2>
          </div>
          <div className="grid industry-grid">
            {[
              "Local Business",
              "Hospitality",
              "Professional Services",
              "Healthcare",
              "Education",
              "Enterprise Technology",
            ].map((industry) => <div className="card" key={industry}><h3>{industry}</h3></div>)}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="why-title">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Why Fixna</p>
            <h2 id="why-title">An engineering-first approach that stays useful as the business changes.</h2>
          </div>
          <div className="grid why-grid">
            {[
              ["Engineering-first", "Architecture before unnecessary technology."],
              ["Product mindset", "We think beyond individual features."],
              ["Cloud-native", "Built for modern deployment and operations."],
              ["AI-ready", "AI is applied where it creates measurable value."],
              ["Observable", "Systems should be measurable and diagnosable."],
              ["Scalable by design", "Start simple, evolve when the business requires it."],
            ].map(([title, text]) => (
              <article className="card" key={title}><h3>{title}</h3><p>{text}</p></article>
            ))}
          </div>
        </div>
      </section>

      <section className="cta-band" aria-labelledby="cta-title">
        <div className="container">
          <div className="cta-card">
            <div>
              <p className="eyebrow" style={{ color: "#c7ed94" }}>Start a conversation</p>
              <h2 id="cta-title">Have a business or technology problem to solve?</h2>
              <p>Tell us what you're building, modernizing or automating.</p>
            </div>
            <div className="cta-actions">
              <Link className="button button-primary" href="/contact">Start a Conversation</Link>
              <Link className="button button-ghost" href="/consulting">Explore Consulting</Link>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
