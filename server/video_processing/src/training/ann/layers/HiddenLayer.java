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

import java.io.*;

/**
 * This class is an abstract layer for intermediate layers in a neural network.
 *
 * @author Austin Milt
 */
public abstract class HiddenLayer extends Layer implements Serializable {

    // private variables shared across child classes
    private Matrix2D weights;
    private Matrix2D bias;
    private Matrix2D deltaWeight;
    private Matrix2D deltaBias;
    private Matrix2D weightsT;
    private Matrix2D inputsT;
    private Matrix2D ones;


    /**
     * Creates an empty HiddenLayer. Generally shouldnt be used.
     */
    public HiddenLayer(){}


    /**
     * Creates a new HiddenLayer with attributes shared across its children
     * (mostly reserves memory for various intermediate calculations).
     *
     * @param inputs number of input data per sample vector, i.e. number of rows of inputs
     * @param samples number of samples, i.e. number of input columns
     * @param outputs number of outputs, i.e. number of neurons in this layer
     * @param bias initial bias value for all neurons in this layer
     */
    public HiddenLayer(int inputs, int samples, int outputs, double bias){
        super(outputs, samples);
        double var = Math.sqrt(2./inputs);
        this.weights = Matrix2D.random(outputs, inputs).multiply(var);
        this.bias = Matrix2D.constant(outputs, 1, bias);
        this.deltaWeight = new Matrix2D(outputs, inputs);
        this.deltaBias = new Matrix2D(outputs, 1);
        this.weightsT = new Matrix2D(inputs, outputs);
        this.inputsT = new Matrix2D(samples, inputs);
        this.ones = new Matrix2D(samples, 1);
        this.set_deltas(new Matrix2D(inputs, samples));
    }


    /**
     * Creates a new HiddenLayer with bias defaulted to 0.
     * @param weights
     * @param bias
     * @param samples
     */
    public HiddenLayer(Matrix2D weights, Matrix2D bias, int samples){
        this(weights.c(), samples, weights.r(), 0);
        weights.copy(this.weights);
        bias.copy(this.bias);
    }


    /**
     * Required feed forward method. Feeds data from previous layer to next
     * layer, applying this layer's transformations along the way. Should use
     * previous layer's "output" and set this layer's "output"
     */
    public abstract void forward();


    /**
     * Required backpropagation backward pass method. Calculates gradients of
     * weight and input (and bias). Should set the "deltaWeight", "deltaBias",
     * and "deltas" of this layer.
     */
    public abstract void backward();


    /**
     * Required update method. Should update "weights" and "bias" of this layer.
     *
     * @param rate learning rate (passed in by the neural net
     */
    public void update(double rate){
        this.weights.add(this.deltaWeight.multiply(-rate, this.deltaWeight), this.weights);
        this.bias.add(this.deltaBias.multiply(-rate, this.deltaBias), this.bias);
    }


    /** Gets this layer's weights. **/
    public Matrix2D W() { return this.weights; }

    /** Gets this layer's bias vector. **/
    public Matrix2D b() { return this.bias; }

    /** Gets this layers' deltaWeight **/
    public Matrix2D dW() { return this.deltaWeight; }

    /** Gets this layer's deltaBias **/
    public Matrix2D db() { return this.deltaBias; }

    /** Gets the transpose matrix used to hold the transpose of this layer's weights. **/
    public Matrix2D WT() { return this.weightsT; }

    /** Gets the transpose matrix used to hold the transpose of the previous layer's output. **/
    public Matrix2D XT() { return this.inputsT; }

    /** Gets the ones matrix used to calculate this layer's deltaBias **/
    public Matrix2D ones() { return this.ones; }

}
