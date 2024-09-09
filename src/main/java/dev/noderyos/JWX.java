package dev.noderyos;

import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.text.*;

public class JWX {
    final String VERSION = "0.1";
    int max_open_charts = 16;
    DecodeFax decode_fax = null;
    AudioProcessor audio_processor;
    final String app_name, program_name;
    // the default image width is based on the IOC = Index of Coooperation
    // IOC 576 originates in the old mechanical fax drum diameter, and 576 * pi = 1809.557
    int default_image_width = 1810;
    int timer_period_ms = 500;
    long old_samplecount = 0;
    List<Image> chart_list;
    int chart_number = 0;
    Timer periodic_timer;
    Image chart = null;
    boolean scaled_images;
    int calibrate_phase = 0;
    int audio_read = 0;
    double reset_target_time;

    public TableDisplay display;

    public JWX(String[] args) {
        display = new TableDisplay();
        display.setHeader(Arrays.asList("Status", "Date", "Lines"));
        app_name = getClass().getSimpleName();
        program_name = app_name + " " + VERSION;
        chart_list = new ArrayList<>();
        audio_processor = new AudioProcessor(this);
        setup_values();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                inner_close();
            }
        });

        decode_fax = new DecodeFax(this);
        decode_fax.init_chart_read(true);
        periodic_timer = new Timer();
        periodic_timer.scheduleAtFixedRate(new PeriodicEvents(), 500, timer_period_ms);
    }

    class PeriodicEvents extends TimerTask {
        @Override
        public void run() {
            chart_list.forEach(Image::perform_periodic);
            decode_fax.periodic_actions();
            set_frequency_status();
            display();
        }
    }

    private void display() {
        switch (decode_fax.state) {
            case WAITSIG:
                display.removeRow(-1);
                display.addRow(Arrays.asList("Waiting for signal ...", "", ""));
                break;
            case WAITSTB:
            case WAITSTE:
                display.removeRow(-1);
                display.addRow(Arrays.asList("Waiting for sync ...", "", ""));
                break;
            case SYNC:
                display.removeRow(-1);
                display.addRow(Arrays.asList("Syncing ...", "", ""));
                break;
            case PROC:
                display.removeRow(-1);
                display.addRow(Arrays.asList("Receiving ...", chart.file.getName(), String.valueOf(decode_fax.image_line)));
                break;
        }
        display.display();
    }

    private void set_frequency_status() {
        double delta = (decode_fax.sample_count - old_samplecount) / (double) decode_fax.sample_rate;
        if (delta != 0) {
            old_samplecount = decode_fax.sample_count;
            decode_fax.frequency_meter_cycles = 0;
        }
    }


    private void setup_values() {
    }

    public Image new_chart(String path, double cal_val) {
        Image panel = new Image(this, path, cal_val);
        chart_list.add(panel);
        chart_number++;
        chart = panel;
        while (chart_list.size() > max_open_charts) {
            Image cp = chart_list.remove(0);
            cp.close();
        }
        return panel;
    }

    public Image new_chart(double cal_val) {
        String path = create_date_time_filename();
        return new_chart(path, cal_val);
    }

    public String create_date_time_filename() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String s = sdf.format(date);
        s = s.replaceFirst("(\\.\\d).*", "$1");
        s = s.replaceAll("-", ".");
        return "chart_" + s + ".jpg";
    }

    public void cancel_calibrate() {
        calibrate_phase = 0;
    }

    // forced close by system
    private void inner_close() {
        decode_fax.init_chart_read(false);
    }

    public <T> void p(T s) {
        System.out.println(s);
    }

    public static void main(final String[] args) {
        new JWX(args);
    }
}
