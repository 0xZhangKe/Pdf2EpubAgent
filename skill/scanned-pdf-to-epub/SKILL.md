---
name: scanned-pdf-to-epub
description: Convert scanned or image-only PDF books into EPUB by first rendering pages to images with the bundled pdf_to_images.py script, then OCRing each page into structured data, and finally assembling a content-faithful EPUB. Use when Codex is asked to convert scanned PDFs, OCR PDF pages, preserve book text exactly, ignore scan artifacts such as watermarks or color blocks, or produce an EPUB from page images.
---

# Scanned PDF to EPUB

## Overview

Convert a scanned PDF into an EPUB without changing the book's content. Treat OCR as transcription: preserve the author's words, punctuation, ordering, headings, footnotes, tables, and page-local structure as faithfully as possible while omitting scanning artifacts.

## Workflow

1. Create a working directory beside the target PDF or in a user-specified output directory.
2. Render the PDF into an ordered image list before doing OCR.
3. OCR every page image into structured JSON; do not write prose directly from the OCR result.
4. Review and normalize only structure, not content.
5. Assemble the EPUB from the structured JSON.
6. Validate the EPUB and do spot checks against source page images.

## Render PDF Pages

Use the bundled renderer script:

```bash
python3 /Users/zhangke/.codex/skills/scanned-pdf-to-epub/scripts/pdf_to_images.py \
  /path/to/book.pdf \
  --output-dir /path/to/work/pages \
  --dpi 300
```

The script writes `page-0001.png`, `page-0002.png`, and so on, plus `manifest.json`. It tries `pypdfium2`, then PyMuPDF, then `pdf2image` with Poppler. If none are available, install one backend before continuing.

Keep the page order exactly as the PDF order. Use the generated manifest for OCR input rather than re-discovering files by a naive directory sort.

## OCR Rules

Use an OCR engine available in the environment or requested by the user. OCR every page image independently, then save page-level JSON before merging.

Hard rules:

- Preserve recognized content exactly. Do not modernize spelling, translate, summarize, polish, complete cut-off text, or silently fix OCR wording.
- Mark uncertainty instead of guessing. Use the schema's `confidence` and `notes` fields for unreadable text.
- Ignore scanner artifacts that are not book content, including watermarks, color blocks, crop marks, shadow bands, bleed-through noise, page edge texture, and repeated non-content stamps.
- Keep meaningful book content even if it looks repetitive: headers, footers, page numbers, captions, footnotes, marginal notes, tables, figures, formulas, and copyright notices.
- Preserve reading order. For multi-column pages, determine columns visually and store blocks in reading order.
- Preserve paragraph and line-break intent. Do not merge paragraphs just because OCR output lacks spacing.

Read `references/ocr-structure.md` before designing the OCR JSON schema or prompt. Read `references/fidelity-rules.md` before deciding whether to omit, correct, or reformat content.

## EPUB Assembly

Prefer `scripts/assemble_epub.py` when the OCR output matches the reference schema. It creates a minimal EPUB 3 file from structured JSON without external dependencies:

```bash
python3 /Users/zhangke/.codex/skills/scanned-pdf-to-epub/scripts/assemble_epub.py \
  --input /path/to/book-ocr.json \
  --output /path/to/book.epub \
  --title "Book Title" \
  --author "Author Name"
```

If the book has chapters, split XHTML files by chapter. If chapter boundaries are uncertain, split by page and use page labels in the table of contents. Do not invent chapter titles.

Formatting may be adjusted for EPUB readability, but text content must remain unchanged. Use semantic tags for headings, paragraphs, lists, tables, block quotes, figures, and footnotes when the OCR structure supports them.

## Validation

Before returning the EPUB:

- Confirm page image count equals PDF page count.
- Confirm every page image has a corresponding OCR JSON entry.
- Compare samples from the beginning, middle, and end of the EPUB against page images.
- Check low-confidence OCR regions manually when possible.
- Validate the EPUB with `epubcheck` if available; otherwise inspect the zip structure and open the EPUB in a local reader if possible.
- Report any unreadable or uncertain regions explicitly instead of hiding them.
