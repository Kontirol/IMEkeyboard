package com.example.kontirol

import android.content.Context
import android.graphics.Color

data class KeyboardTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    // 键盘区域
    val bgColor: Int,
    val keyColor: Int,
    val keySpecialColor: Int,
    val keyPressedColor: Int,
    val keySpecialPressedColor: Int,
    val keyTextColor: Int,
    val keySpecialTextColor: Int,
    val keyShadowColor: Int,
    val popupColor: Int,
    val popupTextColor: Int,
    val longPressHintColor: Int,
    // 候选栏
    val candidateBgColor: Int,
    val candidateTextColor: Int,
    val pinyinLabelColor: Int,
    val candidateSepColor: Int,
    // 模式切换栏
    val modeBarBgColor: Int,
    val brandTextColor: Int,
    val modeSepColor: Int,
    val modeActiveColor: Int,
    val modeActiveBgColor: Int,
    val modeInactiveColor: Int,
) {
    companion object {

        val LIGHT = KeyboardTheme(
            id = "light", name = "ئاقلىق", isDark = false,
            bgColor = Color.parseColor("#D1D3D9"),
            keyColor = Color.parseColor("#FFFFFF"),
            keySpecialColor = Color.parseColor("#ADB0B8"),
            keyPressedColor = Color.parseColor("#C7C9CD"),
            keySpecialPressedColor = Color.parseColor("#8E9096"),
            keyTextColor = Color.parseColor("#000000"),
            keySpecialTextColor = Color.parseColor("#000000"),
            keyShadowColor = Color.parseColor("#9A9CA3"),
            popupColor = Color.parseColor("#1C1C1E"),
            popupTextColor = Color.WHITE,
            longPressHintColor = Color.parseColor("#999999"),
            candidateBgColor = Color.parseColor("#E8E8ED"),
            candidateTextColor = Color.parseColor("#000000"),
            pinyinLabelColor = Color.parseColor("#8E8E93"),
            candidateSepColor = Color.parseColor("#C7C7CC"),
            modeBarBgColor = Color.parseColor("#D1D3D9"),
            brandTextColor = Color.parseColor("#8E8E93"),
            modeSepColor = Color.parseColor("#A0A2AA"),
            modeActiveColor = Color.parseColor("#007AFF"),
            modeActiveBgColor = Color.parseColor("#D0D4DB"),
            modeInactiveColor = Color.parseColor("#6E6E78"),
        )

        val DARK = KeyboardTheme(
            id = "dark", name = "قاراڭغۇ", isDark = true,
            bgColor = Color.parseColor("#1C1C1E"),
            keyColor = Color.parseColor("#2C2C2E"),
            keySpecialColor = Color.parseColor("#3A3A3C"),
            keyPressedColor = Color.parseColor("#555557"),
            keySpecialPressedColor = Color.parseColor("#6E6E70"),
            keyTextColor = Color.WHITE,
            keySpecialTextColor = Color.WHITE,
            keyShadowColor = Color.parseColor("#000000"),
            popupColor = Color.parseColor("#636366"),
            popupTextColor = Color.WHITE,
            longPressHintColor = Color.parseColor("#8E8E93"),
            candidateBgColor = Color.parseColor("#2C2C2E"),
            candidateTextColor = Color.WHITE,
            pinyinLabelColor = Color.parseColor("#8E8E93"),
            candidateSepColor = Color.parseColor("#545458"),
            modeBarBgColor = Color.parseColor("#1C1C1E"),
            brandTextColor = Color.parseColor("#8E8E93"),
            modeSepColor = Color.parseColor("#545458"),
            modeActiveColor = Color.parseColor("#0A84FF"),
            modeActiveBgColor = Color.parseColor("#2C2C2E"),
            modeInactiveColor = Color.parseColor("#636366"),
        )

        val MIDNIGHT = KeyboardTheme(
            id = "midnight", name = "كېچە", isDark = true,
            bgColor = Color.parseColor("#0D1B2A"),
            keyColor = Color.parseColor("#1B2838"),
            keySpecialColor = Color.parseColor("#253545"),
            keyPressedColor = Color.parseColor("#344860"),
            keySpecialPressedColor = Color.parseColor("#4A6280"),
            keyTextColor = Color.parseColor("#E0E8F0"),
            keySpecialTextColor = Color.parseColor("#E0E8F0"),
            keyShadowColor = Color.parseColor("#060C14"),
            popupColor = Color.parseColor("#344860"),
            popupTextColor = Color.WHITE,
            longPressHintColor = Color.parseColor("#6B8DB8"),
            candidateBgColor = Color.parseColor("#1B2838"),
            candidateTextColor = Color.parseColor("#E0E8F0"),
            pinyinLabelColor = Color.parseColor("#6B8DB8"),
            candidateSepColor = Color.parseColor("#344860"),
            modeBarBgColor = Color.parseColor("#0D1B2A"),
            brandTextColor = Color.parseColor("#6B8DB8"),
            modeSepColor = Color.parseColor("#344860"),
            modeActiveColor = Color.parseColor("#5B9BD5"),
            modeActiveBgColor = Color.parseColor("#1B2838"),
            modeInactiveColor = Color.parseColor("#6B8DB8"),
        )

        val FOREST = KeyboardTheme(
            id = "forest", name = "ئورمان", isDark = false,
            bgColor = Color.parseColor("#DCE8DC"),
            keyColor = Color.parseColor("#F0F5F0"),
            keySpecialColor = Color.parseColor("#B8CCB8"),
            keyPressedColor = Color.parseColor("#C8D8C8"),
            keySpecialPressedColor = Color.parseColor("#8CA88C"),
            keyTextColor = Color.parseColor("#1A3A1A"),
            keySpecialTextColor = Color.parseColor("#1A3A1A"),
            keyShadowColor = Color.parseColor("#A0B8A0"),
            popupColor = Color.parseColor("#2D4A2D"),
            popupTextColor = Color.WHITE,
            longPressHintColor = Color.parseColor("#7A9A7A"),
            candidateBgColor = Color.parseColor("#E8F0E8"),
            candidateTextColor = Color.parseColor("#1A3A1A"),
            pinyinLabelColor = Color.parseColor("#5A7A5A"),
            candidateSepColor = Color.parseColor("#B8CCB8"),
            modeBarBgColor = Color.parseColor("#DCE8DC"),
            brandTextColor = Color.parseColor("#5A7A5A"),
            modeSepColor = Color.parseColor("#B0C0B0"),
            modeActiveColor = Color.parseColor("#2D7A2D"),
            modeActiveBgColor = Color.parseColor("#C8D8C8"),
            modeInactiveColor = Color.parseColor("#6A8A6A"),
        )

        val SUNSET = KeyboardTheme(
            id = "sunset", name = "شەپەق", isDark = false,
            bgColor = Color.parseColor("#F5E6DC"),
            keyColor = Color.parseColor("#FFF5F0"),
            keySpecialColor = Color.parseColor("#E8D0C0"),
            keyPressedColor = Color.parseColor("#F0D8C8"),
            keySpecialPressedColor = Color.parseColor("#D4A888"),
            keyTextColor = Color.parseColor("#3A1A0A"),
            keySpecialTextColor = Color.parseColor("#3A1A0A"),
            keyShadowColor = Color.parseColor("#D0B8A8"),
            popupColor = Color.parseColor("#5A3020"),
            popupTextColor = Color.WHITE,
            longPressHintColor = Color.parseColor("#B89888"),
            candidateBgColor = Color.parseColor("#FFF0E8"),
            candidateTextColor = Color.parseColor("#3A1A0A"),
            pinyinLabelColor = Color.parseColor("#A08070"),
            candidateSepColor = Color.parseColor("#E8D0C0"),
            modeBarBgColor = Color.parseColor("#F5E6DC"),
            brandTextColor = Color.parseColor("#A08070"),
            modeSepColor = Color.parseColor("#E0C8B8"),
            modeActiveColor = Color.parseColor("#D4602A"),
            modeActiveBgColor = Color.parseColor("#F0D8C8"),
            modeInactiveColor = Color.parseColor("#A08878"),
        )

        val presets = listOf(LIGHT, DARK, MIDNIGHT, FOREST, SUNSET)

        private const val PREFS_NAME = "keyboard_themes"
        private const val KEY_CURRENT = "current_theme_id"

        fun current(context: Context): KeyboardTheme {
            val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT, "light") ?: "light"
            return presets.find { it.id == id } ?: LIGHT
        }

        fun save(context: Context, theme: KeyboardTheme) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CURRENT, theme.id).apply()
        }
    }
}
