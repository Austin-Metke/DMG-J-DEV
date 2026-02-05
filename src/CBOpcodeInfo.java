public class CBOpcodeInfo {
    private static final String[] REGISTERS = {"B", "C", "D", "E", "H", "L", "(HL)", "A"};

    public static String getMnemonic(int cbOpcode) {
        cbOpcode &= 0xFF;
        int reg = cbOpcode & 0x07;
        int bit = (cbOpcode >> 3) & 0x07;
        String regName = REGISTERS[reg];

        // RLC (0x00-0x07)
        if (cbOpcode <= 0x07) return "RLC " + regName;
        // RRC (0x08-0x0F)
        if (cbOpcode <= 0x0F) return "RRC " + regName;
        // RL (0x10-0x17)
        if (cbOpcode <= 0x17) return "RL " + regName;
        // RR (0x18-0x1F)
        if (cbOpcode <= 0x1F) return "RR " + regName;
        // SLA (0x20-0x27)
        if (cbOpcode <= 0x27) return "SLA " + regName;
        // SRA (0x28-0x2F)
        if (cbOpcode <= 0x2F) return "SRA " + regName;
        // SWAP (0x30-0x37)
        if (cbOpcode <= 0x37) return "SWAP " + regName;
        // SRL (0x38-0x3F)
        if (cbOpcode <= 0x3F) return "SRL " + regName;
        // BIT (0x40-0x7F)
        if (cbOpcode <= 0x7F) return "BIT " + bit + "," + regName;
        // RES (0x80-0xBF)
        if (cbOpcode <= 0xBF) return "RES " + bit + "," + regName;
        // SET (0xC0-0xFF)
        return "SET " + bit + "," + regName;
    }

    public static int getInstructionLength(int cbOpcode) {
        // All CB-prefixed instructions are 2 bytes (CB prefix + opcode)
        return 2;
    }
}
