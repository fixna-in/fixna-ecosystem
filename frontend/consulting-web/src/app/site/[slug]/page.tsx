import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchPublicSite } from "@/lib/public-api";

type PageProps = {
  params: Promise<{ slug: string }>;
};

function formatPrice(amount: string | number | null | undefined, currency: string | null | undefined) {
  if (amount == null) return null;
  const value = typeof amount === "string" ? Number(amount) : amount;
  if (Number.isNaN(value)) return null;
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currency ?? "USD",
  }).format(value);
}

export default async function PublicSitePage({ params }: PageProps) {
  const { slug } = await params;
  const site = await fetchPublicSite(slug);
  if (!site) notFound();

  const { profile, services, testimonials } = site;

  return (
    <article className="public-site">
      <header>
        {profile.logoUrl ? (
          <img src={profile.logoUrl} alt="" width={120} height={120} style={{ borderRadius: 12 }} />
        ) : null}
        <h1>{profile.displayName ?? "Consulting"}</h1>
        {profile.tagline ? <p className="muted">{profile.tagline}</p> : null}
        {profile.websiteUrl ? (
          <p>
            <a href={profile.websiteUrl} rel="noopener noreferrer">
              {profile.websiteUrl}
            </a>
          </p>
        ) : null}
      </header>

      {profile.bio ? (
        <section>
          <h2>About</h2>
          <p>{profile.bio}</p>
        </section>
      ) : null}

      {services.length ? (
        <section>
          <h2>Services</h2>
          <ul className="resource-list">
            {services.map((service) => (
              <li key={`${service.name}-${service.sortOrder}`}>
                <strong>{service.name}</strong>
                {service.description ? <p className="muted">{service.description}</p> : null}
                {formatPrice(service.priceAmount, service.priceCurrency) ? (
                  <p>{formatPrice(service.priceAmount, service.priceCurrency)}</p>
                ) : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {testimonials.length ? (
        <section>
          <h2>Testimonials</h2>
          <ul className="resource-list">
            {testimonials.map((testimonial) => (
              <li key={`${testimonial.authorName}-${testimonial.sortOrder}`}>
                <blockquote>{testimonial.quote}</blockquote>
                <p>
                  <strong>{testimonial.authorName}</strong>
                  {testimonial.authorTitle ? <span className="muted"> — {testimonial.authorTitle}</span> : null}
                </p>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <p className="muted">
        Powered by <Link href="/">Fixna Consulting</Link>
      </p>
    </article>
  );
}
