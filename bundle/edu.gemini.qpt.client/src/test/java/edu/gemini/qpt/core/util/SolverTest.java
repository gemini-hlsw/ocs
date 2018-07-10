package edu.gemini.qpt.core.util;

import org.junit.Test;

import java.util.Iterator;
import java.util.SortedSet;

import static java.lang.Math.PI;
import static java.lang.Math.sin;
import static junit.framework.Assert.assertEquals;

public class SolverTest {

    @Test public void testStepSize() {

        // Should solve correctly for all stepsize < period.
        for (int period = 2; period < 1024; period *= 2) {
            for (int tolerance = 1; tolerance < period; tolerance++) {
                for (int cycles = 1; cycles < 5; cycles++) {
                    Solver s = new TestSolver(tolerance, 1, period);
                    SortedSet<Interval> solutions = s.solve(0, period * cycles * 2).getIntervals();
                    assertEquals(cycles, solutions.size());
                    int cycle = 0;
                    for (Interval solution : solutions) {
                        assertEquals(cycle++ * period * 2 + 1, solution.getStart());
                        assertEquals(solution.getStart() + period - 1, solution.getEnd());
                    }
                }
            }
        }
        
    }

    @Test public void testBoundedSearch() {
        
        // On a closed interval should be able to solve correctly
        // for all 8 cases [start middle end]

        // True from (1..100] and (201..300]
        Solver s = new TestSolver(99, 1, 100);
        
        // [true true true] on (40, 60]
        {
            SortedSet<Interval> solutions = s.solve(40, 60).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(40, 60), solutions.first());
        }
    
        // [true true false] on (40, 160]
        {
            SortedSet<Interval> solutions = s.solve(40, 160).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(40, 100), solutions.first());
        }
        
        // [true false true] on (40, 240]
        {
            SortedSet<Interval> solutions = s.solve(40, 240).getIntervals();
            assertEquals(2, solutions.size());
            Iterator<Interval> it = solutions.iterator();
            assertEquals(new Interval(40, 100), it.next());
            assertEquals(new Interval(201, 240), it.next());
        }
        
        // [true false false] on (70, 160]
        {
            SortedSet<Interval> solutions = s.solve(70, 160).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(70, 100), solutions.first());
        }
        
        // [false true true] on (-40, 60]
        {
            SortedSet<Interval> solutions = s.solve(-40, 60).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(1, 60), solutions.first());
        }
        
        // [false true false] on (-40, 160]
        {
            SortedSet<Interval> solutions = s.solve(-40, 160).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(1, 100), solutions.first());
        }
        
        // [false false true] on (99, 160]
        {
            SortedSet<Interval> solutions = s.solve(-100, 2).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(1, 2), solutions.first());
        }
        
        // [false false false] on (140, 160]
        {
            SortedSet<Interval> solutions = s.solve(140, 160).getIntervals();
            assertEquals(0, solutions.size());
        }
        
    }
    
    @Test public void testOffByOne() {
        
        // True from (1..100] and (201..300]
        Solver s = new TestSolver(99, 1, 100);
        
        // [before after]
        
        // [true true] around (40, 41]
        {
            SortedSet<Interval> solutions = s.solve(40, 41).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(40, 41), solutions.first());
        }
    
        // [true false] around (99, 100]
        {
            SortedSet<Interval> solutions = s.solve(99, 100).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(99, 100), solutions.first());
        }
    
        // [false true] around (-100, 2]
        {
            SortedSet<Interval> solutions = s.solve(-100, 2).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(1, 2), solutions.first());
        }
        
        // [false false] needs a new functor
        {
            Solver s2 = new Solver(1, 1) {
                @Override
                protected boolean f(long t) {
                    return t == 5;
                }
            };
            
            SortedSet<Interval> solutions = s2.solve(0, 10).getIntervals();
            assertEquals(1, solutions.size());
            assertEquals(new Interval(5, 6), solutions.first());
    
        }


    }
    
}

/**
 * Solver for (sine wave with period p) > 0
 */
class TestSolver extends Solver {

    private final double period;

    public TestSolver(long stepSize, long tolerance, double period) {
        super(stepSize, tolerance);
        this.period = period;
    }

    @Override
    protected boolean f(long x) {
        long y = Math.round(period * sin(x * PI / period));
        return y > 0;
    }

}
