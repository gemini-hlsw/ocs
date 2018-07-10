package edu.gemini.qpt.shared.sp;

import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Group implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private List<Obs> obsList;
    private List<Note> noteList;
    private final String name;
    private final GroupType type;

    private final String id;
    
    
    public Group(String name, GroupType type, String id) {
        this.name = name;
        this.type = type;
        this.id = id;
    }

    public SPGroup.GroupType getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }

    public List<Obs> getObservations() {
        assert obsList != null;
        return obsList;
    }

    public List<Note> getNoteList() {
        assert noteList != null;
        return noteList;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public void setChildren(List<Obs> obsList, List<Note> noteList) {
        if (this.obsList != null) throw new IllegalStateException("setChildren has already been called.");
        this.obsList = Collections.unmodifiableList(new ArrayList<Obs>(obsList));
        this.noteList = Collections.unmodifiableList(new ArrayList<Note>(noteList));
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Group && ((Group) obj).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
}
