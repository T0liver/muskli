# ARCHITECTURE.md

Status: **proposal v0.1.** Items marked *(open)* are decided in `docs/DECISIONS.md` before use. Data shapes come from `docs/research/04-data-model.md`; rules from `docs/research/05-business-rules-and-algorithms.md`.

## 1. Stack
| Concern | Choice | Status |
|---|---|---|
| Language | Kotlin | decided |
| UI | Jetpack Compose + Material 3 | decided |
| Navigation | Navigation Compose (type-safe routes) | proposed |
| State | ViewModel + `StateFlow`, unidirectional data flow | proposed |
| Async | Kotlin Coroutines + Flow | proposed |
| Persistence | Room (SQLite) | decided (D-002) |
| Settings | Jetpack DataStore (Preferences) | proposed |
| DI | Hilt | decided (D-006) |
| Serialization | kotlinx.serialization (export/import) | proposed |
| Testing | JUnit, kotlin.test, Turbine (Flow), Compose UI tests, Room in-memory tests | proposed |
| Build | Gradle Kotlin DSL, version catalog `gradle/libs.versions.toml` | proposed |
| minSdk / targetSdk | minSdk 24 (D-007); targetSdk = latest stable | decided (D-007) |

**Dependency policy:** an agent may not add a library without owner approval. When proposing one, state why the platform/stdlib isn't enough, and pin a stable version verified at that time (do not copy versions from memory).

## 2. Modules
```
:domain   pure Kotlin/JVM (no Android imports)
  model/        Workout, WorkoutExercise, WorkoutSet, ExerciseTemplate, enums, Weight, ...
  calc/         Volume, OneRepMax, Records, WarmUp, Plates, Streak, Units, PreviousValues
  repo/         repository interfaces
  usecase/      StartWorkout, AddExercise, CompleteSet, FinishWorkout, ...
:data     Android library
  db/           Room entities, DAOs, migrations, mappers
  repo/         repository implementations
  seed/         bundled catalogue loader (D-021)
  backup/       export/import (JSON)
:app      Android application
  ui/<feature>/ Screen, ViewModel, UiState, components
  ui/theme/     tokens (color, type, shape, spacing)
  service/      rest timer service / notifications
  di/           wiring
```
Rules: `:domain` depends on nothing. `:data` depends on `:domain`. `:app` depends on both. UI never touches DAOs.

### Exercise catalogue (D-005, D-021)
- **`catalogue` branch:** contributors add exercises as validated JSON. Format, schema, version and id rules, CI checks, and URL layout: `docs/CATALOGUE.md` (D-025).
- **CI:** PR checks format/schema; merge to `main` updates the canonical catalogue and publishes a **GitHub Pages** static JSON API.
- **App:** a **bundled snapshot** in assets (T-010) so first launch works offline, plus a **full sync** when online (T-021). Apply either source only when its version is newer; upsert by id; never touch custom exercises; do not hard-delete a published exercise a workout references. Delta-sync is post-MVP. Workout data stays local (D-002).

## 3. Data model
Follow `research/04-data-model.md` section 4 (SQLite proposal), translated to Room entities. Conventions:
- IDs: `String` (UUID/ULID) generated via an injected `IdGenerator`.
- Weights: kg `Double`. Time: epoch millis UTC + the workout's UTC offset (for week/day boundaries).
- Ordering: `position` columns, 0-based.
- Soft delete on workouts (`deletedAt`) to support undo and future sync.
- Schema export on; every schema change = a migration + migration test. **Schema changes need owner approval.**
- `workout_set.side` is in **schema v1**: non-null, default `both` (D-014, D-022). No left/right logging UI in the MVP.
- `completed` and `completed_at` are in schema v1 (F-03/F-05). `partial_reps` and `rpe_target` stay out until a ticket adds them.
- The database stays in the default app database directory so Android Auto Backup includes workouts (D-024). No backup-exclusion rules.
- Schema v2 adds `catalogue_meta`, one row holding the applied catalogue version (D-028).

