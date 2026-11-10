import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getDoctors } from "../api/doctorApi";
import {
  createAppointmentSlot,
  getAppointmentSlots,
} from "../api/adminAppointmentSlotApi";

const EMPTY_FORM = {
  doctorId: "",
  slotDate: "",
  startTime: "",
  endTime: "",
};

function getLocalDateValue() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function formatDate(value) {
  if (!value) {
    return "—";
  }

  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

function formatTime(value) {
  return value ? value.slice(0, 5) : "—";
}

function sortSlots(slots) {
  return [...slots].sort((a, b) => {
    const first = `${a.slotDate}T${a.startTime}`;
    const second = `${b.slotDate}T${b.startTime}`;

    return (
      first.localeCompare(second) ||
      a.doctorName.localeCompare(b.doctorName) ||
      a.id - b.id
    );
  });
}

function StatusBadge({ status }) {
  const isAvailable = status === "AVAILABLE";

  return (
    <span
      className={`badge ${
        isAvailable ? "bg-success" : "bg-secondary"
      }`}
    >
      {isAvailable ? "Còn trống" : status || "Không xác định"}
    </span>
  );
}

export default function AdminAppointmentSlots() {
  const [doctors, setDoctors] = useState([]);
  const [slots, setSlots] = useState([]);
  const [form, setForm] = useState({ ...EMPTY_FORM });

  const [filterDoctorId, setFilterDoctorId] = useState("");
  const [filterDate, setFilterDate] = useState("");
  const [filterStatus, setFilterStatus] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadData() {
      try {
        const [doctorData, slotData] = await Promise.all([
          getDoctors(controller.signal),
          getAppointmentSlots(controller.signal),
        ]);

        if (!controller.signal.aborted) {
          setDoctors(doctorData);
          setSlots(sortSlots(slotData));
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err.message || "Không tải được dữ liệu khung giờ."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadData();

    return () => controller.abort();
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (saving) {
      return;
    }

    setError("");
    setSuccess("");

    const doctorId = Number(form.doctorId);

    if (!Number.isSafeInteger(doctorId) || doctorId <= 0) {
      setError("Vui lòng chọn bác sĩ.");
      return;
    }

    if (!form.slotDate || !form.startTime || !form.endTime) {
      setError("Vui lòng nhập đầy đủ ngày và giờ khám.");
      return;
    }

    if (form.endTime <= form.startTime) {
      setError("Giờ kết thúc phải sau giờ bắt đầu.");
      return;
    }

    const startDateTime = new Date(
      `${form.slotDate}T${form.startTime}`
    );

    if (
      Number.isNaN(startDateTime.getTime()) ||
      startDateTime.getTime() <= Date.now()
    ) {
      setError("Thời điểm bắt đầu khám phải ở tương lai.");
      return;
    }

    setSaving(true);

    try {
      const savedSlot = await createAppointmentSlot({
        doctorId,
        slotDate: form.slotDate,
        startTime: form.startTime,
        endTime: form.endTime,
      });

      setSlots((previous) =>
        sortSlots([...previous, savedSlot])
      );

      // Giữ bác sĩ và ngày để thuận tiện nhập khung giờ kế tiếp.
      setForm((previous) => ({
        ...previous,
        startTime: "",
        endTime: "",
      }));

      // Hiện khung giờ vừa tạo trong danh sách bên phải.
      setFilterDoctorId(String(doctorId));
      setFilterDate(savedSlot.slotDate);
      setFilterStatus("");

      setSuccess(
        `Đã tạo khung giờ ${formatTime(savedSlot.startTime)}–${formatTime(
          savedSlot.endTime
        )}, ngày ${formatDate(savedSlot.slotDate)} cho ${
          savedSlot.doctorName
        }.`
      );
    } catch (err) {
      setError(err.message || "Không tạo được khung giờ.");
    } finally {
      setSaving(false);
    }
  }

  function clearFilters() {
    setFilterDoctorId("");
    setFilterDate("");
    setFilterStatus("");
  }

  const statuses = [
    ...new Set(slots.map((slot) => slot.status).filter(Boolean)),
  ].sort();

  const filteredSlots = slots.filter((slot) => {
    return (
      (!filterDoctorId ||
        String(slot.doctorId) === filterDoctorId) &&
      (!filterDate || slot.slotDate === filterDate) &&
      (!filterStatus || slot.status === filterStatus)
    );
  });

  return (
    <main className="container py-5">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">
            Quản lý khung giờ
          </h1>

          <p className="text-muted mb-0">
            Tạo khung giờ khám và theo dõi lịch của từng bác sĩ.
          </p>
        </div>

        <Link to="/" className="btn btn-outline-secondary">
          Về trang chủ
        </Link>
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

      {loading ? (
        <p className="text-muted">Đang tải dữ liệu...</p>
      ) : (
        <div className="row g-4">
          <div className="col-12 col-lg-4">
            <div className="card shadow-sm">
              <div className="card-body">
                <h2 className="h5 fw-bold mb-3">
                  Tạo khung giờ khám
                </h2>

                {doctors.length === 0 && (
                  <div className="alert alert-warning">
                    Chưa có bác sĩ.{" "}
                    <Link to="/admin/doctors">
                      Thêm bác sĩ trước
                    </Link>
                    , sau đó quay lại trang này.
                  </div>
                )}

                <form onSubmit={handleSubmit}>
                  <fieldset
                    disabled={saving || doctors.length === 0}
                  >
                    <div className="mb-3">
                      <label
                        htmlFor="slot-doctor"
                        className="form-label"
                      >
                        Bác sĩ
                      </label>

                      <select
                        id="slot-doctor"
                        name="doctorId"
                        className="form-select"
                        value={form.doctorId}
                        onChange={handleChange}
                        required
                      >
                        <option value="">
                          -- Chọn bác sĩ --
                        </option>

                        {doctors.map((doctor) => (
                          <option
                            key={doctor.id}
                            value={doctor.id}
                          >
                            {doctor.fullName} — {doctor.specialtyName}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="mb-3">
                      <label
                        htmlFor="slot-date"
                        className="form-label"
                      >
                        Ngày khám
                      </label>

                      <input
                        id="slot-date"
                        name="slotDate"
                        type="date"
                        className="form-control"
                        min={getLocalDateValue()}
                        value={form.slotDate}
                        onChange={handleChange}
                        required
                      />
                    </div>

                    <div className="row g-3 mb-3">
                      <div className="col-6">
                        <label
                          htmlFor="slot-start"
                          className="form-label"
                        >
                          Giờ bắt đầu
                        </label>

                        <input
                          id="slot-start"
                          name="startTime"
                          type="time"
                          step="60"
                          className="form-control"
                          value={form.startTime}
                          onChange={handleChange}
                          required
                        />
                      </div>

                      <div className="col-6">
                        <label
                          htmlFor="slot-end"
                          className="form-label"
                        >
                          Giờ kết thúc
                        </label>

                        <input
                          id="slot-end"
                          name="endTime"
                          type="time"
                          step="60"
                          className="form-control"
                          value={form.endTime}
                          onChange={handleChange}
                          required
                        />
                      </div>
                    </div>

                    <p className="small text-muted">
                      Khung giờ phải ở tương lai và không chồng lấn
                      với khung giờ của cùng bác sĩ trong ngày.
                    </p>

                    <button
                      type="submit"
                      className="btn btn-primary"
                    >
                      {saving ? "Đang tạo..." : "Tạo khung giờ"}
                    </button>
                  </fieldset>
                </form>
              </div>
            </div>
          </div>

          <div className="col-12 col-lg-8">
            <div className="card shadow-sm">
              <div className="card-body">
                <h2 className="h5 fw-bold mb-3">
                  Danh sách khung giờ
                </h2>

                <div className="row g-3 mb-3">
                  <div className="col-12 col-md-4">
                    <label
                      htmlFor="filter-doctor"
                      className="form-label"
                    >
                      Bác sĩ
                    </label>

                    <select
                      id="filter-doctor"
                      className="form-select"
                      value={filterDoctorId}
                      onChange={(event) =>
                        setFilterDoctorId(event.target.value)
                      }
                    >
                      <option value="">Tất cả bác sĩ</option>

                      {doctors.map((doctor) => (
                        <option
                          key={doctor.id}
                          value={doctor.id}
                        >
                          {doctor.fullName}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="col-12 col-md-4">
                    <label
                      htmlFor="filter-date"
                      className="form-label"
                    >
                      Ngày khám
                    </label>

                    <input
                      id="filter-date"
                      type="date"
                      className="form-control"
                      value={filterDate}
                      onChange={(event) =>
                        setFilterDate(event.target.value)
                      }
                    />
                  </div>

                  <div className="col-12 col-md-4">
                    <label
                      htmlFor="filter-status"
                      className="form-label"
                    >
                      Trạng thái
                    </label>

                    <select
                      id="filter-status"
                      className="form-select"
                      value={filterStatus}
                      onChange={(event) =>
                        setFilterStatus(event.target.value)
                      }
                    >
                      <option value="">Tất cả trạng thái</option>

                      {statuses.map((status) => (
                        <option key={status} value={status}>
                          {status === "AVAILABLE"
                            ? "Còn trống"
                            : status}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="d-flex justify-content-between align-items-center mb-3">
                  <span className="small text-muted">
                    Hiển thị {filteredSlots.length}/{slots.length}
                    {" "}khung giờ
                  </span>

                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={clearFilters}
                  >
                    Xóa bộ lọc
                  </button>
                </div>

                {filteredSlots.length === 0 ? (
                  <p className="text-muted mb-0">
                    Không có khung giờ phù hợp.
                  </p>
                ) : (
                  <div className="table-responsive">
                    <table className="table table-hover align-middle mb-0">
                      <thead>
                        <tr>
                          <th scope="col">ID</th>
                          <th scope="col">Bác sĩ</th>
                          <th scope="col">Ngày khám</th>
                          <th scope="col">Giờ khám</th>
                          <th scope="col">Trạng thái</th>
                        </tr>
                      </thead>

                      <tbody>
                        {filteredSlots.map((slot) => (
                          <tr key={slot.id}>
                            <td>{slot.id}</td>
                            <td className="fw-semibold">
                              {slot.doctorName}
                            </td>
                            <td className="text-nowrap">
                              {formatDate(slot.slotDate)}
                            </td>
                            <td className="text-nowrap">
                              {formatTime(slot.startTime)}
                              {" – "}
                              {formatTime(slot.endTime)}
                            </td>
                            <td>
                              <StatusBadge status={slot.status} />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}