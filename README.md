# DMG-J

A Game Boy emulator written in Java. Supports loading ROMs, executing CPU instructions, handling memory management, GPU scanline rendering, sprite rendering (broken at the moment), and (partially) interactive debugging.

## Features

 Partial 8-bit Z80 CPU emulation (`Z80.java`)
 
 Instruction decoding (`CBOpcodeInfo.java`, `OpcodeInfo.java`)
 
 ROM loading and memory mapping (`ROMLoader.java`, `MMU.java`)
 
 GPU with scanline and sprite rendering (sprite rendering currently broken) (`GPU.java`, `Sprite.java`)
 
 Joypad input handling (Broken) (`Joypad.java`)


 Debugger with UI (`DebuggerUI.java`, `DebuggerController.java`)

## Getting Started

### Requirements

- Java 8 or newer
- A valid Game Boy ROM file (`.gb`)
- **NOTE** Only supports MCB1-3

## In Progress
- [ ] Load games without use of BIOS
- [ ] Input Handling
- [ ] Implement all opcodes
- [ ] Sprite Rendering (Broken)
- [ ] Interrupt Handling (Broken)

## TODO
- [x] Memory Viewer/Debugger (Crude)
- [-] Functional UI
- [x] Audio emulation
- [ ] Save states
- [x] BIOS support
- [ ] Link cable emulation
