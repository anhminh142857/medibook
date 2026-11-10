import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import {
  createDoctor,
  getDoctors,
  updateDoctor,
} from "../api/doctorApi";

import { getSpecialties } from "../api/specialtyApi";
import { linkDoctorAccount } from "../api/doctorAccountApi";

const EMPTY_FORM = {
  fullName: "",
  phone: "",
  bio: "",
  specialtyId: "",
};

export default function AdminDoctors() {
  const [doctors, setDoctors] = useState([]);
  const [specialties, setSpecialties] = useState([]);

  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [editingId, setEditingId] = useState(null);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [accountEmail, setAccountEmail] = useState("");
  const [linking, setLinking] = useState(false);
  const [linkError, setLinkError] = useState("");
  const [linkSuccess, setLinkSuccess] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadData() {
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
          setError(
            err.message || "Không tải được dữ liệu quản lý bác sĩ."
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

  function resetForm() {
    setForm({ ...EMPTY_FORM });
    setEditingId(null);
  }

  function handleCancelEdit() {
    resetForm();
    setError("");
    setSuccess("");
  }

  function handleEdit(doctor) {
    setEditingId(doctor.id);

    setForm({
      fullName: doctor.fullName || "",
      phone: doctor.phone || "",
      bio: doctor.bio || "",
      specialtyId: String(doctor.specialtyId ?? ""),
    });

    setError("");
    setSuccess("");
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (saving || linking) {
      return;
    }

    setError("");
    setSuccess("");

    const payload = {
      fullName: form.fullName.trim(),
      phone: form.phone.trim(),
      bio: form.bio.trim(),
      specialtyId: Number(form.specialtyId),
    };

    if (!payload.fullName) {
      setError("Vui lòng nhập họ tên bác sĩ.");
      return;
    }

    if (
      !Number.isSafeInteger(payload.specialtyId) ||
      payload.specialtyId <= 0
    ) {
      setError("Vui lòng chọn chuyên khoa.");
      return;
    }

    setSaving(true);

    try {
      const isEditing = editingId !== null;

      const savedDoctor = isEditing
        ? await updateDoctor(editingId, payload)
        : await createDoctor(payload);

      setDoctors((previous) =>
        isEditing
          ? previous.map((doctor) =>
              doctor.id === savedDoctor.id ? savedDoctor : doctor
            )
          : [...previous, savedDoctor]
      );

      if (selectedDoctor?.id === savedDoctor.id) {
        setSelectedDoctor(savedDoctor);
      }

      resetForm();

      setSuccess(
        isEditing
          ? "Đã cập nhật thông tin bác sĩ."
          : "Đã thêm bác sĩ."
      );
    } catch (err) {
      setError(err.message || "Không lưu được thông tin bác sĩ.");
    } finally {
      setSaving(false);
    }
  }

  function openLinkForm(doctor) {
    setSelectedDoctor(doctor);
    setAccountEmail("");
    setLinkError("");
    setLinkSuccess("");
  }

  function closeLinkForm() {
    setSelectedDoctor(null);
    setAccountEmail("");
    setLinkError("");
    setLinkSuccess("");
  }

  async function handleLinkAccount(event) {
    event.preventDefault();

    if (!selectedDoctor || linking || saving) {
      return;
    }

    const email = accountEmail.trim();

    setLinkError("");
    setLinkSuccess("");

    if (!email) {
      setLinkError("Vui lòng nhập email tài khoản bác sĩ.");
      return;
    }

    const confirmed = window.confirm(
      `Liên kết tài khoản "${email}" với bác sĩ ` +
        `"${selectedDoctor.fullName}" (ID ${selectedDoctor.id}) ` +
        `và cấp quyền ROLE_DOCTOR?`
    );

    if (!confirmed) {
      return;
    }

    setLinking(true);

    try {
      const result = await linkDoctorAccount(
        selectedDoctor.id,
        email
      );

      setLinkSuccess(
        `Đã liên kết tài khoản ${result.email} với bác sĩ ` +
          `${result.doctorName}. Tài khoản đã được cấp ROLE_DOCTOR.`
      );

      setAccountEmail("");
    } catch (err) {
      setLinkError(
        err.message || "Không liên kết được tài khoản bác sĩ."
      );
    } finally {
      setLinking(false);
    }
  }

  const busy = saving || linking;

  return (
    <main className="container py-5">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">
            Quản lý bác sĩ
          </h1>

          <p className="text-muted mb-0">
            Quản lý hồ sơ, chuyên khoa và liên kết tài khoản đăng nhập.
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
        <>
          <div className="row g-4">
            <div className="col-12 col-lg-4">
              <div className="card shadow-sm">
                <div className="card-body">
                  <h2 className="h5 fw-bold mb-3">
                    {editingId !== null
                      ? "Cập nhật bác sĩ"
                      : "Thêm bác sĩ"}
                  </h2>

                  {specialties.length === 0 && (
                    <div className="alert alert-warning">
                      Chưa có chuyên khoa.{" "}
                      <Link to="/admin/specialties">
                        Thêm chuyên khoa trước
                      </Link>
                      , sau đó quay lại trang này.
                    </div>
                  )}

                  <form onSubmit={handleSubmit}>
                    <fieldset
                      disabled={busy || specialties.length === 0}
                    >
                      <div className="mb-3">
                        <label
                          htmlFor="doctor-full-name"
                          className="form-label"
                        >
                          Họ tên bác sĩ
                        </label>

                        <input
                          id="doctor-full-name"
                          name="fullName"
                          className="form-control"
                          value={form.fullName}
                          onChange={handleChange}
                          maxLength={100}
                          required
                        />
                      </div>

                      <div className="mb-3">
                        <label
                          htmlFor="doctor-phone"
                          className="form-label"
                        >
                          Số điện thoại
                        </label>

                        <input
                          id="doctor-phone"
                          name="phone"
                          type="tel"
                          className="form-control"
                          value={form.phone}
                          onChange={handleChange}
                          maxLength={20}
                        />
                      </div>

                      <div className="mb-3">
                        <label
                          htmlFor="doctor-specialty"
                          className="form-label"
                        >
                          Chuyên khoa
                        </label>

                        <select
                          id="doctor-specialty"
                          name="specialtyId"
                          className="form-select"
                          value={form.specialtyId}
                          onChange={handleChange}
                          required
                        >
                          <option value="">
                            -- Chọn chuyên khoa --
                          </option>

                          {specialties.map((specialty) => (
                            <option
                              key={specialty.id}
                              value={specialty.id}
                            >
                              {specialty.name}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="mb-3">
                        <label
                          htmlFor="doctor-bio"
                          className="form-label"
                        >
                          Giới thiệu
                        </label>

                        <textarea
                          id="doctor-bio"
                          name="bio"
                          className="form-control"
                          rows={4}
                          maxLength={255}
                          value={form.bio}
                          onChange={handleChange}
                        />

                        <div className="form-text">
                          {form.bio.length}/255 ký tự
                        </div>
                      </div>

                      <div className="d-flex flex-wrap gap-2">
                        <button
                          type="submit"
                          className="btn btn-primary"
                        >
                          {saving
                            ? "Đang lưu..."
                            : editingId !== null
                              ? "Cập nhật"
                              : "Thêm bác sĩ"}
                        </button>

                        {editingId !== null && (
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={handleCancelEdit}
                          >
                            Hủy chỉnh sửa
                          </button>
                        )}
                      </div>
                    </fieldset>
                  </form>
                </div>
              </div>
            </div>

            <div className="col-12 col-lg-8">
              <div className="card shadow-sm">
                <div className="card-body">
                  <h2 className="h5 fw-bold mb-3">
                    Danh sách bác sĩ ({doctors.length})
                  </h2>

                  {doctors.length === 0 ? (
                    <p className="text-muted mb-0">
                      Chưa có bác sĩ nào.
                    </p>
                  ) : (
                    <div className="table-responsive">
                      <table className="table table-hover align-middle mb-0">
                        <thead>
                          <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Bác sĩ</th>
                            <th scope="col">Chuyên khoa</th>
                            <th scope="col">Giới thiệu</th>
                            <th scope="col" className="text-end">
                              Thao tác
                            </th>
                          </tr>
                        </thead>

                        <tbody>
                          {doctors.map((doctor) => (
                            <tr key={doctor.id}>
                              <td>{doctor.id}</td>

                              <td>
                                <div className="fw-semibold">
                                  {doctor.fullName}
                                </div>

                                <div className="small text-muted">
                                  {doctor.phone || "Chưa có SĐT"}
                                </div>
                              </td>

                              <td>{doctor.specialtyName}</td>

                              <td className="text-break">
                                {doctor.bio || "—"}
                              </td>

                              <td>
                                <div className="d-flex flex-wrap justify-content-end gap-2">
                                  <button
                                    type="button"
                                    className="btn btn-sm btn-outline-primary"
                                    disabled={busy}
                                    onClick={() => handleEdit(doctor)}
                                  >
                                    Sửa
                                  </button>

                                  <button
                                    type="button"
                                    className="btn btn-sm btn-outline-success"
                                    disabled={busy}
                                    onClick={() => openLinkForm(doctor)}
                                  >
                                    Liên kết tài khoản
                                  </button>
                                </div>
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

          {selectedDoctor && (
            <section className="card shadow-sm mt-4">
              <div className="card-body">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                  <h2 className="h5 fw-bold mb-0">
                    Liên kết tài khoản bác sĩ
                  </h2>

                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    disabled={busy}
                    onClick={closeLinkForm}
                  >
                    Đóng
                  </button>
                </div>

                <p className="mb-2">
                  Hồ sơ được chọn:{" "}
                  <span className="fw-semibold">
                    {selectedDoctor.fullName}
                  </span>
                  {" "}(ID: {selectedDoctor.id})
                </p>

                <p className="text-muted small">
                  Nhập email tài khoản đã đăng ký. Thao tác này cấp
                  ROLE_DOCTOR và giữ nguyên mật khẩu, các vai trò cũ.
                  Không dùng email admin. Sau khi liên kết, tài khoản
                  bác sĩ cần đăng xuất rồi đăng nhập lại.
                </p>

                {linkError && (
                  <div className="alert alert-danger" role="alert">
                    {linkError}
                  </div>
                )}

                {linkSuccess && (
                  <div className="alert alert-success" role="status">
                    {linkSuccess}
                  </div>
                )}

                <form onSubmit={handleLinkAccount}>
                  <fieldset disabled={busy}>
                    <div className="row g-3 align-items-end">
                      <div className="col-12 col-md-8">
                        <label
                          htmlFor="doctor-account-email"
                          className="form-label"
                        >
                          Email tài khoản bác sĩ
                        </label>

                        <input
                          id="doctor-account-email"
                          type="email"
                          className="form-control"
                          value={accountEmail}
                          onChange={(event) =>
                            setAccountEmail(event.target.value)
                          }
                          maxLength={255}
                          placeholder="Ví dụ: bacsi@example.com"
                          required
                        />
                      </div>

                      <div className="col-12 col-md-4">
                        <button
                          type="submit"
                          className="btn btn-success"
                        >
                          {linking
                            ? "Đang liên kết..."
                            : "Xác nhận liên kết"}
                        </button>
                      </div>
                    </div>
                  </fieldset>
                </form>
              </div>
            </section>
          )}
        </>
      )}
    </main>
  );
}