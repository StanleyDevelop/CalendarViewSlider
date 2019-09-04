package stan.android.widget.clndrsldr

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

private val DEFAULT_MONTHS_COMPARATOR = Comparator<Pair<Int, Int>> { (year1, month1), (year2, month2) ->
    val y = year1.compareTo(year2)
    if(y == 0) month1.compareTo(month2)
    else y
}
private const val DAYS_IN_WEEK = 7

class CalendarViewSlider: View {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    private var textHeight: Int = 0
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 25f
        textHeight = getTextHeight("1")
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    private var dayCellWidth: Int = 0
    private var selectedDate: Triple<Int, Int, Int>? = null
    private var currentMonth: Pair<Int, Int>? = null
        set(value) {
            field = value
            if(value != null) {
                val (year, month) = value
                onMonthChangeListener(year, month)
            }
        }

    private var xOffset = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var toSkipTodayMonthIfEmpty = true
    fun setToSkipTodayMonthIfEmpty(value: Boolean) {
        toSkipTodayMonthIfEmpty = value
        if(!getMonths().contains(currentMonth)) {
            currentMonth = getDefaultMonth()
        }
        invalidate()
    }
    private var toSkipEmptyMonths = true
    fun setToSkipEmptyMonths(value: Boolean) {
        toSkipEmptyMonths = value
        if(!getMonths().contains(currentMonth)) {
            currentMonth = getDefaultMonth()
        }
        invalidate()
    }
    private var isAutoSelectToday = false
    fun setIsAutoSelectToday(value: Boolean) {
        isAutoSelectToday = value
        invalidate()
    }

    fun setTextSize(value: Float) {
        if(value < 0) throw IllegalArgumentException()
        textPaint.textSize = value
        textHeight = textPaint.getTextHeight("1")
        invalidate()
    }
    private var nonActiveAlpha = 0.5f
    fun setNonActiveAlpha(value: Float) {
        if(value < 0 || value > 1) throw IllegalArgumentException()
        nonActiveAlpha = value
        invalidate()
    }
    private var dayCellHeight: Int = 100
    fun setDayCellHeight(value: Int) {
        if(value < 0) throw IllegalArgumentException()
        dayCellHeight = value
        invalidate()
    }

