/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
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
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.util.wrapper;

import org.objectweb.proactive.annotation.PublicAPI;


/**
 * <p>An reifiable object for wrapping the primitive Java type <code>long</code>.</p>
 * <p>Use this class as result for ProActive asynchronous method calls.</p>
 *
 * @author The ProActive Team
 *
 * Created on Jul 28, 2005
 */
@PublicAPI
public class LongMutableWrapper extends LongWrapper {

    /**
     * 
     */
    private static final long serialVersionUID = 40L;

    /**
     * The no arguments constructor for ProActive.
     */
    public LongMutableWrapper() {
        // nothing to do
    }

    /**
     * Construct an reifiable object for a <code>long</code>.
     * @param value the primitive <code>long</code> value.
     */
    public LongMutableWrapper(long value) {
        super(value);
    }

    /**
     * Set the value with a new one.
     * @param value the new value.
     */
    public void setLongValue(long value) {
        this.value = value;
    }
}
