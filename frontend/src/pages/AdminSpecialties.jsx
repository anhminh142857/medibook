import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  createSpecialty,
  deleteSpecialty,
  getSpecialties,
  updateSpecialty,
} from "../api/specialtyApi";

const emptyForm = {
  name: "",
  description: "",
};

export default function AdminSpecialties() {
  const navigate = useNavigate();

  const [specialties, setSpecialties] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadSpecialties = useCallback(async (signal) => {
    try {
      setLoading(true);
      setError("");

      const data = await getSpecialties(signal);

      setSpecialties(data);
    } catch (err) {
      if (err.name === "AbortError") {
        return;
      }

      setError(err.message || "Không tải được danh sách chuyên khoa.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    loadSpecialties(controller.signal);

    return () => controller.abort();
  }, [loadSpecialties]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const name = form.name.trim();
    const description = form.description.trim();

    if (!name) {
      setError("Vui lòng nhập tên chuyên khoa.");
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");

    try {
      if (editingId) {
        await updateSpecialty(editingId, {
          name,
          description,
        });

        setSuccess("Đã cập nhật chuyên khoa.");
      } else {
        await createSpecialty({
          name,
          description,
        });

        setSuccess("Đã tạo chuyên khoa.");
      }

      setForm(emptyForm);
      setEditingId(null);

      await loadSpecialties();
    } catch (err) {
      setError(err.message || "Không lưu được chuyên khoa.");
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (specialty) => {
    setEditingId(specialty.id);
    setForm({
      name: specialty.name || "",
      description: specialty.description || "",
    });
    setError("");
    setSuccess("");
  };

  const handleDelete = async (specialty) => {
    const confirmed = window.confirm(
      `Bạn có chắc muốn xóa chuyên khoa "${specialty.name}"?`
    );

    if (!confirmed) {
      return;
    }

    setError("");
    setSuccess("");

    try {
      await deleteSpecialty(specialty.id);

      setSuccess("Đã xóa chuyên khoa.");

      if (editingId === specialty.id) {
        setForm(emptyForm);
        setEditingId(null);
      }

      await loadSpecialties();
    } catch (err) {
      setError(err.message || "Không xóa được chuyên khoa.");
    }
  };

  return (
    <main className="container py-5">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">
            Quản lý chuyên khoa
          </h1>

          <p className="text-muted mb-0">
            Thêm, cập nhật hoặc xóa chuyên khoa khám bệnh.
          </p>
        </div>

        <button
          type="button"
          className="btn btn-outline-secondary"
          onClick={() => navigate("/")}
        >
          Về trang chủ
        </button>
      </div>

      {error && (
        <div className="alert alert-danger">
          {error}
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          {success}
        </div>
      )}

      <div className="row g-4">
        <div className="col-12 col-lg-4">
          <div className="card shadow-sm">
            <div className="card-body">
              <h2 className="h6 fw-bold mb-3">
                {editingId
                  ? "Cập nhật chuyên khoa"
                  : "Thêm chuyên khoa mới"}
              </h2>

              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label
                    htmlFor="specialty-name"
                    className="form-label"
                  >
                    Tên chuyên khoa
                  </label>

                  <input
                    id="specialty-name"
                    name="name"
                    type="text"
                    className="form-control"
                    value={form.name}
                    onChange={handleChange}
                    placeholder="Ví dụ: Nội tổng quát"
                    required
                  />
                </div>

                <div className="mb-3">
                  <label
                    htmlFor="specialty-description"
                    className="form-label"
                  >
                    Mô tả
                  </label>

                  <textarea
                    id="specialty-description"
                    name="description"
                    className="form-control"
                    rows={4}
                    value={form.description}
                    onChange={handleChange}
                    placeholder="Mô tả ngắn về chuyên khoa"
                  />
                </div>

                <div className="d-flex gap-2">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={saving}
                  >
                    {saving
                      ? "Đang lưu..."
                      : editingId
                        ? "Cập nhật"
                        : "Thêm chuyên khoa"}
                  </button>

                  {editingId && (
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={resetForm}
                    >
                      Hủy
                    </button>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-8">
          <div className="card shadow-sm">
            <div className="card-body">
              <h2 className="h6 fw-bold mb-3">
                Danh sách chuyên khoa
              </h2>

              {loading ? (
                <p className="text-muted mb-0">
                  Đang tải danh sách chuyên khoa...
                </p>
              ) : specialties.length === 0 ? (
                <p className="text-muted mb-0">
                  Chưa có chuyên khoa nào.
                </p>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead>
                      <tr>
                        <th scope="col">ID</th>
                        <th scope="col">Tên</th>
                        <th scope="col">Mô tả</th>
                        <th
                          scope="col"
                          className="text-end"
                        >
                          Thao tác
                        </th>
                      </tr>
                    </thead>

                    <tbody>
                      {specialties.map((specialty) => (
                        <tr key={specialty.id}>
                          <td>{specialty.id}</td>
                          <td className="fw-semibold">
                            {specialty.name}
                          </td>
                          <td>
                            {specialty.description || "—"}
                          </td>
                          <td className="text-end">
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-primary me-2"
                              onClick={() =>
                                handleEdit(specialty)
                              }
                            >
                              Sửa
                            </button>

                            <button
                              type="button"
                              className="btn btn-sm btn-outline-danger"
                              onClick={() =>
                                handleDelete(specialty)
                              }
                            >
                              Xóa
                            </button>
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
    </main>
  );
}