package com.example.kontirol

class PinyinEngine {

    private val syllables = setOf(
        "a", "ai", "an", "ang", "ao",
        "ba", "bai", "ban", "bang", "bao", "bei", "ben", "beng", "bi", "bian", "biao", "bie", "bin", "bing", "bo", "bu",
        "ca", "cai", "can", "cang", "cao", "ce", "cen", "ceng", "cha", "chai", "chan", "chang", "chao", "che", "chen", "cheng", "chi", "chong", "chou", "chu", "chua", "chuai", "chuan", "chuang", "chui", "chun", "chuo", "ci", "cong", "cou", "cu", "cuan", "cui", "cun", "cuo",
        "da", "dai", "dan", "dang", "dao", "de", "dei", "deng", "di", "dian", "diao", "die", "ding", "diu", "dong", "dou", "du", "duan", "dui", "dun", "duo",
        "e", "ei", "en", "eng", "er",
        "fa", "fan", "fang", "fei", "fen", "feng", "fo", "fou", "fu",
        "ga", "gai", "gan", "gang", "gao", "ge", "gei", "gen", "geng", "gong", "gou", "gu", "gua", "guai", "guan", "guang", "gui", "gun", "guo",
        "ha", "hai", "han", "hang", "hao", "he", "hei", "hen", "heng", "hong", "hou", "hu", "hua", "huai", "huan", "huang", "hui", "hun", "huo",
        "ji", "jia", "jian", "jiang", "jiao", "jie", "jin", "jing", "jiong", "jiu", "ju", "juan", "jue", "jun",
        "ka", "kai", "kan", "kang", "kao", "ke", "ken", "keng", "kong", "kou", "ku", "kua", "kuai", "kuan", "kuang", "kui", "kun", "kuo",
        "la", "lai", "lan", "lang", "lao", "le", "lei", "leng", "li", "lia", "lian", "liang", "liao", "lie", "lin", "ling", "liu", "long", "lou", "lu", "lv", "luan", "lve", "lun", "luo",
        "ma", "mai", "man", "mang", "mao", "me", "mei", "men", "meng", "mi", "mian", "miao", "mie", "min", "ming", "miu", "mo", "mou", "mu",
        "na", "nai", "nan", "nang", "nao", "ne", "nei", "nen", "neng", "ni", "nian", "niang", "niao", "nie", "nin", "ning", "niu", "nong", "nu", "nv", "nuan", "nve", "nuo",
        "o", "ou",
        "pa", "pai", "pan", "pang", "pao", "pei", "pen", "peng", "pi", "pian", "piao", "pie", "pin", "ping", "po", "pu",
        "qi", "qia", "qian", "qiang", "qiao", "qie", "qin", "qing", "qiong", "qiu", "qu", "quan", "que", "qun",
        "ran", "rang", "rao", "re", "ren", "reng", "ri", "rong", "rou", "ru", "ruan", "rui", "run", "ruo",
        "sa", "sai", "san", "sang", "sao", "se", "sen", "seng", "sha", "shai", "shan", "shang", "shao", "she", "shei", "shen", "sheng", "shi", "shou", "shu", "shua", "shuai", "shuan", "shuang", "shui", "shun", "shuo", "si", "song", "sou", "su", "suan", "sui", "sun", "suo",
        "ta", "tai", "tan", "tang", "tao", "te", "teng", "ti", "tian", "tiao", "tie", "ting", "tong", "tou", "tu", "tuan", "tui", "tun", "tuo",
        "wa", "wai", "wan", "wang", "wei", "wen", "weng", "wo", "wu",
        "xi", "xia", "xian", "xiang", "xiao", "xie", "xin", "xing", "xiong", "xiu", "xu", "xuan", "xue", "xun",
        "ya", "yan", "yang", "yao", "ye", "yi", "yin", "ying", "yo", "yong", "you", "yu", "yuan", "yue", "yun",
        "za", "zai", "zan", "zang", "zao", "ze", "zei", "zen", "zeng", "zha", "zhai", "zhan", "zhang", "zhao", "zhe", "zhei", "zhen", "zheng", "zhi", "zhong", "zhou", "zhu", "zhua", "zhuai", "zhuan", "zhuang", "zhui", "zhun", "zhuo", "zi", "zong", "zou", "zu", "zuan", "zui", "zun", "zuo"
    )

    private val charDict: Map<String, List<String>> by lazy { buildCharDict() }

