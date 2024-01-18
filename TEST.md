## 로컬 테스트 설정 방법

> 이 문서에선 앞선 안드로이드 권장사항인 UI 레이어와 데이터 레이어 분리가 이뤄지면 더 쉽게 테스트할 수 있는 이유를 다룬다.
> 또한 코루틴을 사용하는 코드를 테스트하기 위해 비동기 실행을 처리하는 추가적인 단계를 다룬다.
<br>

### 로컬 테스트 종속 항목 추가

1. 앱 레벨의 `build.gradle.kts`에 다음 항목을 추가한다.
   ```gradle
   testImplementation("junit:junit:4.13.2")
   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
   ```
2. 로컬 테스트 디렉토리는 `src/test/java`로 하고, 디렉토리에 새 패키지 `com.example.marsphotos`를 추가한다.
<br>

## 테스트용 fake 데이터 및 종속 항목 만들기

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

## 저장소 테스트 작성

> `NetworkMarsPhotosRepository` 클래스의 `getMarsPhotos()` 메서드를 테스트하는 섹션이다. 모조 클래스의 사용법과 코루틴을 테스트하는 방법을 보여준다.

1. 모조 디렉토리에 테스트 클래스 `NetworkMarsRepositoryTest`를 만든다.
2. `networkMarsPhotosRepository_getMarsPhotos_verifyPhotoList()`라는 테스트 메서드를 만든다.
   저장소를 테스트하기 위해 `NetworkMarsPhotosRepository` 인스턴스를 만든다.
```kotlin
@Test
fun networkMarsPhotosRepository_getMarsPhotos_verifyPhotoList(){
    val repository = NetworkMarsPhotosRepository(
       marsApiService = FakeMarsApiService()
    )
}
```

>[!NOTE]
> 모조 API 서비스 객체를 전달하는 방식인 가짜 구현은 더 일관된 테스트 환경을 만들고 테스트 결함을 줄이는 데 도움이 되며 단일 기능을 테스트하는 간결한 테스트를 용이하게 한다.
> 또한 종속 항목용 모조 클래스를 사용하게 되면 테스트 중인 코드가 테스트되지 않는 코드에 종속되지 않고 예상치 못한 문제가 발생할 수 있는 API에 종속되지 않는다.

3. 저장소의 메서드 반환값과 모조 데이터 클래스의 객체 리스트가 같은지를 확인하는 어썰션을 추가한다.
   ```kotlin
   fun networkMarsPhotosRepository_getMarsPhotos_verifyPhotoList(){
      val repository = NetworkMarsPhotosRepository(
         marsApiService = FakeMarsApiService()
      )
      assertEquals(FakeDataSource.photosList, repository.getMarsPhotos)
   }
   ``` 

>[!WARNING]
> 저장소의 `getMarsPhotos()`메서드는 정지 함수다. 정지 함수를 호출하는 것은 코루틴에서만 가능하여 테스트 클래스에서 코루틴이 포함된 섹션을 테스트하는 방법이 따로 존재한다.
<br>

<h3>테스트 코루틴</h3>

1. `networkMarsPhotosRepository_getMarsPhotos_verifyPhotoList()` 함수를 표현식이 되도록 수정한다.
2. 표현식의 우측에 `runTest()` 함수를 배치한다. 이 메서드에는 람다가 필요하다.
   `runTest()`는 람다에 전달한 메서드를 가져와 `CoroutineScope`를 상속받는 `TestScope`에서 실행한다.
3. 기존 함수 내용은 람다 함수로 이동한다.
```kotlin
fun networkMarsPhotosRepository_getMarsPhotos_verifyPhotoList() =
   runTest {
      val repository = NetworkMarsPhotosRepository(
         marsApiService = FakeMarsApiService()
      )
      assertEquals(FakeDataSource.photosList, repository.getMarsPhotos)
   }
```
<br>

## ViewModel 테스트 작성

> 여기선 `MarsViewModel`에서 `getMarsPhotos()` 함수의 테스트를 작성한다.

1. `MarsViewModel`은 `MarsPhotosRepository`에 종속되므로 모조 저장소 클래스인 `FakeNetworkMarsPhotosRepository`를 먼저 만들고 `getMarsPhotos()`를 재정의한다. 이 메서드는 모조 데이터들을 반환해야 한다.
   ```kotlin
   class FakeNetworkMarsPhotosRepository : MarsPhotosRepository{
      override suspend fun getMarsPhotos(): List<MarsPhoto> {
         return FakeDataSource.photosList
      }
   }
   ```
