import { useEffect, useMemo, useState } from "react";
import {
  createSepayPayment,
  getSepayPaymentsForAppointment,
} from "../api/sepayPaymentApi";

const POLL_DELAY_MS = 8000;

const APPOINTMENT_STATUS_LABELS = {
  PENDING: "Chờ bác sĩ xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Đã khám",
  CANCELLED: "Đã hủy",
};

function isExpired(payment, now) {
  const expiry = Date.parse(payment.expiresAt);
  return Number.isFinite(expiry) && expiry <= now;
}

function formatAmount(amount) {
  return new Intl.NumberFormat("vi-VN").format(amount || 0);
}

function getRemainingTime(payment, now) {
  const expiry = Date.parse(payment.expiresAt);

  if (!Number.isFinite(expiry)) {
    return "";
  }

  const seconds = Math.max(0, Math.floor((expiry - now) / 1000));
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  return `${minutes}:${String(remainingSeconds).padStart(2, "0")}`;
}

function isActivePending(payment, now) {
  return (
    payment.status === "PENDING" &&
    !payment.requiresReview &&
    !isExpired(payment, now)
  );
}

function PaymentStatus({ payment, now }) {
  if (payment.requiresReview) {
    return (
      <span className="badge bg-danger">
        Cần phòng khám kiểm tra
      </span>
    );
  }

  if (payment.status === "SUCCESS") {
    return <span className="badge bg-success">Đã thanh toán</span>;
  }

  if (
    payment.status === "PENDING" &&
    isExpired(payment, now)
  ) {
    return <span className="badge bg-secondary">Đã hết hạn</span>;
  }

  if (payment.status === "PENDING") {
    return <span className="badge bg-warning text-dark">Chờ thanh toán</span>;
  }

  return (
    <span className="badge bg-secondary">
      {payment.status || "Không xác định"}
    </span>
  );
}

function PaymentInstructions({ payment, now, onCopy }) {
  return (
    <div className="border rounded p-3 mt-3">
      <h4 className="h6">Hướng dẫn chuyển khoản · SePay Sandbox</h4>

      <dl className="row mb-2">
        <dt className="col-sm-4">Ngân hàng</dt>
        <dd className="col-sm-8">{payment.receiverGateway || "—"}</dd>

        <dt className="col-sm-4">Tài khoản nhận</dt>
        <dd className="col-sm-8">
          <span className="me-2">{payment.receiverAccount || "—"}</span>
          {payment.receiverAccount && (
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => onCopy(payment.receiverAccount, "Số tài khoản")}
            >
              Sao chép
            </button>
          )}
        </dd>

        <dt className="col-sm-4">Số tiền</dt>
        <dd className="col-sm-8">{formatAmount(payment.amount)} VND</dd>

        <dt className="col-sm-4">Nội dung</dt>
        <dd className="col-sm-8">
          <code className="me-2">{payment.transferContent}</code>
          <button
            type="button"
            className="btn btn-sm btn-outline-secondary"
            onClick={() =>
              onCopy(payment.transferContent, "Nội dung chuyển khoản")
            }
          >
            Sao chép
          </button>
        </dd>

        <dt className="col-sm-4">Hạn giao dịch</dt>
        <dd className="col-sm-8">
          {payment.expiresAt
            ? new Date(payment.expiresAt).toLocaleString("vi-VN")
            : "—"}
          {isActivePending(payment, now) && (
            <span className="ms-2 text-muted">
              Còn khoảng {getRemainingTime(payment, now)}
            </span>
          )}
        </dd>
      </dl>

      <p className="small text-muted mb-0">
        Thời gian đếm ngược chỉ để tham khảo; trạng thái hợp lệ do backend
        xác định.
      </p>
    </div>
  );
}

