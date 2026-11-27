import argparse
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


RESOURCE_TYPES = {"string", "string-array", "array", "plurals"}
FORMAT = re.compile(
    r"%(?:(\d+)\$)?([-#+ 0,(<]*)(?:\d+)?(?:\.\d+)?([tT])?([a-zA-Z%])"
)
TECHNICAL = re.compile(r"//\s*(?:l10n:technical|i18n:\s*technical)\s+(.+)$")


def format_signature(text):
    signature = {}
    implicit = 0
    previous = None
    for match in FORMAT.finditer(text):
        index, flags, date, conversion = match.groups()
        if conversion in ("%", "n") and not date:
            continue
        if date:
            if conversion not in "HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc":
                raise ValueError("invalid date/time formatter")
        elif conversion.lower() not in "bhscoxdefga":
            raise ValueError("invalid formatter conversion")
        if "<" in flags:
            if index is not None or previous is None:
                raise ValueError("invalid relative formatter argument")
            argument = previous
        elif index is not None:
            argument = int(index)
            if argument < 1:
                raise ValueError("formatter arguments start at 1")
        else:
            implicit += 1
            argument = implicit
        previous = argument
        kind = ("t" + conversion) if date else conversion.lower()
        signature.setdefault(argument, set()).add(kind)
    return signature


def read_resources(folder):
    entries = {}
    for path in sorted(folder.glob("*.xml")):
        root = ET.parse(path).getroot()
        if root.tag != "resources":
            raise ValueError(f"{path}: values XML must have a resources root")
        for element in root:
            kind = element.tag
            if kind == "item" and element.get("type") == "string":
                kind = "string"
            if kind not in RESOURCE_TYPES:
                continue
            name = element.get("name")
            if not name:
                raise ValueError(f"{path}: unnamed {kind} resource")
            key = (kind, name)
            if key in entries:
                raise ValueError(f"{path}: duplicate {kind}/{name}")
            translated = element.get("translatable", "true") != "false"
            formatted = element.get("formatted", "true") != "false"
            if kind == "string":
                values = {"value": "".join(element.itertext()).strip()}
            elif kind == "plurals":
                values = {}
                for item in element:
                    quantity = item.get("quantity")
                    if item.tag != "item" or quantity not in {
                        "zero", "one", "two", "few", "many", "other"
                    }:
                        raise ValueError(f"{path}: invalid plural item in {name}")
                    if quantity in values:
                        raise ValueError(f"{path}: duplicate plural quantity in {name}")
                    values[quantity] = "".join(item.itertext()).strip()
                if "other" not in values:
                    raise ValueError(f"{path}: {name} needs an 'other' plural")
            else:
                if any(item.tag != "item" for item in element):
                    raise ValueError(f"{path}: invalid array item in {name}")
                values = {
                    str(index): "".join(item.itertext()).strip()
                    for index, item in enumerate(element)
                }
            if translated and (not values or any(not value for value in values.values())):
                raise ValueError(f"{path}: empty translatable {kind}/{name}")
            entries[key] = (translated, formatted, values)
    return entries


def compare_resources(spanish, english, label):
    errors = []
    expected = {key for key, entry in spanish.items() if entry[0]}
    for kind, name in sorted(expected - english.keys()):
        errors.append(f"{label}: missing English {kind}/{name}")
    for kind, name in sorted(english.keys() - expected):
        errors.append(f"{label}: unexpected English {kind}/{name}")
    for key in sorted(expected & english.keys()):
        _, source_formatted, source_values = spanish[key]
        translated, target_formatted, target_values = english[key]
        name = "/".join(key)
        if not translated:
            errors.append(f"{label}: English {name} cannot opt out of translation")
        if source_formatted != target_formatted:
            errors.append(f"{label}: formatted attribute differs for {name}")
        if source_values.keys() != target_values.keys():
            errors.append(f"{label}: array length/plural quantities differ for {name}")
        if source_formatted and target_formatted:
            for item in source_values.keys() & target_values.keys():
                try:
                    source = format_signature(source_values[item])
                    target = format_signature(target_values[item])
                except ValueError as error:
                    errors.append(f"{label}: {name}[{item}]: {error}")
                    continue
                if source != target:
                    errors.append(f"{label}: formatter arguments differ for {name}[{item}]")
    return errors


