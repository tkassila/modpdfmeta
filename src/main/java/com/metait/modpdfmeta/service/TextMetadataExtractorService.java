package com.metait.modpdfmeta.service;

import com.metait.modpdfmeta.model.DocumentMetadata;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Palvelu, joka louhii puuttuvia metatietoja PDF- tai EPUB-tiedoston tekstisisällöstä.
 * Tukee monirivisiä nimikkeitä sekä otsikon alla olevia tekijätietoja (Etunimi Sukunimi).
 */
public class TextMetadataExtractorService {

    public static class ExtractedField {
        private final String key;          // "title", "author", "publisher", "date", "language", "keywords"
        private final String label;        // Käyttäjälle näkyvä nimi
        private String extractedValue;
        private final String currentValue;
        private boolean selected;

        public ExtractedField(String key, String label, String extractedValue, String currentValue) {
            this.key = key;
            this.label = label;
            this.extractedValue = extractedValue != null ? extractedValue.trim() : "";
            this.currentValue = currentValue != null ? currentValue.trim() : "";
            // Oletuksena valittu, jos uusi arvo löytyi ja se on eri kuin nykyinen
            this.selected = !this.extractedValue.isEmpty() && !this.extractedValue.equalsIgnoreCase(this.currentValue);
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getExtractedValue() { return extractedValue; }
        public void setExtractedValue(String extractedValue) { this.extractedValue = extractedValue; }
        public String getCurrentValue() { return currentValue; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }

    private static final List<String> TUNNETUT_KUSTANTAJAT = Arrays.asList(
            "Otava", "WSOY", "Tammi", "Karisto", "Gummerus", "Gaudeamus",
            "Into Kustannus", "Into", "Atena", "Bazar", "Docendo", "Minerva",
            "Siltala", "Teos", "Vastapaino", "Schildts", "Söderströms", "Johnny Kniga",
            "HarperCollins", "Penguin", "O'Reilly", "Springer", "Wiley", "Routledge",
            "Oxford University Press", "Cambridge University Press", "Suomalaisen Kirjallisuuden Seura",
            "SKS", "Like", "Art House", "Kirjapaja", "Aviador", "Aula & Co"
    );

    private static final List<String> EI_NIMIA = Arrays.asList(
            "sisällys", "sisällysluettelo", "esipuhe", "johdanto", "alkusanat",
            "kustantaja", "kustannus", "painos", "painopaikka", "ilmestynyt",
            "copyright", "contents", "preface", "introduction", "table of contents",
            "helsinki", "turku", "tampere", "oulu", "jyväskylä", "stockholm", "london", "new york"
    );

    /**
     * Etsii metatietoja tiedoston ensimmäisiltä sivuilta / luvuista.
     */
    public List<ExtractedField> etsiMetatiedotTekstista(Path path, DocumentMetadata nykyiset) throws Exception {
        String teksti = lueAlunTeksti(path);
        return louhiMetatiedot(teksti, nykyiset);
    }

    public List<ExtractedField> louhiMetatiedot(String teksti, DocumentMetadata nykyiset) {
        List<ExtractedField> kentat = new ArrayList<>();
        if (teksti == null || teksti.trim().isEmpty()) {
            return kentat;
        }

        String[] rivit = teksti.split("\\r?\\n");
        List<String> siistitRivit = new ArrayList<>();
        for (String r : rivit) {
            String s = r.trim();
            if (!s.isEmpty()) {
                siistitRivit.add(s);
            }
        }

        // 1. Kustantaja
        String kustantaja = etsiKustantaja(teksti, siistitRivit);

        // 2. Painovuosi / Julkaisuvuosi
        String vuosi = etsiVuosi(teksti);

        // 3 & 4. Nimiölehden jäsennys: Monirivinen nimike ja sen alla olevat tekijät
        TitleAndAuthors ta = jasennaNimiölehti(siistitRivit, kustantaja, teksti);

        kentat.add(new ExtractedField("title", "Nimike (Title)", ta.title, nykyiset.getTitle()));
        kentat.add(new ExtractedField("author", "Tekijä (Author)", ta.authors, nykyiset.getAuthor()));
        kentat.add(new ExtractedField("publisher", "Kustantaja", kustantaja, nykyiset.getPublisher()));
        kentat.add(new ExtractedField("date", "Painovuosi / Aika", vuosi, nykyiset.getDate()));

        // 5. Kieli
        String kieli = CharsetFixerService.paatteleKieliTekstista(teksti.substring(0, Math.min(2000, teksti.length())));
        kentat.add(new ExtractedField("language", "Kieli (Language)", kieli != null ? kieli : "", nykyiset.getLanguage()));

        // 6. ISBN (voidaan liittää aiheeseen/avainsanoihin)
        String isbn = etsiIsbn(teksti);
        String nykyisetKw = nykyiset.getKeywords();
        String ehdotusKw = nykyisetKw;
        if (!isbn.isEmpty() && (nykyisetKw == null || !nykyisetKw.contains(isbn))) {
            ehdotusKw = (nykyisetKw == null || nykyisetKw.isEmpty()) ? isbn : nykyisetKw + ", " + isbn;
        }
        kentat.add(new ExtractedField("keywords", "Avainsanat / ISBN", ehdotusKw, nykyisetKw));

        return kentat;
    }

    private static class TitleAndAuthors {
        String title = "";
        String authors = "";
    }

    /**
     * Jäsentelee nimiölehden: tunnistaa monirivisen nimikkeen ja heti sen alla olevat tekijät.
     */
    private TitleAndAuthors jasennaNimiölehti(List<String> rivit, String kustantaja, String kokoTeksti) {
        TitleAndAuthors res = new TitleAndAuthors();

        // 1. Etsitään ensin mahdollinen eksplisiittinen tekijä etuliitteellä
        String eksplisiittinenTekija = etsiTekijaEtuliitteella(kokoTeksti);

        // 2. Etsitään otsikon alkukohta (ensimmäinen ei-metadata rivi)
        int titleStartIdx = -1;
        for (int i = 0; i < Math.min(10, rivit.size()); i++) {
            String r = rivit.get(i);
            if (onOhitettavaRivi(r, kustantaja)) continue;
            titleStartIdx = i;
            break;
        }

        if (titleStartIdx != -1) {
            // Kerätään monirivinen nimike
            StringBuilder sbTitle = new StringBuilder();
            sbTitle.append(rivit.get(titleStartIdx));
            int nextIdx = titleStartIdx + 1;

            while (nextIdx < Math.min(titleStartIdx + 4, rivit.size())) {
                String nextLine = rivit.get(nextIdx);
                if (onOhitettavaRivi(nextLine, kustantaja)) break;

                // Tarkistetaan, onko seuraava rivi tekijä
                if (nayttaaTekijanimelta(nextLine, kustantaja)) {
                    break;
                }

                // Tarkistetaan, onko kyseessä otsikon jatkorivi
                if (onOtsikonJatko(rivit.get(nextIdx - 1), nextLine)) {
                    if (rivit.get(nextIdx - 1).endsWith(":")) {
                        sbTitle.append(" ").append(nextLine);
                    } else if (nextLine.startsWith("-") || nextLine.startsWith("–")) {
                        sbTitle.append(" ").append(nextLine);
                    } else {
                        sbTitle.append(" ").append(nextLine);
                    }
                    nextIdx++;
                } else {
                    break;
                }
            }

            res.title = siistiOtsikko(sbTitle.toString());

            // 3. Tekijöiden etsintä pääotsikon alapuolelta
            List<String> loydetytTekijat = new ArrayList<>();
            for (int j = nextIdx; j < Math.min(nextIdx + 4, rivit.size()); j++) {
                String r = rivit.get(j);
                if (onOhitettavaRivi(r, kustantaja)) break;

                if (nayttaaTekijanimelta(r, kustantaja)) {
                    // Puhdistetaan mahdolliset "Kirjoittanut:" tms. etuliitteet
                    String puhdistettu = r.replaceFirst("(?i)^(?:kirjoittanut|tekijä|tekijät|kirjoittajat|author|authors|by)[:\\s]+", "").trim();
                    if (!puhdistettu.isEmpty() && !loydetytTekijat.contains(puhdistettu)) {
                        loydetytTekijat.add(puhdistettu);
                    }
                } else {
                    // Jos rivi ei enää näytä nimeltä, tekijäosio päättyi
                    break;
                }
            }

            if (!loydetytTekijat.isEmpty()) {
                res.authors = String.join(", ", loydetytTekijat);
            }
        }

        // Jos otsikon alta ei löytynyt tekijää, käytetään etuliitteellistä tai copyright-tekijää
        if (res.authors.isEmpty()) {
            if (!eksplisiittinenTekija.isEmpty()) {
                res.authors = eksplisiittinenTekija;
            } else {
                res.authors = etsiTekijaCopyrightista(kokoTeksti);
            }
        }

        return res;
    }

    private boolean onOtsikonJatko(String edellinen, String seuraava) {
        if (seuraava.isEmpty() || seuraava.length() > 90) return false;

        // Jos edellinen loppuu kaksoispisteeseen, pilkkuun tai yhdysviivaan
        if (edellinen.endsWith(":") || edellinen.endsWith(",") || edellinen.endsWith("-") || edellinen.endsWith("–")) {
            return true;
        }

        // Jos seuraava alkaa pienellä kirjaimella (esim. "ja...", "tai...", "and...", "of...")
        char firstChar = seuraava.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            return true;
        }

        // Jos seuraava alkaa rinnastuskonjunktiolla
        String lower = seuraava.toLowerCase();
        if (lower.startsWith("ja ") || lower.startsWith("tai ") || lower.startsWith("sekä ") ||
            lower.startsWith("eli ") || lower.startsWith("and ") || lower.startsWith("or ") ||
            lower.startsWith("in ") || lower.startsWith("for ") || lower.startsWith("of ") ||
            lower.startsWith("with ") || lower.startsWith("osa ") || lower.startsWith("vol ") ||
            lower.startsWith("part ")) {
            return true;
        }

        // Jos edellinen rivi on lyhyt (alle 45 merkkiä) ja seuraavakin on kohtuullisen lyhyt eikä näytä tekijältä
        if (edellinen.length() < 45 && seuraava.length() < 55) {
            // Esim. pääotsikko rivillä 1 ja alaotsikko rivillä 2
            return true;
        }

        return false;
    }

