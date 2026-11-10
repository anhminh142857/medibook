import { useEffect, useState } from "react";
import { getSpecialties } from "../api/specialtyApi";
import { SpecialtyCard } from "../components/CatalogCards";

export default function Specialties() {
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
        const data = await getSpecialties(controller.signal);

        if (!controller.signal.aborted) {
          setSpecialties(data);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err.message || "Không tải được chuyên khoa.");
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

  const filtered = specialties.filter((specialty) =>
    (specialty.name || "")
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi"))
  );

  return (
    <main className="container py-5">
      <div className="catalog-section-heading">
        <div>
          <h1 className="h2 fw-bold">Danh sách chuyên khoa</h1>
          <p className="text-muted mb-0">
            Tìm chuyên khoa và xem các bác sĩ đang có trên hệ thống.
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
          <label htmlFor="specialty-search" className="form-label">
            Tìm chuyên khoa
          </label>

          <input
            id="specialty-search"
            type="search"
            className="form-control"
            placeholder="Nhập tên chuyên khoa..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">{error}</div>
      )}

      {loading ? (
        <p role="status">Đang tải chuyên khoa...</p>
      ) : !error && (
        <>
          <p className="text-muted">
            Tìm thấy {filtered.length} chuyên khoa
          </p>

          {filtered.length === 0 ? (
            <div className="catalog-empty">
              {specialties.length === 0
                ? "Chưa có chuyên khoa. Admin cần thêm dữ liệu trong trang quản lý."
                : "Không có chuyên khoa phù hợp với từ khóa."}
            </div>
          ) : (
            <div className="row g-4">
              {filtered.map((specialty) => (
                <div className="col-md-6 col-xl-4" key={specialty.id}>
                  <SpecialtyCard specialty={specialty} />
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </main>
  );
}