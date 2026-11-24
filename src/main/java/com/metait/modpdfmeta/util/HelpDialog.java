package com.metait.modpdfmeta.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Monikielinen käyttöohjedialogi (Suomi, English, Svenska, Deutsch, Français, Español).
 */
public class HelpDialog {

    public static void nayta(Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Käyttöohje / User Guide / Benutzerhandbuch / Guide / Guía / Hjälp");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefSize(860, 640);
        dp.getButtonTypes().add(ButtonType.CLOSE);

        var cssResource = HelpDialog.class.getResource("/com/metait/modpdfmeta/dark-theme.css");
        if (cssResource != null) {
            dp.getStylesheets().add(cssResource.toExternalForm());
        }

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().add(luoTab("fi", "Suomi", luoOhjeSuomi()));
        tabPane.getTabs().add(luoTab("en", "English", luoOhjeEnglish()));
        tabPane.getTabs().add(luoTab("sv", "Svenska", luoOhjeSvenska()));
        tabPane.getTabs().add(luoTab("de", "Deutsch", luoOhjeDeutsch()));
        tabPane.getTabs().add(luoTab("fr", "Français", luoOhjeFrancais()));
        tabPane.getTabs().add(luoTab("es", "Español", luoOhjeEspanol()));

        int tabIndex = switch (I18n.getCurrentLanguage()) {
            case FI -> 0;
            case EN -> 1;
            case SV -> 2;
            case DE -> 3;
            case FR -> 4;
            case ES -> 5;
        };
        tabPane.getSelectionModel().select(tabIndex);

        VBox content = new VBox(10, tabPane);
        content.setPadding(new Insets(10));
        dp.setContent(content);

        dialog.showAndWait();
    }

    private static Tab luoTab(String langCode, String otsikko, VBox sisalto) {
        Tab tab = new Tab(" " + otsikko);
        ImageView flag = FlagIconHelper.createFlagView(langCode, 20, 14);
        if (flag != null) {
            tab.setGraphic(flag);
        }
        ScrollPane sp = new ScrollPane(sisalto);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tab.setContent(sp);
        return tab;
    }

    private static VBox luoOhjeSuomi() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        box.getChildren().add(luoOtsikko("modpdfmeta – PDF & EPUB Metatietoeditori"));
        box.getChildren().add(luoKappale("Tämä ohjelma on suunniteltu PDF- ja EPUB-dokumenttien metatietojen tarkasteluun, muokkaamiseen, merkistökorjauksiin sekä tekoäly- ja tekstipohjaiseen metatietojen louhintaan."));

        box.getChildren().add(luoValiotsikko("1. Tiedostojen avaaminen ja selaus"));
        box.getChildren().add(luoKappale("• Avaa tiedosto: Valitse yksittäinen .pdf- tai .epub-tiedosto.\n" +
                "• Valitse hakemisto: Lataa kansion kaikki tuetut tiedostot listaan. Rastita 'Lue myös alihakemistot' sisällyttääksesi alakansiot.\n" +
                "• Edellinen / Seuraava: Selaa listan tiedostoja järjestyksessä.\n" +
                "• Tiedostolistan väritunnisteet: [PDF] näkyy vaaleanpunaisella ja [EPUB] vihreällä taustalla. Tallentamattomat muutokset merkitään tähdellä (*).\n" +
                "• Kaksoisklikkaus (2x): Avaa valitun tiedoston suoraan käyttöjärjestelmän oletusohjelmalla katselua varten."));

        box.getChildren().add(luoValiotsikko("2. PDF-metatiedot: Matala taso vs. XML / XMP"));
        box.getChildren().add(luoKappale("PDF-dokumenteissa on kaksi erillistä metatietotasoa:\n" +
                "1. Matalan tason metatiedot (Document Information Dictionary): Tiedoston perinteinen tietorakenne (Title, Author, Subject jne.).\n" +
                "2. XML-tason metatiedot (Adobe XMP): Moderni XML-standardi (Dublin Core ja XMP Basic).\n" +
                "• Nuolipainikkeet: Siirrä yksittäisiä kenttiä tasolta toiselle (► siirtää matalalta XML-tasolle, ◄ siirtää XML-tasolta matalalle).\n" +
                "• Ryhmäpainikkeet: Voit myös kopioida kaikki kentät kerralla paneelien välillä."));

