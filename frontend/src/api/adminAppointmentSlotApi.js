const BASE_URL = "http://localhost:8080/api/appointment-slots";

async function sendRequest(path = "", options = {}) {
  const token = localStorage.getItem("token");

  if (
    !token ||
    token.trim() === "" ||
    token === "null" ||
    token === "undefined"
  ) {
    throw new Error(
      "Vui lòng đăng nhập bằng tài khoản admin."
    );
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  });

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

export async function getAppointmentSlots(signal) {
  const data = await sendRequest("", {
    method: "GET",
    signal,
  });

  if (!Array.isArray(data)) {
    throw new Error(
      "API danh sách khung giờ không trả về mảng."
    );
  }

  return data;
}

export async function createAppointmentSlot(payload) {
  return sendRequest("", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}