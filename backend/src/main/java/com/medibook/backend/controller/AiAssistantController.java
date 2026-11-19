package com.medibook.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.service.AiAssistantService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(
            AiAssistantService aiAssistantService
    ) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request
    ) {
        return new ChatResponse(
                aiAssistantService.answer(request.message())
        );
    }

    public record ChatRequest(
            @NotBlank(message = "Vui lòng nhập câu hỏi.")
            @Size(
                    max = 1000,
                    message = "Câu hỏi không được vượt quá 1000 ký tự."
            )
            String message
    ) {
    }

    public record ChatResponse(String answer) {
    }
}