### Enums
- SetType: `normal | warmup | dropset | failure`
- ExerciseType: 8 values (SPEC glossary)
- MuscleGroup (20): abdominals, shoulders, biceps, triceps, forearms, quadriceps, hamstrings, calves, glutes, abductors, adductors, lats, upper_back, traps, lower_back, chest, cardio, neck, full_body, other
- Equipment (9): none, barbell, dumbbell, kettlebell, machine, plate, resistance_band, suspension, other

## 4. Live workout design
- A live workout is a **draft** row set in Room (`workout.endTime == null`). Every edit writes immediately (debounced where typing). At most one active draft.
- `WorkoutSessionViewModel` exposes `StateFlow<LiveWorkoutUiState>` built from a Room Flow; events (`AddSet`, `CompleteSet`, `ChangeSetType`, ...) call use cases.
- `CompleteSet` use case: marks set completed with `completed_at`, evaluates the rest-timer rule (pure function in `:domain`), returns an effect (`StartRestTimer(seconds)` or `None`). The ViewModel forwards the effect to the timer service.
- Finish: validates, sets `endTime`, keeps only completed sets (decision: discard or keep uncompleted sets? default **discard with confirmation**, D-011).

## 5. Rest timer (reliability matters)
- Persist `restTimerEndsAtEpochMs` in the draft state. UI time left = `endsAt - now`, so it survives process death.
- Alerts when the screen is off or the app is backgrounded: **D-015** — `specialUse` foreground service while the app was recently used, plus a `setAlarmClock` exact alarm as backup. **T-009** confirmed this on the owner's Samsung (Android 16): the service fired on time, and the service alone did not fire with the screen off. Set the alarm on every rest even while the service is running.
- Notification actions: +15 s, skip.

## 6. UI architecture
- One `Screen` composable per route, stateless; ViewModel owns state. Reusable components in `ui/components/`.
- Design tokens in `ui/theme/` (color, typography, shape, spacing). Reference screenshots in `docs/reference-ui/` guide layout; never copy Hevy assets.
- The live-workout screen is designed first (ticket T-008) and its components are reused everywhere.
- Accessibility: 48dp targets, content descriptions, dynamic type, dark mode from day one.

## 7. Domain calculation module (`:domain/calc`)
Pure functions, no I/O, exhaustive unit tests from `docs/TEST-CASES.md`. API sketch (names are guidance, agents may refine with owner approval):
```kotlin
fun setVolumeKg(set: SetData, type: ExerciseType, bodyweightKg: Double?): Double
fun estimateOneRepMax(weightKg: Double, reps: Int, formula: OneRmFormula): Double?
fun warmUpSets(targetKg: Double, ladder: List<WarmUpRow>, roundingKg: Double): List<WarmUpSet>
fun platesPerSide(targetKg: Double, barKg: Double, inventory: List<Plate>): PlateResult
fun weeklyStreak(workoutDates: List<LocalDate>, weekStart: DayOfWeek, today: LocalDate): Int
fun restTimerEffect(completed: SetData, next: SetData?, exerciseRestS: Int?, defaultRestS: Int?): RestEffect
fun previousFor(history: List<WorkoutExerciseRecord>, setIndex: Int, mode: PreviousMode, routineId: String?): SetData?
fun kgToLb(kg: Double): Double
fun lbToKg(lb: Double): Double
```

## 8. Export format
Single JSON document with `schemaVersion`; mirrors Hevy's public API shapes where reasonable (research/06) so Hevy data maps cleanly. Import validates before applying (all-or-nothing transaction).

## 9. Testing strategy
| Layer | Tests |
|---|---|
| `:domain` | Unit tests, written first from `TC-*` (fast, no Android) |
| `:data` | DAO and migration tests with in-memory Room; repository tests |
| `:app` ViewModels | Unit tests with fake repositories + Turbine |
| `:app` UI | Compose UI tests for the live-workout happy path; screenshot tests optional later |
| Device | Manual acceptance scenarios `AC-*` on the owner's phone, run by VERIFY tickets |

## 10. Out of scope here
User-account / multi-device **workout** sync, analytics SDKs, crash-reporting SDKs, remote config, ads. (Public **exercise catalogue** download per D-005 is in scope.) Do not add out-of-scope items without owner approval.