2. `MarsViewModelTest`라는 테스트 클래스를 만든다. 그 안에 테스트 함수 `marsViewModel_getMarsPhotos_verifyMarsUiStateSuccess()`를 만든다. `runTest()` 메서드의 결과로 설정된 표현식으로 만든다.
3. `runTest()`의 람다 본문에서 뷰 모델 인스턴스를 만든다.
4. 뷰 모델 인스턴스의 `marsUiState`와 `MarsPhotosRepository.getMarsPhotos()`의 호출 결과를 비교하는 어썰션을 추가한다.
```kotlin
@Test
fun marsViewModel_getMarsPhotos_verifyMarsUiStateSuccess() =
   runTest {
       val marsViewModel = MarsViewModel(
           marsPhotosRepository = FakeNetworkMarsPhotosRepository()
       )
       assertEquals(
           MarsUiState.Success("Success: ${FakeDataSource.photosList.size} Mars " +
                   "photos retrieved"),
           marsViewModel.marsUiState
       )
   }
```
>[!WARNING]
> `MarsViewModel`이 저장소를 호출할 때 `viewModelScope.launch`를 통해 Main 디스패체 환경 밑에서 새로운 코루틴을 실행한다. 로컬 단위 테스트 아래의 코드가 Main 디스패처를 참조하는 경우 단위 테스트 실행 시 예외가 발생한다. 이 문제를 해결하려면 단위 테스트를 실행할 때 기본 디스패처를 명시적으로 정의해야 한다.
<br>

### 테스트 디스패처 만들기

Main 디스패처는 UI에서만 사용할 수 있으므로 단위 테스트 친화적인 디스패처롤 바꿔야 한다. 이를 수행해주는 것이 `TestDispatcher`다.

Main 디스패처를 `TestDispathcer`로 바꾸려면 `Dispartchers.setMain()` 함수를 사용하고, 반대로 돌아오려면 `Dispatchers.resetMain()`을 사용한다. 

각 테스트에서 Main 디스패처를 대체하는 코드가 중복되지 않도록 JUnit 테스트 규칙을 추출할 수 있다. 이렇게 추출한 규칙이 TestRule이다. TestRule은 테스트가 실행되는 환경을 제어하는 방법을 제공하며, 검사 추가, 테스트에 필요한 설정 혹은 정리 실행, 테스트 실행 관찰 및 보고 등을 수행한다.

1. 테스트 디렉토리에 `rules`라는 패키지를 생성한다.
2. `rules`밑에 `TestDispatcherRule`이라는 클래스를 만든다. 이 클래스는 `TestWatcher`를 상속받는다.
3. `TestDispatcher` 매개변수를 넣는다. 이 인수의 값 중 `UnconfinedTestDispatcher`는 태스크가 특정 순서로 실행되지 않도록 지정하고, `StandardTestDispatcher`는 코루틴을 완전히 제어한다.
   ```kotlin
   class TestDispatcherRule(
      val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
   ) : TestWatcher() {
      
   }
   ```
4. 테스트가 실행되기 전에 Main 디스패처를 테스트 디스패처로 바꿔야 한다. `TestWatcher` 클래스의 `starting()` 함수는 지정된 테스트가 실행되기 전에 실행된다. `starting()` 함수를 재정의하고 테스트 디스패처로 바꾼다.
   ```kotlin
   class TestDispatcherRule(
      val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
   ) : TestWatcher() {
      override fun starting(description: Description) {
         Dispatchers.setMain(testDispatcher)
      }
   }
   ```
5. 테스트 실행이 완료되면 테스트 디스패처를 Main 디스패처로 되바꾼다. `finished()` 함수는 지정된 테스트가 끝나면 실행된다.
   ```kotlin
   class TestDispatcherRule(
      val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
   ) : TestWatcher() {
      override fun starting(description: Description) {
         Dispatchers.setMain(testDispatcher)
      }

      override fun finished(description: Description) {
         Dispatchers.resetMain()
      }
   }
   ```
6. `MarsViewModelTest.kt`로 돌아가서 테스트 클래서 내부에 `TestDispatcherRule` 인스턴스를 생성하고 읽기 전용 변수에 할당한다.
7. `@get:Rule` 주석을 달아서 테스트에 적용한다.
   ```kotlin
   class MarsViewModelTest {
      @get:Rule
      val testDispatcher = TestDispatcherRule()
      ...
   }   
   ```
   

  
