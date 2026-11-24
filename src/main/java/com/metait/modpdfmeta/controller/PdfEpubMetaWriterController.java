package com.metait.modpdfmeta.controller;

import com.metait.modpdfmeta.model.DocumentMetadata;
import com.metait.modpdfmeta.model.FileItem;
import com.metait.modpdfmeta.service.AiMetadataExtractorService;
import com.metait.modpdfmeta.service.CharsetFixerService;
import com.metait.modpdfmeta.service.EpubMatcherService;
import com.metait.modpdfmeta.service.EpubMetadataService;
import com.metait.modpdfmeta.service.PdfMetadataService;
import com.metait.modpdfmeta.service.TextMetadataExtractorService;
import com.metait.modpdfmeta.service.TextMetadataExtractorService.ExtractedField;
import com.metait.modpdfmeta.util.DialogHelper;
import com.metait.modpdfmeta.util.FlagIconHelper;
import com.metait.modpdfmeta.util.HelpDialog;
import com.metait.modpdfmeta.util.I18n;
import com.metait.modpdfmeta.util.MetadataExtractionDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class PdfEpubMetaWriterController {

    @FXML private Button btnAvaaTiedosto;
    @FXML private Button btnValitseHakemisto;
    @FXML private CheckBox cbAlihakemistot;
    @FXML private Button btnEdellinen;
    @FXML private Button btnSeuraava;
    @FXML private Button btnNimeaTiedosto;
    @FXML private Button btnEtsiEpub;
    @FXML private Button btnEtsiMetadataTekstista;
    @FXML private Button btnHaeAiMetadataa;
    @FXML private ComboBox<I18n.Language> cbKieli;
    @FXML private Button btnOhje;

    // Lokalisointi-kentät (Paneelien ja kenttien otsikot)
    @FXML private Label lblLowPanelTitle;
    @FXML private Label lblLowCharsetText;
    @FXML private Label lblLowTitleLabel;
    @FXML private Label lblLowAuthorLabel;
    @FXML private Label lblLowPublisherLabel;
    @FXML private Label lblLowSubjectLabel;
    @FXML private Label lblLowKeywordsLabel;
    @FXML private Label lblLowDateLabel;
    @FXML private Label lblLowLanguageLabel;

    @FXML private Label lblXmpPanelTitle;
    @FXML private Label lblXmpStandard;
    @FXML private Label lblXmpTitleLabel;
    @FXML private Label lblXmpAuthorLabel;
    @FXML private Label lblXmpPublisherLabel;
    @FXML private Label lblXmpSubjectLabel;
    @FXML private Label lblXmpKeywordsLabel;
    @FXML private Label lblXmpDateLabel;
    @FXML private Label lblXmpLanguageLabel;

    @FXML private Label lblEpubPanelTitle;
    @FXML private Button btnSuljeEpub;
    @FXML private Label lblEpubTitleLabel;
    @FXML private Label lblEpubAuthorLabel;
    @FXML private Label lblEpubPublisherLabel;
    @FXML private Label lblEpubSubjectLabel;
    @FXML private Label lblEpubKeywordsLabel;
    @FXML private Label lblEpubDateLabel;
    @FXML private Label lblEpubLanguageLabel;

    @FXML private Button btnKopioiEpubToLow;
    @FXML private Button btnKopioiEpubToXmp;
    @FXML private Button btnKopioiLowToEpub;
    @FXML private Button btnKopioiXmpToEpub;

    @FXML private Label lblHakemisto;
    @FXML private Label lblTiedosto;
    @FXML private Label lblTiedostojaLkm;
    @FXML private ListView<FileItem> fileListView;

    @FXML private Button btnKopioiXmlToLow;
    @FXML private Button btnKopioiLowToXml;

    // Matalan tason kentät
    @FXML private ComboBox<String> cbLowCharset;
    @FXML private Label lblLowCharset;
    @FXML private HBox boxCharsetWarning;
    @FXML private Label lblCharsetWarning;
    @FXML private Button btnKorjaaMerkisto;
    @FXML private TextField tfLowTitle;
    @FXML private TextField tfLowAuthor;
    @FXML private TextField tfLowPublisher;
    @FXML private TextField tfLowSubject;
    @FXML private TextField tfLowKeywords;
    @FXML private TextField tfLowDate;
    @FXML private TextField tfLowLanguage;
    @FXML private Button btnTallennaLow;

    // Matalan tason kenttäkohtaiset siirtonuolet (Low -> XML)
    @FXML private Button btnLowTitleToXmp;
    @FXML private Button btnLowAuthorToXmp;
    @FXML private Button btnLowPublisherToXmp;
    @FXML private Button btnLowSubjectToXmp;
    @FXML private Button btnLowKeywordsToXmp;
    @FXML private Button btnLowDateToXmp;
    @FXML private Button btnLowLanguageToXmp;

    // XML / XMP -kentät
    @FXML private Label lblXmpCharset;
    @FXML private TextField tfXmpTitle;
    @FXML private TextField tfXmpAuthor;
    @FXML private TextField tfXmpPublisher;
    @FXML private TextField tfXmpSubject;
    @FXML private TextField tfXmpKeywords;
    @FXML private TextField tfXmpDate;
    @FXML private TextField tfXmpLanguage;
    @FXML private Button btnTallennaXmp;

    // XML-tason kenttäkohtaiset siirtonuolet (XML -> Low)
    @FXML private Button btnXmpTitleToLow;
    @FXML private Button btnXmpAuthorToLow;
    @FXML private Button btnXmpPublisherToLow;
    @FXML private Button btnXmpSubjectToLow;
    @FXML private Button btnXmpKeywordsToLow;
    @FXML private Button btnXmpDateToLow;
    @FXML private Button btnXmpLanguageToLow;

    // EPUB-paneeli
    @FXML private VBox epubPanel;
    @FXML private Label lblEpubTiedosto;
    @FXML private TextField tfEpubTitle;
    @FXML private TextField tfEpubAuthor;
    @FXML private TextField tfEpubPublisher;
    @FXML private TextField tfEpubSubject;
    @FXML private TextField tfEpubKeywords;
    @FXML private TextField tfEpubDate;
    @FXML private TextField tfEpubLanguage;
    @FXML private Button btnTallennaEpub;

    @FXML private Label lblStatus;

    // Palvelut
    private final PdfMetadataService pdfService = new PdfMetadataService();
    private final EpubMetadataService epubService = new EpubMetadataService();
    private final EpubMatcherService epubMatcher = new EpubMatcherService();
    private final TextMetadataExtractorService textExtractor = new TextMetadataExtractorService();
    private final AiMetadataExtractorService aiExtractor = new AiMetadataExtractorService();

    // Tila
    private final ObservableList<FileItem> tiedostot = FXCollections.observableArrayList();
    private FileItem nykyinenTiedosto = null;
    private Path avattuEpubVertailuPolku = null;

    private DocumentMetadata alkuperainenLow = new DocumentMetadata();
    private DocumentMetadata alkuperainenXmp = new DocumentMetadata();
    private DocumentMetadata alkuperainenEpub = new DocumentMetadata();

    private static final String PREF_LAST_DIR = "last_selected_directory";
    public static final String PREF_GUI_LANG = "app_gui_language";
    private final Preferences prefs = Preferences.userNodeForPackage(PdfEpubMetaWriterController.class);
    private File viimeisinHakemisto = null;

    private boolean latausKaynnissa = false;

    @FXML
    public void initialize() {
        try {
            String tallennettuHakemisto = prefs.get(PREF_LAST_DIR, null);
            if (tallennettuHakemisto != null) {
                File f = new File(tallennettuHakemisto);
                if (f.exists() && f.isDirectory()) {
                    viimeisinHakemisto = f;
                }
            }
        } catch (Exception ignored) {}

        fileListView.setItems(tiedostot);

        // Tiedostolistan renderöinti: [PDF] punainen tausta, [EPUB] vihreä tausta, muutosindikaattori ja tiedostonimi
        fileListView.setCellFactory(lv -> {
            ListCell<FileItem> cell = new ListCell<>() {
                private final Label lblDirty = new Label("*");
                private final Label lblType = new Label();
                private final Label lblName = new Label();
                private final HBox hbox = new HBox(6);

                {
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    hbox.setMouseTransparent(true);
                    lblDirty.getStyleClass().add("file-list-dirty");
                    lblName.getStyleClass().add("file-list-name");
                }

                @Override
                protected void updateItem(FileItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(null);
                        hbox.getChildren().clear();

                        if (item.isDirty()) {
                            hbox.getChildren().add(lblDirty);
                        }

                        lblType.getStyleClass().removeAll("badge-pdf", "badge-epub", "badge-muu");
                        if (item.isPdf()) {
                            lblType.setText("[PDF]");
                            lblType.getStyleClass().add("badge-pdf");
                        } else if (item.isEpub()) {
                            lblType.setText("[EPUB]");
                            lblType.getStyleClass().add("badge-epub");
                        } else {
                            lblType.setText("[MUU]");
                            lblType.getStyleClass().add("badge-muu");
                        }
                        hbox.getChildren().add(lblType);

                        lblName.setText(item.getFileName());
                        hbox.getChildren().add(lblName);

                        setGraphic(hbox);
                    }
                }
            };
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    FileItem item = cell.getItem();
                    if (item != null) {
                        avaaTiedostoJarjestelmassa(item.getPath());
                    }
                }
            });
            return cell;
        });

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, vanha, uusi) -> {
            if (latausKaynnissa) return;
            if (uusi != null && uusi != nykyinenTiedosto) {
                if (!tarkistaTallentamattomatEnnenSiirtymista()) {
                    // Palautetaan vanha valinta
                    Platform.runLater(() -> fileListView.getSelectionModel().select(vanha));
                    return;
                }
                avaaTiedostoKasittelyyn(uusi);
            }
        });

        // Kuunnellaan tekstikenttien muutoksia tallennuspainikkeiden aktivointia varten
        lisaaMuutosKuuntelijat();

        cbLowCharset.getItems().setAll(
                "Automaattinen",
                "UTF-8",
                "Windows-1252",
                "ISO-8859-1",
                "UTF-16BE"
        );
        cbLowCharset.getSelectionModel().select("Automaattinen");
        cbLowCharset.setDisable(true);

        // Kielivalitsin (aidot lippukuvakkeet ja 2-kirjaimiset koodit: FI, EN, SV, DE, FR, ES)
        cbKieli.getItems().setAll(I18n.Language.values());
        cbKieli.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(I18n.Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(" " + item.getCode().toUpperCase() + "  " + item.getNativeName());
                    setGraphic(FlagIconHelper.createFlagView(item.getCode(), 20, 14));
                }
            }
        });
        cbKieli.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(I18n.Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(" " + item.getCode().toUpperCase());
                    setGraphic(FlagIconHelper.createFlagView(item.getCode(), 20, 14));
                }
            }
        });

        String tallennettuKieli = prefs.get(PREF_GUI_LANG, "FI");
        I18n.Language alkukieli = I18n.Language.fromCode(tallennettuKieli);
        I18n.setCurrentLanguage(alkukieli);
        cbKieli.setValue(alkukieli);
        paivitaKieliasetukset();

        paivitaKenttaSiirtoPainikkeet(false);
    }

    private void lisaaMuutosKuuntelijat() {
        TextField[] lowFields = {tfLowTitle, tfLowAuthor, tfLowPublisher, tfLowSubject, tfLowKeywords, tfLowDate, tfLowLanguage};
        for (TextField tf : lowFields) {
            tf.textProperty().addListener((obs, o, n) -> paivitaMuutosTilat());
        }

        TextField[] xmpFields = {tfXmpTitle, tfXmpAuthor, tfXmpPublisher, tfXmpSubject, tfXmpKeywords, tfXmpDate, tfXmpLanguage};
        for (TextField tf : xmpFields) {
            tf.textProperty().addListener((obs, o, n) -> paivitaMuutosTilat());
        }

        TextField[] epubFields = {tfEpubTitle, tfEpubAuthor, tfEpubPublisher, tfEpubSubject, tfEpubKeywords, tfEpubDate, tfEpubLanguage};
        for (TextField tf : epubFields) {
            tf.textProperty().addListener((obs, o, n) -> paivitaMuutosTilat());
        }
    }

    private void paivitaMuutosTilat() {
        if (latausKaynnissa || nykyinenTiedosto == null) {
            btnTallennaLow.setDisable(true);
            btnTallennaXmp.setDisable(true);
            btnTallennaEpub.setDisable(true);
            return;
        }

        boolean lowChanged = !kerääLowKentat().equals(alkuperainenLow);
        boolean xmpChanged = !kerääXmpKentat().equals(alkuperainenXmp);
        boolean epubChanged = epubPanel.isVisible() && !kerääEpubKentat().equals(alkuperainenEpub);

        btnTallennaLow.setDisable(!lowChanged);
        btnTallennaXmp.setDisable(!xmpChanged);
        btnTallennaEpub.setDisable(!epubChanged);

        boolean dirty = lowChanged || xmpChanged || epubChanged;
        if (nykyinenTiedosto.isDirty() != dirty) {
            nykyinenTiedosto.setDirty(dirty);
            fileListView.refresh();
        }
    }

    private boolean tarkistaTallentamattomatEnnenSiirtymista() {
        if (nykyinenTiedosto == null || !nykyinenTiedosto.isDirty()) {
            return true;
        }

        var valinta = DialogHelper.kysyTallennetaanko(nykyinenTiedosto.getFileName());
        if (valinta == DialogHelper.UnsavedPromptResult.CANCEL) {
            return false;
        } else if (valinta == DialogHelper.UnsavedPromptResult.SAVE) {
            tallennaKaikkiMuuttuneet();
            return true;
        } else {
            // Hylätään muutokset
            nykyinenTiedosto.setDirty(false);
            fileListView.refresh();
            return true;
        }
    }

    private void tallennaKaikkiMuuttuneet() {
        if (!btnTallennaLow.isDisable()) onTallennaLow();
        if (!btnTallennaXmp.isDisable()) onTallennaXmp();
        if (!btnTallennaEpub.isDisable()) onTallennaEpub();
    }

    private File haeAlkuHakemisto() {
        if (viimeisinHakemisto != null && viimeisinHakemisto.exists() && viimeisinHakemisto.isDirectory()) {
            return viimeisinHakemisto;
        }
        File home = new File(System.getProperty("user.home"));
        return (home.exists() && home.isDirectory()) ? home : null;
    }

    private void asetaViimeisinHakemisto(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            this.viimeisinHakemisto = dir;
            try {
                prefs.put(PREF_LAST_DIR, dir.getAbsolutePath());
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void onAvaaTiedosto() {
        if (!tarkistaTallentamattomatEnnenSiirtymista()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Avaa PDF- tai EPUB-tiedosto");

        File alkuHakemisto = haeAlkuHakemisto();
        if (alkuHakemisto != null) {
            fc.setInitialDirectory(alkuHakemisto);
        }

        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF ja EPUB tiedostot (*.pdf, *.epub)", "*.pdf", "*.epub", "*.PDF", "*.EPUB"),
                new FileChooser.ExtensionFilter("PDF-tiedostot (*.pdf)", "*.pdf", "*.PDF"),
                new FileChooser.ExtensionFilter("EPUB-tiedostot (*.epub)", "*.epub", "*.EPUB")
        );

        Stage stage = (Stage) btnAvaaTiedosto.getScene().getWindow();
        List<File> valitut = fc.showOpenMultipleDialog(stage);
        if (valitut != null && !valitut.isEmpty()) {
            asetaViimeisinHakemisto(valitut.get(0).getParentFile());
            tiedostot.clear();
            for (File file : valitut) {
                tiedostot.add(new FileItem(file.toPath()));
            }
            paivitaTiedostojaLkmLabel();
            paivitaHakemistoLabel(valitut.get(0).getParent());
            fileListView.getSelectionModel().select(0);
            paivitaNavigointiPainikkeet();
        }
    }

    @FXML
    private void onValitseHakemisto() {
        if (!tarkistaTallentamattomatEnnenSiirtymista()) return;

        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Valitse tarkasteltava hakemisto");

        File alkuHakemisto = haeAlkuHakemisto();
        if (alkuHakemisto != null) {
            dc.setInitialDirectory(alkuHakemisto);
        }

        Stage stage = (Stage) btnValitseHakemisto.getScene().getWindow();
        File dir = dc.showDialog(stage);
        if (dir != null) {
            asetaViimeisinHakemisto(dir);
            Path dirPath = dir.toPath();
            paivitaHakemistoLabel(dirPath.toString());
            tiedostot.clear();

            boolean recursive = cbAlihakemistot.isSelected();
            List<FileItem> loydetyt = new ArrayList<>();
            skannaaHakemisto(dirPath, loydetyt, recursive);
            loydetyt.sort((a, b) -> a.getFileName().compareToIgnoreCase(b.getFileName()));

            tiedostot.addAll(loydetyt);
            paivitaTiedostojaLkmLabel();

            if (!tiedostot.isEmpty()) {
                fileListView.getSelectionModel().select(0);
            } else {
                tyhjennaKentat();
                lblStatus.setText("Hakemistosta ei löytynyt PDF- tai EPUB-tiedostoja.");
            }
            paivitaNavigointiPainikkeet();
        }
    }

    private void skannaaHakemisto(Path dir, List<FileItem> result, boolean recursive) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && recursive) {
                    skannaaHakemisto(entry, result, true);
                } else if (Files.isRegularFile(entry)) {
                    String name = entry.getFileName().toString().toLowerCase();
                    if (name.endsWith(".pdf") || name.endsWith(".epub")) {
                        result.add(new FileItem(entry));
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private void avaaTiedostoKasittelyyn(FileItem item) {
        nykyinenTiedosto = item;
        paivitaTiedostoLabel(item.getFileName());
        lblStatus.setText("Luetaan metatietoja tiedostosta: " + item.getFileName());

        latausKaynnissa = true;
        try {
            suljeEpubPanelSisainen();
            if (item.isPdf()) {
                var res = pdfService.lueMetatiedot(item.getPath());
                alkuperainenLow = new DocumentMetadata(res.getLowLevelMetadata());
                alkuperainenXmp = new DocumentMetadata(res.getXmpMetadata());

                taytaLowKentat(res.getLowLevelMetadata());
                taytaXmpKentat(res.getXmpMetadata());

                cbLowCharset.setDisable(false);
                cbLowCharset.getSelectionModel().select("Automaattinen");
                paivitaMerkistoVaroitus(res.getLowLevelMetadata().getDetectedCharset());

                btnKopioiXmlToLow.setDisable(false);
                btnKopioiLowToXml.setDisable(false);
                paivitaKenttaSiirtoPainikkeet(true);
                btnEtsiEpub.setDisable(false);
                btnNimeaTiedosto.setDisable(false);
                btnEtsiMetadataTekstista.setDisable(false);
                btnHaeAiMetadataa.setDisable(false);
                lblStatus.setText("PDF-metatiedot luettu onnistuneesti.");
            } else if (item.isEpub()) {
                var res = epubService.lueMetatiedot(item.getPath());
                alkuperainenEpub = new DocumentMetadata(res);
                taytaEpubKentat(res);
                lblEpubTiedosto.setText("Tiedosto: " + item.getFileName());
                epubPanel.setVisible(true);
                epubPanel.setManaged(true);

                // Jos avattu on EPUB, PDF-kentät tyhjennetään tai pidetään selkeänä
                tyhjennaLowKentat();
                tyhjennaXmpKentat();
                cbLowCharset.setDisable(true);
                if (boxCharsetWarning != null) {
                    boxCharsetWarning.setVisible(false);
                    boxCharsetWarning.setManaged(false);
                }
                btnKopioiXmlToLow.setDisable(true);
                btnKopioiLowToXml.setDisable(true);
                paivitaKenttaSiirtoPainikkeet(false);
                btnEtsiEpub.setDisable(true);
                btnNimeaTiedosto.setDisable(false);
                btnEtsiMetadataTekstista.setDisable(false);
                btnHaeAiMetadataa.setDisable(false);
                lblStatus.setText("EPUB-metatiedot luettu.");
            }
        } catch (Exception e) {
            cbLowCharset.setDisable(true);
            if (boxCharsetWarning != null) {
                boxCharsetWarning.setVisible(false);
                boxCharsetWarning.setManaged(false);
            }
            paivitaKenttaSiirtoPainikkeet(false);
            btnEtsiMetadataTekstista.setDisable(true);
            btnHaeAiMetadataa.setDisable(true);
            DialogHelper.naytaVirhe("Virhe tiedoston luvussa", "Metatietojen luku epäonnistui: " + e.getMessage());
            lblStatus.setText("Virhe luettaessa metatietoja: " + e.getMessage());
        } finally {
            latausKaynnissa = false;
            paivitaMuutosTilat();
            paivitaNavigointiPainikkeet();
        }
    }

    private void paivitaNavigointiPainikkeet() {
        int idx = fileListView.getSelectionModel().getSelectedIndex();
        int size = tiedostot.size();
        btnEdellinen.setDisable(size <= 1 || idx <= 0);
        btnSeuraava.setDisable(size <= 1 || idx < 0 || idx >= size - 1);
    }

    @FXML
    private void onEdellinen() {
        if (!tarkistaTallentamattomatEnnenSiirtymista()) return;
        int idx = fileListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            fileListView.getSelectionModel().select(idx - 1);
        }
    }

    @FXML
    private void onSeuraava() {
        if (!tarkistaTallentamattomatEnnenSiirtymista()) return;
        int idx = fileListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < tiedostot.size() - 1) {
            fileListView.getSelectionModel().select(idx + 1);
        }
    }

    @FXML
    private void onNimeaTiedosto() {
        if (nykyinenTiedosto == null) return;
        String nykyinenNimi = nykyinenTiedosto.getFileName();
        Optional<String> uusiNimiOpt = DialogHelper.kysyUusiTiedostonimi(nykyinenNimi);
        if (uusiNimiOpt.isPresent() && !uusiNimiOpt.get().trim().isEmpty()) {
            String uusiNimi = uusiNimiOpt.get().trim();
            if (uusiNimi.equals(nykyinenNimi)) return;

            Path nykyinenPolku = nykyinenTiedosto.getPath();
            Path uusiPolku = nykyinenPolku.resolveSibling(uusiNimi);

            try {
                Files.move(nykyinenPolku, uusiPolku);
                nykyinenTiedosto.setPath(uusiPolku);
                paivitaTiedostoLabel(uusiNimi);
                fileListView.refresh();
                lblStatus.setText("Tiedosto nimettiin uudelleen: " + uusiNimi);
            } catch (Exception e) {
                DialogHelper.naytaVirhe("Nimeämisvirhe", "Tiedoston nimeäminen epäonnistui: " + e.getMessage());
            }
        }
    }

    /**
     * Avaa tiedoston käyttöjärjestelmän oletusohjelmalla (Linux: xdg-open, Mac: open, Windows: start).
     */
    private void avaaTiedostoJarjestelmassa(Path path) {
        if (path == null || !Files.exists(path)) return;
        lblStatus.setText("Avataan oletusohjelmalla: " + path.getFileName());

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd.exe", "/c", "start", "", path.toAbsolutePath().toString()).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
            } else {
                new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
            }
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Avaaminen epäonnistui", "Tiedostoa ei voitu avata järjestelmän katseluohjelmalla: " + e.getMessage());
            lblStatus.setText("Virhe avattaessa tiedostoa katseluohjelmalla: " + e.getMessage());
        }
    }

    @FXML
    private void onEtsiEpub() {
        if (nykyinenTiedosto == null || !nykyinenTiedosto.isPdf()) {
            DialogHelper.naytaInfo("EPUB-haku", "EPUB-vastaavuuden haku on käytettävissä vain PDF-tiedostoille.");
            return;
        }

        List<Path> loydetyt = epubMatcher.etsiVastaavatEpubTiedostot(nykyinenTiedosto.getPath(), cbAlihakemistot.isSelected());
        if (loydetyt.isEmpty()) {
            DialogHelper.naytaInfo("EPUB-haku", "Vastaavaa .epub-tiedostoa ei löytynyt samalla nimellä tai hakemistosta.");
            lblStatus.setText("Ei löytynyt vastaavaa EPUB-tiedostoa.");
            return;
        }

        Path valittuEpub;
        if (loydetyt.size() == 1) {
            valittuEpub = loydetyt.get(0);
        } else {
            Optional<Path> valinta = DialogHelper.valitseEpubLista(loydetyt);
            if (valinta.isEmpty()) return;
            valittuEpub = valinta.get();
        }

        // Avataan EPUB-metatiedot vertailuun (ei automaattista kopiointia!)
        try {
            avattuEpubVertailuPolku = valittuEpub;
            DocumentMetadata meta = epubService.lueMetatiedot(valittuEpub);
            alkuperainenEpub = new DocumentMetadata(meta);

            latausKaynnissa = true;
            taytaEpubKentat(meta);
            lblEpubTiedosto.setText("Tiedosto: " + valittuEpub.getFileName());
            epubPanel.setVisible(true);
            epubPanel.setManaged(true);
            latausKaynnissa = false;

            lblStatus.setText("Vastaava EPUB löydetty ja avattu vertailuun: " + valittuEpub.getFileName());
            DialogHelper.naytaInfo("Vastaava EPUB avattu",
                    "EPUB '" + valittuEpub.getFileName() + "' metatiedot on tuotu näytölle vertailtavaksi.\n" +
                    "Voit käyttää kopiointipainikkeita siirtääksesi haluamasi tiedot.");
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Virhe EPUB-luvussa", "EPUB-metatietojen luku epäonnistui: " + e.getMessage());
        }
    }

    @FXML
    private void onEtsiMetadataTekstista() {
        if (nykyinenTiedosto == null) return;

        try {
            boolean isPdf = nykyinenTiedosto.isPdf();
            DocumentMetadata nykyiset = isPdf ? kerääLowKentat() : kerääEpubKentat();
            List<ExtractedField> loydetyt = textExtractor.etsiMetatiedotTekstista(nykyinenTiedosto.getPath(), nykyiset);

            if (loydetyt.isEmpty()) {
                DialogHelper.naytaInfo("Metadata tekstistä", "Tiedoston tekstisisällöstä ei tunnistettu uusia metatietoja.");
                return;
            }

            Stage stage = (Stage) btnEtsiMetadataTekstista.getScene().getWindow();
            Optional<List<ExtractedField>> valitutOpt = MetadataExtractionDialog.nayta(
                    stage,
                    loydetyt,
                    isPdf ? "PDF" : "EPUB"
            );

            if (valitutOpt.isPresent() && !valitutOpt.get().isEmpty()) {
                List<ExtractedField> valitut = valitutOpt.get();
                for (ExtractedField f : valitut) {
                    asetaKenttaArvo(f.getKey(), f.getExtractedValue(), isPdf);
                }
                paivitaMuutosTilat();
                lblStatus.setText("Tekstistä poimitut metatiedot viety " + (isPdf ? "raaka data -kenttiin." : "EPUB-kenttiin."));
            }
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Virhe tekstin haussa", "Metatietojen louhinta epäonnistui: " + e.getMessage());
        }
    }

    @FXML
    private void onHaeAiMetadataa() {
        if (nykyinenTiedosto == null) return;

        try {
            boolean isPdf = nykyinenTiedosto.isPdf();
            DocumentMetadata nykyiset = isPdf ? kerääLowKentat() : kerääEpubKentat();
            lblStatus.setText("Haetaan metatietoja tekoälyn (AI) avulla...");

            Stage stage = (Stage) btnHaeAiMetadataa.getScene().getWindow();
            List<ExtractedField> loydetyt = aiExtractor.etsiAiMetatiedot(stage, nykyinenTiedosto.getPath(), nykyiset);

            if (loydetyt == null || loydetyt.isEmpty()) {
                DialogHelper.naytaInfo("AI-metatiedot", "Tekoäly ei löytänyt tai erotellut uusia metatietoja tiedoston tekstistä.");
                lblStatus.setText("AI-metatietojen haku valmistui ilman uusia ehdotuksia.");
                return;
            }

            Optional<List<ExtractedField>> valitutOpt = MetadataExtractionDialog.nayta(
                    stage,
                    loydetyt,
                    isPdf ? "PDF (AI)" : "EPUB (AI)"
            );

            if (valitutOpt.isPresent() && !valitutOpt.get().isEmpty()) {
                List<ExtractedField> valitut = valitutOpt.get();
                for (ExtractedField f : valitut) {
                    asetaKenttaArvo(f.getKey(), f.getExtractedValue(), isPdf);
                }
                paivitaMuutosTilat();
                lblStatus.setText("Tekoälyn poimimat metatiedot viety " + (isPdf ? "raaka data -kenttiin." : "EPUB-kenttiin."));
            } else {
                lblStatus.setText("AI-metatietojen vienti peruutettu.");
            }
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Virhe AI-haussa", "Tekoälypohjainen metatietojen haku epäonnistui: " + e.getMessage());
            lblStatus.setText("Virhe AI-metatietojen haussa: " + e.getMessage());
        }
    }

    @FXML
    private void onNaytaOhje() {
        Window owner = btnOhje != null && btnOhje.getScene() != null ? btnOhje.getScene().getWindow() : null;
        HelpDialog.nayta(owner);
    }

    @FXML
    private void onVaihdaKieli() {
        I18n.Language valittu = cbKieli.getValue();
        if (valittu != null) {
            I18n.setCurrentLanguage(valittu);
            prefs.put(PREF_GUI_LANG, valittu.name());
            paivitaKieliasetukset();
        }
    }

    private void paivitaHakemistoLabel(String polku) {
        if (polku != null && !polku.isEmpty()) {
            lblHakemisto.setText(I18n.get("lbl.dir") + polku);
        } else {
            lblHakemisto.setText(I18n.get("lbl.dir") + I18n.get("lbl.dir_none"));
        }
    }

    private void paivitaTiedostoLabel(String tiedostonimi) {
        if (tiedostonimi != null && !tiedostonimi.isEmpty()) {
            lblTiedosto.setText(I18n.get("lbl.file") + tiedostonimi);
        } else {
            lblTiedosto.setText(I18n.get("lbl.file") + I18n.get("lbl.file_none"));
        }
    }

    private void paivitaTiedostojaLkmLabel() {
        lblTiedostojaLkm.setText(I18n.get("lbl.files_title") + " (" + tiedostot.size() + " " +
                (I18n.getCurrentLanguage() == I18n.Language.FI ? "kpl" : "items") + ")");
    }

    private void paivitaKieliasetukset() {
        // Yläpalkki
        btnAvaaTiedosto.setText(I18n.get("btn.open_file"));
        btnValitseHakemisto.setText(I18n.get("btn.select_dir"));
        cbAlihakemistot.setText(I18n.get("cb.subdirs"));
        btnEdellinen.setText(I18n.get("btn.prev"));
        btnSeuraava.setText(I18n.get("btn.next"));
        btnNimeaTiedosto.setText(I18n.get("btn.rename"));
        btnEtsiEpub.setText(I18n.get("btn.find_epub"));
        btnEtsiMetadataTekstista.setText(I18n.get("btn.extract_text"));
        btnHaeAiMetadataa.setText(I18n.get("btn.extract_ai"));
        btnOhje.setText(I18n.get("btn.help"));

        // Yläpalkin polkuinfot
        if (viimeisinHakemisto != null) {
            paivitaHakemistoLabel(viimeisinHakemisto.getAbsolutePath());
        } else {
            paivitaHakemistoLabel(null);
        }

        if (nykyinenTiedosto != null) {
            paivitaTiedostoLabel(nykyinenTiedosto.getFileName());
        } else {
            paivitaTiedostoLabel(null);
        }

        paivitaTiedostojaLkmLabel();

        // Kopiointipalkki
        btnKopioiXmlToLow.setText(I18n.get("btn.copy_xml_to_low"));
        btnKopioiLowToXml.setText(I18n.get("btn.copy_low_to_xml"));

        // Matalan tason paneeli
        if (lblLowPanelTitle != null) lblLowPanelTitle.setText(I18n.get("panel.low_title"));
        if (lblLowCharsetText != null) lblLowCharsetText.setText(I18n.get("lbl.charset"));
        btnKorjaaMerkisto.setText(I18n.get("btn.fix_charset"));
        btnTallennaLow.setText(I18n.get("btn.save_low"));

        if (lblLowTitleLabel != null) lblLowTitleLabel.setText(I18n.get("field.title"));
        if (lblLowAuthorLabel != null) lblLowAuthorLabel.setText(I18n.get("field.author"));
        if (lblLowPublisherLabel != null) lblLowPublisherLabel.setText(I18n.get("field.publisher"));
        if (lblLowSubjectLabel != null) lblLowSubjectLabel.setText(I18n.get("field.subject"));
        if (lblLowKeywordsLabel != null) lblLowKeywordsLabel.setText(I18n.get("field.keywords"));
        if (lblLowDateLabel != null) lblLowDateLabel.setText(I18n.get("field.date"));
        if (lblLowLanguageLabel != null) lblLowLanguageLabel.setText(I18n.get("field.language"));

        // XML-paneeli
        if (lblXmpPanelTitle != null) lblXmpPanelTitle.setText(I18n.get("panel.xmp_title"));
        if (lblXmpStandard != null) lblXmpStandard.setText(I18n.get("lbl.std_unicode"));
        btnTallennaXmp.setText(I18n.get("btn.save_xmp"));

        if (lblXmpTitleLabel != null) lblXmpTitleLabel.setText(I18n.get("field.title"));
        if (lblXmpAuthorLabel != null) lblXmpAuthorLabel.setText(I18n.get("field.author"));
        if (lblXmpPublisherLabel != null) lblXmpPublisherLabel.setText(I18n.get("field.publisher"));
        if (lblXmpSubjectLabel != null) lblXmpSubjectLabel.setText(I18n.get("field.subject"));
        if (lblXmpKeywordsLabel != null) lblXmpKeywordsLabel.setText(I18n.get("field.keywords"));
        if (lblXmpDateLabel != null) lblXmpDateLabel.setText(I18n.get("field.date"));
        if (lblXmpLanguageLabel != null) lblXmpLanguageLabel.setText(I18n.get("field.language"));

        // EPUB-paneeli
        if (lblEpubPanelTitle != null) lblEpubPanelTitle.setText(I18n.get("panel.epub_title"));
        if (btnSuljeEpub != null) btnSuljeEpub.setText(I18n.get("btn.close_epub"));
        btnTallennaEpub.setText(I18n.get("btn.save_epub"));

        if (lblEpubTitleLabel != null) lblEpubTitleLabel.setText(I18n.get("field.title"));
        if (lblEpubAuthorLabel != null) lblEpubAuthorLabel.setText(I18n.get("field.author"));
        if (lblEpubPublisherLabel != null) lblEpubPublisherLabel.setText(I18n.get("field.publisher"));
        if (lblEpubSubjectLabel != null) lblEpubSubjectLabel.setText(I18n.get("field.subject"));
        if (lblEpubKeywordsLabel != null) lblEpubKeywordsLabel.setText(I18n.get("field.keywords"));
        if (lblEpubDateLabel != null) lblEpubDateLabel.setText(I18n.get("field.date"));
        if (lblEpubLanguageLabel != null) lblEpubLanguageLabel.setText(I18n.get("field.language"));

        if (btnKopioiEpubToLow != null) btnKopioiEpubToLow.setText(I18n.get("btn.epub_to_low"));
        if (btnKopioiEpubToXmp != null) btnKopioiEpubToXmp.setText(I18n.get("btn.epub_to_xmp"));
        if (btnKopioiLowToEpub != null) btnKopioiLowToEpub.setText(I18n.get("btn.low_to_epub"));
        if (btnKopioiXmpToEpub != null) btnKopioiXmpToEpub.setText(I18n.get("btn.xmp_to_epub"));

        lblStatus.setText(I18n.get("status.ready"));
    }

    private void asetaKenttaArvo(String key, String value, boolean isPdf) {
        if (isPdf) {
            // PDF: raaka data kenttiin (matalan tason kentät)
            switch (key) {
                case "title" -> tfLowTitle.setText(value);
                case "author" -> tfLowAuthor.setText(value);
                case "publisher" -> tfLowPublisher.setText(value);
                case "subject" -> tfLowSubject.setText(value);
                case "keywords" -> tfLowKeywords.setText(value);
                case "date" -> tfLowDate.setText(value);
                case "language" -> tfLowLanguage.setText(value);
            }
        } else {
            // EPUB: omaan EPUB-kenttäryhmään
            switch (key) {
                case "title" -> tfEpubTitle.setText(value);
                case "author" -> tfEpubAuthor.setText(value);
                case "publisher" -> tfEpubPublisher.setText(value);
                case "subject" -> tfEpubSubject.setText(value);
                case "keywords" -> tfEpubKeywords.setText(value);
                case "date" -> tfEpubDate.setText(value);
                case "language" -> tfEpubLanguage.setText(value);
            }
        }
    }

    @FXML
    private void onSuljeEpubPanel() {
        suljeEpubPanelSisainen();
    }

    private void suljeEpubPanelSisainen() {
        epubPanel.setVisible(false);
        epubPanel.setManaged(false);
        avattuEpubVertailuPolku = null;
        tyhjennaEpubKentat();
        btnTallennaEpub.setDisable(true);
    }

    // Tallennustoiminnot
    @FXML
    private void onTallennaLow() {
        if (nykyinenTiedosto == null || !nykyinenTiedosto.isPdf()) return;
        DocumentMetadata meta = kerääLowKentat();
        try {
            pdfService.tallennaMatalanTasonMetatiedot(nykyinenTiedosto.getPath(), meta);
            meta.setDetectedCharset("UTF-16BE");
            alkuperainenLow = new DocumentMetadata(meta);
            lblLowCharset.setText("Tunnistettu: UTF-16BE");
            if (boxCharsetWarning != null) {
                boxCharsetWarning.setVisible(false);
                boxCharsetWarning.setManaged(false);
            }
            lblStatus.setText("Matalan tason PDF-metatiedot tallennettu onnistuneesti (UTF-16BE Unicode).");
            paivitaMuutosTilat();
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Tallennusvirhe", "Matalan tason metatietojen tallennus epäonnistui: " + e.getMessage());
        }
    }

    @FXML
    private void onTallennaXmp() {
        if (nykyinenTiedosto == null || !nykyinenTiedosto.isPdf()) return;
        DocumentMetadata meta = kerääXmpKentat();
        try {
            pdfService.tallennaXmpMetatiedot(nykyinenTiedosto.getPath(), meta);
            alkuperainenXmp = new DocumentMetadata(meta);
            lblStatus.setText("XML/XMP PDF-metatiedot tallennettu onnistuneesti.");
            paivitaMuutosTilat();
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Tallennusvirhe", "XML/XMP metatietojen tallennus epäonnistui: " + e.getMessage());
        }
    }

    @FXML
    private void onTallennaEpub() {
        Path targetPath = (nykyinenTiedosto != null && nykyinenTiedosto.isEpub())
                ? nykyinenTiedosto.getPath()
                : avattuEpubVertailuPolku;

        if (targetPath == null) return;

        DocumentMetadata meta = kerääEpubKentat();
        try {
            epubService.tallennaMetatiedot(targetPath, meta);
            alkuperainenEpub = new DocumentMetadata(meta);
            lblStatus.setText("EPUB-metatiedot tallennettu tiedostoon: " + targetPath.getFileName());
            paivitaMuutosTilat();
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Tallennusvirhe", "EPUB-metatietojen tallennus epäonnistui: " + e.getMessage());
        }
    }

    // Kopioinnit kenttäryhmien välillä
    @FXML
    private void onKopioiXmlToLow() {
        DocumentMetadata xmp = kerääXmpKentat();
        taytaLowKentat(xmp);
        paivitaMuutosTilat();
        lblStatus.setText("Kopioitu tiedot XML-kentistä matalan tason kenttiin.");
    }

    @FXML
    private void onKopioiLowToXml() {
        DocumentMetadata low = kerääLowKentat();
        taytaXmpKentat(low);
        paivitaMuutosTilat();
        lblStatus.setText("Kopioitu tiedot matalan tason kentistä XML-kenttiin.");
    }

    @FXML
    private void onKopioiEpubToLow() {
        DocumentMetadata epub = kerääEpubKentat();
        taytaLowKentat(epub);
        paivitaMuutosTilat();
        lblStatus.setText("Kopioitu tiedot EPUB-kentistä PDF:n matalan tason kenttiin.");
    }

    @FXML
    private void onKopioiEpubToXmp() {
        DocumentMetadata epub = kerääEpubKentat();
        taytaXmpKentat(epub);
        paivitaMuutosTilat();
        lblStatus.setText("Kopioitu tiedot EPUB-kentistä PDF:n XML-kenttiin.");
    }

    @FXML
    private void onKopioiLowToEpub() {
        DocumentMetadata low = kerääLowKentat();
        taytaEpubKentat(low);
        paivitaMuutosTilat();
        lblStatus.setText("Kopioitu tiedot PDF:n matalan tason kentistä EPUB-kenttiin.");
    }

    @FXML
    private void onKopioiXmpToEpub() {
        DocumentMetadata xmp = kerääXmpKentat();
        taytaEpubKentat(xmp);
        paivitaMuutosTilat();
        lblStatus.setText("Kopioitu tiedot PDF:n XML-kentistä EPUB-kenttiin.");
    }

    // Kenttäkohtaiset kopioinnit matala taso -> XML
    @FXML
    private void onKopioiLowTitleToXmp() {
        tfXmpTitle.setText(tfLowTitle.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Otsikko kopioitu matalalta tasolta XML-kenttään.");
    }

    @FXML
    private void onKopioiLowAuthorToXmp() {
        tfXmpAuthor.setText(tfLowAuthor.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Tekijä kopioitu matalalta tasolta XML-kenttään.");
    }

    @FXML
    private void onKopioiLowPublisherToXmp() {
        tfXmpPublisher.setText(tfLowPublisher.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Kustantaja kopioitu matalalta tasolta XML-kenttään.");
    }

    @FXML
    private void onKopioiLowSubjectToXmp() {
        tfXmpSubject.setText(tfLowSubject.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Aihe kopioitu matalalta tasolta XML-kenttään.");
    }

    @FXML
    private void onKopioiLowKeywordsToXmp() {
        tfXmpKeywords.setText(tfLowKeywords.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Avainsanat kopioitu matalalta tasolta XML-kenttään.");
    }

    @FXML
    private void onKopioiLowDateToXmp() {
        tfXmpDate.setText(tfLowDate.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Päiväys kopioitu matalalta tasolta XML-kenttään.");
    }

    @FXML
    private void onKopioiLowLanguageToXmp() {
        tfXmpLanguage.setText(tfLowLanguage.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Kieli kopioitu matalalta tasolta XML-kenttään.");
    }

    // Kenttäkohtaiset kopioinnit XML -> matala taso
    @FXML
    private void onKopioiXmpTitleToLow() {
        tfLowTitle.setText(tfXmpTitle.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Otsikko kopioitu XML-kentästä matalan tason kenttään.");
    }

    @FXML
    private void onKopioiXmpAuthorToLow() {
        tfLowAuthor.setText(tfXmpAuthor.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Tekijä kopioitu XML-kentästä matalan tason kenttään.");
    }

    @FXML
    private void onKopioiXmpPublisherToLow() {
        tfLowPublisher.setText(tfXmpPublisher.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Kustantaja kopioitu XML-kentästä matalan tason kenttään.");
    }

    @FXML
    private void onKopioiXmpSubjectToLow() {
        tfLowSubject.setText(tfXmpSubject.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Aihe kopioitu XML-kentästä matalan tason kenttään.");
    }

    @FXML
    private void onKopioiXmpKeywordsToLow() {
        tfLowKeywords.setText(tfXmpKeywords.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Avainsanat kopioitu XML-kentästä matalan tason kenttään.");
    }

    @FXML
    private void onKopioiXmpDateToLow() {
        tfLowDate.setText(tfXmpDate.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Päiväys kopioitu XML-kentästä matalan tason kenttään.");
    }

    @FXML
    private void onKopioiXmpLanguageToLow() {
        tfLowLanguage.setText(tfXmpLanguage.getText());
        paivitaMuutosTilat();
        lblStatus.setText("Kieli kopioitu XML-kentästä matalan tason kenttään.");
    }

    private void paivitaKenttaSiirtoPainikkeet(boolean enabled) {
        Button[] buttons = {
            btnLowTitleToXmp, btnLowAuthorToXmp, btnLowPublisherToXmp,
            btnLowSubjectToXmp, btnLowKeywordsToXmp, btnLowDateToXmp, btnLowLanguageToXmp,
            btnXmpTitleToLow, btnXmpAuthorToLow, btnXmpPublisherToLow,
            btnXmpSubjectToLow, btnXmpKeywordsToLow, btnXmpDateToLow, btnXmpLanguageToLow
        };
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setDisable(!enabled);
            }
        }
    }

    // Apumetodit kenttien lukemiseen ja asettamiseen
    private DocumentMetadata kerääLowKentat() {
        DocumentMetadata m = new DocumentMetadata();
        m.setTitle(tfLowTitle.getText().trim());
        m.setAuthor(tfLowAuthor.getText().trim());
        m.setPublisher(tfLowPublisher.getText().trim());
        m.setSubject(tfLowSubject.getText().trim());
        m.setKeywords(tfLowKeywords.getText().trim());
        m.setDate(tfLowDate.getText().trim());
        m.setLanguage(tfLowLanguage.getText().trim());
        return m;
    }

    private DocumentMetadata kerääXmpKentat() {
        DocumentMetadata m = new DocumentMetadata();
        m.setTitle(tfXmpTitle.getText().trim());
        m.setAuthor(tfXmpAuthor.getText().trim());
        m.setPublisher(tfXmpPublisher.getText().trim());
        m.setSubject(tfXmpSubject.getText().trim());
        m.setKeywords(tfXmpKeywords.getText().trim());
        m.setDate(tfXmpDate.getText().trim());
        m.setLanguage(tfXmpLanguage.getText().trim());
        return m;
    }

    private DocumentMetadata kerääEpubKentat() {
        DocumentMetadata m = new DocumentMetadata();
        m.setTitle(tfEpubTitle.getText().trim());
        m.setAuthor(tfEpubAuthor.getText().trim());
        m.setPublisher(tfEpubPublisher.getText().trim());
        m.setSubject(tfEpubSubject.getText().trim());
        m.setKeywords(tfEpubKeywords.getText().trim());
        m.setDate(tfEpubDate.getText().trim());
        m.setLanguage(tfEpubLanguage.getText().trim());
        return m;
    }

    private void taytaLowKentat(DocumentMetadata m) {
        tfLowTitle.setText(m.getTitle());
        tfLowAuthor.setText(m.getAuthor());
        tfLowPublisher.setText(m.getPublisher());
        tfLowSubject.setText(m.getSubject());
        tfLowKeywords.setText(m.getKeywords());
        tfLowDate.setText(m.getDate());
        tfLowLanguage.setText(m.getLanguage());
        if (!m.getDetectedCharset().isEmpty()) {
            lblLowCharset.setText("Tunnistettu: " + m.getDetectedCharset());
        }
    }

    @FXML
    private void onValitseMerkisto() {
        if (latausKaynnissa || nykyinenTiedosto == null || !nykyinenTiedosto.isPdf()) return;
        String valittu = cbLowCharset.getValue();
        if (valittu == null) return;

        try {
            DocumentMetadata uusiLow = pdfService.lueMatalanTasonMetatiedot(nykyinenTiedosto.getPath(), valittu);
            latausKaynnissa = true;
            taytaLowKentat(uusiLow);
            latausKaynnissa = false;

            paivitaMerkistoVaroitus(uusiLow.getDetectedCharset());
            paivitaMuutosTilat();
            lblStatus.setText("Matalan tason metatiedot purettu merkistöllä: " + valittu);
        } catch (Exception e) {
            DialogHelper.naytaVirhe("Merkistövirhe", "Metatietojen uudelleendekoodaus epäonnistui: " + e.getMessage());
        }
    }

    @FXML
    private void onKorjaaMerkisto() {
        if (nykyinenTiedosto == null || !nykyinenTiedosto.isPdf()) return;

        tfLowTitle.setText(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(tfLowTitle.getText())));
        tfLowAuthor.setText(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(tfLowAuthor.getText())));
        tfLowPublisher.setText(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(tfLowPublisher.getText())));
        tfLowSubject.setText(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(tfLowSubject.getText())));
        tfLowKeywords.setText(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(tfLowKeywords.getText())));
        tfLowDate.setText(CharsetFixerService.siistiTeksti(tfLowDate.getText()));
        tfLowLanguage.setText(CharsetFixerService.siistiTeksti(tfLowLanguage.getText()));

        cbLowCharset.getSelectionModel().select("UTF-8");
        lblLowCharset.setText("Tunnistettu: UTF-16BE (Unicode korjattu)");
        if (boxCharsetWarning != null) {
            boxCharsetWarning.setVisible(false);
            boxCharsetWarning.setManaged(false);
        }

        paivitaMuutosTilat();
        btnTallennaLow.setDisable(false);
        nykyinenTiedosto.setDirty(true);
        fileListView.refresh();
        lblStatus.setText("Merkistö korjattu Unicode-muotoon. Tallenna tiedosto painikkeella 'Tallenna PDF (Matala taso)'.");
    }

    private void paivitaMerkistoVaroitus(String charsetName) {
        if (charsetName == null || charsetName.isEmpty() || nykyinenTiedosto == null || !nykyinenTiedosto.isPdf()) {
            if (boxCharsetWarning != null) {
                boxCharsetWarning.setVisible(false);
                boxCharsetWarning.setManaged(false);
            }
            return;
        }

        String upper = charsetName.toUpperCase();
        boolean onStandardiUtf = upper.contains("UTF-8") || upper.contains("UTF-16");
        boolean onVierasMerkisto = upper.contains("WINDOWS") || upper.contains("ISO-8859");

        if (!onStandardiUtf || onVierasMerkisto) {
            lblCharsetWarning.setText("⚠️ Huom: Alkuperäinen merkistö on " + charsetName + " (ei-standardi). Korjaa merkistö UTF:ksi tai siirrä XML-paneeliin.");
            if (boxCharsetWarning != null) {
                boxCharsetWarning.setVisible(true);
                boxCharsetWarning.setManaged(true);
            }
        } else {
            if (boxCharsetWarning != null) {
                boxCharsetWarning.setVisible(false);
                boxCharsetWarning.setManaged(false);
            }
        }
    }

    private void taytaXmpKentat(DocumentMetadata m) {
        tfXmpTitle.setText(m.getTitle());
        tfXmpAuthor.setText(m.getAuthor());
        tfXmpPublisher.setText(m.getPublisher());
        tfXmpSubject.setText(m.getSubject());
        tfXmpKeywords.setText(m.getKeywords());
        tfXmpDate.setText(m.getDate());
        tfXmpLanguage.setText(m.getLanguage());
        if (!m.getDetectedCharset().isEmpty()) {
            lblXmpCharset.setText("Merkistö: " + m.getDetectedCharset());
        }
    }

    private void taytaEpubKentat(DocumentMetadata m) {
        tfEpubTitle.setText(m.getTitle());
        tfEpubAuthor.setText(m.getAuthor());
        tfEpubPublisher.setText(m.getPublisher());
        tfEpubSubject.setText(m.getSubject());
        tfEpubKeywords.setText(m.getKeywords());
        tfEpubDate.setText(m.getDate());
        tfEpubLanguage.setText(m.getLanguage());
    }

    private void tyhjennaKentat() {
        tyhjennaLowKentat();
        tyhjennaXmpKentat();
        tyhjennaEpubKentat();
        paivitaTiedostoLabel(null);
        btnEtsiEpub.setDisable(true);
        btnEtsiMetadataTekstista.setDisable(true);
        btnNimeaTiedosto.setDisable(true);
        btnKopioiXmlToLow.setDisable(true);
        btnKopioiLowToXml.setDisable(true);
    }

    private void tyhjennaLowKentat() {
        tfLowTitle.clear();
        tfLowAuthor.clear();
        tfLowPublisher.clear();
        tfLowSubject.clear();
        tfLowKeywords.clear();
        tfLowDate.clear();
        tfLowLanguage.clear();
        lblLowCharset.setText("Merkistö: -");
        btnTallennaLow.setDisable(true);
    }

    private void tyhjennaXmpKentat() {
        tfXmpTitle.clear();
        tfXmpAuthor.clear();
        tfXmpPublisher.clear();
        tfXmpSubject.clear();
        tfXmpKeywords.clear();
        tfXmpDate.clear();
        tfXmpLanguage.clear();
        lblXmpCharset.setText("Merkistö: UTF-8 (XMP)");
        btnTallennaXmp.setDisable(true);
    }

    private void tyhjennaEpubKentat() {
        tfEpubTitle.clear();
        tfEpubAuthor.clear();
        tfEpubPublisher.clear();
        tfEpubSubject.clear();
        tfEpubKeywords.clear();
        tfEpubDate.clear();
        tfEpubLanguage.clear();
        lblEpubTiedosto.setText("Tiedosto: -");
        btnTallennaEpub.setDisable(true);
    }
}
