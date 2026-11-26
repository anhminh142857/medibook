package com.medibook.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;

import jakarta.persistence.LockModeType;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    boolean existsByAppointment_IdAndStatus(
            Long appointmentId,
            PaymentStatus status
    );

    Optional<PaymentTransaction>
    findFirstByAppointment_IdOrderByCreatedAtDescIdDesc(
            Long appointmentId
    );

    List<PaymentTransaction>
    findAllByAppointment_IdOrderByCreatedAtDescIdDesc(
            Long appointmentId
    );

    List<PaymentTransaction>
    findAllByAppointment_IdAndProviderOrderByCreatedAtDescIdDesc(
            Long appointmentId,
            String provider
    );

    @Query("""
            select p.appointment.id
            from PaymentTransaction p
            where p.txnRef = :txnRef
            """)
    Optional<Long> findAppointmentIdByTxnRef(
            @Param("txnRef") String txnRef
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from PaymentTransaction p
            where p.txnRef = :txnRef
            """)
    Optional<PaymentTransaction> findByTxnRefForUpdate(
            @Param("txnRef") String txnRef
    );
}