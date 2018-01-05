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

import training.ann.layers.*;
import training.math.Matrix2D;
import training.samples.Batch;
import training.samples.BatchCollection;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;


/**
 * This is a basic neural network, accepting vector inputs and training/predicting on them.
 *
 * @author Austin Milt
 */
public class ANN extends Network implements Serializable {

    ///////////////////////////////////////////////////////////////////////////
    // CLASS VARIABLES ////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    private transient InputLayer first;
    private transient Layer last;
    private transient SoftmaxLayer out = null;
    private double baseLearnRate = 0.001;
    private double learnRate = baseLearnRate;
    private double decayRate = 0.0001;
    private transient boolean forwardFlag = false;
    private transient int size;
    private HashMap<String, Matrix2D> id2label;
    private HashMap<Integer, String> num2id;
    private HashMap<String, Integer> id2num;



    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /** Creates an empty neural net. */
    public ANN() {}



    ///////////////////////////////////////////////////////////////////////////
    // TRAINING/PREDICTION METHODS ////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Feeds forward through the network the given input data and does dropout (i.e. during training).
     *
     * @param data test data to feed forward through the network.
     */
    public void forward(Matrix2D data) { this.forward(data, true); }

    /**
     * Feeds forward through the network the given input data
     *
     * @param data test data to feed forward through the network.
     * @param drop flag to indicate whether to do dropout on activation layers (e.g. training vs prediction)
     */
    public void forward(Matrix2D data, boolean drop) {

        // catch easy mistakes (i.e. calling this too early)
        if ((!(this.first instanceof InputLayer)) || (this.out == null)) {
            throw new RuntimeException("Network is missing some necessary layers.");
        } else if (this.forwardFlag) {
            throw new RuntimeException("Must perform a backward pass before another forward pass.");
        }

        // loop over layers, doing the forward pass for each
        this.first.forward(data);
        Layer layer = this.first.get_next();
        while (layer != null) {
            if (layer instanceof LeakyReLULayer) { ((LeakyReLULayer) layer).forward(drop); }
            else { layer.forward(); }
            layer = layer.get_next();
        }
        this.forwardFlag = true;
    }


    /**
     * Does backward pass through the network with the test answers (assuming they match with the input data),
     * asking layers to calculate gradients of weight, bias, and input.
     *
     * @param labels correct answer labels from forward pass
     */
    public void backward(Matrix2D labels) {

        // check for easy mistakes, like running too soon
        if ((this.first == null) || (this.out == null)) {
            throw new RuntimeException("Network is missing some necessary layers.");
        } else if (!this.forwardFlag) {
            throw new RuntimeException("Must perform a forward pass before (another) backward pass.");
        }

        // loop backward over layers, doing backward pass for each and updating weights
        this.out.backward(labels);
        Layer layer = this.last;
        while (layer != this.first) {
            layer.backward();
            layer = layer.get_previous();
        }
        this.forwardFlag = false;
    }


    /**
     * Updates all the layers in the network with the learning rate set in the network.
     */
    public void update() {
        Layer layer = this.first.get_next();
        while (layer != null) {
            layer.update(this.learnRate);
            layer = layer.get_next();
        }
    }


    /**
     * Calculates the learning rate for a particular epoch using the formula:
     * LearningRate = LearningRate * 1/(1 + decay * epoch)
     *
     * @param epoch epoch to set the learning rate for
     * @return
     */
    public void update_learnrate(int epoch) {
        this.learnRate = this.learnRate * 1d / (1d + this.decayRate * epoch);
    }


    /**
     * Trains the neural net by backpropagation over minibatches.
     *
     * @param batches collection of batches for training the network
     * @param epochs number of iterations to perform backpropagation
     */
    public void train(BatchCollection batches, int epochs){
        int count = batches.get_count();
        double loss;
        Matrix2D Y;
        for (int j = 0; j < epochs; j++){
            update_learnrate(j);
            loss = 0;
            for (Batch b : batches){
                Y = b.get_labels();
                this.forward(b.get_data(), true);
                loss += this.out.loss(Y);
                this.backward(Y);
                this.update();
            }
            System.out.print(String.format("\rEpoch = %4d. Loss = %.4f", j+1, loss/count));
        }
    }


