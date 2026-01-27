package com.bus;

import com.cart.Cartridge;

public final class MMU {
    private final Cartridge cart;
    private final IO io;

    private final byte[] vram = new byte[0x2000]; // 8000-9FFF
    private final byte[] wram = new byte[0x2000]; // C000-DFFF
    private final byte[] oam  = new byte[0x00A0]; // FE00-FE9F
    private final byte[] hram = new byte[0x007F]; // FF80-FFFE

    private int ie = 0x00; // FFFF

    public MMU(Cartridge cart, IO io) {
        this.cart = cart;
        this.io = io;
    }

    public int read8(int addr) {
        addr &= 0xFFFF;

        if (addr <= 0x7FFF) {
            return cart.readRom(addr);
        } else if (addr <= 0x9FFF) {
            return Byte.toUnsignedInt(vram[addr - 0x8000]);
        } else if (addr <= 0xBFFF) {
            return cart.readRam(addr);
        } else if (addr <= 0xDFFF) {
            return Byte.toUnsignedInt(wram[addr - 0xC000]);
        } else if (addr <= 0xFDFF) {
            // Echo RAM
            return Byte.toUnsignedInt(wram[addr - 0xE000]);
        } else if (addr <= 0xFE9F) {
            return Byte.toUnsignedInt(oam[addr - 0xFE00]);
        } else if (addr <= 0xFEFF) {
            // Unusable
            return 0xFF;
        } else if (addr <= 0xFF7F) {
            return io.read(addr);
        } else if (addr <= 0xFFFE) {
            return Byte.toUnsignedInt(hram[addr - 0xFF80]);
        } else {
            return ie & 0xFF;
        }
    }

    public void write8(int addr, int value) {
        addr &= 0xFFFF;
        value &= 0xFF;

        if (addr <= 0x7FFF) {
            cart.writeRom(addr, value);
        } else if (addr <= 0x9FFF) {
            vram[addr - 0x8000] = (byte) value;
        } else if (addr <= 0xBFFF) {
            cart.writeRam(addr, value);
        } else if (addr <= 0xDFFF) {
            wram[addr - 0xC000] = (byte) value;
        } else if (addr <= 0xFDFF) {
            wram[addr - 0xE000] = (byte) value; // echo
        } else if (addr <= 0xFE9F) {
            oam[addr - 0xFE00] = (byte) value;
        } else if (addr <= 0xFEFF) {
            // unusable, ignore
        } else if (addr <= 0xFF7F) {
            io.write(addr, value);
        } else if (addr <= 0xFFFE) {
            hram[addr - 0xFF80] = (byte) value;
        } else {
            ie = value;
        }
    }

    public int read16(int addr) {
        int lo = read8(addr);
        int hi = read8((addr + 1) & 0xFFFF);
        return lo | (hi << 8);
    }

    public void write16(int addr, int value) {
        write8(addr, value & 0xFF);
        write8((addr + 1) & 0xFFFF, (value >>> 8) & 0xFF);
    }

    public int getIE() { return ie & 0xFF; }
    public void setIE(int value) { ie = value & 0xFF; }

    public IO io() { return io; }
    public Cartridge cart() { return cart; }

    public void tick(int cyclesT) {

    }
}
