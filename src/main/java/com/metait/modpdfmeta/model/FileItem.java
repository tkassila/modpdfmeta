package com.metait.modpdfmeta.model;

import java.nio.file.Path;

/**
 * Tiedostolistassa esitettävä tiedostoalkio.
 */
public class FileItem {
    private Path path;
    private boolean dirty;

    public FileItem(Path path) {
        this.path = path;
        this.dirty = false;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public boolean isPdf() {
        String name = getFileName().toLowerCase();
        return name.endsWith(".pdf");
    }

    public boolean isEpub() {
        String name = getFileName().toLowerCase();
        return name.endsWith(".epub");
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public String toString() {
        String prefix = dirty ? "* " : "";
        String type = isPdf() ? "[PDF] " : (isEpub() ? "[EPUB] " : "[MUU] ");
        return prefix + type + getFileName();
    }
}
