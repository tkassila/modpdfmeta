package com.metait.fepubreader

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSString
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import org.mozilla.universalchardet.UniversalDetector
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object PdfMetadataFixer {

    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context)
            initialized = true
        }
    }

    /**
     * Tunnistaa ja purkaa kaksoiskoodatun mojibake-tekstin (esim. UTF-8 tulkittuna Windows-1252:ksi).
     * Esimerkki: "Ã¤" -> "ä", "Ã¶" -> "ö", "Ã¥" -> "å", "â€“" -> "–".
     */
    fun korjaaMojibake(teksti: String): String {
        if (!teksti.contains("Ã") && !teksti.contains("â€") && !teksti.contains("Â")) {
            return teksti
        }
        try {
            val win1252 = Charset.forName("windows-1252")
            val bytes = teksti.toByteArray(win1252)
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            if (decoded.isNotEmpty() && !decoded.contains("\uFFFD")) {
                return decoded
            }
        } catch (_: Exception) {}
        return teksti
    }

    /**
     * Tunnistaa raakatavujen merkistön juniversalchardet-kirjaston ja
     * suomalaisille aineistoille viritetyn varamekanismin avulla.
     */
    fun tunnistaJaDekoodaa(rawBytes: ByteArray?, fallbackCharsetName: String? = null): Pair<String, String>? {
        if (rawBytes == null || rawBytes.isEmpty()) return null

        // 1. Tarkistetaan alkaako teksti UTF-16BE BOM:lla (FE FF)
        if (rawBytes.size >= 2 && rawBytes[0] == 0xFE.toByte() && rawBytes[1] == 0xFF.toByte()) {
            val text = String(rawBytes, 2, rawBytes.size - 2, StandardCharsets.UTF_16BE)
            return Pair(siistiTeksti(korjaaMojibake(text)), "UTF-16BE")
        }

        // 2. Tarkistetaan alkaako teksti UTF-8 BOM:lla (EF BB BF)
        if (rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte()
        ) {
            val text = String(rawBytes, 3, rawBytes.size - 3, StandardCharsets.UTF_8)
            return Pair(siistiTeksti(korjaaMojibake(text)), "UTF-8")
        }

        // 3. Tarkistetaan onko teksti UTF-16 ilman BOM:ia (vuorotellen nollatavut)
        if (rawBytes.size >= 4 &&
            ((rawBytes[0] == 0.toByte() && rawBytes[2] == 0.toByte()) ||
             (rawBytes[1] == 0.toByte() && rawBytes[3] == 0.toByte()))
        ) {
            val isBE = rawBytes[0] == 0.toByte()
            val charset = if (isBE) StandardCharsets.UTF_16BE else StandardCharsets.UTF_16LE
            val text = String(rawBytes, charset)
            return Pair(siistiTeksti(korjaaMojibake(text)), charset.name())
        }

        // 4. Jos koko dokumentille on jo tunnistettu yhteinen merkistö yhdistetystä näytteestä:
        if (fallbackCharsetName != null && fallbackCharsetName.isNotEmpty()) {
            try {
                val charset = Charset.forName(fallbackCharsetName)
                val text = String(rawBytes, charset)
                return Pair(siistiTeksti(korjaaMojibake(text)), fallbackCharsetName)
            } catch (_: Exception) {}
        }

        // 5. Käytetään juniversalchardet UniversalDetector -kirjastoa
        val detector = UniversalDetector(null)
        detector.handleData(rawBytes, 0, rawBytes.size)
        detector.dataEnd()
        val detectedCharsetName = detector.detectedCharset
        detector.reset()

        if (detectedCharsetName != null) {
            try {
                val charset = Charset.forName(detectedCharsetName)
                val text = String(rawBytes, charset)
                return Pair(siistiTeksti(korjaaMojibake(text)), detectedCharsetName)
            } catch (_: Exception) {}
        }

        // 6. Varavaihtoehdot lyhyille teksteille:
        // Yritetään ensin validia UTF-8:aa
        try {
            val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val byteBuffer = ByteBuffer.wrap(rawBytes)
            val charBuffer = utf8Decoder.decode(byteBuffer)
            val text = charBuffer.toString()
            return Pair(siistiTeksti(korjaaMojibake(text)), "UTF-8 (fallback)")
        } catch (_: Exception) {}

        // Yritetään Windows-1252 / ISO-8859-1 (suomalaisille PDF-tiedostoille erittäin yleinen)
        try {
            val win1252 = Charset.forName("windows-1252")
            val text = String(rawBytes, win1252)
            return Pair(siistiTeksti(korjaaMojibake(text)), "WINDOWS-1252 (fallback)")
        } catch (_: Exception) {}

        return Pair(siistiTeksti(korjaaMojibake(String(rawBytes, StandardCharsets.ISO_8859_1))), "ISO-8859-1")
    }

    private fun siistiTeksti(text: String): String {
        return text.replace("\u0000", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Etsii yhdistetystä tavunäytteestä todennäköisimmän merkistön juniversalchardetilla.
     */
    private fun tunnistaYhteinenMerkisto(sampleBytes: ByteArray): String? {
        if (sampleBytes.isEmpty()) return null
        val detector = UniversalDetector(null)
        detector.handleData(sampleBytes, 0, sampleBytes.size)
        detector.dataEnd()
        val detected = detector.detectedCharset
        detector.reset()
        return detected
    }

    /**
     * Lukee XMP-metadatasta (Dublin Core ja Adobe XML) kentät kevyesti ilman raskaita lisäkirjastoja.
     */
    private fun lueXmpMetatiedot(document: PDDocument): Map<String, String> {
        val tulokset = mutableMapOf<String, String>()
        try {
            val catalog = document.documentCatalog ?: return tulokset
            val metadata = catalog.metadata ?: return tulokset
            val inputStream = metadata.createInputStream() ?: return tulokset
            val xmlText = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (xmlText.isEmpty()) return tulokset

            // dc:title
            val titleMatch = Regex("""<dc:title>[\s\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\s\S]*?</dc:title>""", RegexOption.IGNORE_CASE)
                .find(xmlText)
            if (titleMatch != null) {
                tulokset["title"] = siistiTeksti(korjaaMojibake(puraXmlEntiteetit(titleMatch.groupValues[1])))
            }

            // dc:creator (kirjailija)
            val creatorMatch = Regex("""<dc:creator>[\s\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\s\S]*?</dc:creator>""", RegexOption.IGNORE_CASE)
                .find(xmlText)
            if (creatorMatch != null) {
                tulokset["author"] = siistiTeksti(korjaaMojibake(puraXmlEntiteetit(creatorMatch.groupValues[1])))
            }

            // dc:publisher
            val pubMatch = Regex("""<dc:publisher>[\s\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\s\S]*?</dc:publisher>""", RegexOption.IGNORE_CASE)
                .find(xmlText)
            if (pubMatch != null) {
                tulokset["publisher"] = siistiTeksti(korjaaMojibake(puraXmlEntiteetit(pubMatch.groupValues[1])))
            }

            // dc:date
            val dateMatch = Regex("""<dc:date>[\s\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\s\S]*?</dc:date>""", RegexOption.IGNORE_CASE)
                .find(xmlText) ?: Regex("""<xmp:CreateDate>(.*?)</xmp:CreateDate>""", RegexOption.IGNORE_CASE).find(xmlText)
            if (dateMatch != null) {
                tulokset["date"] = dateMatch.groupValues[1].trim()
            }

            // dc:subject (tagit / avainsanat)
            val subjectMatches = Regex("""<dc:subject>[\s\S]*?<rdf:Bag>([\s\S]*?)</rdf:Bag>[\s\S]*?</dc:subject>""", RegexOption.IGNORE_CASE)
                .find(xmlText)
            if (subjectMatches != null) {
                val lis = Regex("""<rdf:li[^>]*>(.*?)</rdf:li>""", RegexOption.IGNORE_CASE)
                    .findAll(subjectMatches.groupValues[1])
                    .map { puraXmlEntiteetit(it.groupValues[1]).trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                if (lis.isNotEmpty()) {
                    tulokset["keywords"] = lis
                }
            }

            // dc:language tai xmp kielitieto
            val langMatch = Regex("""<dc:language>[\s\S]*?<rdf:li[^>]*>(.*?)</rdf:li>[\s\S]*?</dc:language>""", RegexOption.IGNORE_CASE)
                .find(xmlText) ?: Regex("""xml:lang=["'](.*?)["']""", RegexOption.IGNORE_CASE).find(xmlText)
            if (langMatch != null) {
                val rawLang = langMatch.groupValues[1].trim().lowercase()
                if (rawLang.isNotEmpty() && rawLang != "x-default") {
                    tulokset["language"] = rawLang
                }
            }
        } catch (_: Exception) {}
        return tulokset
    }

    private fun puraXmlEntiteetit(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("""&#(\d+);""")) {
                val code = it.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else it.value
            }
    }

    /**
     * Päättelee kielen koodin (esim. fi, sv, en) tekstin skandien tai yleisten sanojen perusteella.
     */
    private fun paatteleKieliTekstista(teksti: String): String? {
        val matala = teksti.lowercase()
        val hasSkandi = matala.contains('ä') || matala.contains('ö')
        val hasAring = matala.contains('å')

        if (hasAring && (matala.contains(" och ") || matala.contains(" för ") || matala.contains(" av "))) {
            return "sv"
        }
        if (hasSkandi && (matala.contains(" ja ") || matala.contains(" on ") || matala.contains(" ei ") || matala.contains(" kuin "))) {
            return "fi"
        }
        if (matala.contains(" the ") || matala.contains(" and ") || matala.contains(" of ") || matala.contains(" with ")) {
            return "en"
        }
        if (hasSkandi) {
            return "fi"
        }
        return null
    }

    /**
     * Suorittaa kehittyneen PDFBox- ja juniversalchardet-metatiedon erikoiskyselyn:
     * 1. Kerää raakatavut COSDictionarysta (COSName.TITLE, COSName.AUTHOR, CREATOR, KEYWORDS, SUBJECT).
     * 2. Yhdistää tavut yhdeksi näytteeksi, jotta juniversalchardet tunnistaa merkistön myös lyhyille teksteille.
     * 3. Lukee ja yhdistää XMP-metatiedot (UTF-8 Dublin Core).
     * 4. Tunnistaa kirjan kielen (/Lang-kenttä tai XMP dc:language).
     * 5. Purkaa mahdolliset mojibake-virheet.
     * 6. Jos tallennus on pyydetty, kirjoittaa korjatut tiedot standardissa UTF-16BE muodossa takaisin.
     */
    fun suoritaMetaErikoiskysely(
        context: Context,
        tiedostoPolku: String,
        tallennaTakaisinPdfTiedostoon: Boolean = false
    ): Map<String, Any?> {
        init(context)

        val file = File(tiedostoPolku)
        if (!file.exists() || !file.canRead()) {
            return mapOf(
                "success" to false,
                "error" to "Tiedostoa ei löydy tai sitä ei voi lukea: $tiedostoPolku"
            )
        }

        var document: PDDocument? = null
        try {
            document = PDDocument.load(file)
            val info: PDDocumentInformation = document.documentInformation ?: PDDocumentInformation()
            val cosDict: COSDictionary = info.cosObject

            // 1. Kerätään raakatavut COSDictionarysta
            val titleItem = cosDict.getItem(COSName.TITLE) as? COSString
            val authorItem = cosDict.getItem(COSName.AUTHOR) as? COSString
            val creatorItem = (cosDict.getItem(COSName.CREATOR) ?: cosDict.getItem(COSName.PRODUCER)) as? COSString
            val kwItem = cosDict.getItem(COSName.KEYWORDS) as? COSString
            val subjectItem = cosDict.getItem(COSName.SUBJECT) as? COSString

            // 2. Yhdistetty tavunäyte tilastollista tunnistinta varten (ohittaa BOM-tavut)
            val combinedStream = ByteArrayOutputStream()
            listOf(titleItem, authorItem, creatorItem, kwItem, subjectItem).forEach { item ->
                if (item != null) {
                    val bytes = item.bytes
                    // Lisätään vain jos ei ole UTF-16BE BOM:ia (FE FF)
                    if (bytes.size >= 2 && !(bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())) {
                        combinedStream.write(bytes)
                        combinedStream.write(' '.code)
                    }
                }
            }
            val combinedSample = combinedStream.toByteArray()
            val commonDetectedCharset = tunnistaYhteinenMerkisto(combinedSample)

            var detectedTitle: String? = null
            var detectedAuthor: String? = null
            var detectedPublisher: String? = null
            var detectedKeywords: String? = null
            var usedCharset: String? = commonDetectedCharset

            // Puretaan otsikko
            if (titleItem != null) {
                val pair = tunnistaJaDekoodaa(titleItem.bytes, commonDetectedCharset)
                if (pair != null) {
                    detectedTitle = pair.first
                    if (usedCharset == null) usedCharset = pair.second
                }
            }

            // Puretaan kirjailija
            if (authorItem != null) {
                val pair = tunnistaJaDekoodaa(authorItem.bytes, commonDetectedCharset)
                if (pair != null) {
                    detectedAuthor = pair.first
                    if (usedCharset == null) usedCharset = pair.second
                }
            }

            // Puretaan kustantaja / luoja
            if (creatorItem != null) {
                val pair = tunnistaJaDekoodaa(creatorItem.bytes, commonDetectedCharset)
                if (pair != null) {
                    detectedPublisher = pair.first
                    if (usedCharset == null) usedCharset = pair.second
                }
            }

            // Puretaan avainsanat
            if (kwItem != null) {
                val pair = tunnistaJaDekoodaa(kwItem.bytes, commonDetectedCharset)
                if (pair != null) {
                    detectedKeywords = pair.first
                }
            }

            // 3. Luetaan XMP-metatiedot ja täydennetään puuttuvat kentät
            val xmpData = lueXmpMetatiedot(document)
            if (detectedTitle.isNullOrEmpty() && !xmpData["title"].isNullOrEmpty()) {
                detectedTitle = xmpData["title"]
                if (usedCharset == null) usedCharset = "UTF-8 (XMP)"
            }
            if (detectedAuthor.isNullOrEmpty() && !xmpData["author"].isNullOrEmpty()) {
                detectedAuthor = xmpData["author"]
            }
            if (detectedPublisher.isNullOrEmpty() && !xmpData["publisher"].isNullOrEmpty()) {
                detectedPublisher = xmpData["publisher"]
            }
            if (detectedKeywords.isNullOrEmpty() && !xmpData["keywords"].isNullOrEmpty()) {
                detectedKeywords = xmpData["keywords"]
            }

            // 4. Kielitiedon selvittäminen (/Lang-kenttä, XMP tai tekstisisältö)
            var detectedLanguage: String? = null
            try {
                val catalog = document.documentCatalog
                if (catalog != null) {
                    val langStr = catalog.language
                    if (!langStr.isNullOrEmpty()) {
                        detectedLanguage = langStr.trim().lowercase()
                    }
                }
            } catch (_: Exception) {}

            if (detectedLanguage.isNullOrEmpty()) {
                detectedLanguage = xmpData["language"]
            }
            if (detectedLanguage.isNullOrEmpty()) {
                val yhdistettyTeksti = listOfNotNull(detectedTitle, detectedAuthor, detectedKeywords).joinToString(" ")
                detectedLanguage = paatteleKieliTekstista(yhdistettyTeksti)
            }

            // Siistitään kielikoodi (esim. fi-fi -> fi)
            if (!detectedLanguage.isNullOrEmpty()) {
                val osat = detectedLanguage.split(Regex("""[-_]"""))
                if (osat.isNotEmpty() && osat[0].length == 2) {
                    detectedLanguage = osat[0]
                }
            }

            // 5. Julkaisuvuosi / Päivämäärä
            var publishYear = 0
            var publishDateText: String? = null
            try {
                val cal = info.creationDate
                if (cal != null) {
                    publishYear = cal.get(java.util.Calendar.YEAR)
                    publishDateText = publishYear.toString()
                }
            } catch (_: Exception) {}

            if (publishYear == 0 && !xmpData["date"].isNullOrEmpty()) {
                val dateStr = xmpData["date"]!!
                val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(dateStr)
                if (yearMatch != null) {
                    publishYear = yearMatch.value.toInt()
                    publishDateText = publishYear.toString()
                } else {
                    publishDateText = dateStr
                }
            }

            // 6. Tallennus takaisin PDF-tiedostoon UTF-16BE-standardina
            var tallennettuPdfään = false
            if (tallennaTakaisinPdfTiedostoon) {
                try {
                    var muuttunut = false
                    if (!detectedTitle.isNullOrEmpty()) {
                        info.title = detectedTitle
                        muuttunut = true
                    }
                    if (!detectedAuthor.isNullOrEmpty()) {
                        info.author = detectedAuthor
                        muuttunut = true
                    }
                    if (!detectedPublisher.isNullOrEmpty()) {
                        info.creator = detectedPublisher
                        muuttunut = true
                    }
                    if (!detectedKeywords.isNullOrEmpty()) {
                        info.keywords = detectedKeywords
                        muuttunut = true
                    }

                    if (muuttunut) {
                        document.documentInformation = info
                        document.save(file)
                        tallennettuPdfään = true
                    }
                } catch (_: Exception) {}
            }

            val tagsList = detectedKeywords?.split(Regex("""[,;]"""))
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            return mapOf(
                "success" to true,
                "title" to (detectedTitle ?: ""),
                "author" to (detectedAuthor ?: ""),
                "publisher" to (detectedPublisher ?: ""),
                "publishDateText" to (publishDateText ?: ""),
                "publishYear" to publishYear,
                "tags" to tagsList,
                "detectedCharset" to (usedCharset ?: "Tuntematon"),
                "language" to (detectedLanguage ?: ""),
                "savedToPdf" to tallennettuPdfään
            )
        } catch (e: Exception) {
            return mapOf(
                "success" to false,
                "error" to (e.message ?: "Tuntematon virhe PDFBox-käsittelyssä")
            )
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {}
        }
    }
}