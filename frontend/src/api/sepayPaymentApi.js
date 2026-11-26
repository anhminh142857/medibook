const BASE_URL = "http://localhost:8080/api/payments/sepay";

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
    throw new Error("Bạn chưa đăng nhập. Vui lòng đăng nhập lại.");
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
        data?.detail ||
          data?.message ||
          "Bạn không có quyền truy cập giao dịch này."
      );
    }

    throw new Error(
      data?.detail ||
        data?.message ||
        `Yêu cầu thanh toán thất bại (HTTP ${response.status}).`
    );
  }

  return data;
}

export async function getSepayPaymentsForAppointment(
  appointmentId,
  signal
) {
  const data = await sendRequest(
    `/appointments/${appointmentId}`,
    {
      method: "GET",
      signal,
    }
  );

  if (!Array.isArray(data)) {
    throw new Error("API thanh toán không trả về danh sách hợp lệ.");
  }

  return data;
}

export function createSepayPayment(appointmentId) {
  return sendRequest(`/appointments/${appointmentId}`, {
    method: "POST",
  });
}
