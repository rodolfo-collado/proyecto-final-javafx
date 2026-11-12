package com.practice.aulaequilibrio.dao;

import com.practice.aulaequilibrio.model.Availability;
import com.practice.aulaequilibrio.model.Teacher;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.function.Function;

/** Cada operación usa su propia sesión y revierte la transacción si falla. */
@RequiredArgsConstructor
public final class CatalogDAO {
    private final SessionFactory factory;

    private <T> T transaction(Function<Session, T> action) {
        try (Session session = factory.openSession()) {
            var tx = session.beginTransaction();
            try {
                T result = action.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    public List<Teacher> listTeachers() {
        return transaction(s -> s.createQuery("from Teacher order by name, id", Teacher.class).getResultList());
    }

    public Teacher saveTeacher(Integer id, String name, String email) {
        if (name == null || name.isBlank() || name.strip().length() > 120)
            throw new IllegalArgumentException("El nombre es obligatorio y debe tener como máximo 120 caracteres.");
        String cleanEmail = email == null || email.isBlank() ? null : email.strip();
        if (cleanEmail != null && cleanEmail.length() > 120)
            throw new IllegalArgumentException("El correo debe tener como máximo 120 caracteres.");
        return transaction(s -> {
            Teacher teacher = id == null ? new Teacher() : requireTeacher(s, id);
            teacher.setName(name.strip());
            teacher.setEmail(cleanEmail);
            if (id == null) s.persist(teacher);
            return teacher;
        });
    }

    public void deleteTeacher(Integer id) {
        try {
            transaction(s -> {
                Teacher teacher = requireTeacher(s, id);
                long blocks = s.createQuery("select count(a) from Availability a where a.teacher.id = :id", Long.class)
                        .setParameter("id", id).getSingleResult();
                if (blocks > 0) throw new IllegalArgumentException(
                        "No se puede eliminar el docente porque tiene bloques asociados. Elimina o reasigna sus bloques primero.");
                s.remove(teacher);
                return null;
            });
        } catch (ConstraintViolationException e) {
            // La clave foránea protege también frente a referencias creadas concurrentemente.
            throw new IllegalArgumentException("No se puede eliminar el docente porque tiene registros asociados.", e);
        }
    }

    public List<Availability> listAvailability(Integer teacherId) {
        return transaction(s -> {
            if (teacherId == null)
                return s.createQuery("select a from Availability a join fetch a.teacher t order by t.name, a.day, a.blockNumber, a.id",
                        Availability.class).getResultList();
            return s.createQuery("select a from Availability a join fetch a.teacher t where t.id = :teacherId "
                            + "order by a.day, a.blockNumber, a.id", Availability.class)
                    .setParameter("teacherId", teacherId).getResultList();
        });
    }

    public Availability saveAvailability(Integer id, Integer teacherId, Integer day, Integer blockNumber) {
        if (teacherId == null) throw new IllegalArgumentException("Selecciona un docente existente.");
        if (day == null || day < 1 || day > 7)
            throw new IllegalArgumentException("Selecciona un día de lunes a domingo.");
        if (blockNumber == null || blockNumber < 1)
            throw new IllegalArgumentException("El número de bloque debe ser un entero mayor que cero.");
        return transaction(s -> {
            Teacher teacher = requireTeacher(s, teacherId);
            Availability block = id == null ? new Availability() : s.find(Availability.class, id);
            if (block == null) throw new IllegalArgumentException("El bloque ya no existe. Actualiza la lista.");
            block.setTeacher(teacher);
            block.setDay(day);
            block.setBlockNumber(blockNumber);
            if (id == null) s.persist(block);
            return block;
        });
    }

    public void deleteAvailability(Integer id) {
        transaction(s -> {
            Availability block = id == null ? null : s.find(Availability.class, id);
            if (block == null) throw new IllegalArgumentException("Selecciona un bloque existente.");
            s.remove(block);
            return null;
        });
    }

    private Teacher requireTeacher(Session session, Integer id) {
        Teacher teacher = id == null ? null : session.find(Teacher.class, id);
        if (teacher == null) throw new IllegalArgumentException("El docente ya no existe. Actualiza la lista.");
        return teacher;
    }
}
