package com.example.kontirol

class PinyinEngine {

    private val trieRoot = PinyinTrieNode()
    private val syllableSet: Set<String> by lazy { buildSyllableSet() }

    init { for (syl in syllableSet) trieRoot.add(syl.toList()) }

    fun isSyllable(s: String) = s in syllableSet

    data class SegmentResult(val completed: List<String>, val active: String) {
        fun fullPinyin() = (completed + listOf(active).filter { it.isNotEmpty() }).joinToString(" ")
    }

    // 缓存上次分词结果，支持增量追加（避免每次全量重分词）
    private var cachedInput: String = ""
    private var cachedResult: SegmentResult = SegmentResult(emptyList(), "")

    fun segment(input: String): SegmentResult {
        if (input.isEmpty()) {
            cachedInput = ""
            cachedResult = SegmentResult(emptyList(), "")
            return cachedResult
        }
        val lower = input.lowercase().trim()

        // 增量追加：新输入是上次输入 + 若干字符
        if (lower.startsWith(cachedInput) && cachedInput.isNotEmpty()) {
            val suffix = lower.substring(cachedInput.length)
            val merged = mergeActive(cachedResult, suffix)
            cachedInput = lower
            cachedResult = merged
            return merged
        }

        // 退格或全新输入 → 全量分词
        val result = segmentFull(lower)
        cachedInput = lower
        cachedResult = result
        return result
    }

    /** 在已有分词结果上追加新字符，只处理 active 部分 */
    private fun mergeActive(prev: SegmentResult, suffix: String): SegmentResult {
        val newActive = prev.active + suffix
        // 检查新 active 是否是完整音节
        if (newActive in syllableSet) {
            return SegmentResult(prev.completed + newActive, "")
        }
        // 尝试用 trie 看能否继续扩展为有效音节
        if (trieRoot.findLongest(newActive) != null) {
            return SegmentResult(prev.completed, newActive)
        }
        // newActive 无法构成有效音节前缀 → 对 active 部分重新分词
        val (words, _) = tokenize(newActive)
        if (words.isEmpty()) return SegmentResult(prev.completed, newActive)
        val newCompleted = prev.completed.toMutableList()
        var pos = 0
        for (w in words) {
            if (pos + w.length <= newActive.length && newActive.substring(pos, pos + w.length) == w && w in syllableSet) {
                newCompleted.add(w); pos += w.length
            } else break
        }
        val remaining = newActive.substring(pos)
        return SegmentResult(newCompleted, remaining)
    }

    private fun segmentFull(input: String): SegmentResult {
        val parts = input.split("'")
        if (parts.size > 1) {
            val c = parts.dropLast(1)
            val last = parts.last()
            return if (last in syllableSet || last.isEmpty()) SegmentResult(c + listOf(last).filter{it.isNotEmpty()}, "")
            else SegmentResult(c, last)
        }
        val (words, _) = tokenize(input)
        if (words.isEmpty()) return SegmentResult(emptyList(), input)
        val completed = mutableListOf<String>()
        var pos = 0
        for (w in words) {
            if (pos + w.length <= input.length && input.substring(pos, pos + w.length) == w && w in syllableSet) {
                completed.add(w); pos += w.length
            } else break
        }
        val active = input.substring(pos)
        return if (active.isNotEmpty()) SegmentResult(completed, active) else SegmentResult(completed, "")
    }

    fun getActiveSyllable(input: String): String? {
        val seg = segment(input)
        return seg.active.ifEmpty { seg.completed.lastOrNull() }
    }

    fun tokenize(sentence: String): Pair<List<String>, List<Char>> {
        val words = mutableListOf<String>()
        val invalid = mutableListOf<Char>()
        var rem = sentence
        while (rem.isNotEmpty()) {
            val match = trieRoot.findLongest(rem)
            if (match != null) {
                val split = trySplitG(rem, match)
                if (split != null) {
                    words.add(split)
                    rem = rem.substring(split.length)
                } else {
                    words.add(match)
                    rem = rem.substring(match.length)
                }
            } else {
                invalid.add(rem[0])
                rem = rem.substring(1)
            }
        }
        return Pair(words, invalid)
    }

