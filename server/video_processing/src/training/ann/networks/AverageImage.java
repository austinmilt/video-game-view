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


package training.ann.networks;

import training.math.Matrix2D;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;

public class AverageImage extends Network implements Serializable {

    private double binaryThreshold = 0.5;
    private HashMap<String, Matrix2D> protoImages = new HashMap<>();
    private int[] inputShape = null;
    public AverageImage(HashMap<String, Matrix2D> protoImages) {
        for (String name : protoImages.keySet()) {

            // check shape of input
            if (this.inputShape == null) {
                this.inputShape = new int[2];
                this.inputShape[0] = protoImages.get(name).r();
                this.inputShape[1] = protoImages.get(name).c();
            }

            else if (protoImages.get(name).r() != this.inputShape[0]) {
                    throw new IllegalArgumentException("Proto image shapes must be identical.");
            }

            else if (protoImages.get(name).c() != this.inputShape[1]) {
                    throw new IllegalArgumentException("Proto image shapes must be identical.");
            }

            // put the validated image into protoimages
            this.protoImages.put(name, protoImages.get(name));
        }
    }


    /**
     * Calculate Pearson's correlation coefficient between elements in the two inputs.
     *
     * @param X1 first matrix
     * @param X2 second matrix
     * @return r, Pearson's correlation coefficient
     */
    private static double pearsons(Matrix2D X1, Matrix2D X2) {
        double N = X1.size();
        double sumX1 = 0d;
        double sumX2 = 0d;
        double sumX1X2 = 0d;
        double sumX1Squared = 0d;
        double sumX2Squared = 0d;
        double x1;
        double x2;
        for (int c = 0; c < X1.size(); c++) {
            x1 = X1.get_by_flat(c);
            x2 = X2.get_by_flat(c);
            sumX1 += x1;
            sumX2 += x2;
            sumX1X2 += x1*x2;
            sumX1Squared += x1*x1;
            sumX2Squared += x2*x2;
        }

        double result = (N*sumX1X2 - sumX1*sumX2);
        result /= Math.sqrt(N*sumX1Squared - sumX1*sumX1) * Math.sqrt(N*sumX2Squared - sumX2*sumX2);
        return result;
    }


    /**
     * Predicts the most likely label of the given sample image.
     * <p>
     * Best label is determined by Pearson's correlation coefficient with glyphs.
     *
     * @param sample image to classify
     * @param validLabels valid output labels to choose from
     * @return most likely label
     */
    public String predict(Matrix2D sample, Collection<String> validLabels) {
        double minError = Double.POSITIVE_INFINITY;
        double error;
        Matrix2D protoImage;
        String bestLabel = null;
        if (validLabels == null) { validLabels = this.protoImages.keySet(); }
        for (String name : validLabels) {
            protoImage = this.protoImages.get(name);
            if (protoImage == null) { error = Double.POSITIVE_INFINITY; }
            else {
                error = pearsons(sample, protoImage);
                error *= error;
                error = 1 - error;
            }
            if (error < minError) {
                minError = error;
                bestLabel = name;
            }
        }
        return bestLabel;
    }

    public Matrix2D predict(Matrix2D input) {
        throw new IllegalArgumentException("No method defined for this set of inputs.");
    }


    public int[] get_input_shape(){ return this.inputShape; }

    public void set_threshold(double threshold) { this.binaryThreshold = threshold; }


    ///////////////////////////////////////////////////////////////////////////
    // SERIALIZATION //////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Saves the caller to a serialized file on disk.
     *
     * @param path path to save the network to
     * @throws IOException
     */
    public void save(String path) throws IOException {
        ObjectOutputStream oos = null;
        FileOutputStream fout;
        try{
            fout = new FileOutputStream(path, false);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(oos  != null){
                oos.close();
            }
        }
    }


    /**
     * Loads an AverageImage detector object that has been saved to disk.
     *
     * @param path path to the serialized AverageImage file
     * @return the loaded AverageImage
     */
    public static AverageImage load(String path) throws IOException {
        ObjectInputStream objectinputstream = null;
        FileInputStream streamIn;
        AverageImage output = null;
        try {
            streamIn = new FileInputStream(path);
            objectinputstream = new ObjectInputStream(streamIn);
            output = (AverageImage) objectinputstream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(objectinputstream != null){
                objectinputstream .close();
            }
        }
        return output;
    }

}
