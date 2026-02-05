import javax.sound.sampled.*;

public class APU {
    private Z80 cpu;
    
    // Game Boy clock frequency
    private static final int CPU_CLOCK = 4194304;
    private static final int SAMPLE_RATE = 44100;
    private static final double CYCLES_PER_SAMPLE = (double) CPU_CLOCK / SAMPLE_RATE;
    
    // Frame sequencer (512 Hz, clocked every 8192 T-cycles)
    private static final int FRAME_SEQUENCER_PERIOD = 8192;
    private int frameSequencerCycles;
    private int frameSequencerStep;

    private double filteredLeft = 0, filteredRight = 0;
   private static final double FILTER_ALPHA = 0.999; // Adjust for cutoff
    
    // Channels
    private PulseChannel channel1;
    private PulseChannel channel2;
    private WaveChannel channel3;
    private NoiseChannel channel4;
    
    // Master control registers
    private int nr50;  // Master volume / VIN panning
    private int nr51;  // Sound panning
    private int nr52;  // Sound on/off
    
    // Wave RAM (16 bytes = 32 4-bit samples)
    private int[] waveRam = new int[16];
    
    // Audio output
    private SourceDataLine audioLine;
    private double sampleCycleAccumulator;
    private byte[] sampleBuffer;
    private int sampleBufferPos;
    private static final int BUFFER_SIZE = 512;  // Smaller buffer for less latency
    
    // Audio enabled flag
    private boolean audioEnabled = true;
    
    public APU() {
        channel1 = new PulseChannel(true);   // Channel 1 has sweep
        channel2 = new PulseChannel(false);  // Channel 2 has no sweep
        channel3 = new WaveChannel();
        channel4 = new NoiseChannel();
        
        sampleBuffer = new byte[BUFFER_SIZE * 4];  // 16-bit stereo = 4 bytes per sample
        sampleBufferPos = 0;
        sampleCycleAccumulator = 0;
        
        initAudio();
    }
    
