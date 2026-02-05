// DebuggerController.java
import java.util.HashSet;
import java.util.Set;

public class DebuggerController {
    private final GameBoy emulator;
    private boolean paused = false;
    private final Set<Integer> breakpoints = new HashSet<>();
    private Runnable onUpdate;

    public DebuggerController(GameBoy emulator) {
        this.emulator = emulator;
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    public void step() {
        emulator.cpu.tick();
        afterStep();
        notifyUpdate();
    }

    public void pause() { 
        paused = true; 
        notifyUpdate();
    }

    public void resume() { 
        paused = false; 
    }

    public boolean isPaused() { 
        return paused; 
    }

    public void afterStep() {
        emulator.gpu.tick();
    }

    public void setPC(int addr) {
        emulator.cpu.registers.pc = addr;
        notifyUpdate();
    }

    public int getPC() {
        return emulator.cpu.registers.pc;
    }

    // Breakpoint methods
    public void toggleBreakpoint(int addr) {
        if (breakpoints.contains(addr)) {
            breakpoints.remove(addr);
        } else {
            breakpoints.add(addr);
        }
        notifyUpdate();
    }

    public boolean hasBreakpoint(int addr) {
        return breakpoints.contains(addr);
    }

    public Set<Integer> getBreakpoints() {
        return breakpoints;
    }

    public void clearAllBreakpoints() {
        breakpoints.clear();
        notifyUpdate();
    }

    private void notifyUpdate() {
        if (onUpdate != null) {
            onUpdate.run();
        }
    }

    public GameBoy getEmulator() {
        return emulator;
    }
}
