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
 * This class of output layer performs softmax with log loss evaluation.
 *
 * @author Austin Milt
 */
public class SoftmaxLayer extends HiddenLayer implements Serializable {

    private Matrix2D diff;
    private Matrix2D L;

    /**
     * Creates a new SoftmaxLayer.
     * @see HiddenLayer
     */
    public SoftmaxLayer(int inputs, int samples, int outputs, double bias){
        super(inputs, samples, outputs, bias);
        this.diff = new Matrix2D(outputs, samples);
        this.L = new Matrix2D(outputs, samples);
    }


    /**
     * Creates a new SoftmaxLayer with the given parameters.
     *
     * @param weights weights to set for the new layer
     * @param bias bias to set for the new layer
     * @param samples number of samples the layer should accept
     */
    public SoftmaxLayer(Matrix2D weights, Matrix2D bias, int samples){
        this(weights.c(), samples, weights.r(), 0);
        weights.copy(this.W());
        bias.copy(this.b());
    }


    /**
     * Performs forward pass for this layer (softmax).
     */
    public void forward(){
        Matrix2D W = this.W();
        Matrix2D X = this.get_previous().get_output();
        Matrix2D b = this.b();
        Matrix2D O = this.O();
        W.dot(X, O).add_to_cols(b, O).subtract(O.max(), O).exp(O).divide(O.sum(), O);
    }


    /** Backward pass for an output layer requires an answer matrix. */
    public void backward(){throw new RuntimeException("Must supply input labels."); }


    /** Performs backward pass for this layer, dependent on the correct answer for the forward pass preSample. */
    public void backward(Matrix2D labels){
        Matrix2D dX = this.D();
        Matrix2D X = this.get_previous().O();
        Matrix2D XT = this.XT();
        Matrix2D O = this.O();
        Matrix2D W = this.W();
        Matrix2D dW = this.dW();
        Matrix2D ones = this.ones();
        Matrix2D db = this.db();
        O.subtract(labels, this.diff);
        W.transpose(this.WT()).dot(this.diff, dX);
        this.diff.dot(X.transpose(XT), dW);
        this.diff.dot(ones, db);
    }


    /** Reduces the layer to accept a single test preSample. */
    public SoftmaxLayer reduce() { return new SoftmaxLayer(this.W(), this.b(), 1); }


    /** Calculates log loss for the given test answers assuming they match the test input from the forward pass. */
    public double loss(Matrix2D labels){
        return -this.get_output().log(this.L).multiply(labels, this.L).sum();
    }


    /** Calculates the loss for a prediction given the answer. */
    public double loss(Matrix2D prediction, Matrix2D answer) { return -prediction.log().multiply(answer).sum(); }
}
