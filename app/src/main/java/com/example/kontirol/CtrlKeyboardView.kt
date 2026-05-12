package com.example.kontirol

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class CtrlKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bgColor = Color.parseColor("#D1D3D9")
    private val keyColor = Color.parseColor("#FFFFFF")
    private val keySpecialColor = Color.parseColor("#ADB0B8")
    private val keyPressedColor = Color.parseColor("#C7C9CD")
    private val keySpecialPressedColor = Color.parseColor("#8E9096")
    private val keyTextColor = Color.parseColor("#000000")
    private val keySpecialTextColor = Color.parseColor("#000000")
    private val keyShadowColor = Color.parseColor("#9A9CA3")
    private val popupColor = Color.parseColor("#1C1C1E")
    private val popupTextColor = Color.WHITE
    private val longPressHintColor = Color.parseColor("#999999")

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

    private var pressedKeyIndex = -1
    private var longPressTriggered = false
    private val longPressDelay = 400L
    private val repeatDelay = 55L
    private var downX = 0f
    private var downY = 0f
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

    fun setLayout(l: KeyboardLayout) { layout = l; isShifted = false; requestLayout(); invalidate() }
    fun toggleShift() { isShifted = !isShifted; invalidate() }
    fun resetShift() { isShifted = false; invalidate() }

    override fun onMeasure(wms: Int, hms: Int) {
        val w = MeasureSpec.getSize(wms)
        val d = resources.displayMetrics.density
        rowHeight = layout.rowHeightDp * d
        val rows = layout.rows.size
        setMeasuredDimension(w, (rowHeight * rows + keyMarginV * 2 * (rows + 1)).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        computeKeys(w)
    }

    private fun computeKeys(w: Int) {
        keyRects.clear(); keyDefs.clear()
        var y = keyMarginV
        for (row in layout.rows) {
            if (row.isEmpty()) { y += rowHeight; continue }
            val tw = row.sumOf { it.widthWeight.toDouble() }.toFloat()
            val pad = keyMarginH * 2f
            val availW = w - pad * (row.size + 1)
            var x = keyMarginH
            for (key in row) {
                val kw = (key.widthWeight / tw) * availW
                keyRects.add(RectF(x, y, x + kw, y + rowHeight - keyMarginV * 2))
                keyDefs.add(key)
                x += kw + pad
            }
            y += rowHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bgColor)
        for (i in keyDefs.indices) { if (i < keyRects.size) drawKey(canvas, i) }
        if (popupVisible && popupText != null) drawPopup(canvas)
    }

    private fun drawKey(canvas: Canvas, i: Int) {
        if (i >= keyRects.size || i >= keyDefs.size) return
        val r = keyRects[i]; val k = keyDefs[i]; val pressed = i == pressedKeyIndex
        if (!pressed) {
            keyPaint.style = Paint.Style.FILL; keyPaint.color = keyShadowColor
            canvas.drawRoundRect(RectF(r.left, r.top + shadowOffset, r.right, r.bottom + shadowOffset), keyRadius, keyRadius, keyPaint)
        }
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = when {
            pressed && k.isSpecial -> keySpecialPressedColor
            pressed -> keyPressedColor
            k.isSpecial -> keySpecialColor
            else -> keyColor
        }
        val kr = if (pressed) RectF(r.left, r.top + shadowOffset, r.right, r.bottom + shadowOffset) else r
        canvas.drawRoundRect(kr, keyRadius, keyRadius, keyPaint)

        val label = keyLabel(k)
        val ug = layout == KeyboardLayouts.UYGHUR
        textPaint.typeface = if (ug && ukijTypeface != null) ukijTypeface else defaultTypeface
        textPaint.color = if (k.isSpecial) keySpecialTextColor else keyTextColor
        textPaint.textSize = when { k.isSpecial -> kr.height() * 0.32f; ug -> kr.height() * 0.44f; else -> kr.height() * 0.40f }
        canvas.drawText(label, kr.centerX(), kr.centerY() - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)

        if (k.longPressLabel != null && !k.isSpecial) {
            smallTextPaint.typeface = if (ug && ukijTypeface != null) ukijTypeface else defaultTypeface
            smallTextPaint.color = longPressHintColor
            smallTextPaint.textSize = kr.height() * 0.20f
            canvas.drawText(k.longPressLabel!!, kr.right - kr.width() * 0.16f, kr.bottom - kr.height() * 0.16f, smallTextPaint)
        }
    }

    private fun keyLabel(k: KeyDef): String = when (k.code) {
        -5 -> if (layout == KeyboardLayouts.UYGHUR) "بوشلۇق" else "space"
        -2 -> "↵"; -3 -> "⌫"; -1 -> if (isShifted) "⇧" else "⇧"
        -6 -> "123"; -7 -> "#+="
        -4 -> if (layout == KeyboardLayouts.UYGHUR) "EN" else "ئۇ"
        else -> if (isShifted && layout == KeyboardLayouts.ENGLISH) k.label.uppercase() else k.label
    }

    private fun drawPopup(canvas: Canvas) {
        val t = popupText ?: return
        val pw = 100f; val ph = 110f; val px = popupX - pw / 2; val py = popupY - ph - 12f
        popupPaint.style = Paint.Style.FILL; popupPaint.color = popupColor
        canvas.drawRoundRect(RectF(px, py, px + pw, py + ph), 18f, 18f, popupPaint)
        val tri = Path().apply { moveTo(popupX - 8f, py + ph); lineTo(popupX + 8f, py + ph); lineTo(popupX, py + ph + 8f); close() }
        canvas.drawPath(tri, popupPaint)
        textPaint.typeface = if (layout == KeyboardLayouts.UYGHUR && ukijTypeface != null) ukijTypeface else defaultTypeface
        textPaint.color = popupTextColor; textPaint.textSize = 36f
        canvas.drawText(t, popupX, (py + ph / 2) - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        try {
            val x = e.x; val y = e.y
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = x; downY = y; longPressTriggered = false
                    pressedKeyIndex = findKey(x, y)
                    if (pressedKeyIndex >= 0) {
                        showPopup(pressedKeyIndex)
                        startLongPress(pressedKeyIndex)
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(20L) } catch (_: Exception) {}
                    }
                    invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(x - downX) > 40f || abs(y - downY) > 40f) {
                        stopAll(); hidePopup()
                        if (pressedKeyIndex >= 0 && findKey(x, y) != pressedKeyIndex) { pressedKeyIndex = -1; invalidate() }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    stopAll(); hidePopup()
                    val idx = findKey(x, y)
                    if (idx >= 0 && idx == pressedKeyIndex && !longPressTriggered) fireKey(idx, false)
                    pressedKeyIndex = -1; longPressTriggered = false; invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopAll(); hidePopup()
                    pressedKeyIndex = -1; longPressTriggered = false; invalidate()
                    return true
                }
            }
        } catch (_: Exception) { pressedKeyIndex = -1; longPressTriggered = false; invalidate() }
        return super.onTouchEvent(e)
    }

    private fun startLongPress(idx: Int) {
        val r = Runnable {
            if (pressedKeyIndex == idx && !longPressTriggered) {
                longPressTriggered = true; hidePopup()
                if (keyDefs[idx].code == -3) {
                    // 删除键：启动连发
                    fireKey(idx, true)
                    startRepeat(idx)
                } else {
                    fireKey(idx, true)
                    pressedKeyIndex = -1
                }
                invalidate()
            }
        }
        pendingLongPress = r; handler.postDelayed(r, longPressDelay)
    }

    private fun startRepeat(idx: Int) {
        val r = object : Runnable {
            override fun run() {
                if (pressedKeyIndex == idx) { fireKey(idx, true); handler.postDelayed(this, repeatDelay) }
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
        for (i in keyRects.indices) { if (keyRects[i].contains(x, y)) return i }; return -1
    }
}
