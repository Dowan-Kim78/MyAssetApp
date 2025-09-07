package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import com.example.myapplication.data.AssetChange
import com.example.myapplication.data.AssetDistribution
import com.example.myapplication.data.Criteria
import com.example.myapplication.data.Investor
import com.example.myapplication.utils.ExcelReader
import com.example.myapplication.utils.ExcelData
import com.example.myapplication.views.LineChartView
import com.example.myapplication.views.PieChartView
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1001
        private const val UPLOADED_FILE_NAME = "uploaded_asset_data.xlsx"
    }
    
    private lateinit var btnDowan: Button
    private lateinit var btnYounghee: Button
    private lateinit var btnJian: Button
    private lateinit var btnJiwoo: Button
    
    private lateinit var btnAccount: Button
    private lateinit var btnProduct: Button
    private lateinit var btnSector: Button
    
    private lateinit var tvSelectedInfo: TextView
    private lateinit var pieChart: PieChartView
    private lateinit var lineChart: LineChartView
    private lateinit var btnUploadFile: ImageButton
    
    private var selectedInvestor: Investor? = null
    private var selectedCriteria: Criteria? = null
    private lateinit var excelReader: ExcelReader
    private var assetData: Map<Investor, List<AssetChange>> = emptyMap()
    private var excelData: ExcelData? = null
    
    // 샘플 데이터 (Excel 파일을 읽을 수 없을 때 사용)
    private val sampleAssetChanges = mapOf(
        Investor.DOWAN to listOf(
            AssetChange("25.08.01", 120000000L, 115000000L),
            AssetChange("25.09.01", 125000000L, 120000000L),
            AssetChange("25.10.01", 130000000L, 125000000L)
        ),
        Investor.YOUNGHEE to listOf(
            AssetChange("25.08.01", 25000000L, 24000000L),
            AssetChange("25.09.01", 28000000L, 27000000L),
            AssetChange("25.10.01", 30000000L, 29000000L)
        ),
        Investor.JIAN to listOf(
            AssetChange("25.08.01", 3000000L, 3100000L),
            AssetChange("25.09.01", 3200000L, 3300000L),
            AssetChange("25.10.01", 3500000L, 3600000L)
        ),
        Investor.JIWOO to listOf(
            AssetChange("25.08.01", 2500000L, 2600000L),
            AssetChange("25.09.01", 2700000L, 2800000L),
            AssetChange("25.10.01", 2900000L, 3000000L)
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        setupCharts()
        
        excelReader = ExcelReader(this)
        loadExcelData()
        
        // 초기 상태 설정
        updateButtonStates()
        updateSelectedInfo()
        updateCharts()
    }
    
    private fun initViews() {
        btnDowan = findViewById(R.id.btnDowan)
        btnYounghee = findViewById(R.id.btnYounghee)
        btnJian = findViewById(R.id.btnJian)
        btnJiwoo = findViewById(R.id.btnJiwoo)
        
        btnAccount = findViewById(R.id.btnAccount)
        btnProduct = findViewById(R.id.btnProduct)
        btnSector = findViewById(R.id.btnSector)
        
        tvSelectedInfo = findViewById(R.id.tvSelectedInfo)
        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)
        btnUploadFile = findViewById(R.id.btnUploadFile)
    }
    
    private fun setupClickListeners() {
        // 투자자 선택 버튼들
        btnDowan.setOnClickListener { selectInvestor(Investor.DOWAN) }
        btnYounghee.setOnClickListener { selectInvestor(Investor.YOUNGHEE) }
        btnJian.setOnClickListener { selectInvestor(Investor.JIAN) }
        btnJiwoo.setOnClickListener { selectInvestor(Investor.JIWOO) }
        
        // 기준 선택 버튼들
        btnAccount.setOnClickListener { selectCriteria(Criteria.ACCOUNT) }
        btnProduct.setOnClickListener { selectCriteria(Criteria.PRODUCT) }
        btnSector.setOnClickListener { selectCriteria(Criteria.SECTOR) }
        
        // 차트 클릭 리스너
        pieChart.setOnClickListener { openFullscreenChart("pie") }
        lineChart.setOnClickListener { openFullscreenChart("line") }
        
        // 파일 업로드 버튼
        btnUploadFile.setOnClickListener { openFilePicker() }
    }
    
    private fun selectInvestor(investor: Investor) {
        selectedInvestor = investor
        updateButtonStates()
        updateSelectedInfo()
        updateCharts()
    }
    
    private fun selectCriteria(criteria: Criteria) {
        selectedCriteria = criteria
        updateButtonStates()
        updateSelectedInfo()
        updateCharts()
    }
    
    private fun updateButtonStates() {
        // 투자자 버튼 상태 업데이트
        val investorButtons = listOf(btnDowan, btnYounghee, btnJian, btnJiwoo)
        val investors = listOf(Investor.DOWAN, Investor.YOUNGHEE, Investor.JIAN, Investor.JIWOO)
        
        investorButtons.forEachIndexed { index, button ->
            button.isSelected = selectedInvestor == investors[index]
        }
        
        // 기준 버튼 상태 업데이트
        val criteriaButtons = listOf(btnAccount, btnProduct, btnSector)
        val criteria = listOf(Criteria.ACCOUNT, Criteria.PRODUCT, Criteria.SECTOR)
        
        criteriaButtons.forEachIndexed { index, button ->
            button.isSelected = selectedCriteria == criteria[index]
        }
    }
    
    private fun updateSelectedInfo() {
        val info = when {
            selectedInvestor != null && selectedCriteria != null -> 
                "선택된 사용자: ${selectedInvestor!!.displayName}, 기준: ${selectedCriteria!!.displayName}"
            selectedInvestor != null -> 
                "선택된 사용자: ${selectedInvestor!!.displayName}, 기준을 선택해주세요"
            selectedCriteria != null -> 
                "사용자를 선택해주세요, 선택된 기준: ${selectedCriteria!!.displayName}"
            else -> "사용자와 기준을 선택해주세요"
        }
        tvSelectedInfo.text = info
    }
    
    private fun updateCharts() {
        if (selectedInvestor != null && selectedCriteria != null) {
            // UI 스레드에서 즉시 업데이트
            runOnUiThread {
                updatePieChart()
                updateLineChart()
            }
        } else {
            // 선택이 완료되지 않았을 때는 빈 그래프 표시
            runOnUiThread {
                pieChart.setData(emptyList())
                lineChart.setData(emptyList())
            }
        }
    }
    
    private fun updatePieChart() {
        val investor = selectedInvestor!!
        val criteria = selectedCriteria!!
        
        // Excel 데이터를 기반으로 분배 현황 생성
        val distributions = if (excelData != null) {
            excelReader.getAssetDistribution(investor, criteria, excelData!!)
        } else {
            // 샘플 데이터 기반으로 분배 현황 생성 (데이터 로드 실패 시)
            generateSampleDistribution(investor, criteria)
        }
        
        pieChart.setData(distributions)
    }
    
    private fun updateLineChart() {
        val investor = selectedInvestor!!
        
        println("=== updateLineChart 디버깅 ===")
        println("투자자: ${investor.displayName}")
        println("excelData가 null인가? ${excelData == null}")
        
        // Excel 데이터가 있으면 정렬된 데이터 사용, 없으면 샘플 데이터 사용
        val chartData = if (excelData != null) {
            val sortedData = excelReader.getSortedAssetChanges(investor, excelData!!)
            println("Excel에서 읽어온 자산 변동 데이터: $sortedData")
            sortedData
        } else {
            val sampleData = sampleAssetChanges[investor] ?: emptyList()
            println("샘플 자산 변동 데이터 사용: $sampleData")
            sampleData
        }
        
        println("최종 차트 데이터: $chartData")
        lineChart.setData(chartData)
    }
    
    // 샘플 데이터 기반으로 분배 현황 생성
    private fun generateSampleDistribution(investor: Investor, criteria: Criteria): List<AssetDistribution> {
        val totalAmount = sampleAssetChanges[investor]?.lastOrNull()?.evaluationAmount ?: 100000000L
        
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
    
    private fun loadExcelData() {
        println("=== MainActivity: Excel 데이터 로드 시작 ===")
        try {
            val inputStream = getExcelInputStream()
            if (inputStream == null) {
                println("Excel 파일을 찾을 수 없습니다")
                Toast.makeText(this, "Excel 파일을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }
            println("Excel 파일 열기 성공")
            
            println("Excel 데이터 읽기 시작...")
            excelData = excelReader.readExcelData(inputStream)
            println("Excel 데이터 읽기 완료")
            
            assetData = excelData!!.assetChanges
            inputStream.close()
            
            println("Excel 데이터 로드 성공")
            println("계좌 목록: ${excelData!!.accountList}")
            println("상품 목록: ${excelData!!.productList}")
            println("섹터 목록: ${excelData!!.sectorList}")
            
            // 데이터 로드 성공 시 메시지 표시
            runOnUiThread {
                val latestDate = excelData?.assetChanges?.values?.firstOrNull()?.lastOrNull()?.date ?: "알 수 없음"
                Toast.makeText(this, "최신 데이터를 로드했습니다\n(날짜: $latestDate)", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            println("Excel 데이터 로드 실패: ${e.message}")
            e.printStackTrace()
            
            // Excel 파일을 읽을 수 없으면 샘플 데이터 사용
            assetData = sampleAssetChanges
            excelData = null
            
            // 데이터 로드 실패 메시지 표시
            runOnUiThread {
                Toast.makeText(this, "데이터 로드를 실패했습니다\n샘플 데이터를 사용합니다", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupCharts() {
        // 일반 모드에서는 zoom 비활성화
        pieChart.setZoomEnabled(false)
        lineChart.setZoomEnabled(false)
    }
    
    private fun openFullscreenChart(chartType: String) {
        if (selectedInvestor == null || selectedCriteria == null) {
            Toast.makeText(this, "사용자와 기준을 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, FullscreenChartActivity::class.java)
        intent.putExtra("chart_type", chartType)
        intent.putExtra("investor", selectedInvestor!!.name)
        intent.putExtra("criteria", selectedCriteria!!.name)
        startActivity(intent)
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        
        // 파일 이름 필터 설정 (금전출납.xlsx만 선택 가능)
        intent.putExtra(Intent.EXTRA_TITLE, "금전출납.xlsx 파일 선택")
        
        // MIME 타입을 더 구체적으로 설정
        intent.type = "*/*"
        val mimeTypes = arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // 파일명 검증
                    val fileName = getFileName(uri)
                    if (fileName != "금전출납.xlsx") {
                        Toast.makeText(this, "금전출납.xlsx 파일만 업로드할 수 있습니다", Toast.LENGTH_LONG).show()
                        return
                    }
                    
                    saveUploadedFile(uri)
                    Toast.makeText(this, "파일이 업로드되었습니다", Toast.LENGTH_SHORT).show()
                    loadExcelData() // 업로드된 파일로 데이터 다시 로드
                } catch (e: Exception) {
                    Toast.makeText(this, "파일 업로드에 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun saveUploadedFile(uri: Uri) {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(filesDir, UPLOADED_FILE_NAME)
        val outputStream = FileOutputStream(file)
        
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }
    
    private fun getExcelInputStream(): InputStream? {
        // 먼저 업로드된 파일이 있는지 확인
        val uploadedFile = File(filesDir, UPLOADED_FILE_NAME)
        return if (uploadedFile.exists()) {
            uploadedFile.inputStream()
        } else {
            // 업로드된 파일이 없으면 기본 assets 파일 사용
            assets.open("금전출납.xlsx")
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}
