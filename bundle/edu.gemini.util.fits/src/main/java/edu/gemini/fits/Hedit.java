//
// $Id: Hedit.java 80 2005-09-05 15:22:50Z shane $
//

package edu.gemini.fits;

import edu.gemini.file.util.LockedFileChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility for editing FITS file headers.
 */
public final class Hedit {
    private static final Logger LOG = Logger.getLogger(Hedit.class.getName());

    private File _file;

    public Hedit(File fitsFile) {
        _file = fitsFile;
    }

    public List<Header> readAllHeaders()
            throws IOException, FitsParseException, InterruptedException {

        long startTime = System.currentTimeMillis();
        LockedFileChannel lfc = new LockedFileChannel(_file, LockedFileChannel.Mode.r);
        lfc.lock();

        FileChannel channel = lfc.getChannel();

        List<Header> allHeaders = new ArrayList<Header>();
        Set<String> keywords = null;
        long endTime = System.currentTimeMillis();

        try {
            int headerIndex = 0;
            while (channel.position() < channel.size()) {
                Header currentHeader = _readHeader(channel, keywords, headerIndex++);
                allHeaders.add(currentHeader);

                int dataSize = calculateDataSectionSize(currentHeader);

                channel.position(dataSize + channel.position());
            }
        } finally {
            try {
                lfc.unlock();
            } catch (IOException ex) {
                // not sure what to do here
                String msg = "Could not unlock cleanly: " + _file.getName();
                LOG.log(Level.SEVERE, msg, ex);
            } finally {
                lfc.close();
            }
        }


        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Read Primary Header in " +
                    (endTime - startTime) + "ms");
        }
        return allHeaders;
    }

    private static int calculateDataSectionSize(Header pdu) {
        if (!pdu.getKeywords().isEmpty()) {
            int bytesSize = pdu.get("BITPIX").getIntValue();
            int nAxis = pdu.get("NAXIS").getIntValue();
            int totalAxis = 1;
            for (int i = 1; i <= nAxis; i++) {
                totalAxis *= pdu.get("NAXIS" + i).getIntValue();
            }
            int pCount = 0;
            if (pdu.getKeywords().contains("PCOUNT")) {
                pCount = pdu.get("PCOUNT").getIntValue();
            }
            totalAxis += pCount;
            int gCount = 1;
            if (pdu.getKeywords().contains("GCOUNT")) {
                gCount = pdu.get("GCOUNT").getIntValue();
            }
            int dataSize = totalAxis * gCount * Math.abs(bytesSize) / 8;
            dataSize += dataSize % FitsConstants.RECORD_SIZE;
            return dataSize;
        } else {
            return 0;
        }
    }

    public Header readPrimary() throws IOException, FitsParseException, InterruptedException {
        return readPrimary(null);
    }

    public Header readPrimary(Set<String> keywords)
            throws IOException, FitsParseException, InterruptedException {

        long startTime = System.currentTimeMillis();
        LockedFileChannel lfc = new LockedFileChannel(_file, LockedFileChannel.Mode.r);
        lfc.lock();

        FileChannel channel = lfc.getChannel();

        Header res;
        try {
            res = _readHeader(channel, keywords, 0);
        } finally {
            try {
                lfc.unlock();
            } catch (IOException ex) {
                // not sure what to do here
                String msg = "Could not unlock cleanly: " + _file.getName();
                LOG.log(Level.SEVERE, msg, ex);
            } finally {
                lfc.close();
            }
        }
        long endTime = System.currentTimeMillis();

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Read Primary Header in " +
                    (endTime - startTime) + "ms");
        }
        return res;
    }

    private static Header _readHeader(FileChannel channel, Set<String> keywords, int headerIndex)
            throws IOException, FitsParseException {

        byte[] bytes = new byte[FitsConstants.RECORD_SIZE];
        Header res = new DefaultHeader(headerIndex);

        boolean done = false;
        RecordIterator fit = RecordIterator.iterateFile(channel);
        while (!done && fit.hasNext()) {
            Record rec = fit.next();
            rec.getBuffer().get(bytes);
            int offset = 0;

            while (offset < bytes.length) {
                String keyword = (new String(bytes, offset, 8, FitsConstants.CHARSET_NAME)).trim();
                if ("END".equals(keyword)) {
                    done = true;
                    break;
                }

                if ((keywords == null) || keywords.contains(keyword)) {
                    String image = new String(bytes, offset,
                            FitsConstants.HEADER_ITEM_SIZE, FitsConstants.CHARSET_NAME);
                    res.add(HeaderItemFormat.parse(image));
                }
                offset += FitsConstants.HEADER_ITEM_SIZE;
            }
        }
        return res;
    }

    @SuppressWarnings({"UNUSED_THROWS", "MethodMayBeStatic"})
    public void deletePrimary(Collection<String> keywords) throws IOException {
        throw new UnsupportedOperationException("not yet");
    }

    @SuppressWarnings({"UNUSED_THROWS", "MethodMayBeStatic"})
    public void setPrimary(Header header) throws IOException {
        throw new UnsupportedOperationException("not yet");
    }

    public void updatePrimary(Collection<? extends HeaderItem> updates) throws IOException, InterruptedException {
        updateHeader(updates, 0);
    }

    public void updateHeader(Collection<? extends HeaderItem> updates, int headerIndex) throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();
        LockedFileChannel lfc = new LockedFileChannel(_file, LockedFileChannel.Mode.rw);
        lfc.lock();

        FileChannel channel = lfc.getChannel();

        try {
            _updateHeader(channel, updates, headerIndex);
        } finally {
            try {
                lfc.unlock();
            } catch (IOException ex) {
                // not sure what to do here
                String msg = "Could not unlock cleanly: " + _file.getName();
                LOG.log(Level.SEVERE, msg, ex);
            } finally {
                lfc.close();
            }
        }
        long endTime = System.currentTimeMillis();

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Updated Primary Header in " +
                    (endTime - startTime) + "ms");
        }
    }


    private static <H extends HeaderItem> void _updateHeader(FileChannel channel, Collection<H> updates, int headerIndex)
            throws IOException {
        if (updates.size() == 0) {
            return; // nothing to do
        }

        Map<String, H> hash = HeaderItemUtil.hash(updates);

        boolean done = false;
        int offset = 0;
        long pos = 0;

        byte[] bytes = new byte[FitsConstants.RECORD_SIZE];

        advanceToHeader(channel, headerIndex);
        
        RecordIterator fit = RecordIterator.iterateFile(channel);
        while (!done && fit.hasNext()) {
            Record rec = fit.next();

            pos = rec.position();
            rec.getBuffer().get(bytes);
            offset = 0;

            while (offset < bytes.length) {

                String keyword = (new String(bytes, offset, 8, FitsConstants.CHARSET_NAME)).trim();
                if ("END".equals(keyword)) {
                    done = true;
                    break;
                }

                if (hash.containsKey(keyword)) {
                    HeaderItem item = hash.get(keyword);
                    ByteBuffer writeBuf = HeaderItemFormat.toByteBuffer(item);
                    FileUtil.writeBuf(channel, writeBuf, pos + offset);
                    hash.remove(keyword);
                }
                offset += FitsConstants.HEADER_ITEM_SIZE;
            }
        }


        if (hash.size() == 0) {
            return; // nothing to add
        }

        // Create a List of the remaining header items.
        List<H> remUpdates = new ArrayList<H>(updates);
        for (ListIterator<H> it = remUpdates.listIterator(); it.hasNext();) {
            HeaderItem item = it.next();
            if (!hash.containsKey(item.getKeyword())) {
                it.remove();
            }
        }

        // Fill up any remaining space in the current record.
        int remainingCards = done ? ((FitsConstants.RECORD_SIZE - offset) / FitsConstants.HEADER_ITEM_SIZE) : 0;
        int limit = Math.min(remainingCards, remUpdates.size());
        if (limit > 0) {
            int bufsize = limit * FitsConstants.HEADER_ITEM_SIZE;
            if (limit < remainingCards) {
                bufsize += FitsConstants.HEADER_ITEM_SIZE; // for END
            }
            ByteBuffer buf = ByteBuffer.allocate(bufsize);
            for (int i = 0; i < limit; ++i) {
                HeaderItem item = remUpdates.get(i);
                buf.put(HeaderItemFormat.toBytes(item));
            }

            if (limit < remainingCards) {
                buf.put("END".getBytes());
            }
            buf.flip();
            FileUtil.writeBuf(channel, buf, pos + offset);
            if (limit < remainingCards) {
                return;
            }
        }

        // Now we know we'll have to insert one or more records.
        // Figure out how many.
        pos += FitsConstants.RECORD_SIZE; // where the insert will happen
        int rem = remUpdates.size() - limit + 1;  // + 1 for "END"

        int nrecs = rem / FitsConstants.ITEMS_PER_RECORD +
                ((rem % FitsConstants.ITEMS_PER_RECORD == 0) ? 0 : 1);
        ByteBuffer buf = ByteBuffer.allocate(nrecs * FitsConstants.RECORD_SIZE);
        for (int i = limit; i < remUpdates.size(); ++i) {
            HeaderItem item = remUpdates.get(i);
            buf.put(HeaderItemFormat.toBytes(item));
        }
        buf.put("END".getBytes());

        // Fill remainder of the buffer with blank chars.
        for (int i = buf.position(); i < buf.capacity(); ++i) {
            buf.put((byte) ' ');
        }

        buf.flip();
        FileUtil.insert(channel, buf, pos);
    }

    /**
     * Moves the channel position to the header with index headerIndex
     */
    private static void advanceToHeader(FileChannel channel, int headerIndex) throws IOException {
        int currentHeaderIndex = 0;

        while (headerIndex > currentHeaderIndex) {
            Header currentHeader = null;
            try {
                currentHeader = _readHeader(channel, null, currentHeaderIndex++);
            } catch (FitsParseException e) {
                throw new IOException(e);
            }

            int dataSize = calculateDataSectionSize(currentHeader);

            channel.position(dataSize + channel.position());
        }
    }
}