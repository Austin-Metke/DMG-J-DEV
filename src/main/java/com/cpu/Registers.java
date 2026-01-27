package com.cpu;

public final class Registers {
    // 8-bit regs
    public int A, F, B, C, D, E, H, L;
    // 16-bit
    public int SP, PC;

    // Flags in F (upper nibble)
    public static final int Z = 1 << 7;
    public static final int N = 1 << 6;
    public static final int HFLAG = 1 << 5;
    public static final int CFLAG = 1 << 4;

    public int AF() { return ((A & 0xFF) << 8) | (F & 0xF0); }
    public int BC() { return ((B & 0xFF) << 8) | (C & 0xFF); }
    public int DE() { return ((D & 0xFF) << 8) | (E & 0xFF); }
    public int HL() { return ((H & 0xFF) << 8) | (L & 0xFF); }

    public void setAF(int v) { A = (v >>> 8) & 0xFF; F = v & 0xF0; }
    public void setBC(int v) { B = (v >>> 8) & 0xFF; C = v & 0xFF; }
    public void setDE(int v) { D = (v >>> 8) & 0xFF; E = v & 0xFF; }
    public void setHL(int v) { H = (v >>> 8) & 0xFF; L = v & 0xFF; }

    public boolean getFlag(int mask) { return (F & mask) != 0; }
    public void setFlag(int mask, boolean on) {
        if (on) F |= mask;
        else F &= ~mask;
        F &= 0xF0; // lower nibble always 0 on GB
    }
}
