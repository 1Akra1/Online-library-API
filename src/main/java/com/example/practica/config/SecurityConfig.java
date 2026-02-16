package com.example.practica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Отключаем CSRF для работы POST/DELETE извне
                .authorizeHttpRequests(auth -> auth
                        // 1. САМЫЙ ВЫСОКИЙ ПРИОРИТЕТ: Защита админских путей
                        // Используем HttpMethod и ** для максимального охвата
                        .requestMatchers("/api/books/export").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/books/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/books/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/books/**").hasAuthority("ADMIN")

                        // 2. ПУБЛИЧНЫЕ ПУТИ: Доступны всем без логина
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/books/**").permitAll()

                        // 3. ВСЁ ОСТАЛЬНОЕ: Только для авторизованных пользователей (например, избранное)
                        .anyRequest().authenticated()
                )
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                // ГАРАНТИЯ ЧИСТОГО ТЕСТА: Отключаем сохранение сессии
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        return http.build();
    }
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        // Этот костыль нужен только для тестов, чтобы принимать пароли в чистом виде
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }
}