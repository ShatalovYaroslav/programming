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
package functionalTests.group.dynamicthreadpool;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.objectweb.proactive.api.PAGroup;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.group.Group;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeFactory;

import functionalTests.GCMFunctionalTest;
import functionalTests.group.A;


/**
 * add and remove member in a group to see the threadpool vary
 *
 * @author The ProActive Team
 */

public class TestDynamicThreadPool extends GCMFunctionalTest {
    private A typedGroup = null;

    public TestDynamicThreadPool() throws ProActiveException {
        super(2, 1);
        super.startDeployment();
    }

    @org.junit.Test
    public void action() throws Exception {
        Group<A> g = PAGroup.getGroup(this.typedGroup);

        this.typedGroup.onewayCall();

        for (int i = 0; i < 100; i++) {
            g.add(g.get(i % 3));
        }

        this.typedGroup.onewayCall();

        int i = 3;
        while (i < g.size()) {
            g.remove(g.size() - 1);
        }
        this.typedGroup.onewayCall();
    }

    @Before
    public void preConditions() throws Exception {
        Object[][] params = { { "Agent0" }, { "Agent1" }, { "Agent2" } };
        Node[] nodes = { NodeFactory.getDefaultNode(), super.getANode(), super.getANode() };
        this.typedGroup = (A) PAGroup.newGroup(A.class.getName(), params, nodes);

        assertTrue(this.typedGroup != null);
    }
}
