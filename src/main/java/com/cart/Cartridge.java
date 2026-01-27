package com.cart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public interface Cartridge {

    int readRom(int addr);
    void writeRom(int addr, int value);
    int readRam(int addr);
    void writeRam(int addr, int value);

    void saveIfBatteryBacked();
    void close();

    static Cartridge load(Path romPath, Path saveDir) throws IOException {
        byte[] rom = Files.readAllBytes(romPath);
        if(rom.length < 0x150) {
            throw new IOException("ROM too small to contain a valid header: " + rom.length);
        }

        int cartType = Byte.toUnsignedInt(rom[0x0147]);
        int romSizeCode = Byte.toUnsignedInt(rom[0x0148]);
        int ramSizeCode = Byte.toUnsignedInt(rom[0x0149]);

        String title = parseTitle(rom);
        int romBanks = romBanksFromCode(romSizeCode);
        int ramBytes = ramBytesFromCode(ramSizeCode);

        byte[] romTrimmed = rom;

        int expectedSize = romBanks * 16 * 1024;
        if(rom.length > expectedSize) {
            romTrimmed = Arrays.copyOf(rom, expectedSize);
        }

        SaveRam saveRam = SaveRam.create(saveDir, title, cartType, ramBytes);


        return switch (cartType) {
            case 0x00 -> new RomOnly(romTrimmed, saveRam);
            case 0x01, 0x02, 0x03 -> new MBC1(romTrimmed, saveRam);
            case 0x0F, 0x10, 0x11, 0x12, 0x13 -> new MBC3(romTrimmed, saveRam);
            case 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E -> new MBC5(romTrimmed, saveRam);
            default -> throw new IOException(String.format("Unsupported cartridge type: 0x%02X", cartType));
        };
    }

    static int ramBytesFromCode(int ramSizeCode) {
        // Common RAM size codes:
        return switch (ramSizeCode) {
            case 0x00 -> 0;
            case 0x01 -> 2 * 1024;   // 2KB
            case 0x02 -> 8 * 1024;   // 8KB
            case 0x03 -> 32 * 1024;  // 32KB (4 banks)
            case 0x04 -> 128 * 1024; // 128KB (16 banks) (MBC5)
            case 0x05 -> 64 * 1024;  // 64KB (8 banks) (MBC5)
            default -> 0;
        };
    }

    static int romBanksFromCode(int romSizeCode) throws IOException {
        // Common ROM size codes. (DMG header)
        return switch (romSizeCode) {
            case 0x00 -> 2;    // 32KB
            case 0x01 -> 4;    // 64KB
            case 0x02 -> 8;    // 128KB
            case 0x03 -> 16;   // 256KB
            case 0x04 -> 32;   // 512KB
            case 0x05 -> 64;   // 1MB
            case 0x06 -> 128;  // 2MB
            case 0x07 -> 256;  // 4MB
            case 0x08 -> 512;  // 8MB (rare for GB)
            // 0x52..0x54 are special sizes (1.1/1.2/1.5MB)
            case 0x52 -> 72;
            case 0x53 -> 80;
            case 0x54 -> 96;
            default -> throw new IOException(String.format("Unknown ROM size code: 0x%02X", romSizeCode));
        };
    }

    static String parseTitle(byte[] rom) {

        //Title is at 0x0134-0x0143

        int start = 0x0134;
        int end = 0x0143;
        StringBuilder sb = new StringBuilder();

        for(int i = start; i <= end; i++) {
            int b = Byte.toUnsignedInt(rom[i]);
            if ( b == 0) break;
            if (b >= 32 && b <= 126) sb.append((char) b);
        }

        String title = sb.toString().trim();
        return title.isEmpty() ? "Unknown" : title;

    }
}
