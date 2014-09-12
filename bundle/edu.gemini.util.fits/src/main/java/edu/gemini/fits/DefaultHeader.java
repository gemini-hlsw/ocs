//
// $Id: DefaultHeader.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.util.*;

/**
 * DefaultHeader is optimized for fast lookup at the expense of slower
 * modifications.  Adding a new header item to the end of the list is
 * reasonable, but inserting or removing elements in general is linear.
 * On the other hand, any operation that requires finding items (either by
 * key or index) should be done in constant time (technically, lookup by
 * keyword is linear in the number of items with the same keyword).
 */
public final class DefaultHeader extends AbstractList<HeaderItem> implements Header {
    private static final class IndexedHI implements HeaderItem {

        static IndexedHI create(int index, HeaderItem delegate) {
            if (delegate instanceof IndexedHI) {
                IndexedHI ihi = (IndexedHI) delegate;
                return new IndexedHI(index, ihi._delegate);
            }
            return new IndexedHI(index, delegate);
        }

        int index;
        HeaderItem _delegate;

        private IndexedHI(int index, HeaderItem delegate) {
            this.index = index;
            _delegate  = delegate;
        }

        public HeaderItem getDelegate() {
            return _delegate;
        }

        public String getKeyword() {
            return _delegate.getKeyword();
        }

        public boolean isStringValue() {
            return _delegate.isStringValue();
        }

        public String getValue() {
            return _delegate.getValue();
        }

        public int getIntValue() {
            return _delegate.getIntValue();
        }

        public double getDoubleValue() {
            return _delegate.getDoubleValue();
        }

        public boolean getBooleanValue() {
            return _delegate.getBooleanValue();
        }

        public String getComment() {
            return _delegate.getComment();
        }

        public boolean equals(Object o) {
            return _delegate.equals(o);
        }

        public int hashCode() {
            return _delegate.hashCode();
        }
    }

    private Map<String, List<IndexedHI>> _map;
    private List<IndexedHI> _delegate;
    private int _index;

    public DefaultHeader() {
        this(0); // the header is the primary
    }

    public DefaultHeader(int index) {
        this(index, 36); //the header is the primary, and it's one fits record the capacity
    }

    public DefaultHeader(Collection<? extends HeaderItem> c) {
        this(c, 0);
    }

    public DefaultHeader(int index, int capacity) {
        _delegate = new ArrayList<IndexedHI>(capacity);
        _map      = new HashMap<String, List<IndexedHI>>(capacity);
        _index = index;
    }

    public DefaultHeader(Collection<? extends HeaderItem> c, int index) {
        _delegate = new ArrayList<IndexedHI>(c.size());
        _map      = new HashMap<String, List<IndexedHI>>(c.size());
        _index    = index; //primary header

        int idx = 0;
        for (HeaderItem hi : c) {
            IndexedHI ihi = IndexedHI.create(idx++, hi);

            _delegate.add(ihi);

            List<IndexedHI> lst = _map.get(hi.getKeyword());
            if (lst == null) {
                lst = new ArrayList<IndexedHI>(1);
                _map.put(hi.getKeyword(), lst);
            } else {
                lst.add(ihi);
            }
        }
    }

    private void _addToMap(String key, IndexedHI newHi) {
        List<IndexedHI> lst = _map.get(key);
        if (lst == null) {
            lst = new ArrayList<IndexedHI>(1);
            _map.put(key, lst);
            lst.add(newHi);
        } else {
            int pos = 0;
            for (int i=lst.size()-1; i>=0; --i) {
                IndexedHI cur = lst.get(i);
                if (newHi.index > cur.index) {
                    pos = i+1;
                    break;
                }
            }
            lst.add(pos, newHi);
        }

    }

    private IndexedHI _findFirstInMap(HeaderItem headerItem) {
        List<IndexedHI> lst = _map.get(headerItem.getKeyword());
        if (lst == null) return null;
        //noinspection SuspiciousMethodCalls
        int lstIndex = lst.indexOf(headerItem);
        if (lstIndex == -1) return null;
        return lst.get(lstIndex);
    }

    private IndexedHI _findLastInMap(HeaderItem headerItem) {
        List<IndexedHI> lst = _map.get(headerItem.getKeyword());
        if (lst == null) return null;
        //noinspection SuspiciousMethodCalls
        int lstIndex = lst.lastIndexOf(headerItem);
        if (lstIndex == -1) return null;
        return lst.get(lstIndex);
    }

    private void _setInMap(String key, IndexedHI newHi) {
        List<IndexedHI> lst = _map.get(key);
        for (int i=0; i<lst.size(); ++i) {
            IndexedHI cur = lst.get(i);
            if (cur.index == newHi.index) {
                lst.set(i, newHi);
                break;
            }
        }
    }

