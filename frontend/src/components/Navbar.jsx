import { useEffect, useState } from "react";
import {
  NavLink,
  useLocation,
  useNavigate,
} from "react-router-dom";

function readCurrentUser() {
  const token = localStorage.getItem("token");
  const savedUser = localStorage.getItem("user");

  if (
    !token ||
    token.trim() === "" ||
    token === "null" ||
    token === "undefined" ||
    !savedUser
  ) {
    return null;
  }

  try {
    const user = JSON.parse(savedUser);

    if (
      !user ||
      typeof user !== "object" ||
      Array.isArray(user) ||
      !user.email
    ) {
      return null;
    }

    return user;
  } catch {
    return null;
  }
}

export default function Navbar() {
  const [user, setUser] = useState(readCurrentUser);
  const [menuOpen, setMenuOpen] = useState(false);

  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    setUser(readCurrentUser());
    setMenuOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    function synchronizeUser() {
      setUser(readCurrentUser());
    }

    window.addEventListener("auth-changed", synchronizeUser);
    window.addEventListener("storage", synchronizeUser);
    window.addEventListener("focus", synchronizeUser);

    return () => {
      window.removeEventListener("auth-changed", synchronizeUser);
      window.removeEventListener("storage", synchronizeUser);
      window.removeEventListener("focus", synchronizeUser);
    };
  }, []);

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    setUser(null);
    setMenuOpen(false);

    window.dispatchEvent(new Event("auth-changed"));

    navigate("/login", { replace: true });
  }

  const roles = Array.isArray(user?.roles) ? user.roles : [];

  const isAdmin = roles.includes("ROLE_ADMIN");
  const isDoctor = roles.includes("ROLE_DOCTOR");

  const isPatientOnly =
    roles.includes("ROLE_PATIENT") && !isAdmin && !isDoctor;

  const roleLabel = isAdmin
    ? "Quản trị viên"
    : isDoctor
      ? "Bác sĩ"
      : isPatientOnly
        ? "Bệnh nhân"
        : "";

  const links = [
    { to: "/", label: "Trang chủ", end: true },
    { to: "/specialties", label: "Chuyên khoa" },
    { to: "/doctors", label: "Bác sĩ" },
  ];

  if (!user || isPatientOnly) {
    links.push({
      to: "/booking",
      label: "Đặt lịch",
    });
  }

  if (isPatientOnly) {
    links.push({
      to: "/my-appointments",
      label: "Lịch hẹn của tôi",
    });
  }

  if (isDoctor) {
    links.push({
      to: "/doctor/appointments",
      label: "Lịch khám của tôi",
    });
  }

  if (isAdmin) {
    links.push(
      {
        to: "/admin/specialties",
        label: "Quản lý chuyên khoa",
      },
      {
        to: "/admin/doctors",
        label: "Quản lý bác sĩ",
      },
      {
        to: "/admin/appointment-slots",
        label: "Quản lý khung giờ",
      }
    );
  }

  return (
    <nav
      className="navbar navbar-expand-xl navbar-dark bg-primary medibook-navbar"
      aria-label="Điều hướng chính"
    >
      <div className="container">
        <NavLink className="navbar-brand" to="/">
          MediBook
        </NavLink>

        <button
          className="navbar-toggler"
          type="button"
          aria-controls="navbarNav"
          aria-expanded={menuOpen}
          aria-label="Mở hoặc đóng menu"
          onClick={() => setMenuOpen((current) => !current)}
        >
          <span className="navbar-toggler-icon" />
        </button>

        <div
          id="navbarNav"
          className={`collapse navbar-collapse ${
            menuOpen ? "show" : ""
          }`}
        >
          <ul className="navbar-nav me-auto">
            {links.map((link) => (
              <li className="nav-item" key={link.to}>
                <NavLink
                  className="nav-link"
                  to={link.to}
                  end={link.end}
                  onClick={() => setMenuOpen(false)}
                >
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>

          {user ? (
            <div className="d-flex flex-column flex-xl-row align-items-xl-center gap-3 auth-actions">
              <div className="text-white text-xl-end">
                <div className="fw-semibold">
                  Xin chào, {user.fullName || user.email}
                </div>

                {roleLabel && (
                  <div className="small opacity-75">
                    {roleLabel}
                  </div>
                )}
              </div>

              <button
                type="button"
                className="btn btn-outline-light btn-sm"
                onClick={handleLogout}
              >
                Đăng xuất
              </button>
            </div>
          ) : (
            <ul className="navbar-nav">
              <li className="nav-item">
                <NavLink
                  className="nav-link"
                  to="/login"
                  onClick={() => setMenuOpen(false)}
                >
                  Đăng nhập
                </NavLink>
              </li>

              <li className="nav-item">
                <NavLink
                  className="nav-link"
                  to="/register"
                  onClick={() => setMenuOpen(false)}
                >
                  Đăng ký
                </NavLink>
              </li>
            </ul>
          )}
        </div>
      </div>
    </nav>
  );
}