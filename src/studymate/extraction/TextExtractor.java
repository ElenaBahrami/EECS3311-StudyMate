package studymate.extraction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import studymate.model.Chunk;

public class TextExtractor {

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    public List<Chunk> extract(String filePath)
            throws IOException {

        Path path = Path.of(filePath);

        // Check if file exists
        if (!Files.exists(path)) {
            throw new IOException(
                    "File does not exist."
            );
        }

        // Check file size
        if (Files.size(path) > MAX_FILE_SIZE) {
            throw new IOException(
                    "File is larger than 20 MB."
            );
        }

        String fileName =
                path.getFileName()
                        .toString()
                        .toLowerCase();

        // Only TXT and PDF are supported
        if (!fileName.endsWith(".txt")
                && !fileName.endsWith(".pdf")) {

            throw new IOException(
                    "Unsupported file type. Only PDF and TXT are supported."
            );
        }

        // TXT extraction
        if (fileName.endsWith(".txt")) {
            return extractTxt(path);
        }

        // PDF extraction
        return extractPdf(path);
    }

    private List<Chunk> extractTxt(Path path)
            throws IOException {

        String text = Files.readString(path);

        if (text.isBlank()) {
            throw new IOException(
                    "File is empty."
            );
        }

        List<Chunk> chunks =
                new ArrayList<>();
        chunks.add(
                new Chunk(1, text)
        );

        return chunks;
    }

    private List<Chunk> extractPdf(Path path)
            throws IOException {

        List<Chunk> chunks =
                new ArrayList<>();

        File file = path.toFile();

        try (PDDocument document =
                     Loader.loadPDF(file)) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            int pageCount =
                    document.getNumberOfPages();

            for (int page = 1;
                 page <= pageCount;
                 page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text =
                        stripper.getText(document);

                if (!text.isBlank()) {

                    chunks.add(
                            new Chunk(page, text.trim())
                    );
                }
            }
        }

        if (chunks.isEmpty()) {

            throw new IOException(
                    "PDF contains no extractable text. "
                    + "Scanned PDFs require OCR, which is not supported."
            );
        }

        return chunks;
    }
}
