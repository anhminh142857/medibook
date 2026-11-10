const BASE_URL = "http://localhost:8080/api/auth/google";

export async function loginWithGoogle(credential) {
  const response = await fetch(BASE_URL, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ credential }),
  });

  const text = await response.text();
  let data = null;

  if (text.trim()) {
    try {
      data = JSON.parse(text);
    } catch {
      throw new Error(
        `Backend trả dữ liệu không phải JSON (HTTP ${response.status}).`
      );
    }
  }

  if (!response.ok) {
    throw new Error(
      data?.detail ||
        data?.message ||
        `Đăng nhập Google thất bại (HTTP ${response.status}).`
    );
  }

  return data;
}