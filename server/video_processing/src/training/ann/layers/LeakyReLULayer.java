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
 * This class of layer performs Leak ReLU activations for a layer, passing
 * along values the same shape as its input.
 *
 * @author Austin Milt
 */
public class LeakyReLULayer extends Layer implements Serializable {
    private static final double DROPRATE = 1.;
    private double rate;
    private double dropout = 1d;
    private boolean doDrop;
    private double dropInv;

    /**
     * Creates a new LeakyReLULayer with the given leak rate and no dropout.
     *
     * @param inputs number of data per test sample, e.g. number of pixels per image
     * @param samples number of samples, e.g. columns of input and output
     * @param rate leak rate for values < 0
     */
    public LeakyReLULayer(int inputs, int samples, double rate){
        this(inputs, samples, rate, DROPRATE);
    }


    /**
     * Creates a new LeakyReLULayer with the given leak rate and dropout regularization rate.
     *
     * @param inputs number of data per test sample, e.g. number of pixels per image
     * @param samples number of samples, e.g. columns of input and output
     * @param rate leak rate for values < 0
     * @param dropout probability that a neuron is activated (1 = always activated)
     */
    public LeakyReLULayer(int inputs, int samples, double rate, double dropout){
        super(inputs, samples);
        this.rate = rate;
        this.dropout = dropout;
        this.doDrop = (dropout == 1);
        this.dropInv = 1d / dropout;
        this.set_deltas(new Matrix2D(inputs, samples));
    }


    /** LeakyReLULayers do nothing during update */
    public void update(double rate){}


    /** Performs forward propagation for this layer with dropout. */
    public void forward() { forward(true); }

    /** Performs forward propagation for this layer, i.e. Max(leakRate*x, x) */
    public void forward(boolean drop){
        Matrix2D X = this.get_previous().O();
        Matrix2D O = this.get_output();
        double v;
        double d = 1d;
        for (int c = 0; c < O.size(); c++) {

            // do dropout if needed
            if (drop && this.doDrop){
                if (Math.random() > this.dropout) { d = 0d; }
                else { d = this.dropInv; }
            }

            // calculate activation
            v = X.get_by_flat(c);
            if (v < 0) { O.set_by_flat(c, v * this.rate * d); }
            else { O.set_by_flat(c, v * d); }
        }
    }


    /** Performs backward propagation for this layer. */
    public void backward(){
        Matrix2D deltas = this.get_next().D();
        Matrix2D dX = this.D();
        Matrix2D O = this.O();
        double v;
        double d;
        for (int c = 0; c < O.size(); c++){
            v = O.get_by_flat(c);
            d = deltas.get_by_flat(c);
            if (v <= 0) { dX.set_by_flat(c, this.rate*d); }
            else { dX.set_by_flat(c, d); }
        }
    }


    /** Reduces the layer to accept a single test sample. */
    public LeakyReLULayer reduce() { return new LeakyReLULayer(this.O().r(), 1, this.rate); }
}
