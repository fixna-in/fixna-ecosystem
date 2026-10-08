export const metadata = {
  title: "Privacy Policy",
  description: "Fixna privacy policy and information-handling practices.",
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
          <p>Fixna may collect information you provide through this website, including your name, company, email address, project description, and any optional timeline or budget information.</p>
          <p>We use this information only to respond to your enquiry and understand the requirements you provide. We do not sell personal information.</p>
          <h2>Contact information</h2>
          <p>If you have questions about this policy, contact <a href="mailto:hello@fixna.in">hello@fixna.in</a>.</p>
          <h2>Website analytics</h2>
          <p>Fixna may use standard analytics and platform logs to understand website performance and improve the experience. The specific tools used may vary by deployment.</p>
          <h2>Your choices</h2>
          <p>You may contact Fixna to request access, correction, or deletion of information you have provided through the website.</p>
        </div>
      </section>
    </main>
  );
}
