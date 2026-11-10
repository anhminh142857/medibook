import { Link } from "react-router-dom";

function getInitials(name) {
  return (name || "BS")
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((word) => word.charAt(0))
    .join("")
    .toUpperCase();
}

export function SpecialtyCard({ specialty }) {
  return (
    <article className="card catalog-card h-100">
      <div className="card-body d-flex flex-column">
        <span className="catalog-symbol mb-3" aria-hidden="true">
          +
        </span>

        <h3 className="h5">{specialty.name}</h3>

        <p className="text-muted flex-grow-1">
          {specialty.description ||
            "Xem danh sách bác sĩ và lựa chọn lịch khám phù hợp."}
        </p>

        <Link
          to={`/doctors?specialtyId=${encodeURIComponent(specialty.id)}`}
          className="btn btn-outline-primary align-self-start"
        >
          Xem bác sĩ
        </Link>
      </div>
    </article>
  );
}

export function DoctorCard({ doctor }) {
  return (
    <article className="card catalog-card h-100">
      <div className="card-body d-flex flex-column">
        <div className="d-flex align-items-center gap-3 mb-3">
          <div className="doctor-avatar" aria-hidden="true">
            {getInitials(doctor.fullName)}
          </div>

          <div>
            <h3 className="h5 mb-1">{doctor.fullName}</h3>

            <span className="text-primary small">
              {doctor.specialtyName || "Chưa cập nhật chuyên khoa"}
            </span>
          </div>
        </div>

        <p className="text-muted flex-grow-1">
          {doctor.bio || "Thông tin giới thiệu đang được cập nhật."}
        </p>

        <Link
          to={`/booking?doctorId=${encodeURIComponent(doctor.id)}`}
          className="btn btn-primary mt-2"
        >
          Xem giờ khám và đặt lịch
        </Link>
      </div>
    </article>
  );
}