    /**
     * Tests how well the neural net performs at getting the correct label
     * on a dataset not used for training.
     * <p>
     * Assumes output labels are one-hot vectors.
     *
     * @param batches validation set
     */
    public double validate(BatchCollection batches){
        double total = 0d;
        double correct = 0d;
        Matrix2D predictions;
        Matrix2D Y;
        double yHot;
        double pHot;
        int yInd;
        int pInd;
        boolean preFlag = this.forwardFlag;

        // pass batches through and check if every label is correct
        for (Batch b : batches){
            Y = b.get_labels();
            this.forwardFlag = false;
            this.forward(b.get_data(), false);
            predictions = this.out.get_output();

            // for each sample, determine whether the prediction is correct
            for (int j = 0; j < predictions.c(); j++){
                yHot = Double.NEGATIVE_INFINITY;
                pHot = Double.NEGATIVE_INFINITY;
                yInd = -1;
                pInd = -2;

                // find the maximum value in the prediction and actual columns
                for (int i = 0; i < predictions.r(); i++){
                    if (Y.get(i,j) > yHot) { yHot = Y.get(i,j); yInd = i; }
                    if (predictions.get(i,j) > pHot) { pHot = predictions.get(i,j); pInd = i; }
                }

                // update accuracy
                if (yInd == pInd) { correct += 1d; }
                total += 1d;
            }
        }
        this.forwardFlag = preFlag;
        return correct/total;
    }


    /** Predicts the label of the input based on the trained weights of the network. */
    public Matrix2D predict(Matrix2D input){
        boolean preFlag = this.forwardFlag;
        this.forwardFlag = false;
        this.forward(input, false);
        Matrix2D prediction = this.out.get_output();
        this.forwardFlag = preFlag;
        return prediction;
    }


    public String predict(Matrix2D input, Collection<String> validIDs) {
        Matrix2D prediction = predict(input);
        double maxValue = Double.NEGATIVE_INFINITY;
        double p;
        String bestID = "";
        if (validIDs  == null) { validIDs = this.id2num.keySet(); }
        for (String id : validIDs) {
            p = prediction.get(this.id2num.get(id), 0);
            if (p > maxValue) {
                maxValue = p;
                bestID = id;
            }
        }
        return bestID;
    }


    /** Converts a predicted label to the closest matching identifier. */
    public String id_by_minloss(Matrix2D prediction){

        // Find the label with the minimum different from the prediction.
        double minLoss = Double.POSITIVE_INFINITY;
        double loss;
        String output = "unknown";
        for (String id : this.id2label.keySet()) {
            loss = this.out.loss(prediction, this.id2label.get(id));
            if (loss < minLoss) {
                minLoss = loss;
                output = id;
            }
        }
        return output;
    }


    /** Converts a predicted label matrix to identifier associated by one-hot integer label. */
    public String id_by_num(Matrix2D prediction){
        return this.num2id.get(prediction.argmax()[0]);
    }



    ///////////////////////////////////////////////////////////////////////////
    // LAYER MANAGEMENT ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /** Adds an InputLayer (should be first layer added). */
    public void add_layer(InputLayer layer){
        if (this.out != null){
            throw new RuntimeException("Cannot add layers after the output layer.");
        }
        else if (this.first != null){
            throw new RuntimeException("Cannot have multiple input layers.");
        }
        else if (this.last != null){
            throw new RuntimeException("Must add input layer before hidden layers.");
        }
        this.first = layer;
        this.last = layer;
        this.size += 1;
    }


    /** Adds a hidden layer, e.g. a Multilayer Perceptron. */
    public void add_layer(HiddenLayer layer){
        if (this.out != null){
            throw new RuntimeException("Cannot add layers after the output layer.");
        }
        else if (this.first == null){
            throw new RuntimeException("Must have added an input layer first.");
        }
        Layer last = this.last;
        this.last = layer;
        layer.set_next(last.get_next());
        last.set_next(layer);
        layer.set_previous(last);
        this.size += 1;
    }


    /** Adds a LeakyReLULayer (should really only be added after a HiddenLayer). */
    public void add_layer(LeakyReLULayer layer){
        if (this.out != null){
            throw new RuntimeException("Cannot add layers after the output layer.");
        }
        else if (this.first == null){
            throw new RuntimeException("Must have added an input layer first.");
        }
        Layer last = this.last;
        this.last = layer;
        layer.set_next(last.get_next());
        last.set_next(layer);
        layer.set_previous(last);
        this.size += 1;
    }


