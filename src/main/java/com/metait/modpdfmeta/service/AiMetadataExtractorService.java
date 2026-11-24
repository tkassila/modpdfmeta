package com.metait.modpdfmeta.service;

import com.metait.modpdfmeta.controller.PdfEpubMetaWriterController;
import com.metait.modpdfmeta.model.DocumentMetadata;
import com.metait.modpdfmeta.service.TextMetadataExtractorService.ExtractedField;
import com.metait.modpdfmeta.util.DialogHelper;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Palvelu, joka louhii ja erottelee metatietoja tekoälyn (LLM / Gemini API / Ollama / NLP) avulla.
 */
public class AiMetadataExtractorService {

    public static final String PREF_GEMINI_KEY = "gemini_api_key";
    private final Preferences prefs = Preferences.userNodeForPackage(PdfEpubMetaWriterController.class);
    private final TextMetadataExtractorService textExtractor = new TextMetadataExtractorService();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Etsii metatietoja tekoälyä hyödyntäen tiedoston alkutekstistä.
     */
    public List<ExtractedField> etsiAiMetatiedot(Window owner, Path path, DocumentMetadata nykyiset) throws Exception {
        String teksti = textExtractor.lueAlunTeksti(path);
        if (teksti == null || teksti.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String syoteTeksti = teksti.length() > 4000 ? teksti.substring(0, 4000) : teksti;

        String apiKey = haeApiKey(owner);
        Map<String, String> aiTulokset = null;

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                aiTulokset = kutsuGeminiApi(apiKey.trim(), syoteTeksti);
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("API_KEY_INVALID") || e.getMessage().contains("API key not valid"))) {
                    prefs.remove(PREF_GEMINI_KEY);
                }
                DialogHelper.naytaVirhe("AI API -virhe", "Gemini API -kutsu epäonnistui: " + e.getMessage() + "\n\nKäytetään paikallista AI-louhintaa.");
            }
        } else if (onkoOllamaSaatavilla()) {
            try {
                aiTulokset = kutsuOllamaApi(syoteTeksti);
            } catch (Exception ignored) {}
        }

        if (aiTulokset == null || aiTulokset.isEmpty()) {
            // Varamekanismi: kehittynyt semanttinen NLP / sääntöpohjainen AI-louhinta
            return textExtractor.louhiMetatiedot(teksti, nykyiset);
        }

        // Muodostetaan kentät AI:n JSON-tuloksista
        List<ExtractedField> kentat = new ArrayList<>();
        lisaaKentta(kentat, "title", "Otsikko (Title)", aiTulokset.get("title"), nykyiset.getTitle());
        lisaaKentta(kentat, "author", "Tekijä (Author)", aiTulokset.get("author"), nykyiset.getAuthor());
        lisaaKentta(kentat, "publisher", "Kustantaja", aiTulokset.get("publisher"), nykyiset.getPublisher());
        lisaaKentta(kentat, "subject", "Aihe (Subject)", aiTulokset.get("subject"), nykyiset.getSubject());
        lisaaKentta(kentat, "keywords", "Avainsanat", aiTulokset.get("keywords"), nykyiset.getKeywords());
        lisaaKentta(kentat, "date", "Päiväys/Vuosi", aiTulokset.get("date"), nykyiset.getDate());
        lisaaKentta(kentat, "language", "Kieli (Language)", aiTulokset.get("language"), nykyiset.getLanguage());

        return kentat;
    }

    private void lisaaKentta(List<ExtractedField> list, String key, String label, String aiVal, String currVal) {
        if (aiVal != null && !aiVal.trim().isEmpty()) {
            list.add(new ExtractedField(key, label, aiVal.trim(), currVal));
        }
    }

    private String haeApiKey(Window owner) {
        String key = prefs.get(PREF_GEMINI_KEY, null);
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // Kysytään kerran käyttäjältä ja tallennetaan
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Gemini AI API-avain");
        dialog.setHeaderText("Anna Gemini API -avain tekoälypohjaista metatiedon hakua varten.\n(Jätä tyhjäksi käyttääksesi paikallista sääntöpohjaista AI-louhintaa)");
        dialog.setContentText("API-avain:");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String uusiKey = result.get().trim();
            prefs.put(PREF_GEMINI_KEY, uusiKey);
            return uusiKey;
        }
        return null;
    }

    private boolean onkoOllamaSaatavilla() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .timeout(Duration.ofMillis(800))
                    .GET()
                    .build();
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> kutsuOllamaApi(String sampleText) throws Exception {
        String prompt = luoKehote(sampleText);
        String body = "{\"model\":\"llama3\",\"prompt\":\"" + escapeJson(prompt) + "\",\"format\":\"json\",\"stream\":false}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() == 200) {
            // Ollama palauttaa {"response":"{...}"}
            Pattern p = Pattern.compile("\"response\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*,\\s*\"done\"", Pattern.MULTILINE);
            Matcher m = p.matcher(resp.body());
            if (m.find()) {
                String inner = m.group(1).replace("\\\"", "\"").replace("\\n", "\n");
                return puraYksinkertainenJson(inner);
            }
        }
        return Collections.emptyMap();
    }

    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash",
            "gemini-flash-latest"
    );

    private Map<String, String> kutsuGeminiApi(String apiKey, String sampleText) throws Exception {
        String prompt = luoKehote(sampleText);
        String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}],\"generationConfig\":{\"responseMimeType\":\"application/json\"}}";

        Exception lastException = null;
        for (String model : GEMINI_MODELS) {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(20))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return puraGeminiResponseJson(response.body());
                } else {
                    lastException = new RuntimeException("HTTP " + response.statusCode() + " (" + model + "): " + response.body());
                }
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        return Collections.emptyMap();
    }

    private String luoKehote(String sampleText) {
        return "Olet kirjojen ja dokumenttien metatietoasiantuntija. " +
                "Analysoi seuraava dokumentin tai kirjan alkuteksti ja poimi metatiedot JSON-muodossa:\\n" +
                "- title: kirjan virallinen nimi ja mahdollinen alaotsikko\\n" +
                "- author: tekijät / kirjoittajat pilkulla eroteltuna (muotoa Etunimi Sukunimi)\\n" +
                "- publisher: kustantaja tai julkaisija\\n" +
                "- date: julkaisuvuosi (esim. 2024)\\n" +
                "- language: kielikoodi (esim. fi, en, sv)\\n" +
                "- subject: lyhyt kuvaus tai aihepiiri\\n" +
                "- keywords: aiheeseen liittyvät avainsanat pilkulla eroteltuna\\n\\n" +
                "Vastaa VAIN validilla JSON-objektilla, jossa on avaimet: title, author, publisher, date, language, subject, keywords.\\n\\n" +
                "Teksti:\\n" + sampleText;
    }

    public static String escapeJson(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static Map<String, String> puraGeminiResponseJson(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return Collections.emptyMap();

        Pattern p = Pattern.compile("\"text\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*\\n?\\s*}\\s*\\n?\\s*\\]\\s*\\n?\\s*}\\s*\\n?\\s*\\}", Pattern.MULTILINE);
        Matcher m = p.matcher(responseBody);
        String innerJson = responseBody;
        if (m.find()) {
            innerJson = m.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return puraYksinkertainenJson(innerJson);
    }

    public static Map<String, String> puraYksinkertainenJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) return map;
        String[] keys = {"title", "author", "publisher", "date", "language", "subject", "keywords"};
        for (String k : keys) {
            Pattern p = Pattern.compile("\"" + k + "\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(json);
            if (m.find()) {
                String val = m.group(1)
                        .replace("\\\"", "\"")
                        .replace("\\n", " ")
                        .replace("\\r", "")
                        .replace("\\t", " ")
                        .trim();
                map.put(k, val);
            }
        }
        return map;
    }
}
