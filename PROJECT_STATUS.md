# CalAdapt — Project Status Log

> **Purpose:** This file is the single source of truth for any AI model working on this project.
> Read this FIRST before exploring the codebase. It tells you exactly where things stand.
>
> **Last updated:** 2026-06-29 (Stage 5 complete, UI Palette overhaul)

---

## 🏗️ What Is CalAdapt?

An **offline-first Android app** (Kotlin + Jetpack Compose + Room + Hilt) that discovers your true maintenance calories empirically using weight trend analysis (EMA), then adaptively adjusts daily calorie & macro targets to hit your weight goal.

**Key tech:** MVVM + Clean Architecture, Material 3 with custom glassmorphism design system, single-activity with Compose Navigation.

---

## 📍 Stage Progress

| Stage | Focus | Status | Notes |
|:-----:|:------|:------:|:------|
| **1** | Foundation & Core Data Layer | ✅ **Done** | Room DB (7 entities, 7 DAOs), 6 Repositories, Hilt DI |
| **2** | Algorithms & Business Logic | ✅ **Done** | EMACalculator, TDEEEngine, MaintenanceDetector, CalorieTargetEngine, MacroCalculator, SafetyGuardrails, PhaseManager, 5 Use Cases |
| **3** | Onboarding & Profile Setup | ✅ **Done** | 4-step onboarding flow (name, sex, age, height, weight, unit system) |
| **4** | Daily Logging Screens | ✅ **Done** | Weight logging (with EMA trend chart, unit toggle, nudge buttons) + Calorie/macro logging (progress ring, meal entries) |
| **5** | Dashboard & Analytics | ✅ **Done** | Dashboard home screen done (CalorieRing, MacroProgressBars, StatCards). Dedicated Analytics screen completed (weight trend, calorie vs target bar chart, TDEE history, macro donut, weekly averages, date range selector). 5th bottom nav tab added. |
| **6** | Body Measurements | ✅ **Done** | Measurement logging screen (10 body fields, Navy body fat auto-calc, delta badges), History tab (trend charts, measurement log list). 4th bottom nav tab ("Measure") |
| **7** | Adaptive Intelligence & Reviews | ✅ **Done** | Weekly Review UI (weight trend, compliance score, TDEE update, new targets), Phase Transition flow (discovery→goal→maintenance), Diet break suggestions, Notification system |
| **8** | Polish, Settings & Edge Cases | ✅ **Done** | Apple Design polish done. Settings screen created with notifications & CSV export. Unit toggle, clear data danger zone, and About guide implemented. Edge cases covered. |

---

## 📂 Project Structure

```
com.caladapt
├── CalAdaptApplication.kt          # Hilt application class
├── MainActivity.kt                 # Single activity, nav host, bottom nav
├── data/
│   ├── db/
│   │   ├── CalAdaptDatabase.kt     # Room database (7 entities)
│   │   ├── entity/                 # 7 entity classes
│   │   └── dao/                    # 7 DAO interfaces
│   └── repository/                 # 6 repository classes
├── domain/
│   ├── algorithm/                  # EMA, TDEE, Calorie, Macro, Safety, Phase, Maintenance
│   ├── model/                      # Goal, Phase, Sex, UnitSystem enums
│   └── usecase/                    # LogWeight, LogCalories, GetDailyTargets, EvaluateWeekly, TransitionPhase
├── ui/
│   ├── theme/                      # Color.kt, Type.kt, Theme.kt (glassmorphism design system)
│   ├── components/                 # GlassComponents, CalorieRing, MacroProgressBar, StatCard, AnimationHelper, PhaseBadge, EmptyStates
│   ├── navigation/                 # Routes, CalAdaptNavGraph, BottomNavBar (4 tabs)
│   └── screens/
│       ├── onboarding/             # OnboardingScreen + ViewModel
│       ├── dashboard/              # DashboardScreen + ViewModel
│       ├── logcalories/            # LogCaloriesScreen + ViewModel
│       ├── logweight/              # LogWeightScreen + ViewModel
│       ├── measurements/           # MeasurementsScreen + ViewModel
│       └── analytics/              # AnalyticsScreen + ViewModel ← Stage 5
└── di/                             # DatabaseModule, AppModule (Hilt)
```

---

## 🎨 Design System Quick Reference

- **Glass cards:** `GlassCard`, `GlassHeroCard` (gradient), `GlassListItem`
- **Buttons:** `GlassAccentButton` (primary), `GlassButton` (secondary), `GlassOutlinedButton`, `GlassIconButton`
- **Inputs:** `GlassTextField` (styled OutlinedTextField)
- **Helpers:** `SectionHeader`, `GlassIconContainer`, `MeshBackground`
- **Animations:** `Modifier.staggeredEntrance(index)`, `Modifier.shimmer()`, `SuccessCheckmark`
- **Colors:** "Midnight Amethyst" dark mode palette. AccentViolet (CTA/Primary), AccentCyan (success/progress), AccentAmber (protein/secondary/warning), AccentIndigo (charts)
- **Patterns:** Every screen uses `MeshBackground` → `Column` with `verticalScroll` → staggered `GlassCard` children

---

## 🔜 Next Steps (Priority Order)

1. **DONE!** All stages complete. The app is ready for beta testing or deployment.

---

## ⚙️ Build Notes

- **JAVA_HOME** must be set to `C:\Program Files\Android\Android Studio\jbr` before running Gradle
- Build command: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
- Uses Gradle Kotlin DSL + Version Catalog
- `fallbackToDestructiveMigration()` is used for dev — need proper migrations before release
