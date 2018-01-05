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


package video;

import employment.detectors.ClockDetectorDigitANN;
import employment.detectors.Detector;
import employment.detectors.NameDetectorAvgImg;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import training.trainers.ClockTrainerDigitANN;
import training.trainers.NameTrainerAvgImg;
import training.trainers.Trainer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import static org.opencv.videoio.Videoio.*;

/**
 * This class handles video parsing for creating training images for detectors
 * and detecting video objects during job execution.
 *
 * @author Austin Milt
 */
public class VideoParser implements Iterable<VideoParser.Frame> {
//    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private File file;
    private VideoCapture video;
    private int frames;
    private int fps;
    private int height;
    private int width;
    private Rect[] regions;
    private static final String PYTHON_INF = "inf";
    private static final String PYTHON_NEGINF = "-inf";

    // command-line option flags and defaults
    private static final String TASK_PLAY = "play";
    private static final String TASK_PARSE = "parse";
    private static final String TASK_PARSE_INT = "parse_interactively";
    private static final String TASK_PARSE_FILE = "parse_by_file";
    private static final String TASK_PARSE_AUTO = "parse_automatically";
    private static final String DEFAULT_VIDEO_TASK = "play";

    // module options
    private static options.Options options;
    static {
        try { options = new options.Options(); }
        catch (IOException e) {
            System.out.println("Couldnt load options file. Failure is imminent.");
        }
    }

    // load opencv
    static {
        String libName = "";
        if (SystemUtils.IS_OS_WINDOWS) { libName = options.VP.OPENCV_WINDOWS().getAbsolutePath(); }
        else if (SystemUtils.IS_OS_LINUX) { libName = options.VP.OPENCV_LINUX().getAbsolutePath(); }
        System.load(libName);
    }

    // get some options as constant
    private static final double DEFAULT_SKIP_SECONDS = options.VP.PARSE_SKIP();
    private static final String CSV_FRAMECOL = options.VP.CSV_COL_FRAME();
    private static final String CSV_TIMECOL = options.VP.CSV_COL_TIME();
    private static final String CSV_INTERVALCOL = options.VP.CSV_COL_INTERVAL();
    private static final String CSV_LABELCOL = options.VP.CSV_COL_LABEL();
    private static final String CSV_DETECTORCOL = options.VP.CSV_COL_DETECTOR();
    private static final String CSV_SEPARATOR = options.VP.CSV_SEP_COL();
    private static final String CSV_INTERVAL_SEPARATOR = options.VP.CSV_SEP_INTERVAL();
    private static final String CSV_LABEL_SEPARATOR = options.VP.CSV_SEP_LABEL();

    // build the detectors and trainers
    private Detector[] detectors = make_detectors();
    private Trainer[] trainers = make_trainers();



    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Creates an empty parser. */
    public VideoParser() throws IOException {}


    /**
     * Creates a new video.VideoParser to parse the video at the given path.
     *
     * @param videoPath path to video to parse
     * @throws IOException
     */
    public VideoParser(String videoPath) throws IOException {
        this.file = new File(videoPath);
        this.video = new VideoCapture();
        this.update_stats();
    }



    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR HELPERS ////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Updates stats for the parser based on the video attributes. */
    private void update_stats() throws FileNotFoundException {
        this.open();
        this.frames = (int) this.video.get(CV_CAP_PROP_FRAME_COUNT);
        this.fps = (int) this.video.get(CV_CAP_PROP_FPS);
        this.height = (int) this.video.get(CV_CAP_PROP_FRAME_HEIGHT);
        this.width = (int) this.video.get(CV_CAP_PROP_FRAME_WIDTH);
        this.regions = make_regions();
        this.close();
    }

    /** Makes subframe regions to inspect in the current video based on assigned detectors. */
    private Rect[] make_regions() {
        double[] roi;
        int left;
        int top;
        int width;
        int height;
        Rect[] regions = new Rect[this.detectors.length];
        for (int i = 0; i < this.detectors.length; i++){
            roi = this.detectors[i].get_roi();
            left = (int) Math.floor(roi[0]);
            top = (int) Math.floor(roi[1]);
            width = (int) Math.floor(roi[2]);
            height = (int) Math.floor(roi[3]);
            regions[i] = new Rect(left, top, width, height);
        }
        return regions;
    }

