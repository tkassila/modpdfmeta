module com.metait.modpdfmeta {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;
    requires org.apache.xmpbox;
    requires epublib.core;
    requires com.github.albfernandez.juniversalchardet;
    requires java.desktop;
    requires java.prefs;
    requires java.net.http;

    opens com.metait.modpdfmeta to javafx.fxml;
    opens com.metait.modpdfmeta.controller to javafx.fxml;
    opens com.metait.modpdfmeta.model to javafx.base;

    exports com.metait.modpdfmeta;
    exports com.metait.modpdfmeta.controller;
    exports com.metait.modpdfmeta.model;
    exports com.metait.modpdfmeta.service;
    exports com.metait.modpdfmeta.util;
}