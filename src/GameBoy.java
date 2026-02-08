import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
public class GameBoy {
    Z80 cpu;
    MMU mmu;
    GPU gpu;
    Joypad joypad;
    Timer_t timer;
    APU apu;
    private JFrame frame;
    private DebuggerUI debuggerUI;
    private DebuggerController debugger;
    private boolean romLoaded = false;
    private boolean useBios = false;

    public GameBoy(Z80 cpu, MMU mmu, GPU gpu) {
        this.cpu = cpu;
        this.mmu = mmu;
        this.gpu = gpu;

        // Link MMU and GPU to CPU
        this.mmu.setGpu(gpu);
        this.mmu.setCpu(cpu);
        this.gpu.setCpu(cpu);
        this.gpu.setMmu(mmu);
        this.cpu.setMmu(mmu);
        this.joypad = new Joypad();
        this.mmu.setjoyPad(joypad);
        
        // Initialize and wire up timer
        this.timer = new Timer_t();
        this.timer.setMmu(mmu);
        this.timer.setCpu(cpu);
        this.mmu.setTimer(timer);
        
        // Initialize and wire up APU
        this.apu = new APU();
        this.apu.setCpu(cpu);
        this.mmu.setApu(apu);
    }

    public void setUseBios(boolean useBios) {
        this.useBios = useBios;
    }

    public void setDebuggerUI(DebuggerUI debuggerUI) {
        this.debuggerUI = debuggerUI;
    }

    public void setDebuggerController(DebuggerController debugger) {
        this.debugger = debugger;
    }



    /** Load a ROM from the given path and reset CPU/GPU/Timer/APU. Use this when switching ROMs. */
    public void loadNewROM(String romPath) throws IOException {
        if( useBios) {
            cpu.setSkipBios(false);
            mmu.setInBios(true);
            mmu.loadROM(ROMLoader.loadROM(romPath));
        } else {
            cpu.setSkipBios(true);
            mmu.loadROM(ROMLoader.loadROM(romPath));
            mmu.setInBios(false);
        }

        cpu.reset();
        gpu.reset();
        timer.reset();
        apu.reset();
        romLoaded = true;
    }

    /** Reset the emulator state (CPU, GPU, Timer, APU) without reloading ROM. */
    public void reset() {
        cpu.reset();
        gpu.reset();
        timer.reset();
        apu.reset();
        if (debugger != null) {
            debugger.pause();  // Pause after reset so user can step through
        }
    }

    /** Initialize the UI without loading a ROM. */
    private void initUI() {
        if (frame == null) {
            frame = new JFrame("DMG-J");

            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenuItem openROMItem = new JMenuItem("Open ROM...");
            JCheckBoxMenuItem useBiosItem = new JCheckBoxMenuItem("Use BIOS");
            
            JMenuItem debuggerItem = new JMenuItem("Show Debugger");
            JMenuItem openBIOSItem = new JMenuItem("Open BIOS...");


            useBiosItem.addActionListener(e -> {
                setUseBios(useBiosItem.isSelected());
            });

            openROMItem.addActionListener(e -> openROM());
            openBIOSItem.addActionListener(e -> openBIOS());
            debuggerItem.addActionListener(e -> {
                if (debuggerUI != null) {
                    boolean visible = debuggerUI.isVisible();
                    debuggerUI.setVisible(!visible);
                    debuggerItem.setText(visible ? "Show Debugger" : "Hide Debugger");
                }
            });
            fileMenu.add(debuggerItem);
            fileMenu.add(openROMItem);
            fileMenu.add(openBIOSItem);
            fileMenu.add(useBiosItem);
            menuBar.add(fileMenu);

            frame.setJMenuBar(menuBar);

            joypad.attachKeyListener(frame);

            frame.setFocusable(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.getContentPane().add(gpu);
            frame.setLocationRelativeTo(null); // Center on screen
            frame.pack();
            frame.setVisible(true);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    frame.requestFocusInWindow();
                }
            });
            
