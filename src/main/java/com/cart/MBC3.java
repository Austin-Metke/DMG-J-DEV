package com.cart;

public final class MBC3 implements Cartridge {
    private final byte[] rom;
    private final SaveRam saveRam;

    private boolean ramEnabled = false;
    private int romBank = 1;   // 1..127
    private int ramBankOrRtc = 0; // 0..3 RAM banks, 0x08..0x0C RTC regs (RTC optional)

    public MBC3(byte[] rom, SaveRam saveRam) {
        this.rom = rom;
        this.saveRam = saveRam;
    }

    @Override
    public int readRom(int addr) {
        addr &= 0x7FFF;
        if (addr < 0x4000) {
            return Byte.toUnsignedInt(rom[addr % rom.length]);
        }
        int bank = romBank & 0x7F;
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
        } else if (addr < 0x4000) {
            romBank = value & 0x7F;
            if (romBank == 0) romBank = 1;
        } else if (addr < 0x6000) {
            ramBankOrRtc = value;
        } else {
            // 0x6000-0x7FFF latch clock data (RTC) - optional
            // We'll ignore for now. Add RTC later if you want.
        }
    }

    @Override
    public int readRam(int addr) {
        if (!ramEnabled || !saveRam.hasRam()) return 0xFF;
        int sel = ramBankOrRtc & 0xFF;
        if (sel >= 0x08 && sel <= 0x0C) {
            // RTC register read (not implemented)
            return 0xFF;
        }
        int bank = sel & 0x03;
        int offset = bank * 0x2000 + (addr & 0x1FFF);
        return saveRam.read(offset);
    }

    @Override
    public void writeRam(int addr, int value) {
        if (!ramEnabled || !saveRam.hasRam()) return;
        int sel = ramBankOrRtc & 0xFF;
        if (sel >= 0x08 && sel <= 0x0C) {
            // RTC register write (not implemented)
            return;
        }
        int bank = sel & 0x03;
        int offset = bank * 0x2000 + (addr & 0x1FFF);
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
