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


package training.ann.layers;

import training.math.Matrix2D;

import java.io.Serializable;


/**
 * This class is used to hold training or testing data to pass forward through a
 * neural net.
 *
 * @author Austin Milt
 */
public class InputLayer extends Layer implements Serializable {

    /**
     * Creates a new InputLayer with the given size of input data to be fed later.
     *
     * @param rows number of input data per sample, e.g. number of pixels per test image
     * @param samples number of samples, e.g. number of test images per pass
     */
    public InputLayer(int rows, int samples) { super(rows, samples); }


    /**
     * Forward pass of an InputLayer requires you to supply a test matrix.
     */
    public void forward(){ throw new IllegalArgumentException("Must supply an input."); }


    /**
     * Performs the forward pass, which is just to copy the input data into this
     * layer's output.
     *
     * @param input input test data to pass through network
     */
    public void forward(Matrix2D input) { input.copy(this.O()); }


    /**
     * Input layers do nothing in the backward pass (shouldnt even be called).
     */
    public void backward(){}


    /**
     * Input layers do nothing in the update (shouldnt even be called).
     * @param rate
     */
    public void update(double rate){}


    /**
     * Reduces the layer to the size appropriate to accept a single test sample.
     * @return a new InputLayer that can accept a single test sample.
     */
    public InputLayer reduce() { return new InputLayer(this.O().r(), 1); }
}
