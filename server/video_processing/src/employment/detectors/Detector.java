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
import training.ann.networks.Network;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * This is the general class for using trained image detectors (e.g neural nets)
 * to detect objects in a video.
 *
 * @author Austin Milt
 */
public abstract class Detector {

    protected Network network;
    protected File networkPath;
    protected double[] roi;
    protected String key;


    /** Creates and empty detector. */
    public Detector(){}

    /**
     * Creates a new Detector using the trained detection object at the given path.
     *
     * @param networkPath path to the input neural network
     */
    public Detector(String networkPath, String key, double[] roi) {
        this.networkPath = new File(networkPath);
        this.key = key;
        this.roi = roi;
    }


    /** Each detector must accept an opencv frame and return a string label for the image. */
    public abstract String detect(Mat frame) throws IOException;

    public abstract String detect(Mat frame, Collection<String> validIDs) throws IOException;


    /** Each detector must be able to initialize by loading its detection object (e.g. neural net) and instantiating any necessary loop variables. */
    public abstract void initialize() throws IOException;


    /**
     * Gets the region of interest associated with the assigned detection object.
     *
     * @return
     */
    public double[] get_roi() { return this.roi; }


    /** Gets the key name of the detector for distinguishing results of detection from other detectors. */
    public String get_key() { return this.key; }
}
