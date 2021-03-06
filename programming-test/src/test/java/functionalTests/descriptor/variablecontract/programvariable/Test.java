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
package functionalTests.descriptor.variablecontract.programvariable;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;

import org.junit.Before;
import org.objectweb.proactive.core.descriptor.legacyparser.ProActiveDescriptorConstants;
import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.core.xml.VariableContractType;
import org.objectweb.proactive.extensions.gcmdeployment.PAGCMDeployment;
import org.objectweb.proactive.gcmdeployment.GCMApplication;

import functionalTests.FunctionalTest;


/**
 * Tests conditions for variables of type ProgramVariable
 */
public class Test extends FunctionalTest {
    private static URL XML_LOCATION = Test.class
            .getResource("/functionalTests/descriptor/variablecontract/programvariable/Test.xml");
    GCMApplication gcma;
    boolean bogusFromDescriptor;
    boolean bogusFromProgram;
    boolean bogusCheckContract;

    @Before
    public void initTest() throws Exception {
        bogusFromDescriptor = true;
        bogusCheckContract = true;
        bogusFromProgram = true;
    }

    @org.junit.Test
    public void action() throws Exception {
        VariableContractImpl variableContract = new VariableContractImpl();

        //Setting from Program
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("test_var", "helloworld");
        variableContract.setVariableFromProgram(map,
                VariableContractType.getType(ProActiveDescriptorConstants.VARIABLES_PROGRAM_TAG));

        //Setting bogus empty variable from Program (this should fail)
        try {
            variableContract.setVariableFromProgram("bogus_from_program", "",
                    VariableContractType.getType(ProActiveDescriptorConstants.VARIABLES_PROGRAM_TAG));
        } catch (Exception e) {
            bogusFromProgram = false;
        }

        //Setting from Descriptor
        variableContract.setDescriptorVariable("force_prog_set", "",
                VariableContractType.getType(ProActiveDescriptorConstants.VARIABLES_PROGRAM_TAG));
        bogusCheckContract = variableContract.checkContract(); //Contract should fail (return false)
        //Now it should be ok

        variableContract.setVariableFromProgram("force_prog_set", "forcedhelloworld",
                VariableContractType.getType(ProActiveDescriptorConstants.VARIABLES_PROGRAM_TAG));

        //Setting bogus from descriptor (this should fail)
        try {
            variableContract.setDescriptorVariable("nonempty", "non_empty",
                    VariableContractType.getType(ProActiveDescriptorConstants.VARIABLES_PROGRAM_TAG));
        } catch (Exception e) {
            bogusFromDescriptor = false;
        }

        variableContract.setVariableFromProgram("forcedFromDesc", "forcedhelloworldFromDesc",
                VariableContractType.getType(ProActiveDescriptorConstants.VARIABLES_PROGRAM_TAG));

        gcma = PAGCMDeployment.loadApplicationDescriptor(XML_LOCATION, variableContract);

        variableContract = (VariableContractImpl) gcma.getVariableContract();

        //System.out.println(variableContract);
        assertTrue(!bogusCheckContract);
        assertTrue(!bogusFromDescriptor);
        assertTrue(!bogusFromProgram);
        assertTrue(variableContract.getValue("test_var").equals("helloworld"));
        assertTrue(variableContract.getValue("force_prog_set").equals("forcedhelloworld"));
        assertTrue(variableContract.getValue("forcedFromDesc").equals("forcedhelloworldFromDesc"));
        assertTrue(variableContract.isClosed());
        assertTrue(variableContract.checkContract());
    }
}
