package com.example.kontirol

import android.content.res.AssetManager

class DictLoader(assetManager: AssetManager?) {

    val charDict: Map<String, List<String>>
    private val wordDict: Map<String, List<String>>

    init {
        if (assetManager != null) {
            charDict = loadRimeCharDict(assetManager)
            wordDict = loadRimeWordDict(assetManager)
        } else {
            charDict = emptyMap()
            wordDict = emptyMap()
        }
    }

    fun getCandidates(fullPinyin: String, activeSyllable: String?, existingChars: List<String>, limit: Int = 5): List<String> {
        val result = mutableListOf<String>()
        wordDict[fullPinyin]?.let { result.addAll(it) }
        for ((key, words) in wordDict) {
            if (key != fullPinyin && key.startsWith(fullPinyin))
                for (w in words) if (w !in result) result.add(w)
        }
        if (activeSyllable != null) {
            wordDict[activeSyllable]?.let { for (w in it) if (w !in result) result.add(w) }
            charDict[activeSyllable]?.let { for (c in it) if (c !in result) result.add(c) }
        }
        for (c in existingChars) { if (c !in result) result.add(c) }
        return result.take(limit)
    }

    fun bestMatch(pinyin: String): String? =
        wordDict[pinyin]?.firstOrNull() ?: charDict[pinyin]?.firstOrNull()

    private fun loadRimeCharDict(am: AssetManager): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        try {
            parseRimeFile(am.open("8105.dict.yaml").bufferedReader()) { parts ->
                if (parts.size >= 2 && parts[0].length == 1)
                    map.getOrPut(parts[1]) { mutableListOf() }
                        .add(Pair(parts[0], parts.getOrNull(2)?.toLongOrNull() ?: 1L))
            }
        } catch (_: Exception) {}
        return map.mapValues { (_, v) -> v.sortedByDescending { it.second }.map { it.first } }
    }

    private fun loadRimeWordDict(am: AssetManager): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        val files = try {
            am.list("")?.filter { it != "8105.dict.yaml" && it.endsWith(".dict.yaml") } ?: emptyList()
        } catch (_: Exception) { emptyList() }
        for (file in files) {
            try {
                parseRimeFile(am.open(file).bufferedReader()) { parts ->
                    if (parts.size >= 2 && parts[0].length >= 2) {
                        val compact = parts[1].replace(" ", "")
                        map.getOrPut(compact) { mutableListOf() }
                            .add(Pair(parts[0], parts.getOrNull(2)?.toLongOrNull() ?: 1L))
                    }
                }
            } catch (_: Exception) {}
        }
        return map.mapValues { (_, v) -> v.sortedByDescending { it.second }.map { it.first } }
    }

    /** 解析 Rime 格式文件：跳过 --- 头，... 后为数据，无 header 则全当数据 */
    private fun parseRimeFile(reader: java.io.BufferedReader, onEntry: (List<String>) -> Unit) {
        reader.use { r ->
            var inData = false
            var sawHeader = false
            r.forEachLine { raw ->
                val t = raw.trim()
                if (!inData) {
                    if (t == "...") { inData = true; sawHeader = true; return@forEachLine }
                    if (!sawHeader && t.isNotEmpty() && !t.startsWith("#") && !t.startsWith("-")) {
                        inData = true; sawHeader = true
                    } else return@forEachLine
                }
                if (!inData || t.isEmpty() || t.startsWith("#")) return@forEachLine
                onEntry(t.split("\t"))
            }
        }
    }
}
