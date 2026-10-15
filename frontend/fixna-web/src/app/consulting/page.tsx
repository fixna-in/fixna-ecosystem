import Link from "next/link";
import { consultingExpertise, engagementModels } from "@/content/consulting";
import { canonicalFor } from "@/lib/canonical";

export const metadata = {
  title: "Consulting",
  description: "Technology consulting, architecture design, cloud modernization, Kafka, Java, AI, and engineering advisory from Fixna.",
  ...canonicalFor("/consulting"),
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
            <p>
              These service entry points map directly to the expertise and
              engagement models below — each starts with a conversation about
              your requirement.
            </p>
          </div>
          <div className="grid card-grid-3">
            {[
              ["Architecture Review", "Define the technical direction and make trade-offs explicit.", "/contact"],
              ["Cloud Modernization Assessment", "Design cloud-native platforms that are secure, observable, and scalable.", "/contact"],
              ["AI Workflow Assessment", "Identify high-value AI use cases and build responsible workflows.", "/contact"],
            ].map(([title, text, href]) => (
              <article className="card" key={title}>
                <h3>{title}</h3>
                <p>{text}</p>
                <p><Link className="button button-secondary" href={href}>Start with this service</Link></p>
              </article>
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
            <p className="eyebrow">Let’s discuss your requirement</p>
            <h2 id="consulting-cta-title">Tell us what needs to be built, modernized, or improved.</h2>
          </div>
            <Link className="button button-primary" href="/contact">Discuss Your Requirement</Link>
          </div>
        </div>
      </section>
    </>
  );
}
