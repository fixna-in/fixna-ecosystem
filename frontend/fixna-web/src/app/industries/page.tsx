import Link from "next/link";
import { SectionHeading } from "@/components/section-heading";

export const metadata = {
  title: "Industries",
  description: "Fixna builds practical technology for local businesses, hospitality, professional services, and enterprise technology teams.",
};

const industries = [
  {
    title: "Local Business",
    description: "Local businesses need clear workflows, better customer engagement, and marketing systems that are easy to operate.",
    problem: "Fragmented tools and limited operational capacity make campaigns difficult to manage consistently.",
    product: "LocalBoost",
  },
  {
    title: "Hospitality",
    description: "Hospitality operations need coordinated service, guest, and business workflows across teams and locations.",
    problem: "Operational complexity creates inconsistent service and makes it harder to scale consistently.",
    product: "Hospitality",
  },
  {
    title: "Professional Services",
    description: "Advisory and service businesses need dependable delivery systems, client visibility, and clear operational processes.",
    problem: "Manual workflows and disconnected tools reduce delivery consistency and visibility.",
    product: "Consulting",
  },
  {
    title: "Enterprise Technology",
    description: "Technology teams need architecture guidance, modernization support, and resilient systems that support growth.",
    problem: "Legacy systems, integrations, and distributed services introduce delivery and operational risk.",
    product: "Technology consulting",
  },
];

export default function IndustriesPage() {
  return (
    <>
      <section className="page-hero page-hero--compact">
        <div className="container">
          <p className="page-kicker">Industries</p>
          <h1>Technology shaped by the realities of each business.</h1>
          <p>Fixna builds products and solutions for the workflows that matter most, without forcing every business into a generic system.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <SectionHeading
            eyebrow="Where we work"
            title="Business context is part of the technical solution."
            description="The right technology depends on how work happens, where the constraints are, and what needs to be reliable in production."
          />
          <div className="industry-grid industry-grid--detail">
            {industries.map((industry) => (
              <article className="industry-card" key={industry.title}>
                <span className="industry-card__index">{industry.title}</span>
                <h3>{industry.title}</h3>
                <p>{industry.description}</p>
                <div>
                  <strong>Business problem</strong>
                  <p>{industry.problem}</p>
                </div>
                <span className="industry-card__product">{industry.product}</span>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="page-section page-section--muted">
        <div className="container section-callout">
          <div>
            <p className="eyebrow">Future products</p>
            <h2>More vertical products are being developed within the Fixna ecosystem.</h2>
          </div>
          <Link className="button button-secondary" href="/products">Explore the product ecosystem</Link>
        </div>
      </section>
    </>
  );
}
