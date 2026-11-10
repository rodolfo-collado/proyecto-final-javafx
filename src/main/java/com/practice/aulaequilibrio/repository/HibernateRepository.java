package com.practice.aulaequilibrio.repository;

import com.practice.aulaequilibrio.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Operaciones CRUD básicas con Hibernate. Cada método abre y cierra su propia sesión.
 */
public abstract class HibernateRepository<T> {

    private final Class<T> type;
    private final Function<T, Integer> idGetter;

    protected HibernateRepository(Class<T> type, Function<T, Integer> idGetter) {
        this.type = type;
        this.idGetter = idGetter;
    }

    public List<T> findAll(String orderBy) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createSelectionQuery(
                    "from " + type.getSimpleName() + " e order by e." + orderBy, type
            ).getResultList();
        }
    }

    public Optional<T> findById(Integer id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.find(type, id));
        }
    }

    /**
     * Indica si existe otro registro con el mismo valor (sin distinguir mayúsculas)
     * en el campo indicado, ignorando el registro con id {@code excludeId}.
     */
    public boolean existsByFieldIgnoreCase(String field, String value, Integer excludeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createSelectionQuery(
                            "select count(e) from " + type.getSimpleName() + " e "
                                    + "where lower(e." + field + ") = lower(:value) "
                                    + "and (:excludeId is null or e.id <> :excludeId)",
                            Long.class)
                    .setParameter("value", value)
                    .setParameter("excludeId", excludeId)
                    .getSingleResult();
            return count > 0;
        }
    }

    public T save(T entity) {
        return inTransaction(session -> {
            if (idGetter.apply(entity) == null) {
                session.persist(entity);
                return entity;
            }
            return session.merge(entity);
        });
    }

    public boolean deleteById(Integer id) {
        return inTransaction(session -> {
            T current = session.find(type, id);
            if (current == null) {
                return false;
            }
            session.remove(current);
            return true;
        });
    }

    private <R> R inTransaction(Function<Session, R> work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                R result = work.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }
}
