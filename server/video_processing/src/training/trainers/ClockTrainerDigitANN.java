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
import image_libraries.ClockImageLibrary;
import image_libraries.Image;
import training.ann.layers.InputLayer;
import training.ann.layers.LeakyReLULayer;
import training.ann.layers.MultilayerPerceptron;
import training.ann.layers.SoftmaxLayer;
import training.ann.networks.ANN;
import training.math.Matrix2D;
import training.samples.Batch;
import training.samples.BatchCollection;
import training.samples.Sample;
import options.Options;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This class trains detection of hero names from video frames (saved as images on the hdd).
 *
 * @author Austin Milt
 */
public class ClockTrainerDigitANN extends Trainer {

    // Constants for determining file name of a clock image
    private static final String HOUR = "h";
    private static final String MIN = "m";
    private static final String SEC = "s";
    private static final String UUIDSEP = "_";
    private static Options options;
    static {
        try { options = new Options(); }
        catch (IOException e) {
            System.out.println("Couldnt load options file. Failure is imminent.");
        }
    }
    private File saveTo = null;


    /** Converts a string identification label to the filename format used for composite clock images. */
    public static String label_to_filename(String label){

        // split the label by the time SEPARATOR and get the hours, minutes, and seconds
        String[] split = label.split(ClockTrainerDigitANN.options.CT.SEPARATOR());
        String hour;
        String min;
        String sec;
        if (split.length == 3) {
            hour = split[0];
            min = split[1];
            sec = split[2];
        }

        else if (split.length == 2){
            hour = null;
            min = split[0];
            sec = split[1];
        }

        else { throw new IllegalArgumentException("Label isnt a recognized clock time."); }


        // create a new filename based on the time
        StringBuilder filename = new StringBuilder();
        if (hour == null) { filename.append(String.format("%s%s", HOUR, "")); }
        else { filename.append(String.format("%s%s", HOUR, hour)); }
        filename.append(String.format("%s%s", MIN, min));
        filename.append(String.format("%s%s", SEC, sec));
        filename.append(UUIDSEP);
        filename.append(UUID.randomUUID().toString());
        return filename.toString();
    }


    /** Converts from the filename format used for clock composite images back into the associated label (i.e. the game clock string). */
    public static String filename_to_label(String filename){

        // pull the hour, minutes, and seconds out of the file name
        String[] split = new File(filename).getName().split(UUIDSEP, 2);
        split = split[0].split(SEC);
        String sec = split[1];
        split = split[0].split(MIN);
        String min = split[1];
        split = split[0].split(HOUR);
        String hour;
        if (split.length == 0) { hour = null; }
        else { hour = split[1]; }

        // combine into the correct label
        String label;
        String separator = ClockTrainerDigitANN.options.CT.SEPARATOR();
        if (hour == null) { label = min + separator + sec; }
        else { label = hour + separator + min + separator + sec; }
        return label;
    }


    /**
     * Converts an opencv Mat frame to a Matrix2D (primary object type for detection).
     *
     * @param frame opencv Mat frame
     * @return new matrix2d same shape as the frame
     */
    public static Matrix2D frame_to_matrix2d(Mat frame) { return frame_to_matrix2d(frame, null); }


