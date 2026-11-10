package com.medibook.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.medibook.backend.dto.AppointmentResponse;
import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.Doctor;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.DoctorRepository;

@Service
public class DoctorAppointmentService {

    private static final Logger log =
            LoggerFactory.getLogger(DoctorAppointmentService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final EmailNotificationService emailNotificationService;

    public DoctorAppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            EmailNotificationService emailNotificationService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(
            String doctorEmail
    ) {
        Doctor doctor = requireCurrentDoctor(doctorEmail);

        return appointmentRepository
                .findAllForDoctorId(doctor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse confirmAppointment(
            String doctorEmail,
            Long appointmentId
    ) {
        Doctor doctor = requireCurrentDoctor(doctorEmail);

        Appointment appointment =
                findOwnedAppointmentForUpdate(
                        doctor.getId(),
                        appointmentId
                );

        if (!"PENDING".equals(appointment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể xác nhận lịch hẹn đang chờ xử lý."
            );
        }

        requireBookedSlot(appointment);

        appointment.setStatus("CONFIRMED");

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        log.info(
                "Bac si id={} da xac nhan lich hen id={}",
                doctor.getId(),
                appointmentId
        );

        sendAppointmentConfirmedEmail(savedAppointment);

        return toResponse(savedAppointment);
    }

    @Transactional
    public AppointmentResponse completeAppointment(
            String doctorEmail,
            Long appointmentId
    ) {
        Doctor doctor = requireCurrentDoctor(doctorEmail);

        Appointment appointment =
                findOwnedAppointmentForUpdate(
                        doctor.getId(),
                        appointmentId
                );

        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể hoàn thành lịch hẹn đã xác nhận."
            );
        }

        requireBookedSlot(appointment);

        AppointmentSlot slot = appointment.getSlot();

        LocalDateTime endDateTime = LocalDateTime.of(
                slot.getSlotDate(),
                slot.getEndTime()
        );

        if (LocalDateTime.now().isBefore(endDateTime)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chưa đến giờ kết thúc khám, chưa thể hoàn thành lịch hẹn."
            );
        }

        appointment.setStatus("COMPLETED");

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        log.info(
                "Bac si id={} da hoan thanh lich hen id={}",
                doctor.getId(),
                appointmentId
        );

        sendAppointmentCompletedEmail(savedAppointment);

        return toResponse(savedAppointment);
    }

    private void sendAppointmentConfirmedEmail(Appointment appointment) {
        String appointmentTime = formatAppointmentTime(appointment);

        String subject = "MediBook - Lich kham da duoc xac nhan";

        String body = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #198754;">Lich kham da duoc xac nhan</h2>
                    <p>Xin chao <strong>%s</strong>,</p>
                    <p>Bac si <strong>%s</strong> da xac nhan lich kham cua ban.</p>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Ma lich hen</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Bac si</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Thoi gian</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Trang thai</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Da xac nhan</td>
                        </tr>
                    </table>
                    <p>Vui long den dung thoi gian kham.</p>
                    <p>Day la email thong bao tu dong tu MediBook.</p>
                </div>
                """.formatted(
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getFullName(),
                appointment.getId(),
                appointment.getDoctor().getFullName(),
                appointmentTime
        );

        emailNotificationService.sendAfterCommit(
                appointment.getPatient().getEmail(),
                subject,
                body
        );
    }

    private void sendAppointmentCompletedEmail(Appointment appointment) {
        String appointmentTime = formatAppointmentTime(appointment);

        String subject = "MediBook - Lich kham da hoan thanh";

        String body = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #0d6efd;">Lich kham da hoan thanh</h2>
                    <p>Xin chao <strong>%s</strong>,</p>
                    <p>Lich kham sau day da duoc hoan thanh.</p>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Ma lich hen</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Bac si</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Thoi gian</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Trang thai</td>
                            <td style="padding: 8px; border: 1px solid #dee2e6;">Hoan thanh</td>
                        </tr>
                    </table>
                    <p>Day la email thong bao tu dong tu MediBook.</p>
                </div>
                """.formatted(
                appointment.getPatient().getFullName(),
                appointment.getId(),
                appointment.getDoctor().getFullName(),
                appointmentTime
        );

        emailNotificationService.sendAfterCommit(
                appointment.getPatient().getEmail(),
                subject,
                body
        );
    }

    private String formatAppointmentTime(Appointment appointment) {
        AppointmentSlot slot = appointment.getSlot();

        LocalDateTime startDateTime = LocalDateTime.of(
                slot.getSlotDate(),
                slot.getStartTime()
        );

        LocalDateTime endDateTime = LocalDateTime.of(
                slot.getSlotDate(),
                slot.getEndTime()
        );

        return startDateTime.format(DATE_TIME_FORMATTER)
                + " - "
                + endDateTime.format(DATE_TIME_FORMATTER);
    }

    private Doctor requireCurrentDoctor(String email) {
        Doctor doctor = doctorRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản chưa được liên kết với hồ sơ bác sĩ."
                ));

        User user = doctor.getUser();

        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản bác sĩ không hoạt động."
            );
        }

        boolean hasDoctorRole = user.getRoles().stream()
                .anyMatch(role ->
                        "ROLE_DOCTOR".equals(role.getName())
                );

        if (!hasDoctorRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không có vai trò bác sĩ."
            );
        }

        return doctor;
    }

    private Appointment findOwnedAppointmentForUpdate(
            Long doctorId,
            Long appointmentId
    ) {
        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch hẹn."
                ));

        if (!doctorId.equals(appointment.getDoctor().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy lịch hẹn."
            );
        }

        return appointment;
    }

    private void requireBookedSlot(Appointment appointment) {
        if (!"BOOKED".equals(appointment.getSlot().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trạng thái lịch hẹn và khung giờ không đồng bộ."
            );
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        AppointmentSlot slot = appointment.getSlot();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                slot.getId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                appointment.getStatus()
        );
    }
}