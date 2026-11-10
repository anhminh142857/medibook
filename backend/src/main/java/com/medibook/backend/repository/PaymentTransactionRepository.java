package com.medibook.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibook.backend.entity.PaymentStatus;
import com.medibook.backend.entity.PaymentTransaction;

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
}