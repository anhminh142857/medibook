const BASE_URL = "http://localhost:8080/api/appointments";

function clearLoginState() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");

  window.dispatchEvent(new Event("auth-changed"));
}

async function sendRequest(path, options = {}) {
  const token = localStorage.getItem("token");

  if (
    !token ||
    token.trim() === "" ||
    token === "null" ||
    token === "undefined"
  ) {
    clearLoginState();

    throw new Error(
      "Bạn chưa đăng nhập. Vui lòng đăng nhập trước."
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
    clearLoginState();

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

    if (response.status === 409) {
      throw new Error(
        data?.detail ||
          data?.message ||
          "Lịch hẹn không còn ở trạng thái cho phép thực hiện thao tác."
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

export async function getMyAppointments(signal) {
  const data = await sendRequest("/me", {
    method: "GET",
    signal,
  });

  if (!Array.isArray(data)) {
    throw new Error(
      "API danh sách lịch hẹn không trả về mảng. Hãy kiểm tra Response trong Network."
    );
  }

  return data;
}

export async function cancelAppointment(appointmentId) {
  return sendRequest(`/${appointmentId}/cancel`, {
    method: "POST",
  });
}

export async function createAppointment(slotId) {
  return sendRequest("", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ slotId }),
  });
}