    private boolean nayttaaTekijanimelta(String r, String kustantaja) {
        if (r == null || r.trim().isEmpty()) return false;
        String s = r.trim();

        // Etuliitteet
        if (s.matches("(?i)^(?:kirjoittanut|tekijä|tekijät|kirjoittajat|author|authors|by)[:\\s]+.+")) {
            return true;
        }

        // Karsitaan numerot, erikoismerkit
        if (s.matches(".*[0-9!?;#@/*=<>].*")) return false;
        if (s.length() < 4 || s.length() > 80) return false;

        // Ei saa olla tunnettu kustantaja tai paikkakunta
        if (kustantaja != null && !kustantaja.isEmpty() && s.toLowerCase().contains(kustantaja.toLowerCase())) {
            return false;
        }
        for (String eiNimi : EI_NIMIA) {
            if (s.equalsIgnoreCase(eiNimi)) return false;
        }

        // Tarkistetaan nimen muoto: "Etunimi Sukunimi" tai "Etunimi T. Sukunimi" tai "Matti Meikäläinen, Maija Mallikas"
        // Sallitaan 2-4 osaa per nimi, iso alkukirjain
        String namePart = "[A-ZÄÖÅ][a-zäöå]+(?:-[A-ZÄÖÅ][a-zäöå]+)?(?:\\s+[A-ZÄÖÅ]\\.?)?(?:\\s+[A-ZÄÖÅ][a-zäöå]+(?:-[A-ZÄÖÅ][a-zäöå]+)?){1,3}";
        String fullPattern = "^" + namePart + "(?:(?:,[\\s]+|[\\s]+ja[\\s]+|[\\s]+and[\\s]+|[\\s]+&[\\s]+)" + namePart + ")*$";

        return s.matches(fullPattern);
    }

