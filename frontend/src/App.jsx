import {
  BrowserRouter,
  Routes,
  Route,
  Link,
} from "react-router-dom";

import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";

import Home from "./pages/Home";
import Specialties from "./pages/Specialties";
import Doctors from "./pages/Doctors";
import Login from "./pages/Login";
import Register from "./pages/Register";
import MyAppointments from "./pages/MyAppointments";
import Booking from "./pages/Booking";
import AdminSpecialties from "./pages/AdminSpecialties";
import AdminDoctors from "./pages/AdminDoctors";
import AdminAppointmentSlots from "./pages/AdminAppointmentSlots";
import DoctorAppointments from "./pages/DoctorAppointments";

import "./index.css";
import "./App.css";

function NotFound() {
  return (
    <div className="container py-5">
      <div className="card shadow-sm">
        <div className="card-body text-center py-5">
          <h2>Không tìm thấy trang</h2>

          <p className="text-muted">
            Đường dẫn bạn truy cập không tồn tại.
          </p>

          <Link to="/" className="btn btn-primary">
            Về trang chủ
          </Link>
        </div>
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <div className="app-layout">
        <Navbar />

        <div className="app-content">
          <Routes>
            <Route path="/" element={<Home />} />

            <Route
              path="/specialties"
              element={<Specialties />}
            />

            <Route
              path="/doctors"
              element={<Doctors />}
            />

            <Route
              path="/login"
              element={<Login />}
            />

            <Route
              path="/register"
              element={<Register />}
            />

            <Route
              path="/booking"
              element={
                <ProtectedRoute
                  allowedRoles={["ROLE_PATIENT"]}
                  patientOnly
                >
                  <Booking />
                </ProtectedRoute>
              }
            />

            <Route
              path="/my-appointments"
              element={
                <ProtectedRoute
                  allowedRoles={["ROLE_PATIENT"]}
                  patientOnly
                >
                  <MyAppointments />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin/specialties"
              element={
                <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                  <AdminSpecialties />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin/doctors"
              element={
                <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                  <AdminDoctors />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin/appointment-slots"
              element={
                <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                  <AdminAppointmentSlots />
                </ProtectedRoute>
              }
            />

            <Route
              path="/doctor/appointments"
              element={
                <ProtectedRoute allowedRoles={["ROLE_DOCTOR"]}>
                  <DoctorAppointments />
                </ProtectedRoute>
              }
            />

            <Route path="*" element={<NotFound />} />
          </Routes>
        </div>

        <footer className="app-footer">
          MediBook — Hệ thống đặt lịch khám bệnh
        </footer>
      </div>
    </BrowserRouter>
  );
}

export default App;