# modpdfmeta – PDF & EPUB Metadata Editor

[![Java](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-Build-green.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**modpdfmeta** is a modern, modular desktop application developed in **Java 21** and **JavaFX** for inspecting, editing, repairing character encodings, and extracting metadata in **PDF** and **EPUB** documents.

It provides a dual-layer view of PDF metadata (traditional Low-Level Document Information Dictionary vs. modern Adobe XMP metadata), Mojibake repair, automated EPUB pairing, text mining, and optional AI-powered metadata extraction via Google Gemini or local Ollama.

---

## Key Features

### 1. Dual-Layer PDF Metadata Editing & Synchronization
* **Low-Level Metadata (Document Information Dictionary)**: View and modify trailer dictionary fields (`Title`, `Author`, `Subject`, `Keywords`, `Date`, `Language`, `Publisher`).
* **XML / XMP Metadata (Adobe Extensible Metadata Platform)**: Edit modern XMP schema metadata (Dublin Core, Adobe PDF, XMP Basic).
* **Field-by-Field & Group Transfer**: Move values between Low-Level and XML panels using dedicated direction arrows (`►` and `◄`) or transfer the entire group at once.

### 2. Character Encoding Detection & Mojibake Repair
* **Automatic Detection**: Powered by `juniversalchardet` and byte pattern inspection (UTF-8, UTF-16BE BOM, Windows-1252, ISO-8859-1).
* **Visual Warning Banner**: Alerts you immediately if legacy non-standard encoding is detected in the document.
* **One-Click Unicode Repair ("Korjaa merkistö UTF:ksi")**:
  * Unscrambles corrupted double-encoded Mojibake characters (e.g. `Ã¤` &rarr; `ä`, `â€“` &rarr; `–`).
  * Enforces proper Unicode UTF-16BE (`FE FF` BOM) encoding on save, ensuring the file remains valid and clean across all PDF readers.
* **Manual Charset Selector**: Dropdown menu allowing explicit raw-byte re-interpretation when needed.

### 3. EPUB Metadata & Automated Matching
* **EPUB OPF Metadata**: Inspect and update EPUB metadata (`Title`, `Creator`, `Publisher`, `Date`, `Language`, etc.) using Epublib.
* **Automated PDF &harr; EPUB Matching**: Search for corresponding EPUB files by base name in the folder tree and display both formats side-by-side for comparison and cross-copying.

### 4. Text Mining & AI Metadata Extraction
* **Rule-Based Text Extraction**: Extracts multi-line titles, author name patterns (`Firstname Lastname`), publisher, publication year, ISBN/keywords, and language directly from the opening pages of a document.
* **AI-Assisted Extraction (Google Gemini / Ollama)**:
  * Leverages Google Gemini models (`gemini-3.5-flash-lite`, `gemini-3.5-flash`, `gemini-3.8-flash`) with automatic fallback to extract structured metadata JSON.
  * Local Ollama (e.g., Llama 3) support for offline environments.
* **Interactive Field Selection**: A checklist dialog allows you to review and selectively apply discovered fields.

### 5. Ergonomic Desktop Experience
* **Dracula-Inspired Dark Theme**: Clean, eye-friendly interface built with JavaFX CSS.
* **File List with Distinctive Badges**:
  * `[PDF]` shown in soft light red (`#ff8787`).
  * `[EPUB]` shown in fresh green (`#2e7d32`).
  * Unsaved changes indicated dynamically with an asterisk (`*`).
* **Instant File Preview**: Double-click any file in the list to launch it immediately in your operating system's default viewer (`xdg-open` on Linux, `open` on macOS, `start` on Windows).
* **Directory Navigation**: Browse folders with Next / Previous controls and optional recursive subfolder scanning.
* **Dynamic Multi-Language GUI**: Switch the entire interface on the fly between 6 European languages (**🇫🇮 FI, 🇬🇧 EN, 🇸🇪 SV, 🇩🇪 DE, 🇫🇷 FR, 🇪🇸 ES**) via the flag/code dropdown in the toolbar. The selection is automatically remembered across app restarts.
* **Multi-Language User Guide**: Built-in help dialog with tabs available in all 6 supported languages, opening directly to your currently active GUI language.

---

## Technology Stack

* **Language**: Java 21 (LTS)
* **GUI Framework**: JavaFX 21 (OpenJFX)
* **PDF Engine**: [Apache PDFBox 3.0.3](https://pdfbox.apache.org/) (native COS dictionary and XMP manipulation)
* **EPUB Engine**: [Epublib 3.1](https://github.com/psiegman/epublib)
* **Encoding Detection**: [juniversalchardet 2.5.0](https://github.com/albfernandez/juniversalchardet)
* **Testing**: JUnit 5 (Jupiter)
* **Build System**: Apache Maven 3.9+

---

## Getting Started

### Prerequisites
* **Java Development Kit (JDK) 21** or newer.
* **Apache Maven 3.9+**.

Verify your environment:
```bash
java -version
mvn -version
```

### Installation & Build

Clone the repository and build the project:
```bash
git clone https://github.com/your-username/modpdfmeta.git
cd modpdfmeta
mvn clean package
```

### Running the Application

Run directly using the JavaFX Maven plugin:
```bash
mvn javafx:run
```

Or run the built JAR from the `target/` directory:
```bash
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/modpdfmeta-1.0-SNAPSHOT.jar
```

---

## AI Configuration (Optional)

The application supports tekoäly / AI metadata extraction:

1. **Google Gemini API**:
   * Set your API key in your environment:
     ```bash
     export GEMINI_API_KEY="your-api-key-here"
     ```
   * Alternatively, leave it unset and the application will prompt you on first use and securely save it in Java Preferences.
2. **Local Ollama**:
   * If an Ollama instance is running at `http://localhost:11434` with model `llama3`, the application detects it automatically when no Gemini key is provided.
3. **Local NLP Fallback**:
   * If neither API is configured, the built-in rule-based semantic NLP extractor handles metadata mining without any external network calls.

---

## Project Structure

```text
modpdfmeta/
├── pom.xml
├── README.md
├── Agents.md
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/metait/modpdfmeta/
    │   │       ├── controller/
    │   │       │   ├── PdfEpubMetaWriterApplication.java
    │   │       │   └── PdfEpubMetaWriterController.java
    │   │       ├── model/
    │   │       │   ├── DocumentMetadata.java
    │   │       │   └── FileItem.java
    │   │       ├── service/
    │   │       │   ├── AiMetadataExtractorService.java
    │   │       │   ├── CharsetFixerService.java
    │   │       │   ├── EpubMatcherService.java
    │   │       │   ├── EpubMetadataService.java
    │   │       │   ├── PdfMetadataService.java
    │   │       │   └── TextMetadataExtractorService.java
    │   │       └── util/
    │   │           ├── DialogHelper.java
    │   │           ├── HelpDialog.java
    │   │           └── MetadataExtractionDialog.java
    │   └── resources/
    │       └── com/metait/modpdfmeta/
    │           ├── dark-theme.css
    │           └── pdfepubmetawriter-view.fxml
    └── test/
        └── java/
            └── com/metait/modpdfmeta/
                ├── PdfMetadataServiceTest.java
                └── ServicesTest.java
```

---

## Running Unit Tests

Execute the comprehensive test suite (covers charset decoding, BOM detection, Mojibake unscrambling, language guessing, and JSON parsing):
```bash
mvn test
```

---

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
