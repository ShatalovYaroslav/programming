/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package functionalTests.remoteobject.registry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.objectweb.proactive.core.remoteobject.RemoteObjectFactory;
import org.objectweb.proactive.core.remoteobject.RemoteObjectHelper;
import org.objectweb.proactive.core.remoteobject.RemoteObjectProtocolFactoryRegistry;

import functionalTests.FunctionalTest;


/**
 * Test for the remote object protocol registry
 * registration and deletion of a 'dummy' protocol
 * @author The ProActive Team
 *
 */
public class RemoteObjectProtocolRegistryTest extends FunctionalTest {
    @org.junit.Test
    public void addRemoveAProtocol() throws Exception {
        RemoteObjectProtocolFactoryRegistry.put("dummy", DummyProtocol.class);

        Class<? extends RemoteObjectFactory> dummyFactoryClass = RemoteObjectProtocolFactoryRegistry
                .get("dummy");

        assertNotNull(dummyFactoryClass);

        RemoteObjectFactory dummyFactory = RemoteObjectHelper.getRemoteObjectFactory("dummy");

        assertTrue(dummyFactory instanceof DummyProtocol);

        RemoteObjectProtocolFactoryRegistry.remove("dummy");

        dummyFactoryClass = RemoteObjectProtocolFactoryRegistry.get("dummy");

        assertNull(dummyFactoryClass);
    }
}
