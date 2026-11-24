package com.metait.modpdfmeta.util;

import com.metait.modpdfmeta.service.TextMetadataExtractorService.ExtractedField;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MetadataExtractionDialog {

    public static Optional<List<ExtractedField>> nayta(Window owner, List<ExtractedField> loydetytKentat, String tiedostotyyppi) {
        Dialog<List<ExtractedField>> dialog = new Dialog<>();
        dialog.setTitle("Tekstistä löydetyt metatiedot");
        dialog.initOwner(owner);

        // Tyylitellään ikkuna tummalla teemalla
        var cssResource = MetadataExtractionDialog.class.getResource("/com/metait/modpdfmeta/dark-theme.css");
        if (cssResource != null) {
            dialog.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        }

        dialog.setHeaderText("Tekstisisällöstä louhitut metatiedot (" + tiedostotyyppi + ").\n" +
                "Valitse valintaruuduilla (Kyllä / Ei) mitkä arvot viedään " +
                (tiedostotyyppi.equalsIgnoreCase("PDF") ? "raaka data (matalan tason) kenttiin." : "EPUB-kenttiin."));

        GridPane grid = new GridPane();
        grid.setHgap(12.0);
        grid.setVgap(10.0);
        grid.setPadding(new Insets(15, 15, 15, 15));

        // Sarakeotsikot
        Label hOta = new Label("Valitse");
        hOta.setStyle("-fx-font-weight: bold;");
        Label hKentta = new Label("Kenttä");
        hKentta.setStyle("-fx-font-weight: bold;");
        Label hNykyinen = new Label("Nykyinen arvo");
        hNykyinen.setStyle("-fx-font-weight: bold;");
        Label hUusi = new Label("Löydetty arvo (muokattavissa)");
        hUusi.setStyle("-fx-font-weight: bold;");

        grid.add(hOta, 0, 0);
        grid.add(hKentta, 1, 0);
        grid.add(hNykyinen, 2, 0);
        grid.add(hUusi, 3, 0);

        List<RowControls> rows = new ArrayList<>();

        int rowIndex = 1;
        for (ExtractedField f : loydetytKentat) {
            CheckBox cb = new CheckBox();
            cb.setSelected(f.isSelected());

            Label lblKentta = new Label(f.getLabel() + ":");
            lblKentta.setMinWidth(130);

            String curr = f.getCurrentValue().isEmpty() ? "(tyhjä)" : f.getCurrentValue();
            Label lblCurr = new Label(curr);
            lblCurr.setMaxWidth(180);
            lblCurr.setStyle("-fx-text-fill: #98c1d9; -fx-font-style: italic;");

            TextField tfUusi = new TextField(f.getExtractedValue());
            tfUusi.setMinWidth(280);
            GridPane.setHgrow(tfUusi, Priority.ALWAYS);

            // Jos kenttään kirjoitetaan arvo, valitaan ruksi automaattisesti
            tfUusi.textProperty().addListener((obs, o, n) -> {
                if (!n.trim().isEmpty() && !cb.isSelected()) {
                    cb.setSelected(true);
                }
            });

            grid.add(cb, 0, rowIndex);
            grid.add(lblKentta, 1, rowIndex);
            grid.add(lblCurr, 2, rowIndex);
            grid.add(tfUusi, 3, rowIndex);

            rows.add(new RowControls(f, cb, tfUusi));
            rowIndex++;
        }

        VBox content = new VBox(10.0, grid);
        dialog.getDialogPane().setContent(content);

        ButtonType btnHyvaksy = new ButtonType("Käytä valitut arvot", ButtonType.OK.getButtonData());
        ButtonType btnPeruuta = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(btnHyvaksy, btnPeruuta);

        dialog.setResultConverter(btn -> {
            if (btn == btnHyvaksy) {
                List<ExtractedField> tulos = new ArrayList<>();
                for (RowControls r : rows) {
                    if (r.cb.isSelected()) {
                        r.field.setSelected(true);
                        r.field.setExtractedValue(r.tf.getText().trim());
                        tulos.add(r.field);
                    }
                }
                return tulos;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static class RowControls {
        final ExtractedField field;
        final CheckBox cb;
        final TextField tf;

        RowControls(ExtractedField field, CheckBox cb, TextField tf) {
            this.field = field;
            this.cb = cb;
            this.tf = tf;
        }
    }
}
