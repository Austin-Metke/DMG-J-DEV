package com.cart;

public final class MBC5 implements Cartridge {
    private final byte[] rom;
    private final SaveRam saveRam;

    private boolean ramEnabled = false;
    private int romBank = 1; // 9-bit
    private int ramBank = 0; // 0..15

    public MBC5(byte[] rom, SaveRam saveRam) {
        this.rom = rom;
        this.saveRam = saveRam;
    }

    @Override
    public int readRom(int addr) {
        addr &= 0x7FFF;
        if (addr < 0x4000) {
            return Byte.toUnsignedInt(rom[addr % rom.length]);
        }
        int bank = romBank & 0x1FF;
        if (bank == 0) bank = 1;
        int offset = bank * 0x4000 + (addr - 0x4000);
        return Byte.toUnsignedInt(rom[offset % rom.length]);
    }

    @Override
    public void writeRom(int addr, int value) {
        addr &= 0x7FFF;
        value &= 0xFF;

        if (addr < 0x2000) {
            ramEnabled = (value & 0x0F) == 0x0A;
        } else if (addr < 0x3000) {
            romBank = (romBank & 0x100) | value;
        } else if (addr < 0x4000) {
            romBank = (romBank & 0x0FF) | ((value & 0x01) << 8);
        } else if (addr < 0x6000) {
            ramBank = value & 0x0F;
        }
    }

    @Override
    public int readRam(int addr) {
        if (!ramEnabled || !saveRam.hasRam()) return 0xFF;
        int offset = ramBank * 0x2000 + (addr & 0x1FFF);
        return saveRam.read(offset);
    }

    @Override
    public void writeRam(int addr, int value) {
        if (!ramEnabled || !saveRam.hasRam()) return;
        int offset = ramBank * 0x2000 + (addr & 0x1FFF);
        saveRam.write(offset, value & 0xFF);
    }

    @Override
    public void saveIfBatteryBacked() {
        saveRam.saveIfNeeded();
    }

    @Override
    public void close() {
        saveRam.close();
    }
}
