package com.metait.modpdfmeta.service;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merkkijonojen ja raakatavujen käsittely, merkistön tunnistus ja Mojibake-korjaukset.
 */
public class CharsetFixerService {

    public static class DecodeResult {
        private final String text;
        private final String charset;

        public DecodeResult(String text, String charset) {
            this.text = text;
            this.charset = charset;
        }

        public String getText() { return text; }
        public String getCharset() { return charset; }
    }

    /**
     * Tunnistaa ja purkaa kaksoiskoodatun mojibake-tekstin (esim. UTF-8 tulkittuna Windows-1252:ksi).
     * Esimerkki: "Ã¤" -> "ä", "Ã¶" -> "ö", "Ã¥" -> "å", "â€“" -> "–".
     */
    public static String korjaaMojibake(String teksti) {
        if (teksti == null || teksti.isEmpty()) {
            return "";
        }
        if (!teksti.contains("Ã") && !teksti.contains("â€") && !teksti.contains("Â")) {
            return teksti;
        }
        try {
            Charset win1252 = Charset.forName("windows-1252");
            byte[] bytes = teksti.getBytes(win1252);
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            String decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString();
            if (!decoded.isEmpty() && !decoded.contains("\uFFFD")) {
                return decoded;
            }
        } catch (Exception ignored) {
        }
        return teksti;
    }

    /**
     * Tunnistaa raakatavujen merkistön juniversalchardet-kirjaston ja varamekanismien avulla.
     */
    public static DecodeResult tunnistaJaDekoodaa(byte[] rawBytes, String fallbackCharsetName) {
        if (rawBytes == null || rawBytes.length == 0) {
            return null;
        }

        // 1. Tarkistetaan alkaako teksti UTF-16BE BOM:lla (FE FF)
        if (rawBytes.length >= 2 && (rawBytes[0] & 0xFF) == 0xFE && (rawBytes[1] & 0xFF) == 0xFF) {
            String text = new String(rawBytes, 2, rawBytes.length - 2, StandardCharsets.UTF_16BE);
            return new DecodeResult(siistiTeksti(korjaaMojibake(text)), "UTF-16BE");
        }

        // 2. Tarkistetaan alkaako teksti UTF-8 BOM:lla (EF BB BF)
        if (rawBytes.length >= 3 &&
                (rawBytes[0] & 0xFF) == 0xEF &&
                (rawBytes[1] & 0xFF) == 0xBB &&
                (rawBytes[2] & 0xFF) == 0xBF) {
            String text = new String(rawBytes, 3, rawBytes.length - 3, StandardCharsets.UTF_8);
            return new DecodeResult(siistiTeksti(korjaaMojibake(text)), "UTF-8");
        }

        // 3. Tarkistetaan onko teksti UTF-16 ilman BOM:ia (vuorotellen nollatavut)
        if (rawBytes.length >= 4 &&
                ((rawBytes[0] == 0 && rawBytes[2] == 0) || (rawBytes[1] == 0 && rawBytes[3] == 0))) {
            boolean isBE = (rawBytes[0] == 0);
            Charset charset = isBE ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
            String text = new String(rawBytes, charset);
            return new DecodeResult(siistiTeksti(korjaaMojibake(text)), charset.name());
        }

        // 4. Jos dokumentille on jo aiemmin tunnistettu yhteinen merkistö yhdistetystä näytteestä:
        if (fallbackCharsetName != null && !fallbackCharsetName.isEmpty()) {
            try {
                Charset charset = Charset.forName(fallbackCharsetName);
                String text = new String(rawBytes, charset);
                return new DecodeResult(siistiTeksti(korjaaMojibake(text)), fallbackCharsetName);
            } catch (Exception ignored) {
            }
        }

        // 5. Käytetään juniversalchardet UniversalDetector -kirjastoa
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(rawBytes, 0, rawBytes.length);
        detector.dataEnd();
        String detectedCharsetName = detector.getDetectedCharset();
        detector.reset();

        if (detectedCharsetName != null) {
            try {
                Charset charset = Charset.forName(detectedCharsetName);
                String text = new String(rawBytes, charset);
                return new DecodeResult(siistiTeksti(korjaaMojibake(text)), detectedCharsetName);
            } catch (Exception ignored) {
            }
        }

        // 6. Varavaihtoehdot: Yritetään ensin validia UTF-8:aa
        try {
            var utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            String text = utf8Decoder.decode(ByteBuffer.wrap(rawBytes)).toString();
            return new DecodeResult(siistiTeksti(korjaaMojibake(text)), "UTF-8");
        } catch (Exception ignored) {
        }

        // Windows-1252 / ISO-8859-1
        try {
            Charset win1252 = Charset.forName("windows-1252");
            String text = new String(rawBytes, win1252);
            return new DecodeResult(siistiTeksti(korjaaMojibake(text)), "WINDOWS-1252");
        } catch (Exception ignored) {
        }

        return new DecodeResult(siistiTeksti(korjaaMojibake(new String(rawBytes, StandardCharsets.ISO_8859_1))), "ISO-8859-1");
    }

    /**
     * Purkaa raakatavut käyttäjän eksplisiittisesti valitsemalla merkistöllä.
     */
    public static String dekoodaaPakotetullaMerkistolla(byte[] bytes, String charsetName) {
        if (bytes == null || bytes.length == 0) return "";
        try {
            Charset cs = Charset.forName(charsetName);
            String text = new String(bytes, cs);
            return siistiTeksti(korjaaMojibake(text));
        } catch (Exception e) {
            return siistiTeksti(new String(bytes, StandardCharsets.ISO_8859_1));
        }
    }

    public static String siistiTeksti(String text) {
        if (text == null) return "";
        return text.replace("\u0000", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Etsii yhdistetystä tavunäytteestä todennäköisimmän merkistön juniversalchardetilla.
     */
    public static String tunnistaYhteinenMerkisto(byte[] sampleBytes) {
        if (sampleBytes == null || sampleBytes.length == 0) return null;
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(sampleBytes, 0, sampleBytes.length);
        detector.dataEnd();
        String detected = detector.getDetectedCharset();
        detector.reset();
        return detected;
    }

    public static String puraXmlEntiteetit(String text) {
        if (text == null) return "";
        String s = text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");

        Pattern p = Pattern.compile("&#(\\d+);");
        Matcher m = p.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            try {
                int code = Integer.parseInt(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (Exception e) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String paatteleKieliTekstista(String teksti) {
        if (teksti == null || teksti.isEmpty()) return null;
        String matala = teksti.toLowerCase();
        boolean hasSkandi = matala.contains("ä") || matala.contains("ö");
        boolean hasAring = matala.contains("å");

        if (hasAring || matala.contains(" och ") || matala.contains(" för ") || matala.contains(" av ") || matala.contains(" är ")) {
            return "sv";
        }
        if (hasSkandi && (matala.contains(" ja ") || matala.contains(" on ") || matala.contains(" ei ") || matala.contains(" kuin "))) {
            return "fi";
        }
        if (matala.contains(" the ") || matala.contains(" and ") || matala.contains(" of ") || matala.contains(" with ")) {
            return "en";
        }
        if (hasSkandi) {
            return "fi";
        }
        return null;
    }
}
