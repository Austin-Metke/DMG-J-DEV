import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GPU extends JPanel {
    private BufferedImage screen;
    private Graphics2D canvas;
    private final int scale;
    public static final int WIDTH = 160;
    public static final int HEIGHT = 144;

    private int bgmap;
    private int scy;
    private int scx;
    private int bgtile;
    private int[][] pal;
    public int[][] bg;
    public int[][] obj0;
    public int[][] obj1;
    private int obj_size; // 0 = 8x8, 1 = 8x16

    private int switchbg;
    private int switchlcd;
    public int[] vram;
    public int[] oam;
    public Sprite[] objdata;
    private int modeClock;
    private int mode;
    int line;

    private int[][][] tileSet;
    private Z80 cpu;
    private int switchobj;
    private MMU mmu;

    public void setCpu(Z80 cpu) {
        this.cpu = cpu;
    }
    
    public void setMmu(MMU mmu) {
        this.mmu = mmu;
    }

    public GPU(int scale) {
        this.scale = scale;
        this.setPreferredSize(new Dimension(WIDTH * scale, HEIGHT * scale));

        reset();
    }

    public void reset() {
        screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        canvas = screen.createGraphics();

        canvas.setColor(Color.WHITE);
        canvas.fillRect(0, 0, WIDTH, HEIGHT);
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        repaint();

        vram = new int[0x2000];
        tileSet = new int[512][8][8];
        oam = new int[160];
        objdata = new Sprite[40];

        pal = new int[][]{
                {255, 255, 255, 255},
                {192, 192, 192, 255},
                {96, 96, 96, 255},
                {0, 0, 0, 255}
        };

        bg = new int[4][4];
        obj0 = new int[4][4];
        obj1 = new int[4][4];

        // Initialize sprite palettes with default grayscale values
        // Color 0 is transparent for sprites, but we still set it
        for (int i = 0; i < 4; i++) {
            obj0[i] = new int[]{255, 255, 255, 255};
            obj1[i] = new int[]{255, 255, 255, 255};
        }
        // Default palette mapping (will be overwritten by game)
        obj0[0] = new int[]{255, 255, 255, 255}; // transparent
        obj0[1] = new int[]{192, 192, 192, 255};
        obj0[2] = new int[]{96, 96, 96, 255};
        obj0[3] = new int[]{0, 0, 0, 255};
        obj1[0] = new int[]{255, 255, 255, 255}; // transparent
        obj1[1] = new int[]{192, 192, 192, 255};
        obj1[2] = new int[]{96, 96, 96, 255};
        obj1[3] = new int[]{0, 0, 0, 255};

        for (int i = 0; i < 384; i++) {
            for (int j = 0; j < 8; j++) {
                tileSet[i][j] = new int[8];
            }
        }

        for(int i = 0, n = 0; i < 40; i++, n+=4) {
           oam[n + 0] = 0;
           oam[n + 1] = 0;
           oam[n + 2] = 0;
           oam[n + 3] = 0;
           objdata[i] = new Sprite(oam, i);
        }


    }

    public void updateTile(int addr, int val) {
        int saddr = addr - 0x8000;
        if ((addr & 1) != 0) saddr--;

        int tile = (addr >> 4) & 511;
        int y = (addr >> 1) & 7;

        for (int x = 0; x < 8; x++) {
            int sx = 1 << (7 - x);
            tileSet[tile][y][x] = ((vram[saddr] & sx) != 0 ? 1 : 0) | ((vram[saddr + 1] & sx) != 0 ? 2 : 0);
        }

    }

    public void tick() {
        modeClock += cpu.registers.t;


        switch (mode) {
            case 2:
                if (modeClock >= 80) {
                    modeClock = 0;
                    mode = 3;
                }
                break;
            case 3:
                if (modeClock >= 172) {
                    modeClock = 0;
                    mode = 0;
                    renderScan();
                }
                break;
            case 0:
                if (modeClock >= 204) {
                    modeClock = 0;
                    line++;
                    if (line == 144) {
                        mode = 1;
                        pushFramebufferToScreen();

                        int flags = mmu.readByte(0xFF0F);
                        mmu.writeByte(0xFF0F, flags|0x01);

                    } else {
                        mode = 2;
                    }
                }
                break;
            case 1:
                if (modeClock >= 456) {
                    modeClock = 0;
                    line++;
                    if (line > 153) {
                        mode = 2;
                        line = 0;
                    }
                }
                break;
        }
     }

    private void renderScan() {

        int[] scanrow = new int[160]; // stores bg color index

        if(switchbg == 1) {

        int mapOffset = (bgmap != 0) ? 0x1C00 : 0x1800;
        int tileRow = ((line + scy) & 0xFF) >> 3;
        int pixelY = (line + scy) & 0x07;
        // === Background Rendering ===
        for (int i = 0; i < 160; i++) {
            int pixelX = (scx + i) & 0xFF;
            int tileCol = pixelX >> 3;
            int mapIndex = (mapOffset + tileRow * 32 + tileCol) & 0x1FFF;
            int tileIndex = vram[mapIndex] & 0xFF;
            if (bgtile == 0) tileIndex = (byte) tileIndex + 256;


            int colorIndex = tileSet[tileIndex][pixelY][pixelX & 0x07];
            scanrow[i] = colorIndex;

            int[] color = pal[colorIndex];
            int rgb = ((color[0] & 0xFF) << 16) |
                    ((color[1] & 0xFF) << 8)  |
                    ((color[2] & 0xFF));
            screen.setRGB(i, line, rgb);
        }

        }
        if(switchobj == 1) {

            // === Sprite Rendering ===
            for (Sprite s : objdata) {

                int spriteHeight = (obj_size == 1) ? 16 : 8;

                if (line < s.y || line >= s.y + spriteHeight) continue; // only process sprites that intersect this scanline
                int tileLine = (line - s.y);

                // Apply Y-flip
                if (s.yFlip) {
                    tileLine = (spriteHeight - 1) - tileLine;
                }

                int tileIndex;
                if (obj_size == 1) {
                    // 8x16 mode: use top tile for lines 0-7, bottom tile for lines 8-15
                    int baseTile = s.tile & 0xFE; // round down to even for 8x16
                    if (tileLine >= 8) {
                        tileIndex = baseTile + 1;
                        tileLine -= 8;
                    } else {
                        tileIndex = baseTile;
                    }
                } else {
                    // 8x8 mode
                    tileIndex = s.tile;
                }

                int[] row = tileSet[tileIndex][tileLine];

                // Select the correct sprite palette (OBP0 or OBP1)
                int[][] spritePal = s.palette ? obj1 : obj0;

                for (int x = 0; x < 8; x++) {
                    int screenX = s.x + x;
                    if (screenX < 0 || screenX >= 160) continue;

                    // Apply X-flip when reading tile data
                    int tileX = s.xFlip ? (7 - x) : x;
                    int colorIndex = row[tileX];
                    if (colorIndex == 0) continue; // Transparent

                    // === Priority check ===
                    // priority == true means sprite is above BG (bit 7 was clear)
                    // priority == false means sprite is behind BG colors 1-3
                    if (!s.priority && scanrow[screenX] != 0) continue;

                    int[] color = spritePal[colorIndex];
                    int rgb = ((color[0] & 0xFF) << 16) |
                            ((color[1] & 0xFF) << 8) |
                            ((color[2] & 0xFF));
                    screen.setRGB(screenX, line, rgb);
                }
            }
        }
    }

    public boolean isInVBlank() {
        return line >= 144 && line <= 153;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        g.drawImage(screen, 0, 0, width, height, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH * scale, HEIGHT * scale);
    }



    private void pushFramebufferToScreen() {
        repaint();
    }

    public int readByte(int addr) {
        return switch (addr) {
            case 0xFF40 -> ((switchbg & 1)) |
                    ((switchobj & 1) << 1) |
                    ((obj_size & 1) << 2) |
                    ((bgmap & 1) << 3) |
                    ((bgtile & 1) << 4) |
                    ((switchlcd & 1) << 7);
            case 0xFF42 -> scy;
            case 0xFF43 -> scx;
            case 0xFF44 -> line;
            default -> 0;
        };
    }

    public void writeByte(int addr, int val) {
        switch (addr) {
            case 0xFF40:
                switchlcd = (val & 0x80) != 0 ? 1 : 0;
                switchobj = (val & 0x02) != 0 ? 1 : 0;
                bgmap     = (val & 0x08) != 0 ? 1 : 0;
                bgtile    = (val & 0x10) != 0 ? 1 : 0;
                switchbg  = (val & 0x01) != 0 ? 1 : 0;
                obj_size  = (val & 0x04) != 0 ? 1 : 0; // ✅ sprite height mode
                break;

            case 0xFF41:
                // Optional: implement STAT interrupt flags if needed
                break;
            case 0xFF42:
                scy = val;
                break;
            case 0xFF43:
                scx = val;
                break;
            case 0xFF44:
                break; // LY is read-only

            case 0xFF46: // OAM DMA

                for (int i = 0; i < 160; i++) {
                    int srcAddr = (val << 8) + i;
                    int data = cpu.mmu.readByte(srcAddr);
                    oam[i] = data;
                    buildObjData(0xFE00 + i, data);
                }

                break;

            case 0xFF47:
                for (int i = 0; i < 4; i++) {
                    switch ((val >> (i * 2)) & 3) {
                        case 0 -> pal[i] = new int[]{255, 255, 255, 255};
                        case 1 -> pal[i] = new int[]{192, 192, 192, 255};
                        case 2 -> pal[i] = new int[]{96, 96, 96, 255};
                        case 3 -> pal[i] = new int[]{0, 0, 0, 255};
                    }
                }
                break;
            case 0xFF48:
                for (int i = 0; i < 4; i++) {
                    switch ((val >> (i * 2)) & 3) {
                        case 0 -> obj0[i] = new int[]{255, 255, 255, 255};
                        case 1 -> obj0[i] = new int[]{192, 192, 192, 255};
                        case 2 -> obj0[i] = new int[]{96, 96, 96, 255};
                        case 3 -> obj0[i] = new int[]{0, 0, 0, 255};
                    }
                }
                break;
            case 0xFF49:
                for (int i = 0; i < 4; i++) {
                    switch ((val >> (i * 2)) & 3) {
                        case 0 -> obj1[i] = new int[]{255, 255, 255, 255};
                        case 1 -> obj1[i] = new int[]{192, 192, 192, 255};
                        case 2 -> obj1[i] = new int[]{96, 96, 96, 255};
                        case 3 -> obj1[i] = new int[]{0, 0, 0, 255};
                    }
                }
                break;
            case 0xFF4A:
                // Optional: Window Y position
                break;
            case 0xFF4B:
                // Optional: Window X position (val - 7)
                break;
        }
    }

    public void buildObjData(int addr, int val) {
        int obj = (addr - 0xFE00) >> 2; // convert address to sprite index (0–39)
        if (obj < 40) {
            switch (addr & 3) {
                case 0 -> objdata[obj].y = val - 16;
                case 1 -> objdata[obj].x = val - 8;
                case 2 -> objdata[obj].tile = val;
                case 3 -> {
                    objdata[obj].palette  = (val & 0x10) != 0;
                    objdata[obj].xFlip    = (val & 0x20) != 0;
                    objdata[obj].yFlip    = (val & 0x40) != 0;
                    objdata[obj].priority = (val & 0x80) == 0;
                }
            }
        }
    }


}
