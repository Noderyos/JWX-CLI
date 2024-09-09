package dev.noderyos.filters;

public class GoertzelFilter {

    private final double samples;
    private final double scaling_factor;

    private final double goertzel_factor;
    private double threshold;
    private double s0, s1, s2, samplecount, value;

    public GoertzelFilter(double frequency, double samples, double threshold) {
        this.samples = samples;
        this.threshold = threshold;
        this.goertzel_factor = 2 * Math.cos(2 * Math.PI * frequency);
        // "scaling factor" gives unit result
        // for unit input level
        this.scaling_factor = 4.0 / (samples * samples);
        reset(true);
    }

    public void process(double v) {
        s0 = v + goertzel_factor * s1 - s2;
        s2 = s1;
        s1 = s0;
        samplecount++;
        if (samplecount >= samples) update();
    }

    void update() {
        value = (s2 * s2 + s1 * s1
                - goertzel_factor * s1 * s2)
                * scaling_factor;
        reset(false);
    }

    public void reset(boolean full) {
        s1 = s2 = samplecount = 0;
        if (full) {
            value = 0;
        }
    }

    public void set_threshold(double v) {
        threshold = v;
    }

    public double value() {
        return value;
    }

    public boolean active() {
        return (value >= threshold);
    }
}
