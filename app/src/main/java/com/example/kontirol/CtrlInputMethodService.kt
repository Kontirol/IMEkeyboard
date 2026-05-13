package com.example.kontirol

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity

class CtrlInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "CtrlKeyboard"
        const val MODE_UYGHUR = 0
        const val MODE_ENGLISH = 1
        const val MODE_CHINESE = 2
        private const val CANDIDATES_PER_PAGE = 5
    }

    private var currentMode = MODE_UYGHUR
    private var currentKeyboardType = "main"
    private var keyboardView: CtrlKeyboardView? = null
    private var candidateArea: LinearLayout? = null
    private var pinyinLabel: TextView? = null
    private var candidateTexts = arrayOfNulls<TextView>(CANDIDATES_PER_PAGE)
    private var candidateNextBtn: TextView? = null
    private var allCandidates = listOf<String>()
    private var candidatePage = 0
    private var activeSyllable: String? = null
    private var langSwitch: TextView? = null
    private var pinyinBuffer = StringBuilder()
    private val pinyinEngine = PinyinEngine()
    private var dictLoader: DictLoader? = null

    override fun onCreateInputView(): View {
        try { dictLoader = DictLoader(resources.assets) } catch (_: Exception) {}
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#D1D3D9"))
        }
        candidateArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp2px(40))
            setBackgroundColor(Color.parseColor("#E8E8ED")); gravity = Gravity.CENTER_VERTICAL
            setPadding(dp2px(8), 0, dp2px(4), 0); visibility = View.GONE
        }
        pinyinLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER; textSize = 13f; setTextColor(Color.parseColor("#8E8E93"))
            setTypeface(Typeface.MONOSPACE); setPadding(0, 0, dp2px(10), 0); setSingleLine(true)
        }
        candidateArea?.addView(pinyinLabel)
        candidateArea?.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp2px(1), dp2px(24)); setBackgroundColor(Color.parseColor("#C7C7CC"))
        })
        for (i in 0 until CANDIDATES_PER_PAGE) {
            val idx = i
            candidateTexts[i] = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.CENTER; textSize = 15f; setTextColor(Color.parseColor("#000000"))
                setOnClickListener { selectCandidate(candidatePage * CANDIDATES_PER_PAGE + idx) }
            }
            candidateArea?.addView(candidateTexts[i])
        }
        candidateNextBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp2px(32), LinearLayout.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER; text = "›"; textSize = 20f
            setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#8E8E93"))
            visibility = View.GONE; setOnClickListener { nextCandidatePage() }
        }
        candidateArea?.addView(candidateNextBtn); root.addView(candidateArea)
        val modeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp2px(28))
            setBackgroundColor(Color.parseColor("#D1D3D9")); gravity = Gravity.CENTER; setPadding(dp2px(12), 0, dp2px(12), 0)
        }
        modeBar.addView(TextView(this).apply {
            text = "Ctrl"; textSize = 10f; setTextColor(Color.parseColor("#8E8E93"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        langSwitch = TextView(this).apply {
            text = modeLabelText(); textSize = 13f; setTextColor(Color.parseColor("#000000"))
            setPadding(dp2px(10), dp2px(3), dp2px(10), dp2px(3))
            setBackgroundColor(Color.parseColor("#C0C2C8")); setOnClickListener { cycleLanguage() }
        }
        modeBar.addView(langSwitch); root.addView(modeBar)
        keyboardView = CtrlKeyboardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            onKeyAction = { key, isLong -> safeHandleKey(key, isLong) }
        }
        root.addView(keyboardView)
        loadKeyboardLayout(); return root
    }

    private fun modeLabelText() = when (currentMode) {
        MODE_UYGHUR -> "ئۇ"; MODE_ENGLISH -> "EN"; MODE_CHINESE -> "中"; else -> "EN"
    }

    private fun loadKeyboardLayout() {
        when (currentKeyboardType) {
            "number" -> keyboardView?.setLayout(KeyboardLayouts.NUMBER)
            "symbol" -> keyboardView?.setLayout(KeyboardLayouts.SYMBOL)
            else -> when (currentMode) {
                MODE_UYGHUR -> keyboardView?.setLayout(KeyboardLayouts.UYGHUR)
                MODE_ENGLISH -> keyboardView?.setLayout(KeyboardLayouts.ENGLISH)
                MODE_CHINESE -> keyboardView?.setLayout(KeyboardLayouts.CHINESE)
            }
        }
        updateCandidates()
    }

    private fun safeHandleKey(key: KeyDef, isLong: Boolean) {
        try { handleKey(key, isLong) } catch (e: Exception) { Log.e(TAG, "err", e) }
    }

    private fun handleKey(key: KeyDef, isLong: Boolean) {
        val code = key.code; val ic = currentInputConnection
        when (code) {
            -1 -> keyboardView?.toggleShift()
            -3 -> { if (currentMode == MODE_CHINESE && pinyinBuffer.isNotEmpty()) { pinyinBuffer.deleteCharAt(pinyinBuffer.length - 1); updateCandidates() } else ic?.deleteSurroundingText(1, 0) }
            -2 -> { commitPinyinBuffer(); ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)); ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) }
            -5 -> { if (currentMode == MODE_CHINESE && pinyinBuffer.isNotEmpty() && allCandidates.isNotEmpty()) selectCandidate(0) else ic?.commitText(" ", 1); keyboardView?.resetShift() }
            -6 -> { commitPinyinBuffer(); currentKeyboardType = if (currentKeyboardType == "main") "number" else "main"; loadKeyboardLayout() }
            -7 -> { currentKeyboardType = if (currentKeyboardType == "symbol") "main" else "symbol"; loadKeyboardLayout() }
            -4 -> { commitPinyinBuffer(); currentMode = if (currentMode == MODE_UYGHUR) MODE_ENGLISH else MODE_UYGHUR; currentKeyboardType = "main"; langSwitch?.text = modeLabelText(); loadKeyboardLayout() }
            else -> {
                if (currentMode == MODE_CHINESE && currentKeyboardType == "main" && code > 0) {
                    val c = code.toChar()
                    if (c.isLetter()) { pinyinBuffer.append(c.lowercaseChar()); updateCandidates(); if (allCandidates.isEmpty()) commitPinyinBuffer() }
                    else { commitPinyinBuffer(); ic?.commitText(c.toString(), 1) }
                } else if (currentMode == MODE_ENGLISH && keyboardView?.isShifted == true && code > 0) {
                    ic?.commitText(code.toChar().uppercaseChar().toString(), 1); keyboardView?.resetShift()
                } else if (code > 0) {
                    ic?.commitText(code.toChar().toString(), 1); if (keyboardView?.isShifted == true) keyboardView?.resetShift()
                }
            }
        }
    }

    private fun updateCandidates() {
        if (currentMode == MODE_CHINESE && pinyinBuffer.isNotEmpty()) {
            activeSyllable = pinyinEngine.getActiveSyllable(pinyinBuffer.toString())
            val rawChars = pinyinEngine.getCandidates(pinyinBuffer.toString(), limit = 100)
            allCandidates = dictLoader?.getCandidates(pinyinBuffer.toString(), activeSyllable, rawChars, limit = 100) ?: rawChars
            candidatePage = 0; pinyinLabel?.text = pinyinBuffer.toString()
            renderCandidatePage(); candidateArea?.visibility = View.VISIBLE
        } else {
            activeSyllable = null; allCandidates = emptyList(); candidatePage = 0; pinyinLabel?.text = ""
            for (i in 0 until CANDIDATES_PER_PAGE) candidateTexts[i]?.text = ""
            candidateNextBtn?.visibility = View.GONE; candidateArea?.visibility = View.GONE
        }
    }

    private fun renderCandidatePage() {
        val start = candidatePage * CANDIDATES_PER_PAGE
        val pageItems = allCandidates.drop(start).take(CANDIDATES_PER_PAGE)
        for (i in 0 until CANDIDATES_PER_PAGE) candidateTexts[i]?.text = pageItems.getOrNull(i) ?: ""
        candidateNextBtn?.visibility = if (start + CANDIDATES_PER_PAGE < allCandidates.size) View.VISIBLE else View.GONE
    }

    private fun nextCandidatePage() {
        val totalPages = (allCandidates.size + CANDIDATES_PER_PAGE - 1) / CANDIDATES_PER_PAGE
        candidatePage = (candidatePage + 1) % totalPages; renderCandidatePage()
    }

    private fun selectCandidate(index: Int) {
        if (index >= allCandidates.size) return
        currentInputConnection?.commitText(allCandidates[index], 1)
        val syl = activeSyllable
        if (syl != null) {
            val idx = pinyinBuffer.lastIndexOf(syl)
            if (idx >= 0) pinyinBuffer.delete(idx, pinyinBuffer.length) else pinyinBuffer.clear()
        } else pinyinBuffer.clear()
        updateCandidates()
    }

    private fun commitPinyinBuffer() {
        if (pinyinBuffer.isNotEmpty()) { currentInputConnection?.commitText(pinyinBuffer.toString(), 1); pinyinBuffer.clear(); updateCandidates() }
    }

    private fun cycleLanguage() {
        commitPinyinBuffer()
        currentMode = when (currentMode) { MODE_UYGHUR -> MODE_ENGLISH; MODE_ENGLISH -> MODE_CHINESE; MODE_CHINESE -> MODE_UYGHUR; else -> MODE_UYGHUR }
        currentKeyboardType = "main"; langSwitch?.text = modeLabelText(); loadKeyboardLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting); loadKeyboardLayout() }
    override fun onFinishInputView(finishingInput: Boolean) { super.onFinishInputView(finishingInput); commitPinyinBuffer() }
    private fun dp2px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
