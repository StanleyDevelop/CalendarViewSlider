package stan.android.widget.clndrsldr.sample

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import stan.android.widget.clndrsldr.CalendarViewSlider
import java.util.*

private val months = Array(12) {
    when(it) {
        0 -> "jan"
        1 -> "feb"
        2 -> "mar"
        3 -> "apr"
        4 -> "may"
        5 -> "jun"
        6 -> "jul"
        7 -> "aug"
        8 -> "sep"
        9 -> "oct"
        10-> "nov"
        11-> "dec"
        else -> throw IllegalArgumentException()
    }
}

class MainActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var toSkipTodayMonthIfEmpty = true
        var toSkipEmptyMonths = true
        var isAutoSelectToday = false
        var verticalLineType = CalendarViewSlider.VerticalLineType.NONE
        var horizontalLineType = CalendarViewSlider.HorizontalLineType.NONE

        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.RED)

            val viewSlider = CalendarViewSlider(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(Color.WHITE)
                setTextSize(55f)
                setDayCellHeight(111)
                setToSkipEmptyMonths(toSkipEmptyMonths)
                setToSkipTodayMonthIfEmpty(toSkipTodayMonthIfEmpty)
                setIsAutoSelectToday(isAutoSelectToday)
                //
                val calendar = Calendar.getInstance()
                val nowYear = calendar[Calendar.YEAR]
                val nowMonth = calendar[Calendar.MONTH]
                val data = mutableMapOf<Int, Map<Int, Set<Int>>>()

                calendar[Calendar.YEAR] = nowYear
                calendar[Calendar.MONTH] = nowMonth + 1
                data[calendar[Calendar.YEAR]] = mutableMapOf<Int, Set<Int>>().apply {
                    data[calendar[Calendar.YEAR]]?.also(::putAll)
                    put(calendar[Calendar.MONTH], setOf(2, 4))
                }

                calendar[Calendar.YEAR] = nowYear
                calendar[Calendar.MONTH] = nowMonth + 5
                data[calendar[Calendar.YEAR]] = mutableMapOf<Int, Set<Int>>().apply {
                    data[calendar[Calendar.YEAR]]?.also(::putAll)
                    put(calendar[Calendar.MONTH], setOf(6, 23))
                }

                calendar[Calendar.YEAR] = nowYear
                calendar[Calendar.MONTH] = nowMonth - 1
                data[calendar[Calendar.YEAR]] = mutableMapOf<Int, Set<Int>>().apply {
                    data[calendar[Calendar.YEAR]]?.also(::putAll)
                    put(calendar[Calendar.MONTH], setOf(1, 3))
                }

                calendar[Calendar.YEAR] = nowYear
                calendar[Calendar.MONTH] = nowMonth - 7
                data[calendar[Calendar.YEAR]] = mutableMapOf<Int, Set<Int>>().apply {
                    data[calendar[Calendar.YEAR]]?.also(::putAll)
                    put(calendar[Calendar.MONTH], setOf(11, 17))
                }

                setData(data)
                //
                setOnSelectDateListener { year, month, day ->
                    Toast.makeText(
                        context,
                        "selected date: $year/${months[month]}/$day",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                setOnMonthChangeListener { year, month ->
                    Toast.makeText(
                        context,
                        "current month: $year/${months[month]}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                setVerticalLineType(verticalLineType)
                setHorizontalLineType(horizontalLineType)
            }
            addView(viewSlider)

            addView(LinearLayout(context).apply {
                val toSkipEmptyMonthsTextView = TextView(context).apply {
                    text = toSkipEmptyMonths.toString()
                }
                addView(
                    Button(context).apply {
                        text = "to skip empty months"
                        setOnClickListener {
                            toSkipEmptyMonths = !toSkipEmptyMonths
                            viewSlider.setToSkipEmptyMonths(toSkipEmptyMonths)
                            toSkipEmptyMonthsTextView.text = toSkipEmptyMonths.toString()
                        }
                    }
                )
                addView(toSkipEmptyMonthsTextView)
            })
            addView(LinearLayout(context).apply {
                val toSkipTodayMonthIfEmptyTextView = TextView(context).apply {
                    text = toSkipTodayMonthIfEmpty.toString()
                }
                addView(
                    Button(context).apply {
                        text = "to skip today month if empty"
                        setOnClickListener {
                            toSkipTodayMonthIfEmpty = !toSkipTodayMonthIfEmpty
                            viewSlider.setToSkipTodayMonthIfEmpty(toSkipTodayMonthIfEmpty)
                            toSkipTodayMonthIfEmptyTextView.text = toSkipTodayMonthIfEmpty.toString()
                        }
                    }
                )
                addView(toSkipTodayMonthIfEmptyTextView)
            })

            addView(LinearLayout(context).apply {
                val isAutoSelectTodayTextView = TextView(context).apply {
                    text = isAutoSelectToday.toString()
                }
                addView(
                    Button(context).apply {
                        text = "is Auto Select Today"
                        setOnClickListener {
                            isAutoSelectToday = !isAutoSelectToday
                            viewSlider.setIsAutoSelectToday(isAutoSelectToday)
                            isAutoSelectTodayTextView.text = isAutoSelectToday.toString()
                        }
                    }
                )
                addView(isAutoSelectTodayTextView)
            })

            addView(LinearLayout(context).apply {
                val verticalLineTypeTextView = TextView(context).apply {
                    text = verticalLineType.toString()
                }
                addView(
                    Button(context).apply {
                        text = "vertical Line Type"
                        setOnClickListener {
                            verticalLineType = when(verticalLineType) {
                                CalendarViewSlider.VerticalLineType.NONE -> CalendarViewSlider.VerticalLineType.REGULAR
                                CalendarViewSlider.VerticalLineType.REGULAR -> CalendarViewSlider.VerticalLineType.NONE
                            }
                            viewSlider.setVerticalLineType(verticalLineType)
                            verticalLineTypeTextView.text = verticalLineType.toString()
                        }
                    }
                )
                addView(verticalLineTypeTextView)
            })
            addView(LinearLayout(context).apply {
                val horizontalLineTypeTextView = TextView(context).apply {
                    text = horizontalLineType.toString()
                }
                addView(
                    Button(context).apply {
                        text = "horizontal Line Type"
                        setOnClickListener {
                            horizontalLineType = when(horizontalLineType) {
                                CalendarViewSlider.HorizontalLineType.NONE -> CalendarViewSlider.HorizontalLineType.REGULAR
                                CalendarViewSlider.HorizontalLineType.REGULAR -> CalendarViewSlider.HorizontalLineType.I_OS_STYLE
                                CalendarViewSlider.HorizontalLineType.I_OS_STYLE -> CalendarViewSlider.HorizontalLineType.NONE
                            }
                            viewSlider.setHorizontalLineType(horizontalLineType)
                            horizontalLineTypeTextView.text = horizontalLineType.toString()
                        }
                    }
                )
                addView(horizontalLineTypeTextView)
            })

            addView(
                Button(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = "unselect date"
                    setOnClickListener {
                        viewSlider.unSelectDate()
                    }
                }
            )
        }

        setContentView(view)
    }
}