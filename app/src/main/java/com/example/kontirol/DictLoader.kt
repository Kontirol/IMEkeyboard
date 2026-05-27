package com.example.kontirol

import android.content.res.AssetManager

class DictLoader(assetManager: AssetManager?) {

    val charDict: Map<String, List<Pair<String, Long>>>
    private val wordDict: Map<String, List<Pair<String, Long>>>

    init {
        if (assetManager != null) {
            charDict = loadRimeCharDict(assetManager)
            wordDict = loadRimeWordDict(assetManager)
        } else {
            charDict = emptyMap()
            wordDict = emptyMap()
        }
    }

    /**
     * 纯词语搜索（不含单字兜底）。
     * @return 匹配的词语列表（可能为空）
     */
    fun getCandidates(
        completed: List<String>,
        active: String,
        limit: Int = 32
    ): List<String> {
        val result = mutableListOf<String>()
        val compactKey = (completed + listOf(active).filter { it.isNotEmpty() }).joinToString("")

        // 精确匹配
        if (active.isEmpty() && completed.isNotEmpty()) {
            wordDict[compactKey]?.let { words ->
                for ((word, _) in words) {
                    if (word !in result) result.add(word)
                }
            }
        }

        // 前缀匹配：按需过滤 wordDict keys，无需预建索引
        if (compactKey.isNotEmpty() && active.isNotEmpty()) {
            val candidates = mutableListOf<Pair<String, Long>>()
            for (key in wordDict.keys) {
                if (key.startsWith(compactKey)) {
                    wordDict[key]?.let { words ->
                        for ((w, f) in words) {
                            if (w !in result) candidates.add(Pair(w, f))
                        }
                    }
                }
            }
            candidates.sortByDescending { it.second }
            for ((word, _) in candidates) {
                if (word !in result) {
                    result.add(word)
                    if (result.size >= limit) break
                }
            }
        }

        return result.take(limit)
    }

    /**
     * 按音节前缀查询单字候选（用于正在输入时显示提示字）
     */
    fun getCharCandidatesByPrefix(prefix: String, limit: Int = 12): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (syl in charDict.keys.filter { it.startsWith(prefix) }.sortedBy { it.length }) {
            charDict[syl]?.let { chars ->
                for ((ch, _) in chars) {
                    if (ch !in seen) {
                        seen.add(ch)
                        result.add(ch)
                        if (result.size >= limit) return result
                    }
                }
            }
        }
        return result
    }

    fun bestMatch(completed: List<String>): String? {
        if (completed.isEmpty()) return null
        val key = completed.joinToString("")
        wordDict[key]?.firstOrNull()?.first?.let { return it }
        return charDict[completed.last()]?.firstOrNull()?.first
    }

    private fun loadRimeCharDict(am: AssetManager): Map<String, List<Pair<String, Long>>> {
        val map = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        try {
            parseRimeFile(am.open("8105.dict.yaml").bufferedReader()) { parts ->
                if (parts.size >= 2 && parts[0].length == 1) {
                    val syl = parts[1]
                    val freq = parts.getOrNull(2)?.toLongOrNull() ?: 1L
                    map.getOrPut(syl) { mutableListOf() }.add(Pair(parts[0], freq))
                }
            }
        } catch (_: Exception) {}
        return map.mapValues { (_, v) -> v.sortedByDescending { it.second } }
    }

    private fun loadRimeWordDict(am: AssetManager): Map<String, List<Pair<String, Long>>> {
        val map = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        val files = try {
            am.list("")?.filter { it != "8105.dict.yaml" && it.endsWith(".dict.yaml") } ?: emptyList()
        } catch (_: Exception) { emptyList() }
        for (file in files) {
            try {
                parseRimeFile(am.open(file).bufferedReader()) { parts ->
                    if (parts.size >= 2 && parts[0].length >= 2) {
                        val compact = parts[1].replace(" ", "")
                        val freq = parts.getOrNull(2)?.toLongOrNull() ?: 1L
                        map.getOrPut(compact) { mutableListOf() }.add(Pair(parts[0], freq))
                    }
                }
            } catch (_: Exception) {}
        }
        return map.mapValues { (_, v) -> v.sortedByDescending { it.second } }
    }

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
