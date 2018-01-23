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


package training.trainers;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import image_libraries.Image;
import image_libraries.NameImageLibrary;
import training.ann.networks.AverageImage;
import training.math.Matrix2D;
import options.Options;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;


/**
 * This class trains detection of hero names from video frames (saved as images on the hdd).
 *
 * @author Austin Milt
 */
public class NameTrainerAvgImg extends Trainer {

    private static Options options;
    static {
        try { options = new Options(); }
        catch (IOException e) {
            System.out.println("Couldnt load options file. Failure is imminent.");
        }
    }
    private File saveTo = null;


    /** Saves the frame to the correct hero directory as a training image. */
    public String save_image(Mat frame, String label){
        String uppLabel = label.toUpperCase().trim();
        File outdir = new File(options.NT.TRAINING_DIR(), uppLabel);
        outdir.mkdirs();
        String outpath = new File(outdir, String.format("%s.png", UUID.randomUUID().toString())).getAbsolutePath();
        Imgcodecs.imwrite(outpath, frame);
        return outpath;
    }


    /**
     * Does final preparation of sample for passing into the detection network
     * in order to insure all samples are standardized for detection.
     *
     * @param sample sample that needs classification
     * @param target destination matrix to store prepped sample in. If null, a new one is made.
     * @return target
     */
    public static Matrix2D prepare_input(Matrix2D sample, Matrix2D target) {
        if (target == null) { target = new Matrix2D(sample.r(), sample.c()); }

        // make the whole thing sum to 1 (consistency across glyphs)
        sample.divide(sample.sum(), target);

        return target;
    }


    /**
     * Class used to store indices and distances in Dijkstra's algorithm.
     */
    private static class IndexNode {
        private int i;
        private int j;
        private int distance;
        private IndexNode(int i, int j, int distance) {
            this.i = i;
            this.j = j;
            this.distance = distance;
        }
    }


    /**
     * Computes cell-wise distances from foreground pixels using Dijkstra's algorithm.
     *
     * @param binaryImage binary image, where 1 is foreground, 0 is background
     * @return matrix of integer distances from foreground pixels
     */
    private static Matrix2D compute_distance_matrix(Matrix2D binaryImage) {

        // set up the queue by finding all foreground pixels
        ArrayList<IndexNode> queue = new ArrayList<>();
        for (int c = 0; c < binaryImage.size(); c++) {
            if (binaryImage.get_by_flat(c) == 1d) {
                int[] ij = binaryImage.flat_to_nested(c);
                queue.add(new IndexNode(ij[0], ij[1], 0));
            }
        }

        // explore all pixels, moving out from foreground and updating
        // distance as you go
        Matrix2D output = Matrix2D.constant(binaryImage.r(), binaryImage.c(), Double.POSITIVE_INFINITY);
        int[][] neighborhood = new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        int rows = binaryImage.r();
        int cols = binaryImage.c();
        while (queue.size() > 0) {
            IndexNode current = queue.remove(0);
            if (output.get(current.i, current.j) <= current.distance) { continue; } // skip nodes already visited
            output.set(current.i, current.j, current.distance);
            for (int[] nbr : neighborhood) {
                int ii = current.i + nbr[0];
                int jj = current.j + nbr[1];
                if ((ii >= 0) && (ii < rows) && (jj >= 0) && (jj < cols)) {
                    if (output.get(ii, jj) == Double.POSITIVE_INFINITY) {
                        queue.add(new IndexNode(ii, jj, current.distance + 1));
                    }
                }
            }
        }

        return output;
    }


    /**
     * Creates a probability surface where the value in each cell is the probability
     * that that pixel is a foreground pixel.
     *
     * @param protoImage glyph to base the surface on
     * @return as above
     */
    private static Matrix2D compute_probability_surface(Matrix2D protoImage) {

        // get distance matrix for determining decline in probability surface
        Matrix2D binary = protoImage.gt(options.NT.BINARY_THRESHOLD());
        Matrix2D distance = compute_distance_matrix(binary);

        // apply distance decay function to distance matrix
        for (int c = 0; c < distance.size(); c++) {
            distance.set_by_flat(c, Math.pow(distance.get_by_flat(c) + 1d, -1d) * protoImage.get_by_flat(c));
        }

        return distance;

    }

    /** Performs training and saves the trained detector to disk. */
    public void train() throws IOException {

        if (options.NT.SAVE()) {
            this.saveTo = new File(options.get_jar().getParentFile(), UUID.randomUUID().toString());
            if (!this.saveTo.mkdirs()) {
                System.out.println("Failed to make output directory for Name training images. Failure is imminent.");
            }
            else {
                System.out.println("Name training glyphs will be saved to " + this.saveTo.getAbsolutePath());
            }
        }

        // Load the NameImageLibrary
        NameImageLibrary library;
        library = NameImageLibrary.make_from_imagedir(options.NT.TRAINING_DIR().getAbsolutePath(), options.NT.SEARCH());

        // get the average image from the library for each label
        Set<String> names = library.get_names();
        HashMap<String, Matrix2D> name2Proto = new HashMap<>();
        int height = 0;
        int width = 0;
        double c;
        for (String name : names){
            c = 0d;
            for (Image img : library.get(name)) {
                height = img.get_height();
                width = img.get_width();
                if (!name2Proto.containsKey(name)) { name2Proto.put(name, new Matrix2D(height, width)); }
                name2Proto.get(name).add(img.to_Matrix2D_grayscale(), name2Proto.get(name));
//                name2Proto.get(name).add(img.to_Matrix2D_whiteness(), name2Proto.get(name));
                c += 1d;
            }
            name2Proto.get(name).divide(c, name2Proto.get(name));

            // create a probability surface from the glyph to accommodate small
            // shifts in the position of foreground pixels in input samples
            name2Proto.put(name, compute_probability_surface(name2Proto.get(name)));
            name2Proto.put(name, prepare_input(name2Proto.get(name), name2Proto.get(name)));

            // save all samples as images
            if (this.saveTo != null) {
                Matrix2D proto = name2Proto.get(name);
                BufferedImage bwImage = new BufferedImage(proto.c(), proto.r(), BufferedImage.TYPE_BYTE_GRAY);
                double lum;
                double colorScalar = 255d * (1d / proto.max());
                for (int y = 0; y < proto.r(); y++){
                    for (int x = 0; x < proto.c(); x++){
                        lum = proto.get(y, x);
                        int rgb = 255 - ((int) (colorScalar*lum));
                        bwImage.setRGB(x, y, (new Color(rgb, rgb, rgb)).getRGB());
                    }
                }
                File outputFile = new File(this.saveTo, String.format("%s.png", name));
                ImageIO.write(bwImage, "png", outputFile);
            }

        }

        // save detection object to disk
        AverageImage detectionObject = new AverageImage(name2Proto);
        detectionObject.set_threshold(options.NT.BINARY_THRESHOLD());
        try { detectionObject.save(options.ND.NETWORK().getAbsolutePath()); }
        catch (IOException e) {
            System.out.println("Couldnt save");
        }
    }


    public static void main(String[] args) throws IOException {
        NameTrainerAvgImg trainer = new NameTrainerAvgImg();
        trainer.train();
    }
}
