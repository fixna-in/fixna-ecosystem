ALTER TABLE consulting_profiles
    ADD COLUMN public_slug VARCHAR(100) UNIQUE,
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE testimonials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    author_name VARCHAR(200) NOT NULL,
    author_title VARCHAR(200),
    quote TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_testimonials_tenant_id ON testimonials(tenant_id);
