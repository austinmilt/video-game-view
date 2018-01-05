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
 * Generic (abstract) Layer class for organizing layers within a neural net.
 *
 * @author Austin Milt
 */
public abstract class Layer implements Serializable {

    // private variables shared across child classes
    private transient Layer next;
    private transient Layer previous;
    private Matrix2D output;
    private Matrix2D deltas;


    /**
     * Requires a forward pass method in all derived layers.
     */
    public abstract void forward();


    /**
     * Requires a backward pass method in all derived layers.
     */
    public abstract void backward();


    /**
     * Requires an update method that accepts the learning rate in all derived layers.
     * @param rate
     */
    public abstract void update(double rate);


    /**
     * Requires all derived layers be able to reduce() to a version that can accept a
     * single test sample with the same attributes as the calling layer.
     * @return
     */
    public abstract Layer reduce();


    /**
     * Creates a new empty Layer, but shouldnt really be called.
     */
    public Layer(){}


    /**
     * Creates a new layer with the given "output" shape.
     *
     * @param rows rows of the output matrix
     * @param columns columns of the output matrix
     */
    public Layer(int rows, int columns){ this.output = new Matrix2D(rows, columns); }


    /** Get the next layer. */
    public Layer get_next() { return this.next; }

    /** Get the next layer. */
    public Layer next() { return this.next; }

    /** Get the previous layer. */
    public Layer get_previous() { return this.previous; }

    /** Get the previous layer. */
    public Layer previous() { return this.previous; }

    /** Get this layer's "output" */
    public Matrix2D get_output() { return this.output; }

    /** Get this layer's "output" */
    public Matrix2D O() { return this.output; }

    /** Get this layer's "deltas" */
    public Matrix2D D() { return this.deltas; }

    /** Set this layer's "deltas" */
    public void set_deltas(Matrix2D deltas) { this.deltas = deltas; }

    /** Set this layer's next layer. */
    public void set_next(Layer L){this.next = L;}

    /** Set this layer's previous layer. */
    public void set_previous(Layer L){this.previous = L;}

    /** Set this layer's "output" matrix. */
    public void set_output(Matrix2D output){output.copy(this.output);}
}
