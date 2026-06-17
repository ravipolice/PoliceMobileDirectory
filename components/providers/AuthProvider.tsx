"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { User } from "firebase/auth";
import { onAuthChange } from "@/lib/firebase/auth";
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase/config";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  isAdmin: false,
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    // Only run on client side
    if (typeof window === "undefined") {
      setLoading(false);
      return;
    }

    let currentCheckUid: string | null = null;

    try {
      const unsubscribe = onAuthChange(async (firebaseUser) => {
        if (firebaseUser) {
          const uid = firebaseUser.uid;
          currentCheckUid = uid;
          setUser(firebaseUser);
          setLoading(true);

          const email = firebaseUser.email?.trim().toLowerCase();

          if (email === "ravipolice@gmail.com" || uid === "k3dvMjtvoWMz6tpl3iLYpxS3NPw1") {
            if (currentCheckUid === uid) {
              setIsAdmin(true);
              setLoading(false);
            }
            return;
          }

          try {
            // Check doc by UID
            const docRefUid = doc(db, "admins", uid);
            const docSnapUid = await getDoc(docRefUid);
            if (currentCheckUid !== uid) return;

            if (docSnapUid.exists()) {
              setIsAdmin(true);
              setLoading(false);
              return;
            }

            // Check doc by email
            if (email) {
              const docRefEmail = doc(db, "admins", email);
              const docSnapEmail = await getDoc(docRefEmail);
              if (currentCheckUid !== uid) return;

              if (docSnapEmail.exists()) {
                setIsAdmin(true);
                setLoading(false);
                return;
              }
            }

            if (currentCheckUid === uid) {
              setIsAdmin(false);
              setLoading(false);
            }
          } catch (error) {
            console.warn("User is not an admin or error checking admin status:", error);
            if (currentCheckUid === uid) {
              setIsAdmin(false);
              setLoading(false);
            }
          }
        } else {
          currentCheckUid = null;
          setUser(null);
          setIsAdmin(false);
          setLoading(false);
        }
      });

      return () => unsubscribe();
    } catch (error) {
      console.error("Error initializing auth:", error);
      setLoading(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

