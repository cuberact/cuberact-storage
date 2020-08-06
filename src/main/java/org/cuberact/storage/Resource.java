/*
 * Copyright 2017 Michal Nikodim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cuberact.storage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.cuberact.storage.deferred.DeferredExecutor;
import org.cuberact.storage.deferred.DeferredTask;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class Resource {

    private final Storage storage;
    private final String path;
    private URI uri;
    private String fullName;
    private String name;
    private String extension;
    private final Predicate<DeferredTask> matcher = task ->
            task instanceof WriteTask &&
                    ((WriteTask) task).resource.equals(Resource.this);

    Resource(Storage storage, String path) {
        this.storage = storage;
        this.path = path;
    }

    public Storage getStorage() {
        return storage;
    }

    public String getPath() {
        return path;
    }

    public URI getUri() {
        if (uri == null) {
            if (getStorage().getType() == StorageType.ZIP) {
                this.uri = Storage.createURI("jar:/file:/" + Storage.normalizePath(getStorage().getPath().toString()) + "!/" + path);
            } else {
                this.uri = Storage.createURI("file:/" + Storage.normalizePath(getStorage().getPath().resolve(path).toString()));
            }
        }
        return uri;
    }

    public String getFullName() {
        if (fullName == null) {
            fullName = path.lastIndexOf("/") != -1 ? path.substring(path.lastIndexOf("/") + 1) : path;
        }
        return fullName;
    }

    public String getName() {
        if (name == null) {
            int i = getFullName().lastIndexOf('.');
            if (i != -1) {
                name = getFullName().substring(0, i);
            } else {
                name = getFullName();
            }
        }
        return name;
    }

    public String getExtension() {
        if (extension == null) {
            int i = getFullName().lastIndexOf('.');
            if (i != -1) {
                extension = getFullName().substring(i + 1);
            } else {
                extension = "";
            }
        }
        return extension;
    }

    public boolean exists() {
        ifWriteWaitingThenRunImmediately();
        return storage.exists() && storage.runInStorage(fs -> Files.exists(fs.getPath(path)));
    }

    public long size() {
        ifWriteWaitingThenRunImmediately();
        return storage.runInStorage(fs -> Files.size(fs.getPath(path)));
    }

    public void delete() {
        ifWriteWaitingThenRunImmediately();
        storage.runInStorage(fs -> {
            Storage.delete(fs.getPath(path));
            return null;
        });
    }

    public byte[] readToBytes() {
        ifWriteWaitingThenRunImmediately();
        return storage.runInStorage(fs -> {
            try {
                return Files.readAllBytes(fs.getPath(path));
            } catch (IOException e) {
                throw new StorageException(e);
            }
        });
    }

    public ByteBuffer readToByteBuffer() {
        byte[] content = readToBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(content.length).order(ByteOrder.nativeOrder());
        byteBuffer.put(content);
        byteBuffer.flip();
        return byteBuffer;
    }

    public String readToString() {
        return new String(readToBytes(), getStorage().getCharset());
    }

    public <E> E readFromInputStream(InputStreamProcessor<E> processor) {
        ifWriteWaitingThenRunImmediately();
        return storage.runInStorage(fs -> {
            try (InputStream inputStream = Files.newInputStream(fs.getPath(path))) {
                return processor.read(inputStream);
            } catch (Throwable e) {
                throw new StorageException(e);
            }
        });
    }

    public void write(CharSequence content, boolean append) {
        write(content.toString().getBytes(getStorage().getCharset()), append);
    }

    public void write(Reader content, boolean append) {
        ifWriteWaitingThenRunImmediately();
        storage.runInStorage(fs -> {
            Path writePath = fs.getPath(path);
            try {
                Files.createDirectories(writePath.getParent());
            } catch (IOException e) {
                throw new StorageException(e);
            }
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(writePath, getStorage().getCharset(), getOpenOption(append))) {
                char[] buffer = new char[4096];
                int n;
                while ((n = content.read(buffer)) != -1) {
                    bufferedWriter.write(buffer, 0, n);
                }
                return null;
            } catch (IOException e) {
                throw new StorageException(e);
            } finally {
                Storage.closeQuietly(content);
            }
        });
    }

    public void write(byte[] content, boolean append) {
        ifWriteWaitingThenRunImmediately();
        writeInternal(content, append);
    }

    public void write(InputStream content, boolean append) {
        write(new InputStreamReader(content, getStorage().getCharset()), append);
    }

    public void writeBinary(InputStream inputStream) {
        ifWriteWaitingThenRunImmediately();
        storage.runInStorage(fs -> {
            Path writePath = fs.getPath(path);
            try {
                Files.createDirectories(writePath.getParent());
            } catch (IOException e) {
                throw new StorageException(e);
            }
            try (OutputStream outputStream = Files.newOutputStream(writePath, getOpenOption(false))) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, n);
                }
                return null;
            } catch (IOException e) {
                throw new StorageException(e);
            } finally {
                Storage.closeQuietly(inputStream);
            }
        });
    }

    public void ifWriteWaitingThenRunImmediately() {
        DeferredExecutor.runImmediately(matcher);
    }

    public void writeDeferred(CharSequence content) {
        writeDeferred(content.toString().getBytes(getStorage().getCharset()));
    }

    public void writeDeferred(final byte[] content) {
        writeDeferred(new BytesSupplier(content));
    }

    public void writeDeferred(final Supplier<byte[]> contentProvider) {
        DeferredExecutor.runDeferred(new WriteTask(this, contentProvider));
    }

    void writeInternal(byte[] content, boolean append) {
        storage.runInStorage(fs -> {
            try {
                Path writePath = fs.getPath(path);
                Path parentPath = writePath.getParent();
                if (parentPath != null) {
                    Files.createDirectories(parentPath);
                }
                Files.write(writePath, content, getOpenOption(append));
                return null;
            } catch (IOException e) {
                throw new StorageException(e);
            }
        });
    }

    @Override
    public int hashCode() {
        return getUri().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            Resource other = (Resource) obj;
            return Objects.equals(getUri(), other.getUri());
        }
        return false;
    }

    @Override
    public String toString() {
        return "  Resource - path = " + path
                + " [exists: " + exists()
                + ", uri: " + getUri().toString()
                + "]";
    }

    private OpenOption getOpenOption(boolean append) {
        if (exists()) {
            return append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
        }
        return StandardOpenOption.CREATE;
    }

    private static class BytesSupplier implements Supplier<byte[]> {

        private final byte[] bytes;

        private BytesSupplier(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte[] get() {
            return bytes;
        }
    }

    static class WriteTask extends DeferredTask {

        final Resource resource;
        final Supplier<byte[]> contentProvider;

        WriteTask(Resource resource, Supplier<byte[]> contentProvider) {
            super(DEFERRED_DELAY_IN_MILLISECONDS);
            this.resource = resource;
            this.contentProvider = contentProvider;
        }

        @Override
        public void run() {
            resource.writeInternal(contentProvider.get(), false);
        }

        @Override
        public int hashCode() {
            return 31 + resource.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && Objects.equals(resource, ((WriteTask) o).resource);
        }

        @Override
        public String toString() {
            return resource.getUri().toString();
        }
    }
}
