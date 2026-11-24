package com.metait.modpdfmeta;

import com.metait.modpdfmeta.service.AiMetadataExtractorService;
import com.metait.modpdfmeta.service.CharsetFixerService;
import com.metait.modpdfmeta.service.EpubMatcherService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ServicesTest {

    @Test
    public void testKorjaaMojibake() {
        // "Ã¤" -> "ä"
        String mojibake = "TÃ¤mÃ¤ on testi";
        String korjattu = CharsetFixerService.korjaaMojibake(mojibake);
        assertEquals("Tämä on testi", korjattu);
    }

    @Test
    public void testPuraXmlEntiteetit() {
        String xml = "Kirja &amp; tekij&#228;";
        String pura = CharsetFixerService.puraXmlEntiteetit(xml);
        assertEquals("Kirja & tekijä", pura);
    }

    @Test
    public void testPaatteleKieli() {
        assertEquals("fi", CharsetFixerService.paatteleKieliTekstista("Tämä kirja on suomeksi ja kertoo tarinan"));
        assertEquals("sv", CharsetFixerService.paatteleKieliTekstista("Boken är för alla och handlar om"));
        assertEquals("en", CharsetFixerService.paatteleKieliTekstista("The story of the world and nature"));
    }

    @Test
    public void testBaseNameExtraction() {
        assertEquals("dokumentti", EpubMatcherService.getBaseName("dokumentti.pdf"));
        assertEquals("arkisto.tar", EpubMatcherService.getBaseName("arkisto.tar.gz"));
        assertEquals("tiedosto", EpubMatcherService.getBaseName("tiedosto"));
    }

    @Test
    public void testBOMDetection() {
        byte[] utf8WithBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'H', 'e', 'i'};
        var res = CharsetFixerService.tunnistaJaDekoodaa(utf8WithBom, null);
        assertNotNull(res);
        assertEquals("Hei", res.getText());
        assertEquals("UTF-8", res.getCharset());
    }

    @Test
    public void testDekoodaaPakotetullaMerkistolla() {
        // "ä" Windows-1252:ssä on 0xE4 (tavuna -28)
        byte[] win1252Bytes = new byte[]{(byte) 0xE4};
        String resWin = CharsetFixerService.dekoodaaPakotetullaMerkistolla(win1252Bytes, "Windows-1252");
        assertEquals("ä", resWin);

        // "ä" UTF-8:ssa on 0xC3 0xA4
        byte[] utf8Bytes = "ä".getBytes(StandardCharsets.UTF_8);
        String resUtf = CharsetFixerService.dekoodaaPakotetullaMerkistolla(utf8Bytes, "UTF-8");
        assertEquals("ä", resUtf);
    }

    @Test
    public void testAiJsonParsing() {
        String json = "{\"title\": \"Seitsemän veljestä\", \"author\": \"Aleksis Kivi\", \"publisher\": \"SKS\", \"date\": \"1870\", \"language\": \"fi\"}";
        var map = AiMetadataExtractorService.puraYksinkertainenJson(json);
        assertEquals("Seitsemän veljestä", map.get("title"));
        assertEquals("Aleksis Kivi", map.get("author"));
        assertEquals("SKS", map.get("publisher"));
        assertEquals("1870", map.get("date"));
        assertEquals("fi", map.get("language"));
    }

    @Test
    public void testGeminiResponseParsing() {
        String geminiJson = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"{\\\"title\\\": \\\"Sinuhe egyptiläinen\\\", \\\"author\\\": \\\"Mika Waltari\\\", \\\"publisher\\\": \\\"WSOY\\\", \\\"date\\\": \\\"1945\\\"}\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        var map = AiMetadataExtractorService.puraGeminiResponseJson(geminiJson);
        assertEquals("Sinuhe egyptiläinen", map.get("title"));
        assertEquals("Mika Waltari", map.get("author"));
        assertEquals("WSOY", map.get("publisher"));
        assertEquals("1945", map.get("date"));
    }

    @Test
    public void testFileItemTypes() {
        com.metait.modpdfmeta.model.FileItem pdfItem = new com.metait.modpdfmeta.model.FileItem(java.nio.file.Path.of("testi.pdf"));
        assertTrue(pdfItem.isPdf());
        assertFalse(pdfItem.isEpub());
        assertEquals("[PDF] testi.pdf", pdfItem.toString());

        com.metait.modpdfmeta.model.FileItem epubItem = new com.metait.modpdfmeta.model.FileItem(java.nio.file.Path.of("kirja.epub"));
        assertFalse(epubItem.isPdf());
        assertTrue(epubItem.isEpub());
        assertEquals("[EPUB] kirja.epub", epubItem.toString());

        pdfItem.setDirty(true);
        assertEquals("* [PDF] testi.pdf", pdfItem.toString());
    }

    @Test
    public void testI18nLanguages() {
        assertEquals("Avaa tiedosto...", com.metait.modpdfmeta.util.I18n.get("btn.open_file", com.metait.modpdfmeta.util.I18n.Language.FI));
        assertEquals("Open file...", com.metait.modpdfmeta.util.I18n.get("btn.open_file", com.metait.modpdfmeta.util.I18n.Language.EN));
        assertEquals("Öppna fil...", com.metait.modpdfmeta.util.I18n.get("btn.open_file", com.metait.modpdfmeta.util.I18n.Language.SV));
        assertEquals("Datei öffnen...", com.metait.modpdfmeta.util.I18n.get("btn.open_file", com.metait.modpdfmeta.util.I18n.Language.DE));
        assertEquals("Ouvrir fichier...", com.metait.modpdfmeta.util.I18n.get("btn.open_file", com.metait.modpdfmeta.util.I18n.Language.FR));
        assertEquals("Abrir archivo...", com.metait.modpdfmeta.util.I18n.get("btn.open_file", com.metait.modpdfmeta.util.I18n.Language.ES));

        assertEquals(com.metait.modpdfmeta.util.I18n.Language.EN, com.metait.modpdfmeta.util.I18n.Language.fromCode("en"));
        assertEquals(com.metait.modpdfmeta.util.I18n.Language.FI, com.metait.modpdfmeta.util.I18n.Language.fromCode("fi"));
        assertEquals(com.metait.modpdfmeta.util.I18n.Language.SV, com.metait.modpdfmeta.util.I18n.Language.fromCode("sv"));
        assertEquals("FI", com.metait.modpdfmeta.util.I18n.Language.FI.getDisplayLabel());
        assertEquals("Suomi", com.metait.modpdfmeta.util.I18n.Language.FI.getNativeName());
    }
}
