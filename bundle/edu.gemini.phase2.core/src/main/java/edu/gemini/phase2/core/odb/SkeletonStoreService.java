package edu.gemini.phase2.core.odb;

import edu.gemini.phase2.core.model.SkeletonShell;
import edu.gemini.phase2.core.model.TemplateFolderExpansion;
import static edu.gemini.phase2.core.odb.SkeletonStoreResult.*;
import static edu.gemini.phase2.core.odb.TemplateFolderService.BaselineOption;
import static edu.gemini.phase2.core.odb.TemplateFolderService.TemplateOption;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBIDClashException;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.too.Too;

import java.io.Serializable;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SkeletonStoreService {
    private static final Logger LOG = Logger.getLogger(SkeletonStoreService.class.getName());

    /**
     * (SkeletonStoreResult, ISPProgram)
     */
    public static class ResultAndProgram implements Serializable {
        public final SkeletonStoreResult result;
        public final ISPProgram program;

        public ResultAndProgram(SkeletonStoreResult result, ISPProgram program) {
            if (result == null) throw new IllegalArgumentException("result is null");
            if (program == null) throw new IllegalArgumentException("program is null");

            this.result  = result;
            this.program = program;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResultAndProgram that = (ResultAndProgram) o;

            if (!program.equals(that.program)) return false;
            if (result != that.result) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result1 = result.hashCode();
            result1 = 31 * result1 + program.hashCode();
            return result1;
        }
    }

    private SkeletonStoreService() {}

    public static ResultAndProgram store(SkeletonShell shell, TemplateFolderExpansion tfe, IDBDatabaseService odb)  {
        ISPProgram cur = odb.lookupProgramByID(shell.id);
        if ((cur != null) && !SkeletonStatusService.getStatus(cur).updatable) {
            return new ResultAndProgram(REJECTED, cur);
        } else if (cur != null) {
            update(shell, tfe, odb, cur);
            return new ResultAndProgram(UPDATED, cur);
        } else {
            try {
                return new ResultAndProgram(CREATED, create(shell, tfe, odb));
            } catch (DBIDClashException e) {
                LOG.log(Level.WARNING, "Concurrent creation of program '%d'");
                return store(shell, tfe, odb);
            }
        }
    }

    private static void update(SkeletonShell shell, TemplateFolderExpansion tfe, IDBDatabaseService odb, ISPProgram p)  {
        try {
            p.setDataObject(shell.program);

            ISPTemplateFolder tf = p.getTemplateFolder();
            if (tf == null) {
                tf = odb.getFactory().createTemplateFolder(p, null);
                p.setTemplateFolder(tf);
            }
            tf.setDataObject(shell.folder);
            tf.setTemplateGroups(Collections.<ISPTemplateGroup>emptyList());

            if (tfe != null) {
                // Remove existing children
                p.setObsComponents(Collections.<ISPObsComponent>emptyList());
                p.setGroups(Collections.<ISPGroup>emptyList());
                p.setObservations(Collections.<ISPObservation>emptyList());

                // Expand the template folder into new children.
                TemplateFolderService.store(tfe, TemplateOption.REPLACE, BaselineOption.ADD, p, odb.getFactory());
            }

            Too.set(p, shell.program.getTooType());

        } catch (SPException ex) {
            // Logically this should not be possible here.
            LOG.log(Level.SEVERE, "SPException for " + shell.id, ex);
            throw new RuntimeException(ex);
        }
    }

    private static ISPProgram create(SkeletonShell shell, TemplateFolderExpansion tfe, IDBDatabaseService odb) throws DBIDClashException {
        ISPProgram p = odb.getFactory().createProgram(new SPNodeKey(), shell.id);
        odb.put(p);
        update(shell, tfe, odb, p);
        return p;
    }
}
