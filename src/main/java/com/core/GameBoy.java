package com.core;
import com.bus.Bus;
import com.cpu.CPU;

public class GameBoy {
    private final CPU cpu;
    private final Bus bus;
    // private final PPU ppu;
    // private final Timer timer;
    // etc

    public GameBoy(CPU cpu, Bus bus /*, PPU ppu, Timer timer, ... */) {
        this.cpu = cpu;
        this.bus = bus;
    }

    /**
     * Advance emulation by one CPU instruction (or microstep),
     * and tick all other hardware by the same number of T-cycles.
     */
    public int step() {
        int cycles = cpu.step();     // e.g. cycles in T-cycles or M-cycles, depending on how you define it
        bus.tick(cycles);           // bus propagates to PPU/timers/APU/etc
        return cycles;
    }

    public void run() {
        final int CYCLES_PER_FRAME = 70224; // classic DMG value if you're using T-cycles

        boolean running = true;
        while (running) {
            int frameCycles = 0;

            while (frameCycles < CYCLES_PER_FRAME) {
                frameCycles += step();
            }

            // renderer.presentFrame(ppu.getFrameBuffer());
            // poll input, update joypad state
            // sync to ~59.7 fps
            // check for quit
        }
    }
}