    /** Adds a SoftmaxLayer (should be the last layer added). */
    public void add_layer(SoftmaxLayer layer){
        if (this.out != null){
            throw new RuntimeException("Cannot add layers after the output layer.");
        }
        else if (this.first == null){
            throw new RuntimeException("Must have added an input layer first.");
        }
        this.out = layer;
        this.last.set_next(layer);
        this.out.set_previous(this.last);
        this.size += 1;
    }


    /** Assess what kind of layer addition method is needed and adds the layer. */
    public void add_layer(Layer layer){
        if (layer instanceof InputLayer) { this.add_layer((InputLayer) layer); }
        else if (layer instanceof SoftmaxLayer) { this.add_layer((SoftmaxLayer) layer); }
        else if (layer instanceof LeakyReLULayer) { this.add_layer((LeakyReLULayer) layer); }
        else if (layer instanceof HiddenLayer) { this.add_layer((HiddenLayer) layer); }
        else { throw new IllegalArgumentException("Invalid layer type."); }
    }



    ///////////////////////////////////////////////////////////////////////////
    // MANIPULATORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /** Sets the baseline learning rate of the network and resets the current learning rate. */
    public void set_baserate(double rate) { this.baseLearnRate = rate; this.learnRate = rate; }

    /** Sets the current learning rate of the network. */
    public void set_rate(double rate) { this.learnRate = rate; }

    /** Sets the decay rate of the learning rate. */
    public void set_decay(double decay) { this.decayRate = decay; }

    /** Sets the mapping between label matrices and preSample identifiers. Also sets the num2id map.*/
    public void set_labelmap(HashMap<String, Matrix2D> map){
        this.id2label = map;

        // assign num2id and make sure every id has a unique number
        this.num2id = new HashMap<>();
        this.id2num = new HashMap<>();
        int num;
        for (String id : map.keySet()){
            num = map.get(id).argmax()[0];
            if (this.num2id.containsKey(num)) { throw new IllegalArgumentException("Each id must be associated with a unique one-hot vector."); }
            this.num2id.put(num, id);
            this.id2num.put(id, num);
        }
    }


    /** Reduces the size of the neural net to pass forward a single test preSample, e.g. for prediction after training. */
    public ANN reduce(){
        ANN output = new ANN();
        output.set_rate(this.baseLearnRate);
        output.set_labelmap(this.id2label);
        Layer layer = this.first;
        while (layer != null){
            output.add_layer(layer.reduce());
            layer = layer.get_next();
        }
        return output;
    }

    /** Gets the number of layers in the network. */
    public int get_size() { return this.size; }

    /** Gets the current learning rate set in the network. */
    public double get_rate() { return this.learnRate; }

    /** Gets the current learning rate set in the network. */
    public double get_baserate() { return this.baseLearnRate; }

    /** Gets the shape that inputs must be into the network. */
    public int[] get_input_shape() { return this.first.O().get_shape(); }

    /** Gets the shape that outputs of the network will be. */
    public int[] get_output_shape() { return this.out.O().get_shape(); }

    /** Gets the mapping from one-hot vector index to string identifier used in id_by_num. */
    public HashMap<Integer, String> get_num2id() { return num2id; }

    /** Gets the mapping from string identifier to one-hot vector index. */
    public HashMap<String, Integer> get_id2num() { return id2num; }


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
     * Loads an ANN that has been saved to disk.
     *
     * @param path path to the serialized ANN file
     * @return the loaded ANN
     */
    public static ANN load(String path) throws IOException {
        ObjectInputStream objectinputstream = null;
        FileInputStream streamIn;
        ANN output = null;
        try {
            streamIn = new FileInputStream(path);
            objectinputstream = new ObjectInputStream(streamIn);
            output = (ANN) objectinputstream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(objectinputstream != null){
                objectinputstream .close();
            }
        }
        return output;
    }


    /**
     * Used for serialization of the object.
     *
     * @param out
     * @throws IOException
     * @see Serializable
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeDouble(this.baseLearnRate);
        out.writeInt(this.size); // write the number of layers that must be reloaded
        Layer layer = this.first;
        while (layer != null) {
            out.writeObject(layer);
            layer = layer.next();
        }
    }


    /**
     * Used to recreate a serialized object
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     * @see Serializable
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.set_rate(in.readDouble());
        int layerCount = in.readInt();
        for (int i = 0; i < layerCount; i++) {
            this.add_layer((Layer) in.readObject());
        }
    }
}
