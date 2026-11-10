package com.practice.aulaequilibrio.config;

import com.practice.aulaequilibrio.model.Groupe;
import com.practice.aulaequilibrio.model.Classroom;
import com.practice.aulaequilibrio.model.ClassroomType;
import com.practice.aulaequilibrio.model.Teacher;
import io.github.cdimascio.dotenv.Dotenv;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder()
                        .applySetting(
                                "jakarta.persistence.jdbc.driver",
                                "org.postgresql.Driver"
                        )
                        .applySetting(
                                "jakarta.persistence.jdbc.url",
                                required(dotenv, "DB_URL")
                        )
                        .applySetting(
                                "jakarta.persistence.jdbc.user",
                                required(dotenv, "DB_USER")
                        )
                        .applySetting(
                                "jakarta.persistence.jdbc.password",
                                required(dotenv, "DB_PASSWORD")
                        )
                        .applySetting("hibernate.hbm2ddl.auto", "update")
                        .build();

        try {
            return new MetadataSources(registry)
                    .addAnnotatedClass(Teacher.class)
                    .addAnnotatedClass(Groupe.class)
                    .addAnnotatedClass(Classroom.class)
                    .addAnnotatedClass(ClassroomType.class)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (RuntimeException exception) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw exception;
        }
    }

    private static String required(Dotenv dotenv, String name) {
        String value = dotenv.get(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration for " + name + " in .env or environment variables"
            );
        }

        return value;
    }

    public static void verifyConnection() {
        getSessionFactory().inTransaction(session ->
                session.doWork(connection -> {
                    try (var statement = connection.prepareStatement("SELECT 1");
                         var result = statement.executeQuery()) {
                        if (!result.next() || result.getInt(1) != 1) {
                            throw new java.sql.SQLException(
                                    "PostgreSQL did not respond correctly"
                            );
                        }
                    }
                })
        );
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            try {
                sessionFactory.close();
            } finally {
                sessionFactory = null;
            }
        }
    }
}