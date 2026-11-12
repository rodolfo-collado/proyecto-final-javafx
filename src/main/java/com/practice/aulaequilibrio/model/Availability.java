package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un bloque disponible de un docente; día ISO: lunes=1, domingo=7. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "availability")
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @Setter
    private Teacher teacher;

    @Column(name = "weekday", nullable = false)
    @Setter
    private Integer day;

    @Column(name = "block_number", nullable = false)
    @Setter
    private Integer blockNumber;
}