export default function SepayPaymentPanel({
  appointment,
  initialPayments,
  onPaymentsChange,
}) {
  const [payments, setPayments] = useState(initialPayments);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [pollingEnabled, setPollingEnabled] = useState(true);
  const [now, setNow] = useState(0);

  useEffect(() => {
    const timer = window.setTimeout(() => setNow(Date.now()), 0);

    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function loadPayments() {
      setLoading(true);
      setError("");

      try {
        const data = await getSepayPaymentsForAppointment(
          appointment.id,
          controller.signal
        );

        if (active) {
          setPayments(data);
          onPaymentsChange(appointment.id, data);
        }
      } catch (requestError) {
        if (active && requestError.name !== "AbortError") {
          setError(requestError.message || "Không tải được thanh toán.");
          setPollingEnabled(false);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadPayments();

    return () => {
      active = false;
      controller.abort();
    };
  }, [appointment.id, onPaymentsChange, refreshKey]);

  const activePending = useMemo(
    () =>
      payments.some(
        (payment) =>
          payment.provider === "SEPAY" && isActivePending(payment, now)
      ),
    [payments, now]
  );

  useEffect(() => {
    if (!activePending || !pollingEnabled || loading) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setRefreshKey((current) => current + 1);
    }, POLL_DELAY_MS);

    return () => window.clearTimeout(timer);
  }, [activePending, loading, pollingEnabled, refreshKey]);

  useEffect(() => {
    if (!activePending) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => window.clearInterval(timer);
  }, [activePending]);

  async function handleCopy(value, label) {
    try {
      await navigator.clipboard.writeText(value);
      setNotice(`${label} đã được sao chép.`);
      setError("");
    } catch {
      setError(
        "Trình duyệt không cho phép sao chép. Vui lòng chọn và sao chép thủ công."
      );
    }
  }

  async function handleCreatePayment() {
    if (creating || loading) {
      return;
    }

    setCreating(true);
    setError("");
    setNotice("");

    try {
      const created = await createSepayPayment(appointment.id);
      const current = Array.isArray(payments) ? payments : [];
      const withoutDuplicate = current.filter(
        (payment) => payment.paymentId !== created.paymentId
      );
      const updated = [created, ...withoutDuplicate];

      setPayments(updated);
      onPaymentsChange(appointment.id, updated);
      setPollingEnabled(true);
      setRefreshKey((value) => value + 1);
      setNotice("Đã tải thông tin thanh toán SePay Sandbox.");
    } catch (requestError) {
      setError(
        requestError.message ||
          "Không thể tạo thanh toán. Hãy tải lại trạng thái giao dịch."
      );
      setPollingEnabled(false);
    } finally {
      setCreating(false);
    }
  }

  function handleRefresh() {
    setError("");
    setNotice("");
    setPollingEnabled(true);
    setRefreshKey((value) => value + 1);
  }

  if (loading && payments === null) {
    return <p role="status">Đang tải trạng thái thanh toán...</p>;
  }

  const list = Array.isArray(payments) ? payments : [];
  const hasReview = list.some((payment) => payment.requiresReview);
  const successfulPayments = list.filter(
    (payment) => payment.status === "SUCCESS"
  );
  const hasSuccessfulSepayPayment = successfulPayments.some(
    (payment) => payment.provider === "SEPAY"
  );
  const activePayments = list.filter((payment) =>
    payment.provider === "SEPAY" && isActivePending(payment, now)
  );
  const activeOtherProviderPayments = list.filter(
    (payment) =>
      payment.provider !== "SEPAY" && isActivePending(payment, now)
  );
  const hasSuccessfulPayment = successfulPayments.length > 0;
  const canCreate =
    appointment.status === "PENDING" &&
    !hasReview &&
    !hasSuccessfulPayment &&
    activePayments.length === 0 &&
    activeOtherProviderPayments.length === 0;
  const isCancelled = appointment.status === "CANCELLED";

  return (
    <section
      className="border rounded bg-light p-3 mt-3"
      aria-label={`Thanh toán lịch hẹn ${appointment.id}`}
    >
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
        <div>
          <h3 className="h6 mb-1">Thanh toán lịch hẹn</h3>
          <p className="small mb-0">
            Trạng thái lịch:{" "}
            <strong>
              {APPOINTMENT_STATUS_LABELS[appointment.status] ||
                appointment.status}
            </strong>
          </p>
        </div>

        <button
          type="button"
          className="btn btn-sm btn-outline-primary"
          disabled={loading || creating}
          onClick={handleRefresh}
        >
          {loading ? "Đang tải..." : "Tải lại thanh toán"}
        </button>
      </div>

      {notice && (
        <div className="alert alert-success py-2 mt-3 mb-0" role="status">
          {notice}
        </div>
      )}

      {error && (
        <div className="alert alert-danger py-2 mt-3 mb-0" role="alert">
          {error}
        </div>
      )}

      {loading && (
        <p className="small text-muted mt-3 mb-0" role="status">
          Đang cập nhật trạng thái từ máy chủ...
        </p>
      )}

      {!loading && hasReview && (
        <div className="alert alert-warning mt-3 mb-0" role="alert">
          Có giao dịch cần phòng khám kiểm tra. MediBook không tự xóa trạng
          thái review hoặc hoàn tiền; vui lòng liên hệ phòng khám.
        </div>
      )}

      {!loading && hasSuccessfulPayment && (
        <div className="alert alert-success mt-3 mb-0" role="status">
          {hasSuccessfulSepayPayment
            ? `Đã thanh toán SePay thành công.${
                appointment.status === "PENDING"
                  ? " Lịch vẫn đang chờ bác sĩ xác nhận."
                  : ` Trạng thái lịch hiện tại: ${
                      APPOINTMENT_STATUS_LABELS[appointment.status] ||
                      appointment.status
                    }.`
              }`
            : "Đã thanh toán qua cổng khác. Lịch không thể tiếp tục thanh toán SePay; vui lòng liên hệ phòng khám."}
        </div>
      )}

      {!loading && isCancelled && (
        <div className="alert alert-secondary mt-3 mb-0" role="status">
          Lịch đã hủy; không tiếp tục chuyển khoản. Nếu đã chuyển tiền, vui
          lòng liên hệ phòng khám để được kiểm tra.
        </div>
      )}

      {!loading && activeOtherProviderPayments.length > 0 && !isCancelled && (
        <div className="alert alert-warning mt-3 mb-0" role="alert">
          Lịch có giao dịch đang chờ từ cổng thanh toán khác. Không tạo thêm
          giao dịch SePay; vui lòng liên hệ phòng khám nếu cần hỗ trợ.
        </div>
      )}

      {!loading &&
        !isCancelled &&
        !hasReview &&
        !hasSuccessfulPayment &&
        activePayments.map((payment) => (
          <div key={payment.paymentId}>
            <div className="mt-3">
              <PaymentStatus payment={payment} now={now} />
            </div>
            <PaymentInstructions
              payment={payment}
              now={now}
              onCopy={handleCopy}
            />
          </div>
        ))}

      {!loading &&
        list.map((payment) => {
          const expired =
            payment.status === "PENDING" && isExpired(payment, now);
          const needsSeparateStatus =
            payment.status === "FAILED" ||
            expired ||
            payment.requiresReview ||
            payment.status === "SUCCESS" ||
            payment.provider !== "SEPAY";

          if (!needsSeparateStatus) {
            return null;
          }

          return (
            <div
              className="d-flex flex-wrap align-items-center gap-2 mt-3"
              key={`status-${payment.paymentId}`}
            >
              <span className="small">
                Giao dịch {payment.provider} #{payment.paymentId}:
              </span>
              <PaymentStatus payment={payment} now={now} />
              {payment.status === "SUCCESS" && payment.paidAt && (
                <span className="small text-muted">
                  {new Date(payment.paidAt).toLocaleString("vi-VN")}
                </span>
              )}
            </div>
          );
        })}

      {!loading && canCreate && (
        <div className="mt-3">
          {list.length === 0 ? (
            <p className="small text-muted">
              Chưa có giao dịch thanh toán cho lịch này.
            </p>
          ) : (
            <p className="small text-muted">
              Không có giao dịch thanh toán còn hiệu lực.
            </p>
          )}
          <button
            type="button"
            className="btn btn-primary"
            disabled={creating}
            onClick={handleCreatePayment}
          >
            {creating
              ? "Đang tạo thông tin thanh toán..."
              : "Tạo thông tin thanh toán SePay Sandbox"}
          </button>
        </div>
      )}
    </section>
  );
}
