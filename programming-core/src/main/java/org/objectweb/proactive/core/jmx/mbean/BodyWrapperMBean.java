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
package org.objectweb.proactive.core.jmx.mbean;

import java.io.Serializable;

import javax.management.ObjectName;

import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.jmx.notification.NotificationType;


/**
 * MBean representing an active object.
 *
 * @author The ProActive Team
 */
public interface BodyWrapperMBean extends Serializable {

    /**
     * Returns the unique id.
     *
     * @return The unique id of this active object.
     */
    public UniqueID getID();

    /**
     * Returns the name of the body of the active object that can be used for
     * displaying information
     *
     * @return the name of the body of the active object
     */
    public String getName();

    /**
     * Returns the url of the node containing the active object.
     *
     * @return Returns the url of the node containing the active object
     */
    public String getNodeUrl();

    /**
     * Send a new notification.
     *
     * @param type
     *            The type of the notification. See {@link NotificationType}
     */
    public void sendNotification(String type);

    /**
     * Send a new notification.
     *
     * @param type
     *            Type of the notification. See {@link NotificationType}
     * @param userData
     *            The user data.
     */
    public void sendNotification(String type, Object userData);

    /**
     * Returns the object name used for this MBean.
     * @return The object name used for this MBean.
     */
    public ObjectName getObjectName();

    /**
     * Returns <code>True</code> if the reified object of the body implements
     * {@link java.io.Serializable} <code>False</code> otherwise.
     *
     * @return <code>True</code> if the reified object of the body implements
     *         {@link java.io.Serializable} <code>False</code> otherwise
     */
    public boolean getIsReifiedObjectSerializable();
}