    private void initAudio() {
        try {
            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                16,     // 16-bit
                2,      // Stereo
                4,      // Frame size (2 channels * 2 bytes)
                SAMPLE_RATE,
                false   // Little endian
            );
            
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Audio line not supported! Trying alternative formats...");
                // Try mono
                format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                info = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Mono format also not supported!");
                    audioEnabled = false;
                    return;
                }
            }
            
            // Try to find a suitable mixer
            Mixer selectedMixer = null;
            
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                String name = mixerInfo.getName().toLowerCase();
                
                // Prefer "default" or "alsa_playback" for system sound server routing
                if (name.contains("default") || name.contains("alsa_playback")) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    if (mixer.isLineSupported(info)) {
                        selectedMixer = mixer;
                        break;
                    }
                }
            }
            
            // If no default found, try the first available source data line
            if (selectedMixer == null) {
                for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    if (mixer.isLineSupported(info)) {
                        String name = mixerInfo.getName().toLowerCase();
                        if (name.contains("loopback") || name.contains("snowball")) continue;
                        selectedMixer = mixer;
                        break;
                    }
                }
            }
            
            if (selectedMixer != null) {
                audioLine = (SourceDataLine) selectedMixer.getLine(info);
            } else {
                audioLine = AudioSystem.getSourceDataLine(format);
            }
            
            audioLine.open(format, SAMPLE_RATE / 10 * 4);  // 100ms buffer
            audioLine.start();
        } catch (LineUnavailableException e) {
            System.err.println("Audio initialization failed: " + e.getMessage());
            e.printStackTrace();
            audioEnabled = false;
        } catch (Exception e) {
            System.err.println("Audio error: " + e.getMessage());
            e.printStackTrace();
            audioEnabled = false;
        }
    }
    
    public void setCpu(Z80 cpu) {
        this.cpu = cpu;
    }
    
    public void tick() {
        if (!audioEnabled) return;
        
        int cycles = cpu.registers.t;
        if (cycles == 0) return;
        
        // Check if APU is powered on
        boolean powerOn = (nr52 & 0x80) != 0;
        
        if (powerOn) {
            // Update frame sequencer
            frameSequencerCycles += cycles;
            while (frameSequencerCycles >= FRAME_SEQUENCER_PERIOD) {
                frameSequencerCycles -= FRAME_SEQUENCER_PERIOD;
                clockFrameSequencer();
            }
            
            // Update channel frequency timers
            channel1.tick(cycles);
            channel2.tick(cycles);
            channel3.tick(cycles, waveRam);
            channel4.tick(cycles);
        }
        
        // Generate audio samples at the correct rate
        sampleCycleAccumulator += cycles;
        while (sampleCycleAccumulator >= CYCLES_PER_SAMPLE) {
            sampleCycleAccumulator -= CYCLES_PER_SAMPLE;
            generateSample(powerOn);
        }
    }
    
    private void clockFrameSequencer() {
        // Frame sequencer clocked at 512 Hz
        // Step 0: Length
        // Step 1: -
        // Step 2: Length, Sweep
        // Step 3: -
        // Step 4: Length
        // Step 5: -
        // Step 6: Length, Sweep
        // Step 7: Volume Envelope
        
        switch (frameSequencerStep) {
            case 0:
            case 4:
                channel1.clockLength();
                channel2.clockLength();
                channel3.clockLength();
                channel4.clockLength();
                break;
            case 2:
            case 6:
                channel1.clockLength();
                channel2.clockLength();
                channel3.clockLength();
                channel4.clockLength();
                channel1.clockSweep();
                break;
            case 7:
                channel1.clockEnvelope();
                channel2.clockEnvelope();
                channel4.clockEnvelope();
                break;
        }
        
        frameSequencerStep = (frameSequencerStep + 1) & 7;
    }
    
    private void generateSample(boolean powerOn) {
        int leftSample = 0;
        int rightSample = 0;
        
        if (powerOn) {
            // Get channel outputs (0-15 range)
            int ch1 = channel1.getOutput();
            int ch2 = channel2.getOutput();
            int ch3 = channel3.getOutput();
            int ch4 = channel4.getOutput();
            
            // Mix channels based on NR51 panning
            // Left channel
            if ((nr51 & 0x10) != 0) leftSample += ch1;
            if ((nr51 & 0x20) != 0) leftSample += ch2;
            if ((nr51 & 0x40) != 0) leftSample += ch3;
            if ((nr51 & 0x80) != 0) leftSample += ch4;
            
            // Right channel
            if ((nr51 & 0x01) != 0) rightSample += ch1;
            if ((nr51 & 0x02) != 0) rightSample += ch2;
            if ((nr51 & 0x04) != 0) rightSample += ch3;
            if ((nr51 & 0x08) != 0) rightSample += ch4;
            
            // Apply master volume (NR50)
            int leftVolume = ((nr50 >> 4) & 0x07) + 1;
            int rightVolume = (nr50 & 0x07) + 1;
            
            leftSample = (leftSample * leftVolume);
            rightSample = (rightSample * rightVolume);
        }
        
        // Scale to 16-bit signed range
        // Max per channel is 15, max 4 channels = 60, max volume = 8, so max = 480
        // Scale to use a good portion of the 16-bit range
        leftSample = leftSample * 50;
        rightSample = rightSample * 50;

        filteredLeft = 0;
        filteredRight = 0;
        
        // Apply low-pass filter
        filteredLeft += (leftSample - filteredLeft) * FILTER_ALPHA;
        filteredRight += (rightSample - filteredRight) * FILTER_ALPHA;
        leftSample = (int) filteredLeft;
        rightSample = (int) filteredRight;
        
        // Clamp to 16-bit range
        leftSample = Math.max(-32768, Math.min(32767, leftSample));
        rightSample = Math.max(-32768, Math.min(32767, rightSample));

        
        // Write to buffer (little endian)
        sampleBuffer[sampleBufferPos++] = (byte) (leftSample & 0xFF);
        sampleBuffer[sampleBufferPos++] = (byte) ((leftSample >> 8) & 0xFF);
        sampleBuffer[sampleBufferPos++] = (byte) (rightSample & 0xFF);
        sampleBuffer[sampleBufferPos++] = (byte) ((rightSample >> 8) & 0xFF);
        
        // Flush buffer when full
        if (sampleBufferPos >= sampleBuffer.length) {
            if (audioLine != null && audioLine.isOpen()) {
                audioLine.write(sampleBuffer, 0, sampleBufferPos);
            }
            sampleBufferPos = 0;
        }
    }
    
    public int readByte(int addr) {
        switch (addr) {
            // Channel 1 registers
            case 0xFF10: return channel1.readNR10() | 0x80;
            case 0xFF11: return channel1.readNR11() | 0x3F;
            case 0xFF12: return channel1.readNR12();
            case 0xFF13: return 0xFF;  // NR13 is write-only
            case 0xFF14: return channel1.readNR14() | 0xBF;
            
            // Channel 2 registers
            case 0xFF15: return 0xFF;  // NR20 doesn't exist
            case 0xFF16: return channel2.readNR11() | 0x3F;
            case 0xFF17: return channel2.readNR12();
            case 0xFF18: return 0xFF;  // NR23 is write-only
            case 0xFF19: return channel2.readNR14() | 0xBF;
            
            // Channel 3 registers
            case 0xFF1A: return channel3.readNR30() | 0x7F;
            case 0xFF1B: return 0xFF;  // NR31 is write-only
            case 0xFF1C: return channel3.readNR32() | 0x9F;
            case 0xFF1D: return 0xFF;  // NR33 is write-only
            case 0xFF1E: return channel3.readNR34() | 0xBF;
            
            // Channel 4 registers
            case 0xFF1F: return 0xFF;  // Unused
            case 0xFF20: return 0xFF;  // NR41 is write-only
            case 0xFF21: return channel4.readNR42();
            case 0xFF22: return channel4.readNR43();
            case 0xFF23: return channel4.readNR44() | 0xBF;
            
            // Master control registers
            case 0xFF24: return nr50;
            case 0xFF25: return nr51;
            case 0xFF26: 
                int status = nr52 & 0x80;
                if (channel1.isEnabled()) status |= 0x01;
                if (channel2.isEnabled()) status |= 0x02;
                if (channel3.isEnabled()) status |= 0x04;
                if (channel4.isEnabled()) status |= 0x08;
                return status | 0x70;
            
            // Unused registers
            case 0xFF27: case 0xFF28: case 0xFF29: case 0xFF2A:
            case 0xFF2B: case 0xFF2C: case 0xFF2D: case 0xFF2E: case 0xFF2F:
                return 0xFF;
            
            default:
                // Wave RAM (0xFF30-0xFF3F)
                if (addr >= 0xFF30 && addr <= 0xFF3F) {
                    return waveRam[addr - 0xFF30];
                }
                return 0xFF;
        }
    }
    
    public void writeByte(int addr, int val) {
        val &= 0xFF;
        
        // If APU is powered off, only allow writes to NR52 and wave RAM
        if ((nr52 & 0x80) == 0) {
            if (addr == 0xFF26) {
                nr52 = val & 0x80;
                if ((val & 0x80) != 0) {
                    // Power on - reset frame sequencer
                    frameSequencerStep = 0;
                }
            } else if (addr >= 0xFF30 && addr <= 0xFF3F) {
                waveRam[addr - 0xFF30] = val;
            }
            return;
        }
        
        switch (addr) {
            // Channel 1 registers
            case 0xFF10: channel1.writeNR10(val); break;
            case 0xFF11: channel1.writeNR11(val); break;
            case 0xFF12: channel1.writeNR12(val); break;
            case 0xFF13: channel1.writeNR13(val); break;
            case 0xFF14: channel1.writeNR14(val); break;
            
            // Channel 2 registers
            case 0xFF16: channel2.writeNR11(val); break;
            case 0xFF17: channel2.writeNR12(val); break;
            case 0xFF18: channel2.writeNR13(val); break;
            case 0xFF19: channel2.writeNR14(val); break;
            
            // Channel 3 registers
            case 0xFF1A: channel3.writeNR30(val); break;
            case 0xFF1B: channel3.writeNR31(val); break;
            case 0xFF1C: channel3.writeNR32(val); break;
            case 0xFF1D: channel3.writeNR33(val); break;
            case 0xFF1E: channel3.writeNR34(val); break;
            
            // Channel 4 registers
            case 0xFF20: channel4.writeNR41(val); break;
            case 0xFF21: channel4.writeNR42(val); break;
            case 0xFF22: channel4.writeNR43(val); break;
            case 0xFF23: channel4.writeNR44(val); break;
            
            // Master control registers
            case 0xFF24: nr50 = val; break;
            case 0xFF25: nr51 = val; break;
            case 0xFF26:
                boolean wasOn = (nr52 & 0x80) != 0;
                nr52 = val & 0x80;
                if (wasOn && (nr52 & 0x80) == 0) {
                    // Power off - reset all registers
                    powerOff();
                } else if (!wasOn && (nr52 & 0x80) != 0) {
                    // Power on
                    frameSequencerStep = 0;
                }
                break;
            
            default:
                // Wave RAM (0xFF30-0xFF3F)
                if (addr >= 0xFF30 && addr <= 0xFF3F) {
                    waveRam[addr - 0xFF30] = val;
                }
                break;
        }
    }
    
    private void powerOff() {
        // Reset all registers to 0 when powering off
        channel1.reset();
        channel2.reset();
        channel3.reset();
        channel4.reset();
        nr50 = 0;
        nr51 = 0;
    }
    
    public void reset() {
        frameSequencerCycles = 0;
        frameSequencerStep = 0;
        sampleCycleAccumulator = 0;
        sampleBufferPos = 0;
        
        channel1.reset();
        channel2.reset();
        channel3.reset();
        channel4.reset();
        
        nr50 = 0;
        nr51 = 0;
        nr52 = 0;
        
        for (int i = 0; i < 16; i++) {
            waveRam[i] = 0;
        }
    }
    
    public void close() {
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
    }
    
    // =====================================================
    // Pulse Channel (Channels 1 and 2)
    // =====================================================
    private class PulseChannel {
        private boolean hasSweep;
        
        // Registers
        private int sweepPeriod;
        private boolean sweepNegate;
        private int sweepShift;
        private int duty;
        private int lengthLoad;
        private int envelopeVolume;
        private boolean envelopeAdd;
        private int envelopePeriod;
        private int frequency;
        private boolean lengthEnabled;
        
        // Internal state
        private boolean enabled;
        private boolean dacEnabled;
        private int lengthCounter;
        private int frequencyTimer;
        private int dutyPosition;
        private int currentVolume;
        private int envelopeTimer;
        private int shadowFrequency;
        private int sweepTimer;
        private boolean sweepEnabled;
        private boolean sweepNegateUsed;
        
        // Duty cycle patterns
        private static final int[][] DUTY_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 1},  // 12.5%
            {1, 0, 0, 0, 0, 0, 0, 1},  // 25%
            {1, 0, 0, 0, 0, 1, 1, 1},  // 50%
            {0, 1, 1, 1, 1, 1, 1, 0}   // 75%
        };
        
        public PulseChannel(boolean hasSweep) {
            this.hasSweep = hasSweep;
            reset();
        }
        
        public void reset() {
            sweepPeriod = 0;
            sweepNegate = false;
            sweepShift = 0;
            duty = 0;
            lengthLoad = 0;
            envelopeVolume = 0;
            envelopeAdd = false;
            envelopePeriod = 0;
            frequency = 0;
            lengthEnabled = false;
            
            enabled = false;
            dacEnabled = false;
            lengthCounter = 0;
            frequencyTimer = 0;
            dutyPosition = 0;
            currentVolume = 0;
            envelopeTimer = 0;
            shadowFrequency = 0;
            sweepTimer = 0;
            sweepEnabled = false;
            sweepNegateUsed = false;
        }
        
        public void tick(int cycles) {
            frequencyTimer -= cycles;
            while (frequencyTimer <= 0) {
                frequencyTimer += (2048 - frequency) * 4;
                dutyPosition = (dutyPosition + 1) & 7;
            }
        }
        
        public void clockLength() {
            if (lengthEnabled && lengthCounter > 0) {
                lengthCounter--;
                if (lengthCounter == 0) {
                    enabled = false;
                }
            }
        }
        
        public void clockEnvelope() {
            if (envelopePeriod == 0) return;
            
            if (envelopeTimer > 0) {
                envelopeTimer--;
            }
            
            if (envelopeTimer == 0) {
                envelopeTimer = envelopePeriod;
                if (envelopeAdd && currentVolume < 15) {
                    currentVolume++;
                } else if (!envelopeAdd && currentVolume > 0) {
                    currentVolume--;
                }
            }
        }
        
        public void clockSweep() {
            if (!hasSweep) return;
            
            if (sweepTimer > 0) {
                sweepTimer--;
            }
            
            if (sweepTimer == 0) {
                sweepTimer = sweepPeriod > 0 ? sweepPeriod : 8;
                
                if (sweepEnabled && sweepPeriod > 0) {
                    int newFreq = calculateSweepFrequency();
                    
                    if (newFreq <= 2047 && sweepShift > 0) {
                        frequency = newFreq;
                        shadowFrequency = newFreq;
                        
                        // Do overflow check again
                        calculateSweepFrequency();
                    }
                }
            }
        }
        
        private int calculateSweepFrequency() {
            int newFreq = shadowFrequency >> sweepShift;
            
            if (sweepNegate) {
                newFreq = shadowFrequency - newFreq;
                sweepNegateUsed = true;
            } else {
                newFreq = shadowFrequency + newFreq;
            }
            
            if (newFreq > 2047) {
                enabled = false;
            }
            
            return newFreq;
        }
        
        public int getOutput() {
            if (!enabled || !dacEnabled) {
                return 0;
            }
            
            int sample = DUTY_TABLE[duty][dutyPosition];
            return sample * currentVolume;
        }
        
        public boolean isEnabled() {
            return enabled && dacEnabled;
        }
        
        // Register read/write methods
        public int readNR10() {
            return (sweepPeriod << 4) | (sweepNegate ? 0x08 : 0) | sweepShift;
        }
        
        public void writeNR10(int val) {
            sweepPeriod = (val >> 4) & 0x07;
            boolean newNegate = (val & 0x08) != 0;
            sweepShift = val & 0x07;
            
            // Clearing negate after using it disables channel
            if (sweepNegateUsed && sweepNegate && !newNegate) {
                enabled = false;
            }
            sweepNegate = newNegate;
        }
        
        public int readNR11() {
            return duty << 6;
        }
        
        public void writeNR11(int val) {
            duty = (val >> 6) & 0x03;
            lengthLoad = val & 0x3F;
            lengthCounter = 64 - lengthLoad;
        }
        
        public int readNR12() {
            return (envelopeVolume << 4) | (envelopeAdd ? 0x08 : 0) | envelopePeriod;
        }
        
        public void writeNR12(int val) {
            envelopeVolume = (val >> 4) & 0x0F;
            envelopeAdd = (val & 0x08) != 0;
            envelopePeriod = val & 0x07;
            
            // DAC is enabled if upper 5 bits are not all 0
            dacEnabled = (val & 0xF8) != 0;
            if (!dacEnabled) {
                enabled = false;
            }
        }
        
        public void writeNR13(int val) {
            frequency = (frequency & 0x700) | val;
        }
        
        public int readNR14() {
            return lengthEnabled ? 0x40 : 0;
        }
        
        public void writeNR14(int val) {
            frequency = (frequency & 0xFF) | ((val & 0x07) << 8);
            lengthEnabled = (val & 0x40) != 0;
            
            // Trigger
            if ((val & 0x80) != 0) {
                trigger();
            }
        }
        
        private void trigger() {
            enabled = dacEnabled;
            
            if (lengthCounter == 0) {
                lengthCounter = 64;
            }
            
            frequencyTimer = (2048 - frequency) * 4;
            
            // Envelope
            envelopeTimer = envelopePeriod;
            currentVolume = envelopeVolume;
            
            // Sweep
            if (hasSweep) {
                shadowFrequency = frequency;
                sweepTimer = sweepPeriod > 0 ? sweepPeriod : 8;
                sweepEnabled = sweepPeriod > 0 || sweepShift > 0;
                sweepNegateUsed = false;
                
                if (sweepShift > 0) {
                    calculateSweepFrequency();
                }
            }
        }
    }
    
    // =====================================================
    // Wave Channel (Channel 3)
    // =====================================================
    private class WaveChannel {
        // Registers
        private boolean dacEnabled;
        private int lengthLoad;
        private int volumeCode;
        private int frequency;
        private boolean lengthEnabled;
        
        // Internal state
        private boolean enabled;
        private int lengthCounter;
        private int frequencyTimer;
        private int positionCounter;
        private int sampleBuffer;
        
        public WaveChannel() {
            reset();
        }
        
        public void reset() {
            dacEnabled = false;
            lengthLoad = 0;
            volumeCode = 0;
            frequency = 0;
            lengthEnabled = false;
            
            enabled = false;
            lengthCounter = 0;
            frequencyTimer = 0;
            positionCounter = 0;
            sampleBuffer = 0;
        }
        
        public void tick(int cycles, int[] waveRam) {
            frequencyTimer -= cycles;
            while (frequencyTimer <= 0) {
                frequencyTimer += (2048 - frequency) * 2;
                positionCounter = (positionCounter + 1) & 31;
                
                // Read sample from wave RAM
                int byteIndex = positionCounter / 2;
                int sample = waveRam[byteIndex];
                if ((positionCounter & 1) == 0) {
                    sampleBuffer = (sample >> 4) & 0x0F;
                } else {
                    sampleBuffer = sample & 0x0F;
                }
            }
        }
        
        public void clockLength() {
            if (lengthEnabled && lengthCounter > 0) {
                lengthCounter--;
                if (lengthCounter == 0) {
                    enabled = false;
                }
            }
        }
        
        public int getOutput() {
            if (!enabled || !dacEnabled) {
                return 0;
            }
            
            // Apply volume shift
            int shifted;
            switch (volumeCode) {
                case 0: shifted = 0; break;              // Mute
                case 1: shifted = sampleBuffer; break;   // 100%
                case 2: shifted = sampleBuffer >> 1; break;  // 50%
                case 3: shifted = sampleBuffer >> 2; break;  // 25%
                default: shifted = 0;
            }
            
            return shifted;
        }
        
        public boolean isEnabled() {
            return enabled && dacEnabled;
        }
        
        // Register read/write methods
        public int readNR30() {
            return dacEnabled ? 0x80 : 0;
        }
        
        public void writeNR30(int val) {
            dacEnabled = (val & 0x80) != 0;
            if (!dacEnabled) {
                enabled = false;
            }
        }
        
        public void writeNR31(int val) {
            lengthLoad = val;
            lengthCounter = 256 - lengthLoad;
        }
        
        public int readNR32() {
            return volumeCode << 5;
        }
        
        public void writeNR32(int val) {
            volumeCode = (val >> 5) & 0x03;
        }
        
        public void writeNR33(int val) {
            frequency = (frequency & 0x700) | val;
        }
        
        public int readNR34() {
            return lengthEnabled ? 0x40 : 0;
        }
        
        public void writeNR34(int val) {
            frequency = (frequency & 0xFF) | ((val & 0x07) << 8);
            lengthEnabled = (val & 0x40) != 0;
            
            // Trigger
            if ((val & 0x80) != 0) {
                trigger();
            }
        }
        
        private void trigger() {
            enabled = dacEnabled;
            
            if (lengthCounter == 0) {
                lengthCounter = 256;
            }
            
            frequencyTimer = (2048 - frequency) * 2;
            positionCounter = 0;
        }
    }
    
    // =====================================================
    // Noise Channel (Channel 4)
    // =====================================================
    private class NoiseChannel {
        // Registers
        private int lengthLoad;
        private int envelopeVolume;
        private boolean envelopeAdd;
        private int envelopePeriod;
        private int clockShift;
        private boolean widthMode;  // true = 7-bit, false = 15-bit
        private int divisorCode;
        private boolean lengthEnabled;
        
        // Internal state
        private boolean enabled;
        private boolean dacEnabled;
        private int lengthCounter;
        private int frequencyTimer;
        private int currentVolume;
        private int envelopeTimer;
        private int lfsr;  // Linear Feedback Shift Register
        
        // Divisor lookup table
        private static final int[] DIVISORS = {8, 16, 32, 48, 64, 80, 96, 112};
        
        public NoiseChannel() {
            reset();
        }
        
        public void reset() {
            lengthLoad = 0;
            envelopeVolume = 0;
            envelopeAdd = false;
            envelopePeriod = 0;
            clockShift = 0;
            widthMode = false;
            divisorCode = 0;
            lengthEnabled = false;
            
            enabled = false;
            dacEnabled = false;
            lengthCounter = 0;
            frequencyTimer = 0;
            currentVolume = 0;
            envelopeTimer = 0;
            lfsr = 0x7FFF;  // All 1s
        }
        
        public void tick(int cycles) {
            frequencyTimer -= cycles;
            while (frequencyTimer <= 0) {
                frequencyTimer += DIVISORS[divisorCode] << clockShift;
                
                // Clock the LFSR
                int xorResult = (lfsr & 0x01) ^ ((lfsr >> 1) & 0x01);
                lfsr = (lfsr >> 1) | (xorResult << 14);
                
                if (widthMode) {
                    // 7-bit mode: also set bit 6
                    lfsr &= ~0x40;
                    lfsr |= xorResult << 6;
                }
            }
        }
        
        public void clockLength() {
            if (lengthEnabled && lengthCounter > 0) {
                lengthCounter--;
                if (lengthCounter == 0) {
                    enabled = false;
                }
            }
        }
        
        public void clockEnvelope() {
            if (envelopePeriod == 0) return;
            
            if (envelopeTimer > 0) {
                envelopeTimer--;
            }
            
            if (envelopeTimer == 0) {
                envelopeTimer = envelopePeriod;
                if (envelopeAdd && currentVolume < 15) {
                    currentVolume++;
                } else if (!envelopeAdd && currentVolume > 0) {
                    currentVolume--;
                }
            }
        }
        
        public int getOutput() {
            if (!enabled || !dacEnabled) {
                return 0;
            }
            
            // Output is inverted bit 0 of LFSR
            int sample = (~lfsr) & 0x01;
            return sample * currentVolume;
        }
        
        public boolean isEnabled() {
            return enabled && dacEnabled;
        }
        
        // Register read/write methods
        public void writeNR41(int val) {
            lengthLoad = val & 0x3F;
            lengthCounter = 64 - lengthLoad;
        }
        
        public int readNR42() {
            return (envelopeVolume << 4) | (envelopeAdd ? 0x08 : 0) | envelopePeriod;
        }
        
        public void writeNR42(int val) {
            envelopeVolume = (val >> 4) & 0x0F;
            envelopeAdd = (val & 0x08) != 0;
            envelopePeriod = val & 0x07;
            
            dacEnabled = (val & 0xF8) != 0;
            if (!dacEnabled) {
                enabled = false;
            }
        }
        
        public int readNR43() {
            return (clockShift << 4) | (widthMode ? 0x08 : 0) | divisorCode;
        }
        
        public void writeNR43(int val) {
            clockShift = (val >> 4) & 0x0F;
            widthMode = (val & 0x08) != 0;
            divisorCode = val & 0x07;
        }
        
        public int readNR44() {
            return lengthEnabled ? 0x40 : 0;
        }
        
        public void writeNR44(int val) {
            lengthEnabled = (val & 0x40) != 0;
            
            // Trigger
            if ((val & 0x80) != 0) {
                trigger();
            }
        }
        
        private void trigger() {
            enabled = dacEnabled;
            
            if (lengthCounter == 0) {
                lengthCounter = 64;
            }
            
            frequencyTimer = DIVISORS[divisorCode] << clockShift;
            
            // Envelope
            envelopeTimer = envelopePeriod;
            currentVolume = envelopeVolume;
            
            // Reset LFSR
            lfsr = 0x7FFF;
        }
    }
}
