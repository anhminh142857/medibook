const BASE_URL = "http://localhost:8080/api/admin/doctors";

export async function linkDoctorAccount(doctorId, email) {
  const token = localStorage.getItem("token");

  if (
    !token ||
    token.trim() === "" ||
    token === "null" ||
    token === "undefined"
  ) {
    throw new Error("Vui lòng đăng nhập bằng tài khoản admin.");
  }

  const response = await fetch(`${BASE_URL}/${doctorId}/account`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      email: email.trim(),
    }),
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
        "Bạn không có quyền liên kết tài khoản bác sĩ."
      );
    }

    throw new Error(
      data?.detail ||
        data?.message ||
        `Liên kết tài khoản thất bại (HTTP ${response.status}).`
    );
  }

  return data;
}