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
package org.objectweb.proactive.core.body.future;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.body.UniversalBody;
import org.objectweb.proactive.core.body.exceptions.FutureMonitoringPingFailureException;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.core.util.HeartbeatResponse;
import org.objectweb.proactive.core.util.ActiveObjectLocationInfo;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


public class FutureMonitoring implements Runnable {

    /** Ping one body every 21s */
    private static int TTM = 21000;

    /**
     * For each body, the list of futures to monitor. We ping the updater body,
     * so we should detect a broken automatic continuations chain.
     */
    private static final Map<UniqueID, ConcurrentLinkedQueue<FutureProxy>> futuresToMonitor = new ConcurrentHashMap<UniqueID, ConcurrentLinkedQueue<FutureProxy>>();
    private static final ConcurrentHashMap<UniqueID, String> nodeUrls = new ConcurrentHashMap<UniqueID, String>();

    static final Logger logger = ProActiveLogger.getLogger(Loggers.CORE);
    static {

        /* Dynamically configurable to make shorter tests */
        String ttm = CentralPAPropertyRepository.PA_FUTUREMONITORING_TTM.getValueAsString();
        if (ttm != null) {
            int tmp = Integer.parseInt(ttm);
            if (tmp >= 0) {
                TTM = tmp;
            } else {
                logger.error(CentralPAPropertyRepository.PA_FUTUREMONITORING_TTM.getName() +
                    " must be positive. This value is ignored");
            }
        }

        if (TTM > 0) {
            Thread t = new Thread(new FutureMonitoring(), "Monitoring the Futures");
            t.setDaemon(true);
            t.start();
        } else {
            logger.info("Future Monitoring is disabled");
        }
    }

    /** To avoid copy-pasting */
    private static void monitoringDelay() {
        try {
            Thread.sleep(TTM);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true iff a body has been pinged
     */
    private static boolean pingBody(UniqueID bodyId) {
        boolean pinged = false;
        FutureMonitoringPingFailureException bodyException = null;
        Collection<FutureProxy> futures = futuresToMonitor.get(bodyId);
        String nodeUrl = nodeUrls.get(bodyId);
        if (futures == null) {

            /*
             * By the time we got to iterate over these futures, they have all
             * been updated, so the body entry was removed.
             */
            return false;
        }

        for (FutureProxy fp : futures) {
            if (!pinged) {

                /* Not yet pinged somebody */
                UniversalBody body = null;

                synchronized (fp) {
                    if (fp.isAwaited()) {
                        body = fp.getUpdater();
                    }
                }
                if (body != null) {
                    /* OK, Found somebody to ping */
                    pinged = true;
                    try {
                        Integer state = (Integer) body.receiveHeartbeat();
                        /* If the object is dead, ping failed ... */
                        if (state.equals(HeartbeatResponse.IS_DEAD)) {
                            throw new ProActiveRuntimeException("Awaited body " + bodyId + " on " + nodeUrl +
                                " has been terminated.");
                        }
                        /* Successful ping, nothing more to do */
                        return true;
                    } catch (Exception e) {
                        /* Ping failure, update all awaited futures on this node with the exception */
                        bodyException = new FutureMonitoringPingFailureException(bodyId, nodeUrl, e);
                    }
                }
            }

            if (bodyException != null) {
                synchronized (fp) {
                    if (fp.isAwaited()) {
                        fp.receiveReply(new MethodCallResult(null, bodyException));
                    }
                }
            }
        }
        return pinged;
    }

    /**
     * Arrange to ping a single body every TTM
     * There is a single daemon thread running the monitoring, so
     * it will end with the JVM.
     */
    public void run() {
        for (;;) {
            boolean pingedOneBody = false;
            for (UniqueID bodyId : futuresToMonitor.keySet()) {
                boolean pingedThisBody = pingBody(bodyId);
                if (pingedThisBody) {
                    pingedOneBody = true;
                    monitoringDelay();
                }
            }
            if (!pingedOneBody) {
                monitoringDelay();
            }
        }
    }

    private static UniqueID getUpdaterBodyId(FutureProxy fp) {
        UniversalBody body = fp.getUpdater();
        if (body == null) {
            new Exception("Cannot monitor this future, unknown updater body").printStackTrace();
            return null;
        }
        return body.getID();
    }

    private static ActiveObjectLocationInfo getUpdaterLocationInfo(FutureProxy fp) {
        UniversalBody body = fp.getUpdater();
        if (body == null) {
            new Exception("Cannot monitor this future, unknown updater body").printStackTrace();
            return null;
        }
        UniqueID id = body.getID();
        String nodeUrl = "[unknown]";
        try {
            nodeUrl = body.getNodeURL();
        } catch (Throwable e) {

        }
        return new ActiveObjectLocationInfo(id, nodeUrl);
    }

    public static void removeFuture(FutureProxy fp) {
        UniqueID updaterId = getUpdaterBodyId(fp);
        if (updaterId == null) {
            return;
        }
        synchronized (futuresToMonitor) {
            /*
             * Avoid a race with monitorFutureProxy(FutureProxy)
             */
            ConcurrentLinkedQueue<FutureProxy> futures = futuresToMonitor.get(updaterId);
            if (futures != null) {
                futures.remove(fp);
                if (futures.isEmpty()) {
                    futuresToMonitor.remove(updaterId);
                    nodeUrls.remove(updaterId);
                }
            }
        }
    }

    public static void monitorFutureProxy(FutureProxy fp) {
        if (fp.isAvailable()) {
            return;
        }
        ActiveObjectLocationInfo info = getUpdaterLocationInfo(fp);
        UniqueID updaterId = info.getBodyId();
        if (updaterId == null) {
            return;
        }
        String nodeUrl = info.getNodeUrl();
        synchronized (futuresToMonitor) {
            /*
             * Avoid a race with the suppression in the ConcurrentHashMap when the
             * ConcurrentLinkedQueue is empty.
             */
            ConcurrentLinkedQueue<FutureProxy> futures = futuresToMonitor.get(updaterId);
            if (futures == null) {
                futures = new ConcurrentLinkedQueue<FutureProxy>();
                futuresToMonitor.put(updaterId, futures);
                nodeUrls.put(updaterId, nodeUrl);
            }
            if (!futures.contains(fp)) {
                futures.add(fp);
            }
        }
    }
}
