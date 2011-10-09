/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.vfs.spi;

import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract real filesystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractFileSystem implements FileSystem {

    protected static final boolean NEEDS_CONVERSION = File.separatorChar != '/';

    /**
     * Get path.
     *
     * @param mountPoint the mount point
     * @param target the target
     * @return path instance
     */
    protected abstract Path getPath(VirtualFile mountPoint, VirtualFile target);

    /**
     * Read attributes.
     *
     * @param mountPoint the mount point
     * @param target the target
     * @return file attributes
     */
    protected BasicFileAttributes readAttributes(VirtualFile mountPoint, VirtualFile target) {
        try {
            Path path = getPath(mountPoint, target);
            return Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return Files.newInputStream(getPath(mountPoint, target));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public File getFile(VirtualFile mountPoint, VirtualFile target) {
        return getPath(mountPoint, target).toFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        try {
            Path path = getPath(mountPoint, target);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        try {
            Path path = getPath(mountPoint, target);
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        return readAttributes(mountPoint, target).lastModifiedTime().toMillis();
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        Path path = getPath(mountPoint, target);
        return Files.exists(path);
    }

    /** {@inheritDoc} */
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        return isDirectory(mountPoint, target) == false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        return readAttributes(mountPoint, target).isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        DirectoryStream<Path> stream = null;
        try {
            Path path = getPath(mountPoint, target);
            List<String> files = new ArrayList<String>();
            stream = Files.newDirectoryStream(path);
            for (Path p : stream)
                files.add(p.getFileName().toString());
            return files;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            VFSUtils.safeClose(stream);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        // no operation - the real FS can't be closed
    }
}
