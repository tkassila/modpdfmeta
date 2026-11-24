package com.metait.modpdfmeta.util;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class DialogHelper {

    public enum UnsavedPromptResult {
        SAVE,
        DISCARD,
        CANCEL
    }

    public static UnsavedPromptResult kysyTallennetaanko(String tiedostonimi) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Tallentamattomia muutoksia");
        alert.setHeaderText("Tiedostoon '" + tiedostonimi + "' on tehty muutoksia.");
        alert.setContentText("Haluatko tallentaa muutokset ennen siirtymistä?");

        asetaTeema(alert);

        ButtonType btnTallenna = new ButtonType("Tallenna");
        ButtonType btnHylkaa = new ButtonType("Hylkää muutokset");
        ButtonType btnPeruuta = new ButtonType("Peruuta");

        alert.getButtonTypes().setAll(btnTallenna, btnHylkaa, btnPeruuta);

        Optional<ButtonType> valinta = alert.showAndWait();
        if (valinta.isEmpty() || valinta.get() == btnPeruuta) {
            return UnsavedPromptResult.CANCEL;
        } else if (valinta.get() == btnTallenna) {
            return UnsavedPromptResult.SAVE;
        } else {
            return UnsavedPromptResult.DISCARD;
        }
    }

    public static Optional<String> kysyUusiTiedostonimi(String nykyinenNimi) {
        TextInputDialog dialog = new TextInputDialog(nykyinenNimi);
        dialog.setTitle("Nimeä tiedosto uudelleen");
        dialog.setHeaderText("Anna uusi tiedostonimi tiedostolle:");
        dialog.setContentText("Uusi nimi:");
        asetaTeema(dialog);
        return dialog.showAndWait();
    }

    public static Optional<Path> valitseEpubLista(List<Path> epubit) {
        ChoiceDialog<Path> dialog = new ChoiceDialog<>(epubit.get(0), epubit);
        dialog.setTitle("Valitse vastaava EPUB");
        dialog.setHeaderText("Löytyi useampi kuin yksi vastaava .epub-tiedosto.");
        dialog.setContentText("Valitse tarkasteltava tiedosto:");
        asetaTeema(dialog);
        return dialog.showAndWait();
    }

    public static void naytaInfo(String otsikko, String viesti) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(otsikko);
        alert.setHeaderText(null);
        alert.setContentText(viesti);
        asetaTeema(alert);
        alert.showAndWait();
    }

    /**
     * Näyttää virheilmoituksen, jossa on mukana 'Kopioi leikepöydälle' -painike
     * virheviestin helppoa kopiointia ja selvittämistä varten.
     */
    public static void naytaVirhe(String otsikko, String viesti) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(otsikko);
        alert.setHeaderText(null);
        asetaTeema(alert);

        String naytettavaViesti = viesti != null ? viesti : "";

        if (naytettavaViesti.length() > 180 || naytettavaViesti.contains("\n")) {
            TextArea textArea = new TextArea(naytettavaViesti);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefWidth(540);
            textArea.setPrefHeight(200);
            alert.getDialogPane().setContent(textArea);
        } else {
            alert.setContentText(naytettavaViesti);
        }

        ButtonType btnKopioi = new ButtonType("Kopioi leikepöydälle");
        ButtonType btnSulje = new ButtonType("Sulje", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnKopioi, btnSulje);

        Button kopioiNappi = (Button) alert.getDialogPane().lookupButton(btnKopioi);
        if (kopioiNappi != null) {
            kopioiNappi.addEventFilter(ActionEvent.ACTION, event -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(naytettavaViesti);
                clipboard.setContent(content);
                kopioiNappi.setText("✓ Kopioitu leikepöydälle!");
                event.consume(); // Ei sulje dialogia, joten käyttäjä voi rauhassa lukea ja sulkea erikseen
            });
        }

        alert.showAndWait();
    }

    private static void asetaTeema(javafx.scene.control.Dialog<?> dialog) {
        try {
            var css = DialogHelper.class.getResource("/com/metait/modpdfmeta/dark-theme.css");
            if (css != null) {
                dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}
    }
}