            // Enable drag-and-drop ROM loading
            setupDragAndDrop();
        }
    }

    private void openROM() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select ROM");
        chooser.setFileFilter(new FileNameExtensionFilter("Game Boy ROMs (.gb, .gbc)", "gb", "gbc"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            loadROMFile(chooser.getSelectedFile());
        }
    }

    private void openBIOS() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select BIOS");
        chooser.setFileFilter(new FileNameExtensionFilter("Game Boy BIOS (.bin)", "bin"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            loadBIOSFile(chooser.getSelectedFile());
        }
    }

    /** Load a ROM from a File object (used by both file chooser and drag-drop). */
    private void loadROMFile(File file) {
        String path = file.getAbsolutePath();
        String name = file.getName();
        
        // Validate file extension
        String lowerName = name.toLowerCase();
        if (!lowerName.endsWith(".gb") && !lowerName.endsWith(".gbc")) {
            JOptionPane.showMessageDialog(frame, 
                "Invalid file type. Please drop a .gb or .gbc ROM file.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            loadNewROM(path);
            frame.setTitle("DMG-J - " + name);
            if (debuggerUI != null) {
                debuggerUI.onROMLoaded();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to load ROM: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBIOSFile(File file) {
        String path = file.getAbsolutePath();
        try {
            mmu.loadBIOS(ROMLoader.loadBIOS(path));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to load BIOS: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Set up drag-and-drop support for loading ROMs. */
    private void setupDragAndDrop() {
        new DropTarget(frame, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) event.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    
                    if (!droppedFiles.isEmpty()) {
                        // Load the first dropped file
                        loadROMFile(droppedFiles.get(0));
                    }
                    event.dropComplete(true);
                } catch (UnsupportedFlavorException | IOException ex) {
                    event.dropComplete(false);
                    JOptionPane.showMessageDialog(frame, 
                        "Failed to process dropped file: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent event) {
                if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    event.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    event.rejectDrag();
                }
            }
        });
    }

    public void frame() {
        if (!romLoaded) return;  // Don't run emulation until ROM is loaded
        if (debugger != null && debugger.isPaused()) return;  // Don't run if debugger is paused

        int frameCycles = 70224;
        int targetTicks = cpu.clock_t + frameCycles;

        while (cpu.clock_t < targetTicks) {
            // Fetch and execute opcode
            int opcode = mmu.readByte(cpu.registers.pc++) & 0xFF;
            cpu.opcodeMap[opcode].execute();
            cpu.registers.pc &= 0xFFFF;  // Mask PC to 16-bit

            // Update master clock
            cpu.clock_m += cpu.registers.m;
            cpu.clock_t += cpu.registers.t;

            // Update the timer
            timer.inc();

            // Tick GPU with instruction cycles (before resetting)
            gpu.tick();
            
            // Tick APU
            apu.tick();

            // Reset instruction cycle counters
            cpu.registers.m = 0;
            cpu.registers.t = 0;

            // Handle delayed IME enable (EI takes effect after next instruction)
            if (cpu.enableIMEAfterNextInstr) {
                cpu.registers.ime = 1;
                cpu.enableIMEAfterNextInstr = false;
            }

            // Check and handle interrupts
            cpu.checkInterrupts();

            // Update clock again for any cycles used by interrupt handling
            cpu.clock_m += cpu.registers.m;
            cpu.clock_t += cpu.registers.t;

            // Update timer again in case an interrupt occurred
            timer.inc();

            // Tick GPU again with interrupt handling cycles (if any)
            gpu.tick();
            
            // Tick APU again with interrupt handling cycles (if any)
            apu.tick();
        }
        gpu.repaint(); // Ensure screen is refreshed
    }



    public void run() {
        initUI();
        Timer emulationTimer = new Timer(16, e -> frame()); // ~60Hz (1000ms/60fps ≈ 16.6ms)
        emulationTimer.start();
    }
}
