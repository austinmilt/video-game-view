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


package video;

/**
 * Java equivalent of the Interval class in intervals.py
 */
public class Interval {

    private Object data;
    private double start;
    private double end;
    private Interval next;


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an empty interval with default start and end points.
     */
    public Interval() { this(null); }


    /**
     * Constructs an interval with the given data with default start and end points.
     *
     * @param data data to associate with this interval
     */
    public Interval(Object data) { this(data, Double.NEGATIVE_INFINITY); }


    /**
     * Constructs an interval with the given data and starting point and default end point.
     *
     * @param data data to associate with this interval
     * @param start starting point (inclusive) in time of the interval
     */
    public Interval(Object data, double start) { this(data, start, Double.POSITIVE_INFINITY); }


    /**
     * Constructs an interval with the given data, starting, and ending point and no next interval.
     *
     * @param data data to associate with this interval
     * @param start starting point (inclusive) in time of the interval
     * @param end ending point (exclusive) in time of the interval
     */
    public Interval(Object data, double start, double end) { this(data, start, end, null); }


    /**
     * Constructs an interval with the given data, starting and ending point and given next interval.
     *
     * @param data data to associate with this interval
     * @param start starting point (inclusive) in time of the interval
     * @param end ending point (exclusive) in time of the interval
     * @param next next interval in a linked list after this one (if any)
     */
    public Interval(Object data, double start, double end, Interval next) {
        if (start > end) { throw new IllegalArgumentException("Intervals must end after they start (end >= start)."); }
        this.data = data;
        this.start = start;
        this.end = end;
        this.next = next;
    }



    ///////////////////////////////////////////////////////////////////////////
    // COMPARATORS ////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Tests whether the provided time is inside this interval. */
    public boolean contains_time(double time) {
        boolean result = false;
        if ((this.start <= time) && (this.end > time)) { result = true; }
        return result;
    }


    /** Tests whether this interval overlaps the other. */
    public boolean overlaps(Interval other) {
        boolean result = true;
        if (this.end <= other.end) { result = false; }
        else if (this.start >= other.end) { result = false; }
        return result;
    }


    /** Tests whether this interval comes strictly after the other. */
    public boolean is_after(Interval other) {
        return (this.start >= other.end);
    }


    /** Tests whether this interval comes strictly before the other. */
    public boolean is_before(Interval other) {
        return (this.end <= other.end);
    }



    ///////////////////////////////////////////////////////////////////////////
    // LOOKUPS ////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    public String toString() {
        return String.format("Interval [%.2f %.2f)", this.start, this.end);
    }

    /** Returns the duration of this interval (end - start). */
    public double get_duration() {
        return this.end - this.start;
    }

    /** Returns the data object associated with this interval. */
    public Object get_data() { return this.data; }

    /** Returns the starting point of this interval. */
    public double get_start() { return this.start; }

    /** Returns the ending point of this interval. */
    public double get_end() { return this.end; }

    /** Returns the next interval after this one. */
    public Interval get_next() { return this.next; }

    /** Sets the data object associated with this interval. */
    public void set_data(Object data) { this.data = data; }

    /** Sets the next interval after this one. */
    public void set_next(Interval next) { this.next = next; }



}
