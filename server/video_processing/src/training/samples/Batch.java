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


package training.samples;

import training.math.Matrix2D;


/**
 * This class contains test samples by collating them together into a single
 * to pass through a network and keeps them associated with their answer labels.
 *
 * @author Austin Milt
 */
public class Batch {
    private Matrix2D data;
    private Matrix2D labels;
    private int size;
    private int inputs;
    private int outputs;
    private String[] ids;


    /** Creates a new empty Batch. */
    public Batch(){}


    /** Creates a new Batch containing all the given samples. */
    public Batch(Sample[] samples){ this(samples, range(samples.length)); }


    /**
     * Creates a new Batch containing the samples indicated by the given indices.
     *
     * @param samples all test samples
     * @param indices the indices in <code>samples</code> to contain in this batch
     */
    public Batch(Sample[] samples, int[] indices){

        this.size = indices.length;
        this.inputs = samples[indices[0]].get_attributes().r();
        this.outputs = samples[indices[0]].get_label().r();
        this.data = new Matrix2D(this.inputs, this.size);
        this.labels = new Matrix2D(this.outputs, this.size);
        this.ids = new String[this.size];

        // set data in the aggregate matrices by looping over samples
        Matrix2D data;
        Matrix2D label;
        for (int s = 0; s < this.size; s++){
            this.ids[s] = samples[s].get_id();

            // set the input data
            data = samples[indices[s]].get_attributes();
            label = samples[indices[s]].get_label();
            for (int i = 0; i < this.inputs; i++){
                this.data.set(i, s, data.get(i, 0));
            }

            // set the output label
            for (int i = 0; i < this.outputs; i++){
                this.labels.set(i, s, label.get(i, 0));
            }
        }
    }

    // helper functions to produce a range of ints
    public static int[] range(int min, int max){
        return range(min, max, 1);
    }
    public static int[] range(int max){ return range(0, max, 1); }
    public static int[] range(int min, int max, int step){
        int[] out = new int[(max - min) / step];
        int x = min;
        for (int i = 0; i < out.length; i++){
            out[i] = x;
            x += step;
        }
        return out;
    }

    /** Gets this batch's test data. */
    public Matrix2D get_data() { return this.data; }

    /** Gets this batch's answer labels. */
    public Matrix2D get_labels() { return this.labels; }

    /** Gets this batch's number of input data per sample. */
    public int get_inputs() { return this.inputs; }

    /** Gets this batch's number of label data per sample. */
    public int get_outputs() { return this.outputs; }

    /** gets this batch's number of samples. */
    public int get_size() { return this.size; }

    /** Makes a deep copy of this Batch. */
    public Batch copy() {
        Batch b = new Batch();
        b.data = this.data.copy();
        b.labels = this.labels.copy();
        b.size = this.size;
        b.inputs = this.inputs;
        b.outputs = this.outputs;
        b.ids = this.ids.clone();
        return b;
    }
}
