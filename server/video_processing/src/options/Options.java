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


package options;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;

public class Options {

    ///////////////////////////////////////////////////////////////////////////
    // CONSTANTS AND PRIVATE VARIABLES/////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    // constants
    private static final String DEFAULT_OPTIONS_NAME = "settings.config";
    private static final String DEFAULT_OPTIONS = Options.locate_options_file();
//    private static final String DEFAULT_OPTIONS = "D:\\Dropbox\\youtube_game_hud\\server\\deployment\\settings.config";
    private static final File JAR = Options.locate_jar();
    private static final String COMMENT = "#";
    private static final String VALUE_DEFINITION = "=";
    private static final String VALUE_SEPARATOR = ",";
    private static final String RANGE_SEPARATOR = "-";
    private static final String OPTION_SEPARATOR = ".";

    private String optionsFile;

    // object storing the options as strings
    private HashMap<String, String> data = new HashMap<>();



    ///////////////////////////////////////////////////////////////////////////
    // OPTION SUBCLASSES //////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    // ClockTrainer settings
    public ClockTrainerOptions CT;
    public class ClockTrainerOptions {
        public static final String NAME = "clock_trainer";
        public File TRAINING_DIR() { return get_file(explicit(NAME, "training_dir")); }
        public String SEARCH() { return get_string(explicit(NAME, "search_str")); }
        public boolean SAVE() { return get_boolean(explicit(NAME, "save_glyphs")); }
        public double THRESHOLD() { return get_double(explicit(NAME, "bw_threshold")); }
        public int GLYPHS() { return get_int(explicit(NAME, "glyphs")); }
        public int WIDTH_001() { return get_int(explicit(NAME, "segwidth_001")); }
        public int WIDTH_010() { return get_int(explicit(NAME, "segwidth_010")); }
        public int WIDTH_100() { return get_int(explicit(NAME, "segwidth_100")); }
        public int[][] SEGMENT_001() { return get_list_range_int(explicit(NAME, "positions_001")); }
        public int[][] SEGMENT_010() { return get_list_range_int(explicit(NAME, "positions_010")); }
        public int[][] SEGMENT_100() { return get_list_range_int(explicit(NAME, "positions_100")); }
        public int LAYER_SIZE() { return get_int(explicit(NAME, "layer_size")); }
        public double LEARNING_RATE() { return get_double(explicit(NAME, "learning_rate")); }
        public double DECAY_RATE() { return get_double(explicit(NAME, "decay_rate")); }
        public int EPOCHS() { return get_int(explicit(NAME, "epochs")); }
        public int BATCH_SIZE() { return get_int(explicit(NAME, "batch_size")); }
        public int LABEL_SIZE() { return get_int(explicit(NAME, "label_size")); }
        public double PROP_TRAINING() { return get_double(explicit(NAME, "training_proportion")); }
        public double PROP_VALIDATION() { return get_double(explicit(NAME, "validation_proportion")); }
        public double DROPOUT_RATE() { return get_double(explicit(NAME, "dropout_rate")); }
        public String SEPARATOR() { return get_string(explicit(NAME, "time_separator")); }
        public String IMAGE_FORMAT() { return get_string(explicit(NAME, "image_format")); }
    }


    // ClockDetector settings
    public ClockDetectorOptions CD;
    public class ClockDetectorOptions {
        public static final String NAME = "clock_detector";
        public double[] ROI() { return get_list_double(explicit(NAME, "frame_region")); }
        public File NETWORK() { return get_file(explicit(NAME, "serialized_network")); }
        public boolean SAVE() { return get_boolean(explicit(NAME, "save_predictions")); }
        public String KEY() { return get_string(explicit(NAME, "key")); }
    }


