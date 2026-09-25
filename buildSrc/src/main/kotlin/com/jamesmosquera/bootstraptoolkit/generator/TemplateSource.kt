package com.jamesmosquera.bootstraptoolkit.generator

/**
 * A live template variable. Declaration order is the Tab order in the editor.
 * [options] become an `enum(...)` expression: a completion list shown when the template expands.
 * [expression] is a raw live template expression such as `date("yyyy")`
 * (https://www.jetbrains.com/help/idea/template-variables.html#predefined_functions).
 */
data class Variable(
    val name: String,
    val default: String,
    val options: List<String> = emptyList(),
    val expression: String = "",
)

/**
 * COMPONENT: a fragment, generated for HTML/Vue and JSX/TSX.
 * PAGE: a whole document (`<!doctype html>`...), generated for plain HTML only.
 */
enum class Kind { COMPONENT, PAGE }

/**
 * One source template: an HTML file under src/templates whose name is the abbreviation
 * (bs5-btn.html -> bs5-btn) and which starts with a header comment:
 *
 * ```
 * <!--
 * description: Bootstrap 5 button
 * var VARIANT: primary
 * var TEXT: Button
 * -->
 * <button type="button" class="btn btn-$VARIANT$">$TEXT$</button>
 * ```
 *
 * Optional header keys: `kind: page` marks a whole document (HTML only, see [Kind]);
 * `options NAME: a, b, c` offers a completion list for a variable declared above it;
 * `expr NAME: date("yyyy")` computes it with a live template function (the default is the fallback).
 * The body is plain Bootstrap 5.3 HTML; the JSX version is derived from it by [JsxConverter].
 */
data class TemplateSource(
    val name: String,
    val description: String,
    val variables: List<Variable>,
    val body: String,
    val kind: Kind = Kind.COMPONENT,
) {
    companion object {
        private val NAME = Regex("""bs5-[a-z0-9]+(-[a-z0-9]+)*""")
        private val VARIABLE_NAME = Regex("""[A-Z][A-Z0-9_]*""")
        private val VARIABLE_USE = Regex("""\$([A-Za-z_][A-Za-z0-9_]*)\$""")
        private val HEADER = Regex("""\A\s*<!--(.*?)-->""", RegexOption.DOT_MATCHES_ALL)
        private val PREDEFINED = setOf("END", "SELECTION")

        fun parse(name: String, text: String): TemplateSource {
            require(NAME.matches(name)) { "file name must be the abbreviation, like bs5-card-image (got '$name')" }
            val source = text.replace("\r\n", "\n")
            val header = HEADER.find(source) ?: error("must start with a <!-- ... --> header")

            var description = ""
            var kind = Kind.COMPONENT
            val variables = mutableListOf<Variable>()
            header.groupValues[1].lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                require(':' in line) { "header line must be 'key: value' (got '$line')" }
                val key = line.substringBefore(':').trim()
                val value = line.substringAfter(':').trim()
                when {
                    key == "description" -> description = value
                    key == "kind" -> kind = Kind.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                        ?: error("kind must be 'component' or 'page' (got '$value')")
                    key.startsWith("var ") -> variables += Variable(key.removePrefix("var ").trim(), value)
                    key.startsWith("options ") -> {
                        val target = key.removePrefix("options ").trim()
                        val index = variables.indexOfFirst { it.name == target }
                        require(index >= 0) { "options for '$target' must come after 'var $target'" }
                        val options = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        require(options.isNotEmpty()) { "options for '$target' are empty" }
                        variables[index] = variables[index].copy(options = options)
                    }
                    key.startsWith("expr ") -> {
                        val target = key.removePrefix("expr ").trim()
                        val index = variables.indexOfFirst { it.name == target }
                        require(index >= 0) { "expr for '$target' must come after 'var $target'" }
                        require(value.isNotEmpty()) { "expr for '$target' is empty" }
                        variables[index] = variables[index].copy(expression = value)
                    }
                    else -> error("unknown header key '$key' (expected 'description', 'kind', 'var NAME', 'options NAME' or 'expr NAME')")
                }
            }

            val body = source.substring(header.range.last + 1)
                .lines().dropWhile { it.isBlank() }.joinToString("\n").trimEnd()

            require(description.isNotBlank()) { "header needs a 'description'" }
            require(body.isNotBlank()) { "template body is empty" }
            variables.forEach {
                require(VARIABLE_NAME.matches(it.name)) { "variable '${it.name}' must be UPPER_SNAKE_CASE" }
                require(it.name !in PREDEFINED) { "\$${it.name}\$ is predefined, do not declare it" }
                require('"' !in it.default && '\\' !in it.default) { "default of ${it.name} must not contain \" or \\" }
                require(it.options.none { o -> '"' in o || '\\' in o }) { "options of ${it.name} must not contain \" or \\" }
                require(it.options.size == it.options.toSet().size) { "options of ${it.name} have duplicates" }
                require(it.options.isEmpty() || it.default in it.options) { "default of ${it.name} must be one of its options" }
                require(it.options.isEmpty() || it.expression.isEmpty()) { "${it.name} cannot have both options and expr" }
            }
            val declared = variables.map { it.name }
            require(declared.size == declared.toSet().size) { "duplicate variable declaration" }
            val used = VARIABLE_USE.findAll(body).map { it.groupValues[1] }.toSet() - PREDEFINED
            (used - declared.toSet()).let { require(it.isEmpty()) { "used but not declared: $it" } }
            (declared.toSet() - used).let { require(it.isEmpty()) { "declared but not used: $it" } }

            val isDocument = body.trimStart().startsWith("<!doctype", ignoreCase = true)
            require(isDocument == (kind == Kind.PAGE)) {
                if (isDocument) "a <!doctype html> document needs 'kind: page'" else "'kind: page' must start with <!doctype html>"
            }

            return TemplateSource(name, description, variables, body, kind)
        }
    }
}
