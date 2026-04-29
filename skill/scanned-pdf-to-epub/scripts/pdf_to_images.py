#!/usr/bin/env python3
"""Render PDF pages to ordered image files for scanned-pdf-to-epub."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Callable


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pdf", help="Input PDF path")
    parser.add_argument("-o", "--output-dir", help="Directory for rendered page images")
    parser.add_argument("--dpi", type=int, default=300, help="Render DPI, default: 300")
    parser.add_argument("--format", choices=["png", "jpg", "jpeg"], default="png")
    parser.add_argument("--manifest", help="JSON manifest output path")
    parser.add_argument("--force", action="store_true", help="Overwrite existing page images")
    return parser.parse_args()


def natural_key(path: Path) -> list[object]:
    return [int(part) if part.isdigit() else part for part in re.split(r"(\d+)", path.name)]


def default_output_dir(pdf: Path) -> Path:
    return pdf.with_suffix("")


def page_path(output_dir: Path, page_number: int, image_format: str) -> Path:
    suffix = "jpg" if image_format == "jpeg" else image_format
    return output_dir / f"page-{page_number:04d}.{suffix}"


def render_with_pypdfium2(pdf: Path, output_dir: Path, dpi: int, image_format: str, force: bool) -> list[Path]:
    import pypdfium2 as pdfium  # type: ignore

    document = pdfium.PdfDocument(str(pdf))
    scale = dpi / 72
    paths: list[Path] = []
    try:
        for index in range(len(document)):
            output = page_path(output_dir, index + 1, image_format)
            paths.append(output)
            if output.exists() and not force:
                continue
            page = document[index]
            bitmap = page.render(scale=scale)
            image = bitmap.to_pil()
            image.save(output)
    finally:
        document.close()
    return paths


def render_with_pymupdf(pdf: Path, output_dir: Path, dpi: int, image_format: str, force: bool) -> list[Path]:
    import fitz  # type: ignore

    document = fitz.open(pdf)
    zoom = dpi / 72
    matrix = fitz.Matrix(zoom, zoom)
    paths: list[Path] = []
    try:
        for index, page in enumerate(document, start=1):
            output = page_path(output_dir, index, image_format)
            paths.append(output)
            if output.exists() and not force:
                continue
            pixmap = page.get_pixmap(matrix=matrix, alpha=False)
            pixmap.save(output)
    finally:
        document.close()
    return paths


def render_with_pdf2image(pdf: Path, output_dir: Path, dpi: int, image_format: str, force: bool) -> list[Path]:
    from pdf2image import convert_from_path, pdfinfo_from_path  # type: ignore

    info = pdfinfo_from_path(str(pdf))
    page_count = int(info["Pages"])
    paths = [page_path(output_dir, page, image_format) for page in range(1, page_count + 1)]
    missing = [page for page, path in enumerate(paths, start=1) if force or not path.exists()]
    for page in missing:
        images = convert_from_path(str(pdf), dpi=dpi, first_page=page, last_page=page)
        images[0].save(paths[page - 1])
    return paths


def choose_renderer() -> tuple[str, Callable[[Path, Path, int, str, bool], list[Path]]]:
    candidates = [
        ("pypdfium2", render_with_pypdfium2),
        ("PyMuPDF", render_with_pymupdf),
        ("pdf2image", render_with_pdf2image),
    ]
    errors: list[str] = []
    for name, renderer in candidates:
        module = "fitz" if name == "PyMuPDF" else name
        try:
            __import__(module)
            return name, renderer
        except Exception as exc:
            errors.append(f"{name}: {exc}")
    message = "\n".join(errors)
    raise RuntimeError(
        "No PDF rendering backend is available. Install one of: "
        "pypdfium2, PyMuPDF, or pdf2image with poppler.\n"
        f"Import errors:\n{message}"
    )


def write_manifest(manifest_path: Path, pdf: Path, output_dir: Path, dpi: int, image_format: str, renderer: str, paths: list[Path]) -> None:
    data = {
        "source_pdf": str(pdf),
        "output_dir": str(output_dir),
        "dpi": dpi,
        "format": image_format,
        "renderer": renderer,
        "pages": [
            {"page_number": index, "image_path": str(path)}
            for index, path in enumerate(sorted(paths, key=natural_key), start=1)
        ],
    }
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    pdf = Path(args.pdf).expanduser().resolve()
    if not pdf.is_file():
        print(f"PDF file does not exist: {pdf}", file=sys.stderr)
        return 2
    if pdf.suffix.lower() != ".pdf":
        print(f"Input is not a PDF: {pdf}", file=sys.stderr)
        return 2

    output_dir = Path(args.output_dir).expanduser().resolve() if args.output_dir else default_output_dir(pdf).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    renderer_name, renderer = choose_renderer()
    paths = renderer(pdf, output_dir, args.dpi, args.format, args.force)
    manifest = Path(args.manifest).expanduser().resolve() if args.manifest else output_dir / "manifest.json"
    write_manifest(manifest, pdf, output_dir, args.dpi, args.format, renderer_name, paths)
    print(f"Rendered {len(paths)} pages with {renderer_name}")
    print(f"Images: {output_dir}")
    print(f"Manifest: {manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
