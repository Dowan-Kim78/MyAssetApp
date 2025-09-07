package com.example.myapplication.data

data class InvestorData(
    val name: String,
    val investmentAmount: Long,
    val evaluationAmount: Long,
    val date: String
)

data class AssetDistribution(
    val category: String,
    val amount: Long,
    val percentage: Float
)

data class AssetChange(
    val date: String,
    val investmentAmount: Long,
    val evaluationAmount: Long
)

enum class Investor(val displayName: String) {
    DOWAN("도완"),
    YOUNGHEE("영희"),
    JIAN("지안"),
    JIWOO("지우")
}

enum class Criteria(val displayName: String) {
    ACCOUNT("계좌"),
    PRODUCT("상품"),
    SECTOR("섹터")
}


