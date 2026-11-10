const BASE_URL = "http://localhost:8080/api/doctors";

async function sendRequest(path, options = {}) {
  const token = localStorage.getItem("token");

  const hasToken =
    token &&
    token.trim() !== "" &&
    token !== "null" &&
    token !== "undefined";

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...options.headers,
      ...(hasToken
        ? { Authorization: `Bearer ${token}` }
        : {}),
    },
  });

  // Xử lý 401 trước khi đọc body.
  if (response.status === 401) {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.dispatchEvent(new Event("auth-changed"));

    throw new Error(
      "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."
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
    if (response.status === 403) {
      throw new Error(
        "Bạn không có quyền thực hiện thao tác này."
      );
    }

    throw new Error(
      data?.detail ||
        data?.message ||
        `Yêu cầu thất bại (HTTP ${response.status}).`
    );
  }

  return data;
}

export async function getDoctors(signal) {
  const data = await sendRequest("", {
    method: "GET",
    signal,
  });

  if (!Array.isArray(data)) {
    throw new Error(
      "API danh sách bác sĩ không trả về mảng."
    );
  }

  return data;
}

export async function createDoctor(payload) {
  return sendRequest("", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}

export async function updateDoctor(id, payload) {
  return sendRequest(`/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}