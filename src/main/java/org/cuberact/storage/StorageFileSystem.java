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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

/**
 * @author Michal Nikodim (michal.nikodim@gmail.com)
 */
class StorageFileSystem extends FileSystem {

    private static final FileSystem DEFAULT_FS = FileSystems.getDefault();

    private final Path rootPath;

    StorageFileSystem(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public Path getPath(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return rootPath.resolve(Storage.normalizePath(path));
    }

    @Override
    public FileSystemProvider provider() {
        return DEFAULT_FS.provider();
    }

    @Override
    public void close() throws IOException {
        DEFAULT_FS.close();
    }

    @Override
    public boolean isOpen() {
        return DEFAULT_FS.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return DEFAULT_FS.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return DEFAULT_FS.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return DEFAULT_FS.getRootDirectories();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return DEFAULT_FS.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return DEFAULT_FS.supportedFileAttributeViews();
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return DEFAULT_FS.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return DEFAULT_FS.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return DEFAULT_FS.newWatchService();
    }
}
