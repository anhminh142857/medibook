const BASE_URL = "http://localhost:8080/api/specialties";

async function sendRequest(path, options = {}) {
  const token = localStorage.getItem("token");

  const hasToken =
    token &&
    token.trim() !== "" &&
    token !== "null" &&
    token !== "undefined";

  const method = options.method || "GET";

  if (method !== "GET" && !hasToken) {
    throw new Error("Vui lòng đăng nhập để thực hiện thao tác này.");
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...options.headers,
      ...(hasToken ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (response.status === 401) {
    if (hasToken) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.dispatchEvent(new Event("auth-changed"));
    }

    throw new Error(
      hasToken
        ? "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."
        : "Backend chưa cho phép truy cập dữ liệu này khi chưa đăng nhập."
    );
  }

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
        (response.status === 403
          ? "Bạn không có quyền thực hiện thao tác này."
          : `Yêu cầu thất bại (HTTP ${response.status}).`)
    );
  }

  return data;
}

export async function getSpecialties(signal) {
  const data = await sendRequest("", { method: "GET", signal });

  if (!Array.isArray(data)) {
    throw new Error("API danh sách chuyên khoa không trả về mảng.");
  }

  return data;
}

export function createSpecialty(payload) {
  return sendRequest("", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function updateSpecialty(id, payload) {
  return sendRequest(`/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export function deleteSpecialty(id) {
  return sendRequest(`/${id}`, { method: "DELETE" });
}