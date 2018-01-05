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


package training.math;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class contains data storage, math methods, and transforms for two-
 * dimensional matrices. It is the primary data manipulation format used
 * throughout this project.
 *
 * @author Austin Milt
 */
public class Matrix2D implements Iterable<Double>, Serializable {

    ///////////////////////////////////////////////////////////////////////////
    // CLASS AND INSTANCE VARIABLES ///////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    private double[] data;
    private int[][] flat2nested;
    private int[][] nested2flat;
    private int[] shape = new int[2];
    private int size;
    private HashMap<String, Object> attributes = new HashMap<>();

    private static final boolean INPLACE = true;
    private static final boolean NEW = false;


    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a zeroed matrix of the given size.
     *
     * @param rows number of rows to put in the matrix
     * @param columns number of columns to put in the matrix
     */
    public Matrix2D(int rows, int columns){
        this.data = new double[rows*columns];
        this.shape[0] = rows;
        this.shape[1] = columns;
        this.size = rows*columns;
        this.nested2flat = new int[rows][columns];
        this.flat2nested = new int[rows*columns][2];
        int c = 0;
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                this.nested2flat[i][j] = c;
                this.flat2nested[c] = new int[]{i, j};
                c += 1;
            }
        }
    }


    /**
     * Creates a new matrix with the given data.
     *
     * @param data data array to turn into a matrix
     */
    public Matrix2D(double[][] data){
        this(data.length, data[0].length);
        for (int c = 0; c < this.size; c++){
            this.data[c] = data[this.flat2nested[c][0]][this.flat2nested[c][1]];
        }
    }


    /**
     * Creates a new matrix filled with a constant value.
     *
     * @param rows number of rows in the new matrix
     * @param columns number of columns in the new matrix
     * @param v value that every cell should have
     * @return constant-value matrix
     */
    public static Matrix2D constant(int rows, int columns, double v){
        return new Matrix2D(rows, columns).add(v);
    }


    /**
     * Creates a new matrix filled with 1s.
     * @see <code>Matrix2D.constant</code>
     */
    public static Matrix2D ones(int rows, int columns){
        return constant(rows, columns, 1);
    }


    /**
     * Creates a new matrix with uniform random values.
     *
     * @param rows number of rows in the new matrix
     * @param columns number of columns in the new matrix
     * @param min minimum value for cells to take
     * @param max maximum value for cells to take
     * @return a new matrix with uniformly distributed values in the range (min, max)
     */
    public static Matrix2D random(int rows, int columns, double min, double max){
        Matrix2D out = new Matrix2D(rows, columns);
        double range = max - min;
        for (int i = 0; i < (rows*columns); i++){
            out.data[i] = Math.random()*range+min;
        }
        return out;
    }


    /**
     * Creates a new matrix of uniformly distributed random values up to the maximum provided.
     * @see <code>Matrix2D.random</code>
     */
    public static Matrix2D random(int rows, int columns, double max){
        return Matrix2D.random(rows, columns, 0, max);
    }


    /**
     * Creates a new matrix of uniformly distributed random values between 0 and 1.
     * @see <code>Matrix2D.random</code>
     */
    public static Matrix2D random(int rows, int columns){
        return Matrix2D.random(rows, columns, 0, 1);
    }


    /**
     * Creates a square identity matrix the same size as the caller.
     * @return
     */
    public Matrix2D identity(){
        if (this.shape[0] != this.shape[1]){
            throw new IllegalArgumentException("Caller must be square.");
        }
        return identity(this.shape[0]);
    }


    /**
     * Creates an identity matrix of the given size.
     *
     * @param size number of rows and columns in the output matrix
     * @return identity matrix with 1s on diagonal and 0s everywhere else
     */
    public static Matrix2D identity(int size){
        Matrix2D output = new Matrix2D(size, size);
        for (int c = 0; c < size; c++){
            output.data[output.nested2flat[c][c]] = 1;
        }
        return output;
    }


    /** Creates a new matrix that is a copy of the caller. */
    public Matrix2D copy() { return copy(new Matrix2D(this.shape[0], this.shape[1])); }


    /** Creates a copy of the caller in the target matrix. */
    public Matrix2D copy(Matrix2D target){
        Matrix2D output;
        if (target == null){output = new Matrix2D(this.shape[0], this.shape[1]);}
        else {output = target;}
        if (!valid_shape(output.shape, this.shape)) { throw new IllegalArgumentException("Destination matrix must be same shape as caller."); }
        System.arraycopy(this.data, 0, output.data, 0, this.size);
        return output;
    }



    ///////////////////////////////////////////////////////////////////////////
    // HELPER FUNCTIONS ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    // Tests to see if a target matrix is a valid shape for the operation
    private boolean valid_shape(int rT, int cT, int[] c){ return valid_shape(rT, cT, c[0], c[1]); }
    private boolean valid_shape(int[] t, int rC, int cC){ return valid_shape(t[0], t[1], rC, cC); }
    private boolean valid_shape(int[] t, int[] c){ return valid_shape(t[0], t[1], c[0], c[1]); }
    private boolean valid_shape(int rTest, int cTest, int rCorrect, int cCorrect){
        return (rTest == rCorrect) && (cTest == cCorrect);
    }
    private void throw_shape_error(boolean valid){
        if (!valid) { throw new IllegalArgumentException("Target is invalid shape for operation."); }
    }


    // tests if target is self or given target
    private Matrix2D get_target(Matrix2D target){
        if (target == null){ return this; }
        else { return target; }
    }


    // tests if target is self or new matrix and creates the new matrix if needed
    private Matrix2D get_target_by_flag(boolean flag){ return get_target_by_flag(flag, this.shape[0], this.shape[1]); }
    private Matrix2D get_target_by_flag(boolean flag, int rows, int columns){
        if (flag == INPLACE) { return this; }
        else if (flag == NEW) { return new Matrix2D(rows, columns); }
        throw new IllegalArgumentException("Unrecognized target output flag.");
    }



    ///////////////////////////////////////////////////////////////////////////
    // ARITHMETIC /////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Multiplies the contents of this Matrix2D by a scalar and returns a new Matrix2D.
     *
     * @param scalar scalar to use in multiplication
     * @return a new Matrix2D of the same shape as the original, multiplied by the scalar
     */
    public Matrix2D multiply(double scalar) { return multiply(scalar, NEW); }

    /**
     * Multiplies the contents of this Matrix2D by a scalar and either returns a
     * new Matrix2D or overwrites the calling Matrix2D.
     *
     * @param scalar scalar to use in multiplication
     * @param target output target (<code>Matrix2D.NEW</code> or <code>Matrix2D.INPLACE</code>)
     * @return a Matrix2D of the same shape as the original, multiplied by the scalar
     */
    public Matrix2D multiply(double scalar, boolean target) { return multiply(scalar, get_target_by_flag(target)); }

    /**
     * Multiplies the contents of this Matrix2D by a scalar and writes the
     * contents to the given target.
     *
     * @param scalar scalar to use in multiplication
     * @param target output target
     * @return updates the target Matrix2D with the product of multiplication
     */
    public Matrix2D multiply(double scalar, Matrix2D target) {
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]*scalar;
        }
        return target;
    }


    /**
     * Multiplies the calling Matrix2D by the given Matrix2D, element-wise.
     *
     * @param B Matrix2D to use in multiplication
     * @return new Matrix2D of the same shape as the input
     */
    public Matrix2D multiply(Matrix2D B){ return multiply(B, NEW); }

    /**
     * Multiplies the calling Matrix2D by the given Matrix2D, element-wise
     * and returns either a new Matrix2D or overwrites the caller.
     *
     * @param B Matrix2D to use in multiplication
     * @param target output target (<code>Matrix2D.NEW</code> or <code>Matrix2D.INPLACE</code>)
     * @return new Matrix2D of the same shape as the input, or overwrites original
     */
    public Matrix2D multiply(Matrix2D B, boolean target){ return multiply(B, get_target_by_flag(target)); }

    /**
     * Multiplies the calling Matrix2D by the given Matrix2D, element-wise,
     * overwrites the cells of the target, and returns that target.
     *
     * @param B Matrix2D to use in multiplication
     * @param target output target
     * @return updates the target Matrix2D with the product of multiplication
     */
    public Matrix2D multiply(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]*B.data[c];
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but with division. */
    public Matrix2D divide(double scalar){ return divide(scalar, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with division. */
    public Matrix2D divide(double scalar, boolean target){ return divide(scalar, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with division. */
    public Matrix2D divide(double scalar, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]/scalar;
        }
        return target;
    }

    /** Analogous to <code>Matrix2D.multiply</code>, but with division. */
    public Matrix2D divide(Matrix2D B) { return divide(B, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with division. */
    public Matrix2D divide(Matrix2D B, boolean target) { return divide(B, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with division. */
    public Matrix2D divide(Matrix2D B, Matrix2D target) {
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]/B.data[c];
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but with addition. */
    public Matrix2D add(double additive){ return add(additive, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with addition. */
    public Matrix2D add(double additive, boolean target){ return add(additive, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with addition. */
    public Matrix2D add(double additive, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]+additive;
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but with addition. */
    public Matrix2D add(Matrix2D B){ return add(B, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with addition. */
    public Matrix2D add(Matrix2D B, boolean target){ return add(B, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with addition. */
    public Matrix2D add(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]+B.data[c];
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but with subtraction. */
    public Matrix2D subtract(double subtractive){ return subtract(subtractive, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with subtraction. */
    public Matrix2D subtract(double subtractive, boolean target){ return subtract(subtractive, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with subtraction. */
    public Matrix2D subtract(double subtractive, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]-subtractive;
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but with subtraction. */
    public Matrix2D subtract(Matrix2D B){ return subtract(B, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with subtraction. */
    public Matrix2D subtract(Matrix2D B, boolean target){ return subtract(B, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but with subtraction. */
    public Matrix2D subtract(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = this.data[c]-B.data[c];
        }
        return target;
    }


    /** Returns the sum of values in the caller. */
    public double sum(){
        double esum = 0;
        for (int i = 0; i < this.size; i++){
            esum += this.data[i];
        }
        return esum;
    }


    /** Adds each cell of input to every column of caller. */
    public Matrix2D add_to_cols(Matrix2D B){ return add_to_cols(B, NEW); }

    /** Adds each cell of input to every column of caller. */
    public Matrix2D add_to_cols(Matrix2D B, boolean target){ return add_to_cols(B, get_target_by_flag(target)); }

    /** Adds each cell of input to every column of caller. */
    public Matrix2D add_to_cols(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape[0], target.shape[1]));
        int[] index;
        for (int i = 0; i < this.size; i++){
            index = this.flat2nested[i];
            target.data[i] = this.data[i] + B.get(index[0], 0);
        }
        return target;
    }


    /** Adds each cell of input to every row of caller. */
    public Matrix2D add_to_rows(Matrix2D B){ return add_to_rows(B, NEW); }

    /** Adds each cell of input to every row of caller. */
    public Matrix2D add_to_rows(Matrix2D B, boolean target){ return add_to_rows(B, get_target_by_flag(target)); }

    /** Adds each cell of input to every row of caller. */
    public Matrix2D add_to_rows(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, 1, this.shape[0]));
        int[] index;
        for (int i = 0; i < this.size; i++){
            index = this.flat2nested[i];
            target.data[i] = this.data[i] + B.get(0, index[1]);
        }
        return target;
    }

    /** Sums over the rows of the caller and returns a matrix of a single row. */
    private Matrix2D sum_rows(){ return sum_rows(get_target_by_flag(NEW, 1, this.shape[1])); }

    /** Sums over the rows of the caller and returns a matrix of a single row. */
    private Matrix2D sum_rows(Matrix2D target){
        Matrix2D out = get_target(target);
        throw_shape_error(valid_shape(out.shape, 1, this.shape[1]));
        int[] index;
        for (int i = 0; i < this.size; i++){
            index = this.flat2nested[i];
            out.set(0, index[1], out.get(0, index[1]) + this.data[i]);
        }
        return out;
    }


    /** Sums over the columns of the caller and returns a matrix of a single column. */
    private Matrix2D sum_cols(){ return sum_cols(get_target_by_flag(NEW, this.shape[0], 1)); }

    /** Sums over the rows of the caller and returns a matrix of a single row. */
    private Matrix2D sum_cols(Matrix2D target){
        Matrix2D out = get_target(target);
        throw_shape_error(valid_shape(out.shape, this.shape[0], 1));
        int[] index;
        for (int i = 0; i < this.size; i++){
            index = this.flat2nested[i];
            out.set(index[0], 0, out.get(index[0], 0) + this.data[i]);
        }
        return out;
    }


    /** Sums over the given axis of the caller and returns a matrix of a single row (or column). */
    public Matrix2D sum(int axis) {
        if (axis == 0) {
            return this.sum_rows();
        }
        else if (axis == 1){
            return this.sum_cols();
        }
        else {
            throw new IllegalArgumentException("Invalid axis to sum over.");
        }
    }



    ///////////////////////////////////////////////////////////////////////////
    // LOGICALS/COMPARISONS ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the maximum value in the Matrix2D.
     *
     * @return maximum value cell value
     */
    public double max(){
        double curMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.size; i++){
            if (this.data[i] > curMax){
                curMax = this.data[i];
            }
        }
        return curMax;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the maximum between the comparison values. */
    public Matrix2D max(double v){ return max(v, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the maximum between the comparison values. */
    public Matrix2D max(double v, boolean target){ return max(v, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the maximum between the comparison values. */
    public Matrix2D max(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = Math.max(this.data[c], v);
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the maximum between the comparison values. */
    public Matrix2D max(Matrix2D B){ return max(B, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the maximum between the comparison values. */
    public Matrix2D max(Matrix2D B, boolean target){ return max(B, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the maximum between the comparison values. */
    public Matrix2D max(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = Math.max(this.data[c], B.data[c]);
        }
        return target;
    }


    /**
     * Returns the maximum value in the Matrix2D.
     *
     * @return maximum value cell value
     */
    public double min(){
        double curMin = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.size; i++){
            if (this.data[i] < curMin){
                curMin = this.data[i];
            }
        }
        return curMin;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the minimum between the comparison values. */
    public Matrix2D min(double v){ return min(v, NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the minimum between the comparison values. */
    public Matrix2D min(double v, boolean target){ return min(v, get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the minimum between the comparison values. */
    public Matrix2D min(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = Math.min(this.data[c], v);
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the minimum between the comparison values. */
    public Matrix2D min(Matrix2D B) { return min(B ,NEW); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the minimum between the comparison values. */
    public Matrix2D min(Matrix2D B, boolean target) { return min(B , get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.multiply</code>, but cells are the minimum between the comparison values. */
    public Matrix2D min(Matrix2D B, Matrix2D target) {
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = Math.min(this.data[c], B.data[c]);
        }
        return target;
    }




    /** Tests if elements of caller are greater than other (1/0) and returns a new matrix. */
    public Matrix2D gt(double v) { return gt(v, NEW); }

    /** Tests if elements of caller are greater than other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D gt(double v, boolean target) { return gt(v, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are greater than other (1/0) and updates the target. */
    public Matrix2D gt(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] > v) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }
    
    /** Tests if elements of caller are greater than other (1/0) and returns a new matrix. */
    public Matrix2D gt(Matrix2D B) { return gt(B, NEW); }

    /** Tests if elements of caller are greater than other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D gt(Matrix2D B, boolean target) { return gt(B, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are greater than other (1/0) and updates the target. */
    public Matrix2D gt(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] > B.data[c]) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }


    /** Tests if elements of caller are less than other (1/0) and returns a new matrix. */
    public Matrix2D lt(double v) { return lt(v, NEW); }

    /** Tests if elements of caller are less than other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D lt(double v, boolean target) { return lt(v, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are less than other (1/0) and updates the target. */
    public Matrix2D lt(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] < v) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }

    /** Tests if elements of caller are less than other (1/0) and returns a new matrix. */
    public Matrix2D lt(Matrix2D B) { return lt(B, NEW); }

    /** Tests if elements of caller are less than other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D lt(Matrix2D B, boolean target) { return lt(B, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are less than other (1/0) and updates the target. */
    public Matrix2D lt(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] < B.data[c]) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }


    /** Tests if elements of caller are equal to other (1/0) and returns a new matrix. */
    public Matrix2D eq(double v) { return lt(v, NEW); }

    /** Tests if elements of caller are equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D eq(double v, boolean target) { return eq(v, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are equal to other (1/0) and updates the target. */
    public Matrix2D eq(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] == v) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }

    /** Tests if elements of caller are equal to other (1/0) and returns a new matrix. */
    public Matrix2D eq(Matrix2D B) { return lt(B, NEW); }

    /** Tests if elements of caller are equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D eq(Matrix2D B, boolean target) { return eq(B, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are equal to other (1/0) and updates the target. */
    public Matrix2D eq(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] == B.data[c]) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }


    /** Tests if elements of caller are not equal to other (1/0) and returns a new matrix. */
    public Matrix2D ne(double v) { return lt(v, NEW); }

    /** Tests if elements of caller are not equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D ne(double v, boolean target) { return ne(v, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are not equal to other (1/0) and updates the target. */
    public Matrix2D ne(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] != v) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }

    /** Tests if elements of caller are not equal to other (1/0) and returns a new matrix. */
    public Matrix2D ne(Matrix2D B) { return lt(B, NEW); }

    /** Tests if elements of caller are not equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D ne(Matrix2D B, boolean target) { return ne(B, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are not equal to other (1/0) and updates the target. */
    public Matrix2D ne(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] != B.data[c]) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }


    /** Tests if elements of caller are greater than or equal to other (1/0) and returns a new matrix. */
    public Matrix2D ge(double v) { return lt(v, NEW); }

    /** Tests if elements of caller are greater than or equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D ge(double v, boolean target) { return ge(v, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are greater than or equal to other (1/0) and updates the target. */
    public Matrix2D ge(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] >= v) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }

    /** Tests if elements of caller are greater than or equal to other (1/0) and returns a new matrix. */
    public Matrix2D ge(Matrix2D B) { return lt(B, NEW); }

    /** Tests if elements of caller are greater than or equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D ge(Matrix2D B, boolean target) { return ge(B, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are greater than or equal to other (1/0) and updates the target. */
    public Matrix2D ge(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] >= B.data[c]) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }


    /** Tests if elements of caller are less than or equal to other (1/0) and returns a new matrix. */
    public Matrix2D le(double v) { return lt(v, NEW); }

    /** Tests if elements of caller are less than or equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D le(double v, boolean target) { return le(v, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are less than or equal to other (1/0) and updates the target. */
    public Matrix2D le(double v, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] >= v) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }

    /** Tests if elements of caller are less than or equal to other (1/0) and returns a new matrix. */
    public Matrix2D le(Matrix2D B) { return lt(B, NEW); }

    /** Tests if elements of caller are less than or equal to other (1/0) and returns a new matrix or updates in place. */
    public Matrix2D le(Matrix2D B, boolean target) { return le(B, get_target_by_flag(target, this.shape[0], this.shape[1])); }

    /** Tests if elements of caller are less than or equal to other (1/0) and updates the target. */
    public Matrix2D le(Matrix2D B, Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            if (this.data[c] >= B.data[c]) { target.data[c] = 1d; }
            else { target.data[c] = 0d; }
        }
        return target;
    }



    ///////////////////////////////////////////////////////////////////////////
    // ALGEBRA & OTHERS ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * Calculates the log of the cells, element-wise and returns a new Matrix2D
     * of the same shape
     *
     * @return natural log of the original Matrix2D
     */
    public Matrix2D log() { return log(NEW); }

    /**
     * Calculates the natural log of the cells, element-wise and returns either
     * a new Matrix2D or updates the caller.
     *
     * @param target output target (<code>Matrix2D.NEW</code> or <code>Matrix2D.INPLACE</code>)
     * @return natural log of the original Matrix2D
     */
    public Matrix2D log(boolean target) { return log(get_target_by_flag(target)); }

    /**
     * Calculates the natural log of the cells, element-wise and updates the
     * cells in the target.
     *
     * @param target output target
     * @return updates the target and returns it
     */
    public Matrix2D log(Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = Math.log(this.data[c]);
        }
        return target;
    }


    /** Analogous to <code>Matrix2D.log</code>, but with an exponential. */
    public Matrix2D exp(){ return exp(NEW); }
    /** Analogous to <code>Matrix2D.log</code>, but with an exponential. */
    public Matrix2D exp(boolean target){ return exp(get_target_by_flag(target)); }
    /** Analogous to <code>Matrix2D.log</code>, but with an exponential. */
    public Matrix2D exp(Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        for (int c = 0; c < this.size; c++){
            target.data[c] = Math.exp(this.data[c]);
        }
        return target;
    }
    


    /** Returns the arithmetic average of the values in the caller. */
    public double mean() {
        return this.sum()/(this.size);
    }


    /** Returns the root-mean-squared error between this and the other matrix. */
    public double rmse (Matrix2D B) {
        double error = 0;
        for (int c = 0; c < this.size; c++) {
            error += Math.pow(B.data[c] - this.data[c], 2);
        }
        error /= this.size;
        return Math.sqrt(error);
    }


    /** Calculates the Euclidean distance between this and the other matrix. */
    public double euclidean_distance(Matrix2D B) {
        double distance = 0d;
        for (int c = 0; c < this.size; c++) {
            distance += Math.pow(B.data[c] - this.data[c], 2);
        }
        return Math.sqrt(distance);
    }



    ///////////////////////////////////////////////////////////////////////////
    // LINEAR ALGEBRA /////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Performs a dot product (matrix multiplication) between two matrices and returns a new matrix. */
    public Matrix2D dot(Matrix2D B) { return dot(B, NEW); }

    /** Performs a dot product (matrix multiplication) between two matrices and returns a new matrix or updates in place. */
    public Matrix2D dot(Matrix2D B, boolean target) { return dot(B, get_target_by_flag(target, this.shape[0], B.shape[1])); }

    /** Performs a dot product (matrix multiplication) between two matrices and updates the target. */
    public Matrix2D dot(Matrix2D B, Matrix2D target){
        Matrix2D product;
        if (target == null){ product = new Matrix2D(this.shape[0], B.shape[1]); }
        else { product = target; }
        if (this.shape[1] != B.shape[0]){
            throw new IllegalArgumentException("Number of columns of this matrix must match rows of other.");
        }
        throw_shape_error(valid_shape(product.shape, this.shape[0], B.shape[1]));
        int i;
        int j;
        for (int c = 0; c < product.size; c++){
            i = product.flat2nested[c][0];
            j = product.flat2nested[c][1];
            product.data[c] = 0;
            for (int k = 0; k < B.shape[0]; k++){
                product.data[c] += this.get(i,k)*B.get(k,j);
            }
        }
        return product;
    }



    ///////////////////////////////////////////////////////////////////////////
    // TRANSFORMS /////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Tansposes the matrix and returns a new matrix. */
    public Matrix2D transpose(){ return transpose(get_target_by_flag(NEW, this.shape[1], this.shape[0])); }

    /** Puts the transpose of the caller into the tartet. */
    public Matrix2D transpose(Matrix2D target){
        Matrix2D out = get_target(target);
        throw_shape_error(valid_shape(out.shape[0], out.shape[1], this.shape[1], this.shape[0]));
        int i;
        int j;
        for (int c = 0; c < this.size; c++){
            i = this.flat2nested[c][0];
            j = this.flat2nested[c][1];
            out.set(j, i, this.data[c]);
        }
        return out;
    }


    /** Flattens the caller to a single column. */
    public Matrix2D flatten(){ return flatten(1); }

    /**
     * Flattens the caller along the given axis.
     *
     * @param axis 0: flattens to produce a single row, 1: flattens to produce a single column
     * @return new flat matrix
     */
    public Matrix2D flatten(int axis){
        if (axis == 0){
            return flatten(get_target_by_flag(NEW, 1, this.size));
        }
        else if (axis == 1){
            return flatten(get_target_by_flag(NEW, this.size, 1));
        }
        else {
            throw new IllegalArgumentException("Unrecognized axis.");
        }
    }


    /** Flattens the matrix and fills in the target. */
    public Matrix2D flatten(Matrix2D target){
        Matrix2D out = get_target(target);
        if (target.size != this.size){
            throw new IllegalArgumentException("Target is invalid shape.");
        }
        for (int c = 0; c < this.size; c++){
            out.data[c] = this.data[c];
        }
        return out;
    }


    /** Rotates the matrix by 180 (a vertical + horizontal flip) and returns a new matrix. */
    public Matrix2D rotate180() {return rotate180(new Matrix2D(this.shape[0], this.shape[1])); }

    /** Rotates the matrix by 180 (a vertical + horizontal flip) and updates the target. */
    public Matrix2D rotate180(Matrix2D target){
        throw_shape_error(valid_shape(target.shape, this.shape));
        int maxInd = this.size-1;
        for (int c = 0; c < this.size; c++){
            target.set_by_flat(maxInd-c, this.data[c]);
        }
        return target;
    }

    /** Gets a slice of the caller in the range of given rows and columns and returns a new matrix. */
    public Matrix2D slice(int rowStart, int colStart, int rowStop, int colStop){
        Matrix2D output = new Matrix2D(rowStop-rowStart, colStop-colStart);
        return this.slice(rowStart, colStart, rowStop, colStop, output);
    }

    /** Gets a slice of the caller in the range of given rows and columns. */
    public Matrix2D slice(int rowStart, int colStart, int rowStop, int colStop, Matrix2D target){
        int rows = rowStop - rowStart;
        int cols = colStop - colStart;
        throw_shape_error(valid_shape(target.shape, rows, cols));
        int r = 0;
        int c;
        for (int i = rowStart; i < rowStop; i++){
            c = 0;
            for (int j = colStart; j < colStop; j++){
                target.set(r, c, this.get(i, j));
                c += 1;
            }
            r += 1;
        }
        return target;
    }


    ///////////////////////////////////////////////////////////////////////////
    // ITERATORS //////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public Iterator<Double> iterator() { return new ValueIterator(this); }
    private class ValueIterator implements Iterator {
        private Matrix2D outter;
        private int i = 0;
        private int n;
        public ValueIterator(Matrix2D outter){ this.outter = outter; this.n = outter.size(); }
        public boolean hasNext(){
            return this.i != this.n;
    }
        public Double next() { return outter.get_by_flat(this.i++); }
    }


    public Iterator<Integer> iter_flat() { return new FlatIterator(this.size); }
    private class FlatIterator implements Iterator {
        private int i = 0;
        private int n;
        public FlatIterator(int size){ this.n = size; }
        public boolean hasNext(){
            return this.i != this.n;
        }
        public Integer next() { return this.i++; }
    }


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS, SETTERS, & OTHER //////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the index of the first occurrence of the value.
     * @param v
     * @return
     */
    public int[] index(double v){
        for (int c = 0; c < this.size; c++){
            if (this.data[c] == v){
                return new int[] {this.flat2nested[c][0], this.flat2nested[c][1]};
            }
        }
        throw new NoSuchElementException("Value is not in matrix.");
    }


    /**
     * Returns the first occurrence of the maximum value in the matrix.
     *
     * @return
     */
    public int[] argmax(){
        double max = Double.NEGATIVE_INFINITY;
        int flat = 0;
        for (int c = this.size-1; c > -1; c--){
            if (this.data[c] > max){
                max = this.data[c];
                flat = c;
            }
        }
        return this.flat2nested[flat];
    }


    /**
     * Returns the first occurrence of the minimum value in the matrix.
     *
     * @return
     */
    public int[] argmin(){
        double min = Double.POSITIVE_INFINITY;
        int flat = 0;
        for (int c = this.size-1; c > -1; c--){
            if (this.data[c] < min){
                min = this.data[c];
                flat = c;
            }
        }
        return this.flat2nested[flat];
    }


    /** Prints the full matrix to stdout. */
    public void print() { print(this.r(), this.c()); }

    /** Prints up to the given number of rows to stdout. */
    public void print(int maxRows) { print(maxRows, this.c()); }

    /** Prints up to the given number of rows and columns to stdout. */
    public void print(int maxRows, int maxCols){
        for (int i = 0; i < this.shape[0]; i++){
            if (i == maxRows) { break; }
            for (int j = 0; j < this.shape[1]; j++){
                if (j == maxCols) { break; }
                System.out.print(new DecimalFormat("#.#  ").format(this.get(i,j)));
            }
            System.out.println();
        }
    }


    /** Gets the shape of the matrix. */
    public int[] get_shape(){
        return this.shape;
    }

    /** Gets the number of rows in the matrix. */
    public int r(){
        return this.shape[0];
    }

    /** Gets the number of columns in the matrix. */
    public int c(){
        return this.shape[1];
    }

    /** Gets the number of cells in the matrix. */
    public int size() { return this.size; }

    /** Gets the value in the i,j-th cell of the matrix. */
    public double get(int i, int j){ return this.data[this.nested2flat[i][j]]; }

    /** Sets the value in the i,j-th cell of the matrix. */
    public void set(int i, int j, double v){ this.data[this.nested2flat[i][j]] = v; }

    /** Gets the object of the given attribute. */
    public Object get_attribute(String key) { return this.attributes.get(key); }

    /** Sets the object of the given attribute. */
    public void set_attribute(String key, Object value) { this.attributes.put(key, value); }

    /**
     * Gets the ith value in the matrix by the flat index.
     * <p>
     * The flat index is given by (i*this.c() + j).
     *
     * @param c flat index to return the value of
     * @returns the value at the flat index
     */
    public double get_by_flat(int c) { return this.data[c]; }

    /**
     * Sets the ith value in the matrix by the flat index.
     * @see Matrix2D <code>Matrix.get_by_flat</code>
     */
    public void set_by_flat(int c, double v) { this.data[c] = v; }


    /**
     * Converts the nested index to a flat index in this matrix (without any math).
     *
     * @param i row index
     * @param j column index
     * @return flat index
     */
    public int nested_to_flat(int i, int j) { return this.nested2flat[i][j]; }


    /**
     * Converts the flat index to the nested index in this matrix (without any math).
     *
     * @param c flat index
     * @return i,j indices of nested index
     */
    public int[] flat_to_nested(int c) { int[] o = new int[2]; flat_to_nested(c, o); return o; }


    /**
     * Converts the flat index to the nested index in this matrix (without any math).
     *
     * @param c flat index
     * @param target target array where results should be written
     */
    public void flat_to_nested(int c, int[] target){
        target[0] = this.flat2nested[c][0];
        target[1] = this.flat2nested[c][1];
    }


    /** Performs unit tests of Matrix2D. */
    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {

        // make two test matrices
        double[][] aData = {
                {1, 2, 3},
                {4, 5, 6}
        };
        double[][] bData = {
                {7, 8},
                {9, 10},
                {11, 12}
        };
        double[][] bvData = {
                {20},
                {30}
        };
        double [][] cvData = {
                {20, 30, 40}
        };
        double [][] sData = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        Matrix2D A = new Matrix2D(aData);
        Matrix2D B = new Matrix2D(bData);
        Matrix2D b = new Matrix2D(bvData);
        Matrix2D c = new Matrix2D(cvData);
        Matrix2D S = new Matrix2D(sData);

        // A*2
        System.out.println("First matrix multiplied by 2");
        Matrix2D C = A.multiply(2);
        System.out.println("...before");
        A.print();
        System.out.println("\n...after");
        C.print();

        // A + A
        System.out.println("\n\nFirst matrix added to self");
        System.out.println("...before");
        A.print();
        System.out.println("\n...after");
        A.add(A).print();

        // A / A
        System.out.println("\n\nFirst matrix divided by self");
        System.out.println("...before");
        A.print();
        System.out.println("\n...after");
        A.divide(A).print();

        // A.max(3)
        System.out.println("\n\nElement-wise max of first and 3");
        System.out.println("...before");
        A.print();
        System.out.println("\n...after");
        A.max(3).print();

        // A.min(3)
        System.out.println("\n\nElement-wise min of first and 3");
        System.out.println("...before");
        A.print();
        System.out.println("\n...after");
        A.min(3).print();

        // A.B
        System.out.println("\n\nDot product of A and B");
        Matrix2D D = A.dot(B);
        System.out.println("...matrix A");
        A.print();
        System.out.println("\n...matrix B");
        B.print();
        System.out.println("\n...dot product");
        D.print();

        // A.T
        System.out.println("\n\nTranspose of A");
        System.out.println("...before");
        A.print();
        Matrix2D AT = A.transpose();
        System.out.println("\n...after");
        AT.print();


        // A + b
        System.out.println("\n\nAdding to columns of A");
        System.out.println("...matrix A before");
        A.print();
        System.out.println("\n...vector b before");
        b.print();
        Matrix2D A2 = A.add_to_cols(b);
        System.out.println("\n...after");
        A2.print();


        // A + c
        System.out.println("\n\nAdding to rows of A");
        System.out.println("...matrix A before");
        A.print();
        System.out.println("\n...vector c before");
        c.print();
        Matrix2D A3 = A.add_to_rows(c);
        System.out.println("\n...after");
        A3.print();


        // log(A)
        System.out.println("\n\nlog(A)");
        System.out.println("...matrix A before");
        A.print();
        System.out.println("\n...after");
        A.log().print();


        // exp(A)
        System.out.println("\n\nexp(A)");
        System.out.println("...matrix A before");
        A.print();
        System.out.println("\n...after");
        A.exp().print();


        // repeat dot product with identity
        System.out.println("\n\nRepeat dot product between square matrix and identity to test passing in existing array as target.");
        System.out.println("...original matrix");
        S.print();
        Matrix2D S2 = S.copy();
        System.out.println("\n...identity matrix");
        Matrix2D I = S.identity();
        I.print();
        for (int i = 0; i < 10; i++){
            S = S.dot(I);
            S.dot(I, S2);
        }
        System.out.println("\n...original replaced by new matrix");
        S.print();
        System.out.println("\n...original overwriting existing matrix");
        S2.print();


        // rotate matrix 180 degrees
        System.out.println("\n\nRotating matrix by 180 degrees.");
        System.out.println("...original matrix");
        A.print();
        System.out.println("...rotated matrix");
        A.rotate180().print();

    }

}