    private boolean onOhitettavaRivi(String r, String kustantaja) {
        if (r == null || r.trim().isEmpty()) return true;
        String s = r.trim().toLowerCase();

        if (s.length() < 2) return true;
        if (s.matches("^[\\d\\s.,-]+$")) return true; // Pelkät numerot / sivunumerot
        if (s.startsWith("isbn") || s.startsWith("issn")) return true;
        if (s.contains("copyright") || s.contains("©")) return true;
        if (s.contains("kaikki oikeudet pidätetään") || s.contains("all rights reserved")) return true;
        if (s.contains("painos") || s.contains("painettu") || s.contains("printed")) return true;
        if (s.equals("pdf") || s.equals("epub")) return true;

        if (kustantaja != null && !kustantaja.isEmpty() && s.equals(kustantaja.toLowerCase())) {
            return true;
        }
        for (String eiNimi : EI_NIMIA) {
            if (s.equals(eiNimi)) return true;
        }

        return false;
    }

    private String siistiOtsikko(String title) {
        if (title == null) return "";
        return title.replaceAll("\\s+", " ")
                .replaceAll("(?i)\\s*-\\s*", " - ")
                .trim();
    }

    public String lueAlunTeksti(Path path) throws Exception {
        File file = path.toFile();
        String name = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(Math.min(8, document.getNumberOfPages()));
                return stripper.getText(document);
            }
        } else if (name.endsWith(".epub")) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Book book = (new EpubReader()).readEpub(fis);
                StringBuilder sb = new StringBuilder();
                int luvut = 0;
                for (SpineReference ref : book.getSpine().getSpineReferences()) {
                    Resource res = ref.getResource();
                    if (res != null) {
                        byte[] data = res.getData();
                        if (data != null) {
                            String html = new String(data, StandardCharsets.UTF_8);
                            sb.append(stripHtml(html)).append("\n");
                            luvut++;
                            if (luvut >= 6) break;
                        }
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">");
    }

