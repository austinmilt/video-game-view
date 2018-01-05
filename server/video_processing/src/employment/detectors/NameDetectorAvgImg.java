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
import image_libraries.Image;
import training.ann.networks.AverageImage;
import training.math.Matrix2D;
import training.trainers.NameTrainerAvgImg;
import options.Options;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * This class is the detector for hero names.
 *
 * @author Austin Milt
 */
public class NameDetectorAvgImg extends Detector {

    private static Options options;
    static {
        try { options = new Options(); }
        catch (IOException e) {
            System.out.println("Couldnt load options file. Failure is imminent.");
        }
    }

    private File saveTo = null;
    public static final String KEY = options.ND.KEY();
    private Matrix2D sample;

    public NameDetectorAvgImg() throws IOException {
        super(options.ND.NETWORK().getAbsolutePath(), KEY, options.ND.ROI());
        if (options.ND.SAVE()) {
            this.saveTo = new File(options.get_jar().getParentFile(), UUID.randomUUID().toString());
            if (!this.saveTo.mkdirs()) {
                System.out.println("Failed to make output directory for name detection images. Failure is imminent.");
            }
            else {
                System.out.println("Name detection results will be saved to " + this.saveTo.getAbsolutePath());
            }
        }
    }


    public void initialize() throws IOException {
        this.network = AverageImage.load(this.networkPath.getAbsolutePath());
        int[] inputShape = this.network.get_input_shape();
        this.sample = new Matrix2D(inputShape[0], inputShape[1]);
    }


    /**
     * Converts an opencv Mat frame to a Matrix2D (primary object type for detection).
     *
     * @param frame opencv Mat frame
     * @param target target Matrix2D to feed further through detection
     */
    public static void frame_to_matrix2d(Mat frame, Matrix2D target){
        int rows = frame.rows();
        int cols = frame.cols();
        double[] pixel;
        double gray;
        int c = 0;
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){

                // turn the pixel to grayscale and assign to the output
                pixel = frame.get(i, j);
                gray = Image.luminosity((int) pixel[0], (int) pixel[1], (int) pixel[2]) / 255d;
//                gray = 1d - Image.distance_to_white(pixel[0], pixel[1], pixel[2]);
                target.set_by_flat(c, gray);
                c += 1;
            }
        }
    }


    public String detect(Mat frame) throws IOException { return this.detect(frame, null); }


    /**
     * Detects the given object in the frame using the assigned detection object.
     *
     * @param frame opencv Mat frame
     * @param validIDs valid identifiers to return (e.g. a subset of heroes corresponding to a match); pass null to allow all ids
     * @return detected label
     */
    public String detect(Mat frame, Collection<String> validIDs) throws IOException {

        // attempt to initialize if it hasnt been done yet
        if (this.network == null) { this.initialize(); }

        // convert the opencv frame to a version we can use in detection
        NameDetectorAvgImg.frame_to_matrix2d(frame, this.sample);
        NameTrainerAvgImg.prepare_input(this.sample, this.sample);
        String prediction = this.network.predict(this.sample, validIDs);

        // save results of prediction
        if (this.saveTo != null) {
            BufferedImage bwImage = new BufferedImage(sample.c(), sample.r(), BufferedImage.TYPE_BYTE_GRAY);
            double lum;
            double colorScalar = 255d * (1d / sample.max());
            for (int y = 0; y < sample.r(); y++){
                for (int x = 0; x < sample.c(); x++){
                    lum = sample.get(y, x);
                    int rgb = 255 - ((int) (colorScalar*lum));
                    bwImage.setRGB(x, y, (new Color(rgb, rgb, rgb)).getRGB());
                }
            }
            File outputFile = new File(this.saveTo, String.format("%s_%s.png", prediction, UUID.randomUUID().toString()));
            ImageIO.write(bwImage, "png", outputFile);
        }

        return prediction;
    }

}
