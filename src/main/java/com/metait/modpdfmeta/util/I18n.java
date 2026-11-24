package com.metait.modpdfmeta.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Käyttöliittymän monikielisyyspalvelu (FI, EN, SV, DE, FR, ES).
 */
public class I18n {

    public enum Language {
        FI("fi", "Suomi"),
        EN("en", "English"),
        SV("sv", "Svenska"),
        DE("de", "Deutsch"),
        FR("fr", "Français"),
        ES("es", "Español");

        private final String code;
        private final String nativeName;

        Language(String code, String nativeName) {
            this.code = code;
            this.nativeName = nativeName;
        }

        public String getCode() { return code; }
        public String getNativeName() { return nativeName; }
        public String getDisplayLabel() { return code.toUpperCase(); }

        public static Language fromCode(String code) {
            if (code == null) return FI;
            for (Language l : values()) {
                if (l.name().equalsIgnoreCase(code) || l.code.equalsIgnoreCase(code)) {
                    return l;
                }
            }
            return FI;
        }
    }

    private static Language currentLanguage = Language.FI;
    private static final Map<String, Map<Language, String>> TRANSLATIONS = new HashMap<>();

    static {
        // Työkalupalkki
        add("btn.open_file", "Avaa tiedosto...", "Open file...", "Öppna fil...", "Datei öffnen...", "Ouvrir fichier...", "Abrir archivo...");
        add("btn.select_dir", "Valitse hakemisto...", "Select directory...", "Välj mapp...", "Ordner wählen...", "Choisir dossier...", "Seleccionar carpeta...");
        add("cb.subdirs", "Lue myös alihakemistot", "Read subdirectories", "Läs även undermappar", "Unterordner durchsuchen", "Lire les sous-dossiers", "Leer subdirectorios");
        add("btn.prev", "◄ Edellinen", "◄ Previous", "◄ Föregående", "◄ Zurück", "◄ Précédent", "◄ Anterior");
        add("btn.next", "Seuraava ►", "Next ►", "Nästa ►", "Weiter ►", "Suivant ►", "Siguiente ►");
        add("btn.rename", "Nimeä tiedosto uudelleen", "Rename file", "Byt namn på fil", "Datei umbenennen", "Renommer le fichier", "Renombrar archivo");
        add("btn.find_epub", "Etsi vastaavaa .epub dokumenttia", "Find matching .epub document", "Hitta matchande .epub-dokument", "Passendes .epub suchen", "Trouver .epub correspondant", "Buscar .epub correspondiente");
        add("btn.extract_text", "Etsi metadataa tekstistä", "Extract metadata from text", "Hämta metadata från text", "Metadaten aus Text suchen", "Extraire métadonnées du texte", "Extraer metadatos del texto");
        add("btn.extract_ai", "Hae ai metadataa", "Fetch AI metadata", "Hämta AI-metadata", "KI-Metadaten abrufen", "Extraire métadonnées via IA", "Obtener metadatos con IA");
        add("btn.help", "❓ Ohje", "❓ Help", "❓ Hjälp", "❓ Hilfe", "❓ Aide", "❓ Ayuda");

        // Yläpalkin infotekstit
        add("lbl.dir", "Avattu hakemisto: ", "Opened directory: ", "Öppnad mapp: ", "Geöffneter Ordner: ", "Dossier ouvert: ", "Directorio abierto: ");
        add("lbl.dir_none", "(ei valittu)", "(none selected)", "(inte vald)", "(nicht gewählt)", "(non sélectionné)", "(no seleccionado)");
        add("lbl.file", "Avattu tiedosto: ", "Opened file: ", "Öppnad fil: ", "Geöffnete Datei: ", "Fichier ouvert: ", "Archivo abierto: ");
        add("lbl.file_none", "(ei avattu)", "(not opened)", "(inte öppnad)", "(nicht geöffnet)", "(non ouvert)", "(no abierto)");

        // Tiedostolista
        add("lbl.files_title", "Tiedostot", "Files", "Filer", "Dateien", "Fichiers", "Archivos");

        // Kopiointipalkki
        add("btn.copy_xml_to_low", "◄ Kopioi XML:stä matalan tason kenttiin", "◄ Copy XML to Low-Level fields", "◄ Kopiera från XML till låg nivå", "◄ Von XML in Low-Level kopieren", "◄ Copier XML vers niveau bas", "◄ Copiar XML a nivel bajo");
        add("btn.copy_low_to_xml", "Kopioi matalalta tasolta XML-kenttiin ►", "Copy Low-Level to XML fields ►", "Kopiera från låg nivå till XML ►", "Von Low-Level in XML kopieren ►", "Copier niveau bas vers XML ►", "Copiar nivel bajo a XML ►");

        // Matalan tason paneeli
        add("panel.low_title", "Matalan tason metatiedot (COSDictionary)", "Low-Level Metadata (COSDictionary)", "Metadata på låg nivå (COSDictionary)", "Low-Level-Metadaten (COSDictionary)", "Métadonnées de bas niveau (COSDictionary)", "Metadatos de bajo nivel (COSDictionary)");
        add("lbl.charset", "Merkistö:", "Charset:", "Teckenkodning:", "Zeichensatz:", "Encodage:", "Codificación:");
        add("lbl.detected", "Tunnistettu: ", "Detected: ", "Identifierad: ", "Erkannt: ", "Détecté: ", "Detectado: ");
        add("btn.fix_charset", "Korjaa merkistö UTF:ksi", "Fix charset to UTF", "Korrigera teckenuppsättning till UTF", "Zeichensatz als UTF reparieren", "Corriger l'encodage en UTF", "Corregir codificación a UTF");
        add("btn.save_low", "Tallenna PDF (Matala taso)", "Save PDF (Low-Level)", "Spara PDF (Låg nivå)", "PDF speichern (Low-Level)", "Enregistrer PDF (Niveau bas)", "Guardar PDF (Nivel bajo)");

        // XML-paneeli
        add("panel.xmp_title", "XML- / XMP-metatiedot (Dublin Core)", "XML / XMP Metadata (Dublin Core)", "XML / XMP-metadata (Dublin Core)", "XML- / XMP-Metadaten (Dublin Core)", "Métadonnées XML / XMP (Dublin Core)", "Metadatos XML / XMP (Dublin Core)");
        add("lbl.std_unicode", "✓ Standardi Unicode", "✓ Standard Unicode", "✓ Standard Unicode", "✓ Standard-Unicode", "✓ Unicode standard", "✓ Unicode estándar");
        add("btn.save_xmp", "Tallenna PDF (XML-taso)", "Save PDF (XML-Level)", "Spara PDF (XML-nivå)", "PDF speichern (XML-Ebene)", "Enregistrer PDF (Niveau XML)", "Guardar PDF (Nivel XML)");

        // EPUB-paneeli
        add("panel.epub_title", "EPUB-metatiedot", "EPUB Metadata", "EPUB-metadata", "EPUB-Metadaten", "Métadonnées EPUB", "Metadatos EPUB");
        add("btn.close_epub", "Sulje vertailu", "Close comparison", "Stäng jämförelse", "Vergleich schließen", "Fermer comparaison", "Cerrar comparación");
        add("btn.epub_to_low", "Kopioi EPUB -> PDF (Matala)", "Copy EPUB -> PDF (Low)", "Kopiera EPUB -> PDF (Låg)", "Kopieren EPUB -> PDF (Low)", "Copier EPUB -> PDF (Bas)", "Copiar EPUB -> PDF (Bajo)");
        add("btn.epub_to_xmp", "Kopioi EPUB -> PDF (XML)", "Copy EPUB -> PDF (XML)", "Kopiera EPUB -> PDF (XML)", "Kopieren EPUB -> PDF (XML)", "Copier EPUB -> PDF (XML)", "Copiar EPUB -> PDF (XML)");
        add("btn.low_to_epub", "Kopioi PDF (Matala) -> EPUB", "Copy PDF (Low) -> EPUB", "Kopiera PDF (Låg) -> EPUB", "Kopieren PDF (Low) -> EPUB", "Copier PDF (Bas) -> EPUB", "Copiar PDF (Bajo) -> EPUB");
        add("btn.xmp_to_epub", "Kopioi PDF (XML) -> EPUB", "Copy PDF (XML) -> EPUB", "Kopiera PDF (XML) -> EPUB", "Kopieren PDF (XML) -> EPUB", "Copier PDF (XML) -> EPUB", "Copiar PDF (XML) -> EPUB");
        add("btn.save_epub", "Tallenna EPUB-tiedostoon", "Save to EPUB file", "Spara till EPUB-fil", "In EPUB-Datei speichern", "Enregistrer dans EPUB", "Guardar en archivo EPUB");

        // Kenttien nimet
        add("field.title", "Otsikko (Title):", "Title:", "Titel:", "Titel:", "Titre:", "Título:");
        add("field.author", "Tekijä (Author):", "Author:", "Författare:", "Autor:", "Auteur:", "Autor:");
        add("field.publisher", "Kustantaja:", "Publisher:", "Förlag:", "Verlag:", "Éditeur:", "Editorial:");
        add("field.subject", "Aihe (Subject):", "Subject:", "Ämne:", "Thema:", "Sujet:", "Asunto:");
        add("field.keywords", "Avainsanat:", "Keywords:", "Nyckelord:", "Stichwörter:", "Mots-clés:", "Palabras clave:");
        add("field.date", "Päiväys/Vuosi:", "Date/Year:", "Datum/År:", "Datum/Jahr:", "Date/Année:", "Fecha/Año:");
        add("field.language", "Kieli (Language):", "Language:", "Språk:", "Sprache:", "Langue:", "Idioma:");

        // Tilapalkki ja viestit
        add("status.ready", "Valmis.", "Ready.", "Klar.", "Bereit.", "Prêt.", "Listo.");
        add("status.saved", "Tiedot tallennettu onnistuneesti.", "Data saved successfully.", "Uppgifterna sparades.", "Daten erfolgreich gespeichert.", "Données enregistrées avec succès.", "Datos guardados con éxito.");
    }

    private static void add(String key, String fi, String en, String sv, String de, String fr, String es) {
        Map<Language, String> map = new HashMap<>();
        map.put(Language.FI, fi);
        map.put(Language.EN, en);
        map.put(Language.SV, sv);
        map.put(Language.DE, de);
        map.put(Language.FR, fr);
        map.put(Language.ES, es);
        TRANSLATIONS.put(key, map);
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(Language lang) {
        if (lang != null) {
            currentLanguage = lang;
        }
    }

    public static String get(String key) {
        return get(key, currentLanguage);
    }

    public static String get(String key, Language lang) {
        Map<Language, String> map = TRANSLATIONS.get(key);
        if (map == null) return key;
        String val = map.get(lang);
        if (val == null) {
            val = map.get(Language.EN);
        }
        return val != null ? val : key;
    }
}
