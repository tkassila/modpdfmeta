package com.metait.modpdfmeta.model;

import java.util.Objects;

/**
 * Metatietojen tietomalli, joka kattaa sekä matalan tason, XML/XMP- että EPUB-metatiedot.
 */
public class DocumentMetadata {
    private String title = "";
    private String author = "";
    private String publisher = "";
    private String subject = "";
    private String keywords = "";
    private String date = "";
    private String language = "";
    private String detectedCharset = "";

    public DocumentMetadata() {
    }

    public DocumentMetadata(DocumentMetadata other) {
        copyFrom(other);
    }

    public void copyFrom(DocumentMetadata other) {
        if (other == null) return;
        this.title = other.title != null ? other.title : "";
        this.author = other.author != null ? other.author : "";
        this.publisher = other.publisher != null ? other.publisher : "";
        this.subject = other.subject != null ? other.subject : "";
        this.keywords = other.keywords != null ? other.keywords : "";
        this.date = other.date != null ? other.date : "";
        this.language = other.language != null ? other.language : "";
        this.detectedCharset = other.detectedCharset != null ? other.detectedCharset : "";
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title != null ? title : ""; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author != null ? author : ""; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher != null ? publisher : ""; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject != null ? subject : ""; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords != null ? keywords : ""; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date != null ? date : ""; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language != null ? language : ""; }

    public String getDetectedCharset() { return detectedCharset; }
    public void setDetectedCharset(String detectedCharset) { this.detectedCharset = detectedCharset != null ? detectedCharset : ""; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentMetadata that = (DocumentMetadata) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(author, that.author) &&
               Objects.equals(publisher, that.publisher) &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(keywords, that.keywords) &&
               Objects.equals(date, that.date) &&
               Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, publisher, subject, keywords, date, language);
    }
}
