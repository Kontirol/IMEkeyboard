package com.example.kontirol

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class ThemeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_theme, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = view.findViewById<LinearLayout>(R.id.theme_container)
        val ctx = requireContext()
        val currentId = KeyboardTheme.current(ctx).id

        val dp = ctx.resources.displayMetrics.density

        for (theme in KeyboardTheme.presets) {
            val card = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (12 * dp).toInt()
                }
                setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
                setBackgroundColor(Color.WHITE)
                setOnClickListener {
                    KeyboardTheme.save(ctx, theme)
                    // 返回上一页
                    parentFragmentManager.popBackStack()
                }
            }

            val inner = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // 左侧预览方块（3个色块：背景、按键、强调色）
            val preview = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams((72 * dp).toInt(), (56 * dp).toInt()).apply {
                    marginEnd = (14 * dp).toInt()
                }
            }

            // 顶部条 = modeBarBg
            preview.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (8 * dp).toInt()
                )
                setBackgroundColor(theme.modeBarBgColor)
            })
            // 候选栏
            preview.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (10 * dp).toInt()
                )
                setBackgroundColor(theme.candidateBgColor)
            })
            // 键盘背景 + 模拟 3 个键
            val keyRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (38 * dp).toInt()
                )
                setBackgroundColor(theme.bgColor)
                gravity = Gravity.CENTER
            }
            for (i in 0 until 4) {
                keyRow.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (14 * dp).toInt(),
                        (20 * dp).toInt()
                    ).apply {
                        leftMargin = (2 * dp).toInt(); rightMargin = (2 * dp).toInt()
                    }
                    setBackgroundColor(if (i == 3) theme.keySpecialColor else theme.keyColor)
                })
            }
            preview.addView(keyRow)
            inner.addView(preview)

            // 中间文字
            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(ctx).apply {
                text = theme.name; textSize = 16f; setTextColor(Color.parseColor("#1A1A1A"))
                setTypeface(null, Typeface.BOLD)
            })
            textCol.addView(TextView(ctx).apply {
                text = if (theme.isDark) "⚫ قاراڭغۇ" else "⚪ ئاقلىق"
                textSize = 12f; setTextColor(Color.parseColor("#8E8E93"))
            })
            inner.addView(textCol)

            // 右侧勾选标记
            if (theme.id == currentId) {
                val check = TextView(ctx).apply {
                    text = "✓"; textSize = 22f; setTextColor(Color.parseColor("#007AFF"))
                    setTypeface(null, Typeface.BOLD)
                }
                inner.addView(check)
            }

            card.addView(inner)
            parent.addView(card)
        }
    }
}
