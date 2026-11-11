package com.practice.aulaequilibrio.repository;

import com.practice.aulaequilibrio.model.Groupe;

import java.util.List;

public class GroupeRepository extends HibernateRepository<Groupe> {

    public GroupeRepository() {
        super(Groupe.class, Groupe::getId);
    }

    public List<Groupe> findAll() {
        return findAll("nombre");
    }

    public boolean existsByNombre(String nombre, Integer excludeId) {
        return existsByFieldIgnoreCase("nombre", nombre, excludeId);
    }
}
