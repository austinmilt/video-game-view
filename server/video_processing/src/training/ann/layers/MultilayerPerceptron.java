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
 * This class does typical fully connected layer math on inputs, i.e. WX+b.
 *
 * @author Austin Milt
 */
public class MultilayerPerceptron extends HiddenLayer implements Serializable {

    /**
     * Creates a new MultilayerPerceptron.
     * @see HiddenLayer
     */
    public MultilayerPerceptron(int inputs, int samples, int outputs, double bias){
        super(inputs, samples, outputs, bias);
    }

    /**
     * Creates a new MultilayerPerceptron with the given parameters.
     *
     * @param weights weights to set for the new layer
     * @param bias bias to set for the new layer
     * @param samples number of samples the layer should accept
     */
    public MultilayerPerceptron(Matrix2D weights, Matrix2D bias, int samples){
        super(weights, bias, samples);
    }


    /** Performs forward propagation for this layer, i.e. WX+b */
    public void forward(){
        Matrix2D W = this.W();
        Matrix2D X = this.get_previous().get_output();
        Matrix2D b = this.b();
        Matrix2D O = this.O();
        W.dot(X, O).add_to_cols(b, O);
    }


    /** Performs backward pass for this layer, updating deltas of parameters and inputs. */
    public void backward(){
        Matrix2D dX = this.D();
        Matrix2D deltas = this.get_next().D();
        Matrix2D W = this.W();
        Matrix2D dW = this.dW();
        Matrix2D db = this.db();
        Matrix2D WT = this.WT();
        Matrix2D X = this.get_previous().O();
        Matrix2D XT = this.XT();
        W.transpose(WT).dot(deltas, dX);
        deltas.dot(X.transpose(XT), dW);
        deltas.dot(this.ones(), db);
    }


    /** Reduces the layer to accept a single test sample. */
    public MultilayerPerceptron reduce(){
        return new MultilayerPerceptron(this.W(), this.b(), 1);
    }
}
