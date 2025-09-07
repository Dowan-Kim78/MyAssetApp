package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.AssetChange
import com.example.myapplication.data.AssetDistribution
import com.example.myapplication.data.Criteria
import com.example.myapplication.data.Investor
import com.example.myapplication.utils.ExcelReader
import com.example.myapplication.utils.ExcelData
import com.example.myapplication.views.LineChartView
import com.example.myapplication.views.PieChartView

class FullscreenChartActivity : AppCompatActivity() {
    
    private lateinit var pieChart: PieChartView
    private lateinit var lineChart: LineChartView
    private lateinit var btnClose: ImageButton
    private lateinit var excelReader: ExcelReader
    private var excelData: ExcelData? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_chart)
        
        // 전체 화면 모드 설정
        hideSystemUI()
        
        initViews()
        setupClickListeners()
        
        excelReader = ExcelReader(this)
        
        // Intent에서 데이터 받아오기
        val chartType = intent.getStringExtra("chart_type")
        val investorName = intent.getStringExtra("investor")
        val criteriaName = intent.getStringExtra("criteria")
        
        if (chartType != null && investorName != null && criteriaName != null) {
            val investor = Investor.valueOf(investorName)
            val criteria = Criteria.valueOf(criteriaName)
            
            if (chartType == "pie") {
                setupPieChart(investor, criteria)
            } else if (chartType == "line") {
                setupLineChart(investor)
            }
        }
    }
    
    private fun initViews() {
        pieChart = findViewById(R.id.fullscreenPieChart)
        lineChart = findViewById(R.id.fullscreenLineChart)
        btnClose = findViewById(R.id.btnClose)
    }
    
    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            finish()
        }
    }
    
    private fun setupPieChart(investor: Investor, criteria: Criteria) {
        pieChart.visibility = View.VISIBLE
        lineChart.visibility = View.GONE
        
        // Excel 데이터 로드
        loadExcelData()
        
        // 분배 데이터 설정
        val distributions = excelData?.let { 
            excelReader.getAssetDistribution(investor, criteria, it)
        } ?: generateSampleDistribution(investor, criteria)
        
        pieChart.setFullscreenMode(false) // 일반 크기 유지
        pieChart.setZoomEnabled(true) // 전체 화면에서는 zoom 활성화
        pieChart.setData(distributions)
    }
    
    private fun setupLineChart(investor: Investor) {
        pieChart.visibility = View.GONE
        lineChart.visibility = View.VISIBLE
        
        // Excel 데이터 로드
        loadExcelData()
        
        // 자산 변동 데이터 설정
        val chartData = excelData?.let {
            excelReader.getSortedAssetChanges(investor, it)
        } ?: generateSampleAssetChanges(investor)
        
        lineChart.setFullscreenMode(false) // 일반 크기 유지
        lineChart.setZoomEnabled(true) // 전체 화면에서는 zoom 활성화
        lineChart.setData(chartData)
    }
    
    private fun loadExcelData() {
        try {
            val inputStream = assets.open("금전출납.xlsx")
            excelData = excelReader.readExcelData(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            excelData = null
        }
    }
    
    private fun generateSampleDistribution(investor: Investor, criteria: Criteria): List<AssetDistribution> {
        val totalAmount = 100000000L
        
        return when (criteria) {
            Criteria.ACCOUNT -> {
                val accounts = listOf("국민은행", "신한은행", "하나은행", "기타")
                val percentages = listOf(0.4f, 0.3f, 0.2f, 0.1f)
                
                accounts.zip(percentages).map { (name, percent) ->
                    AssetDistribution(name, (totalAmount * percent).toLong(), percent * 100)
                }
            }
            Criteria.PRODUCT -> {
                val products = listOf("주식", "채권", "펀드", "기타")
                val percentages = listOf(0.5f, 0.25f, 0.15f, 0.1f)
                
                products.zip(percentages).map { (name, percent) ->
                    AssetDistribution(name, (totalAmount * percent).toLong(), percent * 100)
                }
            }
            Criteria.SECTOR -> {
                val sectors = listOf("IT", "금융", "제조업", "기타")
                val percentages = listOf(0.35f, 0.25f, 0.25f, 0.15f)
                
                sectors.zip(percentages).map { (name, percent) ->
                    AssetDistribution(name, (totalAmount * percent).toLong(), percent * 100)
                }
            }
        }
    }
    
    private fun generateSampleAssetChanges(investor: Investor): List<AssetChange> {
        return when (investor) {
            Investor.DOWAN -> listOf(
                AssetChange("25.08.01", 120000000L, 115000000L),
                AssetChange("25.09.01", 125000000L, 120000000L),
                AssetChange("25.10.01", 130000000L, 125000000L)
            )
            Investor.YOUNGHEE -> listOf(
                AssetChange("25.08.01", 25000000L, 24000000L),
                AssetChange("25.09.01", 28000000L, 27000000L),
                AssetChange("25.10.01", 30000000L, 29000000L)
            )
            Investor.JIAN -> listOf(
                AssetChange("25.08.01", 3000000L, 3100000L),
                AssetChange("25.09.01", 3200000L, 3300000L),
                AssetChange("25.10.01", 3500000L, 3600000L)
            )
            Investor.JIWOO -> listOf(
                AssetChange("25.08.01", 2500000L, 2600000L),
                AssetChange("25.09.01", 2700000L, 2800000L),
                AssetChange("25.10.01", 2900000L, 3000000L)
            )
        }
    }
    
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }
}