    fun getActiveSyllable(input: String): String? {
        if (input.isEmpty()) return null
        val lower = input.lowercase().trim()
        if (lower in syllables) return lower
        for (syl in syllables) {
            if (syl.startsWith(lower)) return null
        }
        return findLastSyllable(lower)
    }

    fun getCandidates(input: String, limit: Int = 5): List<String> {
        if (input.isEmpty()) return emptyList()
        val lower = input.lowercase().trim()

        // 完整音节 → 返回该音节所有字
        if (lower in syllables) {
            return charDict[lower]?.take(limit) ?: emptyList()
        }

        // 前缀匹配 → 返回所有匹配音节的首字（去重）
        val matches = mutableListOf<String>()
        for (syl in syllables) {
            if (syl.startsWith(lower)) {
                val chars = charDict[syl]
                if (chars != null && chars.isNotEmpty()) {
                    matches.add(chars.first())
                }
            }
        }
        if (matches.isNotEmpty()) {
            return matches.distinct().take(limit)
        }

        // fallback：最后一个完整音节
        val lastSyl = findLastSyllable(lower)
        if (lastSyl != null) {
            return charDict[lastSyl]?.take(limit) ?: emptyList()
        }

        return emptyList()
    }

    fun getFirstCandidate(syllable: String): String? = charDict[syllable]?.firstOrNull()

    private fun findLastSyllable(input: String): String? {
        for (i in input.length - 1 downTo 1) {
            val sub = input.substring(0, i)
            if (sub in syllables) return sub
        }
        return null
    }

