# Dagger/Hilt to Koin Migration Guide

This document is the working plan for replacing Dagger/Hilt with Koin as the
dependency injection framework for this Android app. The goal is to complete the
DI migration before the wider Compose Multiplatform/Kotlin Multiplatform
migration, so the app has a runtime DI model that can move into shared source
sets later.

## Goals

- Replace Dagger/Hilt with Koin across the app.
- Keep the app buildable after each migration phase.
- Prefer explicit Koin DSL modules over Koin annotations/code generation.
- Prepare feature dependencies for future KMP module/source-set separation.
- Remove Hilt Gradle plugins, Hilt dependencies, generated Hilt code, and all
  `dagger.*`, `dagger.hilt.*`, and `javax.inject.*` usage.

## Non-goals

- Do not migrate the project to KMP/CMP in this phase.
- Do not rewrite feature architecture unless required by the DI migration.
- Do not replace Android-only libraries such as Room, WorkManager, Media3, or
  platform `Context` usage yet.
- Do not introduce Koin annotations unless this guide is explicitly revised.

## Current State

The project is currently a single Android application module:

- Root project: `SecMessage`
- App module: `:app`
- Main app package: `com.dino.message`
- Existing DI module: `app/src/main/java/com/dino/message/di/AppModule.kt`
- Application entrypoint: `app/src/main/java/com/dino/message/DinoOrderApplication.kt`
- Main activity entrypoint:
  `app/src/main/java/com/dino/message/corefeature/presentation/activity/MainActivity.kt`

Current Hilt footprint from repository inspection:

- `@HiltAndroidApp`: 1 usage.
- `@AndroidEntryPoint`: 1 usage.
- `@HiltViewModel`: 34 usages.
- `hiltViewModel()`: 67 usages.
- `@Inject constructor`: 128 usages.
- Field `@Inject`: application/activity/service-level lateinit injections.
- `@Module`, `@InstallIn`, and `@Provides`: centralized in `AppModule`.
- `@ApplicationContext`: used heavily in Android-specific dependencies and
  ViewModels.
- Hilt dependencies/plugins are declared in Gradle version catalog and app
  build files.

The migration should start with the central graph in `AppModule`, then move
feature by feature through constructor annotations, ViewModels, Composables, and
Android components.

## Target DI Architecture

### Koin Style

Use Koin's manual DSL:

- `module { ... }` for dependency groups.
- `single { ... }` or `singleOf(::Type)` for application-wide singletons.
- `factory { ... }` or `factoryOf(::Type)` for short-lived use cases/helpers.
- `viewModel { ... }` or `viewModelOf(::Type)` for Android ViewModels.
- `bind<Interface>()` or explicit typed definitions for interface bindings.
- `androidContext()` for Android `Context`.
- `androidApplication()` for Android `Application`.
- `get()` for graph dependencies inside module definitions.
- `parametersOf(...)` only when a dependency genuinely needs runtime arguments.

Do not use Koin annotations in the first migration. Manual modules are more
explicit, remove DI code generation pressure, and are easier to split into
`commonMain` and platform-specific source sets during the later KMP migration.

### Module Layout

Create small Koin modules grouped by responsibility instead of keeping one large
monolithic module:

- `coreModule`: app-wide utilities, Gson, session, network client, API utility,
  socket manager, notification manager, crash reporter.
- `databaseModule`: Room database and DAOs.
- Feature modules such as `authModule`, `chatModule`, `threadModule`,
  `profileModule`, `groupModule`, `mainModule`, `searchModule`, `reportModule`,
  `activityNotificationModule`, `splashModule`, and `updateModule`.
- `viewModelModule` can be separate if ViewModels are easier to migrate in one
  pass, but long-term prefer each feature owning its ViewModel definitions.
- `appModules`: a single list that aggregates all modules passed to Koin.

Recommended package:

```kotlin
package com.dino.message.di
```

Example aggregate:

```kotlin
val appModules = listOf(
    coreModule,
    databaseModule,
    authModule,
    chatModule,
    threadModule,
    profileModule,
    groupModule,
    mainModule,
    searchModule,
    reportModule,
    activityNotificationModule,
    splashModule,
    updateModule,
)
```

### Startup

Replace `@HiltAndroidApp` with normal Koin startup in
`DinoOrderApplication.onCreate()`:

```kotlin
startKoin {
    androidLogger()
    androidContext(this@DinoOrderApplication)
    modules(appModules)
}
```

