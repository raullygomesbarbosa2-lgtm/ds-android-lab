# DS Android Lab

An original-from-scratch emulator research project. No existing emulator core is used or included.

## What is actually implemented

- `core/src/main/kotlin/lab/ds/core/Arm7Subset.kt` contains a standalone, incomplete Kotlin ARM-state interpreter experiment: a small subset of data-processing instructions, immediate branches, and simple word loads/stores over a 1 MiB byte array. It is not integrated into the Android APK, does not model DS memory/peripherals, and has known limitations; it is not a usable ARM7 or Nintendo DS emulator.
- `Arm7SubsetTest.kt` contains four JVM test cases (MOV/ADD, branch, word store/load, unsupported opcode). The current Android GitHub Actions build does **not** compile or run those tests.
- The Android debug APK is only a static diagnostic screen stating that it is not playable. No CPU core or game loading is present in the APK.

## Explicit limitations

No ARM9, Thumb mode, complete CPU/flags, memory map/MMU, DMA, timers, interrupts, sound, 2D/3D rendering, input mapping, cartridge protocol, firmware/BIOS, or game loader. No copyrighted BIOS, firmware or games are included. This APK cannot run commercial Nintendo DS games and is a nonfunctional emulator prototype, not a playable emulator.

## Build

GitHub Actions workflow `.github/workflows/android.yml` builds a debug APK (Android 7.0+, API 24+) and uploads it as `ds-android-lab-debug-apk` under the workflow run's Artifacts. It currently builds only the static shell. The ARM7 experiment and JVM tests are source-only and are not part of that APK/build.
