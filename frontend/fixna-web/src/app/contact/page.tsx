import { productLinks } from "@/lib/product-links";
import { ContactForm } from "./contact-form";

export const metadata = {
  title: "Contact",
  description: "Start a conversation with Fixna about your product, technology, cloud, automation, or consulting requirement.",
};

export default function ContactPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container">
          <p className="page-kicker">Contact</p>
          <h1>Tell us what you are building.</h1>
          <p>We will help you turn the idea, problem, or modernization requirement into a clear engineering path.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container contact-grid">
          <div className="contact-card">
            <p className="eyebrow">Start a conversation</p>
            <h2>We would like to understand your requirement.</h2>
            <p>Share the context, constraints, and desired outcome. You can also use the same information to request architecture, product engineering, or consulting support.</p>
            <p><strong>Email:</strong> <a href="mailto:hello@fixna.in">hello@fixna.in</a></p>
            <p><strong>Product links:</strong> <a href={productLinks.localboost} target="_blank" rel="noreferrer">LocalBoost</a> · <a href={productLinks.consulting} target="_blank" rel="noreferrer">Consulting</a></p>
          </div>

          <div className="contact-card">
            <ContactForm />
          </div>
        </div>
      </section>
    </>
  );
}
