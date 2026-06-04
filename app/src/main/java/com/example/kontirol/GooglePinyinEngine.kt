package com.example.kontirol

import android.content.Context
import com.android.inputmethod.pinyin.PinyinDecoderService
import java.io.File

class GooglePinyinEngine(private val context: Context?) {

    private var initialized = false
    private var pinyinBuf = ""

    fun init(): Boolean {
        if (initialized) return true
        if (context == null) return false
        PinyinDecoderService.ensureLoaded()
        if (!PinyinDecoderService.isLoaded) return false
        try {
            val usrFile = File(context.filesDir, "usr_dict.dat")
            if (!usrFile.exists()) usrFile.createNewFile()
            val usrBytes = (usrFile.absolutePath + "\u0000").toByteArray(Charsets.UTF_8)
            val afd = context.resources.openRawResourceFd(
                context.resources.getIdentifier("dict_pinyin", "raw", context.packageName))
            val ok = PinyinDecoderService.nativeImOpenDecoderFd(afd.fileDescriptor, afd.startOffset, afd.length, usrBytes)
            afd.close()
            if (ok) { PinyinDecoderService.nativeImSetMaxLens(32, 32); initialized = true }
            return ok
        } catch (e: Exception) {
            android.util.Log.e("GooglePinyinEngine", "Init failed", e)
            return false
        }
    }

    fun close() {
        synchronized(this) {
            if (initialized) { PinyinDecoderService.nativeImCloseDecoder(); initialized = false }
        }
    }

    fun reset() {
        synchronized(this) {
            pinyinBuf = ""
            if (initialized) PinyinDecoderService.nativeImResetSearch()
        }
    }

    fun addLetter(ch: Char): Int {
        synchronized(this) {
            pinyinBuf += ch
            return PinyinDecoderService.nativeImAddLetter(ch.code.toByte())
        }
    }

    fun getCandidates(maxCount: Int = 32): List<String> {
        synchronized(this) {
            val bytes = pinyinBuf.toByteArray(Charsets.UTF_8)
            PinyinDecoderService.nativeImSearch(bytes, bytes.size)
            val result = mutableListOf<String>()
            for (i in 0 until maxCount) {
                val c = PinyinDecoderService.nativeImGetChoice(i)
                if (c.isNullOrEmpty()) break
                result.add(c)
            }
            return result
        }
    }

    fun getCandidate(index: Int): String? {
        val c = PinyinDecoderService.nativeImGetChoice(index)
        return if (c.isNullOrEmpty()) null else c
    }

    fun choose(index: Int): Int = PinyinDecoderService.nativeImChoose(index)
    fun getFixedLen(): Int = PinyinDecoderService.nativeImGetFixedLen()
    fun deleteSearch(pos: Int, isPosInSplid: Boolean, clearFixedThisStep: Boolean): Int =
        PinyinDecoderService.nativeImDelSearch(pos, isPosInSplid, clearFixedThisStep)
    fun getSplStart(): IntArray? = PinyinDecoderService.nativeImGetSplStart()
    fun getPyStr(decoded: Boolean = true): String? = synchronized(this) {
        PinyinDecoderService.nativeImGetPyStr(decoded)
    }
    fun getPredicts(fixedStr: String, maxCount: Int = 8): List<String> {
        val num = PinyinDecoderService.nativeImGetPredictsNum(fixedStr)
        val result = mutableListOf<String>()
        for (i in 0 until minOf(num, maxCount)) {
            val item = PinyinDecoderService.nativeImGetPredictItem(i)
            if (!item.isNullOrEmpty()) result.add(item)
        }
        return result
    }
    fun cancelInput(): Boolean = PinyinDecoderService.nativeImCancelInput()
    fun flushCache() { PinyinDecoderService.nativeImFlushCache() }
    val isInitialized get() = initialized
}
