package com.medibook.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.dto.SpecialtyRequest;
import com.medibook.backend.dto.SpecialtyResponse;
import com.medibook.backend.service.SpecialtyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public ResponseEntity<List<SpecialtyResponse>> getAllSpecialties() {
        List<SpecialtyResponse> specialties =
                specialtyService.getAllSpecialties();

        return ResponseEntity.ok(specialties);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SpecialtyResponse> createSpecialty(
            @Valid @RequestBody SpecialtyRequest request
    ) {
        SpecialtyResponse response =
                specialtyService.createSpecialty(request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SpecialtyResponse> updateSpecialty(
            @PathVariable("id") Long id,
            @Valid @RequestBody SpecialtyRequest request
    ) {
        SpecialtyResponse response =
                specialtyService.updateSpecialty(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteSpecialty(
            @PathVariable("id") Long id
    ) {
        specialtyService.deleteSpecialty(id);

        return ResponseEntity.noContent().build();
    }
}