package com.example.kontirol

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet

/**
 * 自定义键盘视图，隐藏空白占位键
 */
class UyghurKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : KeyboardView(context, attrs) {

    // 透明背景
    private val transparentDrawable = ColorDrawable(Color.TRANSPARENT)

    override fun onDraw(canvas: Canvas) {
        val keyboard = keyboard ?: return
        
        // 先绘制所有按键，但跳过空白键
        for (key in keyboard.keys) {
            if (key.codes[0] == 0) {
                // 空白键：不绘制任何内容
                continue
            }
        }
        
        // 调用父类绘制
        super.onDraw(canvas)
    }
}
