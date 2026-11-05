import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.Random;

// Sound effects. There are no sound files: the code creates the sounds (waves).
public class SoundPlayer {

    private static final float SAMPLE_RATE = 44100f; // samples per second

    private boolean enabled;  // the player can turn the sound off in the settings
    private boolean working;  // becomes false if the computer has no sound device

    private byte[] cardSound;
    private byte[] winSound;
    private byte[] loseSound;
    private byte[] pushSound;
    private byte[] blackjackSound;

    public SoundPlayer() {
        enabled = true;
        working = true;

        cardSound = makeCardSound();
        // the numbers are the frequencies (in Hz) of the notes
        winSound = makeNotes(new double[] {523.25, 659.25, 783.99}, 0.13);              // C E G
        blackjackSound = makeNotes(new double[] {523.25, 659.25, 783.99, 1046.50}, 0.16); // C E G high C
        loseSound = makeNotes(new double[] {392.00, 329.63, 261.63}, 0.20);             // G E C (going down)
        pushSound = makeNotes(new double[] {440.00}, 0.20);                             // A
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // ---------- the sounds of the game ----------

    public void playWin() {
        play(winSound);
    }

    public void playBlackjack() {
        play(blackjackSound);
    }

    public void playLose() {
        play(loseSound);
    }

    public void playPush() {
        play(pushSound);
    }

    // Plays the card sound several times, one after the other
    public void playCards(int number, int gapMillis) {
        if (!enabled || !working) {
            return;
        }
        Thread thread = new Thread(() -> {
            for (int i = 0; i < number; i++) {
                play(cardSound);
                try {
                    Thread.sleep(gapMillis);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        thread.setDaemon(true); // this thread does not keep the program alive
        thread.start();
    }

    // ---------- playing ----------

    private void play(byte[] data) {
        if (!enabled || !working) {
            return;
        }
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false); // 16 bit, mono
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            // when the sound ends, the clip is closed to free the sound device
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start(); // plays in the background
        } catch (Exception e) {
            working = false; // no sound device: the game goes on without sound
        }
    }

    // ---------- creating the waves ----------

    // A short burst of noise that fades fast, like a card on the table
    private byte[] makeCardSound() {
        int samples = (int) (SAMPLE_RATE * 0.07);
        double[] wave = new double[samples];
        Random random = new Random(1);

        for (int i = 0; i < samples; i++) {
            double fadeIn = Math.min(1.0, i / (SAMPLE_RATE * 0.002)); // 2 ms: no click at the start
            double fade = 1.0 - (double) i / samples;
            wave[i] = (random.nextDouble() * 2 - 1) * fadeIn * fade * fade * 0.5;
        }
        return toBytes(wave);
    }

    // Plays the notes one after the other
    private byte[] makeNotes(double[] frequencies, double secondsPerNote) {
        int samplesPerNote = (int) (SAMPLE_RATE * secondsPerNote);
        double[] wave = new double[samplesPerNote * frequencies.length];

        for (int note = 0; note < frequencies.length; note++) {
            for (int i = 0; i < samplesPerNote; i++) {
                double time = i / SAMPLE_RATE;
                double fadeIn = Math.min(1.0, time / 0.005);          // no click at the start
                double fadeOut = Math.exp(-4.0 * i / samplesPerNote); // the note fades away
                double sine = Math.sin(2 * Math.PI * frequencies[note] * time);
                wave[note * samplesPerNote + i] = 0.4 * fadeIn * fadeOut * sine;
            }
        }
        return toBytes(wave);
    }

    // Numbers from -1 to 1 become 16 bit samples (2 bytes each)
    private byte[] toBytes(double[] wave) {
        byte[] bytes = new byte[wave.length * 2];
        for (int i = 0; i < wave.length; i++) {
            short value = (short) (wave[i] * 32767);
            bytes[i * 2] = (byte) (value & 0xFF);          // low byte
            bytes[i * 2 + 1] = (byte) ((value >> 8) & 0xFF); // high byte
        }
        return bytes;
    }
}