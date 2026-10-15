import Link from "next/link";
import { products } from "@/content/products";
import { canonicalFor } from "@/lib/canonical";

export const metadata = {
  title: "Products",
  description: "Explore Fixna LocalBoost, Fixna Consulting, Fixna Website Audit, Fixna Invoice Assistant, and Fixna Hospitality.",
  ...canonicalFor("/products"),
};

export default function ProductsPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container">
          <p className="page-kicker">Products</p>
          <h1>Software built for specific business outcomes.</h1>
          <p>Each Fixna product is designed as a distinct, independently deployable solution.</p>
        </div>
      </section>

      <section className="page-section">
        <div className="container">
          <div className="grid card-grid-3">
            {products.map((product) => (
              <article className="card product-card" key={product.id}>
                <span className={`status ${product.status === "LIVE" ? "live" : product.status === "COMING SOON" ? "coming-soon" : ""}`}>{product.status}</span>
                <h3>{product.name}</h3>
                <p>{product.description}</p>
                <p className="muted-text">{product.statusNote}</p>
                <p>{product.category}</p>
                <div className="card-list">
                  {product.capabilities.map((capability) => <span key={capability}>{capability}</span>)}
                </div>
                {product.url ? (
                  <Link className="button button-primary" href={product.url} target="_blank" rel="noreferrer">Explore {product.name}</Link>
                ) : (
                  <span className="button button-secondary" aria-disabled="true">Coming Soon</span>
                )}
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="page-section page-section--muted">
        <div className="container narrow">
          <h2>Each product earns its place.</h2>
          <p>
            Fixna ships focused products instead of one overloaded platform.
            Status notes make integration reality explicit before a buyer, reviewer,
            or teammate has to discover it later.
          </p>
        </div>
      </section>
    </>
  );
}
