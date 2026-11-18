import { useState } from "react";
import { Link } from "react-router-dom";
import { registerUser } from "../api/authApi";


const emptyForm = {
  fullName: "",
  email: "",
  password: "",
  confirmPassword: "",
  phone: "",
};


export default function Register() {
  const [form, setForm] = useState(emptyForm);


  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);


  const passwordMismatch =
    form.confirmPassword !== "" &&
    form.password !== form.confirmPassword;


  const handleChange = (e) => {
    const { name, value } = e.target;


    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  };


  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");


    if (form.password !== form.confirmPassword) {
      setError("Mật khẩu nhập lại không khớp.");
      return;
    }


    setLoading(true);


    try {
      const result = await registerUser({
        fullName: form.fullName,
        email: form.email,
        password: form.password,
        phone: form.phone,
      });


      setMessage(
        `Đăng ký thành công với email ${result.email}. Bạn có thể đăng nhập ngay.`
      );
      setForm(emptyForm);
    } catch (err) {
      setError(err.message || "Đăng ký thất bại. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-5">
          <div className="card shadow-sm">
            <div className="card-body">
              <h2 className="text-center mb-2">Đăng ký tài khoản</h2>


              <p className="text-muted text-center mb-4">
                Tạo tài khoản bệnh nhân để đặt lịch và theo dõi lịch khám.
              </p>


              {message && (
                <div className="alert alert-success" role="alert">
                  {message}{" "}
                  <Link to="/login" className="alert-link">
                    Đăng nhập
                  </Link>
                </div>
              )}


              {error && (
                <div className="alert alert-danger" role="alert">
                  {error}
                </div>
              )}


              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label htmlFor="registerFullName" className="form-label">
                    Họ và tên
                  </label>


                  <input
                    id="registerFullName"
                    type="text"
                    className="form-control"
                    name="fullName"
                    value={form.fullName}
                    onChange={handleChange}
                    autoComplete="name"
                    maxLength={100}
                    placeholder="Nhập họ và tên"
                    disabled={loading}
                    required
                  />
                </div>


                <div className="mb-3">
                  <label htmlFor="registerEmail" className="form-label">
                    Email
                  </label>


                  <input
                    id="registerEmail"
                    type="email"
                    className="form-control"
                    name="email"
                    value={form.email}
                    onChange={handleChange}
                    autoComplete="email"
                    maxLength={255}
                    placeholder="Nhập email của bạn"
                    disabled={loading}
                    required
                  />
                </div>


                <div className="mb-3">
                  <label htmlFor="registerPassword" className="form-label">
                    Mật khẩu
                  </label>


                  <input
                    id="registerPassword"
                    type="password"
                    className="form-control"
                    name="password"
                    value={form.password}
                    onChange={handleChange}
                    autoComplete="new-password"
                    placeholder="Nhập mật khẩu"
                    disabled={loading}
                    required
                  />
                </div>


                <div className="mb-3">
                  <label
                    htmlFor="registerConfirmPassword"
                    className="form-label"
                  >
                    Nhập lại mật khẩu
                  </label>


                  <input
                    id="registerConfirmPassword"
                    type="password"
                    className={`form-control ${
                      passwordMismatch ? "is-invalid" : ""
                    }`}
                    name="confirmPassword"
                    value={form.confirmPassword}
                    onChange={handleChange}
                    autoComplete="new-password"
                    placeholder="Nhập lại mật khẩu"
                    disabled={loading}
                    required
                  />


                  {passwordMismatch && (
                    <div className="invalid-feedback">
                      Mật khẩu nhập lại không khớp.
                    </div>
                  )}
                </div>


                <div className="mb-4">
                  <label htmlFor="registerPhone" className="form-label">
                    Số điện thoại{" "}
                    <span className="text-muted small">(không bắt buộc)</span>
                  </label>


                  <input
                    id="registerPhone"
                    type="tel"
                    className="form-control"
                    name="phone"
                    value={form.phone}
                    onChange={handleChange}
                    autoComplete="tel"
                    maxLength={20}
                    placeholder="Nhập số điện thoại"
                    disabled={loading}
                  />
                </div>


                <button
                  type="submit"
                  className="btn btn-primary w-100"
                  disabled={loading}
                >
                  {loading ? "Đang xử lý..." : "Đăng ký"}
                </button>
              </form>


              <p className="text-center mt-4 mb-0">
                Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}