    private var regularTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)!!
    fun setRegularTypeface(value: Typeface) {
        regularTypeface = value
        invalidate()
    }
    private var weekendTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)!!
    fun setWeekendTypeface(value: Typeface) {
        weekendTypeface = value
        invalidate()
    }

    private var regularTextColor = Color.BLACK
    fun setRegularTextColor(value: Int) {
        regularTextColor = value
        invalidate()
    }
    private var todayTextColor = Color.RED
    fun setTodayTextColor(value: Int) {
        todayTextColor = value
        invalidate()
    }
    private var regularSelectedColor = Color.BLUE
    fun setRegularSelectedColor(value: Int) {
        regularSelectedColor = value
        invalidate()
    }
    private var todaySelectedColor = Color.RED
    fun setTodaySelectedColor(value: Int) {
        todaySelectedColor = value
        invalidate()
    }
    private var selectedTextColor = Color.WHITE
    fun setSelectedTextColor(value: Int) {
        selectedTextColor = value
        invalidate()
    }

    fun setLineColor(value: Int) {
        linePaint.color = value
        invalidate()
    }
    fun setLineWidth(value: Float) {
        if(value < 0) throw IllegalArgumentException()
        linePaint.strokeWidth = value
        invalidate()
    }
    private var verticalLineType: VerticalLineType = VerticalLineType.NONE
    fun setVerticalLineType(value: VerticalLineType) {
        verticalLineType = value
        invalidate()
    }
    private var horizontalLineType: HorizontalLineType = HorizontalLineType.NONE
    fun setHorizontalLineType(value: HorizontalLineType) {
        horizontalLineType = value
        invalidate()
    }

    private val DEFAULT_ON_SELECT_DATE_LISTENER: (Int, Int, Int) -> Unit = { _, _, _ -> }
    private var onSelectDateListener = DEFAULT_ON_SELECT_DATE_LISTENER
    fun setOnSelectDateListener(value: (Int, Int, Int) -> Unit) {
        onSelectDateListener = value
    }

    private val DEFAULT_ON_MONTH_CHANGE_LISTENER: (Int, Int) -> Unit = { _, _ -> }
    private var onMonthChangeListener = DEFAULT_ON_MONTH_CHANGE_LISTENER
    fun setOnMonthChangeListener(value: (Int, Int) -> Unit) {
        onMonthChangeListener = value
    }

    private var data = mapOf<Int, Map<Int, Set<Int>>>()
    private var matrixMap = calculateMatrixMap()

    fun setData(value: Map<Int, Map<Int, Set<Int>>>) {
        value.forEach { (_, months) ->
            months.forEach { (month, days) ->
                if(month < 0 || month > 11) throw IllegalArgumentException()
                days.forEach { day ->
                    if(day < 1 || day > 31) throw IllegalArgumentException()
                }
            }
        }
        data = value
        matrixMap = calculateMatrixMap()
        currentMonth = getDefaultMonth()
        invalidate()
    }

    fun selectDate(year: Int, month: Int, dayOfMonth: Int) {
        if(isActive(year, month, dayOfMonth)) {
            selectedDate = Triple(year, month, dayOfMonth)
            currentMonth = year to month
            invalidate()
        }
    }
    fun unSelectDate() {
        selectedDate ?: return
        selectedDate = null
        val todayMonth = getTodayMonth()
        if(isAutoSelectToday && currentMonth != todayMonth) {
            currentMonth = todayMonth//todo?
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = dayCellHeight * calculateWeeksInMonth(Calendar.getInstance())
        val width = MeasureSpec.getSize(widthMeasureSpec)
        dayCellWidth = width / DAYS_IN_WEEK
        setMeasuredDimension(
            width,
            height
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null) return false
        return when(event.pointerCount) {
            1 -> onTouchEventSingle(event)
            else -> onTouchEventMulti(event)
        }
    }
    private var xStartTouch = 0f
    private var xOffsetAnimator: ObjectAnimator? = null
    private fun startAnimateXOffset(to: Float) {
        xOffsetAnimator?.cancel()
        xOffsetAnimator = ObjectAnimator.ofFloat(this, "xOffset", xOffset, to)
            .setDuration(250)
            .apply { start() }
    }

    private var isMoveStarted = false
    private fun onTouchEventSingle(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            onTouchDown(x, y)
            return true
        }
        if(event.action == MotionEvent.ACTION_MOVE) {
            val x = event.x
            val y = event.y
            onTouchMove(x, y)
            return true
        }
        if(event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            xStartTouch = 0f
//            xOffset = 0f
            startAnimateXOffset(0f)
            if(isMoveStarted) {
                isMoveStarted = false
            } else {
                currentMonth?.also { current ->
                    matrixMap[current]?.also { matrix ->
                        for(weekOfMonth in 0 until matrix.size) {
                            for(dayOfWeek in 0 until DAYS_IN_WEEK) {
                                val dayOfMonth = matrix[weekOfMonth][dayOfWeek] ?: continue
                                val (year, month) = current
                                if(!isActive(year, month, dayOfMonth)) continue
                                if(dayOfWeek * dayCellWidth > x
                                    || dayOfWeek * dayCellWidth + dayCellWidth < x) continue
                                if(weekOfMonth * dayCellHeight > y
                                    || weekOfMonth * dayCellHeight + dayCellHeight < y) continue
                                val todayDate = getTodayDate()
                                val isToday = todayDate.first == year
                                    && todayDate.second == month
                                    && todayDate.third == dayOfMonth
                                val isSelected = isSelected(
                                    year = year,
                                    month = month,
                                    dayOfMonth = dayOfMonth,
                                    isToday = isToday
                                )
                                if(isSelected) {
                                    invalidate()
                                    return false
                                }
                                selectedDate = Triple(year, month, dayOfMonth)
                                onSelectDateListener(year, month, dayOfMonth)
                                invalidate()
                                return true
                            }
                        }
                    }
                }
            }
            invalidate()
        }
        return false
    }
    private fun onTouchEventMulti(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        return when(event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> onTouchDown(x, y)
            MotionEvent.ACTION_MOVE -> onTouchMove(x, y)
            MotionEvent.ACTION_POINTER_UP -> {
                val x0 = event.getX(0)
                val y0 = event.getY(0)
                val x1 = event.getX(1)
                val y1 = event.getY(1)
                when(event.actionIndex) {
                    0 -> onTouchDown(x1, y1)
                    else -> onTouchDown(x0, y0)
                }
            }
            else -> false
        }
    }

    private fun onTouchDown(x: Float, y: Float): Boolean {
        xStartTouch = x
//        xStartTouch = xOffset - x
        return true
    }
    private fun onTouchMove(x: Float, y: Float): Boolean {
//        isMoveStarted = true
        val dX = xStartTouch - x
        if(!isMoveStarted) {
            if(dX.absoluteValue < 5.5) {
                return false
            }
            isMoveStarted = true
        }
        xOffset -= dX
        xStartTouch = x
//        xOffset = xStartTouch + x

        val queue = getCurrentQueueMonth()
        val prevMonth = queue.first
        val nextMonth = queue.third

        if(xOffset > width / 2) {
            if(prevMonth != null) {
                currentMonth = prevMonth
                xOffset -= width
            }
        } else if(xOffset < width / -2) {
            if(nextMonth != null) {
                currentMonth = nextMonth
                xOffset += width
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        if(canvas == null) return
        val currentMonth = currentMonth ?: return
        val matrix = matrixMap[currentMonth] ?: return
        drawMonth(
            canvas, xOffset,
            year = currentMonth.first,
            month = currentMonth.second,
            matrix = matrix
        )
        val queue = getCurrentQueueMonth()
        if(xOffset > 0) {
            queue.first?.also { prevMonth ->
                matrixMap[prevMonth]?.also {
                    drawMonth(
                        canvas, xOffset - width,
                        year = prevMonth.first,
                        month = prevMonth.second,
                        matrix = it
                    )
                }
            }
        } else if(xOffset < 0) {
            queue.third?.also { nextMonth ->
                matrixMap[nextMonth]?.also {
                    drawMonth(
                        canvas, xOffset + width,
                        year = nextMonth.first,
                        month = nextMonth.second,
                        matrix = it
                    )
                }
            }
        }
        //todo draw month divider
    }
    private fun drawMonth(
        canvas: Canvas,
        xOffset: Float,
        year: Int, month: Int,
        matrix: Array<Array<Int?>>
    ) {
        val todayDate = getTodayDate()
        matrix.forEachIndexed { weekOfMonth, daysOfWeek ->
            daysOfWeek.forEachIndexed { dayOfWeek, dayOfMonth ->
                if(dayOfMonth != null) {
                    val isToday = todayDate.first == year
                        && todayDate.second == month
                        && todayDate.third == dayOfMonth
                    val isSelected = isSelected(year, month, dayOfMonth, isToday)
                    val isActive = isActive(year, month, dayOfMonth)
                    drawDay(
                        canvas,
                        xOffset,
                        dayCellWidth, dayCellHeight,
                        weekOfMonth, dayOfWeek, dayOfMonth,
                        isActive = isActive,
                        isToday = isToday,
                        isSelected = isSelected
                    )
                }
            }
        }
        when(verticalLineType) {
            VerticalLineType.REGULAR -> drawRegularVerticalLines(
                canvas,
                xOffset = xOffset,
                weeksInMonth = matrix.size
            )
        }
        when(horizontalLineType) {
            HorizontalLineType.REGULAR -> drawRegularHorizontalLines(
                canvas,
                xOffset = xOffset,
                weeksInMonth = matrix.size
            )
            HorizontalLineType.I_OS_STYLE -> {
                val emptyCellCount = matrix[matrix.size - 1].count {
                    it == null
                } + matrix[matrix.size - 2].count {
                    it == null
                }
                drawIOSStyleHorizontalLines(
                    canvas,
                    xOffset = xOffset,
                    emptyCellCount = emptyCellCount
                )
            }
        }
        //todo drawHorizontalLines
    }
    private fun drawDay(
        canvas: Canvas,
        xOffset: Float,
        dayCellWidth: Int, dayCellHeight: Int,
        weekOfMonth: Int, dayOfWeek: Int, dayOfMonth: Int,
        isActive: Boolean, isToday: Boolean, isSelected: Boolean
    ) {
        val text = dayOfMonth.toString()
        val textWidth = textPaint.getTextWidth(text)
        textPaint.color = when {
            isSelected -> selectedTextColor
            isToday -> todayTextColor
            else -> regularTextColor
        }
        if(isActive) {
            textPaint.alpha = 255
        } else {
            textPaint.alpha = (255*nonActiveAlpha).toInt()
        }
        val isWeekendDay = isWeekendDay(dayOfWeek)
        if(isWeekendDay) {
            textPaint.typeface = weekendTypeface
        } else {
            textPaint.typeface = regularTypeface
        }
        val radius = min(dayCellHeight, dayCellWidth)/2
        if(isSelected) {
            selectedPaint.color = when {
                isToday -> todaySelectedColor
                else -> regularSelectedColor
            }
            canvas.drawCircle(
                xOffset + dayCellWidth * dayOfWeek + dayCellWidth/2,
                (dayCellHeight * weekOfMonth + dayCellHeight/2).toFloat(),
                radius * 0.7f,
                selectedPaint
            )
        }
        canvas.drawText(
            text,
            xOffset + dayCellWidth * dayOfWeek + dayCellWidth/2 - textWidth/2,
            (dayCellHeight * weekOfMonth + dayCellHeight/2 + textHeight/2).toFloat(),
            textPaint
        )
    }

    private fun drawRegularVerticalLines(
        canvas: Canvas,
        xOffset: Float,
        weeksInMonth: Int
    ) = drawRegularVerticalLines(
        canvas = canvas,
        daysInWeek = DAYS_IN_WEEK,
        dayCellWidth = dayCellWidth,
        dayCellHeight = dayCellHeight,
        xOffset = xOffset,
        weeksInMonth = weeksInMonth,
        paint = linePaint
    )

    private fun drawRegularHorizontalLines(
        canvas: Canvas,
        xOffset: Float,
        weeksInMonth: Int
    ) = drawRegularHorizontalLines(
        canvas = canvas,
        daysInWeek = DAYS_IN_WEEK,
        dayCellWidth = dayCellWidth,
        dayCellHeight = dayCellHeight,
        xOffset = xOffset,
        weeksInMonth = weeksInMonth,
        paint = linePaint
    )
    private fun drawIOSStyleHorizontalLines(
        canvas: Canvas,
        xOffset: Float,
        emptyCellCount: Int
    ) = drawIOSStyleHorizontalLines(
        canvas = canvas,
        daysInWeek = DAYS_IN_WEEK,
        dayCellWidth = dayCellWidth,
        dayCellHeight = dayCellHeight,
        xOffset = xOffset,
        emptyCellCount = emptyCellCount,
        paint = linePaint
    )

    private fun calculateWeeksInMonth(calendar: Calendar): Int {
        return 6//todo
    }
    private fun calculateDayOfWeek(calendar: Calendar): Int {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2
        return if(dayOfWeek < 0) 7 + dayOfWeek else dayOfWeek//todo
    }

    private fun getMonths() = getMonths(
        dates = data,
        toSkipEmptyMonths = toSkipEmptyMonths,
        toSkipTodayMonthIfEmpty = toSkipTodayMonthIfEmpty
    )
    private fun getDefaultMonth() = getDefaultMonth(
        dates = data,
        toSkipEmptyMonths = toSkipEmptyMonths,
        toSkipTodayMonthIfEmpty = toSkipTodayMonthIfEmpty
    )
    private fun calculateMatrixMap() = getMonths(
        data,
        toSkipEmptyMonths = false,
        toSkipTodayMonthIfEmpty = false
    ).map { (year, month) ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        (year to month) to getMatrix(calendar)
    }.toMap()
    private fun getMatrix(
        calendar: Calendar
    ) = getMatrix(
        daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH),
        weeksInMonth = calculateWeeksInMonth(calendar),
        firstDayOfWeek = calculateDayOfWeek(calendar)
    )

    private fun getCurrentQueueMonth() = getCurrentQueueMonth(
        dates = data,
        toSkipEmptyMonths = toSkipEmptyMonths,
        toSkipTodayMonthIfEmpty = toSkipTodayMonthIfEmpty,
        currentMonth = currentMonth
    )

    private fun isWeekendDay(dayOfWeek: Int): Boolean {
        if(dayOfWeek < 0 || dayOfWeek > 6) throw IllegalArgumentException()
        return dayOfWeek > 4//todo
    }
    private fun isActive(year: Int, month: Int, day: Int): Boolean {
        return data[year]?.get(month)?.find { it == day } != null
    }

    private fun isSelected(
        year: Int, month: Int, dayOfMonth: Int,
        isToday: Boolean
    ) = isSelected(
        year, month, dayOfMonth,
        selectedDate = selectedDate,
        isAutoSelectToday = isAutoSelectToday,
        isToday = isToday
    )

    enum class VerticalLineType {
        NONE, REGULAR
    }
    enum class HorizontalLineType {
        NONE, REGULAR, I_OS_STYLE
    }
}

