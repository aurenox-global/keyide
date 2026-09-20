package com.keyide.app.editor

/** Snippets de código por lenguaje (insertables en el cursor). */
object Snippets {

    data class Snippet(val label: String, val code: String)

    fun forLanguage(lang: String): List<Snippet> = when (lang) {
        "javascript", "typescript" -> listOf(
            Snippet("function", "function name(args) {\n  \n}"),
            Snippet("arrow", "const name = (args) => {\n  \n}"),
            Snippet("console.log", "console.log()"),
            Snippet("for", "for (let i = 0; i < n; i++) {\n  \n}"),
            Snippet("if / else", "if (cond) {\n  \n} else {\n  \n}"),
            Snippet("try / catch", "try {\n  \n} catch (e) {\n  console.error(e)\n}"),
            Snippet("fetch", "fetch(url)\n  .then(r => r.json())\n  .then(d => console.log(d))\n  .catch(e => console.error(e))"),
            Snippet("class", "class Name {\n  constructor() {\n  }\n}"),
            Snippet("import", "import { x } from \"./mod\"")
        )
        "python" -> listOf(
            Snippet("def", "def name(args):\n    "),
            Snippet("class", "class Name:\n    def __init__(self):\n        pass"),
            Snippet("if", "if cond:\n    "),
            Snippet("for", "for i in range(10):\n    print(i)"),
            Snippet("try / except", "try:\n    pass\nexcept Exception as e:\n    print(e)"),
            Snippet("with open", "with open(\"archivo.txt\") as f:\n    data = f.read()"),
            Snippet("print", "print()")
        )
        "kotlin" -> listOf(
            Snippet("fun", "fun name(args): Return {\n    \n}"),
            Snippet("class", "class Name(\n) {\n}"),
            Snippet("main", "fun main() {\n    println(\"Hola\")\n}"),
            Snippet("when", "when (x) {\n    1 -> \n    else -> \n}"),
            Snippet("val list", "val items = listOf()")
        )
        "java" -> listOf(
            Snippet("main", "public static void main(String[] args) {\n    \n}"),
            Snippet("class", "public class Name {\n}"),
            Snippet("for", "for (int i = 0; i < n; i++) {\n}"),
            Snippet("println", "System.out.println()")
        )
        "html" -> listOf(
            Snippet("HTML5", "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n  <meta charset=\"utf-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <title></title>\n</head>\n<body>\n  \n</body>\n</html>"),
            Snippet("script", "<script src=\"\"></script>"),
            Snippet("style", "<style>\n  \n</style>"),
            Snippet("div", "<div class=\"\">\n  \n</div>")
        )
        "css" -> listOf(
            Snippet("regla", ".clase {\n  \n}"),
            Snippet("flex", "display: flex;\nalign-items: center;\njustify-content: center;"),
            Snippet(":root", ":root {\n  --var: #000;\n}")
        )
        "shell" -> listOf(
            Snippet("shebang", "#!/bin/sh"),
            Snippet("for", "for f in *; do\n  echo \"\$f\"\ndone"),
            Snippet("if", "if [ -f archivo ]; then\n  echo ok\nfi")
        )
        else -> listOf(Snippet("TODO", "// TODO: "))
    }
}
