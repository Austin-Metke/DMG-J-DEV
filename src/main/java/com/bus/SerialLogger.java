package com.bus;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class SerialLogger {
    private final ConcurrentLinkedQueue<Byte> bytes = new ConcurrentLinkedQueue<>();

    public void onByte(int value) {
        bytes.add((byte) (value & 0xFF));
    }

    public String drainText() {
        StringBuilder sb = new StringBuilder();
        Byte b;
        while ((b = bytes.poll()) != null) {
            int v = Byte.toUnsignedInt(b);
            sb.append((char) v);
        }
        return sb.toString();
    }
}
