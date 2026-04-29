#!/usr/bin/env python3
"""Assemble a simple EPUB 3 file from scanned-pdf-to-epub OCR JSON."""

from __future__ import annotations

import argparse
import html
import json
import mimetypes
import posixpath
import uuid
import zipfile
from pathlib import Path
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, help="OCR JSON file")
    parser.add_argument("--output", required=True, help="Output .epub path")
    parser.add_argument("--title", help="EPUB title override")
    parser.add_argument("--author", help="EPUB author override")
    parser.add_argument("--language", default=None, help="BCP-47 language code")
    return parser.parse_args()


def esc(value: Any) -> str:
    return html.escape(str(value), quote=True)


def block_to_html(block: dict[str, Any]) -> str:
    kind = block.get("type", "paragraph")
    text = block.get("text", "")

    if kind == "heading":
        level = int(block.get("level") or 2)
        level = max(1, min(level, 6))
        return f"<h{level}>{esc(text)}</h{level}>"
    if kind == "list":
        items = block.get("items") or []
        lis = "".join(f"<li>{esc(item)}</li>" for item in items)
        return f"<ul>{lis}</ul>"
    if kind == "table":
        rows = block.get("rows") or []
        body = []
        for row in rows:
            cells = "".join(f"<td>{esc(cell)}</td>" for cell in row)
            body.append(f"<tr>{cells}</tr>")
        return f"<table>{''.join(body)}</table>"
    if kind == "figure":
        caption = block.get("caption", text)
        return f"<figure><figcaption>{esc(caption)}</figcaption></figure>"
    if kind == "quote":
        return f"<blockquote>{esc(text)}</blockquote>"
    if kind == "footnote":
        return f"<p class=\"footnote\">{esc(text)}</p>"
    if kind in {"page-number", "header", "footer"}:
        return f"<p class=\"{esc(kind)}\">{esc(text)}</p>"
    if kind == "unknown":
        return f"<p class=\"unknown\">{esc(text)}</p>"
    return f"<p>{esc(text)}</p>"


def page_xhtml(page: dict[str, Any], title: str) -> str:
    page_number = page.get("page_number", "")
    blocks = page.get("blocks") or []
    body = "\n".join(block_to_html(block) for block in blocks)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head>
  <title>{esc(title)} - Page {esc(page_number)}</title>
  <link rel="stylesheet" type="text/css" href="../styles/style.css" />
</head>
<body>
  <section epub:type="pagebreak" id="page-{esc(page_number)}">
    {body}
  </section>
</body>
</html>
"""


def nav_xhtml(title: str, pages: list[dict[str, Any]]) -> str:
    links = []
    for index, page in enumerate(pages, start=1):
        label = page.get("label") or f"Page {page.get('page_number', index)}"
        links.append(f'<li><a href="pages/page-{index}.xhtml">{esc(label)}</a></li>')
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>{esc(title)} Contents</title></head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>{esc(title)}</h1>
    <ol>{''.join(links)}</ol>
  </nav>
</body>
</html>
"""


def content_opf(book_id: str, title: str, author: str, language: str, page_count: int) -> str:
    manifest_pages = "\n".join(
        f'    <item id="page-{i}" href="pages/page-{i}.xhtml" media-type="application/xhtml+xml"/>'
        for i in range(1, page_count + 1)
    )
    spine_pages = "\n".join(f'    <itemref idref="page-{i}"/>' for i in range(1, page_count + 1))
    return f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="book-id">{esc(book_id)}</dc:identifier>
    <dc:title>{esc(title)}</dc:title>
    <dc:creator>{esc(author)}</dc:creator>
    <dc:language>{esc(language)}</dc:language>
    <meta property="dcterms:modified">2000-01-01T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="style" href="styles/style.css" media-type="text/css"/>
{manifest_pages}
  </manifest>
  <spine>
{spine_pages}
  </spine>
</package>
"""


def write_epub(data: dict[str, Any], output: Path, title_override: str | None, author_override: str | None, language_override: str | None) -> None:
    pages = data.get("pages") or []
    if not pages:
        raise ValueError("Input JSON must contain a non-empty pages array.")

    title = title_override or data.get("title") or "Untitled"
    author = author_override or data.get("author") or "Unknown"
    language = language_override or data.get("language") or "und"
    book_id = data.get("identifier") or f"urn:uuid:{uuid.uuid4()}"

    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w") as epub:
        epub.writestr("mimetype", "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        epub.writestr("META-INF/container.xml", CONTAINER_XML)
        epub.writestr("OEBPS/content.opf", content_opf(book_id, title, author, language, len(pages)))
        epub.writestr("OEBPS/nav.xhtml", nav_xhtml(title, pages))
        epub.writestr("OEBPS/styles/style.css", STYLE_CSS)
        for index, page in enumerate(pages, start=1):
            epub.writestr(posixpath.join("OEBPS", "pages", f"page-{index}.xhtml"), page_xhtml(page, title))


CONTAINER_XML = """<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""


STYLE_CSS = """
body { font-family: serif; line-height: 1.45; margin: 5%; }
h1, h2, h3, h4, h5, h6 { line-height: 1.2; }
table { border-collapse: collapse; margin: 1em 0; }
td, th { border: 1px solid #777; padding: 0.25em 0.4em; vertical-align: top; }
.footnote, .page-number, .header, .footer { font-size: 0.9em; }
.unknown { outline: 1px dotted #999; }
"""


def main() -> None:
    args = parse_args()
    input_path = Path(args.input)
    output_path = Path(args.output)
    with input_path.open("r", encoding="utf-8") as file:
        data = json.load(file)
    write_epub(data, output_path, args.title, args.author, args.language)
    media_type = mimetypes.guess_type(output_path.name)[0] or "application/epub+zip"
    print(f"Wrote {output_path} ({media_type})")


if __name__ == "__main__":
    main()
