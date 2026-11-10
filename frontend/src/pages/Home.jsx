import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getDoctors } from "../api/doctorApi";
import { getSpecialties } from "../api/specialtyApi";
import {
  DoctorCard,
  SpecialtyCard,
} from "../components/CatalogCards";

export default function Home() {
  const [doctors, setDoctors] = useState([]);
  const [specialties, setSpecialties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadData() {
      setLoading(true);
      setError("");

      try {
        const [doctorData, specialtyData] = await Promise.all([
          getDoctors(controller.signal),
          getSpecialties(controller.signal),
        ]);

        if (!controller.signal.aborted) {
          setDoctors(doctorData);
          setSpecialties(specialtyData);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err.message || "Không tải được dữ liệu.");
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

  return (
    <main className="container py-4 py-lg-5">
      <section className="medibook-hero mb-5">
        <div className="row g-4 align-items-center">
          <div className="col-lg-7">
            <p className="hero-eyebrow">MEDIBOOK · ĐẶT LỊCH KHÁM</p>

            <h1 className="display-5 fw-bold mb-3">
              Chủ động chọn bác sĩ,
              <br />
              thuận tiện đặt lịch khám.
            </h1>

            <p className="lead text-muted mb-4">
              Tìm chuyên khoa, xem bác sĩ và lựa chọn khung giờ
              phù hợp ngay trên MediBook.
            </p>

            <div className="d-flex flex-wrap gap-3">
              <Link to="/doctors" className="btn btn-primary btn-lg">
                Tìm bác sĩ
              </Link>

              <Link
                to="/specialties"
                className="btn btn-outline-primary btn-lg"
              >
                Xem chuyên khoa
              </Link>
            </div>
          </div>

          <div className="col-lg-5">
            <div className="hero-booking-panel">
              <span className="catalog-symbol mb-3" aria-hidden="true">
                +
              </span>

              <h2 className="h4 mb-3">Lịch khám trong tầm tay</h2>

              <ul className="hero-checklist">
                <li>Tìm bác sĩ theo chuyên khoa</li>
                <li>Chọn ngày và khung giờ còn trống</li>
                <li>Theo dõi trạng thái lịch hẹn</li>
              </ul>

              <Link to="/booking" className="btn btn-primary w-100 mt-3">
                Đặt lịch ngay
              </Link>
            </div>
          </div>
        </div>
      </section>

      {error && (
        <div className="alert alert-danger" role="alert">
          <p className="mb-2">{error}</p>

          <button
            type="button"
            className="btn btn-sm btn-outline-danger"
            disabled={loading}
            onClick={() => setReloadKey((value) => value + 1)}
          >
            Thử lại
          </button>
        </div>
      )}

      <section className="mb-5">
        <div className="catalog-section-heading">
          <div>
            <h2 className="h3">Khám theo chuyên khoa</h2>
            <p className="text-muted mb-0">
              Chọn chuyên khoa để xem các bác sĩ tương ứng.
            </p>
          </div>

          <Link to="/specialties">Xem tất cả chuyên khoa</Link>
        </div>

        {loading ? (
          <p role="status">Đang tải chuyên khoa...</p>
        ) : !error && specialties.length === 0 ? (
          <div className="catalog-empty">
            Chưa có chuyên khoa được cập nhật.
          </div>
        ) : (
          <div className="row g-4">
            {specialties.slice(0, 4).map((specialty) => (
              <div className="col-md-6 col-xl-3" key={specialty.id}>
                <SpecialtyCard specialty={specialty} />
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="mb-5">
        <div className="catalog-section-heading">
          <div>
            <h2 className="h3">Bác sĩ trên MediBook</h2>
            <p className="text-muted mb-0">
              Xem thông tin và lựa chọn bác sĩ phù hợp.
            </p>
          </div>

          <Link to="/doctors">Xem tất cả bác sĩ</Link>
        </div>

        {loading ? (
          <p role="status">Đang tải bác sĩ...</p>
        ) : !error && doctors.length === 0 ? (
          <div className="catalog-empty">
            Chưa có hồ sơ bác sĩ được cập nhật.
          </div>
        ) : (
          <div className="row g-4">
            {doctors.slice(0, 3).map((doctor) => (
              <div className="col-md-6 col-xl-4" key={doctor.id}>
                <DoctorCard doctor={doctor} />
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="mb-3">
        <h2 className="h3 mb-4">Đặt lịch qua ba bước</h2>

        <div className="row g-4">
          {[
            ["01", "Chọn bác sĩ", "Tìm bác sĩ theo chuyên khoa và thông tin giới thiệu."],
            ["02", "Chọn giờ khám", "Chọn ngày khám và khung giờ còn trống."],
            ["03", "Theo dõi lịch hẹn", "Đăng nhập để đặt lịch và xem trạng thái xử lý."],
          ].map(([step, title, description]) => (
            <div className="col-md-4" key={step}>
              <div className="card h-100">
                <div className="card-body">
                  <span className="step-number">{step}</span>
                  <h3 className="h5 mt-3">{title}</h3>
                  <p className="text-muted mb-0">{description}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}