        box.getChildren().add(luoValiotsikko("3. Merkistön korjaus ja Mojibaken purku"));
        box.getChildren().add(luoKappale("Moniin vanhoihin PDF-tiedostoihin on tallennettu tekstiä väärällä koodauksella (esim. Windows-1252 tai korruptoitunut UTF-8 mojibake: 'Ã¤' -> 'ä').\n" +
                "• Varoitusbanneri: Ohjelma varoittaa automaattisesti, jos merkistö on ei-standardi.\n" +
                "• 'Korjaa merkistö UTF:ksi': Purkaa automaattisesti kaksoiskoodaukset ja asettaa kentät tallennettavaksi standardina Unicode UTF-16BE (BOM FE FF) -muodossa.\n" +
                "• Pudotusvalikko: Voit myös pakottaa raakatavujen uudelleentulkinnan (Automaattinen, UTF-8, Windows-1252, ISO-8859-1, UTF-16BE)."));

        box.getChildren().add(luoValiotsikko("4. EPUB-metatiedot ja vastaavuuden etsintä"));
        box.getChildren().add(luoKappale("• EPUB-muokkaus: Tarkastele ja muokkaa kirjan OPF-metatietoja (Otsikko, Tekijä, Kustantaja, Aihe, Vuosi, Kieli).\n" +
                "• 'Etsi vastaavaa .epub dokumenttia': Etsii samasta kansiosta tai alikansioista vastaavaa EPUB-kirjaa ja avaa sen vertailuun PDF-tiedoston rinnalle."));

        box.getChildren().add(luoValiotsikko("5. Metatiedon etsintä tekstistä ja Tekoäly (AI)"));
        box.getChildren().add(luoKappale("• 'Etsi metadataa tekstistä': Louhii sääntöpohjaisesti dokumentin alun sivuilta monirivisen otsikon, tekijät, kustantajan, julkaisuvuoden ja kielen.\n" +
                "• 'Hae ai metadataa': Käyttää tekoälyä (Gemini API tai paikallinen Ollama) sisällön semanttiseen analysointiin.\n" +
                "• Valintadialogi: Ennen kenttien ylikirjoitusta avautuu dialogi, jossa voit valita rastittamalla (Kyllä / Ei) mitkä löydetyistä kentistä haluat ottaa käyttöön."));

        box.getChildren().add(luoValiotsikko("6. Tallennus ja turvallisuus"));
        box.getChildren().add(luoKappale("• Tallennuspainikkeet ovat aktiivisia vain silloin, kun kenttiin on tehty todellisia muutoksia.\n" +
                "• Tiedostoa vaihdettaessa ohjelma kysyy vahvistuksen, jos tallentamattomia muutoksia on kesken."));

