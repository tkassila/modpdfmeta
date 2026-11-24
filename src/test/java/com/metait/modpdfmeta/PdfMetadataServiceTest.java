package com.metait.modpdfmeta;

import com.metait.modpdfmeta.model.DocumentMetadata;
import com.metait.modpdfmeta.service.PdfMetadataService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PdfMetadataServiceTest {

    @Test
    public void testSaveAndReadLowLevelAndXmp(@TempDir Path tempDir) throws Exception {
        Path pdfPath = tempDir.resolve("test.pdf");

        // Luodaan pieni tyhjä PDF testausta varten
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfPath.toFile());
        }

        PdfMetadataService service = new PdfMetadataService();

        // 1. Tallennetaan matalan tason metatiedot
        DocumentMetadata lowMeta = new DocumentMetadata();
        lowMeta.setTitle("Matalan Tason Otsikko");
        lowMeta.setAuthor("Testi Kirjailija");
        lowMeta.setPublisher("Testi Kustantaja");
        lowMeta.setSubject("Testi Aihe");
        lowMeta.setKeywords("tag1, tag2");
        lowMeta.setDate("2024");
        lowMeta.setLanguage("fi");

        service.tallennaMatalanTasonMetatiedot(pdfPath, lowMeta);

        // Luetaan takaisin
        var res1 = service.lueMetatiedot(pdfPath);
        assertEquals("Matalan Tason Otsikko", res1.getLowLevelMetadata().getTitle());
        assertEquals("Testi Kirjailija", res1.getLowLevelMetadata().getAuthor());
        assertEquals("fi", res1.getLowLevelMetadata().getLanguage());
        assertEquals("UTF-16BE", res1.getLowLevelMetadata().getDetectedCharset());

        // 2. Tallennetaan XML/XMP metatiedot
        DocumentMetadata xmpMeta = new DocumentMetadata();
        xmpMeta.setTitle("XMP Otsikko");
        xmpMeta.setAuthor("XMP Kirjailija");
        xmpMeta.setPublisher("XMP Kustantaja");
        xmpMeta.setSubject("XMP Aihe");
        xmpMeta.setKeywords("xmp1, xmp2");
        xmpMeta.setDate("2025");
        xmpMeta.setLanguage("en");

        service.tallennaXmpMetatiedot(pdfPath, xmpMeta);

        // Luetaan takaisin
        var res2 = service.lueMetatiedot(pdfPath);
        assertEquals("XMP Otsikko", res2.getXmpMetadata().getTitle());
        assertEquals("XMP Kirjailija", res2.getXmpMetadata().getAuthor());
        assertEquals("en", res2.getXmpMetadata().getLanguage());
    }

    @Test
    public void testTextMetadataExtraction(@TempDir Path tempDir) throws Exception {
        Path pdfPath = tempDir.resolve("kirja.pdf");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (var cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Suuri Seikkailukirja");
                cs.newLineAtOffset(0, -20);
                cs.showText("Kirjoittanut: Mikael Karhu");
                cs.newLineAtOffset(0, -20);
                cs.showText("Kustantaja: Otava");
                cs.newLineAtOffset(0, -20);
                cs.showText("Copyright 2023");
                cs.endText();
            }
            doc.save(pdfPath.toFile());
        }

        var extractor = new com.metait.modpdfmeta.service.TextMetadataExtractorService();
        var fields = extractor.etsiMetatiedotTekstista(pdfPath, new DocumentMetadata());

        assertFalse(fields.isEmpty());

        var titleField = fields.stream().filter(f -> f.getKey().equals("title")).findFirst().orElseThrow();
        assertEquals("Suuri Seikkailukirja", titleField.getExtractedValue());

        var authorField = fields.stream().filter(f -> f.getKey().equals("author")).findFirst().orElseThrow();
        assertEquals("Mikael Karhu", authorField.getExtractedValue());

        var pubField = fields.stream().filter(f -> f.getKey().equals("publisher")).findFirst().orElseThrow();
        assertEquals("Otava", pubField.getExtractedValue());

        var dateField = fields.stream().filter(f -> f.getKey().equals("date")).findFirst().orElseThrow();
        assertEquals("2023", dateField.getExtractedValue());
    }

    @Test
    public void testMultilineTitleAndUnderlyingAuthors() {
        String teksti = """
                Suomen historian
                käännekohdat ja merkittävät tapahtumat
                Tuomas Forsberg, Jussi Huhtala
                Kustannusosakeyhtiö Otava
                Helsinki 2022
                ISBN 978-951-1-12345-6
                """;

        var extractor = new com.metait.modpdfmeta.service.TextMetadataExtractorService();
        var fields = extractor.louhiMetatiedot(teksti, new DocumentMetadata());

        var titleField = fields.stream().filter(f -> f.getKey().equals("title")).findFirst().orElseThrow();
        assertEquals("Suomen historian käännekohdat ja merkittävät tapahtumat", titleField.getExtractedValue());

        var authorField = fields.stream().filter(f -> f.getKey().equals("author")).findFirst().orElseThrow();
        assertEquals("Tuomas Forsberg, Jussi Huhtala", authorField.getExtractedValue());

        var pubField = fields.stream().filter(f -> f.getKey().equals("publisher")).findFirst().orElseThrow();
        assertEquals("Otava", pubField.getExtractedValue());

        var dateField = fields.stream().filter(f -> f.getKey().equals("date")).findFirst().orElseThrow();
        assertEquals("2022", dateField.getExtractedValue());
    }
}
