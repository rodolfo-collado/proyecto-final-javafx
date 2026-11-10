package com.practice.aulaequilibrio.dao;

import com.practice.aulaequilibrio.model.Availability;
import com.practice.aulaequilibrio.model.Teacher;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogDAOTest {
    private SessionFactory factory;
    private CatalogDAO dao;

    @BeforeEach
    void setup() {
        factory = createFactory("catalog_" + System.nanoTime());
        dao = new CatalogDAO(factory);
    }

    public static SessionFactory createFactory(String database) {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("jakarta.persistence.jdbc.driver", "org.h2.Driver")
                .applySetting("jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + database + ";MODE=PostgreSQL")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .build();
        try {
            return new MetadataSources(registry).addAnnotatedClass(Teacher.class)
                    .addAnnotatedClass(Availability.class).buildMetadata().buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }

    @AfterEach void close() { factory.close(); }

    @Test void twoTeachersFilterEditAndDeleteBlock() {
        var ana = dao.saveTeacher(null, "Ana Pérez", "ana@example.test");
        var luis = dao.saveTeacher(null, "Luis López", "luis@example.test");
        var first = dao.saveAvailability(null, ana.getId(), 1, 2);
        dao.saveAvailability(null, ana.getId(), 3, 4);
        dao.saveAvailability(null, luis.getId(), 2, 6);
        var filtered = dao.listAvailability(ana.getId());
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(a -> a.getTeacher().getId().equals(ana.getId())));
        assertEquals(3, dao.listAvailability(null).size());
        dao.saveAvailability(first.getId(), ana.getId(), 5, 8);
        var edited = dao.listAvailability(ana.getId()).stream().filter(a -> a.getId().equals(first.getId())).findFirst().orElseThrow();
        assertEquals(5, edited.getDay());
        assertEquals(8, edited.getBlockNumber());
        dao.deleteAvailability(first.getId());
        assertEquals(1, dao.listAvailability(ana.getId()).size());
        assertEquals(1, dao.listAvailability(luis.getId()).size());
    }

    @Test void teacherCrudAndReferencedTeacherProtection() {
        var teacher = dao.saveTeacher(null, "  María  ", " ");
        assertEquals("María", teacher.getName());
        assertNull(teacher.getEmail());
        dao.saveTeacher(teacher.getId(), "María Gómez", "maria@example.test");
        assertEquals("María Gómez", dao.listTeachers().getFirst().getName());
        var block = dao.saveAvailability(null, teacher.getId(), 7, 1);
        var error = assertThrows(IllegalArgumentException.class, () -> dao.deleteTeacher(teacher.getId()));
        assertTrue(error.getMessage().contains("bloques asociados"));
        assertEquals(1, dao.listTeachers().size());
        dao.deleteAvailability(block.getId());
        dao.deleteTeacher(teacher.getId());
        assertTrue(dao.listTeachers().isEmpty());
    }

    @Test void validatesBeforeSaving() {
        var teacher = dao.saveTeacher(null, "Docente válido", null);
        for (String name : new String[]{null, "", "   ", "a".repeat(121)})
            assertThrows(IllegalArgumentException.class, () -> dao.saveTeacher(null, name, null));
        for (Integer day : new Integer[]{null, 0, 8})
            assertThrows(IllegalArgumentException.class, () -> dao.saveAvailability(null, teacher.getId(), day, 1));
        for (Integer number : new Integer[]{null, 0, -1})
            assertThrows(IllegalArgumentException.class, () -> dao.saveAvailability(null, teacher.getId(), 1, number));
        assertThrows(IllegalArgumentException.class, () -> dao.saveAvailability(null, null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> dao.saveAvailability(null, -1, 1, 1));
        assertTrue(dao.listAvailability(null).isEmpty());
    }

    @Test void databaseRequiresTeacherAndProtectsForeignKey() {
        var teacher = dao.saveTeacher(null, "Docente", null);
        dao.saveAvailability(null, teacher.getId(), 1, 1);
        assertThrows(RuntimeException.class, () -> factory.inTransaction(s -> s.remove(s.find(Teacher.class, teacher.getId()))));
        assertThrows(RuntimeException.class, () -> factory.inTransaction(s -> {
            var missingTeacher = new Availability();
            missingTeacher.setDay(1); missingTeacher.setBlockNumber(1);
            s.persist(missingTeacher);
        }));
        assertEquals(1, dao.listTeachers().size());
    }

    @Test void blockCanBeReassignedToAnotherExistingTeacher() {
        var first = dao.saveTeacher(null, "Primero", null);
        var second = dao.saveTeacher(null, "Segundo", null);
        var block = dao.saveAvailability(null, first.getId(), 1, 1);
        dao.saveAvailability(block.getId(), second.getId(), 2, 2);
        assertTrue(dao.listAvailability(first.getId()).isEmpty());
        assertEquals(1, dao.listAvailability(second.getId()).size());
        dao.deleteTeacher(first.getId());
    }
}
