import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import {
  completeDoctorAppointment,
  confirmDoctorAppointment,
  getDoctorAppointments,
} from "../api/doctorAppointmentApi";

const STATUS_LABELS = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
};

const STATUS_COLORS = {
  PENDING: "bg-warning text-dark",
  CONFIRMED: "bg-primary",
  COMPLETED: "bg-success",
  CANCELLED: "bg-secondary",
};

function normalizeAppointment(appointment) {
  return {
    ...appointment,
    slotDate:
      appointment.slotDate ?? appointment.appointmentDate ?? "",
    startTime:
      appointment.startTime ?? appointment.appointmentTime ?? "",
    endTime: appointment.endTime ?? "",
  };
}

function formatDate(value) {
  if (!value) {
    return "Chưa có ngày";
  }

  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

function formatTime(value) {
  return value ? value.slice(0, 5) : "—";
}

function hasEnded(appointment, now) {
  if (!appointment.slotDate || !appointment.endTime) {
    return false;
  }

  const end = new Date(
    `${appointment.slotDate}T${appointment.endTime}`
  ).getTime();

  return Number.isFinite(end) && now >= end;
}

export default function DoctorAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [filterDate, setFilterDate] = useState("");
  const [filterStatus, setFilterStatus] = useState("");

  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [reloadKey, setReloadKey] = useState(0);
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    if (!success) return;

    const timer = window.setTimeout(() => {
      setSuccess("");
    }, 5000);

    return () => window.clearTimeout(timer);
  }, [success]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNow(Date.now());
    }, 10000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    async function loadAppointments() {
      setLoading(true);
      setError("");

      try {
        const data = await getDoctorAppointments(controller.signal);

        if (!controller.signal.aborted) {
          setAppointments(data.map(normalizeAppointment));
          setNow(Date.now());
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err.message || "Không tải được lịch khám.");
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadAppointments();

    return () => controller.abort();
  }, [reloadKey]);

  async function handleAction(appointment, action) {
    if (busyId !== null) {
      return;
    }

    const isConfirm = action === "confirm";

    const confirmed = window.confirm(
      `${isConfirm ? "Xác nhận" : "Hoàn thành"} lịch hẹn #${
        appointment.id
      } của bệnh nhân ${appointment.patientName || "được chọn"}?`
    );

    if (!confirmed) {
      return;
    }

    setBusyId(appointment.id);
    setError("");
    setSuccess("");

    try {
      const response = isConfirm
        ? await confirmDoctorAppointment(appointment.id)
        : await completeDoctorAppointment(appointment.id);

      const updated = normalizeAppointment(response);

      setAppointments((previous) =>
        previous.map((item) =>
          item.id === updated.id ? updated : item
        )
      );

      setSuccess(
        isConfirm
          ? `Đã xác nhận lịch hẹn #${appointment.id}.`
          : `Đã hoàn thành lịch hẹn #${appointment.id}.`
      );
    } catch (err) {
      setError(err.message || "Không cập nhật được lịch hẹn.");
    } finally {
      setBusyId(null);
    }
  }

  const filteredAppointments = appointments.filter((appointment) => {
    return (
      (!filterDate || appointment.slotDate === filterDate) &&
      (!filterStatus || appointment.status === filterStatus)
    );
  });

  const hasFilters = Boolean(filterDate || filterStatus);

  const pendingCount = appointments.filter(
    (appointment) => appointment.status === "PENDING"
  ).length;

  const confirmedCount = appointments.filter(
    (appointment) => appointment.status === "CONFIRMED"
  ).length;

  const missingScheduleData = appointments.some(
    (appointment) =>
      !appointment.slotDate ||
      !appointment.startTime ||
      !appointment.endTime
  );

  const busy = busyId !== null;

  return (
    <main className="container py-5">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">
            Lịch khám của tôi
          </h1>

          <p className="text-muted mb-0">
            Xem, xác nhận và hoàn thành các lịch hẹn thuộc hồ sơ bác sĩ của bạn.
          </p>
        </div>

        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={loading || busy}
            onClick={() => {
              setSuccess("");
              setReloadKey((value) => value + 1);
            }}
          >
            Tải lại
          </button>

          <Link to="/" className="btn btn-outline-secondary">
            Về trang chủ
          </Link>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {success && (
        <div className="alert alert-success" role="status">
          {success}
        </div>
      )}

      {!loading && missingScheduleData && (
        <div className="alert alert-warning" role="alert">
          Một số lịch hẹn thiếu trường ngày hoặc giờ trong response.
          Chưa thể xác định thời điểm hoàn thành. Cần kiểm tra
          AppointmentResponse.java trước khi sửa tiếp.
        </div>
      )}

      <div className="card shadow-sm">
        <div className="card-body">
          <div className="row g-3 align-items-end mb-4">
            <div className="col-12 col-md-4">
              <label htmlFor="doctor-filter-date" className="form-label">
                Ngày khám
              </label>

              <input
                id="doctor-filter-date"
                type="date"
                className="form-control"
                value={filterDate}
                onChange={(event) => setFilterDate(event.target.value)}
              />
            </div>

            <div className="col-12 col-md-4">
              <label
                htmlFor="doctor-filter-status"
                className="form-label"
              >
                Trạng thái
              </label>

              <select
                id="doctor-filter-status"
                className="form-select"
                value={filterStatus}
                onChange={(event) => setFilterStatus(event.target.value)}
              >
                <option value="">Tất cả trạng thái</option>

                {Object.entries(STATUS_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-4">
              <button
                type="button"
                className="btn btn-outline-secondary w-100"
                onClick={() => {
                  setFilterDate("");
                  setFilterStatus("");
                }}
              >
                Xóa bộ lọc
              </button>
            </div>
          </div>

          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary mb-3" role="status">
                <span className="visually-hidden">Đang tải...</span>
              </div>

              <p className="text-muted mb-0">Đang tải lịch khám...</p>
            </div>
          ) : appointments.length === 0 ? (
            <div className="text-center py-5">
              <h3 className="h5 mb-2">Chưa có lịch hẹn nào</h3>

              <p className="text-muted mb-0">
                Các lịch hẹn bệnh nhân đặt với hồ sơ bác sĩ của bạn sẽ hiển thị tại đây.
              </p>
            </div>
          ) : (
            <>
              <div className="d-flex flex-wrap gap-2 mb-3">
                <span className="badge bg-primary-subtle text-primary-emphasis">
                  Tổng cộng: {appointments.length} lịch
                </span>

                <span className="badge bg-warning-subtle text-warning-emphasis">
                  Chờ xác nhận: {pendingCount}
                </span>

                <span className="badge bg-primary-subtle text-primary-emphasis">
                  Đã xác nhận: {confirmedCount}
                </span>
              </div>

              <p className="small text-muted">
                Hiển thị {filteredAppointments.length}/
                {appointments.length} lịch hẹn
              </p>

              {filteredAppointments.length === 0 ? (
                <div className="alert alert-info mb-0">
                  {hasFilters
                    ? "Không có lịch hẹn phù hợp với bộ lọc hiện tại."
                    : "Không có lịch hẹn nào."}
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover align-middle">
                    <thead>
                      <tr>
                        <th scope="col">Mã lịch</th>
                        <th scope="col">Bệnh nhân</th>
                        <th scope="col">Ngày khám</th>
                        <th scope="col">Giờ khám</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col">Thao tác</th>
                      </tr>
                    </thead>

                    <tbody>
                      {filteredAppointments.map((appointment) => {
                        const canComplete = hasEnded(appointment, now);
                        const isProcessing = busyId === appointment.id;

                        return (
                          <tr key={appointment.id}>
                            <td>#{appointment.id}</td>

                            <td className="fw-semibold">
                              {appointment.patientName ||
                                `Bệnh nhân #${appointment.patientId}`}
                            </td>

                            <td className="text-nowrap">
                              {formatDate(appointment.slotDate)}
                            </td>

                            <td className="text-nowrap">
                              {formatTime(appointment.startTime)}
                              {" – "}
                              {formatTime(appointment.endTime)}
                            </td>

                            <td>
                              <span
                                className={`badge ${
                                  STATUS_COLORS[appointment.status] ||
                                  "bg-secondary"
                                }`}
                              >
                                {STATUS_LABELS[appointment.status] ||
                                  appointment.status}
                              </span>
                            </td>

                            <td>
                              {appointment.status === "PENDING" && (
                                <button
                                  type="button"
                                  className="btn btn-sm btn-primary"
                                  disabled={busy}
                                  onClick={() =>
                                    handleAction(appointment, "confirm")
                                  }
                                >
                                  {isProcessing
                                    ? "Đang xử lý..."
                                    : "Xác nhận"}
                                </button>
                              )}

                              {appointment.status === "CONFIRMED" && (
                                <div>
                                  <button
                                    type="button"
                                    className="btn btn-sm btn-success"
                                    disabled={busy || !canComplete}
                                    onClick={() =>
                                      handleAction(appointment, "complete")
                                    }
                                  >
                                    {isProcessing
                                      ? "Đang xử lý..."
                                      : "Hoàn thành"}
                                  </button>

                                  {!canComplete && (
                                    <div className="small text-muted mt-1">
                                      Chỉ hoàn thành khi đã đến giờ kết thúc.
                                    </div>
                                  )}
                                </div>
                              )}

                              {!["PENDING", "CONFIRMED"].includes(
                                appointment.status
                              ) && (
                                <span className="text-muted">—</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </main>
  );
}