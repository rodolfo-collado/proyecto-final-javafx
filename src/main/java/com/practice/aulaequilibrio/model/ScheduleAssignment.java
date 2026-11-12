package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedule_assignment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_schedule_classroom_slot",
                columnNames = {"version_id", "day_of_week", "block_number", "classroom_id"}),
        @UniqueConstraint(name = "uk_schedule_teacher_slot",
                columnNames = {"version_id", "day_of_week", "block_number", "teacher_id"}),
        @UniqueConstraint(name = "uk_schedule_group_slot",
                columnNames = {"version_id", "day_of_week", "block_number", "group_id"})
})
public class ScheduleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private ScheduleVersion version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Groupe group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "day_of_week", nullable = false)
    private int day;

    @Column(name = "block_number", nullable = false)
    private int block;

    public ScheduleAssignment(ScheduleVersion version) {
        this.version = version;
    }

    public void assign(Groupe group, Subject subject, Teacher teacher, Classroom classroom, int day, int block) {
        this.group = group;
        this.subject = subject;
        this.teacher = teacher;
        this.classroom = classroom;
        this.day = day;
        this.block = block;
    }
}
