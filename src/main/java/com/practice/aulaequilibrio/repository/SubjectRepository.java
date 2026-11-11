package com.practice.aulaequilibrio.repository;

import com.practice.aulaequilibrio.model.Subject;

import java.util.List;

public class SubjectRepository extends HibernateRepository<Subject> {

    public SubjectRepository() {
        super(Subject.class, Subject::getId);
    }

    public List<Subject> findAll() {
        return findAll("codigo");
    }

    public boolean existsByCodigo(String codigo, Integer excludeId) {
        return existsByFieldIgnoreCase("codigo", codigo, excludeId);
    }
}
