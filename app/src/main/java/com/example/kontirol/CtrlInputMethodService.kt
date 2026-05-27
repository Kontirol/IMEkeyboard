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
    private var langSwitch: TextView? = null
    private var pinyinBuffer = StringBuilder()

    private var gEngine: GooglePinyinEngine? = null
    private var ktEngine: PinyinEngine? = null
    private var ktDict: DictLoader? = null
    private var useGoogle = false

    // 增量同步：记录已同步到引擎的字符数，避免每次 reset+重建
    private var enginePos = 0

    override fun onCreateInputView(): View {
        try {
            val ge = GooglePinyinEngine(this)
            if (ge.init()) { gEngine = ge; useGoogle = true }
        } catch (_: Exception) {}

        if (!useGoogle) {
            try { ktDict = DictLoader(resources.assets) } catch (_: Exception) {}
            ktEngine = PinyinEngine()
        }

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
            gravity = Gravity.CENTER; text = ">"; textSize = 20f
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

    private fun modeLabelText() = when (currentMode) { MODE_UYGHUR -> "ئۇ"; MODE_ENGLISH -> "EN"; MODE_CHINESE -> "中"; else -> "EN" }

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
        try { handleKey(key, isLong) } catch (e: Exception) { Log.e(TAG, "handleKey", e) }
    }

    private fun handleKey(key: KeyDef, isLong: Boolean) {
        val code = key.code; val ic = currentInputConnection
        when (code) {
            -1 -> keyboardView?.toggleShift()
            -3 -> {
                if (currentMode == MODE_CHINESE && pinyinBuffer.isNotEmpty()) {
                    pinyinBuffer.deleteCharAt(pinyinBuffer.length - 1)
                    fullSyncEngine()
                    updateCandidates()
                } else ic?.deleteSurroundingText(1, 0)
            }
            -2 -> { commitPinyinBuffer(); ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)); ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)) }
            -5 -> {
                when {
                    currentMode == MODE_CHINESE && pinyinBuffer.isNotEmpty() && allCandidates.isNotEmpty() -> selectCandidate(0)
                    currentMode == MODE_CHINESE && pinyinBuffer.isNotEmpty() -> commitPinyinBuffer()
                    else -> ic?.commitText(" ", 1)
                }
                keyboardView?.resetShift()
            }
            -6 -> { commitPinyinBuffer(); currentKeyboardType = if (currentKeyboardType == "main") "number" else "main"; loadKeyboardLayout() }
            -7 -> { commitPinyinBuffer(); currentKeyboardType = if (currentKeyboardType == "symbol") "main" else "symbol"; loadKeyboardLayout() }
            -4 -> cycleLanguage()
            else -> {
                if (currentMode == MODE_CHINESE && currentKeyboardType == "main" && code > 0) {
                    val c = code.toChar()
                    if (c.isLetter()) {
                        pinyinBuffer.append(c.lowercaseChar())
                        incrSyncEngine(c.lowercaseChar())
                        updateCandidates()
                    } else {
                        commitPinyinBuffer()
                        ic?.commitText(c.toString(), 1)
                    }
                } else if (currentMode == MODE_ENGLISH && keyboardView?.isShifted == true && code > 0) {
                    ic?.commitText(code.toChar().uppercaseChar().toString(), 1)
                    keyboardView?.resetShift()
                } else if (code > 0) {
                    ic?.commitText(code.toChar().toString(), 1)
                    if (keyboardView?.isShifted == true) keyboardView?.resetShift()
                }
            }
        }
    }

    // 增量同步：只追加新字母，不重建
    private fun incrSyncEngine(ch: Char) {
        if (!useGoogle) return
        val engine = gEngine ?: return
        try {
            if (enginePos != pinyinBuffer.length - 1) {
                // 引擎不同步（删过字/切过模式等），全量重建
                fullSyncEngine()
                return
            }
            engine.addLetter(ch)
            enginePos++
        } catch (e: Exception) {
            Log.e(TAG, "incrSync", e)
            enginePos = 0
        }
    }

    // 全量重建：reset + 逐字添加
    private fun fullSyncEngine() {
        if (!useGoogle) return
        val engine = gEngine ?: return
        try {
            engine.reset()
            enginePos = 0
            for (ch in pinyinBuffer) {
                engine.addLetter(ch)
                enginePos++
            }
        } catch (e: Exception) {
            Log.e(TAG, "fullSync", e)
            enginePos = 0
        }
    }

    private fun updateCandidates() {
        if (currentMode != MODE_CHINESE || pinyinBuffer.isEmpty()) { hideCandidates(); return }
        try {
            if (useGoogle && gEngine != null) {
                allCandidates = gEngine!!.getCandidates(32)
                pinyinLabel?.text = gEngine!!.getPyStr(true) ?: pinyinBuffer.toString()
            } else {
                val seg = ktEngine?.segment(pinyinBuffer.toString()) ?: run { hideCandidates(); return }
                val completed = seg.completed; val active = seg.active
                val words = ktDict?.getCandidates(completed, active, limit = 50) ?: emptyList()
                if (words.isNotEmpty()) {
                    allCandidates = words
                    pinyinLabel?.text = completed.joinToString(" ") + if (active.isNotEmpty()) " $active" else ""
                } else if (completed.size >= 2 && active.isEmpty()) {
                    allCandidates = getCharCandidates(completed[0])
                    pinyinLabel?.text = "[" + completed[0] + "] " + completed.drop(1).joinToString(" ")
                } else if (completed.size == 1 && active.isEmpty()) {
                    allCandidates = getCharCandidates(completed[0])
                    pinyinLabel?.text = completed[0]
                } else if (active.isNotEmpty()) {
                    allCandidates = ktDict?.getCharCandidatesByPrefix(active, limit = 12) ?: emptyList()
                    pinyinLabel?.text = completed.joinToString(" ").let { if (it.isNotEmpty()) "$it $active" else active }
                } else { allCandidates = emptyList(); pinyinLabel?.text = pinyinBuffer.toString() }
            }
            candidatePage = 0
            renderCandidatePage()
            candidateArea?.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "updateCandidates", e)
            hideCandidates()
        }
    }

    private fun getCharCandidates(syl: String): List<String> {
        return ktDict?.charDict?.get(syl)?.take(50)?.map { it.first } ?: emptyList()
    }

    private fun hideCandidates() {
        allCandidates = emptyList(); candidatePage = 0; pinyinLabel?.text = ""
        for (i in 0 until CANDIDATES_PER_PAGE) candidateTexts[i]?.text = ""
        candidateNextBtn?.visibility = View.GONE; candidateArea?.visibility = View.GONE
    }

    private fun renderCandidatePage() {
        val start = candidatePage * CANDIDATES_PER_PAGE
        val page = allCandidates.drop(start).take(CANDIDATES_PER_PAGE)
        for (i in 0 until CANDIDATES_PER_PAGE) candidateTexts[i]?.text = page.getOrNull(i) ?: ""
        candidateNextBtn?.visibility = if (start + CANDIDATES_PER_PAGE < allCandidates.size) View.VISIBLE else View.GONE
    }

    private fun nextCandidatePage() {
        val tp = (allCandidates.size + CANDIDATES_PER_PAGE - 1) / CANDIDATES_PER_PAGE
        candidatePage = (candidatePage + 1) % tp; renderCandidatePage()
    }

    private fun selectCandidate(index: Int) {
        if (index >= allCandidates.size) return
        val sel = allCandidates[index]
        if (sel.isEmpty()) return
        try {
            currentInputConnection?.commitText(sel, 1)
        } catch (e: Exception) {
            Log.e(TAG, "commitText", e)
        }
        pinyinBuffer.clear()
        enginePos = 0
        gEngine?.reset()
        updateCandidates()
    }

    private fun commitPinyinBuffer() {
        if (pinyinBuffer.isNotEmpty()) {
            currentInputConnection?.commitText(pinyinBuffer.toString(), 1)
            pinyinBuffer.clear()
            enginePos = 0
            gEngine?.reset()
            updateCandidates()
        }
    }

    private fun cycleLanguage() {
        commitPinyinBuffer()
        currentMode = when (currentMode) { MODE_UYGHUR -> MODE_ENGLISH; MODE_ENGLISH -> MODE_CHINESE; MODE_CHINESE -> MODE_UYGHUR; else -> MODE_UYGHUR }
        currentKeyboardType = "main"; langSwitch?.text = modeLabelText(); loadKeyboardLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting); loadKeyboardLayout() }
    override fun onFinishInputView(finishingInput: Boolean) { super.onFinishInputView(finishingInput); commitPinyinBuffer() }
    override fun onDestroy() { gEngine?.close(); super.onDestroy() }
    private fun dp2px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
