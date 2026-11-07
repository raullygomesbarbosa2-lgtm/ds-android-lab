# DS Android Lab

An original, from-scratch Nintendo DS emulation research prototype. No melonDS, DeSmuME, or other emulator core is used or included. This is **not a complete emulator** and cannot run commercial DS games.

## Current implementation

- `Arm7Subset.kt`: a compact, newly written ARM-state interpreter for selected ARM7 instructions (data processing, branch, single data transfer, condition codes), with clear unsupported-instruction traps.
- Diagnostic Android shell; not a game player.
- JVM unit tests exercise arithmetic/flags, branch, memory and unsupported opcodes.

## Limitations

No ARM9, Thumb, MMU, DMA, timers, interrupts, sound, 2D/3D graphics, cartridge protocol, firmware, BIOS or game loader. No copyrighted BIOS/firmware or game data included. Any built APK is a diagnostic prototype and cannot play commercial DS games.

GitHub Actions builds a debug APK and runs the JVM tests. Find the APK under Actions → Android prototype build → Artifacts. Research prototype; no game compatibility implied.