    private fun buildCharDict(): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        for (line in CHAR_DATA.trim().lines()) {
            val parts = line.trim().split(":")
            if (parts.size == 2) {
                // 逐字符分割（每个汉字是一个字符），排除空白
                map[parts[0]] = parts[1].trim().map { it.toString() }.filter { it.isNotBlank() }.toMutableList()
            }
        }
        return map
    }

    companion object {
        private val CHAR_DATA = """
a:啊阿吖嗄腌
ai:爱艾碍癌哀唉挨矮隘埃皑霭
an:安按暗岸案俺谙黯鞍氨庵桉鹌
ang:昂肮盎
ao:奥傲敖熬澳凹袄懊坳嗷鳌鏖
ba:把八吧巴爸拔罢霸芭坝叭疤跋靶笆耙灞
bai:百白败摆拜柏伯佰稗
ban:办半版班般板扮搬斑颁伴瓣拌扳绊钣
bang:帮棒绑磅邦榜蚌谤梆膀
bao:报包保宝饱抱暴爆薄堡鲍雹苞煲褒豹趵
bei:被北备背悲杯碑卑贝钡狈蓓悖惫
ben:本奔笨苯夯畚
beng:蹦绷甭泵迸蚌嘣甏
bi:比笔必避壁逼鼻毕闭碧臂彼弊秘泌庇敝蔽陛鄙毙璧篦弼
bian:变边便编遍辩鞭辨贬卞扁砭碥匾汴
biao:表标彪膘飙镖裱
bie:别憋鳖瘪蹩
bin:宾滨彬斌殡缤濒摈
bing:并病兵冰饼丙柄秉炳禀邴
bo:波播拨博薄伯剥玻勃泊驳铂箔帛博脖菠舶搏箔
bu:不部步布补捕卜埠簿哺怖埔卟
ca:擦嚓礤
cai:才采财材菜彩裁猜踩蔡睬
can:参残餐灿惨惭惨蚕粲璨
cang:藏仓苍沧舱
cao:草操曹槽糙嘈漕
ce:测策侧厕册恻
ceng:曾层蹭噌
cha:查察差茶插叉刹诧茬碴喳衩搽锸
chai:差拆柴豺侪钗
chan:产颤缠蝉馋禅阐铲搀蟾谄婵躔
chang:长常场唱厂尝昌偿畅倡猖敞怅昶
chao:超朝潮炒抄吵巢嘲焯绰
che:车彻撤扯澈掣坼
chen:陈晨沉称衬臣辰尘趁琛忱嗔抻
cheng:成城程称承诚呈乘惩撑澄秤逞骋瞠
chi:吃持赤池迟尺耻翅斥炽痴弛匙侈弛嗤
chong:冲重充崇宠虫涌憧忡铳
chou:抽仇愁臭丑筹酬绸瞅踌畴惆
chu:出处初除楚触储础畜厨锄雏橱矗怵搐绌
chuai:揣踹啜
chuan:传穿川船串喘
chuang:创床窗闯疮幢怆
chui:吹垂锤炊捶陲
chun:春纯唇醇蠢椿淳
chuo:戳绰啜辍龊
ci:此次词刺瓷辞慈磁雌祠疵赐
cong:从丛匆聪葱囱琮淙
cou:凑辏腠
cu:促粗簇醋蹴猝蹙
cuan:窜攒篡蹿爨
cui:催脆摧翠崔粹璀瘁
cun:存村寸忖皴
cuo:错措挫搓磋撮痤矬
da:大打达答搭哒嗒沓鞑
dai:大代带待袋戴呆贷逮歹殆怠黛
dan:但单蛋弹担淡胆丹旦诞惮眈耽疸
dang:当党档挡荡铛宕凼
dao:到道导倒刀岛盗稻悼捣蹈
de:的地得德
deng:等灯登邓凳瞪蹬噔
di:地第低调底的弟敌滴底抵堤递帝迪笛狄涤翟嫡蒂缔谛
dian:点电店典垫殿滇淀颠甸奠惦碘癜巅佃
diao:掉调雕吊刁貂叼碉
die:爹跌叠碟蝶迭谍喋耋
ding:定顶丁订叮盯钉鼎锭仃
diu:丢铥
dong:动东冬洞懂董冻栋侗恫
dou:都豆斗抖逗兜陡痘窦蚪
du:读度都独毒督渡肚赌堵杜睹妒镀笃
duan:断段短端锻缎煅椴
dui:对队堆兑碓怼
dun:吨顿蹲盾敦钝墩盹沌炖
duo:多夺朵躲堕舵跺咄剁踱
e:额恶俄哦饿鹅扼遏鄂噩娥鳄峨愕
en:恩摁蒽
er:而二儿尔耳饵洱贰迩珥
fa:发法罚阀伐乏筏砝垡珐
fan:反饭翻犯范番繁凡烦返泛樊帆藩梵
fang:放方房访防妨芳仿坊纺彷
fei:非飞费废肥匪菲肺沸妃诽绯斐
fen:分份纷奋粉坟芬焚愤氛汾忿酚
feng:风封丰峰锋疯奉凤枫冯逢缝蜂讽烽俸
fo:佛
fou:否缶
fu:服夫副富复父福附负府赴伏妇扶符腐傅辅覆幅腹赋芙拂釜孵
ga:嘎尬噶钆尕
gai:改该盖概钙丐溉
gan:赶感敢干甘肝杆尴柑竿擀
gang:刚钢港纲岗杠缸罡冈
gao:高告搞稿糕膏皋羔篙槁
ge:个各歌格哥革隔戈葛阁割鸽搁咯
gei:给
gen:跟根亘艮
geng:更耕梗庚羹哽赓
gong:工公共供功宫攻贡躬恭龚巩拱
gou:够狗购构沟勾苟钩垢佝缑
gu:古顾股故鼓姑骨孤谷雇辜菇箍沽锢
gua:挂瓜刮寡卦呱
guai:怪拐乖掴
guan:关管官观馆冠惯灌贯棺掼涫
guang:光广逛胱犷咣
gui:规归贵鬼桂跪柜龟硅轨瑰诡刽皈
gun:滚棍辊衮磙
guo:过国果郭锅裹帼椁聒
ha:哈蛤铪
hai:还海害孩亥骇骸氦
han:汉韩含喊寒罕翰撼函旱汗涵捍悍焊憨酣
hang:行航杭巷夯沆
hao:好号毫豪浩耗郝嚎壕貉皓镐
he:和合河何喝核荷盒禾贺赫褐鹤壑阂阖
hei:黑嘿
hen:很狠恨痕
heng:横衡恒哼亨蘅珩
hong:红宏洪虹轰鸿哄弘泓
hou:后候厚侯猴喉吼逅篌
hu:护胡互户湖呼虎忽糊乎狐沪壶弧浒唬蝴斛
hua:话花化华划画滑猾桦哗骅
huai:坏怀淮徊槐踝
huan:还换环欢患缓幻唤焕寰宦涣痪
huang:黄皇荒慌晃煌惶簧凰恍潢蝗
hui:会回挥汇慧灰惠辉毁悔恢绘晦徽讳贿
hun:婚混魂昏浑馄荤诨
huo:活火伙或获货霍惑祸豁夥
ji:几机及记计级集即基己际技极击济奇纪激急既继疾鸡积迹绩吉辑籍疾挤脊冀忌季寂祭剂悸
jia:家加价假甲架夹佳驾嫁贾稼茄颊
jian:见间建件简坚减检渐剑践健箭监鉴键兼舰尖肩艰拣捡俭剪茧柬
jiang:将讲江降奖疆姜蒋酱浆僵匠桨
jiao:叫教交角较校脚焦骄郊嚼娇搅饺矫窖椒蛟
jie:接结节解姐介界借届街截杰洁揭戒阶劫竭羯
jin:进今近金斤尽紧仅禁劲锦筋晋巾浸津襟瑾
jing:经京精境井静景竟睛镜敬净竞惊警径晶痉靖
jiong:窘炯扃迥
jiu:就九旧久酒纠救究舅韭鸠厩咎
ju:句具局据举居巨聚拒距俱剧矩菊鞠驹
juan:卷捐娟倦眷绢隽鹃
jue:决觉绝掘角爵诀倔崛厥獗
jun:军均君俊峻骏竣菌筠
ka:卡咖咯咔
kai:开凯慨楷恺揩
kan:看刊砍堪坎侃槛
kang:抗康扛慷炕亢
kao:考靠烤拷犒铐
ke:可科课刻客棵壳颗渴柯磕苛蝌
ken:肯垦恳啃龈
keng:坑铿吭
kong:空孔控恐倥崆
kou:口扣寇叩蔻叩
ku:苦哭库酷裤窟枯骷
kua:跨夸垮挎胯
kuai:快块筷会侩蒯
kuan:款宽髋
kuang:况矿框狂旷眶匡筐邝
kui:亏愧溃葵窥盔魁睽逵
kun:困昆坤捆琨
kuo:扩括阔廓
la:拉啦辣蜡腊喇剌
lai:来赖莱睐癞籁
lan:蓝兰烂览拦澜栏懒揽婪缆榄
lang:浪朗郎狼廊琅螂啷
lao:老劳捞牢唠佬姥酪烙
le:了乐勒
lei:类累泪雷垒磊蕾擂儡羸
leng:冷愣楞
li:里力理利立离历李例礼丽励粒莉厉璃栗狸漓篱
lia:俩
lian:连联脸练炼恋莲廉帘镰敛涟琏
liang:两亮量辆良梁凉粮谅粱晾
liao:了料聊辽疗寥撩僚燎缭
lie:列烈裂猎劣冽趔
lin:林临邻淋琳霖凛鳞磷躏
ling:另令领零灵岭龄凌玲铃陵聆菱翎
liu:六流留刘柳溜榴瘤硫浏馏
long:龙拢笼隆聋珑窿陇
lou:楼漏露搂篓陋娄
lu:路陆录露卢鲁炉芦庐碌禄虏颅辘
lv:绿率旅律铝吕履屡缕氯滤驴
luan:乱卵峦挛孪鸾
lun:论轮伦沦仑抡
luo:落罗洛络骆裸锣萝逻箩骡
ma:吗妈马嘛码骂麻蟆
mai:买卖麦迈脉埋霾
man:满慢漫蛮曼蔓馒幔谩
mang:忙盲芒茫氓蟒
mao:毛猫冒帽矛貌茅茂髦锚
me:么麽
mei:没每美妹媒梅煤眉霉魅玫媚酶莓
men:门们闷扪焖
meng:梦猛蒙孟盟萌懵锰蟒
mi:米密秘迷觅蜜弥谜眯靡泌
mian:面棉免眠绵缅勉冕娩
miao:秒妙描庙苗瞄渺藐淼
mie:灭蔑篾乜
min:民敏闽泯悯抿
ming:名明命鸣铭冥茗瞑
miu:谬缪
mo:摸默魔磨墨末沫莫陌寞蓦漠
mou:某谋牟眸哞
mu:目木母幕墓牧慕姆穆暮牡亩募睦拇
na:那拿哪纳娜呐捺肭
nai:乃奶耐奈氖艿
nan:难男南楠囡腩
nang:囊馕囔
nao:脑闹恼挠瑙淖
ne:呢讷
nei:内馁
nen:嫩恁
neng:能
ni:你泥逆拟妮腻倪匿霓昵
nian:年念碾撵拈廿黏
niang:娘酿
niao:鸟尿袅嬲
nie:捏涅聂镍啮孽蹑
nin:您恁
ning:宁凝拧狞泞柠聍
niu:牛纽扭妞忸拗
nong:农弄浓脓侬
nu:怒努奴弩孥
nv:女钕恧
nuan:暖
nuo:诺挪懦糯喏搦
o:哦噢
ou:偶欧殴鸥呕藕
pa:怕爬帕趴琶啪耙
pai:拍排派牌徘湃俳
pan:判盘盼潘攀叛畔磐蟠
pang:旁胖庞乓膀彷
pao:跑泡炮抛袍刨疱
pei:配培陪佩赔沛裴胚焙霈
pen:喷盆湓
peng:朋碰鹏捧棚蓬膨烹嘭抨
pi:批皮匹脾疲僻劈屁痞癖丕噼
pian:片篇偏骗翩便骈
piao:票漂飘瓢缥剽
pie:瞥撇苤
pin:品拼频贫聘姘嫔
ping:平评屏凭瓶萍坪苹乒
po:破迫婆坡泼颇泊魄粕
pu:普铺扑朴谱葡蒲仆瀑曝圃
qi:起其期七奇气器齐企汽妻弃棋骑启岂戚凄乞祈讫
qia:恰洽掐髂
qian:前千钱签欠浅牵潜迁铅谦嵌遣歉纤黔倩
qiang:强枪墙抢腔呛羌蔷跄
qiao:桥悄巧敲乔瞧翘窍俏峭撬跷
qie:且切窃怯茄妾砌趄
qin:亲琴勤侵寝秦禽钦沁芹擒覃
qing:请情清青轻庆晴倾卿氢擎顷氰
qiong:穷琼穹茕
qiu:求球秋丘邱囚酋糗虬
qu:去取区曲趣渠屈驱娶屈蛆觑
quan:全权圈劝泉拳犬券诠痊
que:却确缺雀鹊瘸榷阕
qun:群裙逡
ran:然染燃冉髯
rang:让壤攘嚷瓤
rao:绕扰饶娆桡
re:热惹喏
ren:人任认忍仁韧刃纫
reng:仍扔
ri:日
rong:容融荣蓉溶绒榕熔戎茸冗
rou:肉柔揉蹂鞣
ru:如入儒乳辱汝蠕褥濡
ruan:软阮朊
rui:瑞锐蕊睿芮
run:润闰
ruo:若弱偌箬
sa:洒撒萨卅飒
sai:赛塞腮鳃
san:三散伞叁馓
sang:桑丧嗓搡
sao:扫骚嫂搔缫
se:色涩瑟啬铯
sen:森
seng:僧
sha:杀沙啥傻砂纱刹鲨莎厦
shai:晒筛酾
shan:山善闪衫陕扇删珊杉擅煽膳苫
shang:上商伤尚赏裳殇觞
shao:少绍烧稍勺邵哨韶捎芍
she:社设舍射涉蛇舌奢赦摄慑
shen:什深身神审甚申伸沈慎渗绅呻
sheng:生声省剩胜升圣盛绳牲笙甥
shi:是时十事实使市世示识师士史式食始失视室势诗石试释施适氏驶湿拾殖饰什矢侍逝噬
shou:手受收首守授售瘦寿狩
shu:书数树输属术述束鼠竖叔舒疏署薯暑恕曙
shua:刷耍唰
shuai:帅摔衰甩率蟀
shuan:拴栓涮闩
shuang:双爽霜孀
shui:水谁睡税
shun:顺瞬舜吮
shuo:说硕朔烁铄
si:四五思斯死丝似司私寺撕肆厮嗣伺嘶
song:送松宋颂耸诵忪嵩
sou:搜艘嗽擞飕
su:速苏素诉肃塑宿俗酥粟溯
suan:算酸蒜狻
sui:随虽岁碎遂隋穗髓祟
sun:孙损笋榫荪
suo:所索锁缩梭琐唆
ta:他她它踏塔塌榻獭挞
tai:太台态抬泰胎汰苔肽
tan:谈探弹坦叹摊炭贪滩谭坛檀毯
tang:堂唐糖躺汤倘塘烫淌膛棠
tao:逃套讨桃淘陶掏萄涛滔韬
te:特忒忑
teng:疼腾藤誊滕
ti:提题体替梯踢啼剔蹄剃涕嚏
tian:天田填添甜恬腆舔
tiao:条调跳挑眺迢窕粜
tie:铁贴帖餮
ting:听停庭厅挺亭艇廷烃霆
tong:同通统痛童铜筒桐彤潼瞳
tou:头投透偷骰
tu:土突图途涂屠兔吐秃凸荼
tuan:团湍疃
tui:推退腿褪颓
tun:吞屯囤豚臀
tuo:脱托拖妥拓椭唾驮
wa:瓦挖蛙洼袜蛙娲
wai:外歪崴
wan:万完晚玩湾碗挽弯丸婉蔓皖惋
wang:王望网往忘亡旺汪枉罔惘
wei:为位未委围微维味卫威谓唯伟危慰违巍伪尾炜玮蔚苇
wen:文问温闻稳纹吻蚊雯紊
weng:翁瓮嗡蓊
wo:我握窝卧涡蜗斡
wu:五无物务武午吴误舞屋雾乌伍悟污巫吾勿呜钨侮诬
xi:西喜细系吸息析希戏席习洗稀溪夕悉昔熙膝袭惜熄锡嘻曦
xia:下夏峡吓狭虾瞎霞匣侠暇遐
xian:先现线县显鲜限险献闲仙咸宪贤陷掀弦羡腺衔
xiang:想向相像香乡象响项享箱祥湘巷翔镶橡
xiao:小笑校消效晓萧销削孝潇啸霄骁
xie:写些谢协血鞋械邪斜携泄卸谐蟹胁蝎
xin:新心信辛欣薪馨芯衅昕
xing:行性星兴形型幸醒姓刑杏猩惺
xiong:雄胸兄凶熊匈汹芎
xiu:修秀休袖锈绣嗅臭
xu:需许须需续虚序绪徐叙蓄畜旭吁恤
xuan:选宣旋悬玄轩喧绚眩璇萱
xue:学雪血穴靴薛削鳕
xun:寻讯训迅巡询循熏旬勋汛驯巽
ya:压呀牙亚芽鸭崖哑押涯衙鸦雅
yan:眼言验烟沿演研严颜盐岩延燕掩厌宴雁焰艳砚谚
yang:样养扬洋阳羊央仰杨氧痒漾殃鸯
yao:要药摇腰咬遥耀姚瑶尧舀窈
ye:也业夜叶爷野页液耶咽椰掖曳
yi:一以意义已易亿艺议衣异益移医依疑忆仪遗宜乙蚁翼椅抑矣逸役毅译溢
yin:因音引银印饮隐阴吟淫茵姻殷
ying:应影英营迎硬映赢盈颖婴鹰樱萤
yo:哟唷
yong:用勇永拥涌泳庸咏佣踊雍甬
you:有又由游右油友优忧尤犹幽悠邮酉囿
yu:与于语雨玉预域鱼育余遇欲愈予寓誉浴裕豫逾郁
yuan:远员元院愿原圆源园援缘冤媛猿怨袁渊
yue:月越约阅乐跃悦粤岳曰
yun:运云允韵孕匀蕴酝筠
za:杂砸咋匝咂
zai:在再灾载栽仔宰哉崽
zan:咱赞暂攒簪昝
zang:脏藏葬奘臧
zao:早造遭糟枣燥藻凿躁灶
ze:则责择泽咋啧仄
zei:贼
zen:怎谮
zeng:增赠憎曾综缯甑
zha:扎炸闸渣诈栅咋乍铡楂
zhai:宅窄斋债寨择摘
zhan:站战展占粘盏斩瞻沾斩毡
zhang:张长章掌丈障涨账仗杖彰璋樟
zhao:找照着招赵召兆朝罩沼肇
zhe:这者着折哲浙遮蔗蛰辙
zhen:真正阵镇针振震珍诊枕斟甄缜
zheng:正整政证争征郑症挣蒸睁铮筝
zhi:之只知直至制指值治直质志支织职致枝纸智殖置址植旨滞挚稚芝肢脂
zhong:中种重众终钟忠衷肿仲盅
zhou:周洲轴州舟皱宙骤粥肘帚
zhu:主住注助猪著逐诸竹朱珠筑柱祝驻铸煮嘱蛛瞩
zhua:抓爪挝
zhuai:拽
zhuan:转专传砖赚撰篆馔
zhuang:装壮庄状撞桩妆幢
zhui:追坠缀锥惴赘
zhun:准谆肫
zhuo:桌着捉琢卓浊灼拙酌茁斫
zi:自子资字紫姿滋籽仔渍梓孜
zong:总综宗纵棕踪粽鬃偬
zou:走奏邹揍诹
zu:组族足祖租阻卒诅俎
zuan:钻纂攥
zui:最醉罪嘴
zun:尊遵樽鳟
zuo:做作坐左座昨佐撮凿柞
"""
    }
}
