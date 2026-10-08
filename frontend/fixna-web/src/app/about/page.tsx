import { company } from "@/content/company";

const beliefs = [
  ["Clear requirements", "We begin with the business problem and the constraints that matter."],
  ["Practical architecture", "We design for delivery, change, and dependable operations."],
  ["Incremental progress", "We deliver value in clear stages instead of waiting for a perfect version."],
];

export const metadata = {
  title: "About",
  description: "Learn how Fixna combines product engineering, cloud architecture, AI, and consulting to build practical software.",
};

export default function AboutPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container">
          <p className="page-kicker">About Fixna</p>
          <h1>Engineering with purpose.</h1>
          <p>{company.about}</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Who we are</p>
            <h2>Fixna is a technology and product engineering company.</h2>
          </div>
          <div className="grid why-grid">
            <article className="card"><h3>What we believe</h3><p>Software should make business operations clearer, faster, and more reliable.</p></article>
            <article className="card"><h3>What we build</h3><p>Products, platforms, integrations, cloud systems, and AI-enabled solutions.</p></article>
            <article className="card"><h3>Engineering philosophy</h3><p>We favor understandable systems, focused delivery, and measurable outcomes.</p></article>
            <article className="card"><h3>Future vision</h3><p>We build adaptable technology that helps businesses solve increasingly complex problems.</p></article>
          </div>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Our approach</p>
            <h2>Technology should serve the business, not the other way around.</h2>
          </div>
          <ul className="content-list">
            {beliefs.map(([title, description]) => (
              <li key={title}><strong>{title}</strong><span>{description}</span></li>
            ))}
          </ul>
        </div>
      </section>
    </>
  );
}
