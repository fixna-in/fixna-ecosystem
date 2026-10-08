export const metadata = {
  title: "Terms",
  description: "Fixna website terms and conditions.",
};

export default function TermsPage() {
  return (
    <main className="legal-page">
      <section className="page-hero page-hero--compact">
        <div className="container narrow">
          <p className="page-kicker">Terms</p>
          <h1>Website Terms</h1>
          <p>These terms govern use of the Fixna public website.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container narrow prose">
          <p>The Fixna website is provided for informational purposes. Content may change without notice.</p>
          <p>Material on this website should not be treated as a guarantee, promise, or binding commercial offer. Any engagement requires a separate written agreement.</p>
          <h2>Use of content</h2>
          <p>You may use the website for lawful purposes. You may not use it to interfere with its operation, access protected systems, or submit harmful or unlawful content.</p>
          <h2>Third-party links</h2>
          <p>Fixna provides links to product and external services for convenience. We do not control or guarantee those services.</p>
          <h2>Limitation of liability</h2>
          <p>Fixna is not liable for indirect, incidental, consequential, or punitive damages arising from use of the website or its content, except where required by law.</p>
          <h2>Changes</h2>
          <p>Fixna may update these terms at any time. Continued use of the website after changes are published indicates acceptance of the updated terms.</p>
        </div>
      </section>
    </main>
  );
}
