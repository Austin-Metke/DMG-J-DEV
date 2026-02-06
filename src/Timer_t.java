public class Timer_t {

    Clock clock;
    MMU mmu;
    Z80 cpu;

    int div;
    int tma;
    int tima;
    int tac;
    int sdiv;

    public void setMmu(MMU mmu) {
        this.mmu = mmu;
    }

    public void setCpu(Z80 cpu) {
        this.cpu = cpu;
    }

    Timer_t() {
        reset();
    }

    public void reset() {
        div = 0;
        sdiv = 0;
        tma = 0;
        tima = 0;
        tac = 0;
        clock = new Clock();
    }


    public void tick() {
        tima++;
        clock.main = 0;
        if(tima > 255) {
            tima = tma;
            mmu._if |= 4;
        }
    }

    public void inc() {


        clock.sub += cpu.registers.m;

        while(clock.sub >= 4) {
            clock.main++;
            clock.sub -= 4;

            clock.div++;

            if(clock.div == 16) {
                div = (div+1)&0xFF;
                clock.div = 0;
            }
        }


        if((tac&4) != 0) {
            switch(tac&3) {
                case 0:
                    if(clock.main >= 64) tick();
                    break;
                case 1:
                    if(clock.main >=  1) tick();
                    break;
                case 2:
                    if(clock.main >=  4) tick();
                    break;
                case 3:
                    if(clock.main >= 16) tick();
                    break;
            }
        }

    }

    public int readByte(int addr) {
        return switch (addr) {
            case 0xFF04 -> div;
            case 0xFF05 -> tima;
            case 0xFF06 -> tma;
            case 0xFF07 -> tac;
            default -> 0;
        };
    }

    public void writeByte(int addr, int val) {
        switch(addr)
        {
            case 0xFF04: div = 0; break;
            case 0xFF05: tima = val; break;
            case 0xFF06: tma = val; break;
            case 0xFF07: tac = val&7; break;
        }
}



    private static class Clock {
        int main;
        int sub;
        int div;


        Clock() {
            this.main = 0;
            this.sub = 0;
            this.div = 0;
        }
    }



}