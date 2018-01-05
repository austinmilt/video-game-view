"""
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
"""

# contains classes with methods for handling data that are ordered by time

class Interval:
    
    def __init__(self, data, start=float('-inf'), end=float('inf'), next=None):
        """
        Intervals are objects that store data for one interval in time.
        
        Args:
            data (object): data to store with this interval
            
            start (float): (optional) start time (inclusive) of interval. 
                Default is -inf
            
            end (float): (optional) end time (exclusive) of interval. Default 
                is +inf
            
            next (Interval): (optional) next Interval for Intervals in a
                LinkedIntervalSet
            
        Returns:
            Interval: instantiated Interval object
        """
        self.data = data
        self.start = start
        self.end = end
        self.next = next
        
        # intervals must be increasing:
        if self.start > self.end:
            raise ValueError('Intervals must end after they start (i.e. end >= start).')
            
            
    def __repr__(self):
        return '<Interval [%0.2f, %0.2f)>' % (self.start, self.end)
            
    
    def get_duration(self):
        """Gets the duration of time covered by this Interval."""
        return self.end - self.start
        
        
    def contains_time(self, time):
        """
        Tests whether the provided time is inside this interval.
        
        Args:
            time (float): time to test for inclusion in this Interval.
            
        Returns:
            bool: True if the time is in this interval, False otherwise
        """
        result = False
        if (self.start <= time) and (self.end > time): result = True
        return result
        
        
    def overlaps(self, other):
        """
        Tests whether the two Invervals overlap in time.
        
        Args:
            other (Interval): other interval to test against this one
            
        Returns:
            bool: True if the two interval overlap, False otherwise.
        """
        result = True
        if (self.end <= other.start): result = False
        elif (self.start >= other.end): result = False
        return result
        
    
    def is_after(self, other):
        """
        Test whether the current Interval comes strictly after the other 
            Interval.
        
        Args:
            other (Interval): Interval to test against this Interval.
            
        Returns:
            bool: True if this Interval comes later than the other, False
                otherwise.
        """
        return self.start >= other.end
        
        
    def is_before(self, other):
        """
        Test whether the current Interval comes strictly before the other 
            Interval.
        
        Args:
            other (Interval): Interval to test against this Interval.
            
        Returns:
            bool: True if this Interval comes earlier than the other, False
                otherwise.
        """
        return self.end <= other.start
        
        
        
class LinkedIntervalSet:
    
    def __init__(self, intervals=None):
        """
        LinkedIntervalSets link together Intervals in order to manage and query
        data changes over time.
        
        Args:
            intervals (list): (optional) list of Intervals to use to create an
                list of linked Intervals. Default is empty.
                
        Returns:
            LinkedIntervalSet: instantiated LinkedIntervalSet object.
        """
        if intervals is None: intervals = []
        if len(intervals) == 0:
            self.earliest = None
            self.latest = None
            
        else:
            self.earliest = intervals[0]
            self.latest = self.earliest
            
        for i in intervals[1:]: self.add(i)
        
        
    def __repr__(self):
        return '<LinkedIntervalSet with %i intervals>' % self.get_count()
        
        
    def __iter__(self):
        self._cur = self.earliest
        return self
    
    
    def next(self):
        if self._cur is None: raise StopIteration
        else:
            toReturn = self._cur
            self._cur = self._cur.next
            return toReturn
        
        
    def get_count(self):
        """Returns a count of the Intervals in this LinkedIntervalSet."""
        c = 0
        cur = self.earliest
        while cur is not None:
            c += 1
            cur = cur.next
        
        return c
            
            
    def get_range(self):
        """Returns the range of time betwen the first and last Interval."""
        return self.latest.end - self.earliest.start
        
        
    def get_duration(self):
        """Returns the total amount of time covered by all Intervals."""
        total = 0.
        cur = self.earliest
        while cur is not None:
            total += cur.get_duration()
            cur = cur.next
            
        return total
        
        
    def add(self, interval):
        """
        Adds the interval in the correct order to this LinkedIntervalSet.
        
        Args:
            interval: Interval to add.
            
        Returns:
            None: Updates this LinkedIntervalSet in place
        """
        # add the first interval
        if self.earliest is None:
            self.earliest = interval
            self.latest = interval
            self.earliest.next = None
            
        # add to the beginning of the list
        elif interval.is_before(self.earliest):
            interval.next = self.earliest
            self.earliest = interval
            
        # add to the end of the list
        elif interval.is_after(self.latest):
            self.latest.next = interval
            self.latest = interval
            self.latest.next = None
            
        # add somewhere in the middle
        else:
            testNode = self.earliest
            while testNode is not None:
                if interval.is_after(testNode):
                    if interval.overlaps(testNode.next):
                        raise ValueError('Intervals in a LinkedIntervalSet may not overlap.')
                        
                    interval.next = testNode.next
                    testNode.next = interval
                    break
                    
                testNode = testNode.next   
                    
                    
    @staticmethod
    def from_starts(intervals=[]):
        """
        Creates a new LinkedIntervalSet from the starting times of the given
        intervals.
        
        Assumes that the end time of each Interval is the start time of the
        next later Interval. Creates a new Interval for each existing one.
        
        Args:
            intervals (iterable): (optional) list of Intervals to use for the
                new LinkedIntervalSet
                
        Returns:
            LinkedIntervalSet: a new LinkedIntervalSet based on the start
                times of the given Intervals. Note the last interval will use
                its own end. Also note this wont work if multiple intervals 
                have the same start.
        """
        startDict = dict((i.start, i) for i in intervals)
        newIntervals = []
        starts = sorted(startDict.keys())
        for i in xrange(len(starts)-1):
            start = starts[i]
            nextStart = starts[i+1]
            interval = startDict[start]
            nextInterval = startDict[nextStart]
            newIntervals.append(Interval(interval.data, interval.start, nextInterval.start))
        
        newIntervals.append(Interval(nextInterval.data, nextInterval.start, nextInterval.end))
        return LinkedIntervalSet(newIntervals)
        
        
    @staticmethod
    def from_starts_list(data=[]):
        """
        Creates a new LinkedIntervalSet from the starting times of the given
        data.
        
        Assumes that the end time of each datum is the start time of the
        next later datum. Creates a new Interval for each datum.
        
        Args:
            data (iterable): (optional) nested list of data to build from,
                of the form
                    [[start_1, object_2], [start_2, object_2], ...]
                
        Returns:
            LinkedIntervalSet: a new LinkedIntervalSet based on the start
                times of the given data. Note the last interval will get
                and end of infinity. Also note this wont work if multiple data 
                have the same start.
        """
        sortedData = sorted(data, key=lambda x: x[0])
        return LinkedIntervalSet.from_starts([Interval(d[1], d[0]) for d in sortedData])
        
                    
    def query_time(self, time):
        """
        Gets the Interval containing the requested time.
        
        Args:
            time (float): time to query
            
        Returns:
            Interval: Interval at the requested time. If no Interval contains
                that time, returns None
        """
        result = None
        cur = self.earliest
        while cur is not None:
            if cur.contains_time(time):
                result = cur
                break
            
            cur = cur.next
                
        return result
        
        
        
def test():
    
    # make some random intervals
    from random import random
    starts = [random() for i in xrange(20)]
    data = range(20)
    intervals = [Interval(data[i], starts[i]) for i in xrange(20)]
    I = LinkedIntervalSet.from_starts(intervals)
    
    # query some random times
    for i in xrange(10):
        v = random()
        interval = I.query_time(v)
        print '%0.2f'%v, interval, interval.data
        
        
if __name__ == '__main__':
    test()