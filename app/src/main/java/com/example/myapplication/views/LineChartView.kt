package com.example.myapplication.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.myapplication.data.AssetChange

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<AssetChange> = emptyList()
    private var isFullscreen = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Zoom 관련 변수
    private var scaleFactor = 1f
    private var scaleGestureDetector: ScaleGestureDetector
    private var focusX = 0f
    private var focusY = 0f
    private var isZoomEnabled = false
    
    private val investmentColor = Color.parseColor("#FF6384")
    private val evaluationColor = Color.parseColor("#36A2EB")
    private val gridColor = Color.parseColor("#E0E0E0")

    init {
        textPaint.color = Color.BLACK
        textPaint.textSize = 48f
        textPaint.textAlign = Paint.Align.CENTER
        
        gridPaint.color = gridColor
        gridPaint.strokeWidth = 2f
        
        // ScaleGestureDetector 초기화
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f) // 최소 0.5배, 최대 3배
                focusX = detector.focusX
                focusY = detector.focusY
                invalidate()
                return true
            }
        })
    }

    fun setData(data: List<AssetChange>) {
        this.data = data
        invalidate()
    }
    
    // 전체 화면 모드에서 글자 크기 조정
    fun setFullscreenMode(isFullscreen: Boolean) {
        this.isFullscreen = isFullscreen
        invalidate()
    }
    
    // Zoom 활성화/비활성화
    fun setZoomEnabled(enabled: Boolean) {
        this.isZoomEnabled = enabled
        if (!enabled) {
            scaleFactor = 1f // zoom 비활성화 시 원래 크기로 리셋
            invalidate()
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isZoomEnabled) {
            scaleGestureDetector.onTouchEvent(event)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Zoom 적용 (활성화된 경우에만)
        if (isZoomEnabled) {
            canvas.save()
            canvas.scale(scaleFactor, scaleFactor, focusX, focusY)
        }
        
        if (data.isEmpty()) {
            // 데이터가 없을 때 메시지 표시
            textPaint.color = Color.GRAY
            textPaint.textSize = 48f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("데이터를 선택해주세요", width / 2f, height / 2f - 30f, textPaint)
            canvas.drawText("(샘플 데이터 표시 중)", width / 2f, height / 2f + 30f, textPaint)
            // Zoom 복원 (활성화된 경우에만)
            if (isZoomEnabled) {
                canvas.restore()
            }
            return
        }

        val padding = 80f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        val startX = padding
        val startY = padding
        val endX = startX + chartWidth
        val endY = startY + chartHeight

        // 배경 그리기
        paint.color = Color.WHITE
        canvas.drawRect(startX, startY, endX, endY, paint)

        // 격자 그리기
        drawGrid(canvas, startX, startY, endX, endY)

        // 데이터 범위 계산
        val maxValue = data.maxOfOrNull { maxOf(it.investmentAmount, it.evaluationAmount) } ?: 1L
        val minValue = data.minOfOrNull { minOf(it.investmentAmount, it.evaluationAmount) } ?: 0L
        val valueRange = maxValue - minValue

        if (valueRange == 0L) return

        // 투자금액 라인 그리기
        drawLine(canvas, data.map { it.investmentAmount }, investmentColor, startX, startY, endX, endY, minValue, valueRange, "투자금액")

        // 평가금액 라인 그리기
        drawLine(canvas, data.map { it.evaluationAmount }, evaluationColor, startX, startY, endX, endY, minValue, valueRange, "평가금액")

        // X축 라벨 그리기
        drawXAxisLabels(canvas, startX, startY, endX, endY)
        
        // Y축 라벨 그리기
        drawYAxisLabels(canvas, startX, startY, endX, endY, minValue, valueRange)
        
        // 범례 그리기
        drawLegend(canvas, startX, startY, endX, endY)
        
        // Zoom 복원 (활성화된 경우에만)
        if (isZoomEnabled) {
            canvas.restore()
        }
    }

    private fun drawGrid(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        // 수평 격자
        for (i in 0..4) {
            val y = startY + (endY - startY) * i / 4
            canvas.drawLine(startX, y, endX, y, gridPaint)
        }
        
        // 수직 격자
        for (i in 0..data.size) {
            val x = startX + (endX - startX) * i / data.size
            canvas.drawLine(x, startY, x, endY, gridPaint)
        }
    }

    private fun drawLine(
        canvas: Canvas,
        values: List<Long>,
        color: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        minValue: Long,
        valueRange: Long,
        label: String
    ) {
        if (values.isEmpty()) return

        paint.color = color
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE

        // Python 코드처럼 투자금액은 점선, 평가금액은 실선으로 표시
        if (label == "투자금액") {
            paint.pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f) // 점선
        } else {
            paint.pathEffect = null // 실선
        }

        // 2개 이상의 데이터 포인트가 있을 때만 라인 그리기
        if (values.size > 1) {
            val path = Path()
            var isFirst = true

            values.forEachIndexed { index, value ->
                val x = startX + (endX - startX) * index / (values.size - 1)
                val normalizedValue = (value - minValue).toFloat() / valueRange
                val y = endY - (endY - startY) * normalizedValue

                if (isFirst) {
                    path.moveTo(x, y)
                    isFirst = false
                } else {
                    path.lineTo(x, y)
                }
            }

            canvas.drawPath(path, paint)
        }

        // 점 그리기 (데이터 포인트가 1개일 때도 점은 표시)
        paint.style = Paint.Style.FILL
        paint.pathEffect = null // 점은 실선으로
        values.forEachIndexed { index, value ->
            val x = startX + (endX - startX) * index / (values.size - 1)
            val normalizedValue = (value - minValue).toFloat() / valueRange
            val y = endY - (endY - startY) * normalizedValue
            canvas.drawCircle(x, y, 8f, paint)
            
            // 평가금액인 경우 각 점에 대한 상세 정보 표시
            if (label == "평가금액" && index < data.size) {
                val assetChange = data[index]
                val totalAmount = assetChange.evaluationAmount
                val profitAmount = assetChange.evaluationAmount - assetChange.investmentAmount
                val profitRate = if (assetChange.investmentAmount > 0) {
                    (profitAmount.toFloat() / assetChange.investmentAmount * 100)
                } else 0f
                
                val infoText = "${formatAmountInManwon(totalAmount)} (${formatAmountInManwon(profitAmount)}, ${String.format("%.1f", profitRate)}%)"
                
                // 텍스트 그리기
                textPaint.color = Color.BLACK
                textPaint.textSize = 30f
                textPaint.textAlign = Paint.Align.CENTER
                
                // 텍스트 배경 그리기 (테두리 없이)
                val textBounds = Rect()
                textPaint.getTextBounds(infoText, 0, infoText.length, textBounds)
                val textWidth = textBounds.width().toFloat()
                val textHeight = textBounds.height().toFloat()
                val padding = 8f
                
                val bgLeft = x - textWidth / 2 - padding
                val bgRight = x + textWidth / 2 + padding
                val bgTop = y - textHeight - 20f - padding
                val bgBottom = y - 20f + padding
                
                // 배경만 그리기 (테두리 제거)
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, 8f, 8f, paint)
                
                // 텍스트 그리기
                canvas.drawText(infoText, x, y - 20f, textPaint)
            }
        }
    }

    private fun drawXAxisLabels(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        data.forEachIndexed { index, item ->
            val x = startX + (endX - startX) * index / (data.size - 1)
            val y = endY + 60f
            textPaint.color = Color.BLACK
            textPaint.textSize = if (isFullscreen) 48f else 32f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(item.date, x, y, textPaint)
        }
    }
    
    private fun drawYAxisLabels(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, minValue: Long, valueRange: Long) {
        for (i in 0..4) {
            val y = startY + (endY - startY) * i / 4
            val value = minValue + (valueRange * (4 - i) / 4)
            val valueText = formatAmount(value)
            
            textPaint.color = Color.BLACK
            textPaint.textSize = if (isFullscreen) 42f else 28f
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(valueText, startX - 20f, y + 10f, textPaint)
        }
    }
    
    private fun drawLegend(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        val legendY = startY - 50f
        val legendItemWidth = 150f
        
        // 투자금액 범례
        paint.color = investmentColor
        paint.strokeWidth = 6f
        canvas.drawLine(startX, legendY, startX + 30f, legendY, paint)
        
        textPaint.color = Color.BLACK
        textPaint.textSize = if (isFullscreen) 48f else 32f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("투자금액", startX + 40f, legendY + 10f, textPaint)
        
        // 평가금액 범례
        paint.color = evaluationColor
        canvas.drawLine(startX + legendItemWidth, legendY, startX + legendItemWidth + 30f, legendY, paint)
        
        canvas.drawText("평가금액", startX + legendItemWidth + 40f, legendY + 10f, textPaint)
    }
    
    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 100000000 -> "${amount / 100000000}억"
            amount >= 10000 -> "${amount / 10000}만"
            else -> amount.toString()
        }
    }
    
    private fun formatAmountInManwon(amount: Long): String {
        val manwon = amount / 10000
        return String.format("%,d만원", manwon)
    }
}


