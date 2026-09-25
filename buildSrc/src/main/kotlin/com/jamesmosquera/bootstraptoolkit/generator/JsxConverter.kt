package com.jamesmosquera.bootstraptoolkit.generator

/**
 * Converts a Bootstrap HTML snippet into equivalent JSX markup.
 *
 * Rules (see https://react.dev/learn/writing-markup-with-jsx and https://react.dev/reference/react-dom/components/common):
 * - `class` -> `className`, `for` -> `htmlFor`, multi-word attributes -> camelCase (`tabindex` -> `tabIndex`,
 *   `stroke-width` -> `strokeWidth`); `data-*` and `aria-*` stay as they are.
 * - void elements are self-closed (`<img>` -> `<img />`).
 * - `style="a-b: c"` -> `style={{ aB: 'c' }}`.
 * - `<!-- x -->` -> `{/* x */}`.
 * - `checked` -> `defaultChecked`, and `value` -> `defaultValue` on text-like inputs, textarea and select,
 *   so the snippet renders as an uncontrolled field instead of a read-only one with a React warning.
 *
 * Anything it cannot convert safely (braces in text, inline event handlers, `selected`) is rejected
 * so the source gets fixed instead of shipping broken JSX.
 */
object JsxConverter {

    private val VOID_ELEMENTS = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "source", "track", "wbr")

    private val RENAMES = mapOf(
        "class" to "className", "for" to "htmlFor", "tabindex" to "tabIndex", "readonly" to "readOnly",
        "maxlength" to "maxLength", "minlength" to "minLength", "colspan" to "colSpan", "rowspan" to "rowSpan",
        "autocomplete" to "autoComplete", "autofocus" to "autoFocus", "enctype" to "encType",
        "novalidate" to "noValidate", "crossorigin" to "crossOrigin", "srcset" to "srcSet",
        "allowfullscreen" to "allowFullScreen", "frameborder" to "frameBorder", "contenteditable" to "contentEditable",
        "spellcheck" to "spellCheck", "accesskey" to "accessKey", "inputmode" to "inputMode", "datetime" to "dateTime",
        "hreflang" to "hrefLang", "referrerpolicy" to "referrerPolicy", "usemap" to "useMap", "charset" to "charSet",
        "formaction" to "formAction", "playsinline" to "playsInline",
    )

    /** Props typed as `number` in @types/react; integer literals are emitted as `{n}`. */
    private val NUMERIC_PROPS = setOf("tabIndex", "rows", "cols", "size", "span", "start", "colSpan", "rowSpan", "maxLength", "minLength")
    private val INTEGER = Regex("""-?\d+""")

    /** Input types whose `value` is not the edited value, so it stays `value`. */
    private val FIXED_VALUE_INPUT_TYPES = setOf("checkbox", "radio", "submit", "button", "reset", "hidden", "image")

    private val COMMENT = Regex("""<!--(.*?)-->""", RegexOption.DOT_MATCHES_ALL)
    private val OPEN_TAG = Regex("""<([A-Za-z][A-Za-z0-9-]*)((?:\s[^<>]*?)?)\s*(/?)>""")
    private val ATTRIBUTE = Regex("""([^\s="'/<>]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?""")

    fun convert(html: String): String {
        val withoutComments = COMMENT.replace(html, "")
        require('{' !in withoutComments && '}' !in withoutComments) {
            "braces are JSX expressions; remove { } from the markup"
        }
        val tags = OPEN_TAG.replace(html) { convertTag(it.groupValues[1], it.groupValues[2], selfClosed = it.groupValues[3] == "/") }
        return COMMENT.replace(tags) { "{/*${it.groupValues[1]}*/}" }
    }

    private fun convertTag(tag: String, attributes: String, selfClosed: Boolean): String {
        val lowerTag = tag.lowercase()
        // In HTML only void elements may end in "/>"; <div/> is an unclosed <div>, so it is a source bug.
        require(!selfClosed || lowerTag in VOID_ELEMENTS) { "<$tag/> is not valid HTML; close it with </$tag>" }
        val type = ATTRIBUTE.findAll(attributes)
            .firstOrNull { it.groupValues[1].equals("type", ignoreCase = true) }
            ?.let { value(it)?.lowercase() }
        val converted = ATTRIBUTE.replace(attributes) { convertAttribute(lowerTag, type, it) }
        val close = if (lowerTag in VOID_ELEMENTS) " /" else ""
        return "<$tag$converted$close>"
    }

    private fun value(match: MatchResult): String? =
        match.groups[2]?.value ?: match.groups[3]?.value ?: match.groups[4]?.value

    private fun convertAttribute(tag: String, inputType: String?, match: MatchResult): String {
        val name = match.groupValues[1]
        val lower = name.lowercase()
        val value = value(match)
        require(!lower.startsWith("on")) { "inline event handler '$name' is not allowed" }
        require(lower != "selected") { "'selected' has no JSX equivalent on <option>; put defaultValue on the <select>" }

        if (lower == "style") {
            return "style=${styleObject(requireNotNull(value) { "style needs a value" })}"
        }
        val jsxName = when {
            lower == "checked" -> "defaultChecked"
            lower == "value" && isEditable(tag, inputType) -> "defaultValue"
            lower in RENAMES -> RENAMES.getValue(lower)
            lower.startsWith("data-") || lower.startsWith("aria-") -> name
            '-' in name || ':' in name -> camelCase(name)
            else -> name
        }
        if (value == null) return jsxName
        // React's TypeScript types declare these as number, so "-1" would not compile in TSX.
        if (jsxName in NUMERIC_PROPS && INTEGER.matches(value)) return "$jsxName={$value}"
        // JSX string attributes accept both quote styles; keep the source's.
        return if (match.groups[3] != null) "$jsxName='$value'" else "$jsxName=\"$value\""
    }

    private fun isEditable(tag: String, inputType: String?) =
        tag == "textarea" || tag == "select" || (tag == "input" && (inputType ?: "text") !in FIXED_VALUE_INPUT_TYPES)

    private fun styleObject(css: String): String {
        val declarations = css.split(';').map { it.trim() }.filter { it.isNotEmpty() }.map { declaration ->
            require(':' in declaration) { "invalid style declaration '$declaration'" }
            val property = declaration.substringBefore(':').trim()
            val value = declaration.substringAfter(':').trim().replace("\\", "\\\\").replace("'", "\\'")
            // Custom properties (--bs-*) keep their name and must be quoted keys.
            val key = if (property.startsWith("--")) "'$property'" else camelCase(property)
            "$key: '$value'"
        }
        return "{{ ${declarations.joinToString(", ")} }}"
    }

    private fun camelCase(name: String): String =
        name.split('-', ':').filter { it.isNotEmpty() }.mapIndexed { i, part ->
            if (i == 0) part.lowercase() else part.lowercase().replaceFirstChar { it.uppercase() }
        }.joinToString("")
}
