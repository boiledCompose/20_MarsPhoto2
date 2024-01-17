## 레이어 분리

### 레이어를 분리하는 이유

코드를 여러 레이어로 분리하여 관리하는 이유는 다음과 같다:
- 앱의 확장성이 높아지며 앱이 더 견고해지고 테스트하기 쉬워진다.
- 경계가 명확히 정의된 여러 레이어를 사용하면 협업을 더 쉽게 할 수 있다.

>[!NOTE]
> Android의 권장 앱 아키텍처에는 앱에 최소한 UI 레이어와 데이터 레이어가 반드시 있어야 한다고 명시되어 있다.
<br>

## 데이터 레이어

데이터 레이어는 앱의 비즈니스 로직과 앱 데이터 소싱 및 저장을 담당한다. 데이터는 네트워크 요청, 로컬 데이터베이스, 기기의 파일 등 여러 소스에서 가져올 수 있다. 

데이터 레이어와 UI 레이어를 따로 관리함으로써 코드의 한 부분에서 변경해도 다른 부분에 영향을 주지 않는다. 이런 접근 방식은 **관심사 분리**라는 디자인 원칙의 일부이다. 
코드의 한 부분은 자체 관심사에 초점을 맞추고 내부 정보를 다른 코드로부터 숨긴다. 이것을 **캡슐화**라고 한다. 캡슐화된 코드 부분들끼리 상호작용을 해야 하는 경우 인터페이스로 처리한다.

데이터 레이어는 하나 이상의 저장소로 구성되고 저장소 자체에는 0개 이상의 데이터 소스가 포함된다. 권장사항에 따르면 데이터 소스 유형별로 저장소가 있어야 한다.   

저장소는 저장소 클래스를 통해 생성하며, 저장소 클래스는 다음을 실행한다:

- 앱의 다른 부분에서 데이터 노출
- 데이터 변경사항을 하나의 코드 부분에서 담당
- 여러 데이터 소스 간의 충돌 해결
- 앱의 나머지 부분에서 데이터 소스 추상화
- 비즈니스 로직 포함 (이 코드에선 비즈니스 로직을 다루지 않음)

### 데이터 레이어 만들기

1. `data` 패키지를 만들어 데이터 레이어를 관리한다.
2. `MarsPhotosRepository` 인터페이스를 생성한다. 내부에 `getMarsPhotos()`라는 추상 함수를 추가한다. 코루틴에서 호출되는 것이므로 `suspend`로 선언한다
   ```kotlin
   interface MarsPhotosRepository{
      suspend fun getMarsPhotos() : List<MarsPhoto>
   }
   ```
3. 인터페이스를 구현하는 클래스를 선언한다. 이 클래스에서 추상 함수를 구현한다. 추상 함수는 기존에 만들어놓은
   retrofitService 객체의 `getPhotos()` 메서드를 호출하는 기능을 해야 한다.
   ```kotlin
   class NetworkMarsPhotosRepository() : MarsPhotosRepository {
       override suspend fun getMarsPhotos() : List<MarsPhotos> {
         /*MarsApi is a singletone object that exposes lazy initialized Retrofit Service*/
         return MarsApi.retrofitService.getPhotos()
       }
   }
   ```
4. 다른 코드 부분에서 `MarsApi.retrofitService.getPhotos()`를 호출한 부분들을 모두 `NetworkMarsPhotosRepository()` 객체와 그 메서드를 선언하는 것으로 바꾼다.
   ```kotlin
   /*기존 코드*/
   val listResult = MarsApi.retrofitService.getPhotos()

   /*바뀐 코드*/
   val marsPhotosRepository = NetworkMarsPhotosRepository()
   val listResult = marsPhotosRepository.getMarsPhotos()
   ```
<br>

`ViewModel`에서 네트워크 요청을 직접 실행하던 구조에서 저장소를 통해 데이터를 제공받는 구조로 바뀌었다.
<img src = "https://developer.android.com/static/codelabs/basic-android-kotlin-compose-add-repository/img/f7f372187c75f57f_856.png?hl=ko" width=600px, height=150px/>

이 접근 방식은 데이터를 가져오는 코드와 `ViewModel`이 느슨하게 결합되도록 만든다. **느슨하게 결합**되면 저장소에서만 `getMarsPhotos()` 메서드를 관리하여 다른 항목에 부정적인 
영향을 미치지 않고 `ViewModel` 또는 저장소를 변경할 수 있다.

<br>

## 종속 항목 삽입 (Dependency Injection)

클래스에서 다른 클래스가 필요한 경우 필요한 클래스를 **종속 항목**이라고 한다. 종속 항목을 포함하는 방법은 크게 두 가지다:

1. 클래스가 필요한 객체 자체를 인스턴스화 하는 방법
2. 클래스가 필요한 객체를 인수로 전달받는 방법

클래스가 필요한 객체를 인스턴스화하여 사용하는 방법은 구현하기 쉽지만, 클래스와 종속 항목 간의 긴밀한 결합으로 인해 코드가 유연하지 않고 테스트하기 더 어려워진다. 객체의 생성자도 호출하고,
생성자가 변경되면 호출 코드도 변경해야 하기 때문이다. 

종속되는 객체를 클래스 외부에서 인스턴스화한 후 인수로 전달하는 방식으로 코드의 유연성과 접근성을 높일 수 있다. 클래스가 더 이상 하나의 특정 객체에 하드코딩되지 않고, 호출 코드를 수정할 필요 없이 필요한 객체의 구현을 변경할 수 있다. 이렇게 객체를 전달하는 것을 ***종속 항목 삽입***이라고 한다. 간단히 말해 종속 항목이 호출 클래스에 하드코딩되는 대신 런타임에 제공되는 경우인 것이다.

