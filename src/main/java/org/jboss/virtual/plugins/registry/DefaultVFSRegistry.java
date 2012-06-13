/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.virtual.plugins.registry;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.TempInfo;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextConstraints;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;
import org.jboss.virtual.spi.registry.VFSRegistry;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

/**
 * Default vfs registry.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DefaultVFSRegistry extends VFSRegistry
{
   /** Do we force canonical lookup */
   private static boolean forceCanonical;

   static
   {
      forceCanonical = AccessController.doPrivileged(new CheckForceCanonical());

      if (forceCanonical)
         Logger.getLogger(DefaultVFSRegistry.class).info("VFS force canonical lookup is enabled.");
   }

   /**
    * Get vfs cache.
    *
    * @return the vfs cache
    */
   protected VFSCache getCache()
   {
      return VFSCacheFactory.getInstance();
   }

   /**
    * Is the vfs context cacheable.
    *
    * @param context the vfs context
    * @return true if context is cacheable, false otherwise
    */
   protected boolean isCacheable(VFSContext context)
   {
      Set<VFSContextConstraints> constraints = context.getConstraints();
      return constraints != null && constraints.contains(VFSContextConstraints.CACHEABLE);
   }

   public void addContext(VFSContext context)
   {
      if (isCacheable(context))
      {
         getCache().putContext(context);
      }
   }

   public void removeContext(VFSContext context)
   {
      if (isCacheable(context))
      {
         getCache().removeContext(context);
      }
   }

   /**
    * Canonicalize uri.
    *
    * @param uri the uri
    * @return canonical uri
    * @throws IOException for any IO error
    */
   protected static URI canonicalize(URI uri) throws IOException
   {
      if (forceCanonical)
      {
         String path = new File(VFSUtils.stripProtocol(uri)).getCanonicalPath();
         try
         {
            return new URI(uri.getScheme(), uri.getHost(), path, uri.getQuery(), uri.getFragment());
         }
         catch (URISyntaxException e)
         {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
         }
      }
      return uri;
   }

   public VFSContext getContext(URI uri) throws IOException
   {
      if (uri == null)
         throw new IllegalArgumentException("Null uri");

      uri = canonicalize(uri);
      VFSContext context = getCache().findContext(uri);
      if (context != null)
      {
         String relativePath = VFSUtils.getRelativePath(context, uri);
         if (relativePath.length() == 0)
            return context;
      }
      return null;
   }

   public VirtualFile getFile(URI uri) throws IOException
   {
      if (uri == null)
         throw new IllegalArgumentException("Null uri");

      uri = canonicalize(uri);
      VFSContext context = getCache().findContext(uri);
      if (context != null)
      {
         String relativePath = VFSUtils.getRelativePath(context, uri);

         TempInfo ti = context.getFurthestParentTemp(relativePath);
         if (ti != null)
         {
            String path = ti.getPath();
            String subpath = relativePath.substring(path.length());
            VirtualFileHandler child = findHandler(ti.getHandler(), subpath, true);
            if (child != null)
                  return child.getVirtualFile();
         }

         VirtualFileHandler root = context.getRoot();
         VirtualFileHandler child = findHandler(root, relativePath, false);
         return child.getVirtualFile();
      }
      return null;
   }

   /**
    * Find the handler.
    *
    * @param root the root
    * @param path the path
    * @return child handler
    * @param allowNotFound do we allow not found
    * @throws IOException for any error
    */
   protected VirtualFileHandler findHandler(VirtualFileHandler root, String path, boolean allowNotFound) throws IOException
   {
      VirtualFileHandler child = root.getChild(path);
      if (child == null && allowNotFound == false)
      {
         List<VirtualFileHandler> children = root.getChildren(true);
         throw new IOException("Child not found " + path + " for " + root + ", available children: " + children);
      }
      return child;
   }

   /**
    * <tt>PriviligedAction</tt> class for checking a system property
    */
   private static class CheckForceCanonical implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.FORCE_CANONICAL, "false");
         return Boolean.valueOf(forceString);
      }
   }
}
