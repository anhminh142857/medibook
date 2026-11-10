import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  getAppointmentSlots,
  getDoctors,
} from "../api/bookingApi";
import { createAppointment } from "../api/appointmentApi";

function getToday() {
  const now = new Date();

  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("-");
}

function formatTime(value) {
  return value ? value.slice(0, 5) : "—";
}

function isFutureSlot(slot) {
  const start = new Date(`${slot.slotDate}T${slot.startTime}`);
  return Number.isFinite(start.getTime()) && start.getTime() > Date.now();
}

export default function Booking() {
  const [searchParams, setSearchParams] = useSearchParams();
  const doctorId = searchParams.get("doctorId") || "";

  const [doctors, setDoctors] = useState([]);
  const [slots, setSlots] = useState([]);
  const [slotDate, setSlotDate] = useState(getToday);
  const [loading, setLoading] = useState(true);
  const [bookingId, setBookingId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadData() {
      setLoading(true);
      setError("");

      try {
        const [doctorList, slotList] = await Promise.all([
          getDoctors(controller.signal),
          getAppointmentSlots(controller.signal),
        ]);

        if (!controller.signal.aborted) {
          setDoctors(doctorList);
          setSlots(slotList);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err.message || "Không tải được dữ liệu đặt lịch.");
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadData();
    return () => controller.abort();
  }, [reloadKey]);

  function changeDoctor(value) {
    const next = new URLSearchParams(searchParams);

    if (value) {
      next.set("doctorId", value);
    } else {
      next.delete("doctorId");
    }

    setSearchParams(next);
    setMessage("");
    setError("");
  }

  const selectedDoctor = doctors.find(
    (doctor) => String(doctor.id) === doctorId
  );

  const invalidDoctor = !loading && doctorId && !selectedDoctor;

  const filteredSlots = slots
    .filter(
      (slot) =>
        String(slot.doctorId) === doctorId &&
        slot.slotDate === slotDate
    )
    .sort((first, second) =>
      first.startTime.localeCompare(second.startTime)
    );

  async function handleBooking(slot) {
    if (bookingId !== null) return;

    if (slot.status !== "AVAILABLE" || !isFutureSlot(slot)) {
      setError("Khung giờ này không còn nhận đặt. Vui lòng tải lại.");
      return;
    }

    const confirmed = window.confirm(
      `Đặt lịch với bác sĩ ${slot.doctorName} lúc ${formatTime(
        slot.startTime
      )} ngày ${slot.slotDate}?`
    );

    if (!confirmed) return;

    setError("");
    setMessage("");
    setBookingId(slot.id);

    try {
      const appointment = await createAppointment(slot.id);

      setSlots((current) =>
        current.map((item) =>
          item.id === slot.id ? { ...item, status: "BOOKED" } : item
        )
      );

      setMessage(
        `Đặt lịch thành công! Mã lịch hẹn: #${appointment.id}.`
      );
    } catch (err) {
      setError(err.message || "Không đặt được lịch khám.");
    } finally {
      setBookingId(null);
    }
  }

  return (
    <main className="container py-5">
      <div className="catalog-section-heading">
        <div>
          <h1 className="h2 fw-bold">Đặt lịch khám</h1>
          <p className="text-muted mb-0">
            Chọn bác sĩ, ngày khám và một khung giờ còn trống.
          </p>
        </div>

        <Link to="/my-appointments" className="btn btn-outline-primary">
          Lịch hẹn của tôi
        </Link>
      </div>

      {message && (
        <div className="alert alert-success" role="status">
          {message}{" "}
          <Link to="/my-appointments">Xem lịch hẹn</Link>
        </div>
      )}

      {error && (
        <div className="alert alert-danger" role="alert">{error}</div>
      )}

      <div className="card shadow-sm mb-4">
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-6">
              <label htmlFor="doctorId" className="form-label">
                Chọn bác sĩ
              </label>

              <select
                id="doctorId"
                className="form-select"
                value={doctorId}
                disabled={loading || bookingId !== null}
                onChange={(event) => changeDoctor(event.target.value)}
              >
                <option value="">-- Chọn bác sĩ --</option>

                {invalidDoctor && (
                  <option value={doctorId}>Bác sĩ không tồn tại</option>
                )}

                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.id}>
                    {doctor.fullName} — {doctor.specialtyName}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-6">
              <label htmlFor="slotDate" className="form-label">
                Ngày khám
              </label>

              <input
                id="slotDate"
                type="date"
                className="form-control"
                value={slotDate}
                min={getToday()}
                disabled={bookingId !== null}
                onChange={(event) => {
                  setSlotDate(event.target.value);
                  setMessage("");
                }}
              />
            </div>
          </div>

          <button
            type="button"
            className="btn btn-outline-secondary mt-3"
            disabled={loading || bookingId !== null}
            onClick={() => setReloadKey((current) => current + 1)}
          >
            {loading ? "Đang tải..." : "Tải lại khung giờ"}
          </button>
        </div>
      </div>

      {loading ? (
        <p role="status">Đang tải dữ liệu...</p>
      ) : invalidDoctor ? (
        <div className="alert alert-warning">
          Bác sĩ trên đường dẫn không còn tồn tại hoặc chưa được tải.
          Vui lòng chọn lại bác sĩ.
        </div>
      ) : !doctorId ? (
        <div className="alert alert-info">
          Chọn bác sĩ và ngày khám để xem các khung giờ.
        </div>
      ) : (
        <section>
          <h2 className="h4 mb-3">
            Khung giờ của bác sĩ {selectedDoctor?.fullName}
          </h2>

          {filteredSlots.length === 0 ? (
            <div className="alert alert-info">
              Chưa có khung giờ cho bác sĩ này trong ngày đã chọn.
              Bạn có thể chọn ngày khác.
            </div>
          ) : (
            <div className="row g-3">
              {filteredSlots.map((slot) => {
                const future = isFutureSlot(slot);
                const available = slot.status === "AVAILABLE" && future;

                const label = !future
                  ? "Đã qua giờ khám"
                  : slot.status === "AVAILABLE"
                    ? "Còn trống"
                    : slot.status === "BOOKED"
                      ? "Đã được đặt"
                      : "Không nhận đặt";

                return (
                  <div className="col-sm-6 col-lg-4" key={slot.id}>
                    <div className="card h-100">
                      <div className="card-body">
                        <h3 className="h5">
                          {formatTime(slot.startTime)}
                          {" – "}
                          {formatTime(slot.endTime)}
                        </h3>

                        <p className={available ? "text-success" : "text-muted"}>
                          {label}
                        </p>

                        <button
                          type="button"
                          className="btn btn-primary w-100"
                          disabled={!available || bookingId !== null}
                          onClick={() => handleBooking(slot)}
                        >
                          {bookingId === slot.id ? "Đang đặt..." : "Đặt lịch"}
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      )}
    </main>
  );
}