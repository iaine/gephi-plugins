/*
 * SonicBridge - audio playback engine.
 *
 * Plays a single linked audio file on a dedicated background thread so the
 * Gephi EDT / OpenGL render loop is never blocked. Uses only the JDK
 * javax.sound.sampled API, so no extra Maven dependencies are required for
 * uncompressed formats (WAV / AIFF / AU). See the note in playFile() about
 * MP3/OGG support.
 */
package uk.ac.warwick.cim.sonic;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays linked audio files on a background thread.
 * <p>
 * This is a process-wide singleton (one playback thread at a time). Calling
 * {@link #play(File)} while something is already playing stops the current clip
 * first, so clicking from node to node behaves like a media player rather than
 * layering sounds on top of each other.
 *
 * @author Iain Emsley (SonicBridge)
 */
public final class AudioPlayer {

    private static final Logger LOGGER = Logger.getLogger(AudioPlayer.class.getName());
    private static final AudioPlayer INSTANCE = new AudioPlayer();

    /** The thread currently doing playback, if any. */
    private final AtomicReference<PlaybackThread> current = new AtomicReference<>();

    /** Master volume in the range 0.0 (silent) .. 1.0 (full). */
    private volatile float volume = 1.0f;

    private AudioPlayer() {
    }

    public static AudioPlayer getInstance() {
        return INSTANCE;
    }

    /**
     * Set the master volume, applied to the current and future clips.
     *
     * @param value 0.0 (silent) to 1.0 (full); values are clamped.
     */
    public void setVolume(float value) {
        if (value < 0f) {
            value = 0f;
        } else if (value > 1f) {
            value = 1f;
        }
        this.volume = value;
        // apply live to a clip already playing
        PlaybackThread t = current.get();
        if (t != null) {
            t.applyVolume();
        }
    }

    /** @return the current master volume (0.0 .. 1.0). */
    public float getVolume() {
        return volume;
    }

    /**
     * Stop whatever is playing and start the given file on a new background
     * thread. Returns immediately; playback happens off the calling thread.
     *
     * @param file the audio file to play; ignored if null or missing.
     */
    public void play(File file) {
        stop();

        if (file == null || !file.isFile()) {
            LOGGER.log(Level.WARNING, "Audio file not found: {0}", file);
            return;
        }

        PlaybackThread t = new PlaybackThread(file);
        // record it before starting so a near-instant stop() can find it
        current.set(t);
        t.start();
    }

    /**
     * Stop the current clip if one is playing. Safe to call when nothing is
     * playing.
     */
    public void stop() {
        PlaybackThread t = current.getAndSet(null);
        if (t != null) {
            t.requestStop();
        }
    }

    /** @return true if a clip is currently playing. */
    public boolean isPlaying() {
        PlaybackThread t = current.get();
        return t != null && t.isAlive();
    }

    /**
     * Daemon thread that streams a single file to the sound card in small
     * buffers, checking a stop flag between buffers so it can be interrupted
     * promptly.
     */
    private final class PlaybackThread extends Thread {

        private final File file;
        private volatile boolean stopRequested = false;
        private volatile SourceDataLine line;

        PlaybackThread(File file) {
            super("SonicBridge-Playback-" + file.getName());
            this.file = file;
            // daemon so a half-finished clip never blocks Gephi shutdown
            setDaemon(true);
        }

        void requestStop() {
            stopRequested = true;
            SourceDataLine l = line;
            if (l != null) {
                l.stop();
                l.close();
            }
        }

        /**
         * Apply the player's current volume to this clip's line, if the mixer
         * exposes a gain control. Converts linear 0..1 to decibels.
         */
        void applyVolume() {
            SourceDataLine l = line;
            if (l == null) {
                return;
            }
            try {
                if (l.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    javax.sound.sampled.FloatControl gain =
                            (javax.sound.sampled.FloatControl)
                                    l.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                    float v = volume;
                    float dB;
                    if (v <= 0f) {
                        dB = gain.getMinimum(); // effectively mute
                    } else {
                        // linear amplitude -> dB, clamped to the control's range
                        dB = (float) (20.0 * Math.log10(v));
                        if (dB < gain.getMinimum()) {
                            dB = gain.getMinimum();
                        } else if (dB > gain.getMaximum()) {
                            dB = gain.getMaximum();
                        }
                    }
                    gain.setValue(dB);
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Volume control unavailable on this line", e);
            }
        }

        @Override
        public void run() {
            try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(file)) {

                // Decode to signed PCM so the default mixer can always open a line.
                AudioFormat base = rawStream.getFormat();
                AudioFormat pcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        base.getSampleRate(),
                        16,
                        base.getChannels(),
                        base.getChannels() * 2,
                        base.getSampleRate(),
                        false);

                try (AudioInputStream pcmStream =
                             AudioSystem.getAudioInputStream(pcm, rawStream)) {

                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcm);
                    SourceDataLine l = (SourceDataLine) AudioSystem.getLine(info);
                    line = l;
                    l.open(pcm);
                    l.start();
                    applyVolume();

                    byte[] buffer = new byte[4096];
                    int read;
                    while (!stopRequested
                            && (read = pcmStream.read(buffer, 0, buffer.length)) != -1) {
                        l.write(buffer, 0, read);
                    }

                    if (!stopRequested) {
                        l.drain();
                    }
                    l.stop();
                    l.close();
                }
            } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
                // WAV/AIFF/AU work out of the box. For MP3/OGG, add a JavaSound
                // SPI such as mp3spi/vorbisspi to the module pom and this same
                // code path will pick it up automatically.
                LOGGER.log(Level.WARNING,
                        "Unsupported audio format (add an mp3/ogg SPI for compressed files): "
                                + file, e);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Playback failed for " + file, e);
            } finally {
                // clear the slot if we are still the active thread
                current.compareAndSet(this, null);
            }
        }
    }
}
