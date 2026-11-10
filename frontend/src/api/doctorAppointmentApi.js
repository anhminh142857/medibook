const BASE_URL = "http://localhost:8080/api/doctor/appointments";

async function sendRequest(path = "", options = {}) {
  const token = localStorage.getItem("token");

  if (
    !token ||
    token.trim() === "" ||
    token === "null" ||
    token === "undefined"
  ) {
    throw new Error("Vui lòng đăng nhập bằng tài khoản bác sĩ.");
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
        data?.detail ||
          data?.message ||
          "Tài khoản không có quyền bác sĩ hoặc chưa được liên kết hồ sơ."
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

export async function getDoctorAppointments(signal) {
  const data = await sendRequest("", {
    method: "GET",
    signal,
  });

  if (!Array.isArray(data)) {
    throw new Error(
      "API lịch khám bác sĩ không trả về mảng."
    );
  }

  return data;
}

export async function confirmDoctorAppointment(appointmentId) {
  return sendRequest(`/${appointmentId}/confirm`, {
    method: "POST",
  });
}

export async function completeDoctorAppointment(appointmentId) {
  return sendRequest(`/${appointmentId}/complete`, {
    method: "POST",
  });
}