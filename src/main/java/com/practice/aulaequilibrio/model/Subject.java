package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;

@Entity
@Table(name = "asignatura")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "sesiones_requeridas", nullable = false)
    private Integer sesionesRequeridas;

    public Subject() {
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getSesionesRequeridas() {
        return sesionesRequeridas;
    }

    public void setSesionesRequeridas(Integer sesionesRequeridas) {
        this.sesionesRequeridas = sesionesRequeridas;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
