from pathlib import Path
import tempfile
import unittest

from check_localization import check_project, format_signature, kotlin_literals


class LocalizationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.write("app/src/main/res/values/strings.xml", """
            <resources>
                <string name="brand" translatable="false">Brand</string>
                <string name="hello">Hola %1$s: %2$d</string>
            </resources>
        """)
        self.write("app/src/main/res/values-en/strings.xml", """
            <resources><string name="hello">%2$d: Hello %1$s</string></resources>
        """)

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def test_valid_bilingual_resources_and_technical_exception(self):
        self.write("app/src/main/java/Example.kt", """
            val collection = "users" // l10n:technical Firestore collection identifier
            val input = ""
            Text("$userMessage")
            Text(stringResource(R.string.hello, alias, count))
            /* Text("Not executable") /* nested comment */ */
        """)
        self.assertEqual([], check_project(self.root))

    def test_missing_translation_and_wrong_resource_type_fail(self):
        for english in (
            "<resources/>",
            '<resources><string-array name="hello"><item>Hello</item></string-array></resources>',
        ):
            with self.subTest(english=english):
                self.write("app/src/main/res/values-en/strings.xml", english)
                errors = check_project(self.root)
                self.assertTrue(any("missing English string/hello" in error for error in errors))

    def test_mismatched_formatter_arguments_fail(self):
        self.write("app/src/main/res/values-en/strings.xml", """
            <resources><string name="hello">Hello %1$s: %2$s</string></resources>
        """)
        self.assertTrue(any("formatter arguments differ" in error for error in check_project(self.root)))

    def test_new_compose_variable_course_and_raw_text_fail(self):
        for source in (
            'Text("Cancel")',
            'val notice = "Retry"; Text(notice)',
            'Course(title = "Listening")',
            'Text("""Hello $alias""")',
            r'Text("\u0048ello")',
            'Text("${if (ready) "Ready" else "Wait"}")',
            'Text("// l10n:technical this is still product text")',
        ):
            with self.subTest(source=source):
                self.write("app/src/main/java/Example.kt", source)
                self.assertTrue(any("hardcoded text" in error for error in check_project(self.root)))

    def test_exception_needs_a_reason_and_does_not_exempt_the_file(self):
        for annotation in ("// l10n:technical", "// l10n:technical x", "// l10n:technical x  "):
            self.write("app/src/main/java/Example.kt", f'val key = "users" {annotation}')
            self.assertTrue(any("hardcoded text" in error for error in check_project(self.root)))
        self.write("app/src/main/java/Example.kt", """
            // l10n:technical Firestore collection identifier
            val key = "users"
            Text("Cancel")
        """)
        errors = check_project(self.root)
        self.assertEqual(1, sum("hardcoded text" in error for error in errors))

    def test_arrays_plurals_and_literal_percent(self):
        for locale, greeting in (("values", "Hola"), ("values-en", "Hello")):
            self.write(f"app/src/main/res/{locale}/courses.xml", f"""
                <resources>
                    <string-array name="answers"><item>{greeting}</item><item>2</item></string-array>
                    <plurals name="people">
                        <item quantity="one">%1$d {greeting}</item>
                        <item quantity="other">%1$d {greeting}</item>
                    </plurals>
                    <string name="complete" formatted="false">100% {greeting}</string>
                </resources>
            """)
        self.assertEqual([], check_project(self.root))
        self.write("app/src/main/res/values-en/courses.xml", """
            <resources>
                <string-array name="answers"><item>Hello</item></string-array>
                <plurals name="people"><item quantity="other">%1$s people</item></plurals>
                <string name="complete" formatted="false">100% Hello</string>
            </resources>
        """)
        errors = check_project(self.root)
        self.assertTrue(any("array length/plural quantities differ" in error for error in errors))
        self.assertTrue(any("formatter arguments differ" in error for error in errors))

    def test_duplicate_empty_and_untranslated_english_entries_fail(self):
        for extra in (
            '<string name="hello">Again</string>',
            '<string name="new_text"></string>',
        ):
            self.write("app/src/main/res/values-en/more.xml", f"<resources>{extra}</resources>")
            self.assertTrue(check_project(self.root))
        (self.root / "app/src/main/res/values-en/more.xml").unlink()
        self.write("app/src/main/res/values-en/strings.xml", """
            <resources><string name="hello" translatable="false">Hello %1$s: %2$d</string></resources>
        """)
        self.assertTrue(any("cannot opt out" in error for error in check_project(self.root)))

    def test_default_language_qualified_copy_requires_english_counterpart(self):
        self.write("app/src/main/res/values-night/strings.xml", """
            <resources><string name="evening">Buenas noches</string></resources>
        """)
        self.assertTrue(any("missing English string/evening" in error for error in check_project(self.root)))
        self.write("app/src/main/res/values-en-night/strings.xml", """
            <resources><string name="evening">Good evening</string></resources>
        """)
        self.assertEqual([], check_project(self.root))

    def test_regional_english_overrides_keep_keys_and_formats(self):
        self.write("app/src/main/res/values-en-rUS/strings.xml", """
            <resources><string name="hello">Hi %1$s: %2$d</string></resources>
        """)
        self.assertEqual([], check_project(self.root))
        self.write("app/src/main/res/values-en-land/strings.xml", """
            <resources><string name="only_english">English only</string></resources>
        """)
        self.assertTrue(any("unexpected English string/only_english" in error for error in check_project(self.root)))

    def test_parser_handles_comments_escapes_and_nested_interpolation(self):
        literals = kotlin_literals(
            r'''/* "ignored" */ val user = "$name"; val quoted = "\"Hello\""
            val raw = """Hello ${if (ready) "Ready" else "Wait"}"""
            val character = '"'
            val separator = "\n\t"
            '''
        )
        static = [literal[2] for literal in literals]
        self.assertIn("", static)
        self.assertIn('"Hello"', static)
        self.assertIn("Ready", static)
        self.assertIn("Wait", static)
        self.assertIn("Hello ", static)
        self.assertIn("\n\t", static)
        with self.assertRaises(ValueError):
            kotlin_literals('val text = "unterminated')

    def test_formatter_reordering_reuse_and_bad_arguments(self):
        self.assertEqual(
            format_signature("%1$s %2$d %%"),
            format_signature("%2$d %1$s %<s %%"),
        )
        self.assertEqual({1: {"tH", "tM"}}, format_signature("%1$tH:%1$tM"))
        for text in ("%0$s", "%<s", "%1$q", "%1$tq"):
            with self.subTest(text=text):
                with self.assertRaises(ValueError):
                    format_signature(text)


if __name__ == "__main__":
    unittest.main()
