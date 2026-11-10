package com.medibook.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import com.medibook.backend.dto.AppointmentRequest;
import com.medibook.backend.dto.AppointmentResponse;
import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.User;
import com.medibook.backend.repository.AppointmentRepository;
import com.medibook.backend.repository.AppointmentSlotRepository;
import com.medibook.backend.repository.UserRepository;

@Service
public class AppointmentService {

    private static final Logger log =
            LoggerFactory.getLogger(AppointmentService.class);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository appointmentSlotRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentSlotRepository appointmentSlotRepository,
            UserRepository userRepository,
            EmailNotificationService emailNotificationService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentSlotRepository = appointmentSlotRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public AppointmentResponse createAppointment(
            Long patientId,
            AppointmentRequest request
    ) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bệnh nhân không tồn tại."
                ));

        if (!"ACTIVE".equals(patient.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không hoạt động."
            );
        }

        boolean isStaff = patient.getRoles().stream()
                .anyMatch(role ->
                        "ROLE_DOCTOR".equals(role.getName())
                                || "ROLE_ADMIN".equals(role.getName())
                );

        if (isStaff) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản bác sĩ hoặc quản trị viên không được đặt lịch khám."
            );
        }

        boolean isPatient = patient.getRoles().stream()
                .anyMatch(role ->
                        "ROLE_PATIENT".equals(role.getName())
                );

        if (!isPatient) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không có vai trò bệnh nhân."
            );
        }

        if (request == null
                || request.getSlotId() == null
                || request.getSlotId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã khung giờ không hợp lệ."
            );
        }

        AppointmentSlot slot = appointmentSlotRepository
                .findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khung giờ không tồn tại."
                ));

        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Khung giờ không còn trống."
            );
        }

        LocalDateTime start = LocalDateTime.of(
                slot.getSlotDate(),
                slot.getStartTime()
        );

        if (!start.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể đặt khung giờ đã bắt đầu hoặc trong quá khứ."
            );
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(slot.getDoctor());
        appointment.setSlot(slot);
        appointment.setStatus("PENDING");

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        slot.setStatus("BOOKED");
        appointmentSlotRepository.save(slot);

        log.info(
                "Da tao lich hen id={} cho benh nhan id={}",
                savedAppointment.getId(),
                patient.getId()
        );

        scheduleNotification(savedAppointment, false);

        return toResponse(savedAppointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(
            String patientEmail,
            Long appointmentId
    ) {
        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lịch hẹn không tồn tại."
                ));

        if (!appointment.getPatient().getEmail().equals(patientEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không được hủy lịch hẹn của người khác."
            );
        }

        if ("CANCELLED".equals(appointment.getStatus())) {
            return toResponse(appointment);
        }

        if (!"PENDING".equals(appointment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể hủy lịch hẹn đang chờ xử lý."
            );
        }

        AppointmentSlot slot = appointmentSlotRepository
                .findByIdForUpdate(appointment.getSlot().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khung giờ không tồn tại."
                ));

        LocalDateTime start = LocalDateTime.of(
                slot.getSlotDate(),
                slot.getStartTime()
        );

        if (!start.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể hủy lịch hẹn đã đến giờ khám."
            );
        }

        if (!"BOOKED".equals(slot.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trạng thái lịch hẹn và khung giờ không đồng bộ."
            );
        }

        appointment.setStatus("CANCELLED");
        slot.setStatus("AVAILABLE");

        appointmentRepository.save(appointment);
        appointmentSlotRepository.save(slot);

        log.info(
                "Da huy lich hen id={} cua benh nhan id={}",
                appointment.getId(),
                appointment.getPatient().getId()
        );

        scheduleNotification(appointment, true);

        return toResponse(appointment);
    }

    private void scheduleNotification(
            Appointment appointment,
            boolean cancelled
    ) {
        try {
            AppointmentSlot slot = appointment.getSlot();

            String title = cancelled
                    ? "Lịch khám đã được hủy"
                    : "Đặt lịch khám thành công";

            String status = cancelled
                    ? "Đã hủy"
                    : "Chờ xác nhận";

            String note = cancelled
                    ? "Lịch hẹn đã được hủy. Bạn có thể quay lại MediBook để tìm khung giờ còn trống."
                    : "Lịch hẹn đang chờ bác sĩ xác nhận. Vui lòng theo dõi trạng thái trên MediBook.";

            String body = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;line-height:1.6;">
                        <h2 style="color:#0d6efd;">%s</h2>
                        <p>Xin chào <strong>%s</strong>,</p>
                        <table style="border-collapse:collapse;width:100%%;">
                            <tr>
                                <td style="padding:8px;border:1px solid #dee2e6;">Mã lịch hẹn</td>
                                <td style="padding:8px;border:1px solid #dee2e6;">#%s</td>
                            </tr>
                            <tr>
                                <td style="padding:8px;border:1px solid #dee2e6;">Bác sĩ</td>
                                <td style="padding:8px;border:1px solid #dee2e6;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:8px;border:1px solid #dee2e6;">Ngày khám</td>
                                <td style="padding:8px;border:1px solid #dee2e6;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:8px;border:1px solid #dee2e6;">Khung giờ</td>
                                <td style="padding:8px;border:1px solid #dee2e6;">%s – %s</td>
                            </tr>
                            <tr>
                                <td style="padding:8px;border:1px solid #dee2e6;">Trạng thái</td>
                                <td style="padding:8px;border:1px solid #dee2e6;">%s</td>
                            </tr>
                        </table>
                        <p>%s</p>
                        <p style="color:#6c757d;">Đây là email thông báo tự động từ MediBook.</p>
                    </div>
                    """.formatted(
                    title,
                    escape(appointment.getPatient().getFullName()),
                    appointment.getId(),
                    escape(appointment.getDoctor().getFullName()),
                    slot.getSlotDate().format(DATE_FORMAT),
                    slot.getStartTime().format(TIME_FORMAT),
                    slot.getEndTime().format(TIME_FORMAT),
                    status,
                    note
            );

            emailNotificationService.sendAfterCommit(
                    appointment.getPatient().getEmail(),
                    "MediBook - " + title,
                    body
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Khong the chuan bi email cho lich hen id={}. "
                            + "Khong huy thao tac nghiep vu vi loi thong bao.",
                    appointment.getId(),
                    exception
            );
        }
    }

    private String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
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