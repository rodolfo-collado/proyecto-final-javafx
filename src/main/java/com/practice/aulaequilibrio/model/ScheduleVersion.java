package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_version")
public class ScheduleVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDate createdOn;

    protected ScheduleVersion() {
    }

    public ScheduleVersion(String name) {
        this.name = name;
        this.createdOn = LocalDate.now();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }
}
