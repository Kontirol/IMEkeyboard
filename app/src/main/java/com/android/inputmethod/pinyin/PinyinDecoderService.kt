package com.android.inputmethod.pinyin

import android.content.res.AssetFileDescriptor
import java.io.FileDescriptor

class PinyinDecoderService {

    companion object {
        private var loaded = false

        fun ensureLoaded() {
            if (loaded) return
            try {
                System.loadLibrary("jni_pinyinime")
                loaded = true
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("PinyinDecoder", "Failed to load jni_pinyinime", e)
            }
        }

        val isLoaded get() = loaded

        @JvmStatic external fun nativeImOpenDecoder(sysDict: ByteArray?, usrDict: ByteArray?): Boolean
        @JvmStatic external fun nativeImOpenDecoderFd(fd: FileDescriptor, startOffset: Long, length: Long, usrDict: ByteArray?): Boolean
        @JvmStatic external fun nativeImSetMaxLens(maxSpsLen: Int, maxHzsLen: Int)
        @JvmStatic external fun nativeImCloseDecoder(): Boolean
        @JvmStatic external fun nativeImSearch(pyBuf: ByteArray, pyLen: Int): Int
        @JvmStatic external fun nativeImDelSearch(pos: Int, isPosInSplid: Boolean, clearFixedThisStep: Boolean): Int
        @JvmStatic external fun nativeImResetSearch()
        @JvmStatic external fun nativeImAddLetter(ch: Byte): Int
        @JvmStatic external fun nativeImGetPyStr(decoded: Boolean): String
        @JvmStatic external fun nativeImGetPyStrLen(decoded: Boolean): Int
        @JvmStatic external fun nativeImGetSplStart(): IntArray?
        @JvmStatic external fun nativeImGetChoice(choiceId: Int): String
        @JvmStatic external fun nativeImChoose(choiceId: Int): Int
        @JvmStatic external fun nativeImCancelLastChoice(): Int
        @JvmStatic external fun nativeImGetFixedLen(): Int
        @JvmStatic external fun nativeImCancelInput(): Boolean
        @JvmStatic external fun nativeImFlushCache(): Boolean
        @JvmStatic external fun nativeImGetPredictsNum(fixedStr: String): Int
        @JvmStatic external fun nativeImGetPredictItem(predictNo: Int): String
        @JvmStatic external fun nativeSyncUserDict(userDict: ByteArray?, tomerge: String): String
        @JvmStatic external fun nativeSyncBegin(userDict: ByteArray?): Boolean
        @JvmStatic external fun nativeSyncFinish(): Boolean
        @JvmStatic external fun nativeSyncGetLemmas(): String
        @JvmStatic external fun nativeSyncPutLemmas(tomerge: String): Int
        @JvmStatic external fun nativeSyncGetLastCount(): Int
        @JvmStatic external fun nativeSyncGetTotalCount(): Int
        @JvmStatic external fun nativeSyncClearLastGot(): Boolean
        @JvmStatic external fun nativeSyncGetCapacity(): Int
    }
}