private fun getMatrix(
    daysInMonth: Int,
    weeksInMonth: Int,
    firstDayOfWeek: Int
): Array<Array<Int?>> {
    val result = Array(weeksInMonth) {
        arrayOfNulls<Int>(DAYS_IN_WEEK)
    }
    var currentDayOfMonth = 1
    var currentDayOfWeek = firstDayOfWeek
    for(weekOfMonth in 0 until weeksInMonth) {
        for(dayOfWeek in currentDayOfWeek until DAYS_IN_WEEK) {
            if(currentDayOfMonth > daysInMonth) break
            result[weekOfMonth][dayOfWeek] = currentDayOfMonth
            currentDayOfMonth++
        }
        currentDayOfWeek = 0
    }
    return result
}

private fun getTodayMonth(): Pair<Int, Int> {
    return Calendar.getInstance().run {
        get(Calendar.YEAR) to get(Calendar.MONTH)
    }
}
private fun getTodayDate() = Calendar.getInstance().run {
    Triple(
        get(Calendar.YEAR),
        get(Calendar.MONTH),
        get(Calendar.DAY_OF_MONTH)
    )
}
private fun getDefaultMonth(
    dates: Map<Int, Map<Int, Set<Int>>>,
    toSkipEmptyMonths: Boolean,
    toSkipTodayMonthIfEmpty: Boolean
): Pair<Int, Int>? {
    val todayMonth = getTodayMonth()
    if(!toSkipTodayMonthIfEmpty) return todayMonth
    val months = getMonths(dates, toSkipEmptyMonths, toSkipTodayMonthIfEmpty)
    return months.firstOrNull { it == todayMonth } ?: months.firstOrNull()
}

