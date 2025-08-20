# Expense Tracker Lite

A native Android expense tracking app built with Jetpack Compose, featuring currency conversion, pagination, and offline support.

## 🎯 Features

- **Dashboard Screen**: Welcome message, profile image, total balance, income/expenses display
- **Add Expense Screen**: Category selection, amount input, currency conversion, date picker
- **Pagination**: 10 items per page with infinite scroll/load more functionality
- **Currency Conversion**: Real-time conversion using open.er-api.com
- **Offline Support**: Local storage with Room database
- **Filtering**: This Month, Last 7 Days, All time filters
- **Modern UI**: Material Design 3 with Jetpack Compose

## 🏗️ Architecture

### Clean Architecture with MVVM

```
ui/ → Composables & Screens
├── screens/
│   ├── DashboardScreen.kt
│   └── AddExpenseScreen.kt
├── navigation/
│   └── NavGraph.kt
└── theme/

viewmodel/ → State, Events, ViewModel
├── DashboardViewModel.kt
└── AddExpenseViewModel.kt

domain/ → UseCases, Entities
├── model/
│   └── Expense.kt
├── repository/
│   └── ExpenseRepository.kt
├── usecase/
│   └── ConvertToUsdUseCase.kt
└── mappers/
    └── ExpenseMappers.kt

data/ → Repository, DataSource, DTO, Mappers
├── local/
│   ├── ExpenseEntity.kt
│   ├── ExpenseDao.kt
│   └── AppDatabase.kt
├── remote/
│   └── ExchangeRateApi.kt
├── repository/
│   └── ExpenseRepositoryImpl.kt
└── di/
    └── AppModule.kt
```

### Key Technologies

- **UI**: Jetpack Compose, Material Design 3
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Database**: Room with Kotlin Coroutines
- **Networking**: Retrofit, OkHttp, Moshi
- **State Management**: StateFlow
- **Navigation**: Navigation Compose
- **Testing**: JUnit, Coroutines Test, Turbine

## 🧪 Unit Tests

### Test Structure

The project includes comprehensive unit tests following the **Arrange-Act-Assert** pattern:

```
app/src/test/java/com/bn/bassemexpensetrackerlite/
├── AddExpenseViewModelTest.kt
├── ConvertToUsdUseCaseTest.kt
├── DashboardViewModelTest.kt
└── SimpleTest.kt
```

### Test Coverage

#### 1. AddExpenseViewModelTest
- **`invalid amount shows error`**: Tests validation logic for negative/zero amounts
- **`save converts EUR to USD and persists`**: Tests currency conversion and persistence

#### 2. ConvertToUsdUseCaseTest
- **`converts using rates map`**: Tests EUR to USD conversion (10 EUR → 20 USD)
- **`returns original amount when currency is USD`**: Tests USD currency handling

#### 3. DashboardViewModelTest
- **`loads first page and totals`**: Tests initial data loading and totals calculation
- **`can load more when data available`**: Tests pagination functionality

### Running Tests

#### Option 1: Android Studio
1. Right-click on `app/src/test/java/com/bn/bassemexpensetrackerlite/`
2. Select "Run Tests in 'com.bn.bassemexpensetrackerlite'"

#### Option 2: Command Line
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.bn.bassemexpensetrackerlite.AddExpenseViewModelTest"

# Run specific test method
./gradlew testDebugUnitTest --tests "com.bn.bassemexpensetrackerlite.AddExpenseViewModelTest.invalid amount shows error"
```

#### Option 3: Using the provided script
```bash
chmod +x run_tests.sh
./run_tests.sh
```

### Test Dependencies

```kotlin
// Testing
testImplementation(libs.junit)
testImplementation(libs.mockito.core)
testImplementation(libs.mockwebserver)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 35
- Kotlin 2.0.21

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd BassemExpenseTrackerLite
```

2. Open in Android Studio and sync project

3. Run the app on an emulator or device

### Build Configuration

- **compileSdk**: 35
- **targetSdk**: 35
- **minSdk**: 24
- **Kotlin**: 2.0.21

## 📱 Screenshots

### Dashboard Screen
- Welcome message with profile image
- Summary cards showing total balance, income, and expenses
- Filter chips (This Month, Last 7 Days, All)
- Paginated expense list with load more functionality
- Floating Action Button to add new expense

### Add Expense Screen
- Category selection with icons
- Amount input with currency symbol
- Currency dropdown with autocomplete (fetched from API)
- Income/Expense type selection
- Date picker
- Save button with validation

## 🔧 API Integration

### Currency Conversion
- **API**: https://open.er-api.com/v6/latest/USD
- **Free tier**: No API key required
- **Rate limits**: 1000 requests per month
- **Fallback**: Mock data for offline scenarios

### Supported Currencies
- USD, EUR, GBP, AED, SAR, EGP, and more (fetched dynamically)

## 📊 Pagination Strategy

- **Page Size**: 10 items per page
- **Implementation**: Local pagination using Room database
- **Loading States**: Loading indicators and error handling
- **Filter Integration**: Pagination works with all filters (This Month, Last 7 Days, All)

## 🗄️ Data Storage

### Room Database
- **Entity**: ExpenseEntity with all expense fields
- **DAO**: ExpenseDao with pagination and aggregation methods
- **Database**: AppDatabase with Room configuration

### DataStore (Planned)
- User preferences
- App settings
- Currency preferences

## 🎨 Design Implementation

The app closely follows the provided Dribbble design:
- **Typography**: Material Design 3 typography scale
- **Colors**: Material Design 3 color system
- **Shapes**: Rounded corners and elevation
- **Spacing**: Consistent 8dp grid system
- **Icons**: Material Design icons

## 🐛 Known Issues

1. **Gradle Test Configuration**: Some Gradle test tasks may require Android Studio for optimal execution
2. **NDK Warnings**: NDK source.properties warnings (non-critical)
3. **Deprecated APIs**: Some Compose APIs show deprecation warnings (will be updated in future versions)

## 🔮 Future Enhancements

- [ ] Export expenses to CSV/PDF
- [ ] Receipt image upload and storage
- [ ] Budget tracking and alerts
- [ ] Category management
- [ ] Data backup and sync
- [ ] Dark theme support
- [ ] Widgets for quick expense entry

## 📄 License

This project is created for technical interview purposes.

## 👨‍💻 Author

Built with ❤️ using modern Android development practices.

