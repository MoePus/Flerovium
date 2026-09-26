package com.moepus.flerovium.functions;

import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

public final class RetainedResourceReaders {
    private static final int MAX_RETAINED_READERS = 8;
    private static final int BYTE_BUFFER_SIZE = 8192;
    private static final int CHAR_BUFFER_SIZE = 8192;
    private static final Reader EMPTY_READER = new EmptyReader();
    private static final ArrayDeque<RetainedUtf8BufferedReader> POOL = new ArrayDeque<>();

    private RetainedResourceReaders() {
    }

    public static BufferedReader open(Resource resource) throws IOException {
        InputStream stream = resource.open();
        RetainedUtf8BufferedReader reader = borrow();
        reader.open(stream);
        return reader;
    }

    public static void clearRetained() {
        synchronized (POOL) {
            POOL.clear();
        }
    }

    private static RetainedUtf8BufferedReader borrow() {
        synchronized (POOL) {
            RetainedUtf8BufferedReader reader = POOL.pollFirst();
            return reader == null ? new RetainedUtf8BufferedReader() : reader;
        }
    }

    private static void release(RetainedUtf8BufferedReader reader) {
        synchronized (POOL) {
            if (POOL.size() < MAX_RETAINED_READERS) {
                POOL.addFirst(reader);
            }
        }
    }

    private static final class RetainedUtf8BufferedReader extends BufferedReader {
        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        private final byte[] byteArray = new byte[BYTE_BUFFER_SIZE];
        private final char[] charArray = new char[CHAR_BUFFER_SIZE];
        private final ByteBuffer bytes = ByteBuffer.wrap(this.byteArray);
        private final CharBuffer chars = CharBuffer.wrap(this.charArray);
        private final char[] singleChar = new char[1];
        private InputStream input;
        private boolean inputEnded;
        private boolean decodeFinished;
        private boolean flushFinished;
        private boolean closed = true;

        RetainedUtf8BufferedReader() {
            super(EMPTY_READER, 1);
            this.bytes.limit(0);
            this.chars.limit(0);
        }

        void open(InputStream input) {
            this.input = input;
            this.inputEnded = false;
            this.decodeFinished = false;
            this.flushFinished = false;
            this.closed = false;
            this.decoder.reset();
            this.bytes.clear();
            this.bytes.flip();
            this.chars.clear();
            this.chars.flip();
        }

        @Override
        public int read() throws IOException {
            int read = this.read(this.singleChar, 0, 1);
            return read < 0 ? -1 : this.singleChar[0];
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            checkBounds(buffer, offset, length);
            this.ensureOpen();
            if (length == 0) {
                return 0;
            }

            int copied = this.copyChars(buffer, offset, length);
            while (copied == 0 && this.fillChars()) {
                copied = this.copyChars(buffer, offset, length);
            }
            return copied == 0 ? -1 : copied;
        }

        @Override
        public boolean ready() throws IOException {
            this.ensureOpen();
            return this.chars.hasRemaining() || this.input.available() > 0;
        }

        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }

            InputStream stream = this.input;
            this.input = null;
            this.closed = true;
            try {
                if (stream != null) {
                    stream.close();
                }
            } finally {
                RetainedResourceReaders.release(this);
            }
        }

        private int copyChars(char[] buffer, int offset, int length) {
            int copied = Math.min(length, this.chars.remaining());
            if (copied > 0) {
                this.chars.get(buffer, offset, copied);
            }
            return copied;
        }

        private boolean fillChars() throws IOException {
            this.chars.clear();
            while (this.chars.position() == 0 && !this.flushFinished) {
                this.decodeMore();
            }
            this.chars.flip();
            return this.chars.hasRemaining();
        }

        private void decodeMore() throws IOException {
            CoderResult result = this.decodeFinished
                    ? this.decoder.flush(this.chars)
                    : this.decoder.decode(this.bytes, this.chars, this.inputEnded);
            if (result.isOverflow()) {
                return;
            }
            if (result.isError()) {
                this.throwCoding(result);
            }
            if (this.decodeFinished) {
                this.flushFinished = true;
            } else if (this.inputEnded) {
                this.decodeFinished = true;
            } else if (!this.readMoreBytes()) {
                this.inputEnded = true;
            }
        }

        private boolean readMoreBytes() throws IOException {
            this.bytes.compact();
            int offset = this.bytes.position();
            int length = this.bytes.remaining();
            int read = 0;
            while (read == 0 && length > 0) {
                read = this.input.read(this.byteArray, offset, length);
            }
            if (read > 0) {
                this.bytes.position(offset + read);
            }
            this.bytes.flip();
            return read >= 0;
        }

        private void ensureOpen() throws IOException {
            if (this.closed) {
                throw new IOException("Reader is closed");
            }
        }

        private void throwCoding(CoderResult result) throws CharacterCodingException {
            result.throwException();
        }
    }

    private static final class EmptyReader extends Reader {
        @Override
        public int read(char[] cbuf, int off, int len) {
            return -1;
        }

        @Override
        public void close() {
        }
    }

    private static void checkBounds(char[] buffer, int offset, int length) {
        if ((offset | length) < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException();
        }
    }
}
