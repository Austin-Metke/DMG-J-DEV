package com.bus;

public final class IO {
    private final int[] regs = new int[0x80]; // FF00-FF7F
    private final SerialLogger serial;

    // Serial regs
    private int sb = 0x00; // FF01
    private int sc = 0x7E; // FF02

    // Interrupt flag (IF) is FF0F, but we’ll store it here too.
    // IE (FFFF) is in MMU.

    public IO(SerialLogger serial) {
        this.serial = serial;
        // Power-on-ish defaults (not complete)
        regs[0x00] = 0xCF; // JOYP (often 0xCF after boot)
        regs[0x02] = sc;
        regs[0x01] = sb;
        regs[0x0F] = 0xE1; // IF (common after boot; not strictly required yet)
    }

    public int read(int addr) {
        int off = addr & 0x7F;
        switch (addr & 0xFFFF) {
            case 0xFF01: return sb;
            case 0xFF02: return sc;
            default: return regs[off] & 0xFF;
        }
    }

    public void write(int addr, int value) {
        value &= 0xFF;
        int off = addr & 0x7F;

        switch (addr & 0xFFFF) {
            case 0xFF01 -> {
                sb = value;
                regs[0x01] = value;
            }
            case 0xFF02 -> {
                sc = value;
                regs[0x02] = value;

                // Common test ROM convention:
                // If SC written with 0x81, push SB byte to "serial output".
                if ((value & 0x81) == 0x81) {
                    serial.onByte(sb);
                }
            }
            default -> regs[off] = value;
        }
    }

    public int getIF() {
        return regs[0x0F] & 0xFF;
    }

    public void setIF(int value) {
        regs[0x0F] = value & 0xFF;
    }
}
