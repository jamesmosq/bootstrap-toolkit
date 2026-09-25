package com.jamesmosquera.bootstraptoolkit.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JsxConverterTest {

    private fun jsx(html: String) = JsxConverter.convert(html)

    @Test
    fun `class and for are renamed`() = assertEquals(
        """<label htmlFor="email" className="form-label">Email</label>""",
        jsx("""<label for="email" class="form-label">Email</label>"""),
    )

    @Test
    fun `void elements are self-closed, already closed ones are kept`() {
        assertEquals("""<img src="a.jpg" className="img-fluid" alt="" />""", jsx("""<img src="a.jpg" class="img-fluid" alt="">"""))
        assertEquals("""<hr />""", jsx("""<hr>"""))
        assertEquals("""<br />""", jsx("""<br/>"""))
        assertEquals("""<input type="text" />""", jsx("""<input type="text" />"""))
    }

    @Test
    fun `non-void self-closing tag is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { jsx("""<div class="x"/>""") }
    }

    @Test
    fun `data and aria attributes are kept, other hyphenated ones become camelCase`() = assertEquals(
        """<svg strokeWidth="2" xlinkHref="#i" data-bs-toggle="modal" aria-label="Close" tabIndex={-1}></svg>""",
        jsx("""<svg stroke-width="2" xlink:href="#i" data-bs-toggle="modal" aria-label="Close" tabindex="-1"></svg>"""),
    )

    @Test
    fun `style becomes an object, custom properties stay quoted`() = assertEquals(
        """<div className="progress-bar" style={{ width: '25%', '--bs-gutter-x': '1rem', backgroundColor: 'red' }}></div>""",
        jsx("""<div class="progress-bar" style="width: 25%; --bs-gutter-x: 1rem; background-color: red;"></div>"""),
    )

    @Test
    fun `comments become jsx comments`() =
        assertEquals("""{/* Button trigger */}<button></button>""", jsx("""<!-- Button trigger --><button></button>"""))

    @Test
    fun `form fields become uncontrolled`() {
        assertEquals("""<input type="email" defaultValue="a@b.co" />""", jsx("""<input type="email" value="a@b.co">"""))
        assertEquals("""<input className="form-check-input" type="checkbox" value="" defaultChecked />""",
            jsx("""<input class="form-check-input" type="checkbox" value="" checked>"""))
        assertEquals("""<input type="submit" value="Send" />""", jsx("""<input type="submit" value="Send">"""))
        assertEquals("""<textarea defaultValue="x" rows={3}></textarea>""", jsx("""<textarea value="x" rows="3"></textarea>"""))
    }

    @Test
    fun `numeric props become number expressions for TSX, non-numeric values stay strings`() {
        assertEquals("""<td colSpan={2} className="x"></td>""", jsx("""<td colspan="2" class="x"></td>"""))
        assertEquals("<div tabIndex=\"\$N\$\"></div>", jsx("<div tabindex=\"\$N\$\"></div>"))
    }

    @Test
    fun `boolean and unquoted attributes`() =
        assertEquals("""<button type="button" disabled>Go</button>""", jsx("""<button type=button disabled>Go</button>"""))

    @Test
    fun `multi-line tags and template variables are preserved`() = assertEquals(
        "<div className=\"card\">\n  <img src=\"\$SRC\$\"\n       className=\"card-img-top\" alt=\"\$ALT\$\" />\n</div>\$END\$",
        jsx("<div class=\"card\">\n  <img src=\"\$SRC\$\"\n       class=\"card-img-top\" alt=\"\$ALT\$\">\n</div>\$END\$"),
    )

    @Test
    fun `unsafe markup is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { jsx("""<p>{x}</p>""") }
        assertThrows(IllegalArgumentException::class.java) { jsx("""<button onclick="go()">Go</button>""") }
        assertThrows(IllegalArgumentException::class.java) { jsx("""<option selected>One</option>""") }
    }
}
