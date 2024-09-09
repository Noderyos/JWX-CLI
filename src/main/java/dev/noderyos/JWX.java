package dev.noderyos;

import java.nio.file.FileSystems;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.text.*;
import java.net.*;

public class JWX {
    final String VERSION = "0.1";
    int max_open_charts = 16;
    DecodeFax decode_fax = null;
    AudioProcessor audio_processor;
    final String app_path, app_name, program_name, user_dir, data_path, chart_path, init_path, file_sep;
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

    public JWX(String[] args) {
        app_name = getClass().getSimpleName();
        URL url = getClass().getResource(app_name + ".class");
        String temp = url.getPath().replaceFirst("(.*?)!.*", "$1");
        temp = temp.replaceFirst("file:", "");
        app_path = new File(temp).getPath();
        user_dir = System.getProperty("user.home");
        file_sep = FileSystems.getDefault().getSeparator();
        data_path = user_dir + file_sep + "." + app_name;
        chart_path = data_path + file_sep + "charts";
        File f = new File(chart_path);
        if (!f.exists()) {
            f.mkdirs();
        }
        init_path = data_path + file_sep + app_name + ".ini";
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
        // these next two must be created in displayed order
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
        double free = Runtime.getRuntime().freeMemory();
        String str = String.format(
                "time %f reset_target_time %f %s chart %d free mem %.2e " +
                        "integ %.2f wsig %.2f image_line %d sample_count %d audio_read %d " +
                        "bi start %.4f active %d bi end %.4f active %d",
                decode_fax.time_sec,
                reset_target_time,
                decode_fax.state.toString(),
                chart_number,
                free,
                decode_fax.pll_integral,
                decode_fax.wsig,
                decode_fax.image_line,
                decode_fax.sample_count,
                audio_read,
                decode_fax.gstart.value(),
                decode_fax.gstart.active() ? 1 : 0,
                decode_fax.gend.value(),
                decode_fax.gend.active() ? 1 : 0);
        p(str);
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
        String path = chart_path + file_sep + create_date_time_filename();
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
