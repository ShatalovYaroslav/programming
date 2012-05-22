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
package functionalTests.gcmdeployment.remoteobject;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.objectweb.proactive.gcmdeployment.GCMApplication;
import org.objectweb.proactive.gcmdeployment.GCMVirtualNode;

import functionalTests.GCMFunctionalTest;


public class TestGCMRemoteObjectsSubscribeFromAO extends GCMFunctionalTest {
    public TestGCMRemoteObjectsSubscribeFromAO() throws ProActiveException {
        super(1, 1);
        super.startDeployment();
    }

    @Test
    public void testRemote() throws ActiveObjectCreationException, NodeException, InterruptedException {

        Node node = super.getANode();
        RemoteAO rao = PAActiveObject.newActive(RemoteAO.class, new Object[] { super.gcmad }, node);

        Assert.assertTrue(rao.isSuccess().getBooleanValue());
    }

    static public class RemoteAO implements Serializable, RunActive {

        private static final long serialVersionUID = 52;
        GCMApplication gcma;
        boolean success = false;

        public RemoteAO() {

        }

        public RemoteAO(GCMApplication gcma) {
            this.gcma = gcma;
        }

        public void callback(Node node, String vnName) {
            System.out.println("Callback occured !");
            success = true;
        }

        public void runActivity(Body body) {
            Service service = new Service(body);

            try {
                GCMVirtualNode vn1 = gcma.getVirtualNode(DEFAULT_VN_NAME);
                vn1.subscribeNodeAttachment(PAActiveObject.getStubOnThis(), "callback", true);

                service.blockingServeOldest("callback");
            } catch (Exception e) {
                e.printStackTrace();
            }

            service.blockingServeOldest("isSuccess");
        }

        public BooleanWrapper isSuccess() {
            return new BooleanWrapper(success);
        }
    }
}
