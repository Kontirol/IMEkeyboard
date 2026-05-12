package com.example.kontirol

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.View

/**
 * 维吾尔文输入法服务
 */
class UyghurInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var currentKeyboardType = KEYBOARD_UYGHUR
    private var isShiftActive = false

    companion object {
        private const val KEYBOARD_UYGHUR = 0
        private const val KEYBOARD_NUMBER = 1
        private const val KEYBOARD_SYMBOL = 2

        private const val KEY_SHIFT = -1
        private const val KEY_ENTER = -2
        private const val KEY_DELETE = -5
        private const val KEY_MODE_CHANGE = -4
        private const val KEY_LEFT = -7
        private const val KEY_RIGHT = -8
    }

    override fun onCreateInputView(): View {
        val layout = layoutInflater.inflate(R.layout.uyghur_keyboard_view, null)
        keyboardView = layout.findViewById(R.id.keyboard_view)
        keyboardView?.setOnKeyboardActionListener(this)
        loadKeyboard(KEYBOARD_UYGHUR)
        return layout
    }

    private fun loadKeyboard(type: Int) {
        val xmlRes = when (type) {
            KEYBOARD_NUMBER -> R.xml.number_keyboard
            KEYBOARD_SYMBOL -> R.xml.symbol_keyboard
            else -> R.xml.uyghur_keyboard
        }
        val keyboard = Keyboard(this, xmlRes)
        keyboardView?.keyboard = keyboard
        currentKeyboardType = type
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        when (primaryCode) {
            KEY_SHIFT -> handleShift()
            KEY_DELETE -> handleDelete()
            KEY_ENTER -> handleEnter()
            KEY_MODE_CHANGE -> handleModeChange()
            KEY_LEFT -> handleCursorMove(-1)
            KEY_RIGHT -> handleCursorMove(1)
            else -> handleCharacterInput(primaryCode)
        }
    }

    private fun handleShift() {
        isShiftActive = !isShiftActive
        keyboardView?.isShifted = isShiftActive
        keyboardView?.invalidateAllKeys()
    }

    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun handleModeChange() {
        val nextType = when (currentKeyboardType) {
            KEYBOARD_UYGHUR -> KEYBOARD_NUMBER
            KEYBOARD_NUMBER -> KEYBOARD_SYMBOL
            else -> KEYBOARD_UYGHUR
        }
        loadKeyboard(nextType)
    }

    private fun handleCursorMove(direction: Int) {
        val keyCode = if (direction < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun handleCharacterInput(primaryCode: Int) {
        if (primaryCode > 0) {
            val text = primaryCode.toChar().toString()
            currentInputConnection?.commitText(text, 1)
            if (isShiftActive) {
                isShiftActive = false
                keyboardView?.isShifted = false
                keyboardView?.invalidateAllKeys()
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {
        text?.let { currentInputConnection?.commitText(it, 1) }
    }
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
