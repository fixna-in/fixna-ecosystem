import { technologyGroups } from "@/content/technology";

export const metadata = {
  title: "Technology",
  description: "Fixna uses Java, Spring Boot, cloud-native platforms, Kafka, PostgreSQL, Redis, OpenTelemetry, Next.js, React, TypeScript, and AI technologies.",
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
    </>
  );
}
