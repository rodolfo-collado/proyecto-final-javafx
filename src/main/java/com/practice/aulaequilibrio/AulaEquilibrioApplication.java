package com.practice.aulaequilibrio;

import com.practice.aulaequilibrio.config.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AulaEquilibrioApplication extends Application {
    @Override
    public void init() {
        try {
            HibernateUtil.verifyConnection();
            HibernateUtil.loadDemoData();
        } catch (RuntimeException exception) {
            HibernateUtil.shutdown();
            throw exception;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AulaEquilibrioApplication.class.getResource("views/home-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1040, 740);
        stage.setTitle("Aula Equilibrio");
        stage.setMinWidth(1000);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }
}
