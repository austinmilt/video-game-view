/*
Copyright 2018 Austin Walker Milt

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


package employment.detectors;

import org.opencv.core.Mat;
import training.ann.networks.ANN;
import training.math.Matrix2D;
import training.trainers.ClockTrainerDigitANN;
import options.Options;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * This class is the detector for game clock time.
 *
 * @author Austin Milt
 */
public class ClockDetectorDigitANN extends Detector {

    private static Options options;
    static {
        try { options = new Options(); }
        catch (IOException e) {
            System.out.println("Couldnt load options file. Failure is imminent.");
        }
    }
    public static final String KEY = options.CD.KEY();
    private File saveTo = null;
    private Matrix2D preSample;
    private List<String> secTenOptions;
    private List<String> numberOptions;

    public ClockDetectorDigitANN() throws IOException {
        super(options.CD.NETWORK().getAbsolutePath(), KEY, options.CD.ROI());
        if (options.CD.SAVE()) {
            this.saveTo = new File(options.get_jar().getParentFile(), UUID.randomUUID().toString());
            if (!this.saveTo.mkdirs()) {
                System.out.println("Failed to make output directory for clock detection images. Failure is imminent.");
            }
            else {
                System.out.println("Clock detection results will be saved to " + this.saveTo.getAbsolutePath());
            }
        }
    }


    public void initialize() throws IOException {
        this.network = ANN.load(this.networkPath.getAbsolutePath());
        this.secTenOptions = new ArrayList<>();
        this.numberOptions = new ArrayList<>();
        for (String key : ((ANN) this.network).get_id2num().keySet()) {
            if      (key.startsWith("0")) { this.secTenOptions.add(key); this.numberOptions.add(key); }
            else if (key.startsWith("1")) { this.secTenOptions.add(key); this.numberOptions.add(key); }
            else if (key.startsWith("2")) { this.secTenOptions.add(key); this.numberOptions.add(key); }
            else if (key.startsWith("3")) { this.secTenOptions.add(key); this.numberOptions.add(key); }
            else if (key.startsWith("4")) { this.secTenOptions.add(key); this.numberOptions.add(key); }
            else if (key.startsWith("5")) { this.secTenOptions.add(key); this.numberOptions.add(key); }
            else if (key.startsWith("6")) { this.numberOptions.add(key); }
            else if (key.startsWith("7")) { this.numberOptions.add(key); }
            else if (key.startsWith("8")) { this.numberOptions.add(key); }
            else if (key.startsWith("9")) { this.numberOptions.add(key); }
        }
    }


    public String detect(Mat frame, Collection<String> dummy) throws IOException { return this.detect(frame); }


    /**
     * Detects the given object in the frame using the assigned detection object.
     *
     * @param frame opencv Mat frame
     * @return detected label
     */
    public String detect(Mat frame) throws IOException {

        // dont try detection if the detector hasnt trained yet
        if (this.network == null) { this.initialize(); }

        // convert the opencv frame to a version we can use in detection
        int rows;
        int cols;
        rows = frame.rows();
        cols = frame.cols();
        if (this.preSample == null) { this.preSample = new Matrix2D(rows, cols); }
        ClockTrainerDigitANN.frame_to_matrix2d(frame, this.preSample);

        // segment characters into several samples
        ArrayList<Matrix2D> samples;
        samples = ClockTrainerDigitANN.segment_characters(this.preSample);

        // pass the samples through the network, get their prediction and
        // reassemble predicted label
        StringBuilder label = new StringBuilder();
        Matrix2D sample;
        int dividerIndex = samples.size() - 3;
        int minTenIndex = dividerIndex + 1;
        List<String> validIDs;
        for (int i = 0; i < samples.size(); i++) {
            sample = samples.get(i);
            if (i == dividerIndex) {
                label.append(":");
                continue;
            }
            else if (i == minTenIndex) { validIDs = this.secTenOptions; }
            else { validIDs = this.numberOptions; }
            String glyphID = this.network.predict(ClockTrainerDigitANN.prepare_input(sample), validIDs);
            if (glyphID.length() > 0) {
                label.append(glyphID.substring(0,1));

                // save results of prediction
                if (this.saveTo != null) {
                    BufferedImage bwImage = new BufferedImage(sample.c(), sample.r(), BufferedImage.TYPE_BYTE_GRAY);
                    double lum;
                    for (int y = 0; y < sample.r(); y++) {
                        for (int x = 0; x < sample.c(); x++) {
                            lum = sample.get(y, x);
                            if (lum > options.CT.THRESHOLD()) {
                                bwImage.setRGB(x, y, Color.BLACK.getRGB());
                            } else {
                                bwImage.setRGB(x, y, Color.WHITE.getRGB());
                            }
                        }
                    }
                    File outputFile = new File(this.saveTo, String.format("%s_%s.png", glyphID.replace(":", "div"), UUID.randomUUID().toString()));
                    ImageIO.write(bwImage, "png", outputFile);
                }
            }
        }
        return label.toString();
    }

}