    // NameTrainer settings
    public NameTrainerOptions NT;
    public class NameTrainerOptions {
        public static final String NAME = "name_trainer";
        public File TRAINING_DIR() { return get_file(explicit(NAME, "training_dir")); }
        public String SEARCH() { return get_string(explicit(NAME, "search_str")); }
        public boolean SAVE() { return get_boolean(explicit(NAME, "save_glyphs")); }
//        public int LAYER_SIZE() { return get_int(explicit(NAME, "layer_size")); }
//        public double LEARNING_RATE() { return get_double(explicit(NAME, "learning_rate")); }
//        public double DECAY_RATE() { return get_double(explicit(NAME, "decay_rate")); }
//        public int EPOCHS() { return get_int(explicit(NAME, "epochs")); }
//        public int BATCH_SIZE() { return get_int(explicit(NAME, "batch_size")); }
//        public double PROP_TRAINING() { return get_double(explicit(NAME, "training_proportion")); }
//        public double PROP_VALIDATION() { return get_double(explicit(NAME, "validation_proportion")); }
//        public double DROPOUT_RATE() { return get_double(explicit(NAME, "dropout_rate")); }
        public double BINARY_THRESHOLD() { return get_double(explicit(NAME, "binary_threshold")); }
    }


    // NameDetector settings
    public NameDetectorOptions ND;
    public class NameDetectorOptions {
        public static final String NAME = "name_detector";
        public double[] ROI() { return get_list_double(explicit(NAME, "frame_region")); }
        public File NETWORK() { return get_file(explicit(NAME, "serialized_network")); }
        public boolean SAVE() { return get_boolean(explicit(NAME, "save_predictions")); }
        public String KEY() { return get_string(explicit(NAME, "key")); }
    }


    // video.VideoParser settings
    public VideoParserOptions VP;
    public class VideoParserOptions {
        public static final String NAME = "video_parser";
        public String[] DETECTORS() { return get_list_string(explicit(NAME, "detectors")); }
        public double SKIP_FRAMES() { return get_double(explicit(NAME, "play_skip_frames")); }
        public double SKIP_SECONDS() { return get_double(explicit(NAME, "play_skip_seconds")); }
        public double SKIP_MINUTES() { return get_double(explicit(NAME, "play_skip_minutes")); }
        public double PARSE_SKIP() { return get_double(explicit(NAME, "parse_skip_default")); }
        public String CSV_SEP_COL() { return get_string(explicit(NAME, "csv_separator_column")); }
        public String CSV_SEP_INTERVAL() { return get_string(explicit(NAME, "csv_separator_interval")); }
        public String CSV_SEP_LABEL() { return get_string(explicit(NAME, "csv_separator_label")); }
        public String CSV_COL_FRAME() { return get_string(explicit(NAME, "csv_column_frame")); }
        public String CSV_COL_TIME() { return get_string(explicit(NAME, "csv_column_time")); }
        public String CSV_COL_INTERVAL() { return get_string(explicit(NAME, "csv_column_interval")); }
        public String CSV_COL_LABEL() { return get_string(explicit(NAME, "csv_column_label")); }
        public String CSV_COL_DETECTOR() { return get_string(explicit(NAME, "csv_column_detector")); }
        public File OPENCV_WINDOWS() { return get_file(explicit(NAME, "opencv_windows")); }
        public File OPENCV_LINUX() { return get_file(explicit(NAME, "opencv_linux")); }
    }


    // job settings
    public JobOptions JB;
    public class JobOptions {
        public static final String NAME = "job";
        public File JAR_DIR() { return get_file(explicit(NAME, "jar_dir")); }
        public File SCRATCH() { return get_file(explicit(NAME, "scratch_directory")); }
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Makes options from default options file. */
    public Options() throws IOException { this(DEFAULT_OPTIONS); }

    /** Makes options from the given options file. */
    public Options(String optionsFile) throws IOException { this(Options.read_options(optionsFile), optionsFile); }

    /** Makes options from the given data map (as returned by read_options). */
    public Options(HashMap<String, String> dataMap) { this(dataMap, null); }

    /** Makes options from the given data map (as returned by read_options). */
    public Options(HashMap<String, String> dataMap, String optionsPath) {
        this.optionsFile = optionsPath;
        this.CT = new ClockTrainerOptions();
        this.NT = new NameTrainerOptions();
        this.CD = new ClockDetectorOptions();
        this.ND = new NameDetectorOptions();
        this.VP = new VideoParserOptions();
        this.JB = new JobOptions();
        for (String k : dataMap.keySet()) { this.data.put(k, dataMap.get(k)); }
    }