After Koin starts, dependencies previously field-injected into
`DinoOrderApplication` should be resolved through Koin:

```kotlin
private val crashReporter: CrashReporter by inject()
private val inAppNotificationManager: InAppNotificationManager by inject()
```

If any startup dependency must be unavailable in the crash UI process, keep the
existing crash-process guard before resolving that dependency.

### Compose and ViewModels

Replace:

```kotlin
import androidx.hilt.navigation.compose.hiltViewModel

val viewModel: SomeViewModel = hiltViewModel()
```

with:

```kotlin
import org.koin.androidx.compose.koinViewModel

val viewModel: SomeViewModel = koinViewModel()
```

For navigation argument/runtime parameters, prefer Koin parameters:

```kotlin
val viewModel: SomeViewModel = koinViewModel {
    parametersOf(argument)
}
```

Register ViewModels with:

```kotlin
viewModelOf(::SomeViewModel)
```

or, when parameters are needed:

```kotlin
viewModel { parameters ->
    SomeViewModel(
        id = parameters.get(),
        repository = get(),
    )
}
```

Remove `@HiltViewModel`, `@Inject constructor`, and Hilt imports from each
ViewModel after the corresponding Koin definition exists.

### Android Context

Replace `@ApplicationContext context: Context` constructor dependencies with
plain `context: Context` in Android-only classes, then provide `androidContext()`
from Koin definitions.

Example:

```kotlin
single { SPrefManager(gson = get(), context = androidContext()) }
single { HttpAPIUtil(sPrefManager = get(), context = androidContext()) }
```

For KMP readiness, do not spread Android `Context` deeper into domain classes.
If a domain/use-case class currently depends on `Context`, migrate it later to a
platform-neutral abstraction such as a resource provider, file provider, or
session abstraction.

### Lifetimes

Use the following default mapping:

| Hilt concept | Koin equivalent |
| --- | --- |
| `@Singleton @Provides` | `single { ... }` |
| `@Provides` without scope | `factory { ... }` unless object should be shared |
| `@Inject constructor` service/repository | `singleOf(::Type)` or explicit `single { ... }` |
| `@Inject constructor` use case | `factoryOf(::Type)` by default |
| `@HiltViewModel` | `viewModelOf(::Type)` |
| `@ApplicationContext` | `androidContext()` |
| `Application` | `androidApplication()` |
| `@AndroidEntryPoint` field injection | `by inject()` or constructor/manual lookup |

Be conservative with singleton scope. Keep stateful managers, database, DAOs,
repositories, network client, session provider, crash reporter, socket manager,
and notification manager as `single`. Use cases should usually be `factory`
unless they intentionally hold state.

## Gradle Migration

### Add Koin

Add Koin versions and libraries to `gradle/libs.versions.toml`.

Use current Koin artifacts from official Koin docs when implementation begins.
At the time this guide was created, the Koin 4.x docs use:

- `io.insert-koin:koin-bom`
- `io.insert-koin:koin-core`
- `io.insert-koin:koin-android`
- `io.insert-koin:koin-androidx-compose`
- `io.insert-koin:koin-compose`
- `io.insert-koin:koin-compose-viewmodel`
- `io.insert-koin:koin-compose-viewmodel-navigation`
- `io.insert-koin:koin-test` for tests

For this Android-only phase, add at minimum:

```kotlin
implementation(platform(libs.koin.bom))
implementation(libs.koin.android)
implementation(libs.koin.androidx.compose)
implementation(libs.koin.compose)
implementation(libs.koin.compose.viewmodel)
implementation(libs.koin.compose.viewmodel.navigation)
testImplementation(libs.koin.test)
```

If a package or artifact changes in newer Koin docs, update the version catalog
before coding and keep this guide aligned.

### Keep Hilt Temporarily

During the parallel migration window, keep Hilt dependencies and plugins until
all Hilt call sites are removed. This reduces risk and allows feature-by-feature
conversion.

Remove Hilt only after the final cleanup phase:

- Root `build.gradle.kts`: remove `alias(libs.plugins.dagger.hilt)`.
- App `build.gradle.kts`: remove `alias(libs.plugins.dagger.hilt)`.
- App dependencies: remove `implementation(libs.hilt.android)`,
  `ksp(libs.hilt.compiler)`, and
  `implementation(libs.androidx.hilt.navigation.compose)`.
- Version catalog: remove Hilt plugin/library/version entries.

