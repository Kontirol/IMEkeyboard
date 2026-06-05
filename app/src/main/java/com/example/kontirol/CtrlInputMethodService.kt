package com.example.kontirol

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
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

    // 三个语言标签
    private var langLabelZh: TextView? = null
    private var langLabelEn: TextView? = null
    private var langLabelUg: TextView? = null

    // ===== 引擎 =====
    private var gEngine: GooglePinyinEngine? = null
    private var ktEngine: PinyinEngine? = null
    private var ktDict: DictLoader? = null
    private var useGoogle = false

    // ===== 拼音缓冲区 =====
    @Volatile
    private var pinyinBuffer = ""

    // ===== 异步候选计算 =====
    private val computeThread = HandlerThread("pinyin-compute").apply { start() }
    private val computeHandler = Handler(computeThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var computeGeneration = 0
    private val DEBOUNCE_MS = 16L  // 一帧

    override fun onCreateInputView(): View {
        // 双引擎始终初始化：Google 用于候选生成，自研用于 tryConsumePartial fallback
        try {
            val ge = GooglePinyinEngine(this)
            if (ge.init()) { gEngine = ge; useGoogle = true }
        } catch (_: Exception) {}

        try { ktDict = DictLoader(resources.assets) } catch (_: Exception) {}
        ktEngine = PinyinEngine()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#D1D3D9"))
        }
        candidateArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp2px(44))
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

        // ===== 顶部语言切换栏 =====
        val modeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp2px(48))
            setBackgroundColor(Color.parseColor("#D1D3D9")); gravity = Gravity.CENTER_VERTICAL; setPadding(dp2px(12), 0, dp2px(12), 0)
        }
        modeBar.addView(TextView(this).apply {
            text = "Ctrl"; textSize = 10f; setTextColor(Color.parseColor("#8E8E93"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val sep = { modeBar.addView(TextView(this).apply {
            text = "|"; textSize = 12f; setTextColor(Color.parseColor("#A0A2AA"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })}

        val langLabelStyle = { tv: TextView ->
            tv.textSize = 15f; tv.setPadding(dp2px(8), dp2px(3), dp2px(8), dp2px(3))
            tv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        langLabelZh = TextView(this).apply {
            text = "中"; langLabelStyle(this)
            setOnClickListener { switchTo(MODE_CHINESE) }
        }
        modeBar.addView(langLabelZh); sep()

        langLabelEn = TextView(this).apply {
            text = "EN"; langLabelStyle(this)
            setOnClickListener { switchTo(MODE_ENGLISH) }
        }
        modeBar.addView(langLabelEn); sep()

        langLabelUg = TextView(this).apply {
            text = "ئۇ"; langLabelStyle(this)
            setOnClickListener { switchTo(MODE_UYGHUR) }
        }
        modeBar.addView(langLabelUg)

        root.addView(modeBar)
        keyboardView = CtrlKeyboardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            onKeyAction = { key, isLong -> safeHandleKey(key, isLong) }
        }
        root.addView(keyboardView)
        loadKeyboardLayout(); refreshLangHighlight()
        return root
    }

    private fun switchTo(mode: Int) {
        if (currentMode == mode) return
        commitPinyinBuffer()
        currentMode = mode; currentKeyboardType = "main"
        refreshLangHighlight(); loadKeyboardLayout()
    }

    private fun refreshLangHighlight() {
        val selColor = Color.parseColor("#007AFF"); val selBg = Color.parseColor("#D0D4DB")
        val normColor = Color.parseColor("#6E6E78"); val normBg = Color.TRANSPARENT

        fun style(tv: TextView?, active: Boolean) {
            tv?.setTextColor(if (active) selColor else normColor)
            tv?.setBackgroundColor(if (active) selBg else normBg)
            tv?.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
        style(langLabelZh, currentMode == MODE_CHINESE)
        style(langLabelEn, currentMode == MODE_ENGLISH)
        style(langLabelUg, currentMode == MODE_UYGHUR)
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
        scheduleCompute()
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
                    pinyinBuffer = pinyinBuffer.substring(0, pinyinBuffer.length - 1)
                    scheduleCompute()
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
            -4 -> switchTo(when (currentMode) { MODE_UYGHUR -> MODE_ENGLISH; MODE_ENGLISH -> MODE_CHINESE; MODE_CHINESE -> MODE_UYGHUR; else -> MODE_UYGHUR })
            else -> {
                if (currentMode == MODE_CHINESE && currentKeyboardType == "main" && code > 0) {
                    val c = code.toChar()
                    if (c.isLetter()) {
                        pinyinBuffer += c.lowercaseChar()
                        scheduleCompute()
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

    // ========== 异步候选计算（核心） ==========

    private fun scheduleCompute() {
        computeGeneration++
        val gen = computeGeneration
        computeHandler.removeCallbacksAndMessages(null)
        val buf = pinyinBuffer

        computeHandler.postDelayed({
            if (computeGeneration != gen) return@postDelayed
            val result = doCompute(buf)
            if (computeGeneration == gen) {
                mainHandler.post { applyResult(result) }
            }
        }, DEBOUNCE_MS)
    }

    /** 在后台线程执行所有 engine 操作。
     *  主线程永不触碰 engine，避免 JNI 锁阻塞触控。 */
    private fun doCompute(buf: String): ComputeResult {
        if (buf.isEmpty() || currentMode != MODE_CHINESE) {
            return ComputeResult(emptyList(), "", false)
        }

        try {
            if (useGoogle && gEngine != null) {
                val engine = gEngine!!
                engine.reset()
                for (ch in buf) {
                    engine.addLetter(ch)
                }
                val candidates = engine.getCandidates(32)
                val pyStr = engine.getPyStr(true) ?: buf
                return ComputeResult(candidates, pyStr, true)
            } else {
                val seg = ktEngine?.segment(buf) ?: return ComputeResult(emptyList(), buf, true)
                val completed = seg.completed; val active = seg.active
                val words = ktDict?.getCandidates(completed, active, limit = 50) ?: emptyList()

                if (words.isNotEmpty()) {
                    val label = completed.joinToString(" ") + if (active.isNotEmpty()) " $active" else ""
                    return ComputeResult(words, label, true)
                } else if (completed.size >= 2 && active.isEmpty()) {
                    val chars = ktDict?.charDict?.get(completed[0])?.take(50)?.map { it.first } ?: emptyList()
                    val label = "[" + completed[0] + "] " + completed.drop(1).joinToString(" ")
                    return ComputeResult(chars, label, true)
                } else if (completed.size == 1 && active.isEmpty()) {
                    val chars = ktDict?.charDict?.get(completed[0])?.take(50)?.map { it.first } ?: emptyList()
                    return ComputeResult(chars, completed[0], true)
                } else if (active.isNotEmpty()) {
                    val chars = ktDict?.getCharCandidatesByPrefix(active, limit = 12) ?: emptyList()
                    val label = completed.joinToString(" ").let { if (it.isNotEmpty()) "$it $active" else active }
                    return ComputeResult(chars, label, true)
                } else {
                    return ComputeResult(emptyList(), buf, true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "doCompute", e)
            return ComputeResult(emptyList(), "", false)
        }
    }

    private fun applyResult(result: ComputeResult) {
        if (currentMode != MODE_CHINESE || !result.visible) {
            hideCandidates()
            return
        }
        allCandidates = result.candidates
        candidatePage = 0
        pinyinLabel?.text = result.label
        renderCandidatePage()
        candidateArea?.visibility = View.VISIBLE
    }

    data class ComputeResult(
        val candidates: List<String>,
        val label: String,
        val visible: Boolean
    )

    // ========== 候选栏 UI ==========

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

    /** 选中候选词 — 部分消费拼音。
     *  "nishi" 选 "拟"(只消耗 "ni") → 保留 "shi" 继续选字。 */
    private fun selectCandidate(index: Int) {
        if (index >= allCandidates.size) return
        val sel = allCandidates[index]
        if (sel.isEmpty()) return
        try {
            currentInputConnection?.commitText(sel, 1)
        } catch (e: Exception) {
            Log.e(TAG, "commitText", e)
        }

        val consumed = tryConsumePartial(sel)
        if (consumed > 0 && consumed < pinyinBuffer.length) {
            // 还有剩余拼音 → 保留并重新计算候选
            pinyinBuffer = pinyinBuffer.substring(consumed)
            scheduleCompute()
            return
        }

        // 全部消费或无法确定 → 清空
        pinyinBuffer = ""
        allCandidates = emptyList()
        hideCandidates()
    }

    /** 返回已消费的拼音字符数。0 = 无法确定（全清）。
     *  优先使用分词+字典反查。此方法不依赖引擎状态，仅做纯数据查表。 */
    private fun tryConsumePartial(sel: String): Int {
        if (ktEngine == null || ktDict == null) return 0
        val seg = ktEngine!!.segment(pinyinBuffer)
        val completed = seg.completed
        if (completed.isEmpty()) return 0

        if (sel.length == 1) {
            // 单字：在 completed 音节中逐个查找，找到即止
            var consumedLen = 0
            for (syl in completed) {
                consumedLen += syl.length
                val chars = ktDict!!.charDict[syl]?.map { it.first } ?: emptyList()
                if (sel in chars) return consumedLen
            }
            return 0
        } else {
            // 多字词：按拼音前缀从短到长匹配 wordDict
            var consumedLen = 0
            for (i in completed.indices) {
                consumedLen += completed[i].length
                val key = completed.take(i + 1).joinToString("")
                val words = ktDict!!.wordDict[key]?.map { it.first } ?: emptyList()
                if (sel in words) return consumedLen
            }
            return 0
        }
    }

    private fun commitPinyinBuffer() {
        if (pinyinBuffer.isEmpty()) return
        val buf = pinyinBuffer
        pinyinBuffer = ""
        currentInputConnection?.commitText(buf, 1)
        allCandidates = emptyList()
        hideCandidates()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting); loadKeyboardLayout() }
    override fun onFinishInputView(finishingInput: Boolean) { super.onFinishInputView(finishingInput); commitPinyinBuffer() }
    override fun onDestroy() {
        computeThread.quitSafely()
        gEngine?.close()
        super.onDestroy()
    }

    private fun dp2px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
