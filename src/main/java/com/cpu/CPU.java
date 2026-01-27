package com.cpu;

import com.bus.Bus;
import com.bus.MMU;

public final class CPU {
    private final Bus bus;
    public final Registers r = new Registers();

    private boolean ime = false;     // Interrupt Master Enable
    private boolean halted = false;

    public CPU(Bus bus) {
        this.bus = bus;
        powerOnDefaultsNoBootRom();
    }

    private void powerOnDefaultsNoBootRom() {
        // Common "no boot ROM" start state used by many emulators.
        // Not perfect, but good for bringing up early tests.
        r.PC = 0x0100;
        r.SP = 0xFFFE;

        r.A = 0x01; r.F = 0xB0;
        r.B = 0x00; r.C = 0x13;
        r.D = 0x00; r.E = 0xD8;
        r.H = 0x01; r.L = 0x4D;

        ime = false;
        halted = false;
    }

    /**
     * Step CPU by one instruction (or idle if halted).
     * Returns cycles in T-cycles. (Most docs: NOP=4T, JP nn=16T, etc.)
     */
    public int step() {
        // TODO: interrupt handling (IE/IF + priority + IME timing + HALT edge cases)
        if (halted) {
            // In real hardware, HALT wakes when an interrupt is pending (even if IME=0),
            // with special edge cases (HALT bug). We'll implement later.
            return 4;
        }

        int opcode = read8(r.PC++);
        return exec(opcode);
    }

    private int exec(int op) {
        return switch (op) {
            case 0x00 -> 4; // NOP

            case 0x31 -> { // LD SP,d16
                int nn = read16(r.PC);
                r.PC = (r.PC + 2) & 0xFFFF;
                r.SP = nn;
                yield 12;
            }

            case 0x3E -> { // LD A,d8
                r.A = read8(r.PC++);
                yield 8;
            }

            case 0xAF -> { // XOR A
                r.A ^= r.A;
                r.setFlag(Registers.Z, r.A == 0);
                r.setFlag(Registers.N, false);
                r.setFlag(Registers.HFLAG, false);
                r.setFlag(Registers.CFLAG, false);
                yield 4;
            }

            case 0xC3 -> { // JP a16
                int nn = read16(r.PC);
                r.PC = nn;
                yield 16;
            }

            case 0xEA -> { // LD (a16),A
                int nn = read16(r.PC);
                r.PC = (r.PC + 2) & 0xFFFF;
                write8(nn, r.A);
                yield 16;
            }

            case 0xCD -> { // CALL a16
                int nn = read16(r.PC);
                r.PC = (r.PC + 2) & 0xFFFF;
                push16(r.PC);
                r.PC = nn;
                yield 24;
            }

            case 0xC9 -> { // RET
                r.PC = pop16();
                yield 16;
            }

            case 0xF3 -> { // DI
                ime = false;
                yield 4;
            }

            case 0xFB -> { // EI (note: real EI enables IME after next instruction)
                // TODO: implement delayed IME enable
                ime = true;
                yield 4;
            }

            case 0x76 -> { // HALT
                halted = true;
                yield 4;
            }

            default -> throw new IllegalStateException(String.format(
                    "Unimplemented opcode 0x%02X at PC=0x%04X", op, (r.PC - 1) & 0xFFFF));
        };
    }

    private int read8(int addr) { return bus.read8(addr); }
    private void write8(int addr, int v) { bus.write8(addr, v); }
    private int read16(int addr) { return bus.read16(addr); }

    private void push16(int value) {
        r.SP = (r.SP - 1) & 0xFFFF;
        write8(r.SP, (value >>> 8) & 0xFF);
        r.SP = (r.SP - 1) & 0xFFFF;
        write8(r.SP, value & 0xFF);
    }

    private int pop16() {
        int lo = read8(r.SP);
        r.SP = (r.SP + 1) & 0xFFFF;
        int hi = read8(r.SP);
        r.SP = (r.SP + 1) & 0xFFFF;
        return lo | (hi << 8);
    }

    public boolean ime() { return ime; }
    public void setIme(boolean v) { ime = v; }

    public boolean halted() { return halted; }
    public void setHalted(boolean v) { halted = v; }

    public void reset() {
    }
}
