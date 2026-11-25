package com.metait.modpdfmeta;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PdfEpubMetaWriterApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PdfEpubMetaWriterApplication.class.getResource("pdfepubmetawriter-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1240, 800);
        stage.setTitle("PdfEpubMetaWriter - PDF & EPUB Metadata editor");
        stage.setScene(scene);
        stage.show();
    }
}
