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
            <li><Link href="/what-we-do">What we do</Link></li>
            <li><Link href="/consulting">Consulting services</Link></li>
            <li><Link href="/technology">Technology</Link></li>
            <li><Link href="/industries">Industries</Link></li>
            <li><Link href="/products">Products</Link></li>
            <li><Link href="/contact">Start a conversation</Link></li>
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