private fun getMonths(
    dates: Map<Int, Map<Int, Set<Int>>>,
    toSkipEmptyMonths: Boolean,
    toSkipTodayMonthIfEmpty: Boolean
): List<Pair<Int, Int>> {
    val result = if(toSkipEmptyMonths) {
        onlyMonthsWithDays(dates, toSkipTodayMonthIfEmpty)
    } else {
        allMonths(dates, toSkipTodayMonthIfEmpty)
    }.sortedWith(DEFAULT_MONTHS_COMPARATOR)
    return result
}

private fun onlyMonthsWithDays(
    dates: Map<Int, Map<Int, Set<Int>>>,
    toSkipTodayMonthIfEmpty: Boolean
): List<Pair<Int, Int>> {
    val result = dates.filter { (_, months) -> months.isNotEmpty() }.mapValues { (_, months) ->
        months.filter { (_, days) -> days.isNotEmpty() }
    }.flatMap { (year, months) ->
        months.keys.map {
            year to it
        }
    }.toMutableSet()
    if(!toSkipTodayMonthIfEmpty) result.add(getTodayMonth())
    return result.toList()
}

private fun allMonths(
    dates: Map<Int, Map<Int, Set<Int>>>,
    toSkipTodayMonthIfEmpty: Boolean
): List<Pair<Int, Int>> {
    if(dates.isEmpty()) {
        return if(toSkipTodayMonthIfEmpty) emptyList()
        else listOf(getTodayMonth())
    }
    val (minYear, minMonths) = dates.minBy { it.key }!!
    val (minMonth, _) = minMonths.minBy { it.key }!!
    val (maxYear, maxMonths) = dates.maxBy { it.key }!!
    val (maxMonth, _) = maxMonths.maxBy { it.key }!!

    if(toSkipTodayMonthIfEmpty) {
        return allMonths(
            minDate = minYear to minMonth,
            maxDate = maxYear to maxMonth
        )
    }

    val (todayYear, todayMonth) = getTodayMonth()

    val minDate = when {
        minYear > todayYear -> todayYear to todayMonth
        minYear == todayYear -> minYear to min(minMonth, todayMonth)
        else -> minYear to minMonth
    }
    val maxDate = when {
        maxYear < todayYear -> todayYear to todayMonth
        maxYear == todayYear -> maxYear to max(maxMonth, todayMonth)
        else -> maxYear to maxMonth
    }
    return allMonths(
        minDate = minDate,
        maxDate = maxDate
    )
}

