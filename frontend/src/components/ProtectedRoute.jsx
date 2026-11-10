import { Navigate, useLocation } from "react-router-dom";

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

export default function ProtectedRoute({
  children,
  allowedRoles = [],
  patientOnly = false,
}) {
  const location = useLocation();
  const user = readCurrentUser();

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location.pathname + location.search,
        }}
      />
    );
  }

  const roles = Array.isArray(user.roles) ? user.roles : [];
  const isAdmin = roles.includes("ROLE_ADMIN");
  const isDoctor = roles.includes("ROLE_DOCTOR");
  const isPatient = roles.includes("ROLE_PATIENT");

  if (patientOnly) {
    if (isAdmin) {
      return <Navigate to="/admin/specialties" replace />;
    }

    if (isDoctor) {
      return <Navigate to="/doctor/appointments" replace />;
    }

    if (!isPatient) {
      return <Navigate to="/" replace />;
    }
  }

  const hasAllowedRole =
    allowedRoles.length === 0 ||
    roles.some((role) => allowedRoles.includes(role));

  if (!hasAllowedRole) {
    return <Navigate to="/" replace />;
  }

  return children;
}