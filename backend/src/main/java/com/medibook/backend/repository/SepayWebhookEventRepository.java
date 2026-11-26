package com.medibook.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibook.backend.entity.SepayWebhookEvent;

public interface SepayWebhookEventRepository
        extends JpaRepository<SepayWebhookEvent, String> {
}