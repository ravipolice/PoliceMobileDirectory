"use client";

import { useState, useEffect } from "react";
import { signInWithGoogle, signOut } from "@/lib/firebase/auth";
import { useRouter } from "next/navigation";
import { Logo } from "@/components/common/Logo";

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [unauthorizedError, setUnauthorizedError] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      if (params.get("error") === "unauthorized") {
        setUnauthorizedError(true);
        signOut().catch((err) => console.error("Error signing out unauthorized user:", err));
      }
    }
  }, []);

  const handleSignIn = async () => {
    setLoading(true);
    setError(null);
    setUnauthorizedError(false);
    try {
      await signInWithGoogle();
      router.push("/");
    } catch (err: any) {
      setError(err.message || "Failed to sign in");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow-lg">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-6 flex h-32 w-32 items-center justify-center">
            <Logo size="lg" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">
            PMD Admin Panel
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            Sign in to access the admin dashboard
          </p>
        </div>

        {unauthorizedError && (
          <div className="mb-4 rounded-lg bg-amber-50 border border-amber-200 p-4 text-sm text-amber-800">
            <p className="font-semibold mb-1">Access Denied</p>
            <p>Your account is not authorized to access this administration panel. Please log in with an administrator account.</p>
          </div>
        )}

        {error && (
          <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        <button
          onClick={handleSignIn}
          disabled={loading}
          className="w-full rounded-lg bg-primary-600 px-4 py-3 font-medium text-white transition-colors hover:bg-primary-700 disabled:opacity-50"
        >
          {loading ? "Signing in..." : "Sign in with Google"}
        </button>

        <p className="mt-4 text-center text-xs text-gray-500">
          Only authorized administrators can access this panel
        </p>
      </div>
    </div>
  );
}

