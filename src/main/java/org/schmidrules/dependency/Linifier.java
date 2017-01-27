package org.schmidrules.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Splits a stream to lines, providing location information. Sadly, this is quite a hassle because Windows Files have line separators with 1
 * or 2 characters, and BufferedReader is no replacement for this. Note: this implementation might have an issue with encodings with
 * preamble like UTF-16 BOM because lines are decoded separately.
 */
public class Linifier {
    private static final byte CR = (byte) 0x0A;
    private static final byte LF = (byte) 0x0D;

    public static Stream<Line> linify(InputStream in, Charset charset) {
        return StreamUtils.asStream(new LineIterator(in, charset));
    }

    private static class LineIterator implements Iterator<Line> {
        private final InputStream in;
        private final Charset charset;

        private Line next;
        private byte[] buffer = new byte[4096];
        private int bufferedBytes = 0; // number of bytes in the buffer
        private int charsReadOverall = 0;
        private int linesReadOverall = 0;
        private int bufferScanPos = 0; // idx of byte not yet read
        private int bufferLineDataStart = 0; // idx of first byte of the next String
        private byte currentByte;
        private boolean eofReached;
        private int usedLineEndingChars = 1;
        private byte usedLineEnding;

        public LineIterator(InputStream in, Charset charset) {
            this.in = in;
            this.charset = charset;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            next = next();
            return next != null;
        }

        @Override
        public Line next() {
            if (next != null) {
                Line current = next;
                next = null;
                return current;
            }
            try {
                return parse();
            } catch (RuntimeException e) {
                throw new IllegalStateException("Failed parsing in state " + this, e);
            }
        }

        private Line parse() {
            if (eofReached) {
                return null;
            }

            while (readByte()) {
                if (currentByte == CR || currentByte == LF) {
                    if (currentByte != usedLineEnding && usedLineEndingChars == 1 && stringBytes() == 1) {
                        // skip 2nd char if different to 1st
                        usedLineEndingChars++;
                        charsReadOverall++;
                        bufferLineDataStart++;
                    } else {
                        usedLineEnding = currentByte;
                        return parseLine();
                    }
                }
            }
            eofReached = true;
            bufferScanPos++;
            return parseLine();
        }

        private Line parseLine() {
            linesReadOverall++;
            String text = new String(buffer, bufferLineDataStart, stringBytes(), charset);
            int lineLength = text.length();
            Location location = new Location(linesReadOverall, charsReadOverall, 0, lineLength);
            charsReadOverall += 1 + lineLength;
            usedLineEndingChars = 1;
            bufferLineDataStart = bufferScanPos;

            return new Line(text, location);
        }

        private int stringBytes() {
            return bufferScanPos - bufferLineDataStart - 1;
        }

        private boolean readByte() {
            if (bufferBytesLeft() > 0) {
                readByteFromBuffer();
                return true;
            }

            if (!readToBuffer()) {
                return false;
            }

            readByteFromBuffer();
            return true;
        }

        private boolean readToBuffer() {
            prepareBufferForInput();
            int readBytes;
            try {
                readBytes = in.read(buffer, bufferedBytes, buffer.length - bufferedBytes);
            } catch (IOException e) {
                throw new IllegalStateException("Failed reading", e);
            }
            if (readBytes == -1) {
                return false;
            }
            bufferedBytes += readBytes;
            bufferScanPos = 0;
            return true;
        }

        private void prepareBufferForInput() {
            if (bufferedBytes != buffer.length) {
                // there is still space left, continue reading
                return;
            }

            if (bufferLineDataStart != 0) {
                // we can still free up space by moving stuff to the buffer start
                System.arraycopy(buffer, bufferLineDataStart, buffer, 0, buffer.length - bufferLineDataStart);
                bufferScanPos -= bufferLineDataStart;
                bufferedBytes -= bufferLineDataStart;
                bufferLineDataStart = 0;
                return;
            }

            // buffer is full and line is not ending, seems we need a bigger buffer
            byte[] newBuffer = new byte[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }

        private void readByteFromBuffer() {
            currentByte = buffer[bufferScanPos];
            bufferScanPos++;
        }

        private int bufferBytesLeft() {
            return bufferedBytes - bufferScanPos;
        }

        @Override
        public String toString() {
            return "LineIterator [in=" + in + ", charset=" + charset + ", next=" + next + ", buffer=" + Arrays.toString(buffer)
                    + ", bufferedBytes=" + bufferedBytes + ", charsReadOverall=" + charsReadOverall + ", linesReadOverall="
                    + linesReadOverall + ", bufferScanPos=" + bufferScanPos + ", bufferLineDataStart=" + bufferLineDataStart
                    + ", currentByte=" + currentByte + ", eofReached=" + eofReached + ", usedLineEndingChars=" + usedLineEndingChars
                    + ", usedLineEnding=" + usedLineEnding + "]";
        }
    }

    public static class Line {
        public final String text;
        public final Location location;

        public Line(String text, Location location) {
            this.text = text;
            this.location = location;
        }
    }
}