    /** Makes the detectors. */
    private Detector[] make_detectors() throws IOException {
        String[] names = options.VP.DETECTORS();
        Detector[] output = new Detector[names.length];
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(options.ND.NAME)) {
                output[i] = new NameDetectorAvgImg();
            }
            else if (names[i].equals(options.CD.NAME)) {
                output[i] = new ClockDetectorDigitANN();
            }
            else { throw new RuntimeException(String.format("Unknown detector name %s", names[i])); }
        }
        return output;
    }


    /** Makes the trainers. */
    private Trainer[] make_trainers() throws IOException {
        String[] names = options.VP.DETECTORS();
        Trainer[] output = new Trainer[names.length];
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(options.ND.NAME)) {
                output[i] = new NameTrainerAvgImg();
            }
            else if (names[i].equals(options.CD.NAME)) {
                output[i] = new ClockTrainerDigitANN();
            }
            else { throw new RuntimeException(String.format("Unknown detector name %s", names[i])); }
        }
        return output;
    }



    ///////////////////////////////////////////////////////////////////////////
    // OPENCV WRAPPERS ////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Wrapper for cap.isOpened() */
    public boolean is_open() { return this.video.isOpened(); }


    /** Wrapper for opencv.open() */
    public void open(){
        this.video.open(this.file.getAbsolutePath());
        if (!this.is_open()){ throw new RuntimeException("Unable to open the file. Might not be a valid path, or maybe missing opencv dlls (e.g. ffmpeg<stuff>.dll."); }
    }


    /** Wrapper for opencv.release() */
    public void close(){ this.video.release(); }


    /** Wrapper for cap.get(CV_CAP_PROP_POS_MSEC) */
    public double current_time() { return this.video.get(CV_CAP_PROP_POS_MSEC); }


    /** Wrapper for cap.get(CV_CAP_PROP_POS_FRAMES ) */
    public int current_index() { return (int) this.video.get(CV_CAP_PROP_POS_FRAMES); }



    ///////////////////////////////////////////////////////////////////////////
    // VIDEO PARSING //////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Passes current frame through all detectors and returns the detected labels. */
    public String[] detect(Mat frame) throws IOException {String[] output = new String[this.regions.length]; detect(frame, output); return output; }
    public void detect(Mat frame, String[] target) throws IOException {
        for (int i = 0; i < this.regions.length; i++){
            target[i] = this.detectors[i].detect(frame.submat(this.regions[i]));
        }
    }


    /** Parses the video to detect objects. See <code>parse(skip, validLabels)</code>. */
    public ArrayList<ParserResult> parse() throws IOException { return parse(this.fps); }


    /** Parses the video to detect objects. See <code>parse(skip, validLabels)</code>. */
    public ArrayList<ParserResult> parse(int skip) throws IOException { return parse(skip, null); }


    /**
     * Parses the video to detect objects. Detection depends on detectors in this.detectors.
     *
     * @param skip number of frames to skip between each round of detection
     * @param validLabels map of valid labels for each detector at any specified intervals as returned by <code>load_valid_labels</code>. Unspecified intervals assume all labels are valid.
     * @return list of nested ParserResults, one for each round of detection,
     *      each containing results for every detector
     */
    public ArrayList<ParserResult> parse(int skip, HashMap<String, OrderedIntervals> validLabels) throws IOException {
        ArrayList<ParserResult> outputs = new ArrayList<>();
        String label;
        Frame fullFrame;
        Mat roiFrame;
        ParserResult curResult;
        FrameIterator frameIter = new FrameIterator(this, skip);
        double videoTime;
        String detectorKey;
        List<String> detectorValidLabels;
        while (frameIter.hasNext()){
            fullFrame = frameIter.next();
            videoTime = fullFrame.time() / 1000d;
            curResult = new ParserResult(videoTime, fullFrame.index());
            for (int i = 0; i < this.regions.length; i++){
                detectorKey = this.detectors[i].get_key();
                detectorValidLabels = null;
                if ((validLabels != null) && validLabels.containsKey(detectorKey)) {
                    detectorValidLabels = Arrays.asList((String[]) validLabels.get(detectorKey).query_time(videoTime).get_data());
                }
                roiFrame = fullFrame.mat().submat(this.regions[i]);
                label = "";
                label = this.detectors[i].detect(roiFrame, detectorValidLabels);
                curResult.put(this.detectors[i].get_key(), label);
            }
            outputs.add(curResult);
            System.out.print(String.format("\rParsing video. %3.0f%% complete.", 100d*((double) fullFrame.index())/frameIter.n));
        }
        System.out.println("\rParsing video. 100% complete.");
        return outputs;
    }


    /**
     * Saves the results of parse() to a csv.
     *
     * @param results results as output by <code>parse</code>
     * @param outFile path to save outputs
     * @throws IOException when the output file cant be written to
     */
    public static void save_detection_results(ArrayList<ParserResult> results, String outFile) throws IOException {

        // build the header row of the file
        String[] detectorKeys = results.get(0).results.keySet().toArray(new String[0]);
        String header = String.join(CSV_SEPARATOR, CSV_FRAMECOL, CSV_TIMECOL, String.join(CSV_SEPARATOR, detectorKeys));

        // write header and results
        FileWriter writer = new FileWriter(outFile);
        writer.write(header);
        String[] row = new String[detectorKeys.length+2];
        for (ParserResult r : results) {
            row[0] = String.format("%d", r.frame());
            row[1] = String.format("%f", r.time());
            for (int i = 0; i < detectorKeys.length; i++){
                row[i+2] = r.get(detectorKeys[i]);
            }
            writer.write("\n" + String.join(CSV_SEPARATOR, row));
        }
        writer.close();
    }


    /** Attemps to automatically detect training images based on existing detectors, once every second. */
    public ArrayList<String> autodetect_training_images() throws IOException { return autodetect_training_images(this.fps); }


    /**
     * Attempts to automatically detect training images based on existing detectors.
     *
     * @param skip number of frames to skip between each detection
     * @return paths to saved image files
     */
    public ArrayList<String> autodetect_training_images(int skip) throws IOException {

        // For each frame, get the regions of interest for detection and do
        // detection, then save the image to the output location
        ArrayList<String> outputs = new ArrayList<>();
        String key;
        Frame fullFrame;
        Mat roiFrame;
        FrameIterator frameIter = new FrameIterator(this, skip);
        while (frameIter.hasNext()){
            fullFrame = frameIter.next();
            for (int i = 0; i < this.regions.length; i++){
                roiFrame = fullFrame.mat().submat(this.regions[i]);
                key = this.detectors[i].detect(roiFrame);
                System.out.println(String.format("frame %f, name %s", fullFrame.time()/1000d, key));
                outputs.add(this.trainers[i].save_image(roiFrame, key));
            }
        }
        return outputs;
    }


    /** Converts a Mat to a BufferedImage for display */
    private static BufferedImage mat_to_img(Mat imgContainer) {
        MatOfByte byteMatData = new MatOfByte();
        Imgcodecs.imencode(".jpg", imgContainer, byteMatData);
        byte[] byteArray = byteMatData.toArray();
        BufferedImage img= null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            img = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return img;
    }


    /** Displays an image. */
    public static JFrame display_image(BufferedImage image) {
        JFrame window = new JFrame();
        if(image != null) {
            window.setTitle("Image Viewer");
            window.setLocationRelativeTo(null);
            JLabel label = new JLabel(new ImageIcon(image.getScaledInstance(image.getWidth()*4, image.getHeight()*4, Image.SCALE_SMOOTH)));
            KeyAdapter listener = new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        window.dispose();
                    }
                }
            };
            window.addKeyListener(listener);
            window.add(label);
            window.pack();
            window.setVisible(true);
            window.setAlwaysOnTop(true);
        }
        return window;
    }


    /** Displays a frame to the user and asks for the correct label. */
    public String query_user(Mat frame){
        BufferedImage img = mat_to_img(frame);
        JFrame window = display_image(img);
        System.out.print("Label for this image (leave blank to skip): ");
        Scanner scanner = new Scanner(System.in);
        String label = scanner.nextLine().toUpperCase().trim();
        if (label.equals("")) { label = null; } // skip non-answers
        window.setVisible(false);
        window.dispose();
        return label;
    }


    /**
     * Interactively queries the user for identification of training images.
     *
     * @param skip number of frames to skip between each query
     * @return paths to saved image files
     */
    public ArrayList<String> parse_training_images_iteractively(int skip){
        ArrayList<String> outputs = new ArrayList<>();
        String key;
        Frame fullFrame;
        Mat roiFrame;
        FrameIterator frameIter = new FrameIterator(this, skip);
        while (frameIter.hasNext()){
            fullFrame = frameIter.next();
            for (int i = 0; i < this.regions.length; i++){
                roiFrame = fullFrame.mat().submat(this.regions[i]);
                key = query_user(roiFrame);
                if (key == null) {
                    System.out.println("Skipping this image...");
                    continue;
                }
                outputs.add(this.trainers[i].save_image(roiFrame, key));
            }
        }
        return outputs;
    }


    private static String[] split_and_trim_string(String string, String delimiter){
        String[] output = string.split(delimiter);
        for (int i = 0; i < output.length; i++) { output[i] = output[i].trim(); }
        return output;
    }


    private static double cast_python_value(String string) {
        string = string.trim();
        if (string.equals(PYTHON_NEGINF)) { return Double.NEGATIVE_INFINITY; }
        else if (string.equals(PYTHON_INF)) { return Double.POSITIVE_INFINITY; }
        else { return Double.valueOf(string); }
    }


    private static double[] split_and_cast_interval(String string) {
        String[] splitted = string.split(CSV_INTERVAL_SEPARATOR);
        return new double[]{ cast_python_value(splitted[0]), cast_python_value(splitted[1]) };
    }


    // Load timings of frame definitions for parsing videos by timing files.
    private HashMap<Integer, String[]> load_timing_csv(String csvPath) throws IOException {
        HashMap<Integer, String[]> output = new HashMap<>();
        FileReader fileReader = new FileReader(csvPath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String[] row = split_and_trim_string(bufferedReader.readLine(), CSV_SEPARATOR);
        HashMap<String, Integer> c2i = new HashMap<>();
        for (int i = 0; i < row.length; i++) { c2i.put(row[i], i); }
        String line;
        int frame;
        String detectorKey;
        String detectorLabel;
        while((line = bufferedReader.readLine()) != null) {
            row = split_and_trim_string(line, CSV_SEPARATOR);
            frame = Integer.valueOf(row[c2i.get(CSV_FRAMECOL)]);
            output.put(frame, new String[this.detectors.length]);

            // try to find the label for every detector at this frame. If no
            // label is given (missing column or blank), skip the detector (set to null)
            for (int i = 0; i < this.detectors.length; i++) {
                detectorKey = this.detectors[i].get_key();
                detectorLabel = null;
                if (c2i.containsKey(detectorKey)) {
                    detectorLabel = row[c2i.get(detectorKey)];
                    if (detectorLabel.equals("")) {
                        detectorLabel = null;
                    }
                }
                output.get(frame)[i] = detectorLabel;
            }
        }
        bufferedReader.close();
        return output;
    }


    // Load valid labels at various times throughout the video to be processed
    private HashMap<String, OrderedIntervals> load_valid_labels(String csvPath) throws IOException {
        FileReader fileReader = new FileReader(csvPath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String[] row = split_and_trim_string(bufferedReader.readLine(), CSV_SEPARATOR);
        HashMap<String, Integer> c2i = new HashMap<>();
        for (int i = 0; i < row.length; i++) { c2i.put(row[i], i); }
        String line;
        double[] intervalBounds;
        String[] rowLabels;
        String detectorKey;
        HashMap<String, OrderedIntervals> validLabels = new HashMap<>();
        Detector detector;
        while((line = bufferedReader.readLine()) != null) {

            // grab the label data from this row
            row = split_and_trim_string(line, CSV_SEPARATOR);
            intervalBounds = split_and_cast_interval(row[c2i.get(CSV_INTERVALCOL)]);
            rowLabels = split_and_trim_string(row[c2i.get(CSV_LABELCOL)], CSV_LABEL_SEPARATOR);
            detectorKey = row[c2i.get(CSV_DETECTORCOL)];

            // build an interval and add to the valid labels for this detector
            detector = null;
            for (Detector d : this.detectors) {
                if (d.get_key().equals(detectorKey)) { detector = d; break; }
            }
            if (detector == null) {
                throw new RuntimeException(String.format("Invalid detector key %s in %s", detectorKey, csvPath));
            }
            if (!validLabels.containsKey(detectorKey)) {
                validLabels.put(detectorKey, new OrderedIntervals());
            }
            validLabels.get(detectorKey).add(new Interval(rowLabels, intervalBounds[0], intervalBounds[1]));
        }

        bufferedReader.close();
        return validLabels;
    }


    /**
     * Parses training images from a video based on an input text file based on
     * frame indices.
     * <p>
     * Input file should be formatted like:
     *
     * frame,[detector_1_name_key],[detector_2_name_key],...
     * [index_1],[detector_1_index_1_label],[detector_2_index_1_label],...
     * ...
     *
     * e.g.
     *
     * frame,name,clock
     * 12,abaddon,10:23
     * 44,drow ranger,22:04
     *
     * @param inputFile path to the csv containing the labels and frame indices
     * @return paths to saved image files
     */
    public ArrayList<String> parse_training_images_by_file(String inputFile) throws IOException {
        ArrayList<String> outputs = new ArrayList<>();
        String key;
        Frame fullFrame;
        Mat roiFrame;
        HashMap<Integer, String[]> labels = load_timing_csv(inputFile);
        Integer[] framesToQuery = labels.keySet().toArray(new Integer[0]);
        Arrays.sort(framesToQuery);
        FrameIterator frameIter = new FrameIterator(this, 1);

        // loop over all frames, but only inspect those that have some definition in the timing csv
        this.open();
        for (int frameIndex : framesToQuery) {
            this.video.set(CV_CAP_PROP_POS_FRAMES, frameIndex);
            System.out.print(String.format("\rProcessing frame %s of %s (%.0f%%)", frameIndex, this.frames, (100d*frameIndex/this.frames)));
            fullFrame = frameIter.next();
            for (int i = 0; i < this.detectors.length; i++){
                key = labels.get(frameIndex)[i];
                if (key != null){
                    roiFrame = fullFrame.mat().submat(this.regions[i]);
                    outputs.add(this.trainers[i].save_image(roiFrame, key));
                }
            }
        }
        return outputs;
    }



    /**
     * Plays the video from the beginning with basic controls and display of frame index and time.
     * <p>
     * SPACE : pause/resume
     * LEFT : back 1 frame (hold down to repeat)
     * RIGHT : forward 1 frame (hold down to repeat)
     *
     * @throws InterruptedException
     */
    public void play() throws InterruptedException {

        // create the video playback window
        JFrame window = new JFrame();
        window.setTitle(this.file.getAbsolutePath());
        ImageIcon img = new ImageIcon();
        JLabel imageLabel = new JLabel(img);
        imageLabel.setHorizontalTextPosition(imageLabel.CENTER);
        imageLabel.setVerticalTextPosition(imageLabel.CENTER);
        imageLabel.setFont(new Font(imageLabel.getName(), Font.PLAIN, 30));
        imageLabel.setForeground(Color.YELLOW);
        window.add(imageLabel);
        window.setVisible(true);

        // create action listeners
        InputMap inputMap = imageLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = imageLabel.getActionMap();
        PlayPause pause = new PlayPause();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pause");
        actionMap.put("pause", pause);

        PlayBackward backOneFrame = new PlayBackward(1d*options.VP.SKIP_FRAMES());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "back_by_frames");
        actionMap.put("back_by_frames", backOneFrame);

        PlayBackward backOneSecond = new PlayBackward(this.fps*options.VP.SKIP_SECONDS());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "back_by_seconds");
        actionMap.put("back_by_seconds", backOneSecond);

        PlayBackward backOneMinute = new PlayBackward(this.fps*60d*options.VP.SKIP_MINUTES());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK), "back_by_minutes");
        actionMap.put("back_by_minutes", backOneMinute);

        PlayForward forwardOneFrame = new PlayForward(1*options.VP.SKIP_FRAMES());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "forward_by_frames");
        actionMap.put("forward_by_frames", forwardOneFrame);

        PlayForward forwardOneSecond = new PlayForward(this.fps*options.VP.SKIP_SECONDS());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "forward_by_seconds");
        actionMap.put("forward_by_seconds", forwardOneSecond);

        PlayForward forwardOneMinute = new PlayForward(this.fps*60d*options.VP.SKIP_MINUTES());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK), "forward_by_minutes");
        actionMap.put("forward_by_minutes", forwardOneMinute);

        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // loop over the video
        long lastDisplay = -10000;
        long curTime;
        long lapse;
        Frame frame = new Frame();
        double delay = (1d / this.fps)*1000d;
        long sleep = (long) (delay / 1000);
        this.open();
        while (this.is_open()) {

            // skip frames
            backOneFrame.activate(this.video);
            backOneSecond.activate(this.video);
            backOneMinute.activate(this.video);
            forwardOneFrame.activate(this.video);
            forwardOneSecond.activate(this.video);
            forwardOneMinute.activate(this.video);

            // get the next frame
            if (this.current_index() == (this.frames-1)) { backOneFrame.isDown = true; }
            this.video.read(frame.mat);
            img.setImage(mat_to_img(frame.mat));
            imageLabel.setText(String.format("Frame %d/%d, Time(s) %.4f", this.current_index(), this.frames, this.current_time()/1000d));
            imageLabel.repaint();
            window.pack();

            // if it's been not enough time since the last update, wait
            curTime = System.currentTimeMillis();
            lapse = curTime - lastDisplay;
            while (
                    (pause.paused() || (lapse < delay)) &&
                    !backOneFrame.activate(this.video) &&
                    !forwardOneFrame.activate(this.video) &&
                    !backOneSecond.activate(this.video) &&
                    !forwardOneSecond.activate(this.video) &&
                    !backOneMinute.activate(this.video) &&
                    !forwardOneMinute.activate(this.video)
            ) {
                curTime = System.currentTimeMillis();
                lapse = curTime - lastDisplay;
                Thread.sleep(sleep);
            }

            // display the frame
            window.repaint();
            lastDisplay = System.currentTimeMillis();

        }
        this.close();
        window.setVisible(false);
        window.dispose();
    }



    ///////////////////////////////////////////////////////////////////////////
    // HELPER CLASSES /////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** This class holds an opencv Mat frame in a video and the time in the video of that frame. */
    public static class Frame {
        private Mat mat = new Mat();
        private double time;
        private int index;
        public Frame(){}
        public Frame(Mat mat, double time, int index){
            this.mat = mat;
            this.time = time;
            this.index = index;
        }
        public void update(Mat mat, double time, int index){
            mat.copyTo(this.mat);
            this.time = time;
            this.index = index;
        }
        public void update(double time, int index){ this.time = time; this.index = index; }
        public Mat mat() { return this.mat; }
        public double time() { return this.time; }
        public int index() { return this.index; }
    }


    /** This is a container class to store results of detection for a single frame. */
    public static class ParserResult {
        private double time;
        private int frame;
        private HashMap<String, String> results = new HashMap<>();
        public ParserResult(double time, int frame){
            this.time = time;
            this.frame = frame;
        }
        public void put(String detectorKey, String detectedLabel){
            this.results.put(detectorKey, detectedLabel);
        }
        public double time() { return this.time; }
        public int frame() { return this.frame; }
        public HashMap<String, String> results() { return this.results; }
        public String get(String key) { return this.results.get(key); }
    }


    private class PlayPause extends AbstractAction {
        private boolean isPaused = false;
        @Override
        public void actionPerformed(ActionEvent e) {
            this.isPaused = !this.isPaused;
        }
        public boolean paused() { return this.isPaused; }
    }


    private class PlayBackward extends AbstractAction {
        private boolean isDown = false;
        private double skip = 1;
        public PlayBackward(){}
        public PlayBackward(double framesToSkip) {
            this.skip = framesToSkip;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            this.isDown = true;
        }
        public boolean activate(VideoCapture video) {
            if (this.isDown) {
                double nextFrame = Math.max(0, video.get(CV_CAP_PROP_POS_FRAMES) - this.skip - 1);
                video.set(CV_CAP_PROP_POS_FRAMES, nextFrame);
                this.isDown = false;
                return true;
            }
            else {
                return false;
            }
        }
    }


    private class PlayForward extends AbstractAction {
        private boolean isDown = false;
        private double skip = 1;
        public PlayForward(){}
        public PlayForward(double framesToSkip) {
            this.skip = framesToSkip;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            this.isDown = true;
        }
        public boolean activate(VideoCapture video) {
            if (this.isDown) {
                double nextFrame = Math.min(video.get(CV_CAP_PROP_FRAME_COUNT)-1, video.get(CV_CAP_PROP_POS_FRAMES) + this.skip - 1);
                video.set(CV_CAP_PROP_POS_FRAMES, nextFrame);
                this.isDown = false;
                return true;
            }
            else {
                return false;
            }
        }
    }


    private static class ParseCloseWindow extends AbstractAction {
        private JFrame window;
        public ParseCloseWindow(JFrame window) {
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.window.dispose();
        }
    }


    /** Iterator over the frames of the video, 1 frame per second. */
    @Override
    public Iterator<Frame> iterator() { return iterator(this.fps); }

    /** Iterator over the frames of the video with <code>skip</code> frames between each return. */
    public Iterator<Frame> iterator(int skip) { return new FrameIterator(this, skip); }

    private class FrameIterator implements Iterator {
        private VideoParser outter;
        private int i = 0;
        private int n;
        private int skip;
        private Frame frame = new Frame();
        public FrameIterator(VideoParser outter, int skip){
            this.outter = outter;
            this.skip = skip;
            this.n = outter.frames;
            if (outter.is_open()) { throw new RuntimeException("Cannot create an iterator on an open video."); }
            outter.open();
        }

        public boolean hasNext(){
            if ((this.i + this.skip) < this.n) { return true; }
            else { outter.close(); return false; }
        }

        @SuppressWarnings("unchecked")
        public Frame next(){
            outter.video.read(this.frame.mat());
            this.frame.update(outter.current_time(), outter.current_index());
            this.i += this.skip;
            outter.video.set(CV_CAP_PROP_POS_FRAMES, this.i);
            return this.frame;
        }

        public int get_frame_index() { return this.i; }
    }



    ///////////////////////////////////////////////////////////////////////////
    // COMMAND-LINE OPTIONS ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////


    private static Options build_command_line_options() {
        Option help = Option.builder("h")
                .longOpt("help")
                .required(false)
                .hasArg(false)
                .desc("print this message")
                .build();

        Option task = Option.builder("t")
                .longOpt("task")
                .required(false)
                .hasArg(true)
                .numberOfArgs(1)
                .desc("what should be done with the video; valid choices are: \n" +
                    TASK_PLAY + ": plays the video\n" +
                    TASK_PARSE + ": parses video with existing detection objects and saves results to a file\n\n" +
                    TASK_PARSE_INT + ": parses training images by prompting user for image labels\n\n" +
                    TASK_PARSE_FILE + ": parses training images based on the provided frame/label file\n\n" +
                    TASK_PARSE_AUTO + ": attempts to parse training images from exiting detection objects"
                )
                .build();

        Option video = Option.builder("v")
                .longOpt("video")
                .required(false)
                .hasArg(true)
                .numberOfArgs(1)
                .desc("input video to parse")
                .build();

        Option parseFile = Option.builder("f")
                .longOpt("parsing_file")
                .required(false)
                .hasArg(true)
                .numberOfArgs(1)
                .desc("input file when one is needed/allowed, i.e. for " + TASK_PARSE_FILE + " and " + TASK_PARSE + "; ignored otherwise")
                .build();

        Option resultsFile = Option.builder("o")
                .longOpt("output_file")
                .required(false)
                .hasArg(true)
                .numberOfArgs(1)
                .desc("file to store the results of parsing in; required when task is " + TASK_PARSE + "; ignored otherwise")
                .build();

        Option skip = Option.builder("s")
                .longOpt("skip")
                .required(false)
                .hasArg(true)
                .numberOfArgs(1)
                .desc("number of seconds to skip between parsing events; ignored when task is " + TASK_PLAY + " or " + TASK_PARSE_FILE + " (parses all frames)")
                .build();

        Option skipFrames = Option.builder("sf")
                .longOpt("skip_frames")
                .required(false)
                .hasArg(true)
                .numberOfArgs(1)
                .desc("number of frames to skip between parsing events; ignored when task is " + TASK_PLAY + " or " + TASK_PARSE_FILE + " (parses all frames); supercedes other skip")
                .build();

        Options options = new Options();
        options.addOption(help);
        options.addOption(task);
        options.addOption(video);
        options.addOption(parseFile);
        options.addOption(resultsFile);
        options.addOption(skip);
        options.addOption(skipFrames);
        return options;
    }



    ///////////////////////////////////////////////////////////////////////////
    // UNIT TESTS/MAIN ////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {



        String[] a = new String[]{
//                "-t", "play",
                "-t", "parse",
                "--video", "D:\\Dropbox\\youtube_game_hud\\server\\test\\video_480p_r01.avi",
                "-o", "D:\\Dropbox\\youtube_game_hud\\server\\test\\results.csv",
                "-f", "D:\\Dropbox\\youtube_game_hud\\server\\test\\labels_r01.csv",
                "-s", "1"
        };
        if (args.length == 0) { args = a; }

        // Parse command-line options
        File video = null;
        File parsingFile = null;
        File resultsFile = null;
        String task = DEFAULT_VIDEO_TASK;
        double skip = DEFAULT_SKIP_SECONDS;
        boolean help = false;
        boolean skipIsFrames = false;
        Options options = build_command_line_options();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse( options, args );

            // get help request
            if( line.hasOption( "help" ) ) {
                help = true;
            }

            // grab video file
            if (line.hasOption("video")) {
                video = new File(line.getOptionValue("video"));
            }

            // check which task we should do. If we are to parse by file, get
            // that file
            if (line.hasOption("task")) {
                task = line.getOptionValue("task");
                if (task.equals(TASK_PARSE_FILE)) {
                    if (!line.hasOption("parsing_file")) {
                        throw new ParseException("Must provide a parsing file when parsing by file.");
                    }
                }

                else if (task.equals(TASK_PARSE)) {
                    if (!line.hasOption("output_file")) {
                        throw new ParseException("Must provide an output results file when parsing.");
                    }
                    resultsFile = new File(line.getOptionValue("output_file"));
                }
            }

            // get parsing file
            if (line.hasOption("parsing_file")) {
                parsingFile = new File(line.getOptionValue("parsing_file"));
            }

            // get the skip rate
            if (line.hasOption("skip")) {
                skip = Double.valueOf(line.getOptionValue("skip"));
                skipIsFrames = false;
            }
            else if (line.hasOption("skip_frames")) {
                skip = Double.valueOf(line.getOptionValue("skip_frames"));
                skipIsFrames = true;
            }

        }
        catch( ParseException exp ) {
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        }


        // do the task requested by the user
        ArrayList<String> results = null;
        if (help) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("video_parser [options] -v path/to/video/file", options);
        }

        else if (video != null){
            VideoParser P = null;
            try {
                P = new VideoParser(video.getAbsolutePath());
                if (!skipIsFrames) {
                    skip = Math.round(P.fps*skip);
                    skipIsFrames = true;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (task.equals(TASK_PLAY)) {
                try {
                    P.play();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            else if (task.equals(TASK_PARSE)) {
                try {
                    HashMap<String, OrderedIntervals> validLabels = null;
                    if (parsingFile != null) {
                        validLabels = P.load_valid_labels(parsingFile.getAbsolutePath());
                    }
                    VideoParser.save_detection_results(P.parse((int) skip, validLabels), resultsFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            else if (task.equals(TASK_PARSE_FILE)) {
                try {
                    results = P.parse_training_images_by_file(parsingFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (String path : results ) {
                    System.out.println(path);
                }
            }

            else if (task.equals(TASK_PARSE_INT)) {
                results = P.parse_training_images_iteractively((int) skip);
                for (String path : results ) {
                    System.out.println(path);
                }
            }

            else if (task.equals(TASK_PARSE_AUTO)) {
                try {
                    results = P.autodetect_training_images((int) skip);
                    for (String path : results) {
                        System.out.println(path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