def kotlin_literals(source, comments=None):
    """Read static string fragments without treating comments or user values as copy."""
    literals = []
    length = len(source)
    if comments is None:
        comments = []

    def code(position, interpolation=False):
        depth = 0
        while position < length:
            if source.startswith("//", position):
                end = source.find("\n", position)
                comments.append((position, source[position:length if end < 0 else end]))
                position = length if end < 0 else end + 1
            elif source.startswith("/*", position):
                position += 2
                nesting = 1
                while position < length and nesting:
                    if source.startswith("/*", position):
                        nesting += 1
                        position += 2
                    elif source.startswith("*/", position):
                        nesting -= 1
                        position += 2
                    else:
                        position += 1
                if nesting:
                    raise ValueError("unterminated Kotlin comment")
            elif source[position] == '"':
                start = position
                delimiter = '"""' if source.startswith('"""', position) else '"'
                position += len(delimiter)
                fragments = []
                while position < length and not source.startswith(delimiter, position):
                    if delimiter == '"' and source[position] == "\\":
                        if position + 1 >= length:
                            raise ValueError("unterminated Kotlin string escape")
                        if source.startswith("\\u", position):
                            escape = source[position + 2:position + 6]
                            if len(escape) != 4 or not re.fullmatch("[0-9a-fA-F]{4}", escape):
                                raise ValueError("invalid Kotlin Unicode escape")
                            fragments.append(chr(int(escape, 16)))
                            position += 6
                        else:
                            escaped = source[position + 1]
                            fragments.append({
                                "n": "\n", "r": "\r", "t": "\t", "b": "\b", "f": "\f"
                            }.get(escaped, escaped))
                            position += 2
                    elif source.startswith("${", position):
                        position = code(position + 2, interpolation=True)
                    elif source[position] == "$" and position + 1 < length and (
                        source[position + 1].isalpha() or source[position + 1] == "_"
                    ):
                        position += 2
                        while position < length and (
                            source[position].isalnum() or source[position] == "_"
                        ):
                            position += 1
                    else:
                        fragments.append(source[position])
                        position += 1
                if position >= length:
                    raise ValueError("unterminated Kotlin string")
                position += len(delimiter)
                literals.append((start, position, "".join(fragments)))
            elif source[position] == "'":
                position += 1
                while position < length and source[position] != "'":
                    position += 2 if source[position] == "\\" else 1
                if position >= length:
                    raise ValueError("unterminated Kotlin character")
                position += 1
            elif interpolation and source[position] == "}" and depth == 0:
                return position + 1
            else:
                if source[position] == "{":
                    depth += 1
                elif source[position] == "}":
                    depth -= 1
                position += 1
        if interpolation:
            raise ValueError("unterminated Kotlin interpolation")
        return position

    code(0)
    return literals


def check_kotlin(path):
    source = path.read_text(encoding="utf-8-sig")
    lines = source.splitlines()
    comments = []
    literals = kotlin_literals(source, comments)
    comment_lines = {
        source.count("\n", 0, position): (position, comment)
        for position, comment in comments
    }
    errors = []
    for start, end, static in literals:
        if not any(character.isalpha() for character in static):
            continue
        line = source.count("\n", 0, start)
        end_line = source.count("\n", 0, end)
        # Exempt only an actual line comment, not annotation text inside a string.
        annotations = []
        if end_line in comment_lines:
            position, comment = comment_lines[end_line]
            if position >= end:
                annotations.append(comment)
        if line and line - 1 in comment_lines and lines[line - 1].lstrip().startswith("//"):
            annotations.append(comment_lines[line - 1][1])
        justified = [
            match for comment in annotations
            if (match := TECHNICAL.search(comment)) and len(match.group(1).strip()) >= 3
        ]
        if not justified:
            errors.append(
                f"{path}:{line + 1}: hardcoded text; use bilingual resources "
                "or justify a technical literal with // l10n:technical <reason>"
            )
    return errors


def check_project(root):
    errors = []
    resources = root / "app" / "src" / "main" / "res"
    if not (resources / "values").is_dir():
        errors.append("app/src/main/res/values: Spanish default resources are required")
    if not (resources / "values-en").is_dir():
        errors.append("app/src/main/res/values-en: complete English resources are required")
    folders = [resources / "values", resources / "values-es"]
    folders.extend(
        folder for folder in resources.glob("values-*")
        if folder.is_dir() and folder.name != "values-es"
        and not re.match(r"values-(?:[a-z]{2}(?:-|$)|b\+)", folder.name)
        and folder.name != "values-en"
    )
    seen_english = set()
    for folder in folders:
        if not folder.is_dir():
            continue
        suffix = "" if folder.name in {"values", "values-es"} else folder.name[7:]
        english_folder = resources / ("values-en" + (f"-{suffix}" if suffix else ""))
        try:
            spanish = read_resources(folder)
            english = read_resources(english_folder)
            if folder.name == "values-es":
                # Explicit Spanish overrides may use the complete English fallback.
                english = {key: value for key, value in english.items() if key in spanish}
            elif english_folder in seen_english:
                continue
            errors.extend(compare_resources(spanish, english, folder.name))
            seen_english.add(english_folder)
        except (ValueError, ET.ParseError) as error:
            errors.append(str(error))
    for english_folder in resources.glob("values-en-*"):
        if english_folder.is_dir() and english_folder not in seen_english:
            try:
                suffix = english_folder.name[len("values-en-"):]
                parts = suffix.split("-")
                if re.fullmatch(r"r[A-Z]{2}", parts[0]):
                    parts = parts[1:]
                fallback = read_resources(resources / "values")
                if parts:
                    fallback.update(read_resources(resources / ("values-" + "-".join(parts))))
                english = read_resources(english_folder)
                source = {key: fallback[key] for key in english if key in fallback}
                errors.extend(compare_resources(source, english, english_folder.name))
            except (ValueError, ET.ParseError) as error:
                errors.append(str(error))
    for path in sorted((root / "app" / "src" / "main").rglob("*.kt")):
        try:
            errors.extend(check_kotlin(path))
        except ValueError as error:
            errors.append(f"{path}: {error}")
    return errors


def main():
    parser = argparse.ArgumentParser(description="Check Spanish/English Android localization")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    try:
        errors = check_project(args.root)
    except (OSError, UnicodeError) as error:
        print(f"Localization check could not read the project: {error}", file=sys.stderr)
        return 1
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1
    print("Spanish/English resources and production Kotlin localization checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
