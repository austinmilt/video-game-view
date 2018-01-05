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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/** Java equivalent of LinkedIntervalSet in intervals.py */
public class OrderedIntervals {

    private Interval root = null;
    private Interval tip = null;
    private int count = 0;


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////


    /** Constructs an empty OrderedIntervals. */
    public OrderedIntervals(){}


    /**
     * Constructs an OrderedIntervals from the given intervals.
     *
     * @param intervals
     */
    public OrderedIntervals(Interval[] intervals) {
        for (Interval interval : intervals) { this.add(interval); }
    }


    /**
     * Constructs a new OrderedIntervals from the starting times of the given
     * intervals.
     * <p>
     * Assumes that the end time of each interval is the start time of the next
     * later interval. Creates a new interval for each existing one.
     * <p>
     * The last interval will use its own end.
     * <p>
     * This will not work if multiple intervals have the same start.
     *
     * @param intervals array of intervals to use for construction
     * @return constructed OrderedIntervals as described above
     */
    public static OrderedIntervals from_starts(Interval[] intervals) {
        int n = intervals.length;
        HashMap<Double, Interval> startMap = new HashMap<>(n);
        for (int i = 0; i < n; i++) { startMap.put(intervals[i].get_start(), intervals[i]); }
        Interval[] newIntervals = new Interval[n];
        ArrayList<Double> starts = new ArrayList<>(startMap.keySet());
        Collections.sort(starts);
        double start;
        double nextStart;
        Interval interval;
        for (int i = 0; i < (n-1); i++) {
            start = starts.get(i);
            nextStart = starts.get(i+1);
            interval = startMap.get(start);
            newIntervals[i] = new Interval(interval.get_data(), start, nextStart);
        }
        interval = startMap.get(starts.get(n-1));
        newIntervals[n-1] = new Interval(interval.get_data(), interval.get_start(), interval.get_end());
        return new OrderedIntervals(newIntervals);
    }


    ///////////////////////////////////////////////////////////////////////////
    // MANIPULATORS ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Adds the interval in the correct position to the ordered list. */
    public void add(Interval interval) {

        // add the first interval
        if (this.root == null) {
            this.root = interval;
            this.tip = interval;
            this.root.set_next(null);
        }

        // add to the beginning
        else if (interval.is_before(this.root)) {
            interval.set_next(this.root);
            this.root = interval;
        }

        // add to the end
        else if (interval.is_after(this.tip)) {
            this.tip.set_next(interval);
            this.tip = interval;
            this.tip.set_next(null);
        }

        // add somewhere in the middle
        else {
            Interval testNode = this.root;
            while (testNode != null) {
                if (interval.is_after(testNode)) {
                    if (interval.overlaps(testNode.get_next())) {
                        throw new RuntimeException("Ordered intervals may not overlap in time.");
                    }
                    interval.set_next(testNode.get_next());
                    testNode.set_next(interval);
                    break;
                }
                testNode = testNode.get_next();
            }
        }

        this.count += 1;
    }



    ///////////////////////////////////////////////////////////////////////////
    // LOOKUPS ////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /** Gets the interval containing the requested time. Returns null if no match.*/
    public Interval query_time(double time) {
        Interval result = null;
        Interval cur = this.root;
        while (cur != null) {
            if (cur.contains_time(time)) {
                result = cur;
                break;
            }
            else if (time < cur.get_start()) { break; }
        }
        return result;
    }

    /** Returns the number of ordered intervals. */
    public int get_count() { return this.count; }


    /** Returns the range of time between the first and last interval. */
    public double get_range() { return this.tip.get_end() - this.root.get_start(); }


    /** Returns the total amount of time covered by all intervals. */
    public double get_duration() {
        double total = 0d;
        Interval cur = this.root;
        while (cur != null) {
            total += cur.get_duration();
            cur = cur.get_next();
        }
        return total;
    }

}
