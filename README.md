# 자산 관리 앱 (Asset Management App)

Android 기반의 자산 관리 및 시각화 애플리케이션입니다. Excel 파일을 통해 자산 데이터를 로드하고, 다양한 기준으로 자산 분배 현황과 변동 추이를 차트로 시각화합니다.

## 📱 주요 기능

### 1. 사용자별 자산 관리
- **4명의 투자자**: 도완, 영희, 지안, 지우
- **개별 자산 현황**: 각 투자자별 자산 분배 및 변동 추이 확인

### 2. 다중 기준 분석
- **계좌별 분석**: 계좌 단위로 자산 분배 현황
- **상품별 분석**: 투자 상품별 자산 분배 현황  
- **섹터별 분석**: 투자 섹터별 자산 분배 현황

### 3. 시각화 차트
- **파이 차트**: 자산 분배 현황을 원형 차트로 표시
  - 0.1% 이하 소액 항목은 "기타"로 통합
  - 내부/외부 라벨링 시스템으로 가독성 향상
  - 16가지 색상 팔레트로 구분
- **라인 차트**: 자산 변동 추이를 선형 차트로 표시
  - 투자금액(점선)과 평가금액(실선) 구분
  - 각 평가금액 점에 상세 정보 표시 (총금액, 수익금액, 수익률)

### 4. 파일 업로드 기능
- **Excel 파일 업로드**: `금전출납.xlsx` 파일만 업로드 가능
- **자동 데이터 갱신**: 파일 업로드 후 즉시 차트 데이터 갱신
- **파일명 검증**: 정확한 파일명만 허용하여 데이터 무결성 보장

### 5. 전체화면 모드
- **차트 확대**: 차트 클릭 시 전체화면으로 확대
- **핀치 줌**: 전체화면에서 확대/축소 기능
- **세로 모드 유지**: 전체화면에서도 세로 방향 유지

## 🛠️ 기술 스택

- **언어**: Kotlin
- **플랫폼**: Android (API 21+)
- **UI**: Custom Views, XML Layouts
- **데이터 처리**: Apache POI (Excel 파일 읽기)
- **차트**: Custom Canvas Drawing
- **파일 관리**: Android File System

## 📁 프로젝트 구조

```
app/
├── src/main/
│   ├── java/com/example/myapplication/
│   │   ├── MainActivity.kt                 # 메인 액티비티
│   │   ├── FullscreenChartActivity.kt     # 전체화면 차트 액티비티
│   │   ├── data/                          # 데이터 모델
│   │   │   ├── AssetChange.kt
│   │   │   ├── AssetDistribution.kt
│   │   │   ├── Criteria.kt
│   │   │   └── Investor.kt
│   │   ├── utils/                         # 유틸리티
│   │   │   ├── ExcelReader.kt
│   │   │   └── ExcelData.kt
│   │   └── views/                         # 커스텀 뷰
│   │       ├── PieChartView.kt
│   │       └── LineChartView.kt
│   ├── res/
│   │   ├── layout/                        # 레이아웃 파일
│   │   ├── drawable/                      # 아이콘 및 배경
│   │   └── values/                        # 색상 및 문자열
│   └── assets/
│       └── 금전출납.xlsx                  # 기본 Excel 파일
```

## 🚀 설치 및 실행

### 1. 프로젝트 클론
```bash
git clone [repository-url]
cd Asset_app
```

### 2. Android Studio에서 열기
- Android Studio에서 프로젝트 폴더 열기
- Gradle 동기화 완료 대기

### 3. 앱 빌드 및 실행
```bash
# Debug 버전 빌드
./gradlew assembleDebug

# 디바이스에 설치
./gradlew installDebug
```

## 📊 데이터 형식

### Excel 파일 구조
- **파일명**: `금전출납.xlsx` (정확한 파일명 필요)
- **시트**: 날짜별 시트 (예: `25.08.01`, `25.09.01`)
- **데이터 범위**: 
  - A열: 계좌 목록
  - B열: 상품 목록  
  - C열: 섹터 목록
  - H, N, T, Z열: 분배 비율 값
  - J, P, V, AB열: 자산 변동 데이터

### 데이터 모델
```kotlin
// 자산 분배
data class AssetDistribution(
    val name: String,      // 항목명
    val amount: Long,      // 금액
    val percentage: Float  // 비율
)

// 자산 변동
data class AssetChange(
    val date: String,           // 날짜
    val investmentAmount: Long, // 투자금액
    val evaluationAmount: Long  // 평가금액
)
```

## 🎨 UI/UX 특징

### 색상 팔레트
- **도완**: 보라색 계열
- **영희**: 파란색 계열
- **지안**: 초록색 계열
- **지우**: 주황색 계열
- **기준 버튼**: 각각 다른 색상으로 구분

### 차트 기능
- **파이 차트**: 
  - 30도 이상: 내부 라벨
  - 30도 미만: 외부 라벨 + 연결선
  - 소액 항목 자동 통합
- **라인 차트**:
  - 투자금액: 점선
  - 평가금액: 실선 + 상세 정보 표시
  - 만원 단위 표시 (#,###만원)

## 📱 사용 방법

1. **앱 실행**: 자산 관리 앱 시작
2. **사용자 선택**: 4명 중 한 명 선택
3. **기준 선택**: 계좌/상품/섹터 중 하나 선택
4. **차트 확인**: 파이 차트와 라인 차트로 데이터 시각화
5. **파일 업로드**: 우측 상단 아이콘으로 새 Excel 파일 업로드
6. **전체화면**: 차트 클릭으로 확대 보기

## 🔧 주요 클래스 설명

### MainActivity
- 메인 UI 관리
- 사용자/기준 선택 처리
- 파일 업로드 기능
- 차트 데이터 로드 및 갱신

### ExcelReader
- Excel 파일 읽기 및 파싱
- 날짜별 시트 자동 감지
- 최신 데이터 우선 로드
- 수식 셀 평가 처리

### PieChartView
- 원형 차트 그리기
- 스마트 라벨링 시스템
- 소액 항목 통합
- 핀치 줌 지원

### LineChartView
- 선형 차트 그리기
- 투자/평가금액 구분 표시
- 상세 정보 툴팁
- 핀치 줌 지원

## 🐛 알려진 이슈

- `startActivityForResult` deprecated 경고 (Android 13+)
- `systemUiVisibility` deprecated 경고 (Android 11+)

## 📝 라이선스

이 프로젝트는 개인 사용을 위한 애플리케이션입니다.

## 👥 개발자

자산 관리 앱 개발팀

---

**버전**: 1.0.0  
**최종 업데이트**: 2024년 12월
