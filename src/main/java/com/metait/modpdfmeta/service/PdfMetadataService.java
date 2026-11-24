package com.metait.modpdfmeta.service;

import com.metait.modpdfmeta.model.DocumentMetadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.xml.XmpSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF-tiedostojen metatietojen luku ja tallennus PDFBox 3:n avulla.
 */
public class PdfMetadataService {

    public static class PdfReadResult {
        private final DocumentMetadata lowLevelMetadata;
        private final DocumentMetadata xmpMetadata;

        public PdfReadResult(DocumentMetadata lowLevelMetadata, DocumentMetadata xmpMetadata) {
            this.lowLevelMetadata = lowLevelMetadata;
            this.xmpMetadata = xmpMetadata;
        }

        public DocumentMetadata getLowLevelMetadata() { return lowLevelMetadata; }
        public DocumentMetadata getXmpMetadata() { return xmpMetadata; }
    }

    /**
     * Lukee sekä matalan tason (COSDictionary) että XML/XMP-metatiedot.
     */
    public PdfReadResult lueMetatiedot(Path path) throws Exception {
        return lueMetatiedot(path, null);
    }

    public PdfReadResult lueMetatiedot(Path path, String forcedCharset) throws Exception {
        File file = path.toFile();
        try (PDDocument document = Loader.loadPDF(file)) {
            DocumentMetadata low = lueMatalanTasonMetatiedot(document, forcedCharset);
            DocumentMetadata xmp = lueXmpMetatiedot(document);
            return new PdfReadResult(low, xmp);
        }
    }

    /**
     * Lukee vain matalan tason metatiedot valitulla merkistöllä uudelleendekoodausta varten.
     */
    public DocumentMetadata lueMatalanTasonMetatiedot(Path path, String forcedCharset) throws Exception {
        File file = path.toFile();
        try (PDDocument document = Loader.loadPDF(file)) {
            return lueMatalanTasonMetatiedot(document, forcedCharset);
        }
    }

