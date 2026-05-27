package com.example.freshkitchen.domain.tips.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "recycling_guide")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecyclingGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "waste_type", nullable = false, length = 50)
    private String wasteType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
}
