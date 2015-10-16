package edu.gemini.auxfile.client;

import edu.gemini.auxfile.api.*;
import edu.gemini.auxfile.server.AuxFileChunk;
import edu.gemini.auxfile.server.AuxFileServer;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.util.security.auth.keychain.KeyChain;
import edu.gemini.util.trpc.client.TrpcClient$;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;

/**
 * An {@link AuxFileSystem} implementation that may be used by a client
 * application to communicate with a server.
 */
public final class AuxFileClient implements AuxFileSystem {

    private static final int FileChunkSize = 32 * 1024;

    private final AuxFileServer server;

    public AuxFileClient(String host, int port) {
        this.server    = TrpcClient$.MODULE$.apply(host, port).withoutKeys().proxy(AuxFileServer.class);
    }

    public AuxFileClient(KeyChain kc, String host, int port) {
        this.server    = TrpcClient$.MODULE$.apply(host, port).withKeyChain(kc).proxy(AuxFileServer.class);
    }

    @Override public Collection<AuxFile> list(SPProgramID programId, Collection<String> fileNames) throws AuxFileException {
        return server.list(programId, fileNames);
    }

    @Override public Collection<AuxFile> listAll(SPProgramID programId) throws AuxFileException {
        return server.listAll(programId);
    }

    @Override public boolean delete(SPProgramID programId, Collection<String> fileNames) throws AuxFileException {
        return server.delete(programId, fileNames);
    }

    @Override public boolean deleteAll(SPProgramID programId) throws AuxFileException {
        return server.deleteAll(programId);
    }

    private static boolean isWritable(File f) {
        if (f.exists()) {
            return f.canWrite();
        } else {
            final File dir = f.getParentFile();
            return dir.exists() && dir.canWrite();
        }
    }

    private boolean notifyListener(AuxFileChunk chunk, SPProgramID progId, String fileName, AuxFileTransferListener listener) {
        if (listener == null) return true;

        final long chunkXfer  = (chunk.getChunkNumber() + 1) * FileChunkSize;
        final long totalBytes = chunk.getFileSize();
        final long bytesXfer  = Math.min(chunkXfer, totalBytes);

        final AuxFileTransferEvent evt;
        evt = new AuxFileTransferEvent(this, progId, fileName, bytesXfer, totalBytes);
        return listener.transferProgressed(evt);
    }

    private AuxFileChunk readChunk(File srcFile, int chunkNumber) throws IOException {
        final long fileLength = srcFile.length();
        final long startPos   = (long) chunkNumber * FileChunkSize;
        final long remaining  = fileLength - startPos;
        final int bufSize     = (int) Math.min(remaining, FileChunkSize);

        final ByteBuffer buf = ByteBuffer.allocateDirect(bufSize);

        final RandomAccessFile raf = new RandomAccessFile(srcFile, "r");
        FileChannel fcin = null;
        try {
            fcin = raf.getChannel();
            fcin.position(startPos);
            while ((fcin.read(buf) != -1) && buf.hasRemaining()) {
                // empty
            }
        } finally {
            if (fcin != null) fcin.close();
        }

        final byte[] chunkData = new byte[bufSize];
        buf.flip();
        buf.get(chunkData);

        final long timestamp  = srcFile.lastModified();
        return new AuxFileChunk(chunkNumber, FileChunkSize, fileLength, timestamp, chunkData);
    }

    private static void writeChunk(AuxFileChunk chunk, File destFile) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(destFile, "rw");

        final byte[] data = chunk.getChunkData();

        final ByteBuffer buf = ByteBuffer.allocateDirect(data.length);
        buf.put(data);
        buf.flip();

        FileChannel fcout = null;
        try {
            fcout = raf.getChannel();
            fcout.position(fcout.size());
            while (buf.hasRemaining()) {
                fcout.write(buf);
            }
        } finally {
            if (fcout != null) {
                fcout.force(true);
                fcout.close();
            }
        }
    }

    @Override public byte[] fetchToMemory(final SPProgramID programId, final String remoteFileName) throws AuxFileException {

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(FileChunkSize)) {

            AuxFileChunk chunk;
            long timestamp = 0;
            int chunkIndex = 0;
            do {
                chunk = server.fetchChunk(programId, remoteFileName, chunkIndex++, FileChunkSize, timestamp);
                if (chunk == null) throw new AuxFileException("Read operation failed");
                timestamp = chunk.getTimestamp();
                out.write(chunk.getChunkData());

            } while (chunkIndex < chunk.getTotalChunks());

            return out.toByteArray();

        } catch (IOException ex) {
            throw AuxFileException.create(ex);

        }
    }


    @Override public boolean fetch(SPProgramID programId, String remoteFileName, File localFile, AuxFileTransferListener listener) throws AuxFileException {
        // Make sure the local file is writable.
        if (!isWritable(localFile)) {
            throw new AuxFileException(localFile + " is not writable");
        }

        File tmp = null;
        try {
            // Create a temp file for writing the result.
            tmp = File.createTempFile("auxfile", "tmp", localFile.getParentFile());

            // Fetch the file one chunk at a time and write to the tmp file.
            long timestamp = 0;
            AuxFileChunk chunk;
            int chunkIndex = 0;
            do {
                chunk = server.fetchChunk(programId, remoteFileName, chunkIndex++, FileChunkSize, timestamp);
                if (chunk == null) return false;
                timestamp = chunk.getTimestamp();
                writeChunk(chunk, tmp);

                if (!notifyListener(chunk, programId, remoteFileName, listener)) return false;
            } while (chunkIndex < chunk.getTotalChunks());

            // Move the tmp file to the final destination.
            if (!tmp.renameTo(localFile)) {
                throw new AuxFileException("OS does not support moving temp file " + tmp.getPath() + " to " + localFile.getPath());
            }
            return true;

        } catch (IOException ex) {
            throw AuxFileException.create(ex);

        } finally {
            if ((tmp != null) && tmp.exists()) tmp.delete();
        }
    }

    @Override public void store(SPProgramID programId, String remoteFileName, File localFile, AuxFileTransferListener listener) throws AuxFileException {
        try {
            // Store the file one chunk at a time
            String token = "";
            int chunkIndex = 0;
            AuxFileChunk chunk;
            do {
                chunk = readChunk(localFile, chunkIndex++);
                token = server.storeChunk(programId, remoteFileName, chunk, token);
                if (!notifyListener(chunk, programId, remoteFileName, listener)) return;
            } while (!chunk.isLastChunk());
        } catch (IOException ex) {
            throw AuxFileException.create(ex);
        }
    }

    @Override public void setDescription(SPProgramID programId, Collection<String> fileNames, String newDescription) throws AuxFileException {
        server.setDescription(programId, fileNames, newDescription);
    }

    @Override public void setChecked(SPProgramID programId, Collection<String> fileNames, boolean newChecked) throws AuxFileException {
        server.setChecked(programId, fileNames, newChecked);
    }
}
