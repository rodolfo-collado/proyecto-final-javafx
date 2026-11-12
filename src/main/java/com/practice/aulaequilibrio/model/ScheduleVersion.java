package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedule_version")
public class ScheduleVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80)
    @Setter
    private String name;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDate createdOn;

    public ScheduleVersion(String name) {
        this.name = name;
        this.createdOn = LocalDate.now();
    }
}
