const BASE_URL = "http://localhost:8080/api";

async function getList(path, signal) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Không tải được dữ liệu (HTTP ${response.status}).`);
  }

  const data = await response.json();

  if (!Array.isArray(data)) {
    throw new Error("API không trả về danh sách hợp lệ.");
  }

  return data;
}

export function getDoctors(signal) {
  return getList("/doctors", signal);
}

export function getAppointmentSlots(signal) {
  return getList("/appointment-slots", signal);
}