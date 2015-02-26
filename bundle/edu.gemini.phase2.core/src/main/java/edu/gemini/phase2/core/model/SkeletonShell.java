package edu.gemini.phase2.core.model;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.template.Phase1Folder;

import java.io.Serializable;

/**
 * case class Skeleton(id: SPProgramID, program: SPProgram, folder: TemplateFolder)
 */
public final class SkeletonShell implements Serializable {
    public final SPProgramID id;
    public final SPProgram program;
    public final Phase1Folder folder;

    public SkeletonShell(SPProgramID id, SPProgram program, Phase1Folder folder) {
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

        final SkeletonShell skeleton = (SkeletonShell) o;
        if (!folder.equals(skeleton.folder)) return false;
        if (!id.equals(skeleton.id)) return false;
        return program.equals(skeleton.program);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + program.hashCode();
        result = 31 * result + folder.hashCode();
        return result;
    }
}
