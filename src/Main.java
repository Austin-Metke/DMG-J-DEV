public class Main {
    public static void main(String[] args) {
        Z80 cpu = new Z80();
        GPU gpu = new GPU(3);
        MMU mmu = new MMU();

        GameBoy gb = new GameBoy(cpu, mmu, gpu);
        DebuggerController debugController = new DebuggerController(gb);
        DebuggerUI debuggerUI = new DebuggerUI(gb, debugController);
        gb.setDebuggerUI(debuggerUI);
        gb.setDebuggerController(debugController);

        gb.run();
    }
}