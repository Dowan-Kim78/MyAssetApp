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
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 선택된 섹터 관련 변수
    private var selectedSectorIndex = -1
    private var overlayText = ""
    
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
        textPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK) // 텍스트 그림자 추가
        
        linePaint.color = Color.BLACK
        linePaint.strokeWidth = 3f
        linePaint.style = Paint.Style.STROKE
        
        // 섹터 테두리용 페인트 설정
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 4f
        strokePaint.style = Paint.Style.STROKE
        strokePaint.setShadowLayer(2f, 1f, 1f, Color.BLACK)
        
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
        
        // 터치 이벤트 처리
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouch(event.x, event.y)
                return true
            }
        }
        return true
    }
    
    private fun handleTouch(x: Float, y: Float) {
        if (data.isEmpty()) return
        
        val centerX = width / 2f
        val centerY = height / 2f - 80f
        val radius = minOf(centerX, centerY) * 0.6f
        
        // 터치 지점이 차트 영역 내에 있는지 확인
        val distance = kotlin.math.sqrt(
            ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble()
        ).toFloat()
        
        if (distance <= radius) {
            // 터치 각도 계산
            val angle = kotlin.math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
            var touchAngle = Math.toDegrees(angle).toFloat()
            
            // -90도부터 시작하도록 조정
            if (touchAngle < 0) touchAngle += 360f
            touchAngle = (touchAngle + 90f) % 360f
            
            // 해당 각도에 해당하는 섹터 찾기
            var currentAngle = 0f
            for (i in data.indices) {
                val sweepAngle = (data[i].percentage / 100f) * 360f
                if (touchAngle >= currentAngle && touchAngle < currentAngle + sweepAngle) {
                    selectedSectorIndex = i
                    overlayText = "${data[i].category}\n${String.format("%.1f", data[i].percentage)}%\n금액: ${String.format("%,d", data[i].amount)}원"
                    invalidate()
                    break
                }
                currentAngle += sweepAngle
            }
        } else {
            // 차트 영역 밖을 터치하면 선택 해제
            selectedSectorIndex = -1
            overlayText = ""
            invalidate()
        }
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
        val centerY = height / 2f - 80f // 하단 마진을 위해 차트를 위로 이동
        val radius = minOf(centerX, centerY) * 0.6f // 차트 크기를 줄여서 라벨 공간 확보

        var startAngle = -90f // 시작 각도를 12시 방향으로 설정

        data.forEachIndexed { index, item ->
            val sweepAngle = (item.percentage / 100f) * 360f
            
            // 선택된 섹터인지 확인
            val isSelected = selectedSectorIndex == index
            
            paint.color = colors[index % colors.size]
            
            // 선택된 섹터는 약간 밝게 표시
            if (isSelected) {
                val hsv = FloatArray(3)
                Color.colorToHSV(colors[index % colors.size], hsv)
                hsv[2] = minOf(1.0f, hsv[2] + 0.2f) // 밝기 증가
                paint.color = Color.HSVToColor(hsv)
            }
            
            // 섹터 그리기
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
            
            // 섹터 테두리 그리기 (시인성 향상)
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                true,
                strokePaint
            )

            // 각 섹터의 중앙 각도 계산
            val midAngle = startAngle + sweepAngle / 2f
            
            // 차트 내부에 텍스트를 표시할지 외부에 라벨을 표시할지 결정
            val shouldShowInternalText = sweepAngle > 30f // 30도 이상일 때만 내부에 텍스트 표시
            
            if (shouldShowInternalText) {
                // 차트 내부에 텍스트 표시 (배경 추가)
                val textRadius = radius * 0.4f
                val textX = centerX + (textRadius * kotlin.math.cos(Math.toRadians(midAngle.toDouble()))).toFloat()
                val textY = centerY + (textRadius * kotlin.math.sin(Math.toRadians(midAngle.toDouble()))).toFloat()

                val textContent = "${item.category}\n${String.format("%.1f", item.percentage)}%"
                textPaint.textSize = if (isFullscreen) 60f else 28f
                
                // 텍스트 배경 그리기
                val textBounds = Rect()
                textPaint.getTextBounds(textContent, 0, textContent.length, textBounds)
                val padding = 10f
                val bgWidth = textBounds.width() + padding * 2
                val bgHeight = textBounds.height() + padding * 2
                
                // 반투명 배경 그리기
                paint.color = Color.argb(180, 0, 0, 0)
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(
                    textX - bgWidth / 2,
                    textY - bgHeight / 2,
                    textX + bgWidth / 2,
                    textY + bgHeight / 2,
                    8f, 8f, paint
                )
                
                // 텍스트 그리기
                textPaint.color = Color.WHITE
                canvas.drawText(textContent, textX, textY + textBounds.height() / 3, textPaint)
            } else {
                // 차트 외부에 라벨 표시 (연결선 포함)
                drawExternalLabel(canvas, centerX, centerY, radius, midAngle, item, index)
            }

            startAngle += sweepAngle
        }
        
        // 선택된 섹터에 대한 오버레이 텍스트 표시
        if (selectedSectorIndex >= 0 && selectedSectorIndex < data.size) {
            drawOverlayText(canvas, centerX, centerY, radius)
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
    
    private fun drawOverlayText(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        if (overlayText.isEmpty()) return
        
        // 오버레이 텍스트 설정
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        overlayPaint.textSize = if (isFullscreen) 48f else 32f
        overlayPaint.textAlign = Paint.Align.CENTER
        overlayPaint.color = Color.WHITE
        overlayPaint.setShadowLayer(6f, 3f, 3f, Color.BLACK)
        
        // 텍스트 크기 측정
        val textBounds = Rect()
        overlayPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()
        
        // 오버레이 배경 설정
        val padding = 20f
        val bgWidth = textWidth + padding * 2
        val bgHeight = textHeight + padding * 2
        
        // 오버레이 위치 (차트 하단 중앙)
        val overlayX = centerX
        val overlayY = centerY + radius + 100f // 차트 아래쪽에 배치
        
        // 배경 그리기
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = Color.argb(220, 0, 0, 0) // 반투명 검은색 배경
        bgPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            overlayX - bgWidth / 2,
            overlayY - bgHeight / 2,
            overlayX + bgWidth / 2,
            overlayY + bgHeight / 2,
            15f, 15f, bgPaint
        )
        
        // 테두리 그리기
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = colors[selectedSectorIndex % colors.size]
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 4f
        canvas.drawRoundRect(
            overlayX - bgWidth / 2,
            overlayY - bgHeight / 2,
            overlayX + bgWidth / 2,
            overlayY + bgHeight / 2,
            15f, 15f, borderPaint
        )
        
        // 텍스트 그리기
        canvas.drawText(overlayText, overlayX, overlayY + textHeight / 3, overlayPaint)
    }
    
}


