'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import apiClient from '@/lib/api-client';

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const payload =
        mode === 'login'
          ? { email, password }
          : { email, password, firstName: firstName || undefined, lastName: lastName || undefined };
      const { data } = await apiClient.post(`/v1/auth/${mode}`, payload);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      router.push('/');
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Authentication failed';
      setError(message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="min-h-screen flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-surface border border-border rounded-xl p-8 shadow-sm">
        <Link href="/" className="block text-center text-2xl font-bold text-foreground mb-1">
          Fixna Website Audit
        </Link>
        <p className="text-center text-sm text-muted-foreground mb-6">
          {mode === 'login' ? 'Sign in to your workspace' : 'Create your workspace'}
        </p>

        <form onSubmit={onSubmit} className="space-y-4">
          <input
            type="email"
            required
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 border border-border rounded-lg bg-input text-foreground"
          />
          <input
            type="password"
            required
            minLength={8}
            placeholder="Password (min 8 characters)"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border border-border rounded-lg bg-input text-foreground"
          />
          {mode === 'register' && (
            <div className="grid grid-cols-2 gap-3">
              <input
                placeholder="First name"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="w-full px-3 py-2 border border-border rounded-lg bg-input text-foreground"
              />
              <input
                placeholder="Last name"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="w-full px-3 py-2 border border-border rounded-lg bg-input text-foreground"
              />
            </div>
          )}
          {error && <p className="text-sm text-danger">{error}</p>}
          <button
            type="submit"
            disabled={busy}
            className="w-full px-4 py-2 font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover disabled:opacity-50"
          >
            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create workspace'}
          </button>
        </form>

        <button
          type="button"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login');
            setError(null);
          }}
          className="mt-4 w-full text-sm text-muted-foreground hover:text-foreground"
        >
          {mode === 'login' ? 'New here? Create a workspace' : 'Already registered? Sign in'}
        </button>
      </div>
    </main>
  );
}