private fun allMonths(
    minDate: Pair<Int, Int>,
    maxDate: Pair<Int, Int>
): List<Pair<Int, Int>> {
    val (minYear, minMonth) = minDate
    val (maxYear, maxMonth) = maxDate
    val result = mutableMapOf<Int, Set<Int>>()
    if(minYear == maxYear) {
        result[minYear] = (minMonth..maxMonth).toSet()
    } else {
        result[minYear] = (minMonth..11).toSet()
        for(year in (minYear+1) until maxYear) {
            result[year] = (0..11).toSet()
        }
        result[maxYear] = (0..maxMonth).toSet()
    }
    return result.flatMap { (year, months) ->
        months.map { month ->
            year to month
        }
    }
}

private fun getCurrentQueueMonth(
    dates: Map<Int, Map<Int, Set<Int>>>,
    toSkipEmptyMonths: Boolean,
    toSkipTodayMonthIfEmpty: Boolean,
    currentMonth: Pair<Int, Int>?
): Triple<Pair<Int, Int>?, Pair<Int, Int>?, Pair<Int, Int>?> {
    if(dates.isEmpty()) return Triple(null, null, null)
    currentMonth ?: return Triple(null, null, null)
    var prevMonth: Pair<Int, Int>? = null
    val months = getMonths(dates, toSkipEmptyMonths, toSkipTodayMonthIfEmpty)
    val iterator = months.iterator()
    while(iterator.hasNext()) {
        val month = iterator.next()
        if(month == currentMonth) {
            val nextMonth = if(iterator.hasNext()) iterator.next() else null
            return Triple(prevMonth, currentMonth, nextMonth)
        }
        prevMonth = month
    }
    return Triple(null, currentMonth, null)
}

