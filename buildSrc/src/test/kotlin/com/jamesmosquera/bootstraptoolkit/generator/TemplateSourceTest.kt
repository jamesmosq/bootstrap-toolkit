package com.jamesmosquera.bootstraptoolkit.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateSourceTest {

    private val valid = "<!--\r\ndescription: Button\r\nvar VARIANT: primary\r\nvar TEXT: Button\r\n-->\r\n\r\n<button class=\"btn btn-\$VARIANT\$\">\$TEXT\$</button>\r\n"

    @Test
    fun `parses header and body, normalising line endings`() {
        val t = TemplateSource.parse("bs5-btn", valid)
        assertEquals("Button", t.description)
        assertEquals(listOf(Variable("VARIANT", "primary"), Variable("TEXT", "Button")), t.variables)
        assertEquals("<button class=\"btn btn-\$VARIANT\$\">\$TEXT\$</button>", t.body)
    }

    @Test
    fun `rejects invalid sources`() {
        fun fails(name: String, text: String) =
            assertThrows(IllegalArgumentException::class.java) { TemplateSource.parse(name, text) }

        fails("btn", valid) // missing bs5- prefix
        fails("bs5-btn", valid.replace("var TEXT: Button\r\n", "")) // used but not declared
        fails("bs5-btn", valid.replace("\$TEXT\$", "Go")) // declared but not used
        fails("bs5-btn", valid.replace("description: Button", "description:")) // no description
        fails("bs5-btn", valid.replace("var VARIANT", "var variant")) // not UPPER_SNAKE_CASE
    }

    @Test
    fun `options attach to a declared variable and must contain its default`() {
        val withOptions = valid.replace("var VARIANT: primary\r\n", "var VARIANT: primary\r\noptions VARIANT: primary, danger\r\n")
        assertEquals(listOf("primary", "danger"), TemplateSource.parse("bs5-btn", withOptions).variables.first().options)

        fun fails(text: String) = assertThrows(IllegalArgumentException::class.java) { TemplateSource.parse("bs5-btn", text) }
        fails(valid.replace("var VARIANT: primary\r\n", "options VARIANT: primary\r\nvar VARIANT: primary\r\n")) // before var
        fails(withOptions.replace("primary, danger", "danger, dark")) // default not an option
        fails(withOptions.replace("primary, danger", "primary, primary")) // duplicates
        fails(withOptions.replace("options VARIANT: primary, danger\r\n", "options VARIANT: primary\r\nexpr VARIANT: date()\r\n")) // both
    }

    @Test
    fun `expr attaches a live template expression`() {
        val withExpr = valid.replace("var TEXT: Button\r\n", "var TEXT: 2026\r\nexpr TEXT: date(\"yyyy\")\r\n")
        assertEquals("date(\"yyyy\")", TemplateSource.parse("bs5-btn", withExpr).variables.last().expression)
        assertThrows(IllegalArgumentException::class.java) {
            TemplateSource.parse("bs5-btn", valid.replace("var TEXT: Button\r\n", "expr TEXT: date()\r\nvar TEXT: Button\r\n"))
        }
    }

    @Test
    fun `pages must be documents and documents must be pages`() {
        val page = "<!--\ndescription: Starter\nkind: page\n-->\n<!doctype html>\n<html>\$END\$</html>"
        assertEquals(Kind.PAGE, TemplateSource.parse("bs5-starter", page).kind)
        assertThrows(IllegalArgumentException::class.java) { TemplateSource.parse("bs5-starter", page.replace("kind: page\n", "")) }
        assertThrows(IllegalArgumentException::class.java) {
            TemplateSource.parse("bs5-btn", valid.replace("description: Button", "description: Button\r\nkind: page"))
        }
        assertThrows(IllegalStateException::class.java) { TemplateSource.parse("bs5-starter", page.replace("kind: page", "kind: site")) }
    }

    @Test
    fun `rejects missing header and unknown keys`() {
        assertThrows(IllegalStateException::class.java) { TemplateSource.parse("bs5-btn", "<button></button>") }
        assertThrows(IllegalStateException::class.java) { TemplateSource.parse("bs5-btn", valid.replace("description", "desc")) }
    }
}