    /**
     * 检查 match 是否应该去掉末尾 'g' 来分割。
     * 条件：match 以 'g' 结尾、去除 'g' 后是有效音节、且 'g' 能作为下一个音节的开头。
     */
    private fun trySplitG(sentence: String, match: String): String? {
        if (!match.endsWith("g") || match.length < 2) return null
        val withoutG = match.dropLast(1)
        if (withoutG !in syllableSet) return null
        val after = sentence.substring(match.length)
        if (after.isEmpty()) return null
        // 用 trie 检查 "g" + after 是否可以开启有效音节 → O(k), 原来 O(400)
        if (trieRoot.findLongest("g$after") != null) return withoutG
        return null
    }

    private class PinyinTrieNode(val key: Char = '\u0000', var end: Boolean = false) {
        val children = mutableMapOf<Char, PinyinTrieNode>()

        fun add(seq: List<Char>) {
            if (seq.isEmpty()) { end = true; return }
            children.getOrPut(seq[0]) { PinyinTrieNode(seq[0]) }.add(seq.drop(1))
        }

        fun findLongest(sentence: String): String? {
            if (sentence.isEmpty()) return null
            var node: PinyinTrieNode = this
            var best: String? = null
            for (i in sentence.indices) {
                node = node.children[sentence[i]] ?: break
                if (node.end) best = sentence.substring(0, i + 1)
            }
            return best
        }
    }

    private fun buildSyllableSet(): Set<String> {
        val s = mutableSetOf<String>()
        for (tok in SYL_RAW.trim().split("\\s+".toRegex())) { if (tok.isNotEmpty()) s.add(tok) }
        return s
    }

    companion object {
        private val SYL_RAW = """
a ai an ang ao
ba bai ban bang bao bei ben beng bi bian biao bie bin bing bo bu
ca cai can cang cao ce cen ceng cha chai chan chang chao che chen cheng chi chong chou chu chua chuai chuan chuang chui chun chuo ci cong cou cu cuan cui cun cuo
da dai dan dang dao de dei deng di dian diao die ding diu dong dou du duan dui dun duo
e ei en eng er
fa fan fang fei fen feng fo fou fu
ga gai gan gang gao ge gei gen geng gong gou gu gua guai guan guang gui gun guo
ha hai han hang hao he hei hen heng hong hou hu hua huai huan huang hui hun huo
ji jia jian jiang jiao jie jin jing jiong jiu ju juan jue jun
ka kai kan kang kao ke ken keng kong kou ku kua kuai kuan kuang kui kun kuo
la lai lan lang lao le lei leng li lia lian liang liao lie lin ling liu long lou lu lv luan lve lun luo
ma mai man mang mao me mei men meng mi mian miao mie min ming miu mo mou mu
na nai nan nang nao ne nei nen neng ni nian niang niao nie nin ning niu nong nu nv nuan nve nuo
o ou
pa pai pan pang pao pei pen peng pi pian piao pie pin ping po pu
qi qia qian qiang qiao qie qin qing qiong qiu qu quan que qun
ran rang rao re ren reng ri rong rou ru ruan rui run ruo
sa sai san sang sao se sen seng sha shai shan shang shao she shei shen sheng shi shou shu shua shuai shuan shuang shui shun shuo si song sou su suan sui sun suo
ta tai tan tang tao te teng ti tian tiao tie ting tong tou tu tuan tui tun tuo
wa wai wan wang wei wen weng wo wu
xi xia xian xiang xiao xie xin xing xiong xiu xu xuan xue xun
ya yan yang yao ye yi yin ying yo yong you yu yuan yue yun
za zai zan zang zao ze zei zen zeng zha zhai zhan zhang zhao zhe zhei zhen zheng zhi zhong zhou zhu zhua zhuai zhuan zhuang zhui zhun zhuo zi zong zou zu zuan zui zun zuo
"""
    }
}
