package com.example.kontirol

data class KeyDef(
    val code: Int,
    val label: String,
    val longPressCode: Int? = null,
    val longPressLabel: String? = null,
    val widthWeight: Float = 1f,
    val isSpecial: Boolean = false
)

data class KeyboardLayout(
    val rows: List<List<KeyDef>>,
    val rowHeightDp: Int = 48
)

object KeyboardLayouts {

    private fun engRow(vararg chars: String): List<KeyDef> =
        chars.map { c -> KeyDef(code = c[0].code, label = c) }

    val ENGLISH = KeyboardLayout(
        rows = listOf(
            engRow("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            engRow("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf(
                KeyDef(-1, "⇧", isSpecial = true, widthWeight = 1.3f),
                *engRow("z", "x", "c", "v", "b", "n", "m").toTypedArray(),
                KeyDef(-3, "⌫", isSpecial = true, widthWeight = 1.3f)
            ),
            listOf(
                KeyDef(-6, "123", isSpecial = true, widthWeight = 1.2f),
                KeyDef(-5, "space", isSpecial = true, widthWeight = 5f),
                KeyDef(-2, "↵", isSpecial = true, widthWeight = 1.5f)
            )
        )
    )

    /** 中文键盘：空格左右有逗号和句号 */
    val CHINESE = KeyboardLayout(
        rows = listOf(
            engRow("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            engRow("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf(
                KeyDef(-1, "⇧", isSpecial = true, widthWeight = 1.3f),
                *engRow("z", "x", "c", "v", "b", "n", "m").toTypedArray(),
                KeyDef(-3, "⌫", isSpecial = true, widthWeight = 1.3f)
            ),
            listOf(
                KeyDef(-6, "123", isSpecial = true, widthWeight = 1.2f),
                KeyDef(0xFF0C, "，", widthWeight = 0.6f),
                KeyDef(-5, "space", isSpecial = true, widthWeight = 3.8f),
                KeyDef(0x3002, "。", widthWeight = 0.6f),
                KeyDef(-2, "↵", isSpecial = true, widthWeight = 1.5f)
            )
        )
    )

    private fun uk(code: Int, label: String, lpCode: Int? = null, lpLabel: String? = null) =
        KeyDef(code, label, lpCode, lpLabel)

    val UYGHUR = KeyboardLayout(
        rowHeightDp = 50,
        rows = listOf(
            listOf(
                uk(0x0686, "چ"), uk(0x06CB, "ۋ"), uk(0x06D0, "ې"), uk(0x0631, "ر"), uk(0x062A, "ت"),
                uk(0x064A, "ي"), uk(0x06C7, "ۇ"), uk(0x06AD, "ڭ"), uk(0x0648, "و"), uk(0x067E, "پ")
            ),
            listOf(
                uk(0x06BE, "ھ"), uk(0x0633, "س"),
                uk(0x062F, "د", 0x0698, "ژ"),
                uk(0x0627, "ا", 0x0641, "ف"),
                uk(0x06D5, "ە", 0x06AF, "گ"),
                uk(0x0649, "ى", 0x062E, "خ"),
                uk(0x0642, "ق", 0x062C, "ج"),
                uk(0x0643, "ك", 0x06C6, "ۆ"),
                uk(0x0644, "ل")
            ),
            listOf(
                KeyDef(-1, "⇧", isSpecial = true, widthWeight = 1.2f),
                uk(0x0632, "ز"), uk(0x0634, "ش"), uk(0x063A, "غ"), uk(0x06C8, "ۈ"),
                uk(0x0628, "ب"), uk(0x0646, "ن"), uk(0x0645, "م"),
                KeyDef(-3, "⌫", isSpecial = true, widthWeight = 1.3f)
            ),
            listOf(
                KeyDef(-6, "123", isSpecial = true, widthWeight = 1.2f),
                KeyDef(-4, "EN", isSpecial = true, widthWeight = 0.8f),
                uk(0x060C, "،"),
                KeyDef(-5, "بوشلۇق", isSpecial = true, widthWeight = 3.5f),
                uk(0x0626, "ئ", 0x06D4, "."),
                KeyDef(-2, "↵", isSpecial = true, widthWeight = 1.5f)
            )
        )
    )

    val NUMBER = KeyboardLayout(
        rows = listOf(
            engRow("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            engRow("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
            listOf(
                KeyDef(-7, "符", isSpecial = true, widthWeight = 1.3f),
                *engRow("!", "\"", "'", ":", ";", ",", "?").toTypedArray(),
                KeyDef(-3, "⌫", isSpecial = true, widthWeight = 1.3f)
            ),
            listOf(
                KeyDef(-4, "ئۇ", isSpecial = true, widthWeight = 1.5f),
                KeyDef(-5, "space", isSpecial = true, widthWeight = 4f),
                KeyDef(0x060C, "،"),
                KeyDef(0x06D4, "."),
                KeyDef(-2, "↵", isSpecial = true, widthWeight = 1.5f)
            )
        )
    )

    val SYMBOL = KeyboardLayout(
        rows = listOf(
            engRow("[", "]", "{", "}", "|", "~", "<", ">", "=", "\\"),
            engRow("°", "²", "³", "¹", "¼", "½", "¾", "×", "÷", "º"),
            listOf(
                KeyDef(-6, "123", isSpecial = true, widthWeight = 1.5f),
                *engRow("_", "^", "€", "£", "¥", "§").toTypedArray(),
                KeyDef(-3, "⌫", isSpecial = true, widthWeight = 1.5f)
            ),
            listOf(
                KeyDef(-4, "ئۇ", isSpecial = true, widthWeight = 1.5f),
                KeyDef(-5, "space", isSpecial = true, widthWeight = 4f),
                KeyDef(0x060C, "،"),
                KeyDef(0x06D4, "."),
                KeyDef(-2, "↵", isSpecial = true, widthWeight = 1.5f)
            )
        )
    )
}
