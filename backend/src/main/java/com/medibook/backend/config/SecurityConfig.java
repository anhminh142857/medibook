package com.medibook.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.medibook.backend.repository.UserRepository;
import com.medibook.backend.service.JwtService;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userRepository
                );

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );
                                    response.setContentType("application/json");
                                    response.setCharacterEncoding("UTF-8");
                                    response.getWriter().write(
                                            "{\"message\":\"Vui long dang nhap.\"}"
                                    );
                                }
                        )
                        .accessDeniedHandler(
                                (request, response, exception) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_FORBIDDEN
                                    );
                                    response.setContentType("application/json");
                                    response.setCharacterEncoding("UTF-8");
                                    response.getWriter().write(
                                            "{\"message\":\"Ban khong co quyen thuc hien thao tac nay.\"}"
                                    );
                                }
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Dang ky va dang nhap.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/google"
                        ).permitAll()

                        // Swagger / OpenAPI.
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Danh muc cong khai.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/specialties",
                                "/api/doctors",
                                "/api/appointment-slots"
                        ).permitAll()

                        // Quan ly danh muc.
                        .requestMatchers(
                                "/api/specialties",
                                "/api/specialties/**",
                                "/api/doctors",
                                "/api/doctors/**",
                                "/api/appointment-slots",
                                "/api/appointment-slots/**"
                        ).hasRole("ADMIN")

                        // API quan tri.
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // API bac si.
                        .requestMatchers(
                                "/api/doctor/**"
                        ).hasRole("DOCTOR")

                        // Benh nhan xem lich hen cua minh.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/appointments/me"
                        ).hasRole("PATIENT")

                        // Benh nhan tao va huy lich hen.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/appointments",
                                "/api/appointments/*/cancel"
                        ).hasRole("PATIENT")

                        // Chan cac duong dan lich hen chua khai bao.
                        .requestMatchers(
                                "/api/appointments/**"
                        ).denyAll()

                        // Webhook tu kiem tra API Key trong controller.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payments/sepay/webhook"
                        ).permitAll()

                        // Benh nhan tao thanh toan SePay.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payments/sepay/appointments/*"
                        ).hasRole("PATIENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments/sepay/appointments/*"
                        ).hasRole("PATIENT")

                        // Benh nhan xem giao dich SePay cua minh.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments/sepay/transactions/*"
                        ).hasRole("PATIENT")

                        // Chan cac duong dan SePay chua khai bao.
                        .requestMatchers(
                                "/api/payments/sepay/**"
                        ).denyAll()

                        // Cac API con lai yeu cau dang nhap.
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}