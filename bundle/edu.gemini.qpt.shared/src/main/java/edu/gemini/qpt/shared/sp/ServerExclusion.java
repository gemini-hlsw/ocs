package edu.gemini.qpt.shared.sp;

public class ServerExclusion {

    public enum ProgramExclusion {
        
        MARKED_COMPLETE("Program is marked as complete."), 
        INVALID_SEMESTER_OR_TYPE("Invalid semester or program type."),
        NON_NUMERIC_BAND("Non-numeric queue band."), 
        
        ;
        
        private final String string;
        
        private ProgramExclusion(final String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }

    }
    
    public enum ObsExclusion {
        
        EXCLUDED_CLASS("Excluded obs class."), 
        EXCLUDED_STATUS("Not ready/ongoing."), 
        NO_REMAINING_STEPS("No remaining unexecuted steps."),
        
        ;
        
        private final String string;
        
        private ObsExclusion(final String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
    
}
