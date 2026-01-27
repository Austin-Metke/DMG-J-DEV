package com.gbemu;

import com.bus.Bus;
import com.bus.IO;
import com.bus.MMU;
import com.bus.SerialLogger;
import com.cart.Cartridge;
import com.core.GameBoy;
import com.cpu.CPU;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        SerialLogger sl = new SerialLogger();
        Path romPath = Path.of("Tetris (World).gb");

        Cartridge cart = Cartridge.load(romPath, romPath.getParent());
        IO io = new IO(sl);
        MMU mmu = new MMU(cart, io);
        Bus bus = new Bus(mmu /* timer, ppu, apu, dma*/);
        CPU cpu = new CPU(bus);

        GameBoy gb = new GameBoy(cpu, bus);

        cpu.reset();
        gb.run();
    }

}

