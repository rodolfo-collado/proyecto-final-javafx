package com.practice.aulaequilibrio.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subject")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "required_sessions", nullable = false)
    private int requiredSessions;

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRequiredSessions() {
        return requiredSessions;
    }

    public void setRequiredSessions(int requiredSessions) {
        this.requiredSessions = requiredSessions;
    }
}