    /**
     * Converts an opencv Mat frame to a Matrix2D (primary object type for detection).
     *
     * @param frame opencv Mat frame
     * @param target target Matrix2D to feed further through detection
     * @return target
     */
    public static Matrix2D frame_to_matrix2d(Mat frame, Matrix2D target){
        if (target == null) { target = new Matrix2D(frame.rows(), frame.cols()); }
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
                target.set_by_flat(c, gray);
                c += 1;
            }
        }
        return target;
    }

    /** Prepares a grayscale image for passing into a neural network for detection. */
    public static Matrix2D prepare_input(Matrix2D input){
        return input.flatten();
    }


    private static Matrix2D[] make_splits(Matrix2D sample, int expectedCharCount){
        Matrix2D[] output = new Matrix2D[expectedCharCount];
        Matrix2D remainder = sample;
        Matrix2D colsum;
        int splitIndex;
        for (int i = 0; i < (expectedCharCount-1); i++){
            colsum = remainder.sum(0);
            colsum.set(0, 0, Double.POSITIVE_INFINITY); // never take the first character
            splitIndex = colsum.argmin()[1];
            output[i] = remainder.slice(0, 0, remainder.r(), splitIndex);
            remainder = remainder.slice(0, splitIndex, remainder.r(), remainder.c());
        }
        output[expectedCharCount-1] = remainder;
        return output;
    }


    public static int[][] determine_segment_positions(Matrix2D sample) {
        int[][] output;
        int cols = sample.c();
        int start = cols;
        int end = -1;
        int startSum;
        int endSum;

        // scan the sample and check if any text pixels are detected. If both
        // a start and end are detected, quit and move to the next step
        for (int j = 0; j < cols; j++) {
            startSum = 0;
            endSum = 0;

            // add up the number of text pixels in this column
            for (int i = 0; i < sample.r(); i++) {
                if (sample.get(i, j) > options.CT.THRESHOLD()) { startSum += 1; }
                if (sample.get(i, cols-j-1) > options.CT.THRESHOLD()) { endSum += 1; }
            }

            // if we've detected a start or end, update that
            if ((start == cols) && (startSum >= 2)) { start = j; }
            if ((end == -1) && (endSum >= 2)) { end = (cols - j); }

            // if the start is less than the end, we've found the bounds and can quit
            if (start < end) { break; }
        }

        // if no bounds were found, there's no text and we can return an empty
        // result. Otherwise, determine if the clock is in the single, double, or
        // triple digits of minutes and return segment positions based on that
        int width = end - start;
        if (width <= 0) { output = new int[0][0]; }
        else if (width >= options.CT.WIDTH_100()) { output = options.CT.SEGMENT_100(); }
        else if (width >= options.CT.WIDTH_010()) { output = options.CT.SEGMENT_010(); }
        else { output = options.CT.SEGMENT_001(); }
        //                                                     MINUTES                            SECONDS
        //                                           hundreds    tens      ones       :        tens      ones
//        else if (width >= 26) { output = new int[][]{{11, 16}, {17, 22}, {22, 27}, {27, 30}, {31, 35}, {36, 41}}; }
//        else if (width >= 21) { output = new int[][]{          {14, 19}, {19, 24}, {25, 28}, {28, 33}, {33, 39}}; }
//        else {                  output = new int[][]{                    {18, 23}, {23, 27}, {27, 32}, {32, 38}}; }
        return output;
    }


    public static ArrayList<Matrix2D> segment_characters(Matrix2D sample) {
        ArrayList<Matrix2D> output = new ArrayList<>();
        int[][] segmentPositions = determine_segment_positions(sample);

        // for each of the segments, only take columns between start and end of
        // text pixels
        for (int[] segmentPosition : segmentPositions) {
            boolean sampling = false;
            Matrix2D segment = new Matrix2D(sample.r(), 5);
            int c = 0;
            double v;
            for (int j = segmentPosition[0]; j < segmentPosition[1]; j++){

                // determine if we should start sampling (if no pixels have been
                // determined to be text yet)
                if (!sampling) {
                    int pixels = 0;
                    for (int i = 0; i < sample.r(); i++) {
                        if (sample.get(i, j) > options.CT.THRESHOLD()) {
                            pixels += 1;
                        }
                        if (pixels >= 1) {
                            sampling = true;
                            break;
                        }
                    }
                }

                // sample this text column. Only take the values of text;
                // everything else gets zeroes
                if (sampling) {
                    for (int i = 0; i < sample.r(); i++) {
                        v = sample.get(i, j);
                        if (v > options.CT.THRESHOLD()) {
                            segment.set(i, c, v);
                        }
                    }
                    c += 1;
                }
            }

            // add the segment to the output
            output.add(segment);
        }
        return output;
    }




    /** Saves the frame to the correct directory as a training image. */
    public String save_image(Mat frame, String label){
        String trimLabel = label.trim();
        File trainingDir = ClockTrainerDigitANN.options.CT.TRAINING_DIR();
        trainingDir.mkdirs();
        String filename = label_to_filename(trimLabel) + ClockTrainerDigitANN.options.CT.IMAGE_FORMAT();
        String outpath = new File(trainingDir, filename).getAbsolutePath();
        Imgcodecs.imwrite(outpath, frame);
        return outpath;
    }

    /**
     * Clusters examples for a single character into separate glyph groups to assist
     * differentiation in detection.
     *
     * @param subsampleMats character examples for a single character to be clustered
     * @param clusters number of clusters to build (should generally be 3-5)
     * @return mapping from sample index to cluster number
     * @note based on https://en.wikipedia.org/wiki/Hierarchical_clustering
     */
    public HashMap<Integer, Integer> cluster_subsamples(ArrayList<Matrix2D> subsampleMats, int clusters) {

        // initialize clusters by giving every example its own cluster,
        // also cache the pairwise distance between examples
        HashMap<Integer, HashSet<Integer>> clusterGroups = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Double>> pairwiseDistances = new HashMap<>();
        for (int i = 0; i < subsampleMats.size(); i++) {
            HashSet<Integer> singleton = new HashSet<>();
            singleton.add(i);
            clusterGroups.put(i, singleton);
            if (!pairwiseDistances.containsKey(i)) { pairwiseDistances.put(i, new HashMap<>()); }
            HashMap<Integer, Double> thisDistances = pairwiseDistances.get(i);
            for (int j = i+1; j < subsampleMats.size(); j++) {
                double distance = subsampleMats.get(i).euclidean_distance(subsampleMats.get(j));
                thisDistances.put(j, distance);
                if (!pairwiseDistances.containsKey(j)) { pairwiseDistances.put(j, new HashMap<>()); }
                HashMap<Integer, Double> thatDistances = pairwiseDistances.get(j);
                thatDistances.put(i, distance);
            }
        }


        // merge clusters until we have the desired number. To merge, find
        // each cluster's nearest neighbor and the two that are the closest
        // are merged
        while (clusterGroups.size() > clusters) {

            // get the mean distance between elements of each cluster to
            // determine which to merge
            double minDistance = Double.POSITIVE_INFINITY;
            double distance;
            int merge1 = 0;
            int merge2 = 1;
            int cluster1;
            int cluster2;
            Integer[] clusterIDs = clusterGroups.keySet().toArray(new Integer[0]);
            for (int i = 0; i < (clusterGroups.size() - 1); i++) {
                cluster1 = clusterIDs[i];
                for (int j = i+1; j < clusterGroups.size(); j++) {
                    cluster2 = clusterIDs[j];

                    // calculate average pairwise distance between elements of
                    // each group
                    distance = 0d;
                    double count = 0d;
                    for (int ii : clusterGroups.get(cluster1)) {
                        for (int jj : clusterGroups.get(cluster2)) {
                            distance += pairwiseDistances.get(ii).get(jj);
                            count += 1d;
                        }
                    }
                    distance /= count;

                    // update minimum distance
                    if (distance < minDistance) {
                        minDistance = distance;
                        merge1 = cluster1;
                        merge2 = cluster2;
                    }
                }
            }

            // merge closest groups
            clusterGroups.get(merge1).addAll(clusterGroups.get(merge2));
            clusterGroups.remove(merge2);
        }

        // assign original inputs based on final clustering
        HashMap<Integer, Integer> assignments = new HashMap<>(clusters);
        int c = 0;
        for (HashSet<Integer> clusterGroup : clusterGroups.values()) {
            for (int i : clusterGroup) { assignments.put(i, c); }
            c += 1;
        }

        return assignments;
    }


    /** Performs training and saves the trained detector to disk. */
    public void train() throws IOException {

        if (options.CT.SAVE()) {
            this.saveTo = new File(options.get_jar().getParentFile(), UUID.randomUUID().toString());
            if (!this.saveTo.mkdirs()) {
                System.out.println("Failed to make output directory for clock training images. Failure is imminent.");
            }
            else {
                System.out.println("Clock training glyphs will be saved to " + this.saveTo.getAbsolutePath());
            }
        }

        // Load the NameImageLibrary
        System.out.println("Loading image library.");
        ClockImageLibrary library;
        File trainingDir = ClockTrainerDigitANN.options.CT.TRAINING_DIR();
        String search = ClockTrainerDigitANN.options.CT.SEARCH();
        library = ClockImageLibrary.make_from_imagedir(trainingDir.getAbsolutePath(), search);

        // grab all character examples to create samples from
        System.out.println("Segmenting clock examples into separate characters.");
        Set<String> ids = library.get_names();
        ArrayList<Matrix2D> subsamples;
        String charString;
        Image img;
        int nChar;
        HashMap<String, ArrayList<Matrix2D>> characterExamples = new HashMap<>();
        for (String id : ids) {

            // convert the composite character image to a series of subsamples
            img = library.get(id);
            nChar = id.length();
            subsamples = ClockTrainerDigitANN.segment_characters(img.to_Matrix2D_grayscale());

            // if the number of subsamples isnt the size expected, throw
            ArrayList<String> sampleLabels = new ArrayList<>();
            if (nChar != subsamples.size()) {
                String msg = String.format("Number of segmented characters (%d) doesnt match the number " +
                        "of characters in the label (%s). Guessing at overlapping labels.", subsamples.size(), id);
                throw new RuntimeException(msg);
            }

            // Got the correct number of segments, so each segment should correspond to a single label
            else {
                for (int i = 0; i < nChar; i++) {
                    subsamples.get(i).set_attribute("origin_id", id);
                    sampleLabels.add(id.substring(i, i + 1));
                }
            }

            // add to the character examples
            for (int i = 0; i < sampleLabels.size(); i++) {
                charString = sampleLabels.get(i);
                if (!characterExamples.containsKey(charString)) {
                    characterExamples.put(charString, new ArrayList<>());
                }
                characterExamples.get(charString).add(subsamples.get(i));
            }
        }


        // To avoid biasing toward one character or another, take the same number of examples
        // for every character
        System.out.printf("Taking equal number of examples per character.");
        int charExamplesToTake = Integer.MAX_VALUE;
        for (ArrayList<Matrix2D> examples: characterExamples.values()) {
            if (examples.size() < charExamplesToTake) { charExamplesToTake = examples.size(); }
        }
        HashMap<String, ArrayList<Matrix2D>> equalCountExamples = new HashMap<>();
        for (String charKey : characterExamples.keySet()) {
            List<Integer> order = Arrays.stream(Batch.range(characterExamples.get(charKey).size())).boxed().collect(Collectors.toList());
            Collections.shuffle(order);
            int c = 0;
            ArrayList<Matrix2D> finalCharExamples = new ArrayList<>(charExamplesToTake);
            while (c < charExamplesToTake) { finalCharExamples.add(characterExamples.get(charKey).get(order.get(c++))); }
            equalCountExamples.put(charKey, finalCharExamples);
        }

        // cluster character examples into different glyphs to facilitate detection
        // Create separate output labels for each of these glyphs.
        // Create samples for feeding through training.
        int finalExampleCount = charExamplesToTake*characterExamples.size();
        Sample[] samples = new Sample[finalExampleCount];
        HashMap<String, Matrix2D> name2Label = new HashMap<>();
        int L = 0;
        int c = 0;
        String[] glyphVersions = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n"};
        int CLUSTERS = options.CT.GLYPHS();
        int labelSize = equalCountExamples.keySet().size()*CLUSTERS;
        for (String charKey : equalCountExamples.keySet()) {

            // cluster
            System.out.print(String.format("\rClustering '%s' into %d representative glyphs and building samples.", charKey, CLUSTERS));
            HashMap<Integer, Integer> clusterAssignments = cluster_subsamples(equalCountExamples.get(charKey), CLUSTERS);

            // create label vectors for output of detection
            for (int i = 0; i < CLUSTERS; i++) {
                Matrix2D newLabel = new Matrix2D(labelSize, 1);
                newLabel.set_by_flat(L, 1d);
                L += 1;
                name2Label.put(charKey + glyphVersions[i], newLabel);
            }

            // build samples
            for (int i : clusterAssignments.keySet()) {
                int assignment = clusterAssignments.get(i);
                Matrix2D example = equalCountExamples.get(charKey).get(i);
                String glyphKey = charKey + glyphVersions[assignment];
                Matrix2D labelMat = name2Label.get(glyphKey);
                samples[c++] = new Sample(ClockTrainerDigitANN.prepare_input(example), labelMat, glyphKey);

                // save all samples as images
                if (this.saveTo != null) {
                    BufferedImage bwImage = new BufferedImage(example.c(), example.r(), BufferedImage.TYPE_BYTE_GRAY);
                    double lum;
                    for (int y = 0; y < example.r(); y++){
                        for (int x = 0; x < example.c(); x++){
                            lum = example.get(y, x);
                            if (lum > options.CT.THRESHOLD()) { bwImage.setRGB(x, y, Color.BLACK.getRGB()); }
                            else { bwImage.setRGB(x, y, Color.WHITE.getRGB()); }
                        }
                    }
                    String originID = ((String) example.get_attribute("origin_id")).replace(":", "x");
                    File outputFile = new File(this.saveTo, String.format("%s_%s_%s.png", glyphKey.replace(":","div"), originID, UUID.randomUUID().toString()));
                    ImageIO.write(bwImage, "png", outputFile);
                }
            }
        }


        // put data into batches for mini-batch processing.
        System.out.println("\nMaking training batches.");
        int N = samples.length;
        int batchSize = ClockTrainerDigitANN.options.CT.BATCH_SIZE();
        int nBatches = N / batchSize;
        BatchCollection[] batches = BatchCollection
                .make_batches(samples, batchSize, nBatches)
                .split(ClockTrainerDigitANN.options.CT.PROP_TRAINING(), ClockTrainerDigitANN.options.CT.PROP_VALIDATION());

        // build network
        System.out.println("Building neural net.");
        int inputs = batches[0].get_inputs();
        int outputs = batches[0].get_outputs();
        int size = batches[0].get_size();
        int layerSize = ClockTrainerDigitANN.options.CT.LAYER_SIZE();
        ANN network = new ANN();
        network.set_rate(ClockTrainerDigitANN.options.CT.LEARNING_RATE());
        network.set_decay(ClockTrainerDigitANN.options.CT.DECAY_RATE());
        network.set_labelmap(name2Label);
        network.add_layer(new InputLayer(inputs, size));
        network.add_layer(new MultilayerPerceptron(inputs, size, layerSize, 0.01));
        network.add_layer(new LeakyReLULayer(layerSize, size, 0.001, ClockTrainerDigitANN.options.CT.DROPOUT_RATE()));
        network.add_layer(new SoftmaxLayer(layerSize, size, outputs, 0.01));

        // train the neural net
        System.out.println("Training neural net.");
        network.train(batches[0], ClockTrainerDigitANN.options.CT.EPOCHS());
        double accuracy = network.validate(batches[1]);
        System.out.println(String.format("\nCorrectly predicted %d%% of samples.", (int) Math.round(accuracy*100d)));

        // save network to disk
        ANN predictor = network.reduce();
        try { predictor.save(options.CD.NETWORK().getAbsolutePath()); }
        catch (IOException e) {
            System.out.println("Couldnt save");
        }
    }


    public static void main(String[] args) throws IOException {
        ClockTrainerDigitANN trainer = new ClockTrainerDigitANN();
        trainer.train();
    }
}
