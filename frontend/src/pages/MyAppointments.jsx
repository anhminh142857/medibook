import {
  Fragment,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Link } from "react-router-dom";

import {
  cancelAppointment,
  getMyAppointments,
} from "../api/appointmentApi";
import { getSepayPaymentsForAppointment } from "../api/sepayPaymentApi";
import SepayPaymentPanel from "../components/SepayPaymentPanel";

const STATUS_LABELS = {
  PENDING: "Chờ xử lý",
  CANCELLED: "Đã hủy",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Đã khám",
};

const STATUS_CLASSES = {
  PENDING: "bg-warning text-dark",
  CANCELLED: "bg-secondary",
  CONFIRMED: "bg-primary",
  COMPLETED: "bg-success",
};

function formatDate(value) {
  if (!value) return "—";

  const parts = value.split("-");

  if (parts.length !== 3) return value;

  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

function formatTime(value) {
  return value ? value.slice(0, 5) : "—";
}

function canCancel(appointment, payments) {
  if (appointment.status !== "PENDING") return false;
  if (!Array.isArray(payments)) return false;
  if (payments.some((payment) => payment.requiresReview)) return false;
  if (payments.some((payment) => payment.status === "SUCCESS")) return false;

  if (!appointment.slotDate || !appointment.startTime) return false;

  const start = new Date(
    `${appointment.slotDate}T${appointment.startTime}`
  );

  return Number.isFinite(start.getTime()) && start.getTime() > Date.now();
}

export default function MyAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [paymentsByAppointment, setPaymentsByAppointment] = useState({});
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const [paymentAppointmentId, setPaymentAppointmentId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [now, setNow] = useState(0);
  const loadControllerRef = useRef(null);

  useEffect(() => {
    const updateTime = () => setNow(Date.now());
    updateTime();
    const timer = window.setInterval(updateTime, 15000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!message) return;

    const timer = window.setTimeout(() => {
      setMessage("");
    }, 5000);

    return () => window.clearTimeout(timer);
  }, [message]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    loadControllerRef.current = controller;

    async function loadAppointments() {
      setLoading(true);
      setError("");
      setAppointments([]);
      setPaymentsByAppointment({});

      try {
        const data = await getMyAppointments(controller.signal);
        const paymentEntries = await Promise.all(
          data.map(async (appointment) => {
            const payments = await getSepayPaymentsForAppointment(
              appointment.id,
              controller.signal
            );
            return [String(appointment.id), payments];
          })
        );

        if (active) {
          setAppointments(data);
          setPaymentsByAppointment(Object.fromEntries(paymentEntries));
        }
      } catch (err) {
        if (active && err.name !== "AbortError") {
          setError(err.message);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadAppointments();

    return () => {
      active = false;
      controller.abort();
      if (loadControllerRef.current === controller) {
        loadControllerRef.current = null;
      }
    };
  }, [reloadKey]);

  useEffect(() => {
    function handleAuthChange() {
      loadControllerRef.current?.abort();
      loadControllerRef.current = null;
      setAppointments([]);
      setPaymentsByAppointment({});
      setPaymentAppointmentId(null);
      setMessage("");
      setError("");

      const token = localStorage.getItem("token");

      if (
        token &&
        token.trim() !== "" &&
        token !== "null" &&
        token !== "undefined"
      ) {
        setLoading(true);
        setReloadKey((current) => current + 1);
      } else {
        setLoading(false);
        setError("Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.");
      }
    }

    function handleStorage(event) {
      if (
        event.key === "token" ||
        event.key === "user" ||
        event.key === null
      ) {
        handleAuthChange();
      }
    }

    window.addEventListener("auth-changed", handleAuthChange);
    window.addEventListener("storage", handleStorage);

    return () => {
      window.removeEventListener("auth-changed", handleAuthChange);
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  const updateAppointmentPayments = useCallback(
    (appointmentId, payments) => {
      setPaymentsByAppointment((current) => ({
        ...current,
        [String(appointmentId)]: payments,
      }));
    },
    []
  );

  async function handleCancel(appointment) {
    const confirmed = window.confirm(
      `Bạn có chắc muốn hủy lịch hẹn #${appointment.id} với bác sĩ ${appointment.doctorName}?`
    );

    if (!confirmed) return;

    setError("");
    setMessage("");
    setCancellingId(appointment.id);

    try {
      const updated = await cancelAppointment(appointment.id);

      setAppointments((current) =>
        current.map((item) =>
          item.id === updated.id ? updated : item
        )
      );
      setPaymentAppointmentId(null);

      setMessage(`Đã hủy lịch hẹn #${updated.id}. Khung giờ đã được mở lại.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setCancellingId(null);
    }
  }

  const activeAppointments = appointments.filter(
    (appointment) => appointment.status !== "CANCELLED"
  );

  function paymentSummary(appointment, payments) {
    if (payments.some((payment) => payment.requiresReview)) {
      if (payments.some((payment) => payment.status === "SUCCESS")) {
        return "Đã thanh toán · Cần phòng khám kiểm tra";
      }
      return "Cần liên hệ phòng khám";
    }

    if (
      payments.some(
        (payment) =>
          payment.status === "SUCCESS" && payment.provider === "SEPAY"
      )
    ) {
      return appointment.status === "PENDING"
        ? "Đã thanh toán SePay · Chờ bác sĩ xác nhận"
        : "Đã thanh toán SePay";
    }

    if (payments.some((payment) => payment.status === "SUCCESS")) {
      return "Đã thanh toán qua cổng khác · Liên hệ phòng khám";
    }

    if (appointment.status === "CANCELLED") {
      return "Lịch đã hủy · Không tiếp tục chuyển khoản";
    }

    const activePendingPayment = payments.find((payment) => {
      const expiry = Date.parse(payment.expiresAt);
      return (
        payment.status === "PENDING" &&
        Number.isFinite(expiry) &&
        expiry > now
      );
    });

    if (activePendingPayment) {
      return activePendingPayment.provider === "SEPAY"
        ? "Chờ thanh toán SePay"
        : `Giao dịch ${activePendingPayment.provider} đang chờ`;
    }

    if (
      payments.some(
        (payment) =>
          payment.status === "PENDING" &&
          Number.isFinite(Date.parse(payment.expiresAt)) &&
          Date.parse(payment.expiresAt) <= now
      )
    ) {
      return "Giao dịch đã hết hạn";
    }

    if (payments.some((payment) => payment.status === "FAILED")) {
      return "Giao dịch thất bại";
    }

    return "Chưa thanh toán";
  }

  return (
    <main className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h2 className="mb-1">Lịch hẹn của tôi</h2>

          <p className="text-muted mb-0">
            Theo dõi và quản lý các lịch khám bạn đã đặt.
          </p>
        </div>

        <div className="d-flex flex-wrap gap-2">
          <Link to="/booking" className="btn btn-primary">
            Đặt lịch mới
          </Link>

          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={loading || cancellingId !== null}
            onClick={() => {
              setMessage("");
              setReloadKey((current) => current + 1);
            }}
          >
            Tải lại
          </button>
        </div>
      </div>

      {message && (
        <div className="alert alert-success" role="status">
          {message}
        </div>
      )}

      {error && (
        <div className="alert alert-danger" role="alert">
          <div>{error}</div>

          <a href="/login" className="alert-link">
            Đi đến trang đăng nhập
          </a>
        </div>
      )}

      {loading ? (
        <div className="card shadow-sm">
          <div className="card-body text-center py-5">
            <div className="spinner-border text-primary mb-3" role="status">
              <span className="visually-hidden">Đang tải...</span>
            </div>

            <p className="text-muted mb-0">Đang tải lịch hẹn...</p>
          </div>
        </div>
      ) : error ? null : appointments.length === 0 ? (
        <div className="card shadow-sm">
          <div className="card-body text-center py-5">
            <h3 className="h5 mb-2">Bạn chưa có lịch hẹn nào</h3>

            <p className="text-muted mb-4">
              Hãy chọn chuyên khoa và bác sĩ phù hợp để đặt lịch khám.
            </p>

            <Link to="/booking" className="btn btn-primary">
              Đặt lịch ngay
            </Link>
          </div>
        </div>
      ) : (
        <>
          <div className="d-flex flex-wrap gap-2 mb-3">
            <span className="badge bg-primary-subtle text-primary-emphasis">
              Tổng cộng: {appointments.length} lịch
            </span>

            <span className="badge bg-success-subtle text-success-emphasis">
              Đang hiệu lực: {activeAppointments.length} lịch
            </span>
          </div>

          <div className="card shadow-sm">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Mã lịch</th>
                    <th>Bác sĩ</th>
                    <th>Ngày khám</th>
                    <th>Khung giờ</th>
                    <th>Trạng thái lịch</th>
                    <th>Thanh toán</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>

                <tbody>
                  {appointments.map((appointment) => (
                    <Fragment key={appointment.id}>
                      <tr>
                        <td>#{appointment.id}</td>

                        <td>{appointment.doctorName}</td>

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
                              STATUS_CLASSES[appointment.status] ||
                              "bg-secondary"
                            }`}
                          >
                            {STATUS_LABELS[appointment.status] ||
                              appointment.status}
                          </span>
                        </td>

                        <td>
                          {paymentSummary(
                            appointment,
                            paymentsByAppointment[String(appointment.id)]
                          )}
                        </td>

                        <td>
                          <div className="d-flex flex-wrap gap-2">
                            <button
                              type="button"
                              className="btn btn-outline-primary btn-sm"
                              disabled={cancellingId !== null}
                              onClick={() =>
                                setPaymentAppointmentId((current) =>
                                  current === appointment.id
                                    ? null
                                    : appointment.id
                                )
                              }
                            >
                              {paymentAppointmentId === appointment.id
                                ? "Đóng thanh toán"
                                : "Thanh toán / Chi tiết"}
                            </button>

                            {canCancel(
                              appointment,
                              paymentsByAppointment[String(appointment.id)]
                            ) ? (
                              <button
                                type="button"
                                className="btn btn-outline-danger btn-sm"
                                disabled={cancellingId !== null}
                                onClick={() => handleCancel(appointment)}
                              >
                                {cancellingId === appointment.id
                                  ? "Đang hủy..."
                                  : "Hủy lịch"}
                              </button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                      {paymentAppointmentId === appointment.id && (
                        <tr>
                          <td colSpan={7}>
                            <SepayPaymentPanel
                              appointment={appointment}
                              initialPayments={
                                paymentsByAppointment[
                                  String(appointment.id)
                                ]
                              }
                              onPaymentsChange={updateAppointmentPayments}
                            />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </main>
  );
}