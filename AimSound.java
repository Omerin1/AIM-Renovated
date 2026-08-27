package aimclassic;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public final class AimSound {
    private AimSound() {}

    public static void imIn() {
        Thread t = new Thread(() -> tone(880, 70, 0.18), "aim-ding");
        t.setDaemon(true);
        t.start();
    }

    public static void signOn() {
        Thread t = new Thread(() -> {
            tone(523, 90, 0.12);
            tone(784, 140, 0.14);
        }, "aim-signon");
        t.setDaemon(true);
        t.start();
    }

    private static void tone(int hz, int ms, double vol) {
        try {
            float sample = 22050f;
            byte[] buf = new byte[(int) (sample * ms / 1000)];
            for (int i = 0; i < buf.length; i++) {
                double angle = i / (sample / hz) * 2.0 * Math.PI;
                buf[i] = (byte) (Math.sin(angle) * 127 * vol);
            }
            AudioFormat fmt = new AudioFormat(sample, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                line.open(fmt);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
            }
        } catch (Exception ignored) {
        }
    }
}
