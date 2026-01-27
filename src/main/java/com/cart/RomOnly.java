package com.cart;

public final class RomOnly implements Cartridge {
    private final byte[] rom;
    private final SaveRam saveRam;

    public RomOnly(byte[] rom, SaveRam saveRam) {
        this.rom = rom;
        this.saveRam = saveRam;
    }

    @Override
    public int readRom(int addr) {
        addr &= 0x7FFF;
        return Byte.toUnsignedInt(rom[addr % rom.length]);
    }

    @Override
    public void writeRom(int addr, int value) {
        // no-op
    }

    @Override
    public int readRam(int addr) {
        if (!saveRam.hasRam()) return 0xFF;
        int offset = addr & 0x1FFF;
        return saveRam.read(offset);
    }

    @Override
    public void writeRam(int addr, int value) {
        if (!saveRam.hasRam()) return;
        int offset = addr & 0x1FFF;
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