    ///////////////////////////////////////////////////////////////////////////
    // HELPER FUNCTIONS ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Reads an options file and returns a mapping that can be used to create a new Options.
     *
     * @param optionsFile path to options file to read
     * @return mapping to feed into Options constructor
     * @throws IOException throws when the file cant be found or cant be read
     */
    public static HashMap<String, String> read_options(String optionsFile) throws IOException {
        String line = null;
        String[] splitLine;
        String key;
        String value;
        HashMap<String, String> output = new HashMap<>();
        FileReader fileReader = new FileReader(optionsFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        while((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) { continue; } //skip blank lines
            if (line.startsWith(COMMENT)) { continue; } //skip commented lines
            splitLine = line.split(VALUE_DEFINITION, 2);
            key = splitLine[0].trim();
            value = splitLine[1].trim();
            output.put(key, value);
        }
        bufferedReader.close();
        return output;
    }


    private static String locate_options_file() {
        String path = Options.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File optionsFile;
        try {
            File jarPath = new File(URLDecoder.decode(path, "UTF-8"));
            optionsFile = new File(jarPath.getParentFile().getParentFile(), DEFAULT_OPTIONS_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to locate options file. May not have been able to decode the path of the jar.");
        }
        return optionsFile.getAbsolutePath();
    }


    private static File locate_jar() {
        String path = Options.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarPath;
        try {
            jarPath = new File(URLDecoder.decode(path, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to locate options file. May not have been able to decode the path of the jar.");
        }
        return jarPath;
    }


    public String get_options_file_path() { return this.optionsFile; }

    public File get_jar() { return Options.JAR; }

    private String[] split_and_trim_value(String value, String separator){
        String[] splitted = value.split(separator);
        String[] output = new String[splitted.length];
        for (int i = 0; i < splitted.length; i++) { output[i] = splitted[i].trim(); }
        return output;
    }

    private static String explicit(String name, String option) {
        return name + OPTION_SEPARATOR + option;
    }

    // getters for shorthand in constant calls
    private File get_file(String key) { return new File(this.data.get(key)); }
    private double get_double(String key) { return Double.valueOf(this.data.get(key)); }
    private int get_int(String key) { return Integer.valueOf(this.data.get(key)); }
    private String get_string(String key) { return this.data.get(key); }
    private boolean get_boolean(String key) { return Boolean.valueOf(this.data.get(key)); }
    private String[] get_list_string(String key) { return split_and_trim_value(this.data.get(key), VALUE_SEPARATOR); }

    private double[] get_list_double(String key) {
        String[] splitted = split_and_trim_value(this.data.get(key), VALUE_SEPARATOR);
        double[] output = new double[splitted.length];
        for (int i = 0; i < splitted.length; i++){
            output[i] = Double.valueOf(splitted[i]);
        }
        return output;
    }

    private int[] get_list_int(String key){
        String[] splitted = split_and_trim_value(this.data.get(key), VALUE_SEPARATOR);
        int[] output = new int[splitted.length];
        for (int i = 0; i < splitted.length; i++){
            output[i] = Integer.valueOf(splitted[i]);
        }
        return output;
    }

    private File[] get_list_file(String key){
        String[] splitted = split_and_trim_value(this.data.get(key), VALUE_SEPARATOR);
        File[] output = new File[splitted.length];
        for (int i = 0; i < splitted.length; i++){
            output[i] = new File(splitted[i]);
        }
        return output;
    }

    private int[][] get_list_range_int(String key){
        String[] splitted = split_and_trim_value(this.data.get(key), VALUE_SEPARATOR);
        int[][] output = new int[splitted.length][2];
        for (int i = 0; i < splitted.length; i++){
            String[] range = split_and_trim_value(splitted[i], RANGE_SEPARATOR);
            output[i][0] = Integer.valueOf(range[0]);
            output[i][1] = Integer.valueOf(range[1]);
        }
        return output;
    }

}
