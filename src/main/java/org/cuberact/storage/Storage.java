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

import org.cuberact.storage.deferred.DeferredExecutor;
import org.cuberact.storage.deferred.DeferredTask;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
public class Storage {

    private final Path path;
    private final URI uri;
    private final StorageType type;
    private final Charset charset;
    private final Predicate<DeferredTask> matcher = task ->
            task instanceof Resource.WriteTask &&
                    ((Resource.WriteTask) task).resource.getStorage().equals(Storage.this);

    public Storage(String path) {
        this(path, null);
    }

    public Storage(String path, StorageType type) {
        this(path, type, StandardCharsets.UTF_8);
    }

    public Storage(String path, StorageType type, Charset charset) {
        Path storagePath = Paths.get(normalizePath(path)).toAbsolutePath();
        if (type == null) {
            type = resolveType(storagePath);
        }
        if (Files.exists(storagePath)) {
            switch (type) {
                case DIRECTORY:
                    if (!Files.isDirectory(storagePath)) {
                        throw new StorageException("Storage '" + storagePath + "' is not directory");
                    }
                    break;
                case ZIP:
                    if (!isZipFile(storagePath)) {
                        throw new StorageException("Storage '" + storagePath + "' is not zip file");
                    }
                    break;
            }
        }
        this.path = Objects.requireNonNull(storagePath, "Path");
        this.uri = createURI("file:/" + normalizePath(this.path.toString()));
        this.type = Objects.requireNonNull(type, "Type");
        this.charset = Objects.requireNonNull(charset);
    }

    public Path getPath() {
        return path;
    }

    public Charset getCharset() {
        return charset;
    }

    public StorageType getType() {
        return type;
    }

    public boolean exists() {
        DeferredExecutor.runImmediately(matcher);
        return Files.exists(path);
    }

    public void delete() {
        DeferredExecutor.runImmediately(matcher);
        if (exists()) {
            delete(path);
        }
    }

    public Resource getResource(String path) {
        return new Resource(this, normalizePath(path));
    }

    public List<Resource> findResources(String glob) {
        DeferredExecutor.runImmediately(matcher);
        if (exists()) {
            return runInStorage(fs -> collectChildren(Storage.this, fs.getPath("/"), new GlobMatcher(glob)));
        }
        return Collections.emptyList();
    }

    public Resource findResource(String glob) {
        List<Resource> resources = findResources(glob);
        if (resources.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (Resource resource : resources) {
                sb.append("  ").append(resource.toString()).append("\n");
            }
            throw new StorageException("Found more then one resources in Storage '" + path.toString() + "' with glob '" + glob + "'\n\n" + sb.toString());
        }
        return resources.isEmpty() ? null : resources.get(0);
    }

    @Override
    public String toString() {
        return "Storage '" + normalizePath(path.toString()) + "'"
                + " [exists: " + exists()
                + ", type: " + getType()
                + "]";
    }

    @Override
    public int hashCode() {
        return uri.hashCode() * 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Storage) {
            Storage other = (Storage) obj;
            return Objects.equals(uri, other.uri);
        }
        return false;
    }

    static URI createURI(String line) {
        return URI.create(line.replaceAll(" ", "%20"));
    }

    <E> E runInStorage(StorageRunner<E> storageRunner) {
        try {
            if (!Files.exists(path)) {
                try {
                    switch (type) {
                        case DIRECTORY:
                            Files.createDirectories(path);
                            break;
                        case ZIP:
                            Files.createDirectories(path.getParent());
                            break;
                    }
                } catch (Throwable t) {
                    throw new StorageException(t);
                }
            }
            if (type == StorageType.ZIP) {
                try (FileSystem zipFileSystem = ZIP_FILE_SYSTEM_PROVIDER.newFileSystem(path, ZIP_ENV)) {
                    return storageRunner.run(zipFileSystem);
                } catch (Throwable e) {
                    throw new StorageException(e);
                }
            }
            return storageRunner.run(new StorageFileSystem(path));
        } catch (Throwable e) {
            throw new StorageException(e);
        }
    }

    interface StorageRunner<E> {
        E run(FileSystem fileSystem) throws Throwable;
    }

    private static StorageType resolveType(Path path) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                return StorageType.DIRECTORY;
            } else {
                if (isZipFile(path)) {
                    return StorageType.ZIP;
                } else {
                    throw new RuntimeException("Can't init Storage '" + path + "'. Only directory or zip file are allowed.");
                }
            }
        }
        String strPath = path.toString();
        return strPath.endsWith(".zip") || strPath.endsWith(".jar") ? StorageType.ZIP : StorageType.DIRECTORY;
    }

    private static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders().stream()
            .filter(fsp -> "jar".equalsIgnoreCase(fsp.getScheme()))
            .findFirst()
            .orElseThrow(() -> new StorageException("Can't find ZIP FILE SYSTEM PROVIDER"));
    private static final Map<String, String> ZIP_ENV = Stream.of(new SimpleEntry<>("create", "true"))
            .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    static String normalizePath(String path) {
        String normalized = path.replace('\\', '/');
        /*if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }*/
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static List<Resource> collectChildren(Storage storage, Path path, GlobMatcher globMatcher) {
        try {
            final List<Resource> children = new ArrayList<>();
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path child, BasicFileAttributes basicFileAttributes) {
                    String subPath = normalizePath(path.relativize(child).toString());
                    if (!"".equals(subPath) && globMatcher.matches(subPath)) {
                        children.add(new Resource(storage, subPath));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return children;
        } catch (Throwable t) {
            throw new StorageException(t);
        }
    }

    static void delete(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable e) {
            throw new StorageException(e);
        }
    }

    static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private static final byte[] ZIP_HEADER = new byte[]{80, 75, 3, 4}; //50 4B 03 04 - zip header in hex
    private static final byte[] ZIP_HEADER_EMPTY = new byte[]{80, 75, 5, 6}; //50 4B 05 06 - empty zip header in hex

    private static boolean isZipFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[4];
            int count = is.read(header);
            return count == 4 && (Arrays.equals(header, ZIP_HEADER) || Arrays.equals(header, ZIP_HEADER_EMPTY));
        } catch (IOException e) {
            return false;
        }
    }
}
