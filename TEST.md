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

인터페이스를 사용하고 이후에 종속 항목 삽입을 사용하여 이러한 인터페이스를 상속하는 클래스를 테스트하려면 **모조 클래스**를 사용하여 해당 클래스의 기능을 시뮬레이션할 수 있다. 테스트를 위해 모조 클래스와 데이터 소스를 삽입하면 반복성과 일관성을 유지하면서 코드를 개별적으로 테스트할 수 있다.

모조 클래스에서 사용할 모조 데이터는 다음과 같은 방법으로 만든다.
1. 테스트 디렉토리에 `fake` 패키지를 만든다.
2. `fake` 패키지에 `FakeDataSource` 객체를 만든다. 이 객체는 `MarsPhoto` 객체 목록으로 설정된 속성을 만든다. 최소 두 개의 객체를 포함하도록 한다.
```kotlin
object FakeDataSource {

   const val idOne = "img1"
   const val idTwo = "img2"
   const val imgOne = "url.1"
   const val imgTwo = "url.2"
   val photosList = listOf(
       MarsPhoto(
           id = idOne,
           imgSrc = imgOne
       ),
       MarsPhoto(
           id = idTwo,
           imgSrc = imgTwo
       )
   )
}
```

이 앱에서 저장소는 API 서비스에 종속된다. 저장소를 테스트하려면 방금 만든 모조 데이터를 반환하는 모조 API 서비스가 있어야 한다.
1. `fake` 패키지에 `FakeMarsApiService`라는 새 클래스를 만든다. 이 클래스는 `MarsApiService` 인터페이스를 상속받는다.
2. `getPhotos()` 함수가 `FakeDataSource.photoList`를 반환하도록 재정의한다.
```kotlin
class FakeMarsApiService : MarsApiService {
   override suspend fun getPhotos(): List<MarsPhoto> {
      return FakeDataSource.photosList
   }
}
```
<br>

<h3>저장소 테스트 작성</h3>

> `NetworkMarsPhotosRepository` 클래스의 `getMarsPhotos()` 메서드를 테스트하는 섹션이다. 모조 클래스의 사용법과 코루틴을 테스트하는 방법을 보여준다.

1. 모조 디렉토리에 테스트 클래스 `NetworkMarsRepositoryTest`를 만든다.
2. 
