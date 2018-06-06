//
// $Id: NotifyingBackend.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.server.notify;

import edu.gemini.auxfile.api.AuxFile;
import edu.gemini.auxfile.api.AuxFileException;
import edu.gemini.auxfile.api.AuxFileListener;
import edu.gemini.auxfile.server.AuxFileChunk;
import edu.gemini.auxfile.server.AuxFileServer;
import edu.gemini.auxfile.server.AuxFileServerDecorator;
import edu.gemini.auxfile.server.file.FileManager;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.io.File;

/**
 *
 */
public final class NotifyingBackend extends AuxFileServerDecorator {
    private final List<AuxFileListener> _listeners = new ArrayList<AuxFileListener>();

    public NotifyingBackend(AuxFileServer decorated) {
        super(decorated);
    }

    public synchronized void addListener(AuxFileListener listener) {
        _listeners.add(listener);
    }

    public synchronized void removeListener(AuxFileListener listener) {
        _listeners.remove(listener);
    }

    private synchronized List<AuxFileListener> _copyListeners() {
        return new ArrayList<AuxFileListener>(_listeners);
    }

    @Override
    public Collection<AuxFile> list(SPProgramID progId, Collection<String> fileNames) throws AuxFileException {
        return super.list(progId, fileNames);
    }

    @Override
    public Collection<AuxFile> listAll(SPProgramID progId) throws AuxFileException {
        return super.listAll(progId);
    }

    @Override
    public boolean delete(SPProgramID progId, Collection<String> fileNames) throws AuxFileException {
        boolean res = super.delete(progId, fileNames);
        if (res) {
            List<AuxFileListener> listeners = _copyListeners();
            for (AuxFileListener listener : listeners) {
                listener.filesDeleted(progId, fileNames);
            }
        }
        return res;
    }

    @Override
    public boolean deleteAll(SPProgramID progId) throws AuxFileException {
        boolean res = super.deleteAll(progId);
        if (res) {
            List<AuxFileListener> listeners = _copyListeners();
            for (AuxFileListener listener : listeners) {
                listener.filesDeleted(progId, null);
            }
        }
        return res;
    }

    @Override
    public AuxFileChunk fetchChunk(SPProgramID progId, String fileName, int chunkNumber, int chunkSize, long timestamp) throws AuxFileException {
        final AuxFileChunk chunk;
        chunk = super.fetchChunk(progId, fileName, chunkNumber, chunkSize, timestamp);

        if ((chunk != null) && chunk.isLastChunk()) {
            File f = FileManager.instance().getProgramFile(progId, fileName);
            List<AuxFileListener> listeners = _copyListeners();
            for (AuxFileListener listener : listeners) {
                listener.fileFetched(progId, f);
            }
        }

        return chunk;
    }

    @Override
    public String storeChunk(SPProgramID progId, String fileName, AuxFileChunk chunk, String token) throws AuxFileException {
        final String res = super.storeChunk(progId, fileName, chunk, token);

        if (chunk.isLastChunk()) {
            File f = FileManager.instance().getProgramFile(progId, fileName);
            List<AuxFileListener> listeners = _copyListeners();
            for (AuxFileListener listener : listeners) {
                listener.fileStored(progId, f);
            }
        }

        return res;
    }

    private void notifyAll(SPProgramID progId, Collection<String> fileNames, BiConsumer<AuxFileListener, Collection<File>> notify) throws AuxFileException {
        final Collection<File> files =
          fileNames
             .stream()
             .map(n -> FileManager.instance().getProgramFile(progId, n))
             .collect(Collectors.toList());

        _copyListeners().stream().forEach(l -> notify.accept(l, files));
    }

    @Override
    public void setDescription(SPProgramID progId, Collection<String> fileNames, String newDescription) throws AuxFileException {
        super.setDescription(progId, fileNames, newDescription);
        notifyAll(progId, fileNames, (l, fs) -> l.descriptionUpdated(progId, newDescription, fs));
    }

    @Override
    public void setChecked(SPProgramID progId, Collection<String> fileNames, boolean newChecked) throws AuxFileException {
        super.setChecked(progId, fileNames, newChecked);
        notifyAll(progId, fileNames, (l, fs) -> l.checkedUpdated(progId, newChecked, fs));
    }

    @Override
    public void setLastEmailed(SPProgramID progId, Collection<String> fileNames, Option<Instant> newLastEmailed) throws AuxFileException {
        super.setLastEmailed(progId, fileNames, newLastEmailed);
        notifyAll(progId, fileNames, (l, fs) -> l.lastEmailedUpdated(progId, newLastEmailed, fs));
    }

}
