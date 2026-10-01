import Link from "next/link";

export default function HomePage() {
  return (
    <section className="consulting-home" aria-labelledby="home-title">
      <div className="home-intro">
        <p className="eyebrow">FIXNA / CONSULTING WORKSPACE</p>
        <h1 id="home-title">Good work starts<br />with a clear view.</h1>
        <p className="home-summary">
          Keep client relationships, delivery, and the business of consulting moving in one place.
        </p>
        <div className="home-actions">
          <Link className="home-primary" href="/clients">Open your workspace <span aria-hidden="true">↗</span></Link>
          <Link className="home-secondary" href="/portal">Client portal</Link>
        </div>
        <div className="home-index" aria-hidden="true">
          <span>01&nbsp; RELATIONSHIPS</span><i /><span>02&nbsp; DELIVERY</span><i /><span>03&nbsp; GROWTH</span>
        </div>
      </div>

      <div className="workspace-links">
        <div className="workspace-heading">
          <div>
            <p className="eyebrow">YOUR PRACTICE, IN MOTION</p>
            <h2>Go to a workspace</h2>
          </div>
          <Link href="/register">Create a workspace <span aria-hidden="true">↗</span></Link>
        </div>
        <nav className="workspace-grid" aria-label="Consulting workspace">
          <Link href="/clients"><span className="link-number">01</span><span><strong>Clients</strong><small>Relationships and contacts</small></span><b aria-hidden="true">↗</b></Link>
          <Link href="/projects"><span className="link-number">02</span><span><strong>Projects</strong><small>Engagements and delivery</small></span><b aria-hidden="true">↗</b></Link>
          <Link href="/meetings"><span className="link-number">03</span><span><strong>Meetings</strong><small>Keep conversations on track</small></span><b aria-hidden="true">↗</b></Link>
          <Link href="/proposals"><span className="link-number">04</span><span><strong>Proposals</strong><small>Shape the next opportunity</small></span><b aria-hidden="true">↗</b></Link>
          <Link href="/invoices"><span className="link-number">05</span><span><strong>Invoices</strong><small>Billing and payments</small></span><b aria-hidden="true">↗</b></Link>
          <Link href="/website"><span className="link-number">06</span><span><strong>Website</strong><small>Your public consulting profile</small></span><b aria-hidden="true">↗</b></Link>
          <Link href="/ai"><span className="link-number">07</span><span><strong>AI assistant</strong><small>Practical, reviewed suggestions</small></span><b aria-hidden="true">↗</b></Link>
        </nav>
      </div>
    </section>
  );
}
