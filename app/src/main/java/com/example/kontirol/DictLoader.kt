package com.example.kontirol

import android.content.res.AssetManager

class DictLoader(assetManager: AssetManager?) {

    val charDict: Map<String, List<Pair<String, Long>>>
    internal val wordDict: Map<String, List<Pair<String, Long>>>

    // 排序 key 列表，用于二分查找前缀匹配 → O(log N + results) 替代 O(N)
    private val wordKeysSorted: List<String>
    private val charKeysSorted: List<String>

    init {
        if (assetManager != null) {
            charDict = loadRimeCharDict(assetManager)
            wordDict = loadRimeWordDict(assetManager)
        } else {
            charDict = emptyMap()
            wordDict = emptyMap()
        }
        wordKeysSorted = wordDict.keys.sorted()
        charKeysSorted = charDict.keys.sorted()
    }

    /**
     * 纯词语搜索（不含单字兜底）。
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

        // 前缀匹配：二分查找 + 范围扫描
        if (compactKey.isNotEmpty() && active.isNotEmpty()) {
            val candidates = mutableListOf<Pair<String, Long>>()
            val seen = mutableSetOf<String>()
            val startIdx = wordKeysSorted.binarySearch(compactKey).let { if (it < 0) -it - 1 else it }
            for (i in startIdx until wordKeysSorted.size) {
                val key = wordKeysSorted[i]
                if (!key.startsWith(compactKey)) break
                wordDict[key]?.let { words ->
                    for ((w, f) in words) {
                        if (seen.add(w)) candidates.add(Pair(w, f))
                    }
                }
                if (candidates.size >= limit * 3) break
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
     * 按音节前缀查询单字候选（二分查找优化）
     */
    fun getCharCandidatesByPrefix(prefix: String, limit: Int = 12): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        val startIdx = charKeysSorted.binarySearch(prefix).let { if (it < 0) -it - 1 else it }
        val matchingKeys = mutableListOf<String>()
        for (i in startIdx until charKeysSorted.size) {
            val key = charKeysSorted[i]
            if (!key.startsWith(prefix)) break
            matchingKeys.add(key)
        }
        matchingKeys.sortBy { it.length }
        for (syl in matchingKeys) {
            charDict[syl]?.let { chars ->
                for ((ch, _) in chars) {
                    if (seen.add(ch)) {
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
