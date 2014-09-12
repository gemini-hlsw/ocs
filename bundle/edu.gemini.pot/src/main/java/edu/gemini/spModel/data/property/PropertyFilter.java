//
// $Id: PropertyFilter.java 7030 2006-05-11 17:55:34Z shane $
//

package edu.gemini.spModel.data.property;

import java.beans.PropertyDescriptor;

/**
 *
 */
public interface PropertyFilter {
    class Not implements PropertyFilter {
        private PropertyFilter _base;

        public Not(PropertyFilter base) {
            _base = base;
        }

        public boolean accept(PropertyDescriptor pd) {
            return !_base.accept(pd);
        }
    }

    class And implements PropertyFilter {
        private PropertyFilter _f1;
        private PropertyFilter _f2;

        public And(PropertyFilter f1, PropertyFilter f2) {
            _f1 = f1;
            _f2 = f2;
        }

        public boolean accept(PropertyDescriptor pd) {
            return _f1.accept(pd) && _f2.accept(pd);
        }
    }

    class Or implements PropertyFilter {
        private PropertyFilter _f1;
        private PropertyFilter _f2;

        public Or(PropertyFilter f1, PropertyFilter f2) {
            _f1 = f1;
            _f2 = f2;
        }

        public boolean accept(PropertyDescriptor pd) {
            return _f1.accept(pd) || _f2.accept(pd);
        }
    }

    PropertyFilter TRUE = new PropertyFilter() {
        public boolean accept(PropertyDescriptor pd) {
            return true;
        }
    };

    PropertyFilter ENGINEERING_FILTER = new PropertyFilter() {
        public boolean accept(PropertyDescriptor pd) {
            return PropertySupport.isEngineering(pd);
        }
    };

    PropertyFilter ITERABLE_FILTER = new PropertyFilter() {
        public boolean accept(PropertyDescriptor pd) {
            return PropertySupport.isIterable(pd);
        }
    };

    PropertyFilter EXPERT_FILTER = new PropertyFilter() {
        public boolean accept(PropertyDescriptor pd) {
            return pd.isExpert();
        }
    };

    PropertyFilter QUERYABLE_FILTER = new PropertyFilter() {
        public boolean accept(PropertyDescriptor pd) {
            return PropertySupport.isQueryable(pd);
        }
    };

    boolean accept(PropertyDescriptor pd);
}
