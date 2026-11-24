package com.metait.modpdfmeta.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Etsii PDF-tiedostolle vastaavaa .epub-tiedostoa.
 * Ensimmäisenä etsitään samalla base-nimellä, ja sen jälkeen muilla vastaavuusmenetelmillä.
 */
public class EpubMatcherService {

    public List<Path> etsiVastaavatEpubTiedostot(Path pdfPath, boolean searchSubdirectories) {
        List<Path> tulokset = new ArrayList<>();
        if (pdfPath == null || !Files.exists(pdfPath)) {
            return tulokset;
        }

        Path parentDir = pdfPath.getParent();
        if (parentDir == null || !Files.isDirectory(parentDir)) {
            return tulokset;
        }

        String pdfFileName = pdfPath.getFileName().toString();
        String baseName = getBaseName(pdfFileName);

        // 1. Ensisijainen: etsi suoraan sama base-nimi .epub-päätteellä
        Path exactMatch = parentDir.resolve(baseName + ".epub");
        if (Files.isRegularFile(exactMatch)) {
            tulokset.add(exactMatch);
        }

        // 2. Toissijaiset: etsi muita .epub-tiedostoja samasta hakemistosta (tai alihakemistoista)
        List<Path> allEpubs = new ArrayList<>();
        kerääEpubTiedostot(parentDir, allEpubs, searchSubdirectories);

        String normalizedBase = normalizeForComparison(baseName);

        for (Path epub : allEpubs) {
            if (epub.equals(exactMatch)) {
                continue; // Ei lisätä tarkkaa osumaa kahdesti
            }
            String epubBase = getBaseName(epub.getFileName().toString());
            String normalizedEpub = normalizeForComparison(epubBase);

            if (normalizedEpub.contains(normalizedBase) || normalizedBase.contains(normalizedEpub)) {
                tulokset.add(epub);
            }
        }

        return tulokset;
    }

    private void kerääEpubTiedostot(Path dir, List<Path> result, boolean recursive) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && recursive) {
                    kerääEpubTiedostot(entry, result, true);
                } else if (Files.isRegularFile(entry) && entry.getFileName().toString().toLowerCase().endsWith(".epub")) {
                    result.add(entry);
                }
            }
        } catch (IOException ignored) {}
    }

    public static String getBaseName(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            return fileName.substring(0, dotIdx);
        }
        return fileName;
    }

    private String normalizeForComparison(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9äöå]", "")
                .trim();
    }
}