private fun isSelected(
    year: Int, month: Int, dayOfMonth: Int,
    selectedDate: Triple<Int, Int, Int>?,
    isAutoSelectToday: Boolean,
    isToday: Boolean
): Boolean {
    return if(selectedDate == null) {
        isAutoSelectToday && isToday
    } else selectedDate.first == year
        && selectedDate.second == month
        && selectedDate.third == dayOfMonth
}

private fun drawRegularVerticalLines(
    canvas: Canvas,
    daysInWeek: Int,
    dayCellWidth: Int,
    dayCellHeight: Int,
    xOffset: Float,
    weeksInMonth: Int,
    paint: Paint
) {
    val linesCount = daysInWeek - 1
    val stopY: Float = (dayCellHeight * weeksInMonth).toFloat()
    canvas.drawLines(
        FloatArray(4 * linesCount).apply {
            repeat(linesCount) { index ->
                val x = xOffset + dayCellWidth * index + dayCellWidth
                val i = index * 4
                set(i, x)
                set(i + 1, 0f)
                set(i + 2, x)
                set(i + 3, stopY)
            }
        },
        paint
    )
}

private fun drawRegularHorizontalLines(
    canvas: Canvas,
    daysInWeek: Int,
    dayCellWidth: Int,
    dayCellHeight: Int,
    xOffset: Float,
    weeksInMonth: Int,
    paint: Paint
) {
    val linesCount = weeksInMonth - 1
    val stopX = xOffset + dayCellWidth * daysInWeek
    canvas.drawLines(
        FloatArray(4 * linesCount).apply {
            repeat(linesCount) { index ->
                val y = (dayCellHeight * index + dayCellHeight).toFloat()
                val i = index * 4
                set(i, xOffset)
                set(i + 1, y)
                set(i + 2, stopX)
                set(i + 3, y)
            }
        },
        paint
    )
}

