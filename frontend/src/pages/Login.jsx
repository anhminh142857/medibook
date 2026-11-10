import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginUser } from "../api/authApi";
import { loginWithGoogle } from "../api/googleAuthApi";
import GoogleLoginButton from "../components/GoogleLoginButton";

export default function Login() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  function finishLogin(result) {
    if (
      typeof result?.token !== "string" ||
      result.token.trim() === ""
    ) {
      throw new Error("Backend chưa trả về token đăng nhập hợp lệ.");
    }

    const { token, ...user } = result;

    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(user));

    window.dispatchEvent(new Event("auth-changed"));

    const isPatient =
      Array.isArray(user.roles) &&
      user.roles.includes("ROLE_PATIENT");

    if (isPatient) {
      navigate("/my-appointments", { replace: true });
    } else {
      navigate("/", { replace: true });
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setLoading(true);

    try {
      const result = await loginUser({
        email: form.email.trim(),
        password: form.password,
      });

      finishLogin(result);
    } catch (err) {
      setError(err.message || "Đăng nhập thất bại. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  async function handleGoogleCredential(credential) {
    if (loading) {
      return;
    }

    setError("");
    setLoading(true);

    try {
      const result = await loginWithGoogle(credential);

      finishLogin(result);
    } catch (err) {
      setError(
        err.message || "Đăng nhập Google thất bại. Vui lòng thử lại."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-5">
          <div className="card shadow-sm">
            <div className="card-body">
              <h2 className="text-center mb-2">Đăng nhập</h2>

              <p className="text-muted text-center mb-4">
                Đăng nhập để đặt lịch và quản lý lịch hẹn của bạn.
              </p>

              {error && (
                <div className="alert alert-danger" role="alert">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label htmlFor="loginEmail" className="form-label">
                    Email
                  </label>

                  <input
                    id="loginEmail"
                    type="email"
                    className="form-control"
                    name="email"
                    value={form.email}
                    onChange={handleChange}
                    autoComplete="username"
                    maxLength={255}
                    placeholder="Nhập email của bạn"
                    disabled={loading}
                    required
                  />
                </div>

                <div className="mb-4">
                  <label htmlFor="loginPassword" className="form-label">
                    Mật khẩu
                  </label>

                  <input
                    id="loginPassword"
                    type="password"
                    className="form-control"
                    name="password"
                    value={form.password}
                    onChange={handleChange}
                    autoComplete="current-password"
                    placeholder="Nhập mật khẩu"
                    disabled={loading}
                    required
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-primary w-100"
                  disabled={loading}
                >
                  {loading ? "Đang đăng nhập..." : "Đăng nhập"}
                </button>
              </form>

              <div className="d-flex align-items-center gap-3 my-4">
                <hr className="flex-grow-1 m-0" />
                <span className="text-muted small">hoặc</span>
                <hr className="flex-grow-1 m-0" />
              </div>

              <GoogleLoginButton onCredential={handleGoogleCredential} />

              <p className="text-center mt-4 mb-0">
                Chưa có tài khoản?{" "}
                <Link to="/register">Đăng ký</Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}