import Link from "next/link";
import { FixnaBrandMark } from "@/components/brand-mark";
import { productLinks } from "@/lib/product-links";

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
            Technology • Product Engineering • AI
          </p>
        </div>

        <div>
          <h2>Products</h2>
          <ul className="footer-nav">
            <li><a href={productLinks.localboost} target="_blank" rel="noreferrer">LocalBoost</a></li>
            <li><a href={productLinks.consulting} target="_blank" rel="noreferrer">Consulting</a></li>
            <li><span>Hospitality <em>— Coming Soon</em></span></li>
          </ul>
        </div>

        <div>
          <h2>Capabilities</h2>
          <ul className="footer-nav">
            <li><Link href="/what-we-do">Product Engineering</Link></li>
            <li><Link href="/what-we-do">Cloud &amp; Modernization</Link></li>
            <li><Link href="/what-we-do">Event-Driven Architecture</Link></li>
            <li><Link href="/what-we-do">AI &amp; GenAI</Link></li>
            <li><Link href="/what-we-do">Integration &amp; Automation</Link></li>
            <li><Link href="/what-we-do">Technology Consulting</Link></li>
          </ul>
        </div>

        <div>
          <h2>Company</h2>
          <ul className="footer-nav">
            <li><Link href="/about">About</Link></li>
            <li><Link href="/industries">Industries</Link></li>
            <li><Link href="/contact">Contact</Link></li>
          </ul>
        </div>
      </div>

      <div className="container footer-bottom">
        <p>© {new Date().getFullYear()} Fixna</p>
        <div className="footer-links">
          <Link href="/privacy-policy">Privacy Policy</Link>
          <Link href="/terms">Terms</Link>
        </div>
      </div>
    </footer>
  );
}