    /**
     * Lukee matalan tason metatiedot suoraan COSDictionary-raakatavuista ja purkaa merkistön.
     */
    private DocumentMetadata lueMatalanTasonMetatiedot(PDDocument document, String forcedCharset) {
        DocumentMetadata meta = new DocumentMetadata();
        PDDocumentInformation info = document.getDocumentInformation();
        if (info == null) {
            info = new PDDocumentInformation();
        }
        COSDictionary cosDict = info.getCOSObject();

        COSString titleItem = getCosString(cosDict, COSName.TITLE);
        COSString authorItem = getCosString(cosDict, COSName.AUTHOR);
        COSString creatorItem = getCosString(cosDict, COSName.CREATOR);
        if (creatorItem == null) creatorItem = getCosString(cosDict, COSName.PRODUCER);
        COSString kwItem = getCosString(cosDict, COSName.KEYWORDS);
        COSString subjectItem = getCosString(cosDict, COSName.SUBJECT);

        boolean isForced = forcedCharset != null && !forcedCharset.isEmpty() && !forcedCharset.equalsIgnoreCase("Automaattinen");
        String usedCharset;

        if (isForced) {
            usedCharset = forcedCharset;
            if (titleItem != null) meta.setTitle(CharsetFixerService.dekoodaaPakotetullaMerkistolla(titleItem.getBytes(), forcedCharset));
            if (authorItem != null) meta.setAuthor(CharsetFixerService.dekoodaaPakotetullaMerkistolla(authorItem.getBytes(), forcedCharset));
            if (creatorItem != null) meta.setPublisher(CharsetFixerService.dekoodaaPakotetullaMerkistolla(creatorItem.getBytes(), forcedCharset));
            if (kwItem != null) meta.setKeywords(CharsetFixerService.dekoodaaPakotetullaMerkistolla(kwItem.getBytes(), forcedCharset));
            if (subjectItem != null) meta.setSubject(CharsetFixerService.dekoodaaPakotetullaMerkistolla(subjectItem.getBytes(), forcedCharset));
        } else {
            // Kerätään yhdistetty tavunäyte automaattista merkistötunnistusta varten
            ByteArrayOutputStream combinedStream = new ByteArrayOutputStream();
            COSString[] items = new COSString[]{titleItem, authorItem, creatorItem, kwItem, subjectItem};
            for (COSString item : items) {
                if (item != null) {
                    byte[] b = item.getBytes();
                    if (b.length >= 2 && !((b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF)) {
                        combinedStream.write(b, 0, b.length);
                        combinedStream.write(' ');
                    }
                }
            }
            String commonDetectedCharset = CharsetFixerService.tunnistaYhteinenMerkisto(combinedStream.toByteArray());
            usedCharset = commonDetectedCharset;

            if (titleItem != null) {
                var r = CharsetFixerService.tunnistaJaDekoodaa(titleItem.getBytes(), commonDetectedCharset);
                if (r != null) {
                    meta.setTitle(r.getText());
                    if (usedCharset == null) usedCharset = r.getCharset();
                }
            }
            if (authorItem != null) {
                var r = CharsetFixerService.tunnistaJaDekoodaa(authorItem.getBytes(), commonDetectedCharset);
                if (r != null) {
                    meta.setAuthor(r.getText());
                    if (usedCharset == null) usedCharset = r.getCharset();
                }
            }
            if (creatorItem != null) {
                var r = CharsetFixerService.tunnistaJaDekoodaa(creatorItem.getBytes(), commonDetectedCharset);
                if (r != null) {
                    meta.setPublisher(r.getText());
                    if (usedCharset == null) usedCharset = r.getCharset();
                }
            }
            if (kwItem != null) {
                var r = CharsetFixerService.tunnistaJaDekoodaa(kwItem.getBytes(), commonDetectedCharset);
                if (r != null) {
                    meta.setKeywords(r.getText());
                }
            }
            if (subjectItem != null) {
                var r = CharsetFixerService.tunnistaJaDekoodaa(subjectItem.getBytes(), commonDetectedCharset);
                if (r != null) {
                    meta.setSubject(r.getText());
                }
            }
        }

        // Päivämäärä / julkaisuvuosi
        try {
            Calendar cal = info.getCreationDate();
            if (cal != null) {
                meta.setDate(String.valueOf(cal.get(Calendar.YEAR)));
            }
        } catch (Exception ignored) {}

        // Kieli
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            if (catalog != null && catalog.getLanguage() != null && !catalog.getLanguage().isEmpty()) {
                meta.setLanguage(catalog.getLanguage().trim().toLowerCase());
            }
        } catch (Exception ignored) {}

        if (meta.getLanguage().isEmpty()) {
            String combinedText = meta.getTitle() + " " + meta.getAuthor() + " " + meta.getKeywords();
            String ded = CharsetFixerService.paatteleKieliTekstista(combinedText);
            if (ded != null) {
                meta.setLanguage(ded);
            }
        }

        meta.setDetectedCharset(usedCharset != null ? usedCharset : "Tuntematon");
        return meta;
    }

    private COSString getCosString(COSDictionary dict, COSName key) {
        if (dict == null) return null;
        Object obj = dict.getDictionaryObject(key);
        if (obj instanceof COSString) {
            return (COSString) obj;
        }
        return null;
    }

    /**
     * Lukee XMP XML -metatiedot.
     */
    private DocumentMetadata lueXmpMetatiedot(PDDocument document) {
        DocumentMetadata meta = new DocumentMetadata();
        meta.setDetectedCharset("UTF-8 (XMP)");
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            if (catalog == null) return meta;
            PDMetadata pdm = catalog.getMetadata();
            if (pdm == null) return meta;
            String xmlText;
            try (InputStream is = pdm.createInputStream()) {
                xmlText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (xmlText.isEmpty()) return meta;

            // dc:title
            Matcher mTitle = Pattern.compile("<dc:title>[\\s\\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\\s\\S]*?</dc:title>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            if (mTitle.find()) {
                meta.setTitle(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(CharsetFixerService.puraXmlEntiteetit(mTitle.group(1)))));
            }

            // dc:creator
            Matcher mCreator = Pattern.compile("<dc:creator>[\\s\\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\\s\\S]*?</dc:creator>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            if (mCreator.find()) {
                meta.setAuthor(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(CharsetFixerService.puraXmlEntiteetit(mCreator.group(1)))));
            }

