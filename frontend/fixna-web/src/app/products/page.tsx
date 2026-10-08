import Link from "next/link";
import { products } from "@/content/products";

export const metadata = {
  title: "Products",
  description: "Explore Fixna LocalBoost, Fixna Consulting, and the upcoming Fixna Hospitality platform.",
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
    </>
  );
}
