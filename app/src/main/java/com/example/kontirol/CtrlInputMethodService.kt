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
    private var langSwitch: TextView? = null

    // ===== 引擎 =====
    private var gEngine: GooglePinyinEngine? = null
    private var ktEngine: PinyinEngine? = null
    private var ktDict: DictLoader? = null
    private var useGoogle = false

    // ===== 拼音缓冲区（同步保护） =====
    @Volatile
    private var pinyinBuffer = ""
    private val bufferLock = Any()

    // ===== 引擎同步（仅 Google 引擎需要） =====
    // enginePos 追踪已同步到 native 引擎的字符数
    private var enginePos = 0

    // ===== 异步候选计算 =====
    // 核心改进：所有引擎查询在后台线程执行，主线程只负责 UI 更新
    private val computeThread = HandlerThread("pinyin-compute").apply { start() }
    private val computeHandler = Handler(computeThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var computeGeneration = 0
    private val DEBOUNCE_MS = 30L  // 防抖延迟（缩短到30ms，因为不再阻塞主线程）

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
        // 切模式时立即刷新候选栏（用当前 buffer）
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
                    synchronized(bufferLock) {
                        if (pinyinBuffer.isNotEmpty()) {
                            pinyinBuffer = pinyinBuffer.substring(0, pinyinBuffer.length - 1)
                        }
                    }
                    fullSyncEngine()
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
            -4 -> cycleLanguage()
            else -> {
                if (currentMode == MODE_CHINESE && currentKeyboardType == "main" && code > 0) {
                    val c = code.toChar()
                    if (c.isLetter()) {
                        synchronized(bufferLock) { pinyinBuffer += c.lowercaseChar() }
                        incrSyncEngine(c.lowercaseChar())
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

    // ========== 引擎同步（主线程，轻量 JNI 调用） ==========

    private fun incrSyncEngine(ch: Char) {
        if (!useGoogle) return
        val engine = gEngine ?: return
        try {
            // 增量同步：引擎状态应当已经包含 pinyinBuffer 中除最后一个字符外的所有内容
            if (enginePos != pinyinBuffer.length - 1) {
                fullSyncEngine()
                return
            }
            engine.addLetter(ch)
            enginePos++
        } catch (e: Exception) {
            Log.e(TAG, "incrSync", e)
            enginePos = 0 // 下次触发 fullSync
        }
    }

    private fun fullSyncEngine() {
        if (!useGoogle) return
        val engine = gEngine ?: return
        try {
            engine.reset()
            enginePos = 0
            val buf = synchronized(bufferLock) { pinyinBuffer }
            for (ch in buf) {
                engine.addLetter(ch)
                enginePos++
            }
        } catch (e: Exception) {
            Log.e(TAG, "fullSync", e)
            enginePos = 0
        }
    }

    // ========== 异步候选计算（核心改进） ==========

    private fun scheduleCompute() {
        // 递增代数，使所有旧计算失效
        computeGeneration++
        val gen = computeGeneration

        // 取消后台线程上所有待处理任务
        computeHandler.removeCallbacksAndMessages(null)

        // 取当前 buffer 快照
        val buf = synchronized(bufferLock) { pinyinBuffer }

        // 在后台线程延迟执行计算（防抖：合并快速连续输入）
        computeHandler.postDelayed({
            if (computeGeneration != gen) return@postDelayed  // 已被新输入取消
            val result = doCompute(buf)
            // 结果回到主线程更新 UI
            if (computeGeneration == gen) {
                mainHandler.post { applyResult(result) }
            }
        }, DEBOUNCE_MS)
    }

    /** 在后台线程执行所有重量级查询 */
    private fun doCompute(buf: String): ComputeResult {
        if (buf.isEmpty() || currentMode != MODE_CHINESE) {
            return ComputeResult(emptyList(), "", false)
        }

        try {
            if (useGoogle && gEngine != null) {
                val engine = gEngine!!
                // Google 引擎的 getCandidates 是重量级 JNI 调用（nativeImSearch + 32x nativeImGetChoice）
                // 这里在后台线程执行，不会阻塞主线程触控
                val candidates: List<String>
                val pyStr: String
                synchronized(engine) {
                    candidates = engine.getCandidates(32)
                    pyStr = engine.getPyStr(true) ?: buf
                }
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

    /** 在主线程应用计算结果（仅更新 UI） */
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

    private fun selectCandidate(index: Int) {
        if (index >= allCandidates.size) return
        val sel = allCandidates[index]
        if (sel.isEmpty()) return
        try {
            currentInputConnection?.commitText(sel, 1)
        } catch (e: Exception) {
            Log.e(TAG, "commitText", e)
        }
        // 清空缓冲区
        synchronized(bufferLock) { pinyinBuffer = "" }
        enginePos = 0
        gEngine?.reset()
        allCandidates = emptyList()
        hideCandidates()
    }

    private fun commitPinyinBuffer() {
        val buf = synchronized(bufferLock) {
            if (pinyinBuffer.isEmpty()) return
            pinyinBuffer.also { pinyinBuffer = "" }
        }
        currentInputConnection?.commitText(buf, 1)
        enginePos = 0
        gEngine?.reset()
        allCandidates = emptyList()
        hideCandidates()
    }

    private fun cycleLanguage() {
        commitPinyinBuffer()
        currentMode = when (currentMode) { MODE_UYGHUR -> MODE_ENGLISH; MODE_ENGLISH -> MODE_CHINESE; MODE_CHINESE -> MODE_UYGHUR; else -> MODE_UYGHUR }
        currentKeyboardType = "main"; langSwitch?.text = modeLabelText(); loadKeyboardLayout()
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
