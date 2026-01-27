package com.cart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SaveRam {
    private final boolean hasBattery;
    private final byte[] ram;
    private final Path savePath;
    private boolean dirty;

    private SaveRam(boolean hasBattery, byte[] ram, Path savePath) {
        this.hasBattery = hasBattery;
        this.ram = ram;
        this.savePath = savePath;
        this.dirty = false;
    }

    public static SaveRam create(Path saveDir, String romTitle, int cartType, int ramBytes) throws IOException {
        boolean battery = switch (cartType) {
            case 0x03, // MBC1+RAM+BATTERY
                 0x06, // MBC2+BATTERY (not implemented here)
                 0x09, // ROM+RAM+BATTERY (rare)
                 0x0F, 0x10, 0x13, // MBC3 battery variants
                 0x1B, 0x1E -> true; // MBC5 battery variants
            default -> false;
        };

        byte[] ram = new byte[Math.max(ramBytes, 0)];
        Files.createDirectories(saveDir);

        String safeName = romTitle.replaceAll("[^a-zA-Z0-9._-]+", "_");
        Path savePath = saveDir.resolve(safeName + ".sav");

        if (battery && ram.length > 0 && Files.exists(savePath)) {
            byte[] data = Files.readAllBytes(savePath);
            System.arraycopy(data, 0, ram, 0, Math.min(data.length, ram.length));
        }

        return new SaveRam(battery, ram, savePath);
    }

    public boolean hasRam() {
        return ram.length > 0;
    }

    public boolean hasBattery() {
        return hasBattery;
    }

    public int read(int offset) {
        if (!hasRam()) return 0xFF;
        return Byte.toUnsignedInt(ram[offset % ram.length]);
    }

    public void write(int offset, int value) {
        if (!hasRam()) return;
        ram[offset % ram.length] = (byte) value;
        dirty = true;
    }

    public void saveIfNeeded() {
        if (!hasBattery || !hasRam() || !dirty) return;
        try {
            Files.write(savePath, ram);
            dirty = false;
        } catch (IOException e) {
            // Keep running; don't crash the emulator on save failure.
            e.printStackTrace();
        }
    }

    public void close() {
        saveIfNeeded();
    }
}
