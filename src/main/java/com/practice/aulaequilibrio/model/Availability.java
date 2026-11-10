package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;

/** Un bloque disponible de un docente; día ISO: lunes=1, domingo=7. */
@Entity
@Table(name = "availability")
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "weekday", nullable = false)
    private Integer day;

    @Column(name = "block_number", nullable = false)
    private Integer blockNumber;

    public Availability() { }

    public Integer getId() { return id; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }
    public Integer getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Integer blockNumber) { this.blockNumber = blockNumber; }
}
