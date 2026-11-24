package com.metait.modpdfmeta.service;

import com.metait.modpdfmeta.model.DocumentMetadata;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EPUB-tiedostojen metatietojen luku ja tallennus Epublib-kirjastolla.
 */
public class EpubMetadataService {

    public DocumentMetadata lueMetatiedot(Path path) throws Exception {
        DocumentMetadata meta = new DocumentMetadata();
        File file = path.toFile();

        try (FileInputStream fis = new FileInputStream(file)) {
            Book book = (new EpubReader()).readEpub(fis);
            Metadata epubMeta = book.getMetadata();
            if (epubMeta != null) {
                // Otsikko
                if (!epubMeta.getTitles().isEmpty()) {
                    meta.setTitle(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(epubMeta.getTitles().get(0))));
                }

                // Kirjailijat
                if (!epubMeta.getAuthors().isEmpty()) {
                    String authors = epubMeta.getAuthors().stream()
                            .map(a -> (a.getFirstname() + " " + a.getLastname()).trim())
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.joining(", "));
                    meta.setAuthor(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(authors)));
                }

                // Kustantajat
                if (!epubMeta.getPublishers().isEmpty()) {
                    meta.setPublisher(CharsetFixerService.siistiTeksti(CharsetFixerService.korjaaMojibake(String.join(", ", epubMeta.getPublishers()))));
                }

                // Aiheet / avainsanat
                if (!epubMeta.getSubjects().isEmpty()) {
                    meta.setKeywords(String.join(", ", epubMeta.getSubjects()));
                    meta.setSubject(meta.getKeywords());
                }

                // Kieli
                if (epubMeta.getLanguage() != null) {
                    meta.setLanguage(epubMeta.getLanguage().trim().toLowerCase());
                }

                // Päivämäärä
                if (!epubMeta.getDates().isEmpty()) {
                    meta.setDate(epubMeta.getDates().get(0).getValue());
                }

                meta.setDetectedCharset("UTF-8 (EPUB OPF)");
            }
        }
        return meta;
    }

    public void tallennaMetatiedot(Path path, DocumentMetadata meta) throws Exception {
        File file = path.toFile();
        Book book;
        try (FileInputStream fis = new FileInputStream(file)) {
            book = (new EpubReader()).readEpub(fis);
        }

        Metadata epubMeta = book.getMetadata();
        if (epubMeta == null) {
            epubMeta = new Metadata();
            book.setMetadata(epubMeta);
        }

        if (meta.getTitle() != null && !meta.getTitle().isEmpty()) {
            epubMeta.setTitles(new ArrayList<>(Collections.singletonList(meta.getTitle())));
        }

        if (meta.getAuthor() != null && !meta.getAuthor().isEmpty()) {
            String[] authorNames = meta.getAuthor().split("[,;]");
            List<Author> authors = new ArrayList<>();
            for (String an : authorNames) {
                String trim = an.trim();
                if (!trim.isEmpty()) {
                    int spaceIdx = trim.lastIndexOf(' ');
                    if (spaceIdx > 0) {
                        String first = trim.substring(0, spaceIdx).trim();
                        String last = trim.substring(spaceIdx + 1).trim();
                        authors.add(new Author(first, last));
                    } else {
                        authors.add(new Author("", trim));
                    }
                }
            }
            if (!authors.isEmpty()) {
                epubMeta.setAuthors(authors);
            }
        }

        if (meta.getPublisher() != null && !meta.getPublisher().isEmpty()) {
            epubMeta.setPublishers(new ArrayList<>(Collections.singletonList(meta.getPublisher())));
        }

        if (meta.getKeywords() != null && !meta.getKeywords().isEmpty()) {
            String[] subs = meta.getKeywords().split("[,;]");
            List<String> subjects = new ArrayList<>();
            for (String s : subs) {
                String trim = s.trim();
                if (!trim.isEmpty()) subjects.add(trim);
            }
            epubMeta.setSubjects(subjects);
        }

        if (meta.getLanguage() != null && !meta.getLanguage().isEmpty()) {
            epubMeta.setLanguage(meta.getLanguage());
        }

        Path tempFile = Files.createTempFile("modpdfmeta_epub_", ".tmp");
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                (new EpubWriter()).write(book, fos);
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