종속 항목 삽입의 장점은 다음과 같다:
- **코드 재사용성 지원:** 코드가 특정 객체에 종속되지 않아 유연성이 높다.
- **리팩터링 편의서 향상:** 코드가 느슨하게 연결되여 코드의 한 섹션을 수정해도 다른 섹션에 영향이 없다.
- **테스트 지원:** 테스트 중에 테스트 객체 전달이 가능하다.
<br>

## Application Container 만들기

컨테이너는 앱에 필요한 종속 항목이 포함된 객체다. 종속 항목들은 전체 애플리케이션에 걸쳐 사용되므로 모든 활동에서 사용할 수 있는 일반적인 위치에 배치해야 한다.

1. `data` 패키지 내에 `AppContainer` 인터페이스를 생성하고 다음과 같이 작성한다.
   ```kotlin
   interface AppContainer {
         val marsPhotosRepository: MarsPhotosRepository
   }
   ```
2. `AppContainer`를 구현하는 `DefaultAppContainer` 클래스를 생성한다. 여기엔 앱 전체에 걸쳐 사용되는 종속 항목들을 배치한다.
   ```kotlin
   class DefaultAppContainer : AppContainer {

      private val baseUrl = https://android-kotlin-fun-mars-server.appspot.com"

      private val retrofit: Retrofit = Retrofit.Builder()
           .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
           .baseUrl(BASE_URL)
           .build()

      private val retrofitService: MarsApiService by lazy {
         retrofit.create(MarsApiService::class.java)
      }
   }
   ```
3. 클래스 내부에 추상 메서드를 구현한다. `retrofitService`를 사용하는 저장소를 반환하도록 한다.
   ```
   override val marsPhotosRepository: MarsPhotosRepository by lazy {
         NetworkMarsPhotosRepository(retrofitService)
   }
   ```
4. `NetworkMarsPhotoRepository` 객체가 `MarsApiService` 객체를 인수로 받도록 수정한다. 이를 통해 기존 싱글톤 객체 `MarsApi`가 사용되지 않게 된다.
   ```
   class NetworkMarsPhotosRepository(
       private val marsApiService: MarsApiService
   ) : MarsPhotosRepository {
         override suspend fun getMarsPhotos(): List<MarsPhoto> = marsApiService.getPhotos()
   }
   ```
<br>

## 앱과 Application Container 연결

<img src="https://developer.android.com/static/codelabs/basic-android-kotlin-compose-add-repository/img/8a410896a22ad569_856.png?hl=ko" width=600, heigth=400/>

1. 패키지의 최상단에 `Application`을 상속받는 클래스를 생성한다.
2. 클래스 내부에 `AppContainer` 변수를 선언하고 `onCreate()`에서 `DefaultAppContainer`로 초기화한다.
   ```kotlin
   class MarsPhotosApplication : Application() {
      lateinit var container: AppContainer
      override fun onCreate() {
         super.onCreate()
         container = DefaultAppContainer()
      }
   }
   ```
3. `manifests/AndroidManifest.xml` 파일의 `<application>`태그에 다음 코드를 추가한다.
   ```xml
   <application
      android:name=".MarsPhotosApplication"
      ...
   </application>
   ```

## ViewModel에 저장소 추가

1. `MarsViewModel`클래스 매개변수로 `MarsPhotosRepository`를 추가한다. 이제 생성자 매개변수의 값을 애플리케이션 컨테이너에서 가져올 수 있다.
   ```kotlin
   class MarsViewModel(private val marsPhotosRepository: MarsPhotosRepository) : ViewModel()
   ```
2.  ViewModel 코드 내부에서 저장소 객체를 선언할 필요가 없으므로 코드 줄을 삭제한다.
   ```
   //remove
   val marsPhotosRepository = NetworkMarsPhotosRepository()
   ```
3. 안드로이드에서 ViewModel을 사용할 때 생성자를 통한 데이터 전달이 기본적으로 제한되어 있으나, `ViewModelProvider.Factory`를 구현함으로써 이러한 제한을 우회할 수 있다. 이 객체는 애플리케이션 컨테이너를 사용하여 `marsPhotosRepository를 검색하고 `ViewModel` 객체가 생성되면 저장소를 `ViewModel`에 전달한다.
   ```
   companion object {
      val Factory: ViewModelProvider.Factory = viewModelFactory {
         initializer {
            val application = (this[APPLICATION_KEY] as MarsPhotosApplication)
            val marsPhotosRepository = application.container.marsPhotosRepository
            MarsViewModel(marsPhotosRepository = marsPhotosRepository)
         }
      }
   }
   ```
   - `APPLICATION_KEY`는 앱의 애플리케이션 객체를 찾는 데 사용된다.
   - 어플리케이션 객체의 `container` 속성을 통해 종속 항목 삽입에 사용된 저장소를 검색할 수 있다.
4. `ViewModel` 객체가 선언될 때 팩토리를 인수로 넘겨줍니다.
   ```
   /* theme/MarsPhotosApp.kt */
   Surface(
            // ...
        ) {
            val marsViewModel: MarsViewModel =
   viewModel(factory = MarsViewModel.Factory)
            // ...
        }
   ```

>[!NOTE]
> 여기까지가 저장소 및 종속 항목 삽입을 사용하도록 앱을 리팩터링한 코드다. 저장소 하나가 포함된 데이터 레이어를 구현하여 안드로이드 권장사항에 따라 UI와 데이터 소스 코드를 분리하였다.
