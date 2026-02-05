public class MMU {
    int inBios;

    public byte[] bios;
    public byte[] rom;
    public int[] wram;
    public int[] eram;
    public int[] zram;
    private Z80 cpu;
    private Joypad joypad;
    private Timer_t timer;
    private APU apu;
    int _ie;
    int _if;
    int romOffset;
    int ramOffset;
    int cartType;

    // Serial port registers (for Blargg test output)
    private int serialData = 0;    // SB (0xFF01)
    private int serialControl = 0; // SC (0xFF02)
    private StringBuilder blarggOutput = new StringBuilder();

    public String getBlarggOutput() {
        return blarggOutput.toString();
    }

    // MBC1 state
    private int romBank;   // Selected ROM bank (1-127 for MBC1)
    private int ramBank;   // Selected RAM bank (0-3)
    private boolean ramOn; // RAM enable switch
    private int mode;      // ROM/RAM expansion mode (0 = ROM banking, 1 = RAM banking)

    public void setGpu(GPU gpu) {
        this.gpu = gpu;
    }

    public void setCpu(Z80 cpu) {
        this.cpu = cpu;
    }

    public void setTimer(Timer_t timer) {
        this.timer = timer;
    }

    public void setApu(APU apu) {
        this.apu = apu;
    }

    private GPU gpu;
    MMU() {

        inBios = 1;

        eram = new int[0x8000];  // 32KB for MBC1 RAM (4 banks of 8KB)
        zram = new int[0x80];
        wram = new int[0x2000];
        romOffset = 0x4000;
        ramOffset = 0x0000;
        cartType = 0;

        // Initialize MBC1 state
        romBank = 1;
        ramBank = 0;
        ramOn = false;
        mode = 0;
    }


    public void loadROM(byte[] romData) {
        this.rom = romData;
        // Read cartridge type from ROM header at 0x0147
        this.cartType = romData[0x0147] & 0xFF;
        System.out.println("Cartridge type: 0x" + Integer.toHexString(cartType));
        
        // Reset MMU state for new ROM
        this.inBios = 1;  // Start with BIOS enabled
        this.romBank = 1;
        this.ramBank = 0;
        this.ramOn = false;
        this.mode = 0;
        this.romOffset = 0x4000;
        this.ramOffset = 0x0000;
        this._ie = 0;
        this._if = 0;
        this.serialData = 0;
        this.serialControl = 0;
        this.blarggOutput.setLength(0);  // Clear previous test output
    }

    public void loadBIOS(byte[] biosData) {
        this.bios = biosData;
    }


    public int readByte(int addr) {
        addr&=0xFFFF;

        switch(addr&0xF000) {
            //BIOS
            case 0x0000:
                if (bios == null && addr < 0x0100 && inBios == 1) {
                    return 0x00; // To prevent the debugger from crashing
                }


                if(inBios == 1) {
                    if (addr < 0x0100) return bios[addr] & 0xFF;
                    else {
                        return rom[addr]&0xFF;
                    }
                }

            // ROM0

            case 0x1000:
            case 0x2000:
            case 0x3000:
                return rom[addr]&0xFF;
            // ROM1 (banked ROM) - use romOffset for MBC
            case 0x4000:
            case 0x5000:
            case 0x6000:
            case 0x7000:
                return rom[romOffset + (addr & 0x3FFF)] & 0xFF;
            //GPU VRAM
            case 0x8000:
            case 0x9000:
                return gpu.vram[addr&0x1FFF];
            // External RAM (banked for MBC1)
            case 0xA000:
            case 0xB000:
                if (ramOn) {
                    return eram[ramOffset + (addr & 0x1FFF)];
                }
                return 0xFF;  // Return 0xFF when RAM is disabled
            //Working RAM
            case 0xC000:
            case 0xD000:
                return wram[addr&0x1FFF];

            //Shadow WRAM
            case 0xE000:
                return wram[addr&0x1FFF];
            // WRAM shadow, I/O, ZeroPage
            case 0xF000:
                switch(addr&0x0F00) {
                    // Working RAM shadow
                    case 0x000: case 0x100: case 0x200: case 0x300:
                    case 0x400: case 0x500: case 0x600: case 0x700:
                    case 0x800: case 0x900: case 0xA00: case 0xB00:
                    case 0xC00: case 0xD00:
                        return wram[addr & 0x1FFF];

                    //Graphics object attribute mem
                    // OAM is 160 bytes, remaing bytes read as 0
                    case 0xE00:

                        if(addr < 0xFEA0) {
                            return gpu.oam[addr&0xFF];
                        } else {
                            return 0;
                        }
                        //Zero page
                    case 0xF00:


                        if(addr == 0xFFFF) {
                            return _ie;
                        } else if (addr >= 0xFF80) {
                            return zram[addr & 0x7F];
                        } else {
                            switch (addr & 0x00F0) {
                                case 0x00:
                                    if (addr == 0xFF0F) return _if;
                                    if (addr == 0xFF00) return joypad.read();
                                    // Serial port registers
                                    if (addr == 0xFF01) return serialData;
                                    if (addr == 0xFF02) return serialControl;
                                    // Timer registers 0xFF04-0xFF07
                                    if (addr >= 0xFF04 && addr <= 0xFF07) {
                                        return timer.readByte(addr);
                                    }
                                    break;
                                // APU registers 0xFF10-0xFF3F
                                case 0x10: case 0x20: case 0x30:
                                    if (apu != null) {
                                        return apu.readByte(addr);
                                    }
                                    return 0xFF;
                                case 0x40: case 0x50: case 0x60: case 0x70:
                                    return gpu.readByte(addr);
                            }

                            return 0x00;
                        }


                }
            default:
                throw new UnsupportedOperationException();
        }
    }


    public void writeByte(int addr, int val) {
        int high = addr & 0xF000;
        if (addr == 0xFF50 && val == 0x01) {
            inBios = 0;
        }

        switch (high) {
            // MBC1: RAM Enable (0x0000-0x1FFF)
            case 0x0000:
            case 0x1000:
                // Writing 0x0A to lower 4 bits enables RAM, anything else disables
                if (cartType >= 1 && cartType <= 3) {  // MBC1, MBC1+RAM, MBC1+RAM+BATTERY
                    ramOn = ((val & 0x0F) == 0x0A);
                }
                break;

            // MBC1: ROM Bank Number - lower 5 bits (0x2000-0x3FFF)
            case 0x2000:
            case 0x3000:
                if (cartType >= 1 && cartType <= 3) {
                    // Set lower 5 bits of ROM bank number
                    int bank = val & 0x1F;
                    // Bank 0 maps to bank 1 (can't select bank 0 here)
                    if (bank == 0) bank = 1;
                    romBank = (romBank & 0x60) | bank;
                    romOffset = romBank * 0x4000;
                }
                break;

            // MBC1: RAM Bank Number OR upper bits of ROM Bank (0x4000-0x5FFF)
            case 0x4000:
            case 0x5000:
                if (cartType >= 1 && cartType <= 3) {
                    if (mode == 0) {
                        // ROM banking mode: these 2 bits become upper bits of ROM bank
                        romBank = (romBank & 0x1F) | ((val & 0x03) << 5);
                        romOffset = romBank * 0x4000;
                    } else {
                        // RAM banking mode: select RAM bank 0-3
                        ramBank = val & 0x03;
                        ramOffset = ramBank * 0x2000;
                    }
                }
                break;

            // MBC1: ROM/RAM Mode Select (0x6000-0x7FFF)
            case 0x6000:
            case 0x7000:
                if (cartType >= 1 && cartType <= 3) {
                    mode = val & 0x01;
                    if (mode == 0) {
                        // ROM banking mode: RAM bank is always 0
                        ramBank = 0;
                        ramOffset = 0;
                    }
                }
                break;

            // GPU VRAM
            case 0x8000:
            case 0x9000:
                gpu.vram[addr&0x1FFF] = val;
                gpu.updateTile(addr, val);
                break;

            // External RAM (banked for MBC1)
            case 0xA000:
            case 0xB000:
                if (ramOn) {
                    eram[ramOffset + (addr & 0x1FFF)] = val;
                }
                break;

            // Working RAM
            case 0xC000:
            case 0xD000:
                wram[addr & 0x1FFF] = val;
                break;


            // Shadow RAM
            case 0xE000:
                wram[addr & 0x1FFF] = val;
                break;

            // WRAM Shadow, I/O, Zero Page
            case 0xF000:
                int low = addr & 0x0F00;
                switch (low) {
                    // WRAM shadow
                    case 0x000: case 0x100: case 0x200: case 0x300:
                    case 0x400: case 0x500: case 0x600: case 0x700:
                    case 0x800: case 0x900: case 0xA00: case 0xB00:
                    case 0xC00: case 0xD00:
                        wram[addr & 0x1FFF] = val;
                        break;

                    // GPU OAM (Object Attribute Memory)
                    case 0xE00:
                        if (addr < 0xFEA0) {
                            gpu.oam[addr&0xFF] = val;
                            gpu.buildObjData(addr, val);
                        }
                        break;

                    // I/O Registers and Zero Page RAM
                    case 0xF00:

                        if (addr == 0xFF00) {
                            joypad.write(val);
                            return;
                        }

                        // Serial port - SB (Serial transfer data)
                        if (addr == 0xFF01) {
                            serialData = val & 0xFF;
                            return;
                        }

                        // Serial port - SC (Serial transfer control)
                        if (addr == 0xFF02) {
                            serialControl = val & 0xFF;
                            // Bit 7 set with internal clock (bit 0) = transfer requested
                            if ((val & 0x81) == 0x81) {
                                char c = (char) serialData;
                                System.out.print(c);  // Print character immediately
                                blarggOutput.append(c);
                                // Transfer complete - clear bit 7
                                serialControl &= 0x7F;
                            }
                            return;
                        }

                        if (addr == 0xFFFF) {
                            this._ie = val;
                            return;
                        }

                        if (addr == 0xFF0F) {
                            this._if = val;
                            return;
                        }


                        if (addr >= 0xFF80) {
                            zram[addr & 0x7F] = val;
                        } else {
                            switch(addr & 0x00F0) {
                                case 0x00:
                                    // Timer registers 0xFF04-0xFF07
                                    if (addr >= 0xFF04 && addr <= 0xFF07) {
                                        timer.writeByte(addr, val);
                                    }
                                    break;
                                // APU registers 0xFF10-0xFF3F
                                case 0x10: case 0x20: case 0x30:
                                    if (apu != null) {
                                        apu.writeByte(addr, val);
                                    }
                                    break;
                                case 0x40: case 0x50: case 0x60: case 0x70:
                                    gpu.writeByte(addr, val);
                                    break;
                            }
                        }
                        break;

                }
                break;

            default:
                throw new UnsupportedOperationException("Write to unsupported address: " + Integer.toHexString(addr));
        }
    }


    public int readWord(int addr) {
        return readByte(addr) + (readByte(addr+1)<<8);
    }




    public void writeWord(int addr, int val) {
        writeByte(addr, val&0xFF); // low
        writeByte(addr+1, (val>>8)&0xFF); // high
    }

    public void setjoyPad(Joypad joypad) {
        this.joypad = joypad;
    }
}