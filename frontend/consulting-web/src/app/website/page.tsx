"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import {
  createTestimonial,
  deleteTestimonial,
  getProfile,
  listTestimonials,
  ProfileInputSchema,
  TestimonialInputSchema,
  updateProfile,
  updateTestimonial,
  type ProfileInput,
  type TestimonialInput,
} from "@/lib/website-api";
import { ErrorState, LoadingState } from "../providers";

export default function WebsitePage() {
  const [error, setError] = useState<unknown>(null);
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: getProfile, retry: false });
  const testimonialsQuery = useQuery({
    queryKey: ["testimonials"],
    queryFn: listTestimonials,
    retry: false,
  });

  const profileForm = useForm<ProfileInput>({
    resolver: zodResolver(ProfileInputSchema),
    values: profileQuery.data
      ? {
          displayName: profileQuery.data.displayName ?? "",
          tagline: profileQuery.data.tagline ?? "",
          bio: profileQuery.data.bio ?? "",
          websiteUrl: profileQuery.data.websiteUrl ?? "",
          logoUrl: profileQuery.data.logoUrl ?? "",
          publicSlug: profileQuery.data.publicSlug ?? "",
          published: profileQuery.data.published,
        }
      : undefined,
  });

  const testimonialForm = useForm<TestimonialInput>({
    resolver: zodResolver(TestimonialInputSchema),
    defaultValues: { authorName: "", authorTitle: "", quote: "", published: false, sortOrder: 0 },
  });

  return (
    <section aria-label="Website">
      <h1>Public website</h1>
      <p className="muted">Manage your marketing profile, publish settings, and testimonials.</p>

      {profileQuery.isLoading ? <LoadingState label="Loading profile" /> : null}
      {profileQuery.error ? <ErrorState error={profileQuery.error} /> : null}

      {profileQuery.data ? (
        <>
          {profileQuery.data.publicSlug && profileQuery.data.published ? (
            <p>
              Live site:{" "}
              <Link href={`/site/${profileQuery.data.publicSlug}`}>
                /site/{profileQuery.data.publicSlug}
              </Link>
            </p>
          ) : (
            <p className="muted">Set a slug and publish to make your site public.</p>
          )}

          <form
            onSubmit={profileForm.handleSubmit(async (values) => {
              setError(null);
              try {
                await updateProfile({
                  ...values,
                  publicSlug: values.publicSlug?.trim() || undefined,
                });
                profileQuery.refetch();
              } catch (err) {
                setError(err);
              }
            })}
          >
            <h2>Profile</h2>
            <label>
              Display name
              <input {...profileForm.register("displayName")} />
            </label>
            <label>
              Tagline
              <input {...profileForm.register("tagline")} />
            </label>
            <label>
              Bio
              <textarea {...profileForm.register("bio")} rows={4} />
            </label>
            <label>
              Website URL
              <input {...profileForm.register("websiteUrl")} />
            </label>
            <label>
              Logo URL
              <input {...profileForm.register("logoUrl")} />
            </label>
            <label>
              Public slug
              <input {...profileForm.register("publicSlug")} placeholder="my-consulting-firm" />
            </label>
            <label>
              <input type="checkbox" {...profileForm.register("published")} />
              Published
            </label>
            <button type="submit" disabled={profileForm.formState.isSubmitting}>
              {profileForm.formState.isSubmitting ? "Saving…" : "Save profile"}
            </button>
          </form>
        </>
      ) : null}

      <form
        onSubmit={testimonialForm.handleSubmit(async (values) => {
          setError(null);
          try {
            await createTestimonial(values);
            testimonialForm.reset({
              authorName: "",
              authorTitle: "",
              quote: "",
              published: false,
              sortOrder: 0,
            });
            testimonialsQuery.refetch();
          } catch (err) {
            setError(err);
          }
        })}
      >
        <h2>Add testimonial</h2>
        <label>
          Author name
          <input {...testimonialForm.register("authorName")} />
        </label>
        {testimonialForm.formState.errors.authorName ? (
          <p role="alert">{testimonialForm.formState.errors.authorName.message}</p>
        ) : null}
        <label>
          Author title
          <input {...testimonialForm.register("authorTitle")} />
        </label>
        <label>
          Quote
          <textarea {...testimonialForm.register("quote")} rows={3} />
        </label>
        <label>
          Sort order
          <input type="number" {...testimonialForm.register("sortOrder", { valueAsNumber: true })} />
        </label>
        <label>
          <input type="checkbox" {...testimonialForm.register("published")} />
          Published
        </label>
        <button type="submit" disabled={testimonialForm.formState.isSubmitting}>
          {testimonialForm.formState.isSubmitting ? "Adding…" : "Add testimonial"}
        </button>
      </form>

      {error ? <ErrorState error={error} /> : null}
      {testimonialsQuery.isLoading ? <LoadingState label="Loading testimonials" /> : null}
      {testimonialsQuery.error ? <ErrorState error={testimonialsQuery.error} /> : null}
      {testimonialsQuery.data?.length ? (
        <ul className="resource-list">
          {testimonialsQuery.data.map((testimonial) => (
            <li key={testimonial.id}>
              <blockquote>{testimonial.quote}</blockquote>
              <p>
                <strong>{testimonial.authorName}</strong>
                {testimonial.authorTitle ? <span className="muted"> — {testimonial.authorTitle}</span> : null}
              </p>
              <p className="muted">
                {testimonial.published ? "Published" : "Draft"} · sort {testimonial.sortOrder}
              </p>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await updateTestimonial(testimonial.id, {
                      authorName: testimonial.authorName,
                      authorTitle: testimonial.authorTitle ?? undefined,
                      quote: testimonial.quote,
                      published: !testimonial.published,
                      sortOrder: testimonial.sortOrder,
                    });
                    testimonialsQuery.refetch();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                {testimonial.published ? "Unpublish" : "Publish"}
              </button>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await deleteTestimonial(testimonial.id);
                    testimonialsQuery.refetch();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Delete
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">No testimonials yet.</p>
      )}
    </section>
  );
}
