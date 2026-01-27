package com.bus;


public final class Bus {
    private final MMU mmu;

    public Bus(MMU mmu) {
        this.mmu = mmu;
    }

    public int read8(int addr) { return mmu.read8(addr); }
    public void write8(int addr, int value) { mmu.write8(addr, value); }

    public int read16(int addr) { return mmu.read16(addr); }
    public void write16(int addr, int value) { mmu.write16(addr, value); }

    public void tick(int cyclesT) {
        mmu.tick(cyclesT); // MMU delegates to PPU/APU/Timer as needed
    }
}
