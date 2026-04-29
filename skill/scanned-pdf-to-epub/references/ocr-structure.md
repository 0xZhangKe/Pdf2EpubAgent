# OCR Structure

Use this shape for page-level OCR output so EPUB assembly can stay deterministic and auditable.

## Document JSON

```json
{
  "title": "Exact title if known",
  "author": "Exact author if known",
  "language": "en",
  "source_pdf": "/absolute/path/book.pdf",
  "pages": [
    {
      "page_number": 1,
      "image_path": "/absolute/path/page-1.png",
      "blocks": [
        {
          "type": "heading",
          "level": 1,
          "text": "Chapter title exactly as printed",
          "confidence": 0.98
        },
        {
          "type": "paragraph",
          "text": "Paragraph text exactly as recognized.",
          "confidence": 0.96
        }
      ]
    }
  ]
}
```

## Block Types

Use these `type` values when possible:

- `heading`: include `level` when visually clear; omit it when uncertain.
- `paragraph`: normal body text.
- `list`: include `items`, preserving markers if printed.
- `table`: include `rows` as a list of cell text lists.
- `figure`: include `caption` only when printed in the book.
- `quote`: quoted or indented display text.
- `footnote`: footnote text and marker.
- `page-number`: printed page number.
- `header` and `footer`: printed running headers or footers.
- `unknown`: content exists but its role is unclear.

## Fields

- `text`: exact OCR text. Preserve punctuation, diacritics, capitalization, and original wording.
- `confidence`: numeric OCR confidence from 0 to 1 when available.
- `notes`: short process notes for uncertainty, never replacement content.
- `bbox`: optional `[x, y, width, height]` in image pixels for traceability.
- `ignored_artifacts`: optional page-level list describing omitted non-content artifacts.

Do not store corrected text separately unless the user explicitly requests a reviewed edition. This skill produces a faithful conversion, not an edited edition.