private fun drawIOSStyleHorizontalLines(
    canvas: Canvas,
    daysInWeek: Int,
    dayCellWidth: Int,
    dayCellHeight: Int,
    xOffset: Float,
    emptyCellCount: Int,
    paint: Paint
) {
    val stopX = xOffset + dayCellWidth * daysInWeek
    canvas.drawLines(
        FloatArray(4 * 3).apply {
            repeat(3) { index ->
                val y = (dayCellHeight * index + dayCellHeight).toFloat()
                val i = index * 4
                set(i, xOffset)
                set(i + 1, y)
                set(i + 2, stopX)
                set(i + 3, y)
            }
        },
        paint
    )
    if(emptyCellCount < 5 || emptyCellCount > 14) throw IllegalArgumentException(
        "Empty Cell Count value must be in 5..14"
    )
    when(emptyCellCount) {
        14 -> return
        5, 6 -> {
            val y3 = (dayCellHeight * 4).toFloat()
            canvas.drawLine(
                xOffset,
                y3,
                stopX,
                y3,
                paint
            )
            val y4 = (dayCellHeight * 5).toFloat()
            canvas.drawLine(
                xOffset,
                y4,
                xOffset + dayCellWidth * (daysInWeek - emptyCellCount),
                y4,
                paint
            )
        }
        7 -> {
            val y3 = (dayCellHeight * 4).toFloat()
            canvas.drawLine(
                xOffset,
                y3,
                stopX,
                y3,
                paint
            )
        }
        else -> {
            val y3 = (dayCellHeight * 4).toFloat()
            canvas.drawLine(
                xOffset,
                y3,
                xOffset + dayCellWidth * (daysInWeek - emptyCellCount + 7),
                y3,
                paint
            )
        }
    }
}

private val techRect = Rect()
private fun Paint.getTextWidth(text: String): Int {
    getTextBounds(text, 0, text.length, techRect)
    return techRect.right
}
private fun Paint.getTextHeight(text: String): Int {
    getTextBounds(text, 0, text.length, techRect)
    return techRect.height()
}