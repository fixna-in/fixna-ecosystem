import Link from "next/link";
import { footerNavigation } from "@/content/navigation";
import { FixnaBrandMark } from "@/components/brand-mark";

export function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-grid">
        <div>
          <Link className="brand footer-brand" href="/" aria-label="Fixna home">
            <FixnaBrandMark />
            <span className="brand-wordmark"><span>Fixna</span></span>
          </Link>
          <p className="footer-description">
            Technology and product engineering for practical software that solves real business problems.
          </p>
        </div>

        <div>
          <h2>Explore</h2>
          <nav aria-label="Footer navigation">
            <ul className="footer-nav">
              {footerNavigation.map((item) => (
                <li key={item.href}><Link href={item.href}>{item.label}</Link></li>
              ))}
            </ul>
          </nav>
        </div>

        <div>
          <h2>Products</h2>
          <ul className="footer-nav">
            <li><a href="https://app.fixna.in" target="_blank" rel="noreferrer">LocalBoost</a></li>
            <li><a href="https://consulting.fixna.in" target="_blank" rel="noreferrer">Consulting</a></li>
            <li><Link href="/products">Hospitality <span aria-label="Coming soon">— Coming Soon</span></Link></li>
          </ul>
        </div>

        <div>
          <h2>Technology</h2>
          <ul className="footer-nav">
            <li><span>Java</span></li>
            <li><span>Cloud</span></li>
            <li><span>Kafka</span></li>
            <li><span>AI</span></li>
            <li><span>Observability</span></li>
          </ul>
        </div>
      </div>

      <div className="container footer-bottom">
        <p>© {new Date().getFullYear()} Fixna.</p>
        <div className="footer-links">
          <a href="https://github.com/fixna-in" target="_blank" rel="noreferrer" aria-label="Fixna GitHub">GitHub</a>
        </div>
      </div>
    </footer>
  );
}
