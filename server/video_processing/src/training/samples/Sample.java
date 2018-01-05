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

import java.io.Serializable;

/**
 * This class contains the data for a single sample for passing into a network
 * for training.
 *
 * @author Austin Milt
 */
public class Sample implements Serializable {
    private String id;
    private Matrix2D attributes;
    private Matrix2D label;

    /** Makes a new sample with the given attributes and label matrix, and a null string identifier. */
    public Sample(Matrix2D attributes, Matrix2D label){ this(attributes, label, null); }

    /** Makes a new sample with the given attributes, label matrix, and string identifier. */
    public Sample(Matrix2D attributes, Matrix2D label, String identifier){
        this.id = identifier;
        this.attributes = attributes.copy();
        this.label = label.copy();
    }

    /** Gets the string identifier of the sample. */
    public String get_id() { return this.id; }

    /** Gets the attribute matrix of the sample. */
    public Matrix2D get_attributes() { return this.attributes; }

    /** Gets the label matrix of the sample. */
    public Matrix2D get_label() { return this.label; }
}
