# DMG-J

A Game Boy (DMG) emulator written in Java.

## Features

- **CPU**: Sharp LR35902 emulation with full instruction set support
- **Graphics**: Scanline-based PPU with background and sprite rendering
- **Audio**: 4-channel APU (pulse, wave, noise)
- **Input**: Joypad emulation with keyboard mapping
- **Timer**: Hardware timer with interrupt support
- **Memory**: MBC1 mapper support for larger ROMs
- **Debugger**: Built-in debugger with memory viewer, disassembler, and breakpoints

## Screenshots

*Coming soon*

## Getting Started

### Requirements

- Java 8 or newer
- A Game Boy ROM file (`.gb` or `.gbc`)
- A Game Boy BIOS file (`gb_bios.bin`) - place in the project directory

### Building

```bash
# Compile all source files
javac -d bin src/*.java

# Run the emulator
java -cp bin Main
```

### Controls

| Key | Button |
|-----|--------|
| Arrow Keys | D-Pad |
| Z | A |
| X | B |
| Enter | Start |
| Backspace | Select |

### Loading ROMs

- Use **File > Open ROM** to select a ROM file
- Or drag and drop a `.gb` / `.gbc` file onto the window

## Project Structure

```
src/
├── Main.java           # Application entry point
├── GameBoy.java        # Main emulator coordinator
├── Z80.java            # CPU emulation (Sharp LR35902)
├── MMU.java            # Memory management unit
├── GPU.java            # Graphics processing unit
├── APU.java            # Audio processing unit
├── Timer_t.java        # Hardware timer
├── Joypad.java         # Input handling
├── Sprite.java         # Sprite data structure
├── ROMLoader.java      # ROM/BIOS file loading
├── OpcodeInfo.java     # CPU instruction metadata
├── CBOpcodeInfo.java   # CB-prefixed instruction metadata
├── DebuggerUI.java     # Debugger interface
└── DebuggerController.java  # Debugger logic
```

## Known Issues

- Window layer not yet implemented
- Some MBC types not supported (only MBC1 currently)
- HALT bug not implemented
- Serial link not implemented

## License

This project is for educational purposes.

## Acknowledgments

- [Pan Docs](https://gbdev.io/pandocs/) - Game Boy technical reference
- [Blargg's test ROMs](https://github.com/retrio/gb-test-roms) - CPU instruction tests
