package com.example.practica.entity;

import jakarta.persistence.*; // Этот пакет содержит правильный @Id
import lombok.Data;

@Entity
@Table(name = "authors")
@Data
public class Author {

    @Id // Теперь Hibernate увидит этот идентификатор
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
}