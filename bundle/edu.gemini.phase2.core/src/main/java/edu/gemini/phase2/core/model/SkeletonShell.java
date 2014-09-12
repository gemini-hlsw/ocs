package edu.gemini.phase2.core.model;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.template.TemplateFolder;

import java.io.Serializable;

/**
 * case class Skeleton(id: SPProgramID, program: SPProgram, folder: TemplateFolder)
 */
public final class SkeletonShell implements Serializable {
    public final SPProgramID id;
    public final SPProgram program;
    public final TemplateFolder folder;

    public SkeletonShell(SPProgramID id, SPProgram program, TemplateFolder folder) {
        if (id == null) throw new IllegalArgumentException("id is null");
        if (program == null) throw new IllegalArgumentException("program is null");
        if (folder == null) throw new IllegalArgumentException("folder is null");

        this.id      = id;
        this.program = program;
        this.folder  = folder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkeletonShell skeleton = (SkeletonShell) o;

        if (!folder.equals(skeleton.folder)) return false;
        if (!id.equals(skeleton.id)) return false;
        if (!program.equals(skeleton.program)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + program.hashCode();
        result = 31 * result + folder.hashCode();
        return result;
    }
}
