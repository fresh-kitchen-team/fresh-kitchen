package com.example.freshkitchen.domain.alarm.entity;

import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Alarm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    @Column
    private LocalDateTime alarmDateTime;

    @Column
    private String title;

    @Column
    private String content;

    @Column
    private String url;

    @Column
    private String img;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "user_id")
    @ToString.Exclude
    private User user;
}
