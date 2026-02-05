public class OpcodeInfo {
    public static String getMnemonic(int opcode) {
        opcode &= 0xFF;
        switch (opcode) {
            // 0x00 - 0x0F
            case 0x00: return "NOP";
            case 0x01: return "LD BC,d16";
            case 0x02: return "LD (BC),A";
            case 0x03: return "INC BC";
            case 0x04: return "INC B";
            case 0x05: return "DEC B";
            case 0x06: return "LD B,d8";
            case 0x07: return "RLCA";
            case 0x08: return "LD (a16),SP";
            case 0x09: return "ADD HL,BC";
            case 0x0A: return "LD A,(BC)";
            case 0x0B: return "DEC BC";
            case 0x0C: return "INC C";
            case 0x0D: return "DEC C";
            case 0x0E: return "LD C,d8";
            case 0x0F: return "RRCA";
            // 0x10 - 0x1F
            case 0x10: return "STOP";
            case 0x11: return "LD DE,d16";
            case 0x12: return "LD (DE),A";
            case 0x13: return "INC DE";
            case 0x14: return "INC D";
            case 0x15: return "DEC D";
            case 0x16: return "LD D,d8";
            case 0x17: return "RLA";
            case 0x18: return "JR r8";
            case 0x19: return "ADD HL,DE";
            case 0x1A: return "LD A,(DE)";
            case 0x1B: return "DEC DE";
            case 0x1C: return "INC E";
            case 0x1D: return "DEC E";
            case 0x1E: return "LD E,d8";
            case 0x1F: return "RRA";
            // 0x20 - 0x2F
            case 0x20: return "JR NZ,r8";
            case 0x21: return "LD HL,d16";
            case 0x22: return "LD (HL+),A";
            case 0x23: return "INC HL";
            case 0x24: return "INC H";
            case 0x25: return "DEC H";
            case 0x26: return "LD H,d8";
            case 0x27: return "DAA";
            case 0x28: return "JR Z,r8";
            case 0x29: return "ADD HL,HL";
            case 0x2A: return "LD A,(HL+)";
            case 0x2B: return "DEC HL";
            case 0x2C: return "INC L";
            case 0x2D: return "DEC L";
            case 0x2E: return "LD L,d8";
            case 0x2F: return "CPL";
            // 0x30 - 0x3F
            case 0x30: return "JR NC,r8";
            case 0x31: return "LD SP,d16";
            case 0x32: return "LD (HL-),A";
            case 0x33: return "INC SP";
            case 0x34: return "INC (HL)";
            case 0x35: return "DEC (HL)";
            case 0x36: return "LD (HL),d8";
            case 0x37: return "SCF";
            case 0x38: return "JR C,r8";
            case 0x39: return "ADD HL,SP";
            case 0x3A: return "LD A,(HL-)";
            case 0x3B: return "DEC SP";
            case 0x3C: return "INC A";
            case 0x3D: return "DEC A";
            case 0x3E: return "LD A,d8";
            case 0x3F: return "CCF";
            // 0x40 - 0x4F: LD B,r and LD C,r
            case 0x40: return "LD B,B";
            case 0x41: return "LD B,C";
            case 0x42: return "LD B,D";
            case 0x43: return "LD B,E";
            case 0x44: return "LD B,H";
            case 0x45: return "LD B,L";
            case 0x46: return "LD B,(HL)";
            case 0x47: return "LD B,A";
            case 0x48: return "LD C,B";
            case 0x49: return "LD C,C";
            case 0x4A: return "LD C,D";
            case 0x4B: return "LD C,E";
            case 0x4C: return "LD C,H";
            case 0x4D: return "LD C,L";
            case 0x4E: return "LD C,(HL)";
            case 0x4F: return "LD C,A";
            // 0x50 - 0x5F: LD D,r and LD E,r
            case 0x50: return "LD D,B";
            case 0x51: return "LD D,C";
            case 0x52: return "LD D,D";
            case 0x53: return "LD D,E";
            case 0x54: return "LD D,H";
            case 0x55: return "LD D,L";
            case 0x56: return "LD D,(HL)";
            case 0x57: return "LD D,A";
            case 0x58: return "LD E,B";
            case 0x59: return "LD E,C";
            case 0x5A: return "LD E,D";
            case 0x5B: return "LD E,E";
            case 0x5C: return "LD E,H";
            case 0x5D: return "LD E,L";
            case 0x5E: return "LD E,(HL)";
            case 0x5F: return "LD E,A";
            // 0x60 - 0x6F: LD H,r and LD L,r
            case 0x60: return "LD H,B";
            case 0x61: return "LD H,C";
            case 0x62: return "LD H,D";
            case 0x63: return "LD H,E";
            case 0x64: return "LD H,H";
            case 0x65: return "LD H,L";
            case 0x66: return "LD H,(HL)";
            case 0x67: return "LD H,A";
            case 0x68: return "LD L,B";
            case 0x69: return "LD L,C";
            case 0x6A: return "LD L,D";
            case 0x6B: return "LD L,E";
            case 0x6C: return "LD L,H";
            case 0x6D: return "LD L,L";
            case 0x6E: return "LD L,(HL)";
            case 0x6F: return "LD L,A";
            // 0x70 - 0x7F: LD (HL),r and LD A,r
            case 0x70: return "LD (HL),B";
            case 0x71: return "LD (HL),C";
            case 0x72: return "LD (HL),D";
            case 0x73: return "LD (HL),E";
            case 0x74: return "LD (HL),H";
            case 0x75: return "LD (HL),L";
            case 0x76: return "HALT";
            case 0x77: return "LD (HL),A";
            case 0x78: return "LD A,B";
            case 0x79: return "LD A,C";
            case 0x7A: return "LD A,D";
            case 0x7B: return "LD A,E";
            case 0x7C: return "LD A,H";
            case 0x7D: return "LD A,L";
            case 0x7E: return "LD A,(HL)";
            case 0x7F: return "LD A,A";
            // 0x80 - 0x8F: ADD A,r and ADC A,r
            case 0x80: return "ADD A,B";
            case 0x81: return "ADD A,C";
            case 0x82: return "ADD A,D";
            case 0x83: return "ADD A,E";
            case 0x84: return "ADD A,H";
            case 0x85: return "ADD A,L";
            case 0x86: return "ADD A,(HL)";
            case 0x87: return "ADD A,A";
            case 0x88: return "ADC A,B";
            case 0x89: return "ADC A,C";
            case 0x8A: return "ADC A,D";
            case 0x8B: return "ADC A,E";
            case 0x8C: return "ADC A,H";
            case 0x8D: return "ADC A,L";
            case 0x8E: return "ADC A,(HL)";
            case 0x8F: return "ADC A,A";
            // 0x90 - 0x9F: SUB r and SBC A,r
            case 0x90: return "SUB B";
            case 0x91: return "SUB C";
            case 0x92: return "SUB D";
            case 0x93: return "SUB E";
            case 0x94: return "SUB H";
            case 0x95: return "SUB L";
            case 0x96: return "SUB (HL)";
            case 0x97: return "SUB A";
            case 0x98: return "SBC A,B";
            case 0x99: return "SBC A,C";
            case 0x9A: return "SBC A,D";
            case 0x9B: return "SBC A,E";
            case 0x9C: return "SBC A,H";
            case 0x9D: return "SBC A,L";
            case 0x9E: return "SBC A,(HL)";
            case 0x9F: return "SBC A,A";
            // 0xA0 - 0xAF: AND r and XOR r
            case 0xA0: return "AND B";
            case 0xA1: return "AND C";
            case 0xA2: return "AND D";
            case 0xA3: return "AND E";
            case 0xA4: return "AND H";
            case 0xA5: return "AND L";
            case 0xA6: return "AND (HL)";
            case 0xA7: return "AND A";
            case 0xA8: return "XOR B";
            case 0xA9: return "XOR C";
            case 0xAA: return "XOR D";
            case 0xAB: return "XOR E";
            case 0xAC: return "XOR H";
            case 0xAD: return "XOR L";
            case 0xAE: return "XOR (HL)";
            case 0xAF: return "XOR A";
            // 0xB0 - 0xBF: OR r and CP r
            case 0xB0: return "OR B";
            case 0xB1: return "OR C";
            case 0xB2: return "OR D";
            case 0xB3: return "OR E";
            case 0xB4: return "OR H";
            case 0xB5: return "OR L";
            case 0xB6: return "OR (HL)";
            case 0xB7: return "OR A";
            case 0xB8: return "CP B";
            case 0xB9: return "CP C";
            case 0xBA: return "CP D";
            case 0xBB: return "CP E";
            case 0xBC: return "CP H";
            case 0xBD: return "CP L";
            case 0xBE: return "CP (HL)";
            case 0xBF: return "CP A";
            // 0xC0 - 0xCF: Control/misc
            case 0xC0: return "RET NZ";
            case 0xC1: return "POP BC";
            case 0xC2: return "JP NZ,a16";
            case 0xC3: return "JP a16";
            case 0xC4: return "CALL NZ,a16";
            case 0xC5: return "PUSH BC";
            case 0xC6: return "ADD A,d8";
            case 0xC7: return "RST 00H";
            case 0xC8: return "RET Z";
            case 0xC9: return "RET";
            case 0xCA: return "JP Z,a16";
            case 0xCB: return "PREFIX CB";
            case 0xCC: return "CALL Z,a16";
            case 0xCD: return "CALL a16";
            case 0xCE: return "ADC A,d8";
            case 0xCF: return "RST 08H";
            // 0xD0 - 0xDF: Control/misc
            case 0xD0: return "RET NC";
            case 0xD1: return "POP DE";
            case 0xD2: return "JP NC,a16";
            case 0xD3: return "ILLEGAL_D3";
            case 0xD4: return "CALL NC,a16";
            case 0xD5: return "PUSH DE";
            case 0xD6: return "SUB d8";
            case 0xD7: return "RST 10H";
            case 0xD8: return "RET C";
            case 0xD9: return "RETI";
            case 0xDA: return "JP C,a16";
            case 0xDB: return "ILLEGAL_DB";
            case 0xDC: return "CALL C,a16";
            case 0xDD: return "ILLEGAL_DD";
            case 0xDE: return "SBC A,d8";
            case 0xDF: return "RST 18H";
            // 0xE0 - 0xEF: LDH and misc
            case 0xE0: return "LDH (a8),A";
            case 0xE1: return "POP HL";
            case 0xE2: return "LD (C),A";
            case 0xE3: return "ILLEGAL_E3";
            case 0xE4: return "ILLEGAL_E4";
            case 0xE5: return "PUSH HL";
            case 0xE6: return "AND d8";
            case 0xE7: return "RST 20H";
            case 0xE8: return "ADD SP,r8";
            case 0xE9: return "JP (HL)";
            case 0xEA: return "LD (a16),A";
            case 0xEB: return "ILLEGAL_EB";
            case 0xEC: return "ILLEGAL_EC";
            case 0xED: return "ILLEGAL_ED";
            case 0xEE: return "XOR d8";
            case 0xEF: return "RST 28H";
            // 0xF0 - 0xFF: LDH and misc
            case 0xF0: return "LDH A,(a8)";
            case 0xF1: return "POP AF";
            case 0xF2: return "LD A,(C)";
            case 0xF3: return "DI";
            case 0xF4: return "ILLEGAL_F4";
            case 0xF5: return "PUSH AF";
            case 0xF6: return "OR d8";
            case 0xF7: return "RST 30H";
            case 0xF8: return "LD HL,SP+r8";
            case 0xF9: return "LD SP,HL";
            case 0xFA: return "LD A,(a16)";
            case 0xFB: return "EI";
            case 0xFC: return "ILLEGAL_FC";
            case 0xFD: return "ILLEGAL_FD";
            case 0xFE: return "CP d8";
            case 0xFF: return "RST 38H";
            default: return String.format("UNKNOWN_%02X", opcode);
        }
    }

    public static int getInstructionLength(int opcode) {
        opcode &= 0xFF;
        return switch (opcode) {
            // 1-byte instructions
            case 0x00, 0x02, 0x03, 0x04, 0x05, 0x07, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0F -> 1;
            case 0x12, 0x13, 0x14, 0x15, 0x17, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1F -> 1;
            case 0x22, 0x23, 0x24, 0x25, 0x27, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2F -> 1;
            case 0x32, 0x33, 0x34, 0x35, 0x37, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3F -> 1;
            // 0x40-0x7F: all 1-byte LD r,r and HALT
            case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47 -> 1;
            case 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F -> 1;
            case 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57 -> 1;
            case 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F -> 1;
            case 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67 -> 1;
            case 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F -> 1;
            case 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77 -> 1;
            case 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F -> 1;
            // 0x80-0xBF: all 1-byte ALU ops
            case 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87 -> 1;
            case 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F -> 1;
            case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97 -> 1;
            case 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F -> 1;
            case 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7 -> 1;
            case 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF -> 1;
            case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> 1;
            case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF -> 1;
            // Control flow - 1 byte
            case 0xC0, 0xC1, 0xC5, 0xC7, 0xC8, 0xC9, 0xCF -> 1;
            case 0xD0, 0xD1, 0xD3, 0xD5, 0xD7, 0xD8, 0xD9, 0xDB, 0xDD, 0xDF -> 1;
            case 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE7, 0xE9, 0xEB, 0xEC, 0xED, 0xEF -> 1;
            case 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF7, 0xF9, 0xFB, 0xFC, 0xFD, 0xFF -> 1;
            // 2-byte instructions (d8, r8, a8)
            case 0x06, 0x0E -> 2;  // LD r,d8
            case 0x10, 0x16, 0x18, 0x1E -> 2;  // STOP, LD r,d8, JR r8
            case 0x20, 0x26, 0x28, 0x2E -> 2;  // JR cc,r8, LD r,d8
            case 0x30, 0x36, 0x38, 0x3E -> 2;  // JR cc,r8, LD r,d8
            case 0xC6, 0xCB, 0xCE -> 2;  // ADD d8, PREFIX CB, ADC d8
            case 0xD6, 0xDE -> 2;  // SUB d8, SBC d8
            case 0xE0, 0xE6, 0xE8, 0xEE -> 2;  // LDH (a8),A, AND d8, ADD SP,r8, XOR d8
            case 0xF0, 0xF6, 0xF8, 0xFE -> 2;  // LDH A,(a8), OR d8, LD HL,SP+r8, CP d8
            // 3-byte instructions (d16, a16)
            case 0x01, 0x08, 0x11, 0x21, 0x31 -> 3;  // LD rr,d16, LD (a16),SP
            case 0xC2, 0xC3, 0xC4, 0xCA, 0xCC, 0xCD -> 3;  // JP/CALL
            case 0xD2, 0xD4, 0xDA, 0xDC -> 3;  // JP/CALL
            case 0xEA, 0xFA -> 3;  // LD (a16),A, LD A,(a16)
            default -> 1;
        };
    }
}
