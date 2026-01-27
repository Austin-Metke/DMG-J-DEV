package com.cart;

public final class MBC1 implements Cartridge {
    private final byte[] rom;
    private final SaveRam saveRam;

    private boolean ramEnabled = false;
    private int romBankLow5 = 1;      // 1..31 (0 becomes 1)
    private int bankHigh2 = 0;        // 0..3
    private int bankingMode = 0;      // 0=ROM banking, 1=RAM banking

    public MBC1(byte[] rom, SaveRam saveRam) {
        this.rom = rom;
        this.saveRam = saveRam;
    }

    @Override
    public int readRom(int addr) {
        addr &= 0x7FFF;
        if (addr < 0x4000) {
            int bank0 = (bankingMode == 0) ? 0 : (bankHigh2 << 5);
            int offset = bank0 * 0x4000 + addr;
            return Byte.toUnsignedInt(rom[offset % rom.length]);
        } else {
            int bank = (bankHigh2 << 5) | romBankLow5;
            bank = normalizeRomBank(bank);
            int offset = bank * 0x4000 + (addr - 0x4000);
            return Byte.toUnsignedInt(rom[offset % rom.length]);
        }
    }

    @Override
    public void writeRom(int addr, int value) {
        addr &= 0x7FFF;
        value &= 0xFF;

        if (addr < 0x2000) {
            ramEnabled = (value & 0x0F) == 0x0A;
        } else if (addr < 0x4000) {
            romBankLow5 = value & 0x1F;
            if (romBankLow5 == 0) romBankLow5 = 1;
        } else if (addr < 0x6000) {
            bankHigh2 = value & 0x03;
        } else {
            bankingMode = value & 0x01;
        }
    }

    @Override
    public int readRam(int addr) {
        if (!ramEnabled || !saveRam.hasRam()) return 0xFF;
        int ramBank = (bankingMode == 1) ? bankHigh2 : 0;
        int offset = ramBank * 0x2000 + (addr & 0x1FFF);
        return saveRam.read(offset);
    }

    @Override
    public void writeRam(int addr, int value) {
        if (!ramEnabled || !saveRam.hasRam()) return;
        int ramBank = (bankingMode == 1) ? bankHigh2 : 0;
        int offset = ramBank * 0x2000 + (addr & 0x1FFF);
        saveRam.write(offset, value & 0xFF);
    }

    private int normalizeRomBank(int bank) {
        // MBC1 quirk: banks 0x00,0x20,0x40,0x60 map to +1
        int masked = bank & 0x7F;
        if (masked == 0x00 || masked == 0x20 || masked == 0x40 || masked == 0x60) masked += 1;
        return masked;
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
