package com.smartinterview.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A small top-to-bottom layout engine over PDFBox: wrapped paragraphs, bullets,
 * code blocks, score bars and automatic page breaks, with a page-numbered footer.
 */
final class PdfCanvas implements AutoCloseable {

    /** An RGB colour with components 0-1. */
    record Rgb(float r, float g, float b) {
        static Rgb hex(int rgb) {
            return new Rgb(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
    }

    static final Rgb INK = Rgb.hex(0x1F2937);
    static final Rgb MUTED = Rgb.hex(0x6B7280);
    static final Rgb ACCENT = Rgb.hex(0x4F46E5);
    static final Rgb SUCCESS = Rgb.hex(0x15803D);
    static final Rgb WARNING = Rgb.hex(0xB45309);
    static final Rgb DANGER = Rgb.hex(0xB91C1C);
    static final Rgb CODE_BG = Rgb.hex(0xF3F4F6);
    static final Rgb TRACK = Rgb.hex(0xE5E7EB);
    static final Rgb WHITE = Rgb.hex(0xFFFFFF);

    private static final float MARGIN_X = 48;
    private static final float MARGIN_TOP = 54;
    private static final float MARGIN_BOTTOM = 54;

    private final PDDocument doc = new PDDocument();
    private final PDFont regular;
    private final PDFont bold;
    private final PDFont mono;
    private final String footerLabel;

    private PDPage page;
    private PDPageContentStream cs;
    private float y;

    PdfCanvas(String footerLabel) throws IOException {
        this.footerLabel = footerLabel;
        this.regular = font("PlusJakartaSans-Regular.ttf");
        this.bold = font("PlusJakartaSans-Bold.ttf");
        this.mono = font("JetBrainsMono-Regular.ttf");
        newPage();
    }

    private PDFont font(String file) throws IOException {
        try (InputStream in = PdfCanvas.class.getResourceAsStream("/com/smartinterview/fonts/" + file)) {
            if (in == null) {
                throw new IOException("Bundled font missing: " + file);
            }
            return PDType0Font.load(doc, in, true);
        }
    }

    float contentWidth() {
        return PDRectangle.A4.getWidth() - 2 * MARGIN_X;
    }

    // ------------------------------------------------------------ paging

    private void newPage() throws IOException {
        if (cs != null) {
            cs.close();
        }
        page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = PDRectangle.A4.getHeight() - MARGIN_TOP;
    }

    void pageBreak() throws IOException {
        newPage();
    }

    private void ensure(float height) throws IOException {
        if (y - height < MARGIN_BOTTOM) {
            newPage();
        }
    }

    void space(float h) {
        y -= h;
    }

    // ------------------------------------------------------------ text

    void title(String text) throws IOException {
        paragraph(text, bold, 24, INK, 0, 4);
    }

    void heading(String text) throws IOException {
        ensure(60);
        space(10);
        paragraph(text, bold, 15, INK, 0, 2);
        rule();
    }

    void subheading(String text) throws IOException {
        ensure(110);
        space(6);
        paragraph(text, bold, 11, ACCENT, 0, 2);
    }

    void label(String text) throws IOException {
        ensure(30);
        space(4);
        paragraph(text.toUpperCase(), bold, 8, MUTED, 0, 1);
    }

    void body(String text) throws IOException {
        paragraph(text, regular, 10, INK, 0, 3);
    }

    void body(String text, Rgb colour, boolean strong) throws IOException {
        paragraph(text, strong ? bold : regular, 10, colour, 0, 3);
    }

    void muted(String text) throws IOException {
        paragraph(text, regular, 9, MUTED, 0, 3);
    }

    void bullet(String text) throws IOException {
        bullet("•", text);
    }

    void bullet(String marker, String text) throws IOException {
        paragraphAt(text, regular, 10, INK, 14, 2, marker);
    }

    void paragraph(String text, PDFont font, float size, Rgb colour, float indent, float after) throws IOException {
        paragraphAt(text, font, size, colour, indent, after, null);
    }

    /** Wrapped text; {@code marker}, if given, is drawn in the left gutter beside the first line. */
    private void paragraphAt(String text, PDFont font, float size, Rgb colour, float indent, float after,
                             String marker) throws IOException {
        float lead = size * 1.45f;
        boolean first = true;
        for (String line : wrapWords(clean(font, text), font, size, contentWidth() - indent)) {
            ensure(lead);
            y -= lead;
            if (first && marker != null) {
                drawLine(marker, font, size, ACCENT, MARGIN_X + 2);
            }
            drawLine(line, font, size, colour, MARGIN_X + indent);
            first = false;
        }
        y -= after;
    }

    /** Draws one already-positioned line at the current y. */
    private void drawLine(String text, PDFont font, float size, Rgb colour, float x) throws IOException {
        if (text.isEmpty()) {
            return;
        }
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(colour.r(), colour.g(), colour.b());
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    // ------------------------------------------------------------ blocks

    void rule() throws IOException {
        ensure(8);
        cs.setStrokingColor(TRACK.r(), TRACK.g(), TRACK.b());
        cs.setLineWidth(0.8f);
        cs.moveTo(MARGIN_X, y);
        cs.lineTo(MARGIN_X + contentWidth(), y);
        cs.stroke();
        y -= 8;
    }

    /** Monospace code on a grey panel, wrapped by character so indentation survives. */
    void code(String code) throws IOException {
        float size = 8.5f;
        float lead = size * 1.4f;
        float pad = 6;
        float width = contentWidth();
        List<String> lines = wrapChars(clean(mono, code.stripTrailing()), mono, size, width - 2 * pad);
        ensure(lead + 2 * pad);
        y -= 2;
        for (int i = 0; i < lines.size(); i++) {
            ensure(lead + pad);
            y -= lead;
            // Each line paints its own slice of the panel, so the panel splits cleanly across pages.
            cs.setNonStrokingColor(CODE_BG.r(), CODE_BG.g(), CODE_BG.b());
            cs.addRect(MARGIN_X, y - 3, width, lead);
            cs.fill();
            drawLine(lines.get(i), mono, size, INK, MARGIN_X + pad);
        }
        y -= 8;
    }

    /** A labelled progress bar, e.g. "Coding  ███████░░░  72%". */
    void bar(String name, double percent, Rgb colour) throws IOException {
        ensure(22);
        y -= 14;
        drawLine(name, bold, 10, INK, MARGIN_X);
        float barX = MARGIN_X + 90;
        float barW = contentWidth() - 90 - 50;
        cs.setNonStrokingColor(TRACK.r(), TRACK.g(), TRACK.b());
        cs.addRect(barX, y - 1, barW, 8);
        cs.fill();
        cs.setNonStrokingColor(colour.r(), colour.g(), colour.b());
        cs.addRect(barX, y - 1, (float) (barW * Math.max(0, Math.min(100, percent)) / 100.0), 8);
        cs.fill();
        drawLine(Math.round(percent) + "%", bold, 10, INK, barX + barW + 10);
        y -= 8;
    }

    /** A coloured pill with white text, at the start of a new line. */
    void badge(String text, Rgb colour) throws IOException {
        float size = 8;
        String safe = clean(bold, text.toUpperCase());
        float w = width(safe, bold, size) + 12;
        ensure(18);
        y -= 12;
        cs.setNonStrokingColor(colour.r(), colour.g(), colour.b());
        cs.addRect(MARGIN_X, y - 3, w, 12);
        cs.fill();
        drawLine(safe, bold, size, WHITE, MARGIN_X + 6);
        y -= 4;
    }

    /** A large score figure with a caption beside it. */
    void bigScore(String figure, String caption, Rgb colour) throws IOException {
        ensure(48);
        y -= 38;
        drawLine(figure, bold, 36, colour, MARGIN_X);
        float x = MARGIN_X + width(figure, bold, 36) + 14;
        drawLine(caption, bold, 14, INK, x);
        y -= 6;
    }

    // ------------------------------------------------------------ wrapping

    private static float width(String s, PDFont font, float size) throws IOException {
        return font.getStringWidth(s) / 1000f * size;
    }

    private List<String> wrapWords(String text, PDFont font, float size, float max) throws IOException {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isBlank()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" +")) {
                if (word.isEmpty()) continue;
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (width(candidate, font, size) <= max) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (width(word, font, size) <= max) {
                    line.append(word);
                } else {
                    // A single word wider than the line (a long identifier or URL): break it by character.
                    List<String> pieces = wrapChars(word, font, size, max);
                    out.addAll(pieces.subList(0, pieces.size() - 1));
                    line.append(pieces.get(pieces.size() - 1));
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private List<String> wrapChars(String text, PDFont font, float size, float max) throws IOException {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (width(line.toString() + c, font, size) > max && !line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                line.append(c);
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Normalises whitespace and swaps any character the font has no glyph for. */
    private String clean(PDFont font, String text) {
        String s = text == null ? "" : text.replace("\r", "").replace("\t", "    ");
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\n') {
                out.append('\n');
                continue;
            }
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                out.append(ch);
            } catch (IllegalArgumentException | IOException e) {
                // A missing glyph is shown as ? in code, where dropping it would change meaning; elsewhere it is omitted.
                if (font == mono) out.append('?');
            }
        }
        return out.toString();
    }

    // ------------------------------------------------------------ output

    void save(java.nio.file.Path file) throws IOException {
        cs.close();
        cs = null;
        int total = doc.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage p = doc.getPage(i);
            try (PDPageContentStream footer = new PDPageContentStream(
                    doc, p, PDPageContentStream.AppendMode.APPEND, true, true)) {
                String text = clean(regular, footerLabel + "   |   Page " + (i + 1) + " of " + total);
                footer.beginText();
                footer.setFont(regular, 8);
                footer.setNonStrokingColor(MUTED.r(), MUTED.g(), MUTED.b());
                footer.newLineAtOffset(MARGIN_X, 30);
                footer.showText(text);
                footer.endText();
            }
        }
        doc.save(file.toFile());
    }

    @Override
    public void close() throws IOException {
        if (cs != null) {
            cs.close();
        }
        doc.close();
    }
}
