import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Joypad {
    private boolean[] keys = new boolean[8];
    private int joypSelect = 0x00; // only bits 4 and 5 matter

    public void setKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_RIGHT -> keys[0] = pressed;
            case KeyEvent.VK_LEFT  -> keys[1] = pressed;
            case KeyEvent.VK_UP    -> keys[2] = pressed;
            case KeyEvent.VK_DOWN  -> keys[3] = pressed;
            case KeyEvent.VK_Z     -> keys[4] = pressed; // A
            case KeyEvent.VK_X     -> keys[5] = pressed; // B
            case KeyEvent.VK_BACK_SPACE -> keys[6] = pressed; // Select
            case KeyEvent.VK_ENTER -> keys[7] = pressed; // Start
        }
    }

    public void attachKeyListener(Component component) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                setKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                setKey(e.getKeyCode(), false);
            }
        });
    }

    public void write(int val) {
        joypSelect = val & 0x30;
    }

    public int read() {
        int result = 0xCF | (joypSelect & 0x30); // upper bits preserved

        if ((joypSelect & 0x10) == 0) { // Direction keys selected
            if (keys[0]) result &= ~0x01; // Right
            if (keys[1]) result &= ~0x02; // Left
            if (keys[2]) result &= ~0x04; // Up
            if (keys[3]) result &= ~0x08; // Down
        }

        if ((joypSelect & 0x20) == 0) { // Button keys selected
            if (keys[4]) result &= ~0x01; // A
            if (keys[5]) result &= ~0x02; // B
            if (keys[6]) result &= ~0x04; // Select
            if (keys[7]) result &= ~0x08; // Start
        }

        return result;
    }



}
