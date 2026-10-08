import Link from "next/link";
import { consultingExpertise, engagementModels } from "@/content/consulting";

export const metadata = {
  title: "Consulting",
  description: "Technology consulting, architecture design, cloud modernization, Kafka, Java, AI, and engineering advisory from Fixna.",
};

export default function ConsultingPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container">
          <p className="page-kicker">Fixna Consulting</p>
          <h1>Technical leadership for ambitious engineering work.</h1>
          <p>We help teams clarify the architecture, reduce uncertainty, and deliver complex technology initiatives with confidence.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Consulting overview</p>
            <h2>From architecture reviews to implementation support.</h2>
          </div>
          <div className="grid card-grid-3">
            {[
              ["Architecture Consulting", "Define the technical direction and make trade-offs explicit."],
              ["Kafka & Event-Driven Systems", "Design reliable streaming, integration, and asynchronous workflows."],
              ["Java & Spring Boot", "Build maintainable backend systems and practical modernization paths."],
              ["Cloud Architecture", "Design cloud-native platforms that are secure, observable, and scalable."],
              ["AI / GenAI", "Identify high-value AI use cases and build responsible workflows."],
              ["Legacy Modernization", "Reduce risk while modernizing critical systems incrementally."],
            ].map(([title, text]) => (
              <article className="card" key={title}><h3>{title}</h3><p>{text}</p></article>
            ))}
          </div>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Expertise</p>
            <h2>Practical consulting across the engineering lifecycle.</h2>
          </div>
          <div className="grid card-grid-4">
            {consultingExpertise.map((item) => <article className="card" key={item}><h3>{item}</h3></article>)}
          </div>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Engagement models</p>
            <h2>Flexible engagement structures for the work that needs to get done.</h2>
          </div>
          <div className="grid process-grid">
            {engagementModels.map((model, index) => (
              <article className="card process-card" key={model}><strong>{String(index + 1).padStart(2, "0")}</strong><h3>{model}</h3></article>
            ))}
          </div>
        </div>
      </section>

      <section className="cta-band" aria-labelledby="consulting-cta-title">
        <div className="container">
          <div className="cta-card">
            <div>
              <p className="eyebrow" style={{ color: "#c7ed94" }}>Let’s discuss your requirement</p>
              <h2 id="consulting-cta-title">Tell us what needs to be built, modernized, or improved.</h2>
            </div>
            <Link className="button button-primary" href="/contact">Discuss Your Requirement</Link>
          </div>
        </div>
      </section>
    </>
  );
}
