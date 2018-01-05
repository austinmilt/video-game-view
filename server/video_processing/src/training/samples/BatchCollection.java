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

import java.util.*;
import java.util.stream.Collectors;


/**
 * This class is a container for Batch objects, to be used when feeding
 * data into a neural net for training.
 *
 * @author Austin Milt
 * @see Batch
 */
public class BatchCollection implements Iterable<Batch> {

    private int batchSize;
    private int inputs;
    private int outputs;
    private int batchCount = 0;
    private ArrayList<Batch> batches = new ArrayList<>();

    /** Creates and empty collection. */
    public BatchCollection() {}

    /** Creates a collection of batches from those provided. */
    public BatchCollection(Batch[] batches) { this.add(batches); }


    /** Adds 1+ Batch's to the collection. */
    public void add(Batch ... batches) {
        for (Batch b : batches){
            if (this.batchCount == 0){
                this.batchSize = b.get_size();
                this.inputs = b.get_inputs();
                this.outputs = b.get_outputs();
            }
            if (b.get_size() != this.batchSize){ throw new IllegalArgumentException("All batches in the collection must have the same number of samples."); }
            else if (b.get_inputs() != this.inputs) { throw new IllegalArgumentException("All batches in the collection must have the same number of inputs."); }
            else if (b.get_outputs() != this.outputs) { throw new IllegalArgumentException("All batches in the collection must have the same number of outputs."); }
            this.batches.add(b);
            this.batchCount += 1;
        }
    }


    /**
     * Makes a new collection by randomly drawing samples from those provided.
     * <p>
     * Samples will not be repeated within a batch, but may be repeated across them.
     *
     * @param samples samples to draw batches from
     * @param batchSize number of samples that should appear in each batch
     * @param nBatches number of batches in the collection
     * @return a new collection of batches
     */
    public static BatchCollection make_batches(Sample[] samples, int batchSize, int nBatches) {
        List<Integer> order = Arrays.stream(Batch.range(samples.length)).boxed().collect(Collectors.toList());
        BatchCollection collection = new BatchCollection();
        int[] toTake = new int[batchSize];
        for (int b = 0; b < nBatches; b++) {
            Collections.shuffle(order);
            for (int i = 0; i < batchSize; i++) { toTake[i] = order.get(i); }
            collection.add(new Batch(samples, toTake));
        }
        return collection;
    }


    /**
     * Randomly splits the collection into smaller collections of the given proportional
     * sizes.
     * <p>
     * Proportions will be scaled so they add up to 1.
     * <p>
     * Collections earlier in the list of proportions get precedent when rounding
     * absolute sizes of output collections, e.g. if the first collection needs
     * 4 batches and the second needs 2, but there are only 5 batches total, then the
     * second collection will only wind up with 1 batch.
     *
     * @param proportion proportion of batches that should appear in the ith collection
     * @return array of collections of the given (approximate) proportional sizes of the original
     */
    public BatchCollection[] split(double ... proportion){

        // calculate total of proportions for scaling of absolute sizes
        double propTotal = 0;
        for (double p : proportion) { propTotal += p; }

        // randomly order caller's batches
        BatchCollection[] output = new BatchCollection[proportion.length];
        List<Integer> order = Arrays.stream(Batch.range(this.batchCount)).boxed().collect(Collectors.toList());
        Collections.shuffle(order);

        // build the output
        BatchCollection C;
        int size;
        int k = 0;
        for (int i = 0; i < output.length; i++){

            // calculate how big this batch should be and check that it's not empty
            output[i] = new BatchCollection();
            size = (int) Math.round((proportion[i]/propTotal)*this.batchCount);

            // add batches to the collection
            for (int j = 0; j < size; j++){
                if (k == this.batchCount) { break; }
                output[i].add(this.get(order.get(k)).copy());
                k += 1;
            }
        }
        return output;
    }

    /** Gets the number of samples per batch. */
    public int get_size() { return this.batchSize; }

    /** Gets the number of input data attributes per sample. */
    public int get_inputs() { return this.inputs; }

    /** Gets the size of output labels per sample. */
    public int get_outputs() { return this.outputs; }

    /** Gets the number of batches in the collection. */
    public int get_count() { return this.batchCount; }

    /** Gets the ith batch from the collection. */
    public Batch get(int i){ return this.batches.get(i); }

    @Override
    public Iterator<Batch> iterator() {
        return new BatchIterator(this);
    }
    private class BatchIterator implements Iterator {
        private Iterator<Batch> iter;
        public BatchIterator(BatchCollection outter){ this.iter = outter.batches.iterator(); }
        public boolean hasNext(){ return this.iter.hasNext(); }
        public Batch next() { return this.iter.next(); }
    }
}
