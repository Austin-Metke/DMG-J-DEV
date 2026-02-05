// DebuggerUI.java
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class DebuggerUI extends JFrame {
    // Dark theme colors
    private static final Color BG_DARK = new Color(0x1e1e1e);
    private static final Color BG_PANEL = new Color(0x252526);
    private static final Color BG_EDITOR = new Color(0x1e1e1e);
    private static final Color FG_TEXT = new Color(0xd4d4d4);
    private static final Color FG_ACCENT = new Color(0x569cd6);
    private static final Color FG_NUMBER = new Color(0xb5cea8);
    private static final Color FG_ADDRESS = new Color(0x9cdcfe);
    private static final Color FG_MNEMONIC = new Color(0xdcdcaa);
    private static final Color HIGHLIGHT_PC = new Color(0x264f78);
    private static final Color BREAKPOINT_COLOR = new Color(0x8b0000);
    private static final Color BORDER_COLOR = new Color(0x3c3c3c);

    private static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 13);
    private static final Font MONO_FONT_BOLD = new Font("Consolas", Font.BOLD, 13);

    private final GameBoy emulator;
    private final DebuggerController debugger;

    // UI Components
    private final JTextPane memoryPane = new JTextPane();
    private final JTextPane disasmPane = new JTextPane();
    private final JTextPane stackPane = new JTextPane();
    private final JSlider memorySlider = new JSlider(0, 0xFFFF, 0);
    private int memoryScrollAddr = 0x0000;

    // Register labels
    private final JLabel regA = new JLabel();
    private final JLabel regBC = new JLabel();
    private final JLabel regDE = new JLabel();
    private final JLabel regHL = new JLabel();
    private final JLabel regSP = new JLabel();
    private final JLabel regPC = new JLabel();
    private final JLabel regF = new JLabel();
    private final JLabel flagZ = new JLabel("Z");
    private final JLabel flagN = new JLabel("N");
    private final JLabel flagH = new JLabel("H");
    private final JLabel flagC = new JLabel("C");

    // Toolbar buttons
    private JButton runPauseButton;

    public DebuggerUI(GameBoy emulator, DebuggerController debugger) {
        this.emulator = emulator;
        this.debugger = debugger;
        debugger.setOnUpdate(this::updateDisplay);

        setTitle("DMG-J Debugger");
        setSize(1000, 700);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);

        setLayout(new BorderLayout(0, 0));

        // Build UI
        add(createToolbar(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        // Register keyboard shortcuts
        registerKeyboardShortcuts();

        updateDisplay();

        // Periodic refresh
        new Timer(200, e -> updateDisplay()).start();
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(BG_PANEL);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        runPauseButton = createToolButton("▶ Run", "Resume execution (F5)", e -> toggleRunPause());
        JButton stepButton = createToolButton("⏭ Step", "Step one instruction (F10)", e -> {
            debugger.step();
            updateDisplay();
        });
        JButton resetButton = createToolButton("↺ Reset", "Reset emulator", e -> {
            emulator.reset();
            memoryScrollAddr = emulator.cpu.registers.pc & 0xFFF0;
            memorySlider.setValue(memoryScrollAddr);
            updateDisplay();
        });
        JButton setPcButton = createToolButton("PC", "Set Program Counter", e -> openPcEditorWindow());
        JButton jumpToPcButton = createToolButton("→PC", "Jump memory view to PC", e -> {
            memoryScrollAddr = emulator.cpu.registers.pc & 0xFFF0;
            memorySlider.setValue(memoryScrollAddr);
            updateDisplay();
        });
        JButton clearBpButton = createToolButton("Clear BP", "Clear all breakpoints", e -> debugger.clearAllBreakpoints());

        toolbar.add(runPauseButton);
        toolbar.addSeparator(new Dimension(10, 0));
        toolbar.add(stepButton);
        toolbar.addSeparator(new Dimension(10, 0));
        toolbar.add(resetButton);
        toolbar.addSeparator(new Dimension(20, 0));
        toolbar.add(setPcButton);
        toolbar.add(jumpToPcButton);
        toolbar.addSeparator(new Dimension(20, 0));
        toolbar.add(clearBpButton);

        return toolbar;
    }

    private JButton createToolButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setBackground(BG_PANEL);
        button.setForeground(FG_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.addActionListener(action);
        return button;
    }

    private JPanel createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Left: Registers panel
        JPanel leftPanel = createRegistersPanel();

        // Center: Memory view with region buttons
        JPanel centerPanel = createMemoryPanel();

        // Right: Disassembly and Stack - setup click handlers
        createDisasmAndStackPanel();

        // Use split panes for resizable layout
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            wrapInTitledPanel("Disassembly", createScrollPane(disasmPane)),
            wrapInTitledPanel("Stack", createScrollPane(stackPane)));
        rightSplit.setResizeWeight(0.7);
        rightSplit.setBackground(BG_DARK);
        rightSplit.setBorder(null);
        rightSplit.setDividerSize(5);

        JSplitPane centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightSplit);
        centerRightSplit.setResizeWeight(0.5);
        centerRightSplit.setBackground(BG_DARK);
        centerRightSplit.setBorder(null);
        centerRightSplit.setDividerSize(5);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(centerRightSplit, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createRegistersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(createTitledBorder("Registers"));
        panel.setPreferredSize(new Dimension(140, 0));

        // 8-bit register
        panel.add(createRegisterRow("A:", regA));
        panel.add(Box.createVerticalStrut(5));

        // 16-bit register pairs
        panel.add(createRegisterRow("BC:", regBC));
        panel.add(createRegisterRow("DE:", regDE));
        panel.add(createRegisterRow("HL:", regHL));
        panel.add(Box.createVerticalStrut(10));

        panel.add(createRegisterRow("SP:", regSP));
        panel.add(createRegisterRow("PC:", regPC));
        panel.add(Box.createVerticalStrut(10));

        panel.add(createRegisterRow("F:", regF));
        panel.add(Box.createVerticalStrut(10));

        // Flags
        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        flagsPanel.setBackground(BG_PANEL);
        flagsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel flagsLabel = new JLabel("Flags: ");
        flagsLabel.setForeground(FG_TEXT);
        flagsLabel.setFont(MONO_FONT);
        flagsPanel.add(flagsLabel);

        for (JLabel flag : new JLabel[]{flagZ, flagN, flagH, flagC}) {
            flag.setFont(MONO_FONT_BOLD);
            flag.setOpaque(true);
            flag.setBackground(BG_DARK);
            flag.setForeground(FG_TEXT);
            flag.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            flagsPanel.add(flag);
        }
        panel.add(flagsPanel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createRegisterRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setBackground(BG_PANEL);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(FG_TEXT);
        nameLabel.setFont(MONO_FONT);

        valueLabel.setForeground(FG_NUMBER);
        valueLabel.setFont(MONO_FONT_BOLD);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private JPanel createMemoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(BG_DARK);

        // Memory text pane
        setupTextPane(memoryPane);
        JScrollPane memScroll = createScrollPane(memoryPane);

        // Mouse wheel scrolling
        memoryPane.addMouseWheelListener(e -> {
            int delta = e.getUnitsToScroll() * 16;
            memoryScrollAddr = Math.max(0, Math.min(0x10000 - 0x100, memoryScrollAddr + delta));
            memorySlider.setValue(memoryScrollAddr);
            updateDisplay();
        });

        // Memory slider
        memorySlider.setBackground(BG_PANEL);
        memorySlider.setForeground(FG_TEXT);
        memorySlider.addChangeListener(e -> {
            memoryScrollAddr = memorySlider.getValue() & 0xFFFF;
            updateDisplay();
        });

        // Memory region buttons
        JPanel regionPanel = createMemoryRegionButtons();

        JPanel memoryWithSlider = new JPanel(new BorderLayout());
        memoryWithSlider.setBackground(BG_DARK);
        memoryWithSlider.add(memScroll, BorderLayout.CENTER);
        memoryWithSlider.add(memorySlider, BorderLayout.SOUTH);

        panel.add(wrapInTitledPanel("Memory", memoryWithSlider), BorderLayout.CENTER);
        panel.add(regionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMemoryRegionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        String[][] regions = {
            {"ROM", "0x0000"},
            {"VRAM", "0x8000"},
            {"SRAM", "0xA000"},
            {"WRAM", "0xC000"},
            {"OAM", "0xFE00"},
            {"I/O", "0xFF00"},
            {"HRAM", "0xFF80"}
        };

        for (String[] region : regions) {
            JButton btn = new JButton(region[0]);
            btn.setFont(new Font("Consolas", Font.PLAIN, 10));
            btn.setBackground(BG_DARK);
            btn.setForeground(FG_ACCENT);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            btn.setToolTipText("Jump to " + region[1]);
            int addr = Integer.decode(region[1]);
            btn.addActionListener(e -> {
                memoryScrollAddr = addr;
                memorySlider.setValue(memoryScrollAddr);
                updateDisplay();
            });
            panel.add(btn);
        }
        return panel;
    }

    private JPanel createDisasmAndStackPanel() {
        setupTextPane(disasmPane);
        setupTextPane(stackPane);

        // Click to toggle breakpoint in disassembly
        disasmPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int offset = disasmPane.viewToModel2D(e.getPoint());
                try {
                    int line = disasmPane.getDocument().getDefaultRootElement().getElementIndex(offset);
                    int addr = getDisasmAddressAtLine(line);
                    if (addr >= 0) {
                        debugger.toggleBreakpoint(addr);
                    }
                } catch (Exception ex) {
                    // Ignore click errors
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        return panel;
    }

    private int getDisasmAddressAtLine(int line) {
        // Parse the address from the disassembly at the given line
        int addr = emulator.cpu.registers.pc;
        for (int i = 0; i < line && i < 20; i++) {
            int opcode = emulator.mmu.readByte(addr) & 0xFF;
            int length;
            if (opcode == 0xCB) {
                int cbOpcode = emulator.mmu.readByte((addr + 1) & 0xFFFF) & 0xFF;
                length = CBOpcodeInfo.getInstructionLength(cbOpcode);
            } else {
                length = OpcodeInfo.getInstructionLength(opcode);
            }
            addr = (addr + length) & 0xFFFF;
        }
        return addr;
    }

    private void setupTextPane(JTextPane pane) {
        pane.setFont(MONO_FONT);
        pane.setEditable(false);
        pane.setBackground(BG_EDITOR);
        pane.setForeground(FG_TEXT);
        pane.setCaretColor(FG_TEXT);
    }

    private JScrollPane createScrollPane(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(BG_EDITOR);
        return scroll;
    }

    private JPanel wrapInTitledPanel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(createTitledBorder(title));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private Border createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            title
        );
        border.setTitleColor(FG_ACCENT);
        border.setTitleFont(MONO_FONT_BOLD);
        return border;
    }

    private void registerKeyboardShortcuts() {
        // F5 - Run/Pause
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "runPause");
        getRootPane().getActionMap().put("runPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleRunPause();
            }
        });

        // F10 - Step
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "step");
        getRootPane().getActionMap().put("step", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                debugger.step();
                updateDisplay();
            }
        });
    }

    private void toggleRunPause() {
        if (debugger.isPaused()) {
            debugger.resume();
            runPauseButton.setText("⏸ Pause");
        } else {
            debugger.pause();
            runPauseButton.setText("▶ Run");
        }
        updateDisplay();
    }

    private void openPcEditorWindow() {
        String currentPc = String.format("0x%04X", emulator.cpu.registers.pc);
        String input = JOptionPane.showInputDialog(this, "Enter new PC value (hex):", currentPc);
        if (input != null) {
            try {
                int pc = Integer.decode(input);
                debugger.setPC(pc);
                memoryScrollAddr = pc & 0xFFF0;
                memorySlider.setValue(memoryScrollAddr);
                updateDisplay();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid PC value.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void onROMLoaded() {
        memoryScrollAddr = emulator.cpu.registers.pc & 0xFFF0;
        memorySlider.setValue(memoryScrollAddr);
        updateDisplay();
    }

    private void updateDisplay() {
        updateRegisters();
        updateMemory();
        updateDisasm();
        updateStack();
        updateRunPauseButton();
    }

    private void updateRegisters() {
        Z80.Registers r = emulator.cpu.registers;
        regA.setText(String.format("0x%02X", r.getA()));
        regBC.setText(String.format("0x%04X", r.getBC()));
        regDE.setText(String.format("0x%04X", r.getDE()));
        regHL.setText(String.format("0x%04X", r.getHL()));
        regSP.setText(String.format("0x%04X", r.getSP()));
        regPC.setText(String.format("0x%04X", r.getPC()));
        regF.setText(String.format("0x%02X", r.getF()));

        // Update flag indicators
        updateFlagLabel(flagZ, r.isFlagZ());
        updateFlagLabel(flagN, r.isFlagN());
        updateFlagLabel(flagH, r.isFlagH());
        updateFlagLabel(flagC, r.isFlagC());
    }

    private void updateFlagLabel(JLabel label, boolean set) {
        if (set) {
            label.setBackground(FG_ACCENT);
            label.setForeground(BG_DARK);
        } else {
            label.setBackground(BG_DARK);
            label.setForeground(new Color(0x555555));
        }
    }

    private void updateMemory() {
        StyledDocument doc = memoryPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        Style normal = getOrCreateStyle(memoryPane, "normal", FG_TEXT, null);
        Style address = getOrCreateStyle(memoryPane, "address", FG_ADDRESS, null);
        Style number = getOrCreateStyle(memoryPane, "number", FG_NUMBER, null);
        Style highlight = getOrCreateStyle(memoryPane, "highlight", FG_TEXT, HIGHLIGHT_PC);

        int startAddr = memoryScrollAddr & 0xFFF0;
        int endAddr = Math.min(startAddr + 0x100, 0x10000);
        int pc = emulator.cpu.registers.pc;

        try {
            for (int addr = startAddr; addr < endAddr; addr++) {
                if (addr % 16 == 0) {
                    doc.insertString(doc.getLength(), String.format("%04X: ", addr), address);
                }

                Style style = (addr == pc) ? highlight : number;
                doc.insertString(doc.getLength(), String.format("%02X ", emulator.mmu.readByte(addr) & 0xFF), style);

                if (addr % 16 == 15) {
                    doc.insertString(doc.getLength(), "\n", normal);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void updateDisasm() {
        StyledDocument doc = disasmPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        Style normal = getOrCreateStyle(disasmPane, "normal", FG_TEXT, null);
        Style address = getOrCreateStyle(disasmPane, "address", FG_ADDRESS, null);
        Style mnemonic = getOrCreateStyle(disasmPane, "mnemonic", FG_MNEMONIC, null);
        Style number = getOrCreateStyle(disasmPane, "number", FG_NUMBER, null);
        Style pcHighlight = getOrCreateStyle(disasmPane, "pcHighlight", FG_TEXT, HIGHLIGHT_PC);
        Style breakpoint = getOrCreateStyle(disasmPane, "breakpoint", Color.WHITE, BREAKPOINT_COLOR);

        int addr = emulator.cpu.registers.pc;
        int currentPc = emulator.cpu.registers.pc;

        try {
            for (int i = 0; i < 20; i++) {
                int opcode = emulator.mmu.readByte(addr) & 0xFF;
                String mnemonicStr;
                int length;

                if (opcode == 0xCB) {
                    int cbOpcode = emulator.mmu.readByte((addr + 1) & 0xFFFF) & 0xFF;
                    mnemonicStr = CBOpcodeInfo.getMnemonic(cbOpcode);
                    length = CBOpcodeInfo.getInstructionLength(cbOpcode);
                } else {
                    mnemonicStr = OpcodeInfo.getMnemonic(opcode);
                    length = OpcodeInfo.getInstructionLength(opcode);
                }

                // Determine line style
                boolean isBreakpoint = debugger.hasBreakpoint(addr);
                boolean isCurrentPc = (addr == currentPc);

                // Choose the appropriate style for this line
                Style lineStyle = isBreakpoint ? breakpoint : (isCurrentPc ? pcHighlight : normal);

                // Breakpoint/PC marker
                String marker = isBreakpoint ? "● " : (isCurrentPc ? "→ " : "  ");
                doc.insertString(doc.getLength(), marker, lineStyle);

                // Address
                doc.insertString(doc.getLength(), String.format("%04X: ", addr), 
                    isBreakpoint ? breakpoint : (isCurrentPc ? pcHighlight : address));

                // Mnemonic
                doc.insertString(doc.getLength(), String.format("%-12s", mnemonicStr), 
                    isBreakpoint ? breakpoint : (isCurrentPc ? pcHighlight : mnemonic));

                // Bytes
                StringBuilder bytes = new StringBuilder();
                for (int j = 0; j < length; j++) {
                    bytes.append(String.format("%02X ", emulator.mmu.readByte((addr + j) & 0xFFFF) & 0xFF));
                }
                doc.insertString(doc.getLength(), bytes.toString(), 
                    isBreakpoint ? breakpoint : (isCurrentPc ? pcHighlight : number));

                doc.insertString(doc.getLength(), "\n", normal);
                addr = (addr + length) & 0xFFFF;
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void updateStack() {
        StyledDocument doc = stackPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        Style normal = getOrCreateStyle(stackPane, "normal", FG_TEXT, null);
        Style address = getOrCreateStyle(stackPane, "address", FG_ADDRESS, null);
        Style number = getOrCreateStyle(stackPane, "number", FG_NUMBER, null);

        int sp = emulator.cpu.registers.getSP();

        try {
            doc.insertString(doc.getLength(), String.format("SP = 0x%04X\n\n", sp), address);

            // Show 8 stack entries (16 bytes, 2 bytes each for addresses)
            for (int i = 0; i < 8; i++) {
                int stackAddr = (sp + i * 2) & 0xFFFF;
                if (stackAddr > 0xFFFE) break;

                int lo = emulator.mmu.readByte(stackAddr) & 0xFF;
                int hi = emulator.mmu.readByte((stackAddr + 1) & 0xFFFF) & 0xFF;
                int value = (hi << 8) | lo;

                doc.insertString(doc.getLength(), String.format("SP+%d: ", i * 2), normal);
                doc.insertString(doc.getLength(), String.format("%04X", stackAddr), address);
                doc.insertString(doc.getLength(), " → ", normal);
                doc.insertString(doc.getLength(), String.format("0x%04X", value), number);
                doc.insertString(doc.getLength(), "\n", normal);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void updateRunPauseButton() {
        if (debugger.isPaused()) {
            runPauseButton.setText("▶ Run");
        } else {
            runPauseButton.setText("⏸ Pause");
        }
    }

    private Style getOrCreateStyle(JTextPane pane, String name, Color fg, Color bg) {
        Style style = pane.getStyle(name);
        if (style == null) {
            style = pane.addStyle(name, null);
            StyleConstants.setForeground(style, fg);
            if (bg != null) {
                StyleConstants.setBackground(style, bg);
            }
            StyleConstants.setFontFamily(style, "Consolas");
            StyleConstants.setFontSize(style, 13);
        }
        return style;
    }
}