            // dc:publisher
            Matcher mPub = Pattern.compile("<dc:publisher>[\\s\\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\\s\\S]*?</dc:publisher>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            if (mPub.find()) {
                meta.setPublisher(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(CharsetFixerService.puraXmlEntiteetit(mPub.group(1)))));
            }

            // dc:date tai xmp:CreateDate
            Matcher mDate = Pattern.compile("<dc:date>[\\s\\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\\s\\S]*?</dc:date>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            if (!mDate.find()) {
                mDate = Pattern.compile("<xmp:CreateDate>(.*?)</xmp:CreateDate>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            }
            if (mDate.find()) {
                String d = mDate.group(1).trim();
                Matcher ym = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b").matcher(d);
                if (ym.find()) {
                    meta.setDate(ym.group(1));
                } else {
                    meta.setDate(d);
                }
            }

            // dc:subject
            Matcher mSub = Pattern.compile("<dc:subject>[\\s\\S]*?<rdf:Bag>([\\s\\S]*?)</rdf:Bag>[\\s\\S]*?</dc:subject>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            if (mSub.find()) {
                Matcher liMatcher = Pattern.compile("<rdf:li[^>]*>(.*?)</rdf:li>", Pattern.CASE_INSENSITIVE).matcher(mSub.group(1));
                List<String> tags = new ArrayList<>();
                while (liMatcher.find()) {
                    String tag = CharsetFixerService.puraXmlEntiteetit(liMatcher.group(1)).trim();
                    if (!tag.isEmpty()) tags.add(tag);
                }
                meta.setKeywords(String.join(", ", tags));
                meta.setSubject(meta.getKeywords());
            }

            // dc:language
            Matcher mLang = Pattern.compile("<dc:language>[\\s\\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\\s\\S]*?</dc:language>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
            if (mLang.find()) {
                String l = mLang.group(1).trim().toLowerCase();
                if (!l.equals("x-default")) {
                    meta.setLanguage(l);
                }
            } else {
                Matcher mLangDirect = Pattern.compile("<dc:language>([a-zA-Z_-]{2,10})</dc:language>", Pattern.CASE_INSENSITIVE).matcher(xmlText);
                if (mLangDirect.find()) {
                    meta.setLanguage(mLangDirect.group(1).trim().toLowerCase());
                } else {
                    Matcher mLangAttr = Pattern.compile("xml:lang=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE).matcher(xmlText);
                    if (mLangAttr.find()) {
                        String l = mLangAttr.group(1).trim().toLowerCase();
                        if (!l.equals("x-default")) {
                            meta.setLanguage(l);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return meta;
    }

    /**
     * Tallentaa matalan tason metatiedot PDF:n COSDictionary-rakenteeseen (standardissa UTF-16BE muodossa).
     * Käyttää väliaikaistiedostoa, jotta luettavaa PDF-tiedostoa ei ylikirjoiteta kesken lazy-latauksen.
     */
    public void tallennaMatalanTasonMetatiedot(Path path, DocumentMetadata meta) throws Exception {
        File file = path.toFile();
        Path tempFile = luoValiaikaistiedosto(path);

        try {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDDocumentInformation info = document.getDocumentInformation();
                if (info == null) {
                    info = new PDDocumentInformation();
                    document.setDocumentInformation(info);
                }
                COSDictionary dict = info.getCOSObject();

                asetaUnicodeKentta(dict, COSName.TITLE, meta.getTitle());
                asetaUnicodeKentta(dict, COSName.AUTHOR, meta.getAuthor());
                asetaUnicodeKentta(dict, COSName.SUBJECT, meta.getSubject());
                asetaUnicodeKentta(dict, COSName.KEYWORDS, meta.getKeywords());
                asetaUnicodeKentta(dict, COSName.CREATOR, meta.getPublisher());

                if (!meta.getDate().isEmpty()) {
                    Calendar cal = Calendar.getInstance();
                    try {
                        String digits = meta.getDate().replaceAll("[^0-9]", "");
                        if (digits.length() >= 4) {
                            int yr = Integer.parseInt(digits.substring(0, 4));
                            cal.set(Calendar.YEAR, yr);
                            info.setCreationDate(cal);
                        }
                    } catch (Exception ignored) {}
                }

                if (!meta.getLanguage().isEmpty()) {
                    PDDocumentCatalog catalog = document.getDocumentCatalog();
                    if (catalog != null) {
                        catalog.setLanguage(meta.getLanguage());
                    }
                }

                document.setDocumentInformation(info);
                document.save(tempFile.toFile());
            }

            // Korvataan alkuperäinen tiedosto vasta kun dokumentti on suljettu ja vapautettu
            java.nio.file.Files.move(tempFile, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    private void asetaUnicodeKentta(COSDictionary dict, COSName key, String value) {
        if (value == null || value.trim().isEmpty()) {
            dict.removeItem(key);
        } else {
            dict.setItem(key, luoUnicodeCOSString(value.trim()));
        }
    }

    public static COSString luoUnicodeCOSString(String text) {
        if (text == null) return null;
        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16BE);
        byte[] withBom = new byte[utf16.length + 2];
        withBom[0] = (byte) 0xFE;
        withBom[1] = (byte) 0xFF;
        System.arraycopy(utf16, 0, withBom, 2, utf16.length);
        return new COSString(withBom);
    }

    /**
     * Tallentaa XML / XMP -metatiedot PDF-dokumentin XMP-virtaan.
     * Käyttää väliaikaistiedostoa, jotta luettavaa PDF-tiedostoa ei ylikirjoiteta kesken lazy-latauksen.
     */
    public void tallennaXmpMetatiedot(Path path, DocumentMetadata meta) throws Exception {
        File file = path.toFile();
        Path tempFile = luoValiaikaistiedosto(path);

        try {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDDocumentCatalog catalog = document.getDocumentCatalog();
                if (catalog == null) return;

                XMPMetadata xmp = XMPMetadata.createXMPMetadata();
                DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
                dc.setTitle(meta.getTitle());
                if (!meta.getAuthor().isEmpty()) {
                    dc.addCreator(meta.getAuthor());
                }
                if (!meta.getPublisher().isEmpty()) {
                    dc.addPublisher(meta.getPublisher());
                }
                if (!meta.getSubject().isEmpty()) {
                    dc.setDescription(meta.getSubject());
                }
                if (!meta.getKeywords().isEmpty()) {
                    String[] parts = meta.getKeywords().split("[,;]");
                    for (String part : parts) {
                        String trim = part.trim();
                        if (!trim.isEmpty()) dc.addSubject(trim);
                    }
                }
                if (!meta.getDate().isEmpty()) {
                    Calendar cal = Calendar.getInstance();
                    try {
                        String digits = meta.getDate().replaceAll("[^0-9]", "");
                        if (digits.length() >= 4) {
                            int yr = Integer.parseInt(digits.substring(0, 4));
                            cal.set(Calendar.YEAR, yr);
                        }
                    } catch (Exception ignored) {}
                    dc.addDate(cal);
                }
                if (!meta.getLanguage().isEmpty()) {
                    dc.addLanguage(meta.getLanguage());
                }

                AdobePDFSchema pdf = xmp.createAndAddAdobePDFSchema();
                pdf.setKeywords(meta.getKeywords());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new XmpSerializer().serialize(xmp, baos, true);

                PDMetadata pdMetadata = new PDMetadata(document);
                pdMetadata.importXMPMetadata(baos.toByteArray());
                catalog.setMetadata(pdMetadata);

                document.save(tempFile.toFile());
            }

            // Korvataan alkuperäinen tiedosto vasta kun dokumentti on suljettu ja vapautettu
            java.nio.file.Files.move(tempFile, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    private Path luoValiaikaistiedosto(Path originalPath) throws java.io.IOException {
        Path parent = originalPath.getParent();
        if (parent != null && java.nio.file.Files.isWritable(parent)) {
            return java.nio.file.Files.createTempFile(parent, "modpdfmeta_pdf_", ".tmp");
        }
        return java.nio.file.Files.createTempFile("modpdfmeta_pdf_", ".tmp");
    }
}
