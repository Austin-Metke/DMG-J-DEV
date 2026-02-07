public class Z80 {

    //Time clock
    int clock_m;
    int clock_t;
    Registers registers;
    boolean enableIMEAfterNextInstr = false;
    private boolean skipBios;

    public void setMmu(MMU mmu) {
        this.mmu = mmu;
    }

    MMU mmu;
    final Instruction[] opcodeMap;
    private final Instruction[] cbOpcodeMap;
    private int lastCbOpcode;

    public Z80() {
        this.registers = new Registers();
        this.opcodeMap = new Instruction[256];
        this.cbOpcodeMap = new Instruction[256];

        opcodeMap[0x00] = this::NOP;
        opcodeMap[0x01] = this::LDBCnn;
        opcodeMap[0x02] = this::LDBCmA;
        opcodeMap[0x03] = this::INCBC;
        opcodeMap[0x04] = this::INCr_b;
        opcodeMap[0x05] = this::DECr_b;
        opcodeMap[0x06] = this::LDrn_b;
        opcodeMap[0x07] = this::RLCA;
        opcodeMap[0x08] = this::LDmmSP;
        opcodeMap[0x09] = this::ADDHLBC;
        opcodeMap[0x0A] = this::LDABCm;
        opcodeMap[0x0B] = this::DECBC;
        opcodeMap[0x0C] = this::INCr_c;
        opcodeMap[0x0D] = this::DECr_c;
        opcodeMap[0x0E] = this::LDrn_c;
        opcodeMap[0x0F] = this::RRCA;
        opcodeMap[0x10] = this::STOP;
        opcodeMap[0x11] = this::LDDEnn;
        opcodeMap[0x12] = this::LDDEmA;
        opcodeMap[0x13] = this::INCDE;
        opcodeMap[0x14] = this::INCr_d;
        opcodeMap[0x15] = this::DECr_d;
        opcodeMap[0x16] = this::LDrn_d;
        opcodeMap[0x17] = this::RLA;
        opcodeMap[0x18] = this::JRn;
        opcodeMap[0x19] = this::ADDHLDE;
        opcodeMap[0x1A] = this::LDADEm;
        opcodeMap[0x1B] = this::DECDE;
        opcodeMap[0x1C] = this::INCr_e;
        opcodeMap[0x1D] = this::DECr_e;
        opcodeMap[0x1E] = this::LDrn_e;
        opcodeMap[0x1F] = this::RRA;
        opcodeMap[0x20] = this::JRNZn;
        opcodeMap[0x21] = this::LDHLnn;
        opcodeMap[0x22] = this::LDHLIA;
        opcodeMap[0x23] = this::INCHL;
        opcodeMap[0x24] = this::INCr_h;
        opcodeMap[0x25] = this::DECr_h;
        opcodeMap[0x26] = this::LDrn_h;
        opcodeMap[0x27] = this::DAA;
        opcodeMap[0x28] = this::JRZn;
        opcodeMap[0x29] = this::ADDHLHL;
        opcodeMap[0x2A] = this::LDAHLI;
        opcodeMap[0x2B] = this::DECHL;
        opcodeMap[0x2C] = this::INCr_l;
        opcodeMap[0x2D] = this::DECr_l;
        opcodeMap[0x2E] = this::LDrn_l;
        opcodeMap[0x2F] = this::CPL;
        opcodeMap[0x30] = this::JRNCn;
        opcodeMap[0x31] = this::LDSPnn;
        opcodeMap[0x32] = this::LDHLDA;
        opcodeMap[0x33] = this::INCSP;
        opcodeMap[0x34] = this::INCHLm;
        opcodeMap[0x35] = this::DECHLm;
        opcodeMap[0x36] = this::LDHLmn;
        opcodeMap[0x37] = this::SCF;
        opcodeMap[0x38] = this::JRCn;
        opcodeMap[0x39] = this::ADDHLSP;
        opcodeMap[0x3A] = this::LDAHLD;
        opcodeMap[0x3B] = this::DECSP;
        opcodeMap[0x3C] = this::INCr_a;
        opcodeMap[0x3D] = this::DECr_a;
        opcodeMap[0x3E] = this::LDrn_a;
        opcodeMap[0x3F] = this::CCF;
        opcodeMap[0x40] = this::LDrr_bb;
        opcodeMap[0x41] = this::LDrr_bc;
        opcodeMap[0x42] = this::LDrr_bd;
        opcodeMap[0x43] = this::LDrr_be;
        opcodeMap[0x44] = this::LDrr_bh;
        opcodeMap[0x45] = this::LDrr_bl;
        opcodeMap[0x46] = this::LDrHLm_b;
        opcodeMap[0x47] = this::LDrr_ba;
        opcodeMap[0x48] = this::LDrr_cb;
        opcodeMap[0x49] = this::LDrr_cc;
        opcodeMap[0x4A] = this::LDrr_cd;
        opcodeMap[0x4B] = this::LDrr_ce;
        opcodeMap[0x4C] = this::LDrr_ch;
        opcodeMap[0x4D] = this::LDrr_cl;
        opcodeMap[0x4E] = this::LDrHLm_c;
        opcodeMap[0x4F] = this::LDrr_ca;
        opcodeMap[0x50] = this::LDrr_db;
        opcodeMap[0x51] = this::LDrr_dc;
        opcodeMap[0x52] = this::LDrr_dd;
        opcodeMap[0x53] = this::LDrr_de;
        opcodeMap[0x54] = this::LDrr_dh;
        opcodeMap[0x55] = this::LDrr_dl;
        opcodeMap[0x56] = this::LDrHLm_d;
        opcodeMap[0x57] = this::LDrr_da;
        opcodeMap[0x58] = this::LDrr_eb;
        opcodeMap[0x59] = this::LDrr_ec;
        opcodeMap[0x5A] = this::LDrr_ed;
        opcodeMap[0x5B] = this::LDrr_ee;
        opcodeMap[0x5C] = this::LDrr_eh;
        opcodeMap[0x5D] = this::LDrr_el;
        opcodeMap[0x5E] = this::LDrHLm_e;
        opcodeMap[0x5F] = this::LDrr_ea;
        opcodeMap[0x60] = this::LDrr_hb;
        opcodeMap[0x61] = this::LDrr_hc;
        opcodeMap[0x62] = this::LDrr_hd;
        opcodeMap[0x63] = this::LDrr_he;
        opcodeMap[0x64] = this::LDrr_hh;
        opcodeMap[0x65] = this::LDrr_hl;
        opcodeMap[0x66] = this::LDrHLm_h;
        opcodeMap[0x67] = this::LDrr_ha;
        opcodeMap[0x68] = this::LDrr_lb;
        opcodeMap[0x69] = this::LDrr_lc;
        opcodeMap[0x6A] = this::LDrr_ld;
        opcodeMap[0x6B] = this::LDrr_le;
        opcodeMap[0x6C] = this::LDrr_lh;
        opcodeMap[0x6D] = this::LDrr_ll;
        opcodeMap[0x6E] = this::LDrHLm_l;
        opcodeMap[0x6F] = this::LDrr_la;
        opcodeMap[0x70] = this::LDHLmr_b;
        opcodeMap[0x71] = this::LDHLmr_c;
        opcodeMap[0x72] = this::LDHLmr_d;
        opcodeMap[0x73] = this::LDHLmr_e;
        opcodeMap[0x74] = this::LDHLmr_h;
        opcodeMap[0x75] = this::LDHLmr_l;
        opcodeMap[0x76] = this::HALT;
        opcodeMap[0x77] = this::LDHLmr_a;
        opcodeMap[0x78] = this::LDrr_ab;
        opcodeMap[0x79] = this::LDrr_ac;
        opcodeMap[0x7A] = this::LDrr_ad;
        opcodeMap[0x7B] = this::LDrr_ae;
        opcodeMap[0x7C] = this::LDrr_ah;
        opcodeMap[0x7D] = this::LDrr_al;
        opcodeMap[0x7E] = this::LDrHLm_a;
        opcodeMap[0x7F] = this::LDrr_aa;
        opcodeMap[0x80] = this::ADDr_b;
        opcodeMap[0x81] = this::ADDr_c;
        opcodeMap[0x82] = this::ADDr_d;
        opcodeMap[0x83] = this::ADDr_e;
        opcodeMap[0x84] = this::ADDr_h;
        opcodeMap[0x85] = this::ADDr_l;
        opcodeMap[0x86] = this::ADDHL;
        opcodeMap[0x87] = this::ADDr_a;
        opcodeMap[0x88] = this::ADCr_b;
        opcodeMap[0x89] = this::ADCr_c;
        opcodeMap[0x8A] = this::ADCr_d;
        opcodeMap[0x8B] = this::ADCr_e;
        opcodeMap[0x8C] = this::ADCr_h;
        opcodeMap[0x8D] = this::ADCr_l;
        opcodeMap[0x8E] = this::ADCHL;
        opcodeMap[0x8F] = this::ADCr_a;
        opcodeMap[0x90] = this::SUBr_b;
        opcodeMap[0x91] = this::SUBr_c;
        opcodeMap[0x92] = this::SUBr_d;
        opcodeMap[0x93] = this::SUBr_e;
        opcodeMap[0x94] = this::SUBr_h;
        opcodeMap[0x95] = this::SUBr_l;
        opcodeMap[0x96] = this::SUBHL;
        opcodeMap[0x97] = this::SUBr_a;
        opcodeMap[0x98] = this::SBCr_b;
        opcodeMap[0x99] = this::SBCr_c;
        opcodeMap[0x9A] = this::SBCr_d;
        opcodeMap[0x9B] = this::SBCr_e;
        opcodeMap[0x9C] = this::SBCr_h;
        opcodeMap[0x9D] = this::SBCr_l;
        opcodeMap[0x9E] = this::SBCHL;
        opcodeMap[0x9F] = this::SBCr_a;
        opcodeMap[0xA0] = this::ANDr_b;
        opcodeMap[0xA1] = this::ANDr_c;
        opcodeMap[0xA2] = this::ANDr_d;
        opcodeMap[0xA3] = this::ANDr_e;
        opcodeMap[0xA4] = this::ANDr_h;
        opcodeMap[0xA5] = this::ANDr_l;
        opcodeMap[0xA6] = this::ANDHL;
        opcodeMap[0xA7] = this::ANDr_a;
        opcodeMap[0xA8] = this::XORr_b;
        opcodeMap[0xA9] = this::XORr_c;
        opcodeMap[0xAA] = this::XORr_d;
        opcodeMap[0xAB] = this::XORr_e;
        opcodeMap[0xAC] = this::XORr_h;
        opcodeMap[0xAD] = this::XORr_l;
        opcodeMap[0xAE] = this::XORHL;
        opcodeMap[0xAF] = this::XORr_a;
        opcodeMap[0xB0] = this::ORr_b;
        opcodeMap[0xB1] = this::ORr_c;
        opcodeMap[0xB2] = this::ORr_d;
        opcodeMap[0xB3] = this::ORr_e;
        opcodeMap[0xB4] = this::ORr_h;
        opcodeMap[0xB5] = this::ORr_l;
        opcodeMap[0xB6] = this::ORHL;
        opcodeMap[0xB7] = this::ORr_a;
        opcodeMap[0xB8] = this::CPr_b;
        opcodeMap[0xB9] = this::CPr_c;
        opcodeMap[0xBA] = this::CPr_d;
        opcodeMap[0xBB] = this::CPr_e;
        opcodeMap[0xBC] = this::CPr_h;
        opcodeMap[0xBD] = this::CPr_l;
        opcodeMap[0xBE] = this::CPHL;
        opcodeMap[0xBF] = this::CPr_a;
        opcodeMap[0xC0] = this::RETNZ;
        opcodeMap[0xC1] = this::POPBC;
        opcodeMap[0xC2] = this::JPNZnn;
        opcodeMap[0xC3] = this::JPnn;
        opcodeMap[0xC4] = this::CALLNZnn;
        opcodeMap[0xC5] = this::PUSHBC;
        opcodeMap[0xC6] = this::ADDn;
        opcodeMap[0xC7] = this::RST00;
        opcodeMap[0xC8] = this::RETZ;
        opcodeMap[0xC9] = this::RET;
        opcodeMap[0xCA] = this::JPZnn;
        opcodeMap[0xCB] = this::MAPcb;
        opcodeMap[0xCC] = this::CALLZnn;
        opcodeMap[0xCD] = this::CALLnn;
        opcodeMap[0xCE] = this::ADCn;
        opcodeMap[0xCF] = this::RST08;
        opcodeMap[0xD0] = this::RETNC;
        opcodeMap[0xD1] = this::POPDE;
        opcodeMap[0xD2] = this::JPNCnn;
        opcodeMap[0xD3] = this::XX;
        opcodeMap[0xD4] = this::CALLNCnn;
        opcodeMap[0xD5] = this::PUSHDE;
        opcodeMap[0xD6] = this::SUBn;
        opcodeMap[0xD7] = this::RST10;
        opcodeMap[0xD8] = this::RETC;
        opcodeMap[0xD9] = this::RETI;
        opcodeMap[0xDA] = this::JPCnn;
        opcodeMap[0xDB] = this::XX;
        opcodeMap[0xDC] = this::CALLCnn;
        opcodeMap[0xDD] = this::XX;
        opcodeMap[0xDE] = this::SBCn;
        opcodeMap[0xDF] = this::RST18;
        opcodeMap[0xE0] = this::LDIOnA;
        opcodeMap[0xE1] = this::POPHL;
        opcodeMap[0xE2] = this::LDIOCA;
        opcodeMap[0xE3] = this::XX;
        opcodeMap[0xE4] = this::XX;
        opcodeMap[0xE5] = this::PUSHHL;
        opcodeMap[0xE6] = this::ANDn;
        opcodeMap[0xE7] = this::RST20;
        opcodeMap[0xE8] = this::ADDSPn;
        opcodeMap[0xE9] = this::JPHL;
        opcodeMap[0xEA] = this::LDmmA;
        opcodeMap[0xEB] = this::XX;
        opcodeMap[0xEC] = this::XX;
        opcodeMap[0xED] = this::XX;
        opcodeMap[0xEE] = this::XORn;
        opcodeMap[0xEF] = this::RST28;
        opcodeMap[0xF0] = this::LDAIOn;
        opcodeMap[0xF1] = this::POPAF;
        opcodeMap[0xF2] = this::LDAIOC;
        opcodeMap[0xF3] = this::DI;
        opcodeMap[0xF4] = this::XX;
        opcodeMap[0xF5] = this::PUSHAF;
        opcodeMap[0xF6] = this::ORn;
        opcodeMap[0xF7] = this::RST30;
        opcodeMap[0xF8] = this::LDHLSPn;
        opcodeMap[0xF9] = this::LDSPHL;
        opcodeMap[0xFA] = this::LDAmm;
        opcodeMap[0xFB] = this::EI;
        opcodeMap[0xFC] = this::CALLMnn;
        opcodeMap[0xFD] = this::XX;
        opcodeMap[0xFE] = this::CPn;
        opcodeMap[0xFF] = this::RST38;
        cbOpcodeMap[0x00] = this::RLCr_b;
        cbOpcodeMap[0x01] = this::RLCr_c;
        cbOpcodeMap[0x02] = this::RLCr_d;
        cbOpcodeMap[0x03] = this::RLCr_e;
        cbOpcodeMap[0x04] = this::RLCr_h;
        cbOpcodeMap[0x05] = this::RLCr_l;
        cbOpcodeMap[0x06] = this::RLCHL;
        cbOpcodeMap[0x07] = this::RLCr_a;
        cbOpcodeMap[0x08] = this::RRCr_b;
        cbOpcodeMap[0x09] = this::RRCr_c;
        cbOpcodeMap[0x0A] = this::RRCr_d;
        cbOpcodeMap[0x0B] = this::RRCr_e;
        cbOpcodeMap[0x0C] = this::RRCr_h;
        cbOpcodeMap[0x0D] = this::RRCr_l;
        cbOpcodeMap[0x0E] = this::RRCHL;
        cbOpcodeMap[0x0F] = this::RRCr_a;
        cbOpcodeMap[0x10] = this::RLr_b;
        cbOpcodeMap[0x11] = this::RLr_c;
        cbOpcodeMap[0x12] = this::RLr_d;
        cbOpcodeMap[0x13] = this::RLr_e;
        cbOpcodeMap[0x14] = this::RLr_h;
        cbOpcodeMap[0x15] = this::RLr_l;
        cbOpcodeMap[0x16] = this::RLHL;
        cbOpcodeMap[0x17] = this::RLr_a;
        cbOpcodeMap[0x18] = this::RRr_b;
        cbOpcodeMap[0x19] = this::RRr_c;
        cbOpcodeMap[0x1A] = this::RRr_d;
        cbOpcodeMap[0x1B] = this::RRr_e;
        cbOpcodeMap[0x1C] = this::RRr_h;
        cbOpcodeMap[0x1D] = this::RRr_l;
        cbOpcodeMap[0x1E] = this::RRHL;
        cbOpcodeMap[0x1F] = this::RRr_a;
        cbOpcodeMap[0x20] = this::SLAr_b;
        cbOpcodeMap[0x21] = this::SLAr_c;
        cbOpcodeMap[0x22] = this::SLAr_d;
        cbOpcodeMap[0x23] = this::SLAr_e;
        cbOpcodeMap[0x24] = this::SLAr_h;
        cbOpcodeMap[0x25] = this::SLAr_l;
        cbOpcodeMap[0x26] = this::SLAHL;
        cbOpcodeMap[0x27] = this::SLAr_a;
        cbOpcodeMap[0x28] = this::SRAr_b;
        cbOpcodeMap[0x29] = this::SRAr_c;
        cbOpcodeMap[0x2A] = this::SRAr_d;
        cbOpcodeMap[0x2B] = this::SRAr_e;
        cbOpcodeMap[0x2C] = this::SRAr_h;
        cbOpcodeMap[0x2D] = this::SRAr_l;
        cbOpcodeMap[0x2E] = this::SRAHL;
        cbOpcodeMap[0x2F] = this::SRAr_a;
        cbOpcodeMap[0x30] = this::SWAPr_b;
        cbOpcodeMap[0x31] = this::SWAPr_c;
        cbOpcodeMap[0x32] = this::SWAPr_d;
        cbOpcodeMap[0x33] = this::SWAPr_e;
        cbOpcodeMap[0x34] = this::SWAPr_h;
        cbOpcodeMap[0x35] = this::SWAPr_l;
        cbOpcodeMap[0x36] = this::SWAPHL;
        cbOpcodeMap[0x37] = this::SWAPr_a;
        cbOpcodeMap[0x38] = this::SRLr_b;
        cbOpcodeMap[0x39] = this::SRLr_c;
        cbOpcodeMap[0x3A] = this::SRLr_d;
        cbOpcodeMap[0x3B] = this::SRLr_e;
        cbOpcodeMap[0x3C] = this::SRLr_h;
        cbOpcodeMap[0x3D] = this::SRLr_l;
        cbOpcodeMap[0x3E] = this::SRLHL;
        cbOpcodeMap[0x3F] = this::SRLr_a;
        cbOpcodeMap[0x40] = this::BIT0b;
        cbOpcodeMap[0x41] = this::BIT0c;
        cbOpcodeMap[0x42] = this::BIT0d;
        cbOpcodeMap[0x43] = this::BIT0e;
        cbOpcodeMap[0x44] = this::BIT0h;
        cbOpcodeMap[0x45] = this::BIT0l;
        cbOpcodeMap[0x46] = this::BIT0m;
        cbOpcodeMap[0x47] = this::BIT0a;
        cbOpcodeMap[0x48] = this::BIT1b;
        cbOpcodeMap[0x49] = this::BIT1c;
        cbOpcodeMap[0x4A] = this::BIT1d;
        cbOpcodeMap[0x4B] = this::BIT1e;
        cbOpcodeMap[0x4C] = this::BIT1h;
        cbOpcodeMap[0x4D] = this::BIT1l;
        cbOpcodeMap[0x4E] = this::BIT1m;
        cbOpcodeMap[0x4F] = this::BIT1a;
        cbOpcodeMap[0x50] = this::BIT2b;
        cbOpcodeMap[0x51] = this::BIT2c;
        cbOpcodeMap[0x52] = this::BIT2d;
        cbOpcodeMap[0x53] = this::BIT2e;
        cbOpcodeMap[0x54] = this::BIT2h;
        cbOpcodeMap[0x55] = this::BIT2l;
        cbOpcodeMap[0x56] = this::BIT2m;
        cbOpcodeMap[0x57] = this::BIT2a;
        cbOpcodeMap[0x58] = this::BIT3b;
        cbOpcodeMap[0x59] = this::BIT3c;
        cbOpcodeMap[0x5A] = this::BIT3d;
        cbOpcodeMap[0x5B] = this::BIT3e;
        cbOpcodeMap[0x5C] = this::BIT3h;
        cbOpcodeMap[0x5D] = this::BIT3l;
        cbOpcodeMap[0x5E] = this::BIT3m;
        cbOpcodeMap[0x5F] = this::BIT3a;
        cbOpcodeMap[0x60] = this::BIT4b;
        cbOpcodeMap[0x61] = this::BIT4c;
        cbOpcodeMap[0x62] = this::BIT4d;
        cbOpcodeMap[0x63] = this::BIT4e;
        cbOpcodeMap[0x64] = this::BIT4h;
        cbOpcodeMap[0x65] = this::BIT4l;
        cbOpcodeMap[0x66] = this::BIT4m;
        cbOpcodeMap[0x67] = this::BIT4a;
        cbOpcodeMap[0x68] = this::BIT5b;
        cbOpcodeMap[0x69] = this::BIT5c;
        cbOpcodeMap[0x6A] = this::BIT5d;
        cbOpcodeMap[0x6B] = this::BIT5e;
        cbOpcodeMap[0x6C] = this::BIT5h;
        cbOpcodeMap[0x6D] = this::BIT5l;
        cbOpcodeMap[0x6E] = this::BIT5m;
        cbOpcodeMap[0x6F] = this::BIT5a;
        cbOpcodeMap[0x70] = this::BIT6b;
        cbOpcodeMap[0x71] = this::BIT6c;
        cbOpcodeMap[0x72] = this::BIT6d;
        cbOpcodeMap[0x73] = this::BIT6e;
        cbOpcodeMap[0x74] = this::BIT6h;
        cbOpcodeMap[0x75] = this::BIT6l;
        cbOpcodeMap[0x76] = this::BIT6m;
        cbOpcodeMap[0x77] = this::BIT6a;
        cbOpcodeMap[0x78] = this::BIT7b;
        cbOpcodeMap[0x79] = this::BIT7c;
        cbOpcodeMap[0x7A] = this::BIT7d;
        cbOpcodeMap[0x7B] = this::BIT7e;
        cbOpcodeMap[0x7C] = this::BIT7h;
        cbOpcodeMap[0x7D] = this::BIT7l;
        cbOpcodeMap[0x7E] = this::BIT7m;
        cbOpcodeMap[0x7F] = this::BIT7a;
        for (int j = 0x80; j <= 0xBF; j++) cbOpcodeMap[j] = this::executeRES;
        for (int j = 0xC0; j <= 0xFF; j++) cbOpcodeMap[j] = this::executeSET;
        
    }


    /**
     * Execute a single instruction: fetch, decode, execute, update clocks,
     * handle delayed IME enable, and check interrupts.
     */
    public void tick() {
        // Fetch and execute opcode
        int opcode = mmu.readByte(registers.pc++) & 0xFF;
        opcodeMap[opcode].execute();
        registers.pc &= 0xFFFF;  // Mask PC to 16-bit

        // Update master clock
        clock_m += registers.m;
        clock_t += registers.t;

        // Handle delayed IME enable (EI takes effect after next instruction)
        if (enableIMEAfterNextInstr) {
            registers.ime = 1;
            enableIMEAfterNextInstr = false;
        }

        // Check and handle interrupts
        checkInterrupts();

        // Update clock again for any cycles used by interrupt handling
        clock_m += registers.m;
        clock_t += registers.t;
    }

    public void reset() {

        this.clock_m = 0;
        this.clock_t = 0;
        this.enableIMEAfterNextInstr = false;
        
        // Reset all registers
        registers.a = 0;
        registers.b = 0;
        registers.c = 0;
        registers.d = 0;
        registers.e = 0;
        registers.h = 0;
        registers.l = 0;
        registers.f = 0;
        registers.pc = 0;
        registers.sp = 0;
        registers.ime = 0;
        registers.m = 0;
        registers.t = 0;

        if(skipBios) {
            registers.pc = 0x100;
            registers.sp = 0xFFFE;
            registers.a = 0x01;
            registers.f = 0xB0;
            registers.b = 0x00;
            registers.c = 0x13;
            registers.d = 0x00;
            registers.e = 0xD8;
            registers.h = 0x01;
            registers.l = 0x4D;

            

        }
        
        
    }










    /*
        *** Load/Store ***
     */
    public void LDrr_bb() { registers.b = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_bc() { registers.b = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_bd() { registers.b = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_be() { registers.b = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_bh() { registers.b = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_bl() { registers.b = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_ba() { registers.b = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrr_cb() { registers.c = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_cc() { registers.c = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_cd() { registers.c = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_ce() { registers.c = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_ch() { registers.c = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_cl() { registers.c = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_ca() { registers.c = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrr_db() { registers.d = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_dc() { registers.d = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_dd() { registers.d = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_de() { registers.d = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_dh() { registers.d = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_dl() { registers.d = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_da() { registers.d = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrr_eb() { registers.e = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_ec() { registers.e = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_ed() { registers.e = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_ee() { registers.e = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_eh() { registers.e = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_el() { registers.e = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_ea() { registers.e = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrr_hb() { registers.h = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_hc() { registers.h = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_hd() { registers.h = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_he() { registers.h = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_hh() { registers.h = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_hl() { registers.h = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_ha() { registers.h = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrr_lb() { registers.l = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_lc() { registers.l = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_ld() { registers.l = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_le() { registers.l = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_lh() { registers.l = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_ll() { registers.l = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_la() { registers.l = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrr_ab() { registers.a = registers.b; registers.m = 1; registers.t = 4; }
    public void LDrr_ac() { registers.a = registers.c; registers.m = 1; registers.t = 4; }
    public void LDrr_ad() { registers.a = registers.d; registers.m = 1; registers.t = 4; }
    public void LDrr_ae() { registers.a = registers.e; registers.m = 1; registers.t = 4; }
    public void LDrr_ah() { registers.a = registers.h; registers.m = 1; registers.t = 4; }
    public void LDrr_al() { registers.a = registers.l; registers.m = 1; registers.t = 4; }
    public void LDrr_aa() { registers.a = registers.a; registers.m = 1; registers.t = 4; }

    public void LDrHLm_b() { registers.b = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }
    public void LDrHLm_c() { registers.c = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }
    public void LDrHLm_d() { registers.d = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }
    public void LDrHLm_e() { registers.e = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }
    public void LDrHLm_h() { registers.h = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }
    public void LDrHLm_l() { registers.l = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }
    public void LDrHLm_a() { registers.a = mmu.readByte((registers.h<<8)+registers.l); registers.m=2; registers.t=8; }

    public void LDHLmr_b() { mmu.writeByte((registers.getHL()), registers.b); registers.m = 2; registers.t = 8;}
    public void LDHLmr_c() { mmu.writeByte((registers.getHL()), registers.c); registers.m = 2; registers.t = 8;}
    public void LDHLmr_d() { mmu.writeByte((registers.getHL()), registers.d); registers.m = 2; registers.t = 8;}
    public void LDHLmr_e() { mmu.writeByte((registers.getHL()), registers.e); registers.m = 2; registers.t = 8;}
    public void LDHLmr_h() { mmu.writeByte(registers.getHL(), registers.h); registers.m = 2; registers.t = 8;}
    public void LDHLmr_l() { mmu.writeByte(registers.getHL(), registers.l); registers.m = 2; registers.t = 8;}
    public void LDHLmr_a() { mmu.writeByte(registers.getHL(), registers.a); registers.m = 2; registers.t = 8;}

    public void LDrn_b() { registers.b = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}
    public void LDrn_c() { registers.c = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}
    public void LDrn_d() { registers.d = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}
    public void LDrn_e() { registers.e = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}
    public void LDrn_h() { registers.h = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}
    public void LDrn_l() { registers.l = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}
    public void LDrn_a() { registers.a = mmu.readByte(registers.pc); registers.pc++; registers.m = 2; registers.t = 8;}

    public void LDHLmn() { mmu.writeByte((registers.h<<8)+registers.l, mmu.readByte(registers.pc)); registers.pc++; registers.m = 3; registers.t = 12;}

    public void LDBCmA() { mmu.writeByte(registers.getBC(), registers.a); registers.m = 2; registers.t = 8; }

    public void LDDEmA() { mmu.writeByte(registers.getDE(), registers.a); registers.m = 2; registers.t = 8; }

    public void LDmmA() { mmu.writeByte(mmu.readWord(registers.pc), registers.a); registers.pc +=2; registers.m = 4; registers.t=16; }

    public void LDABCm() { registers.a = mmu.readByte(registers.getBC()); registers.m = 2; registers.t = 8; }
    public void LDADEm() { registers.a = mmu.readByte(registers.getDE()); registers.m = 2; registers.t = 8; }

    public void LDAmm() { int addr = mmu.readWord(registers.pc); registers.pc += 2; registers.a = mmu.readByte(addr); registers.m = 4; registers.t = 16; }

    public void LDBCnn() { registers.c = mmu.readByte(registers.pc); registers.b = mmu.readByte(registers.pc+1); registers.pc+=2; registers.m = 3; registers.t = 12; }
    public void LDDEnn() { registers.e = mmu.readByte(registers.pc); registers.d = mmu.readByte(registers.pc+1); registers.pc+=2; registers.m = 3; registers.t = 12; }
    public void LDHLnn() { registers.l = mmu.readByte(registers.pc); registers.h = mmu.readByte(registers.pc+1); registers.pc +=2; registers.m = 3; registers.t = 12; }
    public void LDSPnn() { registers.sp = mmu.readWord(registers.pc); registers.pc +=2; registers.m = 3; registers.t = 12; }


    public void LDmmSP() { int addr = mmu.readWord(registers.pc); registers.pc+=2; mmu.writeByte(addr, registers.sp&0xFF); mmu.writeByte((addr+1)&0xFFFF, registers.sp>>8); registers.m = 5; registers.t = 20;}
    public void LDHLIA() { mmu.writeByte(registers.getHL(), registers.a); registers.l = (registers.l + 1) & 0xFF; if ((registers.l & 0xFF) == 0) { registers.h = (registers.h + 1) & 0xFF;} registers.m = 2; registers.t = 8; }
    public void LDAHLI() { registers.a = mmu.readByte(registers.getHL()); registers.l = (registers.l+1)&0xFF; if ((registers.l&0xFF) == 0) { registers.h = (registers.h+1)&0xFF;} registers.m = 2; registers.t = 8; }

    public void LDHLDA() {
        mmu.writeByte(registers.getHL(), registers.a);
        if (registers.l == 0) {
            registers.l = 0xFF;
            registers.h = (registers.h - 1) & 0xFF;
        } else {
            registers.l = (registers.l - 1) & 0xFF;
        }
        registers.m = 2;
        registers.t = 8;
    }

    public void LDAHLD() {
        registers.a = mmu.readByte(registers.getHL());
        registers.l = (registers.l-1)&0xFF;
        if(registers.l==255) {
            registers.h = (registers.h-1)&0xFF;
        }

        registers.m = 2;
        registers.t = 8;
    }

    public void LDAIOn() { registers.a = mmu.readByte(0xFF00+mmu.readByte(registers.pc)); registers.pc++; registers.m = 3; registers.t = 12;}
    public void LDIOnA() { mmu.writeByte(0xFF00+mmu.readByte(registers.pc), registers.a); registers.pc++; registers.m=3; registers.t=12; }
    public void LDAIOC() { registers.a = mmu.readByte(0xFF00+ registers.c); registers.m = 2; registers.t = 8; }
    public void LDIOCA() { mmu.writeByte(0xFF00+registers.c, registers.a); registers.m = 2; registers.t = 8; }

    public void LDHLSPn() {
        int offset = (byte) mmu.readByte(registers.pc);
        registers.pc++;
        int sp = registers.sp;
        int result = (sp + offset) & 0xFFFF;
        registers.h = (result >> 8) & 0xFF;
        registers.l = result & 0xFF;
        registers.f = 0;
        int lowSum = (sp & 0xFF) + (offset & 0xFF);
        if ((lowSum & 0x100) != 0) registers.f |= 0x10;
        if (((sp & 0xF) + (offset & 0xF)) > 0xF) registers.f |= 0x20;
        registers.m = 3;
        registers.t = 12;
    }

    public void SWAPr_b() { registers.b = ((registers.b >> 4) | (registers.b << 4)) & 0xFF; fz(registers.b); registers.m = 2; registers.t = 8; }
    public void SWAPr_c() { registers.c = ((registers.c >> 4) | (registers.c << 4)) & 0xFF; fz(registers.c); registers.m = 2; registers.t = 8; }
    public void SWAPr_d() { registers.d = ((registers.d >> 4) | (registers.d << 4)) & 0xFF; fz(registers.d); registers.m = 2; registers.t = 8; }
    public void SWAPr_e() { registers.e = ((registers.e >> 4) | (registers.e << 4)) & 0xFF; fz(registers.e); registers.m = 2; registers.t = 8; }
    public void SWAPr_h() { registers.h = ((registers.h >> 4) | (registers.h << 4)) & 0xFF; fz(registers.h); registers.m = 2; registers.t = 8; }
    public void SWAPr_l() { registers.l = ((registers.l >> 4) | (registers.l << 4)) & 0xFF; fz(registers.l); registers.m = 2; registers.t = 8; }
    public void SWAPr_a() { registers.a = ((registers.a >> 4) | (registers.a << 4)) & 0xFF; fz(registers.a); registers.m = 2; registers.t = 8; }
    public void SWAPHL() { int v = mmu.readByte(registers.getHL()); v = ((v >> 4) | (v << 4)) & 0xFF; mmu.writeByte(registers.getHL(), v); fz(v); registers.m = 4; registers.t = 16; }

    /* **Data Processing** */

    public void ADDr_b() { addA(registers.b); registers.m = 1; registers.t = 4; }
    public void ADDr_c() { addA(registers.c); registers.m = 1; registers.t = 4; }
    public void ADDr_d() { addA(registers.d); registers.m = 1; registers.t = 4; }
    public void ADDr_e() { addA(registers.e); registers.m = 1; registers.t = 4; }
    public void ADDr_h() { addA(registers.h); registers.m = 1; registers.t = 4; }
    public void ADDr_l() { addA(registers.l); registers.m = 1; registers.t = 4; }
    public void ADDr_a() { addA(registers.a); registers.m = 1; registers.t = 4; }

    public void ADDHL() { addA(mmu.readByte(registers.getHL())); registers.m = 2; registers.t = 8; }
    public void ADDn() { addA(mmu.readByte(registers.pc)); registers.pc++; registers.m = 2; registers.t = 8; }

    private void addA(int val) {
        int a = registers.a;
        int result = a + val;
        registers.f = 0;
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
        if (((a & 0xF) + (val & 0xF)) > 0xF) registers.f |= 0x20;  // H
        if (result > 0xFF) registers.f |= 0x10;  // C
        registers.a = result & 0xFF;
    }
    public void ADDHLBC() { addHL(registers.getBC()); registers.m = 2; registers.t = 8; }
    public void ADDHLDE() { addHL(registers.getDE()); registers.m = 2; registers.t = 8; }
    public void ADDHLHL() { addHL(registers.getHL()); registers.m = 2; registers.t = 8; }
    public void ADDHLSP() { addHL(registers.sp); registers.m = 2; registers.t = 8; }

    private void addHL(int val) {
        int hl = registers.getHL();
        int result = hl + val;
        registers.f &= 0x80;  // Keep Z, clear N/H/C
        if (((hl & 0xFFF) + (val & 0xFFF)) > 0xFFF) registers.f |= 0x20;  // H (carry from bit 11)
        if (result > 0xFFFF) registers.f |= 0x10;  // C
        registers.setHL(result & 0xFFFF);
    }

    public void ADDSPn() {
        int e = (byte) mmu.readByte(registers.pc);
        registers.pc++;
        int sp = registers.sp;
        registers.sp = (sp + e) & 0xFFFF;
        registers.f = 0;
        int lowSum = (sp & 0xFF) + (e & 0xFF);
        if ((lowSum & 0x100) != 0) registers.f |= 0x10;
        if (((sp & 0xF) + (e & 0xF)) > 0xF) registers.f |= 0x20;
        registers.m = 4;
        registers.t = 16;
    }

    public void ADCr_b() { adcA(registers.b); registers.m = 1; registers.t = 4; }
    public void ADCr_c() { adcA(registers.c); registers.m = 1; registers.t = 4; }
    public void ADCr_d() { adcA(registers.d); registers.m = 1; registers.t = 4; }
    public void ADCr_e() { adcA(registers.e); registers.m = 1; registers.t = 4; }
    public void ADCr_h() { adcA(registers.h); registers.m = 1; registers.t = 4; }
    public void ADCr_l() { adcA(registers.l); registers.m = 1; registers.t = 4; }
    public void ADCr_a() { adcA(registers.a); registers.m = 1; registers.t = 4; }
    public void ADCHL() { adcA(mmu.readByte(registers.getHL())); registers.m = 2; registers.t = 8; }
    public void ADCn() { adcA(mmu.readByte(registers.pc)); registers.pc++; registers.m = 2; registers.t = 8; }

    private void adcA(int val) {
        int a = registers.a;
        int carry = (registers.f & 0x10) != 0 ? 1 : 0;
        int result = a + val + carry;
        registers.f = 0;
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
        if (((a & 0xF) + (val & 0xF) + carry) > 0xF) registers.f |= 0x20;  // H
        if (result > 0xFF) registers.f |= 0x10;  // C
        registers.a = result & 0xFF;
    }

    public void SUBr_b() { subA(registers.b); registers.m = 1; registers.t = 4; }
    public void SUBr_c() { subA(registers.c); registers.m = 1; registers.t = 4; }
    public void SUBr_d() { subA(registers.d); registers.m = 1; registers.t = 4; }
    public void SUBr_e() { subA(registers.e); registers.m = 1; registers.t = 4; }
    public void SUBr_h() { subA(registers.h); registers.m = 1; registers.t = 4; }
    public void SUBr_l() { subA(registers.l); registers.m = 1; registers.t = 4; }
    public void SUBr_a() { subA(registers.a); registers.m = 1; registers.t = 4; }
    public void SUBHL() { subA(mmu.readByte(registers.getHL())); registers.m = 2; registers.t = 8; }
    public void SUBn() { subA(mmu.readByte(registers.pc)); registers.pc++; registers.m = 2; registers.t = 8; }

    private void subA(int val) {
        int a = registers.a;
        int result = a - val;
        registers.f = 0x40;  // N always set
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
        if ((val & 0xF) > (a & 0xF)) registers.f |= 0x20;  // H
        if (val > a) registers.f |= 0x10;  // C
        registers.a = result & 0xFF;
    }

    public void SBCr_b() { sbcA(registers.b); registers.m = 1; registers.t = 4; }
    public void SBCr_c() { sbcA(registers.c); registers.m = 1; registers.t = 4; }
    public void SBCr_d() { sbcA(registers.d); registers.m = 1; registers.t = 4; }
    public void SBCr_e() { sbcA(registers.e); registers.m = 1; registers.t = 4; }
    public void SBCr_h() { sbcA(registers.h); registers.m = 1; registers.t = 4; }
    public void SBCr_l() { sbcA(registers.l); registers.m = 1; registers.t = 4; }
    public void SBCr_a() { sbcA(registers.a); registers.m = 1; registers.t = 4; }
    public void SBCHL() { sbcA(mmu.readByte(registers.getHL())); registers.m = 2; registers.t = 8; }
    public void SBCn() { sbcA(mmu.readByte(registers.pc)); registers.pc++; registers.m = 2; registers.t = 8; }

    private void sbcA(int val) {
        int a = registers.a;
        int carry = (registers.f & 0x10) != 0 ? 1 : 0;
        int result = a - val - carry;
        registers.f = 0x40;  // N always set
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
        if ((val & 0xF) + carry > (a & 0xF)) registers.f |= 0x20;  // H
        if (result < 0) registers.f |= 0x10;  // C
        registers.a = result & 0xFF;
    }

    public void CPr_b() { cpA(registers.b); registers.m = 1; registers.t = 4; }
    public void CPr_c() { cpA(registers.c); registers.m = 1; registers.t = 4; }
    public void CPr_d() { cpA(registers.d); registers.m = 1; registers.t = 4; }
    public void CPr_e() { cpA(registers.e); registers.m = 1; registers.t = 4; }
    public void CPr_h() { cpA(registers.h); registers.m = 1; registers.t = 4; }
    public void CPr_l() { cpA(registers.l); registers.m = 1; registers.t = 4; }
    public void CPr_a() { cpA(registers.a); registers.m = 1; registers.t = 4; }
    public void CPHL() { cpA(mmu.readByte(registers.getHL())); registers.m = 2; registers.t = 8; }

    private void cpA(int val) {
        int a = registers.a;
        int result = a - val;
        registers.f = 0x40;  // N always set
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
        if ((val & 0xF) > (a & 0xF)) registers.f |= 0x20;  // H
        if (val > a) registers.f |= 0x10;  // C
    }

    public void CPn() {
        int val = mmu.readByte(registers.pc++);
        int result = (registers.a - val) & 0xFF;

        registers.f = 0;
        if (result == 0) registers.f |= 0x80; // Z
        registers.f |= 0x40; // N is always set
        if ((val & 0x0F) > (registers.a & 0x0F)) registers.f |= 0x20; // H
        if (val > registers.a) registers.f |= 0x10; // C

        registers.m = 2;
        registers.t = 8;
    }


    public void ANDr_b() { registers.a &= registers.b; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDr_c() { registers.a &= registers.c; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDr_d() { registers.a &= registers.d; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDr_e() { registers.a &= registers.e; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDr_h() { registers.a &= registers.h; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDr_l() { registers.a &= registers.l; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDr_a() { registers.a &= registers.a; fzAND(registers.a); registers.m = 1; registers.t = 4; }
    public void ANDHL() { registers.a &= mmu.readByte(registers.getHL()); fzAND(registers.a); registers.m = 2; registers.t = 8; }
    public void ANDn() { registers.a &= mmu.readByte(registers.pc); registers.pc++; fzAND(registers.a); registers.m = 2; registers.t = 8; }


    public void ORr_b() { registers.a |= registers.b; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORr_c() { registers.a |= registers.c; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORr_d() { registers.a |= registers.d; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORr_e() { registers.a |= registers.e; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORr_h() { registers.a |= registers.h; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORr_l() { registers.a |= registers.l; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORr_a() { registers.a |= registers.a; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void ORHL() { registers.a |= mmu.readByte(registers.getHL()); fzOR(registers.a); registers.m = 2; registers.t = 8; }
    public void ORn() { registers.a |= mmu.readByte(registers.pc); registers.pc++; fzOR(registers.a); registers.m = 2; registers.t = 8; }

    public void XORr_b() { registers.a ^= registers.b; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORr_c() { registers.a ^= registers.c; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORr_d() { registers.a ^= registers.d; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORr_e() { registers.a ^= registers.e; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORr_h() { registers.a ^= registers.h; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORr_l() { registers.a ^= registers.l; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORr_a() { registers.a ^= registers.a; fzOR(registers.a); registers.m = 1; registers.t = 4; }
    public void XORHL() { registers.a ^= mmu.readByte(registers.getHL()); fzOR(registers.a); registers.m = 2; registers.t = 8; }
    public void XORn() { registers.a ^= mmu.readByte(registers.pc); registers.pc++; fzOR(registers.a); registers.m = 2; registers.t = 8; }

    public void INCr_b() { registers.b = inc8(registers.b); registers.m = 1; registers.t = 4; }
    public void INCr_c() { registers.c = inc8(registers.c); registers.m = 1; registers.t = 4; }
    public void INCr_d() { registers.d = inc8(registers.d); registers.m = 1; registers.t = 4; }
    public void INCr_e() { registers.e = inc8(registers.e); registers.m = 1; registers.t = 4; }
    public void INCr_h() { registers.h = inc8(registers.h); registers.m = 1; registers.t = 4; }
    public void INCr_l() { registers.l = inc8(registers.l); registers.m = 1; registers.t = 4; }
    public void INCr_a() { registers.a = inc8(registers.a); registers.m = 1; registers.t = 4; }
    public void INCHLm() { int addr = registers.getHL(); mmu.writeByte(addr, inc8(mmu.readByte(addr))); registers.m = 3; registers.t = 12; }

    private int inc8(int val) {
        int result = (val + 1) & 0xFF;
        registers.f &= 0x10;  // Keep only C flag
        if (result == 0) registers.f |= 0x80;  // Z
        if ((val & 0xF) == 0xF) registers.f |= 0x20;  // H (overflow from bit 3)
        // N is cleared (already done by the mask)
        return result;
    }

    public void DECr_b() { registers.b = dec8(registers.b); registers.m = 1; registers.t = 4; }
    public void DECr_c() { registers.c = dec8(registers.c); registers.m = 1; registers.t = 4; }
    public void DECr_d() { registers.d = dec8(registers.d); registers.m = 1; registers.t = 4; }
    public void DECr_e() { registers.e = dec8(registers.e); registers.m = 1; registers.t = 4; }
    public void DECr_h() { registers.h = dec8(registers.h); registers.m = 1; registers.t = 4; }
    public void DECr_l() { registers.l = dec8(registers.l); registers.m = 1; registers.t = 4; }
    public void DECr_a() { registers.a = dec8(registers.a); registers.m = 1; registers.t = 4; }
    public void DECHLm() { int addr = registers.getHL(); mmu.writeByte(addr, dec8(mmu.readByte(addr))); registers.m = 3; registers.t = 12; }

    private int dec8(int val) {
        int result = (val - 1) & 0xFF;
        registers.f &= 0x10;  // Keep only C flag
        if (result == 0) registers.f |= 0x80;  // Z
        registers.f |= 0x40;  // N always set
        if ((val & 0xF) == 0x0) registers.f |= 0x20;  // H (borrow from bit 4)
        return result;
    }

    public void INCBC() {int bc = (registers.getBC()+1)&0xFFFF; registers.setBC(bc); registers.m = 2; registers.t = 8;}
    public void INCDE() {int de = (registers.getDE()+1)&0xFFFF; registers.setDE(de); registers.m = 2; registers.t = 8;}
    public void INCHL() {int hl = (registers.getHL()+1)&0xFFFF; registers.setHL(hl); registers.m = 2; registers.t = 8;}
    public void INCSP() {registers.sp=(registers.sp+1)&0xFFFF; registers.m = 2; registers.t = 8;}


    public void DECBC() {int bc = (registers.getBC()-1)&0xFFFF; registers.setBC(bc); registers.m = 2; registers.t = 8;}
    public void DECDE() {int de = (registers.getDE()-1)&0xFFFF; registers.setDE(de); registers.m = 2; registers.t = 8;}
    public void DECHL() {int hl = (registers.getHL()-1)&0xFFFF; registers.setHL(hl); registers.m = 2; registers.t = 8;}
    public void DECSP() {registers.sp=(registers.sp-1)&0xFFFF; registers.m = 2; registers.t = 8;}

    /*--- Bit manipulation ---*/

    public void BIT0b(){fzBIT(registers.b&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0c(){fzBIT(registers.c&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0d(){fzBIT(registers.d&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0e(){fzBIT(registers.e&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0h(){fzBIT(registers.h&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0l(){fzBIT(registers.l&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0a(){fzBIT(registers.a&0x1); registers.m = 2; registers.t = 8; }
    public void BIT0m(){fzBIT(mmu.readByte(registers.getHL())&0x1); registers.m = 3; registers.t = 12; }

    public void BIT1b(){fzBIT(registers.b&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1c(){fzBIT(registers.c&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1d(){fzBIT(registers.d&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1e(){fzBIT(registers.e&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1h(){fzBIT(registers.h&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1l(){fzBIT(registers.l&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1a(){fzBIT(registers.a&0x2); registers.m = 2; registers.t = 8; }
    public void BIT1m(){fzBIT(mmu.readByte(registers.getHL())&0x2); registers.m = 3; registers.t = 12; }

    public void BIT2b(){fzBIT(registers.b&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2c(){fzBIT(registers.c&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2d(){fzBIT(registers.d&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2e(){fzBIT(registers.e&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2h(){fzBIT(registers.h&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2l(){fzBIT(registers.l&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2a(){fzBIT(registers.a&0x4); registers.m = 2; registers.t = 8; }
    public void BIT2m(){fzBIT(mmu.readByte(registers.getHL())&0x4); registers.m = 3; registers.t = 12; }

    public void BIT3b(){fzBIT(registers.b&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3c(){fzBIT(registers.c&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3d(){fzBIT(registers.d&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3e(){fzBIT(registers.e&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3h(){fzBIT(registers.h&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3l(){fzBIT(registers.l&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3a(){fzBIT(registers.a&0x8); registers.m = 2; registers.t = 8; }
    public void BIT3m(){fzBIT(mmu.readByte(registers.getHL())&0x8); registers.m = 3; registers.t = 12; }

    public void BIT4b(){fzBIT(registers.b&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4c(){fzBIT(registers.c&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4d(){fzBIT(registers.d&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4e(){fzBIT(registers.e&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4h(){fzBIT(registers.h&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4l(){fzBIT(registers.l&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4a(){fzBIT(registers.a&0x10); registers.m = 2; registers.t = 8; }
    public void BIT4m(){fzBIT(mmu.readByte(registers.getHL())&0x10); registers.m = 3; registers.t = 12; }

    public void BIT5b(){fzBIT(registers.b&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5c(){fzBIT(registers.c&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5d(){fzBIT(registers.d&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5e(){fzBIT(registers.e&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5h(){fzBIT(registers.h&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5l(){fzBIT(registers.l&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5a(){fzBIT(registers.a&0x20); registers.m = 2; registers.t = 8; }
    public void BIT5m(){fzBIT(mmu.readByte(registers.getHL())&0x20); registers.m = 3; registers.t = 12; }

    public void BIT6b(){fzBIT(registers.b&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6c(){fzBIT(registers.c&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6d(){fzBIT(registers.d&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6e(){fzBIT(registers.e&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6h(){fzBIT(registers.h&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6l(){fzBIT(registers.l&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6a(){fzBIT(registers.a&0x40); registers.m = 2; registers.t = 8; }
    public void BIT6m(){fzBIT(mmu.readByte(registers.getHL())&0x40); registers.m = 3; registers.t = 12; }

    public void BIT7b(){fzBIT(registers.b&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7c(){fzBIT(registers.c&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7d(){fzBIT(registers.d&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7e(){fzBIT(registers.e&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7h(){fzBIT(registers.h&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7l(){fzBIT(registers.l&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7a(){fzBIT(registers.a&0x80); registers.m = 2; registers.t = 8; }
    public void BIT7m(){fzBIT(mmu.readByte(registers.getHL())&0x80); registers.m = 3; registers.t = 12; }

    public void RLA() {
        int cin = (registers.f & 0x10) != 0 ? 1 : 0;
        int cout = (registers.a & 0x80) != 0 ? 0x10 : 0;
        registers.a = ((registers.a << 1) | cin) & 0xFF;
        registers.f = cout;  // Z=0, N=0, H=0, C=old bit 7
        registers.m = 1;
        registers.t = 4;
    }

    public void RLCA() {
        int bit7 = (registers.a & 0x80) != 0 ? 1 : 0;
        registers.a = ((registers.a << 1) | bit7) & 0xFF;
        registers.f = bit7 == 1 ? 0x10 : 0;  // Z=0, N=0, H=0, C=old bit 7
        registers.m = 1;
        registers.t = 4;
    }

    public void RRA() {
        int cin = (registers.f & 0x10) != 0 ? 0x80 : 0;
        int cout = (registers.a & 0x01) != 0 ? 0x10 : 0;
        registers.a = ((registers.a >> 1) | cin) & 0xFF;
        registers.f = cout;  // Z=0, N=0, H=0, C=old bit 0
        registers.m = 1;
        registers.t = 4;
    }

    public void RRCA() {
        int bit0 = (registers.a & 0x01) != 0 ? 1 : 0;
        registers.a = ((registers.a >> 1) | (bit0 << 7)) & 0xFF;
        registers.f = bit0 == 1 ? 0x10 : 0;  // Z=0, N=0, H=0, C=old bit 0
        registers.m = 1;
        registers.t = 4;
    }

    public void RLr_b() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.b & 0x80) != 0 ? 0x10 : 0;registers.b = ((registers.b << 1) | carryIn) & 0xFF;fz(registers.b);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLr_c() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.c & 0x80) != 0 ? 0x10 : 0;registers.c = ((registers.c << 1) | carryIn) & 0xFF;fz(registers.c);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLr_d() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.d & 0x80) != 0 ? 0x10 : 0;registers.d = ((registers.d << 1) | carryIn) & 0xFF;fz(registers.d);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLr_e() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.e & 0x80) != 0 ? 0x10 : 0;registers.e = ((registers.e << 1) | carryIn) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLr_h() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.h & 0x80) != 0 ? 0x10 : 0;registers.h = ((registers.h << 1) | carryIn) & 0xFF;fz(registers.h);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLr_l() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.l & 0x80) != 0 ? 0x10 : 0;registers.l = ((registers.l << 1) | carryIn) & 0xFF;fz(registers.l);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLr_a() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int carryOut = (registers.a & 0x80) != 0 ? 0x10 : 0;registers.a = ((registers.a << 1) | carryIn) & 0xFF;fz(registers.a);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLHL() {int carryIn = (registers.f & 0x10) != 0 ? 1 : 0;int i = mmu.readByte(registers.getHL());int carryOut = ((i & 0x80) != 0 ? 0x10 : 0);i = ((i << 1) | carryIn) & 0xFF;fz(i);registers.f = (registers.f & 0xEF) | carryOut;mmu.writeByte(registers.getHL(), i);registers.m = 4;registers.t = 16;}

    public void RLCr_b() {int carryIn = (registers.b & 0x80) != 0 ? 1 : 0;int carryOut = (registers.b & 0x80) != 0 ? 0x10 : 0;registers.b = ((registers.b << 1) | carryIn) & 0xFF;fz(registers.b);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCr_c() {int carryIn = (registers.c & 0x80) != 0 ? 1 : 0;int carryOut = (registers.c & 0x80) != 0 ? 0x10 : 0;registers.c = ((registers.c << 1) | carryIn) & 0xFF;fz(registers.c);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCr_d() {int carryIn = (registers.d & 0x80) != 0 ? 1 : 0;int carryOut = (registers.d & 0x80) != 0 ? 0x10 : 0;registers.d = ((registers.d << 1) | carryIn) & 0xFF;fz(registers.d);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCr_e() {int carryIn = (registers.e & 0x80) != 0 ? 1 : 0;int carryOut = (registers.e & 0x80) != 0 ? 0x10 : 0;registers.e = ((registers.e << 1) | carryIn) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCr_h() {int carryIn = (registers.h & 0x80) != 0 ? 1 : 0;int carryOut = (registers.h & 0x80) != 0 ? 0x10 : 0;registers.h = ((registers.h << 1) | carryIn) & 0xFF;fz(registers.h);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCr_l() {int carryIn = (registers.l & 0x80) != 0 ? 1 : 0;int carryOut = (registers.l & 0x80) != 0 ? 0x10 : 0;registers.l = ((registers.l << 1) | carryIn) & 0xFF;fz(registers.l);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCr_a() {int carryIn = (registers.a & 0x80) != 0 ? 1 : 0;int carryOut = (registers.a & 0x80) != 0 ? 0x10 : 0;registers.a = ((registers.a << 1) | carryIn) & 0xFF;fz(registers.a);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RLCHL() {int i = mmu.readByte(registers.getHL());int carryIn = (i & 0x80) != 0 ? 1 : 0;int carryOut = ((i & 0x80) != 0 ? 0x10 : 0);i = ((i << 1) | carryIn) & 0xFF;fz(i);registers.f = (registers.f & 0xEF) | carryOut;mmu.writeByte(registers.getHL(), i);registers.m = 4;registers.t = 16;}

    public void RRr_b() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.b & 1) != 0 ? 0x10 : 0;registers.b = ((registers.b >> 1) | carryIn) & 0xFF;fz(registers.b);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRr_c() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.c & 1) != 0 ? 0x10 : 0;registers.c = ((registers.c >> 1) | carryIn) & 0xFF;fz(registers.c);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRr_d() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.d & 1) != 0 ? 0x10 : 0;registers.d = ((registers.d >> 1) | carryIn) & 0xFF;fz(registers.d);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRr_e() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.e & 1) != 0 ? 0x10 : 0;registers.e = ((registers.e >> 1) | carryIn) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRr_h() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.h & 1) != 0 ? 0x10 : 0;registers.h = ((registers.h >> 1) | carryIn) & 0xFF;fz(registers.h);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRr_l() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.l & 1) != 0 ? 0x10 : 0;registers.l = ((registers.l >> 1) | carryIn) & 0xFF;fz(registers.l);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRr_a() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int carryOut = (registers.a & 1) != 0 ? 0x10 : 0;registers.a = ((registers.a >> 1) | carryIn) & 0xFF;fz(registers.a);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRHL() {int carryIn = (registers.f & 0x10) != 0 ? 0x80 : 0;int i = mmu.readByte(registers.getHL());int carryOut = ((i & 1) != 0 ? 0x10 : 0);i = ((i >> 1) | carryIn) & 0xFF;fz(i);registers.f = (registers.f & 0xEF) | carryOut;mmu.writeByte(registers.getHL(), i);registers.m = 4;registers.t = 16;}

    public void RRCr_b() {int bit0 = registers.b & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.b = ((registers.b >> 1) | (bit0 << 7)) & 0xFF;fz(registers.b);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCr_c() {int bit0 = registers.c & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.c = ((registers.c >> 1) | (bit0 << 7)) & 0xFF;fz(registers.c);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCr_d() {int bit0 = registers.d & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.d = ((registers.d >> 1) | (bit0 << 7)) & 0xFF;fz(registers.d);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCr_e() {int bit0 = registers.e & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.e = ((registers.e >> 1) | (bit0 << 7)) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCr_h() {int bit0 = registers.h & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.h = ((registers.h >> 1) | (bit0 << 7)) & 0xFF;fz(registers.h);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCr_l() {int bit0 = registers.l & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.l = ((registers.l >> 1) | (bit0 << 7)) & 0xFF;fz(registers.l);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCr_a() {int bit0 = registers.a & 0x01;int carryOut = bit0 != 0 ? 0x10 : 0;registers.a = ((registers.a >> 1) | (bit0 << 7)) & 0xFF;fz(registers.a);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void RRCHL() { int i = mmu.readByte(registers.getHL()); int bit0 = i & 0x01; int carryOut = bit0 != 0 ? 0x10 : 0; i = ((i >> 1) | (bit0 << 7)) & 0xFF; fz(i); registers.f = (registers.f & 0xEF) | carryOut; mmu.writeByte(registers.getHL(), i); registers.m = 4; registers.t = 16; }

    public void SLAr_b() { int carryOut = ((registers.b&0x80) != 0) ? 0x10 : 0; registers.b = (registers.b<<1)&0xFF; fz(registers.b); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SLAr_c() { int carryOut = ((registers.c&0x80) != 0) ? 0x10 : 0; registers.c = (registers.c<<1)&0xFF; fz(registers.c); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SLAr_d() { int carryOut = ((registers.d&0x80) != 0) ? 0x10 : 0; registers.d = (registers.d<<1)&0xFF; fz(registers.d); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SLAr_e() {int carryOut = ((registers.e & 0x80) != 0) ? 0x10 : 0;registers.e = (registers.e << 1) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void SLAr_h() { int carryOut = ((registers.h&0x80) != 0) ? 0x10 : 0; registers.h = (registers.h<<1)&0xFF; fz(registers.h); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SLAr_l() { int carryOut = ((registers.l&0x80) != 0) ? 0x10 : 0; registers.l = (registers.l<<1)&0xFF; fz(registers.l); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SLAr_a() { int carryOut = ((registers.a&0x80) != 0) ? 0x10 : 0; registers.a = (registers.a<<1)&0xFF; fz(registers.a); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}

    public void SRAr_b() { int carryIn = registers.b&0x80; int carryOut= (registers.b&1) != 0 ? 0x10 : 0; registers.b = ((registers.b>>1) | carryIn)&0xFF; fz(registers.b); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRAr_c() { int carryIn = registers.c&0x80; int carryOut= (registers.c&1) != 0 ? 0x10 : 0; registers.c = ((registers.c>>1) | carryIn)&0xFF; fz(registers.c); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRAr_d() { int carryIn = registers.d&0x80; int carryOut= (registers.d&1) != 0 ? 0x10 : 0; registers.d = ((registers.d>>1) | carryIn)&0xFF; fz(registers.d); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRAr_e() {int carryIn = registers.e & 0x80;int carryOut = (registers.e & 1) != 0 ? 0x10 : 0;registers.e = ((registers.e >> 1) | carryIn) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void SRAr_h() { int carryIn = registers.h&0x80; int carryOut= (registers.h&1) != 0 ? 0x10 : 0; registers.h = ((registers.h>>1) | carryIn)&0xFF; fz(registers.h); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRAr_l() { int carryIn = registers.l&0x80; int carryOut= (registers.l&1) != 0 ? 0x10 : 0; registers.l = ((registers.l>>1) | carryIn)&0xFF; fz(registers.l); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRAr_a() { int carryIn = registers.a&0x80; int carryOut= (registers.a&1) != 0 ? 0x10 : 0; registers.a = ((registers.a>>1) | carryIn)&0xFF; fz(registers.a); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}

    public void SRLr_b() { int carryOut = ((registers.b&1) != 0) ? 0x10 : 0; registers.b = (registers.b>>1)&0xFF; fz(registers.b); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRLr_c() { int carryOut = ((registers.c&1) != 0) ? 0x10 : 0; registers.c = (registers.c>>1)&0xFF; fz(registers.c); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRLr_d() { int carryOut = ((registers.d&1) != 0) ? 0x10 : 0; registers.d = (registers.d>>1)&0xFF; fz(registers.d); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRLr_e() {int carryOut = ((registers.e & 1) != 0) ? 0x10 : 0;registers.e = (registers.e >> 1) & 0xFF;fz(registers.e);registers.f = (registers.f & 0xEF) | carryOut;registers.m = 2;registers.t = 8;}
    public void SRLr_h() { int carryOut = ((registers.h&1) != 0) ? 0x10 : 0; registers.h = (registers.h>>1)&0xFF; fz(registers.h); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRLr_l() { int carryOut = ((registers.l&1) != 0) ? 0x10 : 0; registers.l = (registers.l>>1)&0xFF; fz(registers.l); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}
    public void SRLr_a() { int carryOut = ((registers.a&1) != 0) ? 0x10 : 0; registers.a = (registers.a>>1)&0xFF; fz(registers.a); registers.f = (registers.f & 0xEF) | carryOut; registers.m = 2; registers.t = 8;}

    public void SLAHL() { int i = mmu.readByte(registers.getHL()); int carryOut = (i & 0x80) != 0 ? 0x10 : 0; i = (i << 1) & 0xFF; fz(i); registers.f = (registers.f & 0xEF) | carryOut; mmu.writeByte(registers.getHL(), i); registers.m = 4; registers.t = 16; }
    public void SRAHL() { int i = mmu.readByte(registers.getHL()); int carryIn = i & 0x80; int carryOut = (i & 1) != 0 ? 0x10 : 0; i = ((i >> 1) | carryIn) & 0xFF; fz(i); registers.f = (registers.f & 0xEF) | carryOut; mmu.writeByte(registers.getHL(), i); registers.m = 4; registers.t = 16; }
    public void SRLHL() { int i = mmu.readByte(registers.getHL()); int carryOut = (i & 1) != 0 ? 0x10 : 0; i = (i >> 1) & 0xFF; fz(i); registers.f = (registers.f & 0xEF) | carryOut; mmu.writeByte(registers.getHL(), i); registers.m = 4; registers.t = 16; }

    public void CPL() {
        registers.a = (~registers.a) & 0xFF;
        registers.f |= 0x60;  // Set N and H, leave Z and C unchanged
        registers.m = 1;
        registers.t = 4;
    }
    public void CCF() {
        int c = ((registers.f & 0x10) != 0) ? 0 : 0x10;  // Invert C
        registers.f = (registers.f & 0x80) | c;  // Keep Z, clear N/H
        registers.m = 1;
        registers.t = 4;
    }
    public void SCF() {
        registers.f = (registers.f & 0x80) | 0x10;  // Keep Z, clear N/H, set C
        registers.m = 1;
        registers.t = 4;
    }

    /*--- Stack ---*/
    public void PUSHBC() { registers.sp--; mmu.writeByte(registers.sp,registers.b); registers.sp--; mmu.writeByte(registers.sp,registers.c); registers.m=4; registers.t=16; }
    public void PUSHDE() { registers.sp--; mmu.writeByte(registers.sp,registers.d); registers.sp--; mmu.writeByte(registers.sp,registers.e); registers.m=4; registers.t=16; }
    public void PUSHHL() { registers.sp--; mmu.writeByte(registers.sp,registers.h); registers.sp--; mmu.writeByte(registers.sp,registers.l); registers.m=4; registers.t=16; }
    public void PUSHAF() { registers.sp--; mmu.writeByte(registers.sp,registers.a); registers.sp--; mmu.writeByte(registers.sp,registers.f); registers.m=4; registers.t=16; }

    public void POPBC() { registers.c=mmu.readByte(registers.sp); registers.sp++; registers.b=mmu.readByte(registers.sp); registers.sp++; registers.m=3; registers.t=12; }
    public void POPDE() { registers.e=mmu.readByte(registers.sp); registers.sp++; registers.d=mmu.readByte(registers.sp); registers.sp++; registers.m=3; registers.t=12; }
    public void POPHL() { registers.l=mmu.readByte(registers.sp); registers.sp++; registers.h=mmu.readByte(registers.sp); registers.sp++; registers.m=3; registers.t=12; }
    public void POPAF() {
        registers.f = mmu.readByte(registers.sp) & 0xF0;  // Mask lower 4 bits (always 0 on real hardware)
        registers.sp++;
        registers.a = mmu.readByte(registers.sp);
        registers.sp++;
        registers.m = 3;
        registers.t = 12;
    }

    /*--- Jump ---*/
    public void JPnn() { registers.pc = mmu.readWord(registers.pc); registers.m=4; registers.t = 16; }
    public void JPHL() {registers.pc = registers.getHL(); registers.m = 1; registers.t = 4; }
    public void JPNZnn() { registers.m = 3; registers.t = 12; if((registers.f&0x80)==0x00) { registers.pc=mmu.readWord(registers.pc); registers.m++; registers.t+=4; } else {registers.pc+=2;}}
    public void JPZnn() { registers.m = 3; registers.t = 12; if((registers.f&0x80)==0x80) { registers.pc=mmu.readWord(registers.pc); registers.m++; registers.t+=4;} else {registers.pc+=2;}}
    public void JPNCnn() { registers.m = 3; registers.t = 12; if((registers.f&0x10)==0x00) { registers.pc=mmu.readWord(registers.pc); registers.m++; registers.t+=4;} else {registers.pc+=2;}}
    public void JPCnn() { registers.m = 3; registers.t = 12; if((registers.f&0x10)==0x10) { registers.pc=mmu.readWord(registers.pc); registers.m++; registers.t+=4;} else {registers.pc+=2;}}

    public void JRn() { int i = mmu.readByte(registers.pc); if(i>127) {i=-((~i+1)&0xFF);} registers.pc++; registers.m=2; registers.t=8; registers.pc+=i; registers.m++; registers.t+=4;}
    public void JRNZn() { int i = mmu.readByte(registers.pc); if(i>127) {i=-((~i+1)&0xFF);} registers.pc++; registers.m=2; registers.t=8; if((registers.f&0x80)==0x00) {registers.pc+=i; registers.m++; registers.t+=4;}}
    public void JRZn() { int i = mmu.readByte(registers.pc); if(i>127) {i=-((~i+1)&0xFF);} registers.pc++; registers.m=2; registers.t=8; if((registers.f&0x80)==0x80) {registers.pc+=i; registers.m++; registers.t+=4;}}
    public void JRNCn() { int i = mmu.readByte(registers.pc); if(i>127) {i=-((~i+1)&0xFF);} registers.pc++; registers.m=2; registers.t=8; if((registers.f&0x10)==0x00) {registers.pc+=i; registers.m++; registers.t+=4;}}
    public void JRCn() { int i = mmu.readByte(registers.pc); if(i>127) {i=-((~i+1)&0xFF);} registers.pc++; registers.m=2; registers.t=8; if((registers.f&0x10)==0x10) {registers.pc+=i; registers.m++; registers.t+=4;}}

    public void STOP() { registers.pc++; registers.m = 2; registers.t = 8; }
    public void LDSPHL() { registers.sp = registers.getHL(); registers.m = 2; registers.t = 8; }

    public void CALLnn() { registers.sp-=2; mmu.writeWord(registers.sp,registers.pc+2); registers.pc=mmu.readWord(registers.pc); registers.m = 6; registers.t = 24; }
    public void CALLNZnn() { registers.m=3; registers.t = 12; if((registers.f&0x80)==0x00) { registers.sp -=2; mmu.writeWord(registers.sp,registers.pc+2); registers.pc=mmu.readWord(registers.pc); registers.m+=3; registers.t+=12;} else registers.pc +=2;}

    public void CALLZnn() { registers.m=3; registers.t = 12; if((registers.f&0x80)==0x80) { registers.sp -=2; mmu.writeWord(registers.sp,registers.pc+2); registers.pc=mmu.readWord(registers.pc); registers.m+=3; registers.t+=12;} else registers.pc +=2;}
    public void CALLNCnn() { registers.m=3; registers.t = 12; if((registers.f&0x10)==0x00) { registers.sp -=2; mmu.writeWord(registers.sp,registers.pc+2); registers.pc=mmu.readWord(registers.pc); registers.m+=3; registers.t+=12;} else registers.pc +=2;}

    public void CALLCnn() { registers.m=3; registers.t = 12; if((registers.f&0x10)==0x10) { registers.sp -=2; mmu.writeWord(registers.sp,registers.pc+2); registers.pc=mmu.readWord(registers.pc); registers.m+=3; registers.t+=12;} else registers.pc +=2;}

    public void CALLMnn() { registers.m = 3;registers.t = 12;if ((registers.f & 0x80) != 0) {registers.sp = (registers.sp - 2) & 0xFFFF;mmu.writeWord(registers.sp, registers.pc + 2);registers.pc = mmu.readWord(registers.pc);registers.m += 3;registers.t += 12;} else {registers.pc += 2;}}


    public void RET() { registers.pc = mmu.readWord(registers.sp); registers.sp+=2; registers.m = 4; registers.t = 16;}
    public void RETI() {registers.ime = 1; registers.pc=mmu.readWord(registers.sp); registers.sp+=2; registers.m=4; registers.t=16;}
    public void RETNZ() {registers.m = 2; registers.t = 8; if((registers.f&0x80)==0x00) { registers.pc=mmu.readWord(registers.sp); registers.sp+=2; registers.m+=3; registers.t+=12;}}
    public void RETZ() {registers.m = 2; registers.t = 8; if((registers.f&0x80)==0x80) { registers.pc=mmu.readWord(registers.sp); registers.sp+=2; registers.m+=3; registers.t+=12;}}
    public void RETNC() {registers.m = 2; registers.t = 8; if((registers.f&0x10)==0x00) { registers.pc=mmu.readWord(registers.sp); registers.sp+=2; registers.m+=3; registers.t+=12;}}
    public void RETC() {registers.m = 2; registers.t = 8; if((registers.f&0x10)==0x10) { registers.pc=mmu.readWord(registers.sp); registers.sp+=2; registers.m+=3; registers.t+=12;}}

    public void RST00() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x00; registers.m=4; registers.t=16; }
    public void RST08() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x08; registers.m=4; registers.t=16; }
    public void RST10() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x10; registers.m=4; registers.t=16; }
    public void RST18() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x18; registers.m=4; registers.t=16; }
    public void RST20() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x20; registers.m=4; registers.t=16; }
    public void RST28() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x28; registers.m=4; registers.t=16; }
    public void RST30() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x30; registers.m=4; registers.t=16; }
    public void RST38() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x38; registers.m=4; registers.t=16; }
    public void RST40() { registers.ime = 0; registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x40; registers.m=4; registers.t=16; }
    public void RST48() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x48; registers.m=4; registers.t=16; }
    public void RST50() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x50; registers.m=4; registers.t=16; }
    public void RST58() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x58; registers.m=4; registers.t=16; }
    public void RST60() {registers.sp-=2; mmu.writeWord(registers.sp,registers.pc); registers.pc=0x60; registers.m=4; registers.t=16; }


    public void NOP() {registers.m = 1; registers.t = 4;}
    public void HALT() { registers.m = 1; registers.t = 4; }

    public void DI() {registers.ime = 0; registers.m =1; registers.t = 4;}
    public void EI() { enableIMEAfterNextInstr = true; registers.m = 1; registers.t = 4;}


    public void DAA() {
        int a = registers.a;
        boolean n = (registers.f & 0x40) != 0; // N flag
        boolean h = (registers.f & 0x20) != 0; // H flag
        boolean c = (registers.f & 0x10) != 0; // C flag

        int correction = 0;
        boolean setC = c;  // Preserve C flag for subtraction case

        if (!n) {
            // After addition
            if (h || (a & 0x0F) > 9) correction |= 0x06;
            if (c || a > 0x99) {
                correction |= 0x60;
                setC = true;
            }
            a = (a + correction) & 0xFF;
        } else {
            // After subtraction
            if (h) correction |= 0x06;
            if (c) correction |= 0x60;
            a = (a - correction) & 0xFF;
            // C flag is preserved (already set in setC = c)
        }

        registers.a = a;

        registers.f &= 0x40; // keep N
        if (a == 0) registers.f |= 0x80; // Z
        if (setC) registers.f |= 0x10;   // C
        // H is always cleared
        registers.m = 1;
        registers.t = 4;
    }




    //Helper Functions
    // For CB-prefixed rotate/shift instructions: Z based on result, N=0, H=0, C set by caller
    public void fz(int i, int as) {
        registers.f &= 0x10;  // Keep only C flag, clear Z, N, H
        if ((i & 0xFF) == 0) {
            registers.f |= 0x80; // Set Z
        }
        if (as != 0) {
            registers.f |= 0x40; // Set N (subtract)
        }
    }


    public void fz(int i) {
        fz(i, 0);
    }

    // Special flag handling for BIT instructions
    public void fzBIT(int i) {
        registers.f &= 0x10;  // Keep only C flag
        if ((i & 0xFF) == 0) registers.f |= 0x80;  // Set Z if bit is 0
        registers.f |= 0x20;  // Always set H
        // N is cleared (already done by the mask)
    }

    // Flag handling for AND (always sets H, clears N and C)
    public void fzAND(int result) {
        registers.f = 0x20;  // H always set, N and C cleared
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
    }

    // Flag handling for OR/XOR (clears all flags except Z)
    public void fzOR(int result) {
        registers.f = 0;  // Clear all flags
        if ((result & 0xFF) == 0) registers.f |= 0x80;  // Z
    }

    public void MAPcb() {
        int i = mmu.readByte(registers.pc) & 0xFF;
        registers.pc++;
        registers.pc &= 0xFFFF;
        lastCbOpcode = i;

        Instruction cbInstruction = cbOpcodeMap[i];
        if (cbInstruction != null) {
            cbInstruction.execute();
        } else {
            throw new UnsupportedOperationException(String.format("Unknown CB opcode: 0x%02X", i));
        }
    }

    private void executeRES() {
        int op = lastCbOpcode;
        int bit = (op >> 3) & 7;
        int r = op & 7;
        int mask = ~(1 << bit);
        if (r == 6) {
            int addr = registers.getHL();
            int val = mmu.readByte(addr) & mask;
            mmu.writeByte(addr, val);
            registers.m = 4;
            registers.t = 16;
        } else {
            switch (r) {
                case 0: registers.b &= mask; break;
                case 1: registers.c &= mask; break;
                case 2: registers.d &= mask; break;
                case 3: registers.e &= mask; break;
                case 4: registers.h &= mask; break;
                case 5: registers.l &= mask; break;
                default: registers.a &= mask; break;
            }
            registers.m = 2;
            registers.t = 8;
        }
    }

    private void executeSET() {
        int op = lastCbOpcode;
        int bit = (op >> 3) & 7;
        int r = op & 7;
        int mask = 1 << bit;
        if (r == 6) {
            int addr = registers.getHL();
            int val = mmu.readByte(addr) | mask;
            mmu.writeByte(addr, val);
            registers.m = 4;
            registers.t = 16;
        } else {
            switch (r) {
                case 0: registers.b |= mask; break;
                case 1: registers.c |= mask; break;
                case 2: registers.d |= mask; break;
                case 3: registers.e |= mask; break;
                case 4: registers.h |= mask; break;
                case 5: registers.l |= mask; break;
                default: registers.a |= mask; break;
            }
            registers.m = 2;
            registers.t = 8;
        }
    }


    // Undefined entries
    public void XX() {
        int opc =registers.pc-1;
        throw new UnsupportedOperationException(String.format("Unimplemented instruction at $%02X, stopping.", mmu.readByte(opc) & 0xFF));
    }


    public void checkInterrupts() {
        if (registers.ime == 1 && (mmu.readByte(0xFFFF) & mmu.readByte(0xFF0F)) != 0) {
            handleInterrupt();
        }
    }



    private void handleInterrupt() {
        int enabled = mmu.readByte(0xFFFF);
        int flags   = mmu.readByte(0xFF0F);
        int fired = enabled & flags;

        for (int i = 0; i < 5; i++) {
            int mask = 1 << i;
            if ((fired & mask) != 0) {
                mmu.writeByte(0xFF0F, flags & ~mask);  // Clear IF bit
                registers.ime = 0;

                // Push PC
                registers.sp = (registers.sp - 2) & 0xFFFF;
                mmu.writeWord(registers.sp, registers.pc);

                // Jump to interrupt vector
                switch (i) {
                    case 0: registers.pc = 0x40; break; // VBlank
                    case 1: registers.pc = 0x48; break; // LCD STAT
                    case 2: registers.pc = 0x50; break; // Timer
                    case 3: registers.pc = 0x58; break; // Serial
                    case 4: registers.pc = 0x60; break; // Joypad
                }

                registers.m = 5;
                registers.t = 20;
                return;
            }
        }

        
    }

    public void setSkipBios(boolean skipBios) {
        this.skipBios = skipBios;
    }
    public boolean getSkipBios() {
        return skipBios;
    }

    public static class Registers {
        public int ime;
        //8 bit registers set
        private int a;
        private int b;
        private int c;
       private int d;
        private int e;
        private int h;
        private int l;
        int m;
        int t;
        int f;
        //16 bit registers
        int pc;
        private int sp;

        Registers() {
            this.a = 0;
            this.b = 0;
            this.c = 0;
            this.d = 0;
            this.e = 0;
            this.h = 0;
            this.l = 0;
            this.m = 0;
            this.t = 0;
            this.f = 0;
            this.pc = 0;
            this.sp = 0;
        }


        public int getBC() {
            return ((b & 0xFF) << 8) | (c & 0xFF);
        }

        public void setBC(int val) {
            b = (val >> 8) & 0xFF;
            c = val & 0xFF;
        }
        public int getDE() {
            return ((d & 0xFF) << 8) | (e & 0xFF);
        }

        public void setDE(int val) {
            d = (val >> 8) & 0xFF;
            e = val & 0xFF;
        }

        public int getHL() {
            return ((h & 0xFF) << 8) | (l & 0xFF);
        }

        public void setHL(int val) {
            h = (val >> 8) & 0xFF;
            l = val & 0xFF;
        }

        // Individual register getters for debugger
        public int getA() { return a & 0xFF; }
        public int getB() { return b & 0xFF; }
        public int getC() { return c & 0xFF; }
        public int getD() { return d & 0xFF; }
        public int getE() { return e & 0xFF; }
        public int getH() { return h & 0xFF; }
        public int getL() { return l & 0xFF; }
        public int getF() { return f & 0xFF; }
        public int getSP() { return sp & 0xFFFF; }
        public int getPC() { return pc & 0xFFFF; }

        // Flag helpers
        public boolean isFlagZ() { return (f & 0x80) != 0; }
        public boolean isFlagN() { return (f & 0x40) != 0; }
        public boolean isFlagH() { return (f & 0x20) != 0; }
        public boolean isFlagC() { return (f & 0x10) != 0; }
    

    
    }



    @FunctionalInterface
    interface Instruction {
        void execute();
    }

}
