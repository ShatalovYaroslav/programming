/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */
package org.objectweb.proactive.extensions.vfsprovider;

import java.io.IOException;
import java.net.URISyntaxException;

import org.objectweb.proactive.api.PARemoteObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.objectweb.proactive.core.remoteobject.RemoteObjectExposer;
import org.objectweb.proactive.core.remoteobject.RemoteObjectHelper;
import org.objectweb.proactive.core.remoteobject.exception.UnknownProtocolException;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.vfsprovider.client.ProActiveFileName;
import org.objectweb.proactive.extensions.vfsprovider.protocol.FileSystemServer;
import org.objectweb.proactive.extensions.vfsprovider.server.FileSystemServerImpl;


/**
 * Deploys {@link FileSystemServer} with instance of {@link FileSystemServerImpl} implementation on
 * the local runtime and provides URL access methods.
 */
public class FileSystemServerDeployer {

    private static final String FILE_SERVER_DEFAULT_NAME = "defaultFileSystemServer";

    /** URL of the remote object */
    final private String url;

    /** URL of root file exposed through that server */
    private String vfsRootURL;

    private FileSystemServerImpl fileSystemServer;

    private RemoteObjectExposer<FileSystemServerImpl> roe;

    /**
     * Deploys locally a FileSystemServer as a RemoteObject with a default name.
     *
     * @param rootPath
     * @param autoclosing
     * @throws IOException
     */
    public FileSystemServerDeployer(String rootPath, boolean autoclosing) throws IOException {
        this(FILE_SERVER_DEFAULT_NAME, rootPath, autoclosing);
    }

    /**
     * Deploys locally a FileSystemServer as a RemoteObject with a given name.
     *
     * @param name
     *            of deployed RemoteObject
     * @param rootPath
     * @param autoclosing
     * @throws IOException
     */
    public FileSystemServerDeployer(String name, String rootPath, boolean autoclosing) throws IOException {
        fileSystemServer = new FileSystemServerImpl(rootPath);
        try {
            roe = PARemoteObject.newRemoteObject(FileSystemServer.class.getName(), this.fileSystemServer);
            roe.createRemoteObject(name, false);
            url = roe.getURL();
        } catch (ProActiveException e) {
            // Ugly but createRemoteObject interface changed
            throw new IOException("" + ProActiveLogger.getStackTraceAsString(e));
        }

        try {
            vfsRootURL = ProActiveFileName.getServerVFSRootURL(url);
        } catch (URISyntaxException e) {
            ProActiveLogger.logImpossibleException(ProActiveLogger.getLogger(Loggers.VFS_PROVIDER_SERVER), e);
            throw new ProActiveRuntimeException(e);
        } catch (UnknownProtocolException e) {
            ProActiveLogger.logImpossibleException(ProActiveLogger.getLogger(Loggers.VFS_PROVIDER_SERVER), e);
            throw new ProActiveRuntimeException(e);
        }
        if (autoclosing)
            fileSystemServer.startAutoClosing();
    }

    public FileSystemServerImpl getLocalFileSystemServer() {
        return this.fileSystemServer;
    }

    public FileSystemServer getRemoteFileSystemServer() throws ProActiveException {
        return (FileSystemServer) RemoteObjectHelper.generatedObjectStub(this.roe.getRemoteObject());
    }

    /**
     * <strong>internal use only!</strong>
     */
    public String getRemoteFileSystemServerURL() {
        return this.url;
    }

    /**
     * @return URL pointing to root file exposed by this provider server; suitable for VFS provider
     */
    public String getVFSRootURL() {
        return vfsRootURL;
    }

    /**
     * Unexport the remote object and stops the server.
     *
     * @throws ProActiveException
     */
    public void terminate() throws ProActiveException {
        if (roe != null) {
            roe.unexportAll();
            roe.unregisterAll();
            roe = null;
            fileSystemServer.stopServer();
            fileSystemServer = null;
        }
    }
}