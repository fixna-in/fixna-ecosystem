import { technologyGroups } from "@/content/technology";
import { canonicalFor } from "@/lib/canonical";

export const metadata = {
  title: "Technology",
  description: "Fixna uses Java, Spring Boot, cloud-native platforms, Kafka, PostgreSQL, Redis, OpenTelemetry, Next.js, React, TypeScript, and AI technologies.",
  ...canonicalFor("/technology"),
};

export default function TechnologyPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container">
          <p className="page-kicker">Technology</p>
          <h1>Solutions built around the right technology choices.</h1>
          <p>We choose established, maintainable tools and architectures based on business needs and operational realities.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="grid technology-grid">
            {technologyGroups.map((group) => (
              <article className="card technology-group" key={group.title}>
                <h3>{group.title}</h3>
                <ul>{group.items.map((item) => <li key={item}>{item}</li>)}</ul>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="page-section page-section--muted">
        <div className="container narrow">
          <h2>Practical choices, not novelty for its own sake.</h2>
          <p>
            Fixna prefers mature tools with clear operational models: readable
            code, observable services, well-understood data stores, and deployment
            paths that teams can support after launch.
          </p>
        </div>
      </section>
    </>
  );
}
