import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { getDoctors } from "../api/doctorApi";
import { getSpecialties } from "../api/specialtyApi";
import { DoctorCard } from "../components/CatalogCards";

export default function Doctors() {
  const [searchParams, setSearchParams] = useSearchParams();
  const specialtyId = searchParams.get("specialtyId") || "";

  const [doctors, setDoctors] = useState([]);
  const [specialties, setSpecialties] = useState([]);
  const [search, setSearch] = useState("");
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
          setError(err.message || "Không tải được danh sách bác sĩ.");
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

  function changeSpecialty(value) {
    const next = new URLSearchParams(searchParams);

    if (value) {
      next.set("specialtyId", value);
    } else {
      next.delete("specialtyId");
    }

    setSearchParams(next);
  }

  const invalidSpecialty =
    specialtyId &&
    !specialties.some((item) => String(item.id) === specialtyId);

  const filtered = doctors.filter((doctor) => {
    const matchesSpecialty =
      !specialtyId || String(doctor.specialtyId) === specialtyId;

    const matchesName = (doctor.fullName || "")
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi"));

    return matchesSpecialty && matchesName;
  });

  return (
    <main className="container py-5">
      <div className="catalog-section-heading">
        <div>
          <h1 className="h2 fw-bold">Danh sách bác sĩ</h1>
          <p className="text-muted mb-0">
            Tìm bác sĩ, xem thông tin và chọn khung giờ khám.
          </p>
        </div>

        <button
          type="button"
          className="btn btn-outline-primary"
          disabled={loading}
          onClick={() => setReloadKey((value) => value + 1)}
        >
          Tải lại
        </button>
      </div>

      <div className="card mb-4">
        <div className="card-body">
          <div className="row g-3 align-items-end">
            <div className="col-md-5">
              <label htmlFor="doctor-search" className="form-label">
                Tên bác sĩ
              </label>

              <input
                id="doctor-search"
                type="search"
                className="form-control"
                placeholder="Nhập tên bác sĩ..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>

            <div className="col-md-5">
              <label htmlFor="doctor-specialty-filter" className="form-label">
                Chuyên khoa
              </label>

              <select
                id="doctor-specialty-filter"
                className="form-select"
                value={specialtyId}
                disabled={loading}
                onChange={(event) => changeSpecialty(event.target.value)}
              >
                <option value="">Tất cả chuyên khoa</option>

                {invalidSpecialty && (
                  <option value={specialtyId}>
                    Chuyên khoa không tồn tại
                  </option>
                )}

                {specialties.map((specialty) => (
                  <option key={specialty.id} value={specialty.id}>
                    {specialty.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-2">
              <button
                type="button"
                className="btn btn-outline-secondary w-100"
                onClick={() => {
                  setSearch("");
                  changeSpecialty("");
                }}
              >
                Xóa lọc
              </button>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">{error}</div>
      )}

      {loading ? (
        <p role="status">Đang tải bác sĩ...</p>
      ) : !error && (
        <>
          <p className="text-muted">Tìm thấy {filtered.length} bác sĩ</p>

          {filtered.length === 0 ? (
            <div className="catalog-empty">
              Không có bác sĩ phù hợp. Hãy đổi bộ lọc hoặc quay lại sau
              khi admin cập nhật hồ sơ.
            </div>
          ) : (
            <div className="row g-4">
              {filtered.map((doctor) => (
                <div className="col-md-6 col-xl-4" key={doctor.id}>
                  <DoctorCard doctor={doctor} />
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </main>
  );
}