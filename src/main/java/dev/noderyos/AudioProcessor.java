package dev.noderyos;

import javax.sound.sampled.*;

final public class AudioProcessor {

    JWX parent;
    AudioFormat audioFormat = null;
    final int word_size = 2;
    final int sbufsz = 4096;
    final int bbufsz = sbufsz * word_size;
    boolean read_enable = false;
    AudioInputReader audio_reader = null;
    Line.Info targetLineInfo;
    Line.Info sourceLineInfo;
    boolean read_valid = false;
    java.util.List<Mixer.Info> source_mixer_list, target_mixer_list;
    int source_mixer_count, target_mixer_count;
    int target_mixer_index = -1;

    public AudioProcessor(JWX p) {
        parent = p;
        targetLineInfo = new Line.Info(TargetDataLine.class);
        sourceLineInfo = new Line.Info(SourceDataLine.class);
        Mixer.Info[] mi_list = AudioSystem.getMixerInfo();
        source_mixer_list = new java.util.ArrayList<>();
        target_mixer_list = new java.util.ArrayList<>();
        for (Mixer.Info mi : mi_list) {
            Mixer mixer = AudioSystem.getMixer(mi);
            if (mixer.isLineSupported(targetLineInfo)) {
                target_mixer_list.add(mi);
            }
            if (mixer.isLineSupported(sourceLineInfo)) {
                source_mixer_list.add(mi);
            }
        }
        target_mixer_count = target_mixer_list.size();
        source_mixer_count = source_mixer_list.size();
    }

    public void enable_audio_read(boolean enable) {
        audioFormat = createAudioFormat();
        target_mixer_index = 0;
        read_enable = enable;
        if (enable) {
            audio_reader = new AudioInputReader(this, parent, word_size);
            audio_reader.start();
        } else {
            try {
                if (audio_reader != null) {
                    read_enable = false;
                    audio_reader.join();
                    audio_reader = null;
                }
            } catch (InterruptedException e) {
                System.out.println("join audio thread: " + e);
                read_enable = false;
            }
        }
    }

    AudioFormat createAudioFormat() {
        int sampleSizeInBits = 8 * word_size;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        float rate = 24000;
        return new AudioFormat(
                rate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian);
    }
}
