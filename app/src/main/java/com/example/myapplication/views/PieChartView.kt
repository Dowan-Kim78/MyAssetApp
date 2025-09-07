package com.example.myapplication.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.myapplication.data.AssetDistribution

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<AssetDistribution> = emptyList()
    private var isFullscreen = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Zoom 관련 변수
    private var scaleFactor = 1f
    private var scaleGestureDetector: ScaleGestureDetector
    private var focusX = 0f
    private var focusY = 0f
    private var isZoomEnabled = false
    private val colors = listOf(
        Color.parseColor("#FF6384"), // 빨간색
        Color.parseColor("#36A2EB"), // 파란색
        Color.parseColor("#FFCE56"), // 노란색
        Color.parseColor("#4BC0C0"), // 청록색
        Color.parseColor("#9966FF"), // 보라색
        Color.parseColor("#FF9F40"), // 주황색
        Color.parseColor("#2ECC71"), // 초록색
        Color.parseColor("#E74C3C"), // 진한 빨간색
        Color.parseColor("#3498DB"), // 진한 파란색
        Color.parseColor("#F39C12"), // 진한 주황색
        Color.parseColor("#9B59B6"), // 진한 보라색
        Color.parseColor("#1ABC9C"), // 진한 청록색
        Color.parseColor("#E67E22"), // 갈색
        Color.parseColor("#34495E"), // 회색
        Color.parseColor("#E91E63"), // 핑크색
        Color.parseColor("#00BCD4")  // 시안색
    )

    init {
        textPaint.color = Color.WHITE
        textPaint.textSize = 63f // 70f에서 10% 감소 (70 * 0.9 = 63)
        textPaint.textAlign = Paint.Align.CENTER
        
        linePaint.color = Color.BLACK
        linePaint.strokeWidth = 3f
        linePaint.style = Paint.Style.STROKE
        
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

    fun setData(data: List<AssetDistribution>) {
        // 0.1% 이하인 값들을 제외하고 "기타" 항목으로 통합
        this.data = filterSmallValues(data)
        invalidate()
    }
    
    // 0.1% 이하인 값들을 필터링하고 "기타" 항목으로 통합
    private fun filterSmallValues(data: List<AssetDistribution>): List<AssetDistribution> {
        val threshold = 0.1f
        val filteredData = data.filter { it.percentage >= threshold }
        val smallValues = data.filter { it.percentage < threshold }
        
        if (smallValues.isNotEmpty()) {
            val otherTotalAmount = smallValues.sumOf { it.amount.toLong() }
            val otherTotalPercentage = smallValues.sumOf { it.percentage.toDouble() }.toFloat()
            
            if (otherTotalPercentage > 0f) {
                val otherItem = AssetDistribution("기타", otherTotalAmount, otherTotalPercentage)
                return filteredData + otherItem
            }
        }
        
        return filteredData
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
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("데이터를 선택해주세요", width / 2f, height / 2f - 20f, textPaint)
            canvas.drawText("(샘플 데이터 표시 중)", width / 2f, height / 2f + 20f, textPaint)
            // Zoom 복원 (활성화된 경우에만)
            if (isZoomEnabled) {
                canvas.restore()
            }
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) * 0.6f // 차트 크기를 줄여서 라벨 공간 확보

        var startAngle = -90f // 시작 각도를 12시 방향으로 설정

        data.forEachIndexed { index, item ->
            val sweepAngle = (item.percentage / 100f) * 360f
            
            paint.color = colors[index % colors.size]
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                true,
                paint
            )

            // 각 섹터의 중앙 각도 계산
            val midAngle = startAngle + sweepAngle / 2f
            
            // 차트 내부에 텍스트를 표시할지 외부에 라벨을 표시할지 결정
            val shouldShowInternalText = sweepAngle > 30f // 30도 이상일 때만 내부에 텍스트 표시
            
            if (shouldShowInternalText) {
                // 차트 내부에 텍스트 표시
                val textRadius = radius * 0.5f
                val textX = centerX + (textRadius * kotlin.math.cos(Math.toRadians(midAngle.toDouble()))).toFloat()
                val textY = centerY + (textRadius * kotlin.math.sin(Math.toRadians(midAngle.toDouble()))).toFloat()

                textPaint.color = Color.WHITE
                textPaint.textSize = if (isFullscreen) 72f else 36f // 일반 모드: 36f로 수정
                canvas.drawText(
                    "${item.category}\n${String.format("%.1f", item.percentage)}%",
                    textX,
                    textY,
                    textPaint
                )
            } else {
                // 차트 외부에 라벨 표시 (연결선 포함)
                drawExternalLabel(canvas, centerX, centerY, radius, midAngle, item, index)
            }

            startAngle += sweepAngle
        }
        
        // 범례 제거 - 차트 내부에 텍스트로 표시
        
        // Zoom 복원 (활성화된 경우에만)
        if (isZoomEnabled) {
            canvas.restore()
        }
    }
    
    private fun drawExternalLabel(
        canvas: Canvas, 
        centerX: Float, 
        centerY: Float, 
        radius: Float, 
        angle: Float, 
        item: AssetDistribution, 
        colorIndex: Int
    ) {
        // 차트 가장자리에서의 시작점 계산
        val startX = centerX + (radius * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val startY = centerY + (radius * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        
        // 연결선의 끝점 계산 (차트에서 더 멀리)
        val labelDistance = radius * 1.5f
        val endX = centerX + (labelDistance * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val endY = centerY + (labelDistance * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        
        // 연결선 그리기
        linePaint.color = colors[colorIndex % colors.size]
        canvas.drawLine(startX, startY, endX, endY, linePaint)
        
        // 라벨 배경 사각형 그리기
        paint.color = colors[colorIndex % colors.size]
        paint.style = Paint.Style.FILL
        
        val labelText = "${item.category}\n${String.format("%.1f", item.percentage)}%"
        textPaint.textSize = if (isFullscreen) 54f else 36f // 10% 감소 (60->54, 40->36)
        textPaint.textAlign = Paint.Align.CENTER
        
        // 텍스트 크기 측정
        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()
        
        // 배경 사각형 크기 (텍스트에 여백 추가)
        val padding = 20f
        val rectWidth = textWidth + padding * 2
        val rectHeight = textHeight + padding * 2
        
        // 사각형 그리기
        canvas.drawRoundRect(
            endX - rectWidth / 2,
            endY - rectHeight / 2,
            endX + rectWidth / 2,
            endY + rectHeight / 2,
            10f, 10f, paint
        )
        
        // 라벨 텍스트 그리기
        textPaint.color = Color.WHITE
        canvas.drawText(labelText, endX, endY + textHeight / 4, textPaint)
    }
    
}


