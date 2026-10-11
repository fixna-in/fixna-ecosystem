import { canonicalFor } from "@/lib/canonical";

export const metadata = {
  title: "Privacy Policy",
  description: "Fixna privacy policy and information-handling practices.",
  ...canonicalFor("/privacy-policy"),
};

export default function PrivacyPolicyPage() {
  return (
    <main className="legal-page">
      <section className="page-hero page-hero--compact">
        <div className="container narrow">
          <p className="page-kicker">Privacy</p>
          <h1>Privacy Policy</h1>
          <p>Fixna respects the privacy of visitors and business contacts.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container narrow prose">
          <p>Fixna may collect information you provide through this website, including your name, company, email address, phone number, and project description submitted through the contact form.</p>
          <p>Contact-form submissions are stored in a private Google Sheets spreadsheet accessible only to Fixna. We use this information only to respond to your enquiry and understand the requirements you provide. We do not sell personal information.</p>
          <h2>Contact information</h2>
          <p>If you have questions about this policy, contact <a href="mailto:fixnainfo@gmail.com">fixnainfo@gmail.com</a>.</p>
          <h2>Website analytics</h2>
          <p>This website does not currently load third-party analytics scripts. Like any web host, our hosting provider (Vercel) processes standard request logs — such as IP address, user agent, and requested path — for operation and abuse prevention.</p>
          <h2>Product applications</h2>
          <p>The LocalBoost and Consulting applications are separate products with their own accounts and data. Data you store there is governed by the terms of the product you use, not this website's contact form.</p>
          <h2>Your choices</h2>
          <p>You may contact Fixna to request access, correction, or deletion of information you have provided through the website.</p>
        </div>
      </section>
    </main>
  );
}
