from pathlib import Path
import html
import re
import sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import ListFlowable, ListItem, Paragraph, Preformatted, SimpleDocTemplate, Spacer


def build_styles():
    styles = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "TitleCustom",
            parent=styles["Title"],
            fontName="Helvetica-Bold",
            fontSize=20,
            leading=24,
            spaceAfter=12,
            alignment=TA_LEFT,
        ),
        "h1": ParagraphStyle(
            "H1Custom",
            parent=styles["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=16,
            leading=20,
            spaceBefore=10,
            spaceAfter=8,
        ),
        "h2": ParagraphStyle(
            "H2Custom",
            parent=styles["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=13,
            leading=16,
            spaceBefore=8,
            spaceAfter=6,
        ),
        "h3": ParagraphStyle(
            "H3Custom",
            parent=styles["Heading3"],
            fontName="Helvetica-Bold",
            fontSize=11,
            leading=14,
            spaceBefore=6,
            spaceAfter=4,
        ),
        "body": ParagraphStyle(
            "BodyCustom",
            parent=styles["BodyText"],
            fontName="Helvetica",
            fontSize=10,
            leading=14,
            spaceAfter=4,
        ),
        "code": ParagraphStyle(
            "CodeCustom",
            parent=styles["Code"],
            fontName="Courier",
            fontSize=8,
            leading=10,
            leftIndent=6,
            rightIndent=6,
            borderPadding=6,
            backColor=colors.whitesmoke,
            borderColor=colors.lightgrey,
            borderWidth=0.5,
            borderRadius=2,
            spaceBefore=4,
            spaceAfter=6,
        ),
    }


def inline_markup(text: str) -> str:
    text = html.escape(text)
    text = re.sub(r"`([^`]+)`", r"<font name='Courier'>\1</font>", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    return text


def parse_markdown(md_text: str):
    lines = md_text.splitlines()
    blocks = []
    i = 0
    while i < len(lines):
        line = lines[i]

        if not line.strip():
            i += 1
            continue

        if line.startswith("```"):
            fence = line
            code_lines = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                code_lines.append(lines[i])
                i += 1
            i += 1
            blocks.append(("code", "\n".join(code_lines)))
            continue

        if line.startswith("# "):
            blocks.append(("title", line[2:].strip()))
            i += 1
            continue
        if line.startswith("## "):
            blocks.append(("h1", line[3:].strip()))
            i += 1
            continue
        if line.startswith("### "):
            blocks.append(("h2", line[4:].strip()))
            i += 1
            continue
        if line.startswith("#### "):
            blocks.append(("h3", line[5:].strip()))
            i += 1
            continue

        if re.match(r"^[-*] ", line):
            items = []
            while i < len(lines) and re.match(r"^[-*] ", lines[i]):
                items.append(lines[i][2:].strip())
                i += 1
            blocks.append(("bullets", items))
            continue

        if re.match(r"^\d+\. ", line):
            items = []
            while i < len(lines) and re.match(r"^\d+\. ", lines[i]):
                items.append(re.sub(r"^\d+\. ", "", lines[i]).strip())
                i += 1
            blocks.append(("numbers", items))
            continue

        paragraph_lines = [line.strip()]
        i += 1
        while i < len(lines):
            current = lines[i]
            if not current.strip():
                break
            if current.startswith("#") or current.startswith("```"):
                break
            if re.match(r"^[-*] ", current) or re.match(r"^\d+\. ", current):
                break
            paragraph_lines.append(current.strip())
            i += 1
        blocks.append(("paragraph", " ".join(paragraph_lines)))

    return blocks


def render(md_path: Path, pdf_path: Path):
    styles = build_styles()
    story = []

    for kind, value in parse_markdown(md_path.read_text(encoding="utf-8")):
        if kind in {"title", "h1", "h2", "h3"}:
            story.append(Paragraph(inline_markup(value), styles[kind]))
            continue
        if kind == "paragraph":
            story.append(Paragraph(inline_markup(value), styles["body"]))
            continue
        if kind == "code":
            story.append(Preformatted(value, styles["code"]))
            continue
        if kind in {"bullets", "numbers"}:
            bullet_type = "bullet" if kind == "bullets" else "1"
            items = [
                ListItem(Paragraph(inline_markup(item), styles["body"]))
                for item in value
            ]
            list_kwargs = {
                "bulletType": bullet_type,
                "leftIndent": 14,
            }
            if kind == "numbers":
                list_kwargs["start"] = "1"
                list_kwargs["bulletFormat"] = "%s."
            story.append(ListFlowable(items, **list_kwargs))
            story.append(Spacer(1, 2))
            continue

    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=16 * mm,
        bottomMargin=16 * mm,
        title=md_path.stem,
        author="OpenAI Codex",
    )
    doc.build(story)


def main():
    if len(sys.argv) != 3:
        raise SystemExit("usage: md_to_pdf.py <input.md> <output.pdf>")
    render(Path(sys.argv[1]), Path(sys.argv[2]))


if __name__ == "__main__":
    main()
