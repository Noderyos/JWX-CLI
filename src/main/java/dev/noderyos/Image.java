package dev.noderyos;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

final public class Image{

    JWX parent;
    boolean old_scaled;
    int old_cal_phase = -1;
    double calibration_val;

    BufferedImage buffered_image = null;
    java.util.List<byte[]> image_array;
    int height;
    int width;
    byte[] bbuffer = null;
    int bbuffer_height = 0;
    int block_size = 400;
    int old_line = 0;
    File file;
    boolean receiving_fax;
    int translate_val = 0;
    boolean changed = false;

    public Image(JWX p, String path, double cal_val) {
        calibration_val = cal_val;
        parent = p;
        width = parent.default_image_width;
        image_array = new java.util.ArrayList<>();
        old_line = 0;
        file = new File(path);
        process_path();
    }

    public void save_file() {
        if (changed && buffered_image != null && buffered_image.getHeight() > 0) {
            try {
                ImageIO.write(buffered_image, "jpg", file);
                changed = false;
            } catch (IOException e) {
                System.out.println("save file: " + e);
            }
        }
        parent.display.addRow(Arrays.asList("", "", ""));
    }

    public void perform_periodic() {
        if (old_scaled != parent.scaled_images) {
            old_scaled = parent.scaled_images;
        }
        calibrate_actions();
    }

    private void calibrate_actions() {
        if (parent.chart == this && old_cal_phase != parent.calibrate_phase) {
            old_cal_phase = parent.calibrate_phase;
        }
    }

    public void close() {
        save_file();
    }

    private void process_path() {
        try {
            receiving_fax = !file.exists();
            if (receiving_fax) {
                file.createNewFile();
            } else {
                load_file();
            }
        } catch (IOException e) {
            System.out.println("process_path: " + e);
        }
    }

    private void load_file() {
        try {
            buffered_image = ImageIO.read(file);
            width = buffered_image.getWidth();
            height = buffered_image.getHeight();
            load_data_array();
        } catch (IOException e) {
            System.out.println("load file: " + e);
        }
    }

    private void load_data_array() {
        image_array.clear();
        Raster raster = buffered_image.getData();
        DataBufferByte dbb = (DataBufferByte) raster.getDataBuffer();
        byte[][] bb = dbb.getBankData();
        int len = bb[0].length;
        int j = 0;
        byte[] line = new byte[width];
        for (int i = 0; i < len; i++) {
            line[j++] = bb[0][i];
            if (j >= width) {
                image_array.add(line);
                j = 0;
                line = new byte[width];
            }
        }
        update_image(true);
        changed = false;
    }


    public void update_image(boolean erase) {
        height = image_array.size();
        if (erase) {
            bbuffer = null;
        }
        if (bbuffer == null) {
            bbuffer_height = 0;
            old_line = 0;
        }
        if (height > 0 && height > old_line) {
            if (height >= bbuffer_height) {
                while (height >= bbuffer_height) {
                    bbuffer_height += block_size;
                }
                byte[] new_bbuffer = new byte[bbuffer_height * width];
                if (bbuffer != null) {
                    System.arraycopy(bbuffer, 0, new_bbuffer, 0, bbuffer.length);
                }
                bbuffer = new_bbuffer;
            }
            int pos = width * old_line;
            for (int i = old_line; i < height; i++) {
                byte[] line = image_array.get(i);
                System.arraycopy(line, 0, bbuffer, pos, width);
                pos += width;
            }
            old_line = height;
            buffered_image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            DataBuffer dbb = new DataBufferByte(bbuffer, width * height);
            SampleModel sm = buffered_image.getSampleModel();
            Raster r = Raster.createRaster(sm, dbb, null);
            buffered_image.setData(r);
            changed = true;
        }
    }

    public void add_line(byte[] line) {
        if (translate_val != 0) {
            line = translate_line(line, translate_val);
        }
        if (calibration_val != 0.0) {
            int y = image_array.size();
            line = clock_correct_line(line, y, width * calibration_val);
        }
        image_array.add(line);
        changed = true;
    }

    public static byte[] translate_line(byte[] a, int p) {
        int len = a.length;
        p = (len * 8 + p) % len;
        int lp = len - p;
        byte[] x = new byte[len];
        System.arraycopy(a, p, x, 0, lp);
        System.arraycopy(a, 0, x, lp, p);
        return x;
    }

    public static byte[] clock_correct_line(byte[] a, int line, double delta) {
        int len = a.length;
        int p = (int) (delta * line);
        p = (len * 32 + p) % len;
        if (p != 0) {
            int lp = len - p;
            byte[] x = new byte[len];
            System.arraycopy(a, p, x, 0, lp);
            System.arraycopy(a, 0, x, lp, p);
            return x;
        }
        return a;
    }
}