        return box;
    }

    private static VBox luoOhjeEnglish() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        box.getChildren().add(luoOtsikko("modpdfmeta – PDF & EPUB Metadata Editor"));
        box.getChildren().add(luoKappale("This software is designed for inspecting, editing, repairing character encodings, and extracting metadata from PDF and EPUB documents using text mining and AI."));

        box.getChildren().add(luoValiotsikko("1. Opening and Browsing Files"));
        box.getChildren().add(luoKappale("• Open File: Choose an individual .pdf or .epub document.\n" +
                "• Select Directory: Loads all supported files from a folder into the list. Check 'Read subdirectories' to scan recursively.\n" +
                "• Previous / Next: Browse files sequentially.\n" +
                "• List Badges: [PDF] is highlighted in light red, and [EPUB] in green. Unsaved modifications are marked with an asterisk (*).\n" +
                "• Double-click (2x): Opens the selected file immediately in your operating system's default viewer."));

        box.getChildren().add(luoValiotsikko("2. Dual-Layer PDF Metadata: Low-Level vs. XML / XMP"));
        box.getChildren().add(luoKappale("PDF documents contain two distinct metadata storage areas:\n" +
                "1. Low-Level Metadata (Document Information Dictionary): Legacy dictionary keys (Title, Author, Subject, etc.).\n" +
                "2. XML-Level Metadata (Adobe XMP): Modern XML standard using Dublin Core and XMP schemas.\n" +
                "• Arrow Buttons: Transfer individual values across panels (► moves Low-level to XML, ◄ moves XML to Low-level).\n" +
                "• Batch Copy: Copy all metadata fields at once between panels."));

        box.getChildren().add(luoValiotsikko("3. Encoding & Mojibake Repair"));
        box.getChildren().add(luoKappale("Legacy PDF files frequently suffer from corrupt character sets (such as Windows-1252 or double-encoded UTF-8 mojibake: 'Ã¤' -> 'ä').\n" +
                "• Warning Banner: Notifies you whenever non-standard legacy encoding is detected.\n" +
                "• 'Fix Charset to UTF': Decodes mojibake and prepares fields to be saved as standard Unicode UTF-16BE (with BOM FE FF).\n" +
                "• Encoding Dropdown: Manually force raw byte decoding (Automatic, UTF-8, Windows-1252, ISO-8859-1, UTF-16BE)."));

        box.getChildren().add(luoValiotsikko("4. EPUB Metadata & Matching"));
        box.getChildren().add(luoKappale("• EPUB Editing: View and modify OPF metadata (Title, Creator, Publisher, Date, Language, etc.).\n" +
                "• 'Find Matching .epub': Searches the directory for a matching EPUB version and displays it side-by-side with your PDF for metadata transfer."));

        box.getChildren().add(luoValiotsikko("5. Text Mining and AI Metadata Extraction"));
        box.getChildren().add(luoKappale("• 'Find Metadata from Text': Rule-based NLP extraction of titles, authors, publishers, publication dates, and languages from the initial pages.\n" +
                "• 'Fetch AI Metadata': Uses LLMs (Google Gemini API or local Ollama) for intelligent document analysis.\n" +
                "• Review Dialog: Lets you selectively accept or reject extracted fields before populating the form."));

        box.getChildren().add(luoValiotsikko("6. Saving & Confirmation Safeguards"));
        box.getChildren().add(luoKappale("• Save buttons remain disabled until you make changes to metadata fields.\n" +
                "• If you navigate to another file with unsaved modifications, the app prompts you to save or discard your changes."));

        return box;
    }

    private static VBox luoOhjeSvenska() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        box.getChildren().add(luoOtsikko("modpdfmeta – PDF & EPUB Metadatasökare och redigerare"));
        box.getChildren().add(luoKappale("Detta program är utvecklat för att granska, redigera, reparera teckenkodning och utvinna metadata från PDF- och EPUB-dokument med hjälp av textanalys och AI."));

        box.getChildren().add(luoValiotsikko("1. Öppna och bläddra bland filer"));
        box.getChildren().add(luoKappale("• Öppna fil: Välj en enskild .pdf- eller .epub-fil.\n" +
                "• Välj mapp: Läser in alla filer från en mapp. Markera 'Läs även undermappar' för rekursiv sökning.\n" +
                "• Föregående / Nästa: Bläddra genom filerna i listan.\n" +
                "• Färgbrickor: [PDF] visas med ljusröd bakgrund och [EPUB] med grön. Osparade ändringar markeras med en stjärna (*).\n" +
                "• Dubbelklick: Öppnar filen direkt i operativsystemets standardprogram."));

        box.getChildren().add(luoValiotsikko("2. Tvålagers PDF-metadata"));
        box.getChildren().add(luoKappale("PDF-dokument har två separata metadatanivåer:\n" +
                "1. Låg nivå (Info Dictionary): Traditionell PDF-struktur (Titel, Författare, Ämne m.m.).\n" +
                "2. XML / XMP-nivå: Modern Adobe XMP-standard (Dublin Core).\n" +
                "• Piltangenter (► / ◄): Kopiera fält mellan nivåerna individuellt eller med gruppknapparna."));

        box.getChildren().add(luoValiotsikko("3. Teckenkodning och Mojibake-reparation"));
        box.getChildren().add(luoKappale("Äldre PDF-filer har ofta felaktig teckenkodning (t.ex. Windows-1252 eller felkodad UTF-8: 'Ã¤' -> 'ä').\n" +
                "• 'Korrigera teckenuppsättning till UTF': Återställer texten och sparar fälten som standard Unicode UTF-16BE med BOM.\n" +
                "• Rullgardinsmeny: Tvinga manuell tolkning vid behov."));

        box.getChildren().add(luoValiotsikko("4. Textutvinning och AI"));
        box.getChildren().add(luoKappale("• 'Hämta metadata från text': Extraherar titel, författare, förlag och årtal från början av dokumentet.\n" +
                "• 'Hämta AI-metadata': Använder Gemini API eller lokal Ollama för semantisk analys med ett bekräftelsedialogfönster."));

        return box;
    }

    private static VBox luoOhjeDeutsch() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        box.getChildren().add(luoOtsikko("modpdfmeta – PDF & EPUB Metadaten-Editor"));
        box.getChildren().add(luoKappale("Dieses Programm dient zur Inspektion, Bearbeitung, Zeichenkodierungs-Reparatur und Extraktion von Metadaten in PDF- und EPUB-Dateien mittels Textanalyse und KI."));

        box.getChildren().add(luoValiotsikko("1. Dateien öffnen und durchsuchen"));
        box.getChildren().add(luoKappale("• Datei öffnen: Einzelne .pdf- oder .epub-Datei auswählen.\n" +
                "• Ordner wählen: Lädt alle Dokumente aus einem Verzeichnis. Option 'Unterordner durchsuchen' für rekursive Suche.\n" +
                "• Zurück / Weiter: Blättern Sie durch die Dokumente.\n" +
                "• Farbkennzeichnung: [PDF] ist hellrot hinterlegt, [EPUB] grün. Ungespeicherte Änderungen werden mit (*) gekennzeichnet.\n" +
                "• Doppelklick: Öffnet das Dokument sofort im Standard-PDF/EPUB-Betrachter Ihres Systems."));

        box.getChildren().add(luoValiotsikko("2. PDF-Metadaten: Info-Dictionary vs. XML / XMP"));
        box.getChildren().add(luoKappale("PDF-Dokumente besitzen zwei getrennte Metadaten-Ebenen:\n" +
                "1. Low-Level (Info-Dictionary): Klassische Felder (Titel, Autor, Thema, Stichwörter usw.).\n" +
                "2. XML / XMP: Moderner Adobe XMP-Standard (Dublin Core).\n" +
                "• Pfeiltasten (► / ◄): Übertragen Sie einzelne Feldwerte zwischen den Ebenen."));

        box.getChildren().add(luoValiotsikko("3. Kodierungsfehler & Mojibake reparieren"));
        box.getChildren().add(luoKappale("Alte PDFs weisen oft defekte Umlaute auf (z. B. 'Ã¤' statt 'ä').\n" +
                "• Warnbanner: Erscheint automatisch bei nicht-standardisierten Kodierungen.\n" +
                "• 'Zeichensatz als UTF reparieren': Bereinigt Mojibake und speichert die Felder als standardisiertes Unicode UTF-16BE (BOM FE FF).\n" +
                "• Dropdown: Manuelle Byte-Dekodierung (UTF-8, Windows-1252, ISO-8859-1)."));

        box.getChildren().add(luoValiotsikko("4. Texterkennung und KI-Metadaten"));
        box.getChildren().add(luoKappale("• 'Metadaten aus Text suchen': Regelbasierte Erkennung von Buchtiteln, Autoren, Verlag und Jahr.\n" +
                "• 'KI-Metadaten abrufen': Nutzt Google Gemini API oder lokales Ollama für semantische Inhaltsanalyse inklusive Auswahldialog."));

        return box;
    }

    private static VBox luoOhjeFrancais() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        box.getChildren().add(luoOtsikko("modpdfmeta – Éditeur de métadonnées PDF & EPUB"));
        box.getChildren().add(luoKappale("Application moderne pour l'inspection, l'édition, la correction d'encodage et l'extraction de métadonnées des documents PDF et EPUB via l'analyse de texte et l'IA."));

        box.getChildren().add(luoValiotsikko("1. Ouverture et navigation dans les fichiers"));
        box.getChildren().add(luoKappale("• Ouvrir un fichier: Sélectionner un fichier .pdf ou .epub.\n" +
                "• Choisir un dossier: Charge tous les documents compatibles. Cochez 'Inclure les sous-dossiers' pour une recherche récursive.\n" +
                "• Précédent / Suivant: Naviguez d'un document à l'autre.\n" +
                "• Badges de liste: [PDF] est surligné en rose clair, [EPUB] en vert. Les modifications non enregistrées sont marquées d'un astérisque (*).\n" +
                "• Double-clic: Ouvre le document directement dans votre lecteur par défaut."));

        box.getChildren().add(luoValiotsikko("2. Métadonnées PDF à double couche"));
        box.getChildren().add(luoKappale("1. Niveau bas (Info Dictionary): Titre, Auteur, Sujet, etc.\n" +
                "2. Niveau XML / XMP: Norme moderne Adobe XMP (Dublin Core).\n" +
                "• Boutons fléchés (► / ◄): Copiez des valeurs individuelles entre les deux panneaux."));

        box.getChildren().add(luoValiotsikko("3. Correction de l'encodage et Mojibake"));
        box.getChildren().add(luoKappale("• Bannière d'avertissement: Détecte les encodages anciens (Windows-1252, ISO-8859).\n" +
                "• 'Corriger l'encodage en UTF': Répare les caractères corrompus (ex. 'Ã©' -> 'é') et enregistre en standard Unicode UTF-16BE (BOM FE FF).\n" +
                "• Menu déroulant pour forcer manuellement le jeu de caractères."));

        box.getChildren().add(luoValiotsikko("4. Extraction de métadonnées et IA"));
        box.getChildren().add(luoKappale("• 'Extraire métadonnées du texte': Extraction automatique des titres, auteurs et dates des premières pages.\n" +
                "• 'Extraire via IA': Utilise l'API Gemini ou Ollama local pour une analyse sémantique avancée avec boîte de dialogue de validation."));

        return box;
    }

    private static VBox luoOhjeEspanol() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        box.getChildren().add(luoOtsikko("modpdfmeta – Editor de metadatos PDF & EPUB"));
        box.getChildren().add(luoKappale("Aplicación de escritorio para inspeccionar, editar, corregir codificación de caracteres y extraer metadatos de documentos PDF y EPUB mediante análisis de texto e IA."));

        box.getChildren().add(luoValiotsikko("1. Apertura y navegación de archivos"));
        box.getChildren().add(luoKappale("• Abrir archivo: Seleccione un archivo .pdf o .epub individual.\n" +
                "• Seleccionar directorio: Carga todos los archivos compatibles de una carpeta. Marque 'Leer subdirectorios' para búsqueda recursiva.\n" +
                "• Anterior / Siguiente: Navegue ordenadamente por la lista.\n" +
                "• Etiquetas distintivas: [PDF] aparece en rojo claro y [EPUB] en verde. Los cambios sin guardar se indican con un asterisco (*).\n" +
                "• Doble clic: Abre el documento en el visor predeterminado del sistema operativo."));

        box.getChildren().add(luoValiotsikko("2. Metadatos PDF: Nivel bajo vs. XML / XMP"));
        box.getChildren().add(luoKappale("1. Nivel bajo (Document Information Dictionary): Título, Autor, Asunto, Palabras clave.\n" +
                "2. Nivel XML / XMP: Estándar moderno de Adobe XMP (Dublin Core).\n" +
                "• Botones de flecha (► / ◄): Copie campos individuales entre ambos paneles."));

        box.getChildren().add(luoValiotsikko("3. Corrección de codificación y Mojibake"));
        box.getChildren().add(luoKappale("• Alerta visual: Informa si el archivo utiliza una codificación heredada (Windows-1252 o ISO-8859).\n" +
                "• 'Corregir codificación a UTF': Elimina errores de doble codificación (ej. 'Ã±' -> 'ñ') y guarda en estándar Unicode UTF-16BE.\n" +
                "• Selector desplegable para forzar la reinterpretación de bytes sin procesar."));

        box.getChildren().add(luoValiotsikko("4. Minería de texto e IA"));
        box.getChildren().add(luoKappale("• 'Buscar metadatos en texto': Extrae títulos de varias líneas, autores y años desde las páginas iniciales.\n" +
                "• 'Obtener metadatos con IA': Emplea la API Gemini o Ollama local para un análisis semántico completo con diálogo de selección interactivo."));

        return box;
    }

    private static Label luoOtsikko(String teksti) {
        Label l = new Label(teksti);
        l.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #50fa7b;");
        return l;
    }

    private static Label luoValiotsikko(String teksti) {
        Label l = new Label(teksti);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #8be9fd; -fx-padding: 8 0 2 0;");
        return l;
    }

    private static Label luoKappale(String teksti) {
        Label l = new Label(teksti);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #f8f8f2; -fx-line-spacing: 2px;");
        return l;
    }
}
