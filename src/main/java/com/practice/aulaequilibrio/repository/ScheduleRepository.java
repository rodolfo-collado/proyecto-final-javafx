package com.practice.aulaequilibrio.repository;

import com.practice.aulaequilibrio.config.HibernateUtil;
import com.practice.aulaequilibrio.model.Classroom;
import com.practice.aulaequilibrio.model.Groupe;
import com.practice.aulaequilibrio.model.ScheduleAssignment;
import com.practice.aulaequilibrio.model.ScheduleVersion;
import com.practice.aulaequilibrio.model.Subject;
import com.practice.aulaequilibrio.model.Teacher;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScheduleRepository {
    public record Option(Integer id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public record Catalogs(List<Option> groups, List<Option> subjects, List<Option> teachers, List<Option> classrooms) {
    }

    public record Placement(int day, int block, Integer groupId, Integer subjectId, Integer teacherId,
                            Integer classroomId) {
    }

    public List<ScheduleVersion> listVersions() {
        return HibernateUtil.getSessionFactory().fromTransaction(session ->
                session.createSelectionQuery("from ScheduleVersion order by createdOn desc, id desc",
                        ScheduleVersion.class).getResultList());
    }

    public int saveVersion(Integer id, String name) {
        String trimmedName = name.strip();
        if (trimmedName.isEmpty() || trimmedName.length() > 80) {
            throw new IllegalArgumentException("El nombre de la versión debe tener entre 1 y 80 caracteres.");
        }
        return HibernateUtil.getSessionFactory().fromTransaction(session -> {
            if (id == null) {
                ScheduleVersion version = new ScheduleVersion(trimmedName);
                session.persist(version);
                return version.getId();
            }
            lockVersion(session, id).setName(trimmedName);
            return id;
        });
    }

    public void deleteVersion(int id) {
        HibernateUtil.getSessionFactory().inTransaction(session -> {
            ScheduleVersion version = lockVersion(session, id);
            boolean hasAssignments = !session.createSelectionQuery(
                            "select id from ScheduleAssignment where version.id = :version", Integer.class)
                    .setParameter("version", id)
                    .setMaxResults(1)
                    .getResultList().isEmpty();
            if (hasAssignments) {
                throw new IllegalArgumentException("Elimina las asignaciones antes de borrar esta versión.");
            }
            session.remove(version);
        });
    }

    public List<ScheduleAssignment> listAssignments(int versionId, Integer groupId) {
        return HibernateUtil.getSessionFactory().fromTransaction(session -> {
            String hql = """
                    select a from ScheduleAssignment a
                    join fetch a.group g
                    join fetch a.subject
                    join fetch a.teacher
                    join fetch a.classroom
                    where a.version.id = :version
                    """;
            if (groupId != null) {
                hql += "and g.id = :groupId\n";
            }
            var query = session.createSelectionQuery(hql + "order by a.day, a.block, a.id", ScheduleAssignment.class)
                    .setParameter("version", versionId);
            if (groupId != null) {
                query.setParameter("groupId", groupId);
            }
            return query.getResultList();
        });
    }

    public Catalogs listCatalogs() {
        return HibernateUtil.getSessionFactory().fromTransaction(session -> new Catalogs(
                options(session, "select id, nombre from Groupe order by nombre"),
                options(session, "select id, name from Subject order by name, code"),
                options(session, "select id, name from Teacher order by name, id"),
                options(session, "select id, codigo from Classroom order by codigo")));
    }

    private static List<Option> options(Session session, String hql) {
        return session.createSelectionQuery(hql, Object[].class).getResultList().stream()
                .map(row -> new Option((Integer) row[0], (String) row[1]))
                .toList();
    }

    public void saveAssignment(int versionId, Integer assignmentId, Placement placement) {
        if (placement.day() < 1 || placement.day() > 7 || placement.block() < 1) {
            throw new IllegalArgumentException("Elige un día válido y un número de bloque mayor que cero.");
        }
        HibernateUtil.getSessionFactory().inTransaction(session -> {
            ScheduleVersion version = lockVersion(session, versionId);
            ScheduleAssignment assignment = assignmentId == null
                    ? new ScheduleAssignment(version)
                    : findAssignment(session, versionId, assignmentId);
            Groupe group = required(session, Groupe.class, placement.groupId(), "un grupo");
            Subject subject = required(session, Subject.class, placement.subjectId(), "una asignatura");
            Teacher teacher = required(session, Teacher.class, placement.teacherId(), "un docente");
            Classroom classroom = required(session, Classroom.class, placement.classroomId(), "un aula");

            checkConflicts(session, versionId, assignmentId, placement);
            assignment.assign(group, subject, teacher, classroom, placement.day(), placement.block());
            if (assignmentId == null) {
                session.persist(assignment);
            }
        });
    }

    public void deleteAssignment(int versionId, int assignmentId) {
        HibernateUtil.getSessionFactory().inTransaction(session -> {
            lockVersion(session, versionId);
            session.remove(findAssignment(session, versionId, assignmentId));
        });
    }

    private static ScheduleVersion lockVersion(Session session, int versionId) {
        // Serializa las modificaciones de una versión mientras se comprueban sus bloques.
        ScheduleVersion version = session.find(ScheduleVersion.class, versionId, LockModeType.PESSIMISTIC_WRITE);
        if (version == null) {
            throw new IllegalArgumentException("La versión ya no existe. Actualiza la vista.");
        }
        return version;
    }

    private static ScheduleAssignment findAssignment(Session session, int versionId, int assignmentId) {
        ScheduleAssignment assignment = session.find(ScheduleAssignment.class, assignmentId);
        if (assignment == null || !Objects.equals(assignment.getVersion().getId(), versionId)) {
            throw new IllegalArgumentException("La asignación ya no está disponible en esta versión.");
        }
        return assignment;
    }

    private static <T> T required(Session session, Class<T> type, Integer id, String label) {
        T entity = id == null ? null : session.find(type, id);
        if (entity == null) {
            throw new IllegalArgumentException("Selecciona " + label + " que exista en el catálogo.");
        }
        return entity;
    }

    private static void checkConflicts(Session session, int versionId, Integer assignmentId, Placement placement) {
        String hql = """
                select a from ScheduleAssignment a
                where a.version.id = :version and a.day = :day and a.block = :block
                and (a.group.id = :groupId or a.teacher.id = :teacherId or a.classroom.id = :classroomId)
                """;
        if (assignmentId != null) {
            hql += "and a.id <> :assignmentId";
        }
        var query = session.createSelectionQuery(hql, ScheduleAssignment.class)
                .setParameter("version", versionId)
                .setParameter("day", placement.day())
                .setParameter("block", placement.block())
                .setParameter("groupId", placement.groupId())
                .setParameter("teacherId", placement.teacherId())
                .setParameter("classroomId", placement.classroomId());
        if (assignmentId != null) {
            query.setParameter("assignmentId", assignmentId);
        }

        List<ScheduleAssignment> conflicts = query.getResultList();
        List<String> occupied = new ArrayList<>();
        if (conflicts.stream().anyMatch(a -> a.getGroup().getId().equals(placement.groupId()))) {
            occupied.add("grupo");
        }
        if (conflicts.stream().anyMatch(a -> a.getTeacher().getId().equals(placement.teacherId()))) {
            occupied.add("docente");
        }
        if (conflicts.stream().anyMatch(a -> a.getClassroom().getId().equals(placement.classroomId()))) {
            occupied.add("aula");
        }
        if (!occupied.isEmpty()) {
            throw new IllegalArgumentException("Cruce de horario (" + String.join(", ", occupied)
                    + "). Cambia el bloque o el recurso ocupado.");
        }
    }
}