Do not remove KSP entirely if Room or Compose Destinations still uses it.

## Migration Phases

### Phase 1: Add Koin Without Replacing Hilt

1. Add Koin dependencies to the version catalog and app module.
2. Create Koin module files under `com.dino.message.di`.
3. Start with core definitions that do not conflict with existing Hilt usage.
4. Add a Koin module verification test if feasible.
5. Keep Hilt active during this phase.

Acceptance criteria:

- App compiles.
- Koin can start with the initial modules.
- No existing Hilt injection is broken.

### Phase 2: Port Core and Data Providers

Port `AppModule` provider methods into Koin modules.

Core definitions to migrate:

- `DinoOrderApplication`
- `Context`
- `Gson`
- `SPrefManager`
- `SessionProvider` via `SPrefSessionProvider`
- `HttpClient`
- `HttpAPIUtil`
- `DataStoreUtil`
- `MessageDatabase`
- DAOs: `MessageDao`, `GroupMessageDao`, `CrashLogDao`,
  `SocketOutboundQueueDao`
- `CrashReporter`
- `AppSocketManager`
- `InAppNotificationManager`

Repository/API definitions to migrate:

- Auth API service and repository.
- Main API service and repository.
- Chat API service and repository.
- Thread API service and repository.
- Profile API service and repository.
- Search API service and repository.
- Splash API service.
- Report API service and repository.
- Activity notification API service and repository.
- Group API service and repository.

Acceptance criteria:

- All providers from `AppModule` have Koin equivalents.
- Interface bindings resolve to the same implementation classes.
- Android-only definitions use `androidContext()` or `androidApplication()`.

### Phase 3: Remove Constructor Injection From Non-ViewModel Classes

Feature by feature, remove `javax.inject.Inject` constructor annotations from:

- API service implementations.
- Repository implementations.
- Use cases.
- Managers/helpers.
- Data/session/socket classes.

Register each class in the correct Koin module before removing the annotation.

Recommended defaults:

- API services: `single`.
- Repositories: `single`.
- Stateful managers/reporters: `single`.
- Use cases: `factory`.
- Pure stateless helpers: `factory` unless sharing is intentional.

Acceptance criteria:

- No migrated class imports `javax.inject.Inject`.
- Koin definitions cover every constructor dependency.
- App compiles after each feature migration.

### Phase 4: Migrate ViewModels and Compose Call Sites

Feature by feature:

1. Register each ViewModel in Koin.
2. Replace `@HiltViewModel` with no DI annotation.
3. Remove `@Inject constructor`.
4. Replace `@ApplicationContext` with plain `Context` only where still needed.
5. Replace `hiltViewModel()` with `koinViewModel()`.
6. Use `parametersOf(...)` for route arguments that are constructor inputs.

Suggested order:

1. Small leaf screens: update, splash, search, report.
2. Core dialogs and main activity ViewModel.
3. Profile feature.
4. Main page feature.
5. Group feature.
6. Chat feature.
7. Thread feature.

Acceptance criteria:

- No migrated screen imports `androidx.hilt.navigation.compose.hiltViewModel`.
- No migrated ViewModel imports `dagger.hilt.android.lifecycle.HiltViewModel`.
- ViewModel recreation across navigation and configuration changes still works.

### Phase 5: Migrate Android Component Injection

Replace Hilt field injection in Android components.

For `DinoOrderApplication`:

- Remove `@HiltAndroidApp`.
- Start Koin once in `onCreate()`.
- Resolve `CrashReporter` and `InAppNotificationManager` through Koin.
- Preserve the crash UI process guard.

For `MainActivity`:

- Remove `@AndroidEntryPoint`.
- Replace field injection with Koin property injection:

```kotlin
private val app: Context by inject()
private val sPrefManager: SPrefManager by inject()
```

For services, workers, receivers, or other Android components:

- Prefer `by inject()` when the component is Koin-aware.
- Prefer `GlobalContext.get().get<T>()` only for framework-created classes where
  Koin property injection is awkward.
- Keep dependency lookup local and explicit.

Acceptance criteria:

- No Android component uses Hilt annotations.
- App startup, main navigation, notifications, crash reporting, and background
  behaviors still work.

### Phase 6: Remove Hilt Completely

After all call sites are migrated:

1. Delete or replace `AppModule`.
2. Remove all Hilt imports and annotations.
3. Remove all `javax.inject.*` imports and annotations.
4. Remove Hilt Gradle plugin and dependencies.
5. Remove Hilt navigation compose dependency.
6. Keep KSP only for Room and Compose Destinations.
7. Run repository-wide searches to confirm removal.

