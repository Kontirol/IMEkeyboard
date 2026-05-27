package com.example.kontirol

class PinyinEngine {

    private val trieRoot = PinyinTrieNode()
    private val syllableSet: Set<String> by lazy { buildSyllableSet() }

    init { for (syl in syllableSet) trieRoot.add(syl.toList()) }

    fun isSyllable(s: String) = s in syllableSet

    data class SegmentResult(val completed: List<String>, val active: String) {
        fun fullPinyin() = (completed + listOf(active).filter { it.isNotEmpty() }).joinToString(" ")
    }

    fun segment(input: String): SegmentResult {
        if (input.isEmpty()) return SegmentResult(emptyList(), "")
        val lower = input.lowercase().trim()
        val parts = lower.split("'")
        if (parts.size > 1) {
            val c = parts.dropLast(1)
            val last = parts.last()
            return if (last in syllableSet || last.isEmpty()) SegmentResult(c + listOf(last).filter{it.isNotEmpty()}, "")
            else SegmentResult(c, last)
        }
        val (words, _) = tokenize(lower)
        if (words.isEmpty()) return SegmentResult(emptyList(), lower)
        val completed = mutableListOf<String>()
        var pos = 0
        for (w in words) {
            if (pos + w.length <= lower.length && lower.substring(pos, pos + w.length) == w && w in syllableSet) {
                completed.add(w); pos += w.length
            } else break
        }
        val active = lower.substring(pos)
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
            val (buf, succ) = trieRoot.find(rem)
            if (succ) { words.add(buf); rem = rem.substring(buf.length) }
            else { invalid.add(rem[0]); rem = rem.substring(1) }
        }
        return Pair(words, invalid)
    }

    private class PinyinTrieNode(val key: Char = '\u0000', var end: Boolean = false) {
        val children = mutableMapOf<Char, PinyinTrieNode>()
        fun add(seq: List<Char>) {
            if (seq.isEmpty()) { end = true }
            else children.getOrPut(seq[0]) { PinyinTrieNode(seq[0]) }.add(seq.drop(1))
        }
        fun find(sentence: String): Pair<String, Boolean> {
            for (i in sentence.indices) {
                val j = sentence.length - i
                if (sentence.length >= j) {
                    val k = sentence.substring(0, j)
                    if (k.isNotEmpty() && k[0] in children) {
                        val (buf, ok) = children[k[0]]!!.find(sentence.substring(1))
                        if (ok) {
                            if (buf.isNotEmpty() && buf.last() == 'g') {
                                val (b1, s1) = children[k[0]]!!.find(buf.dropLast(1))
                                if (b1.isNotEmpty()) {
                                    val rs = 1 + buf.length
                                    if (rs <= sentence.length && buf.last() in children) {
                                        val (_, s2) = children[buf.last()]!!.find(sentence.substring(rs))
                                        if (s1 && s2) return Pair(sentence[0] + b1, true)
                                    }
                                }
                            }
                            return Pair(sentence[0] + buf, true)
                        }
                    }
                }
            }
            return Pair("", end)
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