    private void _removeFromMap(String key, int index) {
        List<IndexedHI> lst = _map.get(key);
        for (int i=0; i<lst.size(); ++i) {
            IndexedHI cur = lst.get(i);
            if (cur.index == index) {
                lst.remove(i);
                break;
            }
        }
    }

    public boolean add(HeaderItem headerItem) {
        IndexedHI ihi = IndexedHI.create(_delegate.size(), headerItem);
        _delegate.add(ihi);
        _addToMap(headerItem.getKeyword(), ihi);
        return true;
    }

    public void add(int index, HeaderItem headerItem) {
        IndexedHI ihi = IndexedHI.create(index, headerItem);
        _delegate.add(index, ihi);
        int sz = _delegate.size();
        for (int i=index+1; i<sz; ++i) {
            IndexedHI cur = _delegate.get(i);
            cur.index = i;
        }
        _addToMap(headerItem.getKeyword(), ihi);
    }

    public boolean addAll(Collection<? extends HeaderItem> c) {
        return addAll(_delegate.size(), c);
    }

    public boolean addAll(int index, Collection<? extends HeaderItem> c) {
        // Create a list holding wrapped HeaderItems with the correct indices.
        List<IndexedHI> tmp = new ArrayList<IndexedHI>();
        int i=index;
        for (HeaderItem headerItem : c) {
            IndexedHI ihi = IndexedHI.create(i++, headerItem);
            tmp.add(ihi);
        }

        // Update the remaining elements in the list to have the right
        // indices.
        int sz = c.size();
        for (i=index; i<_delegate.size(); ++i) {
            IndexedHI ihi = _delegate.get(i);
            ihi.index = i + sz;
        }

        // Insert the items into the delegate list.
        _delegate.addAll(index, tmp);

        // Add them to the map.
        for (i=0; i<tmp.size(); ++i) {
            IndexedHI ihi = tmp.get(i);
            _addToMap(ihi.getKeyword(), ihi);
        }
        return true;
    }

    public void clear() {
        _delegate.clear();
        _map.clear();
    }

    public boolean contains(Object o) {
        if (!(o instanceof HeaderItem)) return false;
        return _findFirstInMap((HeaderItem) o) != null;
    }

    public HeaderItem get(int i) {
        return _delegate.get(i).getDelegate();
    }

    public int indexOf(Object o) {
        if (!(o instanceof HeaderItem)) return -1;
        IndexedHI ihi = _findFirstInMap((HeaderItem) o);
        if (ihi == null) return -1;
        return ihi.index;
    }

    public boolean isEmpty() {
        return _delegate.isEmpty();
    }

    public int lastIndexOf(Object o) {
        if (!(o instanceof HeaderItem)) return -1;
        IndexedHI ihi = _findLastInMap((HeaderItem) o);
        if (ihi == null) return -1;
        return ihi.index;
    }

    public HeaderItem remove(int index) {
        IndexedHI ihi = _delegate.remove(index);
        _removeFromMap(ihi.getKeyword(), ihi.index);
        for (int i=index; i<_delegate.size(); ++i) {
            _delegate.get(i).index = i;
        }
        return ihi.getDelegate();
    }

    public boolean remove(Object o) {
        if (!(o instanceof HeaderItem)) return false;
        IndexedHI ihi = _findFirstInMap((HeaderItem) o);
        if (ihi == null) return false;
        remove(ihi.index);
        return true;
    }

    public HeaderItem set(int i, HeaderItem headerItem) {
        IndexedHI newHI = IndexedHI.create(i, headerItem);
        IndexedHI oldHI = _delegate.set(i, newHI);

        String oldKey = oldHI.getKeyword();
        String newKey = newHI.getKeyword();

        if (oldKey.equals(newKey)) {
            _setInMap(oldKey, newHI);
        } else {
            _removeFromMap(oldKey, oldHI.index);
            _addToMap(newKey, newHI);
        }

        return oldHI;
    }

    public int size() {
        return _delegate.size();
    }

    public HeaderItem get(String keyword) {
        List<IndexedHI> lst = _map.get(keyword);
        if ((lst == null) || (lst.size() == 0)) return null;
        return lst.get(0).getDelegate();
    }

    public List<HeaderItem> getAll(String keyword) {
        List<IndexedHI> lst = _map.get(keyword);
        if ((lst == null) || (lst.size() == 0)) return null;

        List<HeaderItem> res = new ArrayList<HeaderItem>(lst.size());
        for (IndexedHI ihi : lst) {
            res.add(ihi.getDelegate());
        }
        return res;
    }

    public Set<String> getKeywords() {
        return Collections.unmodifiableSet(_map.keySet());
    }

    public int getIndex() {
        return _index;
    }


}
