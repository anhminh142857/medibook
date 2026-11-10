import { useEffect, useRef, useState } from "react";

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const SCRIPT_SRC = "https://accounts.google.com/gsi/client";

let scriptPromise = null;

function loadGoogleScript() {
  if (window.google?.accounts?.id) {
    return Promise.resolve();
  }

  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");

      script.src = SCRIPT_SRC;
      script.async = true;
      script.defer = true;
      script.onload = resolve;
      script.onerror = () => {
        scriptPromise = null;
        reject(new Error("Không tải được thư viện đăng nhập Google."));
      };

      document.head.appendChild(script);
    });
  }

  return scriptPromise;
}

export default function GoogleLoginButton({ onCredential }) {
  const containerRef = useRef(null);
  const callbackRef = useRef(onCredential);
  const [error, setError] = useState("");

  useEffect(() => {
    callbackRef.current = onCredential;
  }, [onCredential]);

  useEffect(() => {
    if (!CLIENT_ID || CLIENT_ID.includes("DAN_CLIENT_ID")) {
      setError("Chưa cấu hình VITE_GOOGLE_CLIENT_ID trong frontend/.env.");
      return undefined;
    }

    let cancelled = false;

    loadGoogleScript()
      .then(() => {
        if (cancelled || !containerRef.current) {
          return;
        }

        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) => {
            if (response?.credential) {
              callbackRef.current(response.credential);
            }
          },
        });

        containerRef.current.innerHTML = "";

        window.google.accounts.id.renderButton(containerRef.current, {
          theme: "outline",
          size: "large",
          text: "signin_with",
          shape: "rectangular",
          width: 320,
          locale: "vi",
        });
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      {error && (
        <div className="alert alert-warning small" role="alert">
          {error}
        </div>
      )}

      <div ref={containerRef} className="d-flex justify-content-center" />
    </div>
  );
}