    private String etsiVuosi(String teksti) {
        // 1. Copyright- tai painosmerkinnän yhteydessä
        Pattern p1 = Pattern.compile("(?i)(?:©|copyright|painos|ilmestynyt|julkaistu|printed|published)[^\\d\\r\\n]{0,35}\\b(19\\d\\d|20\\d\\d)\\b");
        Matcher m1 = p1.matcher(teksti);
        if (m1.find()) {
            return m1.group(1);
        }

        // 2. ISBN:n lähellä oleva vuosiluku (ennen tai jälkeen)
        Pattern p2 = Pattern.compile("(?i)(?:isbn[\\s\\S]{0,80}\\b(19\\d\\d|20\\d\\d)\\b|\\b(19\\d\\d|20\\d\\d)\\b[\\s\\S]{0,80}isbn)");
        Matcher m2 = p2.matcher(teksti);
        if (m2.find()) {
            return m2.group(1) != null ? m2.group(1) : m2.group(2);
        }

        // 3. Paikkakunnan tai kustantajan yhteydessä (esim. Helsinki 2022)
        Pattern p3 = Pattern.compile("(?i)(?:helsinki|turku|tampere|oulu|jyväskylä|porvoo|juva|stockholm|london)[^\\d\\r\\n]{0,25}\\b(19\\d\\d|20\\d\\d)\\b");
        Matcher m3 = p3.matcher(teksti);
        if (m3.find()) {
            return m3.group(1);
        }

        // 4. Standalone vuosiluku rivillä
        Pattern p4 = Pattern.compile("(?m)^[^\\d\\r\\n]*\\b(19\\d\\d|20\\d\\d)\\b[^\\d\\r\\n]*$");
        Matcher m4 = p4.matcher(teksti);
        if (m4.find()) {
            return m4.group(1);
        }

        return "";
    }

    private String etsiKustantaja(String teksti, List<String> rivit) {
        // Etsitään tunnettuja kustantajia
        for (String rivi : rivit) {
            for (String tunnettu : TUNNETUT_KUSTANTAJAT) {
                if (rivi.matches("(?i).*\\b" + Pattern.quote(tunnettu) + "\\b.*")) {
                    return tunnettu;
                }
            }
        }

        // Etsitään eksplisiittisellä etuliitteellä
        Pattern p = Pattern.compile("(?i)(?:kustantaja|kustannusosakeyhtiö|kustannus|publisher|published by)[:\\s]+([A-ZÄÖÅ][^\\r\\n,.]+)");
        Matcher m = p.matcher(teksti);
        if (m.find()) {
            return m.group(1).trim();
        }

        return "";
    }

    private String etsiTekijaEtuliitteella(String teksti) {
        Pattern p = Pattern.compile("(?i)(?:kirjoittanut|tekijä|tekijät|kirjoittajat|author|by)[:\\s]+([A-ZÄÖÅ][a-zäöå]+(?:[ \\t]+[A-ZÄÖÅ][a-zäöå]+){1,3})");
        Matcher m = p.matcher(teksti);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private String etsiTekijaCopyrightista(String teksti) {
        Pattern pCopy = Pattern.compile("(?i)(?:©|copyright)[ \\t]*(?:\\d{4})?[ \\t]*([A-ZÄÖÅ][a-zäöå]+(?:[ \\t]+[A-ZÄÖÅ][a-zäöå]+){1,3})");
        Matcher mCopy = pCopy.matcher(teksti);
        if (mCopy.find()) {
            String name = mCopy.group(1).trim();
            boolean onKustantaja = TUNNETUT_KUSTANTAJAT.stream().anyMatch(k -> k.equalsIgnoreCase(name));
            if (!onKustantaja) {
                return name;
            }
        }
        return "";
    }

    private String etsiIsbn(String teksti) {
        Pattern p = Pattern.compile("(?i)ISBN(?:-1[03])?:?\\s*([0-9Xx\\s-]{10,18})");
        Matcher m = p.matcher(teksti);
        if (m.find()) {
            String raw = m.group(1).trim();
            if (raw.replaceAll("[^0-9Xx]", "").length() >= 10) {
                return "ISBN " + raw;
            }
        }
        return "";
    }
}
