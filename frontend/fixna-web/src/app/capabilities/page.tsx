import { capabilities } from "@/content/capabilities";

export const metadata = {
  title: "Capabilities",
  description: "Product engineering, solution architecture, modernization, event-driven systems, AI, automation, observability, and technical consulting.",
};

export default function CapabilitiesPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container">
          <p className="page-kicker">Capabilities</p>
          <h1>Turn technical complexity into practical delivery.</h1>
          <p>Fixna helps businesses define the right problem, design the right system, and deliver it with confidence.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="grid card-grid-3">
            {capabilities.map((capability, index) => (
              <article className="card" key={capability.title}>
                <span className="card-index">0{index + 1}</span>
                <h3>{capability.title}</h3>
                <p>{capability.description}</p>
                <ul className="content-list" style={{ marginTop: "1.25rem" }}>
                  <li><strong>Problem</strong><span>{capability.problem}</span></li>
                  <li><strong>What Fixna can do</strong><span>{capability.description}</span></li>
                  <li><strong>Typical outcome</strong><span>{capability.outcome}</span></li>
                </ul>
              </article>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
