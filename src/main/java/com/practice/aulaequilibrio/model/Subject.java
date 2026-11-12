package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "asignatura")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    @Setter
    private String codigo;

    @Column(nullable = false, length = 100)
    @Setter
    private String nombre;

    @Column(name = "sesiones_requeridas", nullable = false)
    @Setter
    private Integer sesionesRequeridas;

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
