package com.medibook.backend.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.backend.entity.Appointment;
import com.medibook.backend.entity.AppointmentSlot;
import com.medibook.backend.entity.Doctor;
import com.medibook.backend.entity.Role;
import com.medibook.backend.entity.Specialty;
import com.medibook.backend.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
@Order(100)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataSeeder.class);

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final LocalDate START_DATE =
            LocalDate.of(2026, 11, 18);

    private static final LocalDate END_DATE =
            LocalDate.of(2026, 12, 31);

    private static final LocalTime[] START_TIMES = {
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(13, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0)
    };

    @PersistenceContext
    private EntityManager em;

    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String seedPassword;

    public DataSeeder(
            PasswordEncoder passwordEncoder,
            @Value("${MEDIBOOK_SEED_ENABLED:false}") boolean enabled,
            @Value("${MEDIBOOK_SEED_PASSWORD:}") String seedPassword
    ) {
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String database = String.valueOf(
                em.createNativeQuery("SELECT DATABASE()")
                        .getSingleResult()
        );

        if (!"medibook_demo".equals(database)) {
            throw new IllegalStateException(
                    "Đã dừng seed để bảo vệ dữ liệu. "
                            + "Chỉ được chạy seed trên database medibook_demo. "
                            + "Database hiện tại: " + database
            );
        }

        if (seedPassword == null
                || seedPassword.isBlank()
                || seedPassword.length() < 12
                || seedPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "MEDIBOOK_SEED_PASSWORD phải có ít nhất 12 ký tự "
                            + "và không quá 72 byte UTF-8."
            );
        }

        LocalDateTime now = LocalDateTime.now(ZONE);

        if (now.toLocalDate().isAfter(END_DATE)) {
            throw new IllegalStateException(
                    "Bộ dữ liệu này dành cho thời gian đến 31/12/2026."
            );
        }

        Role adminRole = findOrCreateRole("ROLE_ADMIN");
        Role doctorRole = findOrCreateRole("ROLE_DOCTOR");
        Role patientRole = findOrCreateRole("ROLE_PATIENT");

        findOrCreateUser(
                "Quản trị viên Demo",
                "admin.demo@medibook.local",
                "0900000001",
                adminRole
        );

        String[] doctorNames = {
                "BS. Nguyễn Văn An",
                "BS. Trần Thị Bình",
                "BS. Lê Minh Châu",
                "BS. Phạm Quốc Dũng",
                "BS. Hoàng Thu Hà",
                "BS. Võ Thanh Sơn"
        };

        String[] specialtyNames = {
                "Nội tổng quát",
                "Nhi khoa",
                "Tim mạch",
                "Da liễu",
                "Tai mũi họng",
                "Răng hàm mặt"
        };

        String[] specialtyDescriptions = {
                "Khám sức khỏe tổng quát và tư vấn các vấn đề nội khoa thường gặp.",
                "Khám và tư vấn chăm sóc sức khỏe cho trẻ em.",
                "Khám, tư vấn và theo dõi các vấn đề về tim mạch.",
                "Khám và tư vấn các vấn đề về da, tóc và móng.",
                "Khám và tư vấn các bệnh lý về tai, mũi và họng.",
                "Khám và tư vấn chăm sóc sức khỏe răng miệng."
        };

        String[] doctorBios = {
                "Bác sĩ Nội tổng quát, tư vấn chăm sóc sức khỏe và khám định kỳ.",
                "Bác sĩ Nhi khoa, tư vấn chăm sóc sức khỏe và dinh dưỡng trẻ em.",
                "Bác sĩ Tim mạch, tư vấn theo dõi huyết áp và sức khỏe tim mạch.",
                "Bác sĩ Da liễu, tư vấn chăm sóc da và các vấn đề dị ứng.",
                "Bác sĩ Tai mũi họng, tư vấn chăm sóc sức khỏe đường hô hấp trên.",
                "Bác sĩ Răng hàm mặt, tư vấn vệ sinh và chăm sóc sức khỏe răng miệng."
        };

        Doctor[] doctors = new Doctor[doctorNames.length];

        for (int i = 0; i < doctorNames.length; i++) {
            User doctorUser = findOrCreateUser(
                    doctorNames[i],
                    "bacsi" + (i + 1) + "@medibook.local",
                    "090100000" + (i + 1),
                    doctorRole
            );

            Specialty specialty = findOrCreateSpecialty(
                    specialtyNames[i],
                    specialtyDescriptions[i]
            );

            // Bác sĩ 6 để trống liên kết nhằm kiểm thử chức năng liên kết.
            User linkedUser = i == 5 ? null : doctorUser;

            doctors[i] = findOrCreateDoctor(
                    doctorNames[i],
                    "090100000" + (i + 1),
                    doctorBios[i],
                    specialty,
                    linkedUser
            );
        }

        User p1 = findOrCreateUser(
                "Nguyễn Văn Nam",
                "benhnhan1@medibook.local",
                "0902000001",
                patientRole
        );

        User p2 = findOrCreateUser(
                "Trần Thị Lan",
                "benhnhan2@medibook.local",
                "0902000002",
                patientRole
        );

        User p3 = findOrCreateUser(
                "Lê Văn Hùng",
                "benhnhan3@medibook.local",
                "0902000003",
                patientRole
        );

        int newSlots = 0;

        for (Doctor doctor : doctors) {
            for (LocalDate date = START_DATE;
                 !date.isAfter(END_DATE);
                 date = date.plusDays(1)) {

                for (LocalTime start : START_TIMES) {
                    if (!LocalDateTime.of(date, start).isAfter(now)) {
                        continue;
                    }

                    if (findSlot(doctor, date, start) == null) {
                        findOrCreateSlot(doctor, date, start);
                        newSlots++;
                    }
                }
            }
        }

        // Ngày cố định giúp chạy lại không phát sinh một bộ lịch hẹn mới.
        seedAppointment(
                p1, doctors[0],
                START_DATE.plusDays(1), 8,
                "PENDING"
        );

        seedAppointment(
                p1, doctors[1],
                START_DATE.plusDays(2), 9,
                "CONFIRMED"
        );

        seedAppointment(
                p2, doctors[0],
                START_DATE.plusDays(1), 9,
                "CONFIRMED"
        );

        seedAppointment(
                p3, doctors[1],
                START_DATE.plusDays(3), 14,
                "CANCELLED"
        );

        // Lịch đã qua giờ, dùng thử thao tác "Hoàn thành".
        seedAppointment(
                p2, doctors[2],
                START_DATE.minusDays(1), 9,
                "CONFIRMED"
        );

        // Lịch sử đã hoàn thành.
        seedAppointment(
                p1, doctors[2],
                START_DATE.minusDays(2), 9,
                "COMPLETED"
        );

        em.flush();

        log.info(
                "[SEED] Hoàn tất dữ liệu mẫu trên database {}. "
                        + "Đã thêm {} khung giờ tương lai. "
                        + "Ngày kết thúc: {}.",
                database,
                newSlots,
                END_DATE
        );

        log.info(
                "[SEED] Tài khoản demo: admin.demo, bacsi1..bacsi6, "
                        + "benhnhan1..benhnhan3, tên miền @medibook.local."
        );

        log.info(
                "[SEED] Không tạo giao dịch thanh toán và không gửi email."
        );
    }

    private Role findOrCreateRole(String name) {
        List<Role> found = em.createQuery(
                        "select r from Role r where r.name = :name",
                        Role.class
                )
                .setParameter("name", name)
                .getResultList();

        if (!found.isEmpty()) {
            return found.get(0);
        }

        Role role = new Role(name);
        em.persist(role);
        return role;
    }

    private User findOrCreateUser(
            String fullName,
            String email,
            String phone,
            Role role
    ) {
        List<User> found = em.createQuery(
                        "select u from User u where u.email = :email",
                        User.class
                )
                .setParameter("email", email)
                .getResultList();

        if (!found.isEmpty()) {
            return found.get(0);
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(seedPassword));
        user.setProvider("LOCAL");
        user.setStatus("ACTIVE");
        user.getRoles().add(role);

        em.persist(user);
        return user;
    }

    private Specialty findOrCreateSpecialty(
            String name,
            String description
    ) {
        List<Specialty> found = em.createQuery(
                        "select s from Specialty s where s.name = :name",
                        Specialty.class
                )
                .setParameter("name", name)
                .getResultList();

        if (!found.isEmpty()) {
            return found.get(0);
        }

        Specialty specialty = new Specialty();
        specialty.setName(name);
        specialty.setDescription(description);

        em.persist(specialty);
        return specialty;
    }

    private Doctor findOrCreateDoctor(
            String fullName,
            String phone,
            String bio,
            Specialty specialty,
            User user
    ) {
        List<Doctor> found = em.createQuery(
                        "select d from Doctor d where d.fullName = :name",
                        Doctor.class
                )
                .setParameter("name", fullName)
                .getResultList();

        if (!found.isEmpty()) {
            return found.get(0);
        }

        Doctor doctor = new Doctor();
        doctor.setFullName(fullName);
        doctor.setPhone(phone);
        doctor.setBio(bio);
        doctor.setSpecialty(specialty);
        doctor.setUser(user);

        em.persist(doctor);
        return doctor;
    }

    private AppointmentSlot findSlot(
            Doctor doctor,
            LocalDate date,
            LocalTime start
    ) {
        List<AppointmentSlot> found = em.createQuery(
                        """
                        select s from AppointmentSlot s
                        where s.doctor = :doctor
                          and s.slotDate = :date
                          and s.startTime = :start
                        """,
                        AppointmentSlot.class
                )
                .setParameter("doctor", doctor)
                .setParameter("date", date)
                .setParameter("start", start)
                .getResultList();

        return found.isEmpty() ? null : found.get(0);
    }

    private AppointmentSlot findOrCreateSlot(
            Doctor doctor,
            LocalDate date,
            LocalTime start
    ) {
        AppointmentSlot existing = findSlot(doctor, date, start);

        if (existing != null) {
            return existing;
        }

        AppointmentSlot slot = new AppointmentSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(date);
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setStatus("AVAILABLE");

        em.persist(slot);
        return slot;
    }

    private void seedAppointment(
            User patient,
            Doctor doctor,
            LocalDate date,
            int hour,
            String status
    ) {
        AppointmentSlot slot = findOrCreateSlot(
                doctor,
                date,
                LocalTime.of(hour, 0)
        );

        Long existing = em.createQuery(
                        "select count(a) from Appointment a where a.slot = :slot",
                        Long.class
                )
                .setParameter("slot", slot)
                .getSingleResult();

        if (existing > 0 || !"AVAILABLE".equals(slot.getStatus())) {
            return;
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setStatus(status);

        // Thời điểm tạo trước ngày khám để dữ liệu lịch sử hợp lý.
        appointment.setCreatedAt(
                date.minusDays(1).atTime(7, 0)
        );

        em.persist(appointment);

        if (!"CANCELLED".equals(status)) {
            slot.setStatus("BOOKED");
        }
    }
}