module com.smartinterview {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.materialdesign2;
    requires transitive java.sql;
    requires java.net.http;
    requires org.apache.pdfbox;
    requires org.xerial.sqlitejdbc;
    requires transitive com.fasterxml.jackson.databind;

    opens com.smartinterview to javafx.fxml, javafx.graphics;
    opens com.smartinterview.controller to javafx.fxml;
    opens com.smartinterview.model to javafx.base, com.fasterxml.jackson.databind;
    opens com.smartinterview.ui to javafx.fxml;

    exports com.smartinterview;
    exports com.smartinterview.controller;
    exports com.smartinterview.model;
    exports com.smartinterview.service;
    exports com.smartinterview.ai;
    exports com.smartinterview.db;
    exports com.smartinterview.navigation;
    exports com.smartinterview.ui;
}
