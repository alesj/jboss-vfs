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

import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.nio.file.Path;

/**
 * A real filesystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public final class RealFileSystem extends AbstractFileSystem {

    private static final Logger log = Logger.getLogger("org.jboss.vfs.real");

    /**
     * The root real filesystem (singleton instance).
     */

    private final Path realRoot;

    /**
     * Construct a real filesystem with the given real root.
     *
     * @param realRoot the real root
     */
    public RealFileSystem(File realRoot) {
        this.realRoot = realRoot.toPath();
        log.tracef("Constructed real filesystem at root %s", realRoot);
    }

    /**
     * Get path.
     *
     * @param mountPoint the mount point
     * @param target the target
     * @return path instance
     */
    protected Path getPath(VirtualFile mountPoint, VirtualFile target) {
        if (mountPoint.equals(target)) {
            return realRoot;
        } else if (NEEDS_CONVERSION) {
            return realRoot.resolve(target.getPathNameRelativeTo(mountPoint).replace('/', File.separatorChar));
        } else {
            return realRoot.resolve(target.getPathNameRelativeTo(mountPoint));
        }
    }

    /**
     * {@inheritDoc}
     */
    public File getMountSource() {
        return realRoot.toFile();
    }
}
