## 로컬 테스트 설정 방법

> 이 문서에선 앞선 안드로이드 권장사항인 UI 레이어와 데이터 레이어 분리가 이뤄지면 더 쉽게 테스트할 수 있는 이유를 다룬다.
> 또한 코루틴을 사용하는 코드를 테스트하기 위해 비동기 실행을 처리하는 추가적인 단계를 다룬다.
<br>

### 로컬 테스트 종속 항목 추가

1. 앱 레벨의 `build.gradle.kts`에 다음 항목을 추가한다.
   ```
   testImplementation("junit:junit:4.13.2")
   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
   ```
2. 로컬 테스트 디렉토리는 `src/test/java`로 하고, 디렉토리에 새 패키지 `com.example.marsphotos`를 추가한다.
<br>

### 테스트용 fake 데이터 및 종속 항목 만들기

각 로컬 테스트는 한 가지만 테스트해야 한다. 에를 들어 `ViewModel`의 기능을 테스트할 때 저장소 또는 API 서비스의 기능은 테스트하고 싶지 않을 수 있다. 

