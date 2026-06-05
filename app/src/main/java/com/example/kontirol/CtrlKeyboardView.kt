package com.example.kontirol

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class CtrlKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var theme: KeyboardTheme = KeyboardTheme.LIGHT
        set(value) { field = value; invalidate() }

    private var ukijTypeface: Typeface? = null
    private var defaultTypeface = Typeface.create("sans-serif", Typeface.NORMAL)

    init { try { ukijTypeface = resources.getFont(R.font.ukij_ekran) } catch (_: Exception) {} }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val keyRects = mutableListOf<RectF>()
    private val keyDefs = mutableListOf<KeyDef>()

    private var layout: KeyboardLayout = KeyboardLayouts.ENGLISH
    var isShifted = false

    private val pressedKeys = mutableMapOf<Int, Int>()
    private val origKey = mutableMapOf<Int, Int>()
    private var firstPointerId = -1
    private var longPressTriggered = false
    private val longPressDelay = 400L
    private val repeatDelay = 55L
    private val handler = Handler(Looper.getMainLooper())
    private var pendingLongPress: Runnable? = null
    private var repeatRunnable: Runnable? = null

    private var popupText: String? = null
    private var popupX = 0f
    private var popupY = 0f
    private var popupVisible = false

    var onKeyAction: ((KeyDef, Boolean) -> Unit)? = null

    private var rowHeight = 0f
    private val keyRadius = 10f
    private val keyMarginH = 5f
    private val keyMarginV = 8f
    private val shadowOffset = 2f

    private val tmpRect = RectF()

    fun setLayout(l: KeyboardLayout) { layout = l; isShifted = false; requestLayout(); invalidate() }
    fun toggleShift() { isShifted = !isShifted; invalidate() }
    fun resetShift() { isShifted = false; invalidate() }

    override fun onMeasure(wms: Int, hms: Int) {
        val w = MeasureSpec.getSize(wms)
        val d = resources.displayMetrics.density
        rowHeight = layout.rowHeightDp * d + 10f
        val rows = layout.rows.size
        setMeasuredDimension(w, (rowHeight * rows + keyMarginV * 2 * (rows + 1)).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        computeKeys(w)
    }

    private fun computeKeys(w: Int) {
        keyRects.clear(); keyDefs.clear()
        val pad = keyMarginH * 2f

        var refUnitW = 0f
        val firstRow = layout.rows.getOrNull(0)
        if (firstRow != null && firstRow.isNotEmpty()) {
            val firstTw = firstRow.sumOf { it.widthWeight.toDouble() }.toFloat()
            val firstAvailW = w - pad * (firstRow.size + 1)
            refUnitW = firstAvailW / firstTw
        }

        var y = keyMarginV
        for ((rowIdx, row) in layout.rows.withIndex()) {
            if (row.isEmpty()) { y += rowHeight; continue }
            val tw = row.sumOf { it.widthWeight.toDouble() }.toFloat()

            if (rowIdx == 1 && refUnitW > 0f) {
                val rowContentW = tw * refUnitW + pad * (row.size - 1)
                val margin = (w - rowContentW) / 2f
                var x = margin
                for (key in row) {
                    val kw = key.widthWeight * refUnitW
                    keyRects.add(RectF(x, y, x + kw, y + rowHeight - keyMarginV * 2))
                    keyDefs.add(key)
                    x += kw + pad
                }
            } else {
                val availW = w - pad * (row.size + 1)
                var x = keyMarginH
                for (key in row) {
                    val kw = (key.widthWeight / tw) * availW
                    keyRects.add(RectF(x, y, x + kw, y + rowHeight - keyMarginV * 2))
                    keyDefs.add(key)
                    x += kw + pad
                }
            }
            y += rowHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(theme.bgColor)
        for (i in keyDefs.indices) { if (i < keyRects.size) drawKey(canvas, i) }
        if (popupVisible && popupText != null) drawPopup(canvas)
    }

    private fun drawKey(canvas: Canvas, i: Int) {
        if (i >= keyRects.size || i >= keyDefs.size) return
        val r = keyRects[i]; val k = keyDefs[i]
        val pressed = pressedKeys.containsValue(i)

        if (!pressed) {
            keyPaint.style = Paint.Style.FILL; keyPaint.color = theme.keyShadowColor
            tmpRect.set(r.left, r.top + shadowOffset, r.right, r.bottom + shadowOffset)
            canvas.drawRoundRect(tmpRect, keyRadius, keyRadius, keyPaint)
        }
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = when {
            pressed && k.isSpecial -> theme.keySpecialPressedColor
            pressed -> theme.keyPressedColor
            k.isSpecial -> theme.keySpecialColor
            else -> theme.keyColor
        }
        val kr = if (pressed) {
            tmpRect.set(r.left, r.top + shadowOffset, r.right, r.bottom + shadowOffset)
            tmpRect
        } else r
        canvas.drawRoundRect(kr, keyRadius, keyRadius, keyPaint)

        val label = keyLabel(k)
        val ug = layout == KeyboardLayouts.UYGHUR
        textPaint.typeface = if (ug && ukijTypeface != null) ukijTypeface else defaultTypeface
        textPaint.color = if (k.isSpecial) theme.keySpecialTextColor else theme.keyTextColor
        textPaint.textSize = when { k.isSpecial -> kr.height() * 0.32f; ug -> kr.height() * 0.44f; else -> kr.height() * 0.40f }
        canvas.drawText(label, kr.centerX(), kr.centerY() - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)

        if (k.longPressLabel != null && !k.isSpecial) {
            smallTextPaint.typeface = if (ug && ukijTypeface != null) ukijTypeface else defaultTypeface
            smallTextPaint.color = theme.longPressHintColor
            smallTextPaint.textSize = kr.height() * 0.20f
            canvas.drawText(k.longPressLabel!!, kr.right - kr.width() * 0.16f, kr.bottom - kr.height() * 0.16f, smallTextPaint)
        }
    }

    private fun keyLabel(k: KeyDef): String = when (k.code) {
        -5 -> if (layout == KeyboardLayouts.UYGHUR) "بوشلۇق" else "space"
        -2 -> "↵"; -3 -> "⌫"; -1 -> if (isShifted) "⇧" else "⇧"
        -6 -> "123"; -7 -> "#+="
        -4 -> if (layout == KeyboardLayouts.UYGHUR) "EN" else "ئۇ"
        else -> when {
            isShifted && layout == KeyboardLayouts.ENGLISH -> k.label.uppercase()
            layout == KeyboardLayouts.CHINESE -> k.label.uppercase()
            else -> k.label
        }
    }

    private fun drawPopup(canvas: Canvas) {
        val t = popupText ?: return
        val pw = 100f; val ph = 110f; val px = popupX - pw / 2; val py = popupY - ph - 12f
        popupPaint.style = Paint.Style.FILL; popupPaint.color = theme.popupColor
        tmpRect.set(px, py, px + pw, py + ph)
        canvas.drawRoundRect(tmpRect, 18f, 18f, popupPaint)
        val tri = Path().apply { moveTo(popupX - 8f, py + ph); lineTo(popupX + 8f, py + ph); lineTo(popupX, py + ph + 8f); close() }
        canvas.drawPath(tri, popupPaint)
        textPaint.typeface = if (layout == KeyboardLayouts.UYGHUR && ukijTypeface != null) ukijTypeface else defaultTypeface
        textPaint.color = theme.popupTextColor; textPaint.textSize = 36f
        canvas.drawText(t, popupX, (py + ph / 2) - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        try {
            val action = e.actionMasked
            val idx = e.actionIndex
            val pid = e.getPointerId(idx)
            val x = e.getX(idx)
            val y = e.getY(idx)

            when (action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val ki = findKey(x, y)
                    if (ki >= 0) {
                        pressedKeys[pid] = ki
                        origKey[pid] = ki
                        if (pressedKeys.size == 1) {
                            firstPointerId = pid
                            longPressTriggered = false
                            showPopup(ki)
                            startLongPress(ki)
                            try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(15L) } catch (_: Exception) {}
                        }
                    }
                    invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until e.pointerCount) {
                        val pi = e.getPointerId(i)
                        val oi = origKey[pi] ?: continue
                        val orig = keyRects[oi]
                        val mx = e.getX(i); val my = e.getY(i)
                        val tol = 30f
                        if (mx < orig.left - tol || mx > orig.right + tol ||
                            my < orig.top - tol || my > orig.bottom + tol) {
                            val ni = findKey(mx, my)
                            pressedKeys[pi] = if (ni >= 0) ni else -1
                        } else {
                            pressedKeys[pi] = oi
                        }
                    }
                    invalidate(); return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val ki = origKey[pid]
                    if (ki != null && !longPressTriggered) fireKey(ki, false)
                    if (pid == firstPointerId) { stopAll(); hidePopup() }
                    pressedKeys.remove(pid); origKey.remove(pid)
                    longPressTriggered = false
                    invalidate(); return true
                }
                MotionEvent.ACTION_UP -> {
                    stopAll(); hidePopup()
                    val ki = origKey[pid]
                    if (ki != null && !longPressTriggered) fireKey(ki, false)
                    pressedKeys.clear(); origKey.clear(); firstPointerId = -1; longPressTriggered = false
                    invalidate(); return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopAll(); hidePopup()
                    pressedKeys.clear(); origKey.clear(); firstPointerId = -1; longPressTriggered = false
                    invalidate(); return true
                }
            }
        } catch (_: Exception) { pressedKeys.clear(); origKey.clear(); firstPointerId = -1; longPressTriggered = false; invalidate() }
        return true
    }

    private fun startLongPress(idx: Int) {
        val r = Runnable {
            val oi = origKey[firstPointerId]
            if (oi == idx && !longPressTriggered) {
                longPressTriggered = true; hidePopup()
                if (keyDefs[idx].code == -3) {
                    fireKey(idx, true)
                    startRepeat(idx)
                } else {
                    fireKey(idx, true)
                    pressedKeys.remove(firstPointerId)
                }
                invalidate()
            }
        }
        pendingLongPress = r; handler.postDelayed(r, longPressDelay)
    }

    private fun startRepeat(idx: Int) {
        val r = object : Runnable {
            override fun run() {
                if (origKey[firstPointerId] == idx) { fireKey(idx, true); handler.postDelayed(this, repeatDelay) }
            }
        }
        repeatRunnable = r; handler.postDelayed(r, repeatDelay)
    }

    private fun stopAll() {
        pendingLongPress?.let { handler.removeCallbacks(it) }; pendingLongPress = null
        repeatRunnable?.let { handler.removeCallbacks(it) }; repeatRunnable = null
    }

    private fun fireKey(idx: Int, long: Boolean) {
        if (idx < 0 || idx >= keyDefs.size) return
        try {
            val k = keyDefs[idx]
            if (long && k.longPressCode != null)
                onKeyAction?.invoke(k.copy(code = k.longPressCode!!, label = k.longPressLabel ?: k.label), true)
            else onKeyAction?.invoke(k, long)
        } catch (_: Exception) {}
    }

    private fun showPopup(idx: Int) {
        if (idx < 0 || idx >= keyDefs.size) return
        val k = keyDefs[idx]
        popupText = when { k.longPressLabel != null -> k.longPressLabel; !k.isSpecial -> keyLabel(k); else -> null }
        if (popupText != null) { popupX = keyRects[idx].centerX(); popupY = keyRects[idx].top; popupVisible = true }
    }

    private fun hidePopup() { popupVisible = false; popupText = null }

    private fun findKey(x: Float, y: Float): Int {
        val expand = keyMarginH
        for (i in keyRects.indices) {
            val r = keyRects[i]
            if (x >= r.left - expand && x <= r.right + expand &&
                y >= r.top - expand && y <= r.bottom + expand) return i
        }
        return -1
    }
}
