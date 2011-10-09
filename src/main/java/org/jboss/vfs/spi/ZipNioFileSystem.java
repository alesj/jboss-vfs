/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.CodeSigner;

/**
 * Zip file system based on new NIO fs.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ZipNioFileSystem extends AbstractFileSystem {
    private Path archivePath;
    private volatile java.nio.file.FileSystem zipfs;

    public ZipNioFileSystem(File archiveFile) throws IOException {
        this.archivePath = archiveFile.toPath();
        this.zipfs = FileSystems.newFileSystem(archivePath, null);
    }

    protected Path getPath(VirtualFile mountPoint, VirtualFile target) {
        if (mountPoint.equals(target))
            return archivePath;

        String relativePath;
        if (NEEDS_CONVERSION) {
            relativePath = target.getPathNameRelativeTo(mountPoint).replace('/', File.separatorChar);
        } else {
            relativePath = target.getPathNameRelativeTo(mountPoint);
        }
        return zipfs.getPath(relativePath);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        return false;
    }

    @Override
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        Path path = getPath(mountPoint, target);
        // TODO -- how do we get the CodeSigner[] from new NIO api?
        return null;
    }

    @Override
    public File getMountSource() {
        return archivePath.toFile();
    }

    @Override
    public void close() throws IOException {
        VFSUtils.safeClose(zipfs);
    }
}
