module com.practice.aulaequilibrio {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires java.naming;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.postgresql.jdbc;
    requires io.github.cdimascio.dotenv.java;

    opens com.practice.aulaequilibrio.controller to javafx.fxml;
    exports com.practice.aulaequilibrio;
}