Required cleanup searches:

```bash
grep -RIn "dagger" app/src/main/java app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml
grep -RIn "hilt" app/src/main/java app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml
grep -RIn "javax.inject" app/src/main/java app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml
grep -RIn "@Inject" app/src/main/java
```

Acceptance criteria:

- Cleanup searches return no active Hilt/Dagger/Jakarta injection usage.
- `./gradlew :app:compileDebugKotlin` succeeds.
- Existing tests pass or unrelated failures are documented.

## Feature Migration Checklist

### Gradle and Bootstrap

- [ ] Add Koin version catalog entries.
- [ ] Add Koin app dependencies.
- [ ] Keep Hilt dependencies during parallel migration.
- [ ] Create Koin module files.
- [ ] Create `appModules` list.
- [ ] Start Koin from `DinoOrderApplication`.
- [ ] Verify app compiles with Hilt and Koin temporarily coexisting.

### Core Graph

- [ ] Port `Gson`.
- [ ] Port application `Context`.
- [ ] Port `SPrefManager`.
- [ ] Port `SPrefSessionProvider` and `SessionProvider`.
- [ ] Port `HttpClient`.
- [ ] Port `HttpAPIUtil`.
- [ ] Port `DataStoreUtil`.
- [ ] Port `MessageDatabase`.
- [ ] Port `MessageDao`.
- [ ] Port `GroupMessageDao`.
- [ ] Port `CrashLogDao`.
- [ ] Port `SocketOutboundQueueDao`.
- [ ] Port `CrashReporter`.
- [ ] Port `AppSocketManager`.
- [ ] Port `InAppNotificationManager`.

### Data and Repository Graph

- [ ] Port auth API/repository.
- [ ] Port main API/repository.
- [ ] Port chat API/repository.
- [ ] Port thread API/repository.
- [ ] Port profile API/repository.
- [ ] Port search API/repository.
- [ ] Port splash API service.
- [ ] Port report API/repository.
- [ ] Port activity notification API/repository.
- [ ] Port group API/repository.

### Use Cases

- [ ] Register auth use cases as factories.
- [ ] Register main page use cases as factories.
- [ ] Register chat use cases as factories.
- [ ] Register thread use cases as factories.
- [ ] Register profile use cases as factories.
- [ ] Register group use cases as factories.
- [ ] Register search use cases as factories.
- [ ] Register report use cases as factories.
- [ ] Register notification use cases as factories.
- [ ] Register core/shared use cases as factories.

### ViewModels and UI

- [ ] Register all 34 Hilt ViewModels in Koin.
- [ ] Replace `@HiltViewModel` annotations.
- [ ] Remove ViewModel `@Inject constructor` annotations.
- [ ] Replace all `hiltViewModel()` usages with `koinViewModel()`.
- [ ] Handle runtime navigation arguments with Koin parameters.
- [ ] Verify ViewModel state survives recomposition/configuration changes.

### Android Components

- [ ] Replace application field injection.
- [ ] Replace activity field injection.
- [ ] Audit services for injected dependencies.
- [ ] Audit workers/receivers for injected dependencies.
- [ ] Verify notification and crash-report flows.

### Final Cleanup

- [ ] Remove `AppModule` after all providers are replaced.
- [ ] Remove Hilt plugin from root Gradle.
- [ ] Remove Hilt plugin from app Gradle.
- [ ] Remove Hilt libraries from version catalog.
- [ ] Remove Hilt compiler dependency.
- [ ] Remove Hilt navigation compose dependency.
- [ ] Remove unused imports.
- [ ] Confirm no `dagger`, `hilt`, `javax.inject`, or `@Inject` usages remain.
- [ ] Run `./gradlew :app:compileDebugKotlin`.
- [ ] Run relevant unit/instrumented tests.

## Koin Definition Examples

### Core Module Example

```kotlin
val coreModule = module {
    single<DinoOrderApplication> { androidApplication() as DinoOrderApplication }
    single<Context> { androidContext() }
    single { Gson() }
    single { SPrefManager(gson = get(), context = androidContext()) }
    single<SessionProvider> { SPrefSessionProvider(sPrefManager = get()) }
    single {
        HttpClient(OkHttp) {
            defaultRequest {
                url(BuildConfig.BASE_URL)
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 20_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            }
        }
    }
    single { HttpAPIUtil(sPrefManager = get(), context = androidContext()) }
}
```

