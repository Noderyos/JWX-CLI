package dev.noderyos;

import javax.sound.sampled.*;
import java.nio.*;

final class AudioInputReader extends Thread {

    AudioProcessor audio_processor;
    JWX parent;
    TargetDataLine targetDataLine = null;
    short[] short_buf;
    byte[] byte_buf;
    int word_size;
    int avail;
    double restart_cycle_time_sec = 3600;

    public AudioInputReader(AudioProcessor ap, JWX p, int ws) {
        audio_processor = ap;
        parent = p;
        word_size = ws;
        byte_buf = new byte[audio_processor.bbufsz];
        short_buf = new short[audio_processor.sbufsz];
    }

    private boolean open_target_line() {
        try {
            if (targetDataLine == null) {
                Mixer.Info mi;
                try {
                    int n = audio_processor.target_mixer_index;
                    mi = audio_processor.target_mixer_list.get(n);
                } catch (Exception e) {
                    System.out.println(e + ", proceeding with index 0");
                    mi = audio_processor.target_mixer_list.get(0);
                }
                Mixer mixer = AudioSystem.getMixer(mi);
                targetDataLine = (TargetDataLine) mixer.getLine(audio_processor.targetLineInfo);
                targetDataLine.open(audio_processor.audioFormat, audio_processor.bbufsz * 2);
                targetDataLine.start();
            }
        } catch (LineUnavailableException e) {
            targetDataLine = null;
            System.out.println("open_target_line: " + e);
            audio_processor.read_valid = false;
            return false;
        }
        return true;
    }

    private void close_target_line() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            targetDataLine = null;
        }
    }

    public void restart_stream_test() {
        if (parent.decode_fax.time_sec > parent.reset_target_time && !parent.decode_fax.receiving_fax()) {
            System.out.println(getClass().getSimpleName() + ": restarting target audio line");
            close_target_line();
            open_target_line();
            parent.reset_target_time = parent.decode_fax.time_sec + restart_cycle_time_sec;
        }
    }

    @Override
    public void run() {
        audio_processor.read_valid = true;
        audio_processor.read_enable = true;
        parent.reset_target_time = -1;
        restart_stream_test();
        try {
            while (targetDataLine != null && audio_processor.read_enable) {
                while (audio_processor.read_enable && (avail = targetDataLine.available()) < audio_processor.bbufsz && avail >= 0) {
                    Thread.sleep(20);
                }
                if (audio_processor.read_enable) {
                    parent.audio_read = targetDataLine.read(byte_buf, 0, audio_processor.bbufsz);
                    if (parent.audio_read > 0) {
                        ShortBuffer sb = ByteBuffer.wrap(byte_buf).asShortBuffer();
                        sb.get(short_buf);
                        parent.decode_fax.process_data(short_buf);
                    }
                }
                restart_stream_test();
            }
            close_target_line();
            audio_processor.read_valid = false;
        } catch (InterruptedException e) {
            System.out.println("audio input reader: " + e);
            close_target_line();
            audio_processor.read_valid = false;
        }
    }
}
