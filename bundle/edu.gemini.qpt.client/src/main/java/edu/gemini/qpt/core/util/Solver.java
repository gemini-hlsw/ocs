package edu.gemini.qpt.core.util;



public abstract class Solver {

    private final long stepSize, tolerance;

    public Solver(long stepSize, long tolerance) {
        this.stepSize = stepSize;
        this.tolerance = tolerance;
    }

    /** 
     * Find the interval (a..b] where f(x) is true for a < t < b if f(t),
     * otherwise t < a < b. That is, find the truth domain containing t, 
     * or the next one if f(t) is false.
     */
    public Interval solve(Interval bounds, long t) {
        try {
            long[] ret = new long[2];
            if (f(t)) {
                ret[0] = solve(bounds, t, -stepSize, true);
                ret[1] = solve(bounds, t, stepSize, true);
            } else {
                ret[0] = solve(bounds, t, stepSize, false);
                ret[1] = solve(bounds, ret[0], stepSize, true);
            }
            if (ret[0] >= ret[1] + 1) return null; // empty interval
            return new Interval(ret[0], ret[1] + 1);
        } catch (NoSolutionException nse) {
            return null;
        }
    }

    
    /**
     * Find all domains on the specified interval.
     */
    public Union<Interval> solve(Interval interval) {
        Union<Interval> ret = new Union<Interval>();
        long t = interval.getStart();
        while (interval.contains(t)) {
            Interval i = solve(interval, t);
            if (i == null) break;
//            System.out.println("There is a solution at " + i);
            ret.add(i);
            t = i.getEnd() + tolerance;
        }

        ret.intersect(new Union<Interval>(interval));
        return ret;
    }
    
    public Union<Interval> solve(long start, long end) {
        return solve(new Interval(start, end));
    }
    
    
    private long solve(Interval bounds, long t, long stepSize, boolean findFalse) throws NoSolutionException {
//        System.out.print("Starting at " + t + " and counting by " + stepSize + " until condition is " + !findFalse);
        while (findFalse ? f(t) : !f(t)) {
            if (!bounds.contains(t)) {
                
                
                if (findFalse) {
                    
//                     If we're looking for a false condition when we ran off
                    // the end, just clip it at t.
                    
                    Long ret;
                    if (t < bounds.getStart() && stepSize < 0) {
                        ret = bounds.getStart();
                    } else if (t > bounds.getEnd() && stepSize > 0) {
                        ret = bounds.getEnd() - 1;
                    } else {
                        ret = null;
                    }
                    if (ret != null) {
//                        System.out.println(" ... hit a boundary ... returning " + ret);
                        return ret;
                    }
                    
                } else {
                    
                    if ((t < bounds.getStart() && stepSize < 0) || 
                         (t > bounds.getEnd() && stepSize > 0)) {

                        // Otherwise we're looking for a true condition that may
                        // never appear.
//                        System.out.println(" ... hit a boundary ... no solution.");
                        throw new NoSolutionException();
                    }

                    

                }
                
            }
            t += stepSize;;
        }
//        System.out.println(" ... condition is " + !findFalse + " at "  + t);
        long ret = (Math.abs(stepSize) <= tolerance) ? (findFalse ? t - stepSize : t) : solve(bounds, t, -stepSize / 2, !findFalse);
//        System.out.println("... returning " + ret);
        return ret;
    }
    
    protected abstract boolean f(long t);
    

    
    
//    public static void main(String[] args) {
//        
//        // Solver for t^2 < 50
//        Solver s = new Solver(50, 1) {
//            @Override
//            protected boolean f(long t) {
//                return t > 200;
//            }
//        };
//        
//        
//        System.out.println(s.solve(100, 200));        
//        
//    }
    
    @SuppressWarnings("serial")
    class NoSolutionException extends Exception {
        
    }
    
}