### Database Module Example

```kotlin
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            MessageDatabase::class.java,
            "message_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<MessageDatabase>().messageDao() }
    single { get<MessageDatabase>().groupMessageDao() }
    single { get<MessageDatabase>().crashLogDao() }
    single { get<MessageDatabase>().socketOutboundQueueDao() }
}
```

Keep the exact database name, migration strategy, and DAO accessor names aligned
with the existing `AppModule` and `MessageDatabase`.

### Feature Module Example

```kotlin
val reportModule = module {
    single<ReportApiService> {
        ReportApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
        )
    }
    single<ReportRepository> {
        ReportRepositoryImpl(reportApiService = get())
    }
    factoryOf(::GetReportTypesUseCase)
    factoryOf(::CreatePostReportUseCase)
    viewModelOf(::ReportPostBottomSheetViewModel)
}
```

Use explicit constructor calls when parameter names improve readability or when
Android `Context`, `Application`, or runtime navigation parameters are involved.

## Best Practices

- Migrate one feature slice at a time and compile after each slice.
- Keep Koin modules close to feature boundaries where practical.
- Prefer constructor injection as a design pattern even after removing
  `@Inject`.
- Avoid service-locator calls inside domain/data classes; use Koin only at graph
  boundaries and Android framework entrypoints.
- Keep Android-only dependencies out of domain code whenever the migration
  touches a constructor.
- Use interfaces for repositories and platform abstractions that will move to
  `commonMain` later.
- Do not hide missing dependencies with nullable lookups; fail fast during
  startup or tests.
- Use named qualifiers only when there are multiple bindings of the same type.
- Keep Koin module definitions deterministic and free of runtime side effects
  except object construction.

## Potential Pitfalls

- Duplicate definitions can silently resolve the wrong dependency if qualifiers
  are not used.
- `single` creates shared instances; using it for stateful screen-specific use
  cases can leak state across screens.
- `factory` recreates instances; using it for managers, socket clients,
  databases, or repositories can cause duplicated resources.
- Resolving Android dependencies before `startKoin` will crash.
- Resolving app dependencies before the crash-process guard may initialize
  services in the crash UI process unintentionally.
- ViewModels with route arguments need explicit Koin parameters.
- Compose Destinations generated code may contain `hiltViewModel()` usage or
  imports after screen signatures change.
- Removing Hilt too early can break generated components before all call sites
  are migrated.
- `javax.inject.Singleton` annotations can remain as no-ops if not removed,
  creating false confidence about scope.
- KSP should remain configured for Room and Compose Destinations even after Hilt
  compiler removal.

## Testing and Validation

Run checks from specific to broad:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
```

When adding Koin tests, use `koin-test` to start the app modules and resolve key
bindings:

- `HttpClient`
- `MessageDatabase`
- each DAO
- each repository interface
- each feature ViewModel

Manual smoke scenarios:

- App launches without Hilt.
- Login/register flow opens and performs network calls.
- Main screen loads contacts/groups.
- Chat screen sends, receives, stores, and clears messages.
- Thread/feed screens load posts, comments, stories, media, and playlist data.
- Profile screens load, edit, follow/unfollow, block/unblock.
- Report and activity notification flows work.
- Crash reporter installs handler and can flush pending logs.
- Notification manager connects/disconnects on foreground/background changes.

## Done Definition

The migration is complete when:

- Koin starts the full app dependency graph.
- Every runtime dependency previously provided by Hilt resolves through Koin.
- No source file imports `dagger.*`, `dagger.hilt.*`, or `javax.inject.*`.
- No source file uses `@HiltAndroidApp`, `@AndroidEntryPoint`,
  `@HiltViewModel`, `@Inject`, `@Module`, `@InstallIn`, or `@Provides`.
- Gradle no longer applies the Hilt plugin.
- Gradle no longer declares Hilt runtime/compiler/navigation dependencies.
- The app compiles and the relevant test suite passes or has only documented
  unrelated failures.

## References

- Koin Gradle setup: https://insert-koin.io/docs/setup/gradle/
- Koin Android setup: https://insert-koin.io/docs/reference/koin-android/start/
- Koin Compose integration: https://insert-koin.io/docs/reference/koin-compose/compose/
- Koin ViewModel API: https://insert-koin.io/docs/reference/koin-core/viewmodel/
