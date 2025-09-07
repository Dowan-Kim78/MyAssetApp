package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.data.AssetChange
import com.example.myapplication.data.AssetDistribution
import com.example.myapplication.data.Criteria
import com.example.myapplication.data.Investor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.FormulaEvaluator
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// Excel 데이터를 저장하는 데이터 클래스
data class ExcelData(
    var accountList: List<String> = emptyList(),
    var productList: List<String> = emptyList(),
    var sectorList: List<String> = emptyList(),
    val assetChanges: MutableMap<Investor, MutableList<AssetChange>> = mutableMapOf(),
    // 실제 분배 데이터 저장
    val accountValues: MutableMap<Investor, List<Long>> = mutableMapOf(),
    val productValues: MutableMap<Investor, List<Long>> = mutableMapOf(),
    val sectorValues: MutableMap<Investor, List<Long>> = mutableMapOf()
)

class ExcelReader(private val context: Context) {
    
    // 셀 값을 Long으로 변환하는 헬퍼 함수 (수식 포함)
    private fun getCellValueAsLong(cell: org.apache.poi.ss.usermodel.Cell?, evaluator: FormulaEvaluator): Long {
        return when (cell?.cellType) {
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toLong()
            org.apache.poi.ss.usermodel.CellType.STRING -> {
                val stringValue = cell.stringCellValue
                try {
                    // Remove commas and currency symbols, then convert to long
                    stringValue.replace(",", "").replace("원", "").replace("$", "").trim().toLong()
                } catch (e: NumberFormatException) {
                    0L
                }
            }
            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                try {
                    val evaluatedCell = evaluator.evaluate(cell)
                    when (evaluatedCell.cellType) {
                        org.apache.poi.ss.usermodel.CellType.NUMERIC -> evaluatedCell.numberValue.toLong()
                        org.apache.poi.ss.usermodel.CellType.STRING -> {
                            val stringValue = evaluatedCell.stringValue
                            try {
                                stringValue.replace(",", "").replace("원", "").replace("$", "").trim().toLong()
                            } catch (e: NumberFormatException) {
                                0L
                            }
                        }
                        else -> 0L
                    }
                } catch (e: Exception) {
                    println("Error evaluating formula in cell ${cell.address}: ${e.message}")
                    0L
                }
            }
            else -> 0L
        }
    }
    
    // Excel 파일에서 목록과 자산 데이터를 읽어오는 함수
    fun readExcelData(inputStream: InputStream): ExcelData {
        val workbook = WorkbookFactory.create(inputStream)
        val result = ExcelData()
        
        // 시트 이름 패턴: "우리가족_투자정보(날짜)"
        val sheetNamePattern = Regex("우리가족_투자정보\\((\\d{2}\\.\\d{1,2}\\.\\d{1,2})\\)")
        
        // 모든 우리가족_투자정보 시트를 찾아서 날짜별로 정렬
        val investmentSheets = mutableListOf<Pair<String, org.apache.poi.ss.usermodel.Sheet>>()
        
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            val sheetName = sheet.sheetName
            
            val match = sheetNamePattern.find(sheetName)
            if (match != null) {
                val dateStr = match.groupValues[1]
                investmentSheets.add(Pair(dateStr, sheet))
            }
        }
        
        // 모든 시트를 날짜순으로 정렬
        val sortedSheets = investmentSheets.sortedBy { (dateStr, _) ->
            parseDate(dateStr)
        }
        
        if (sortedSheets.isNotEmpty()) {
            println("=== Excel 파일 읽기 시작 ===")
            println("발견된 투자정보 시트들: ${sortedSheets.map { it.first }}")
            
            // FormulaEvaluator 생성
            val evaluator = workbook.creationHelper.createFormulaEvaluator()
            
            // 가장 최신 시트에서 목록 데이터 읽기 (첫 번째 시트는 가장 오래된 것)
            val (latestDate, latestSheet) = sortedSheets.last()
            println("목록 데이터 읽기용 시트: ${latestSheet.sheetName} (날짜: $latestDate)")
            
            // 목록 데이터 읽기 (최신 시트에서)
            println("계좌 목록 읽기 중... (B10:B13)")
            result.accountList = readListFromRange(latestSheet, 9, 1, 12, 1) // B10:B13
            
            println("상품 목록 읽기 중... (C15:C19)")
            result.productList = readListFromRange(latestSheet, 14, 2, 18, 2)
            
            println("섹터 목록 읽기 중... (D21:D31)")
            result.sectorList = readListFromRange(latestSheet, 20, 3, 30, 3)
            
            // 추가로 다른 위치에서도 시도
            if (result.productList.isEmpty()) {
                println("상품 목록이 비어있어 다른 위치에서 시도... (C1:C11)")
                result.productList = readListFromRange(latestSheet, 0, 2, 10, 2) // C1:C11
            }
            if (result.sectorList.isEmpty()) {
                println("섹터 목록이 비어있어 다른 위치에서 시도... (D1:D16)")
                result.sectorList = readListFromRange(latestSheet, 0, 3, 15, 3) // D1:D16
            }
            
            // 디버깅을 위한 로그
            println("Excel 데이터 파싱 결과 (최신 시트: $latestDate):")
            println("계좌 목록: ${result.accountList}")
            println("상품 목록: ${result.productList}")
            println("섹터 목록: ${result.sectorList}")
            
            // 모든 시트에서 자산 변동 데이터 읽기
            println("=== 모든 시트에서 자산 변동 데이터 읽기 시작 ===")
            
            val investorCols = mapOf(
                Investor.DOWAN to Pair(5, 7), // F열(투자금액), H열(평가금액)
                Investor.YOUNGHEE to Pair(11, 13), // L열(투자금액), N열(평가금액)
                Investor.JIAN to Pair(17, 19), // R열(투자금액), T열(평가금액)
                Investor.JIWOO to Pair(23, 25) // X열(투자금액), Z열(평가금액)
            )
            
            // 각 시트에서 자산 변동 데이터 읽기
            sortedSheets.forEach { (dateStr, sheet) ->
                println("시트 ${sheet.sheetName} (날짜: $dateStr)에서 데이터 읽기")
                
                investorCols.forEach { (investor, cols) ->
                    try {
                        val row = sheet.getRow(2) // 3번째 행 (0-based)
                        
                        val investmentCell = row?.getCell(cols.first)
                        val evaluationCell = row?.getCell(cols.second)
                        
                        val investmentAmount = getCellValueAsLong(investmentCell, evaluator)
                        val evaluationAmount = getCellValueAsLong(evaluationCell, evaluator)
                        
                        println("  ${investor.displayName}: 투자금액=$investmentAmount, 평가금액=$evaluationAmount")
                        
                        result.assetChanges[investor] = result.assetChanges.getOrPut(investor) { mutableListOf() }
                        result.assetChanges[investor]!!.add(
                            AssetChange(
                                date = dateStr,
                                investmentAmount = investmentAmount,
                                evaluationAmount = evaluationAmount
                            )
                        )
                    } catch (e: Exception) {
                        println("  ${investor.displayName} 데이터 읽기 실패: ${e.message}")
                        // 오류 발생 시 0으로 처리
                        result.assetChanges[investor] = result.assetChanges.getOrPut(investor) { mutableListOf() }
                        result.assetChanges[investor]!!.add(
                            AssetChange(
                                date = dateStr,
                                investmentAmount = 0L,
                                evaluationAmount = 0L
                            )
                        )
                    }
                }
            }
            
            println("=== 자산 변동 데이터 읽기 완료 ===")
            println("읽어온 데이터 요약:")
            result.assetChanges.forEach { (investor, changes) ->
                println("  ${investor.displayName}: ${changes.size}개 데이터 포인트")
                changes.forEach { change ->
                    println("    ${change.date}: 투자=${change.investmentAmount}, 평가=${change.evaluationAmount}")
                }
            }
            
            // Excel 시트 구조 디버깅 (최신 시트)
            debugSheetStructure(latestSheet)
            
            // 각 투자자별 분배 데이터 읽기 (최신 시트에서)
            readDistributionValues(latestSheet, result, evaluator)
        } else {
            println("우리가족_투자정보 시트를 찾을 수 없습니다.")
        }
        
        workbook.close()
        return result
    }
    
    // 날짜 문자열을 파싱하여 비교 가능한 형태로 변환
    private fun parseDate(dateStr: String): Long {
        return try {
            val parts = dateStr.split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            // 2000년 기준으로 변환 (23 -> 2023)
            val fullYear = if (year < 100) 2000 + year else year
            
            // 날짜를 Long 값으로 변환 (YYYYMMDD 형식)
            fullYear * 10000L + month * 100L + day
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun readListFromRange(sheet: org.apache.poi.ss.usermodel.Sheet, startRow: Int, col: Int, endRow: Int, endCol: Int): List<String> {
        val list = mutableListOf<String>()
        println("    목록 읽기: 행 ${startRow}-${endRow}, 열 ${col}")
        
        for (rowIndex in startRow..endRow) {
            val row = sheet.getRow(rowIndex)
            if (row == null) {
                println("      행 $rowIndex: null")
                continue
            }
            
            val cell = row.getCell(col)
            if (cell == null) {
                println("      행 $rowIndex, 열 $col: null")
                continue
            }
            
            val value = when (cell.cellType) {
                org.apache.poi.ss.usermodel.CellType.STRING -> {
                    val strValue = cell.stringCellValue?.trim()
                    println("      행 $rowIndex, 열 $col: 문자열 = \"$strValue\"")
                    strValue
                }
                org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                    val numValue = cell.numericCellValue.toString()
                    println("      행 $rowIndex, 열 $col: 숫자 = $numValue")
                    numValue
                }
                else -> {
                    println("      행 $rowIndex, 열 $col: 알 수 없는 타입 = ${cell.cellType}")
                    null
                }
            }
            
            if (!value.isNullOrEmpty() && value != "null") {
                list.add(value)
                println("      -> 추가됨: \"$value\"")
            } else {
                println("      -> 무시됨")
            }
        }
        
        println("    최종 목록: $list")
        return list
    }
    
    fun readAssetData(inputStream: InputStream): Map<Investor, List<AssetChange>> {
        val excelData = readExcelData(inputStream)
        return excelData.assetChanges
    }
    
    fun getAssetDistribution(
        investor: Investor,
        criteria: Criteria,
        excelData: ExcelData
    ): List<AssetDistribution> {
        println("=== getAssetDistribution 디버깅 ===")
        println("투자자: ${investor.displayName}, 기준: ${criteria.displayName}")
        
        // 실제 Excel 시트에서 데이터를 읽어오기
        val (items, values) = readDistributionDataFromSheet(investor, criteria, excelData)
        
        println("읽어온 항목들: $items")
        println("읽어온 값들: $values")
        
        if (items.isEmpty() || values.isEmpty()) {
            println("실제 데이터가 없어 샘플 데이터를 사용합니다.")
            return generateSampleDistribution(investor, criteria)
        }
        
        // 실제 데이터로 AssetDistribution 생성
        val totalAmount = values.sum()
        val distributions = items.zip(values).map { (item, value) ->
            val percentage = if (totalAmount > 0) (value.toFloat() / totalAmount * 100) else 0f
            AssetDistribution(item, value, percentage)
        }
        
        println("생성된 분배 데이터: $distributions")
        return distributions
    }
    
    // Excel 시트에서 분배 값들을 읽어오는 메서드
    private fun readDistributionValues(sheet: org.apache.poi.ss.usermodel.Sheet, result: ExcelData, evaluator: FormulaEvaluator) {
        println("=== 분배 값 읽기 시작 ===")
        
        // 투자자별 실제 투자금액 컬럼 매핑 (H열, N열, T열, Z열)
        val investorCols = mapOf(
            Investor.DOWAN to 7,    // H열 (실제 투자금액)
            Investor.YOUNGHEE to 13, // N열 (실제 투자금액)
            Investor.JIAN to 19,    // T열 (실제 투자금액)
            Investor.JIWOO to 25    // Z열 (실제 투자금액)
        )
        
        investorCols.forEach { (investor, colIndex) ->
            println("투자자: ${investor.displayName}, 컬럼: ${colIndex}")
            
            // 계좌별 값 읽기 (H10:H13, N10:N13, T10:T13, Z10:Z13)
            val accountValues = readValuesFromRange(sheet, 9, colIndex, 12, colIndex, evaluator)
            result.accountValues[investor] = accountValues
            println("  계좌 값들: $accountValues")
            
            // 상품별 값 읽기 (H15:H19, N15:N19, T15:T19, Z15:Z19)
            val productValues = readValuesFromRange(sheet, 14, colIndex, 18, colIndex, evaluator)
            result.productValues[investor] = productValues
            println("  상품 값들: $productValues")
            
            // 섹터별 값 읽기 (H21:H31, N21:N31, T21:T31, Z21:Z31)
            val sectorValues = readValuesFromRange(sheet, 20, colIndex, 30, colIndex, evaluator)
            result.sectorValues[investor] = sectorValues
            println("  섹터 값들: $sectorValues")
        }
        
        // 추가 디버깅: 다른 위치에서도 시도해보기
        println("=== 추가 디버깅: 다른 위치에서 값 찾기 ===")
        debugAlternativeLocations(sheet)
    }
    
    // 다른 위치에서 값이 있는지 확인하는 디버깅 메서드
    private fun debugAlternativeLocations(sheet: org.apache.poi.ss.usermodel.Sheet) {
        // 도완의 H열에서 다양한 행 확인
        println("도완 H열 (7) 전체 확인:")
        for (row in 0..50) {
            val cell = sheet.getRow(row)?.getCell(7)
            if (cell != null && cell.cellType != org.apache.poi.ss.usermodel.CellType.BLANK) {
                val value = when (cell.cellType) {
                    org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                    org.apache.poi.ss.usermodel.CellType.STRING -> "\"${cell.stringCellValue}\""
                    else -> cell.toString()
                }
                println("  행 $row: $value")
            }
        }
        
        // 영희의 N열에서 다양한 행 확인
        println("영희 N열 (13) 전체 확인:")
        for (row in 0..50) {
            val cell = sheet.getRow(row)?.getCell(13)
            if (cell != null && cell.cellType != org.apache.poi.ss.usermodel.CellType.BLANK) {
                val value = when (cell.cellType) {
                    org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                    org.apache.poi.ss.usermodel.CellType.STRING -> "\"${cell.stringCellValue}\""
                    else -> cell.toString()
                }
                println("  행 $row: $value")
            }
        }
    }
    
    // 특정 범위에서 숫자 값들을 읽어오는 메서드
    private fun readValuesFromRange(sheet: org.apache.poi.ss.usermodel.Sheet, startRow: Int, col: Int, endRow: Int, endCol: Int, evaluator: FormulaEvaluator): List<Long> {
        val values = mutableListOf<Long>()
        println("    범위 읽기: 행 ${startRow}-${endRow}, 열 ${col}")
        
        for (rowIndex in startRow..endRow) {
            val row = sheet.getRow(rowIndex)
            if (row == null) {
                println("      행 $rowIndex: null")
                values.add(0L)
                continue
            }
            
            val cell = row.getCell(col)
            if (cell == null) {
                println("      행 $rowIndex, 열 $col: null")
                values.add(0L)
                continue
            }
            
            val value = getCellValueAsLong(cell, evaluator)
            println("      행 $rowIndex, 열 $col: 값 = $value")
            values.add(value)
        }
        return values
    }
    
    // Excel 시트에서 실제 분배 데이터를 읽어오는 메서드
    private fun readDistributionDataFromSheet(
        investor: Investor,
        criteria: Criteria,
        excelData: ExcelData
    ): Pair<List<String>, List<Long>> {
        return when (criteria) {
            Criteria.ACCOUNT -> {
                val items = excelData.accountList
                val values = excelData.accountValues[investor] ?: emptyList()
                println("계좌 데이터 - 항목: $items, 값: $values")
                Pair(items, values)
            }
            Criteria.PRODUCT -> {
                val items = excelData.productList
                val values = excelData.productValues[investor] ?: emptyList()
                println("상품 데이터 - 항목: $items, 값: $values")
                Pair(items, values)
            }
            Criteria.SECTOR -> {
                val items = excelData.sectorList
                val values = excelData.sectorValues[investor] ?: emptyList()
                println("섹터 데이터 - 항목: $items, 값: $values")
                Pair(items, values)
            }
        }
    }
    
    // Excel 시트 구조를 디버깅하는 메서드
    private fun debugSheetStructure(sheet: org.apache.poi.ss.usermodel.Sheet) {
        println("=== Excel 시트 구조 디버깅 ===")
        println("시트명: ${sheet.sheetName}")
        println("마지막 행 번호: ${sheet.lastRowNum}")
        
        // 주요 행들의 데이터 확인
        val debugRows = listOf(9, 10, 11, 12, 14, 15, 16, 17, 18, 20, 21, 22, 23, 24, 25)
        val debugCols = listOf(1, 2, 3, 9, 15, 21, 27) // B, C, D, J, P, V, AB
        
        for (rowIndex in debugRows) {
            val row = sheet.getRow(rowIndex)
            if (row != null) {
                println("행 $rowIndex:")
                for (colIndex in debugCols) {
                    val cell = row.getCell(colIndex)
                    val cellValue = when {
                        cell == null -> "null"
                        cell.cellType == org.apache.poi.ss.usermodel.CellType.STRING -> "\"${cell.stringCellValue}\""
                        cell.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                        cell.cellType == org.apache.poi.ss.usermodel.CellType.FORMULA -> "수식: ${cell.cellFormula}"
                        else -> "${cell.cellType}: ${cell.toString()}"
                    }
                    println("  열 $colIndex: $cellValue")
                }
            } else {
                println("행 $rowIndex: null")
            }
        }
    }
    
    // 샘플 데이터 생성 (실제 데이터가 없을 때 사용)
    private fun generateSampleDistribution(investor: Investor, criteria: Criteria): List<AssetDistribution> {
        val totalAmount = 100000000L // 샘플 총액
        
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
    
    // 사용자별 자산 변동 데이터를 시간순으로 정렬하여 반환
    fun getSortedAssetChanges(investor: Investor, excelData: ExcelData): List<AssetChange> {
        return excelData.assetChanges[investor]?.sortedBy { assetChange ->
            parseDate(assetChange.date)
        } ?: emptyList()
    }
}


