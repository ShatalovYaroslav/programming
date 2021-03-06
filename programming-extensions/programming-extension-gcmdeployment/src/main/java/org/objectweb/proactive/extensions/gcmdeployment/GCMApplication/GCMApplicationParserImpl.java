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
package org.objectweb.proactive.extensions.gcmdeployment.GCMApplication;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.extensions.dataspaces.core.InputOutputSpaceConfiguration;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extensions.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extensions.gcmdeployment.Helpers;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.ApplicationParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.ApplicationParserExecutable;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.ApplicationParserExecutableMPI;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.ApplicationParserProactive;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.CommandBuilder;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptorImpl;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptorParams;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeImpl;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeInternal;
import org.objectweb.proactive.extensions.gcmdeployment.environment.Environment;
import org.objectweb.proactive.gcmdeployment.GCMVirtualNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/*
 * TODO: Write some comment to explain how it works
 * 
 * Exceptions are not catch but always thrown to the caller. If an error occurs, we want to abort the
 * parsing in progress, wrap the Exception inside a ProActiveException and give it to the user.
 */
public class GCMApplicationParserImpl implements GCMApplicationParser {
    private static final String OLD_DESCRIPTOR_SCHEMA = "http://www-sop.inria.fr/oasis/proactive/schema/3.2/DescriptorSchema.xsd";
    private static final String XPATH_GCMAPP = "/app:GCMApplication/";
    private static final String XPATH_VIRTUAL_NODE = XPATH_GCMAPP +
        "app:application/app:proactive/app:virtualNode";
    private static final String XPATH_NODE_PROVIDERS = XPATH_GCMAPP + "app:resources/app:nodeProvider";
    private static final String XPATH_APPLICATION = XPATH_GCMAPP + "app:application";
    private static final String XPATH_NODE_PROVIDER = "app:nodeProvider";
    private static final String XPATH_TECHNICAL_SERVICES = "app:technicalServices";
    private static final String XPATH_FILE = "app:file";

    private static final String[] SUPPORTED_PROTOCOLS = { "file", "http", "http", "https", "jar", "ftp" };
    public static final String ATTR_RP_CAPACITY = "capacity";
    protected URL descriptor;
    protected VariableContractImpl vContract;

    protected Document document;
    protected DocumentBuilderFactory domFactory;
    protected XPath xpath;

    protected List<String> schemas;
    protected CommandBuilder commandBuilder;
    protected Map<String, NodeProvider> nodeProvidersMap;
    protected Map<String, GCMVirtualNodeInternal> virtualNodes;
    protected Map<String, ApplicationParser> applicationParsersMap;
    protected TechnicalServicesProperties appTechnicalServices;
    protected boolean dataSpacesEnabled;
    protected Set<InputOutputSpaceConfiguration> inputOutputSpacesConfigurations;
    protected String dataSpacesNamingServiceURL;

    public GCMApplicationParserImpl(URL descriptor, VariableContractImpl vContract) throws Exception {
        this(descriptor, vContract, null);
    }

    public GCMApplicationParserImpl(URL descriptor, VariableContractImpl vContract, List<String> userSchemas)
            throws Exception {
        this.descriptor = descriptor;
        this.vContract = vContract;
        this.appTechnicalServices = TechnicalServicesProperties.EMPTY;

        this.nodeProvidersMap = null;
        this.virtualNodes = null;
        this.schemas = (userSchemas != null) ? new ArrayList<String>(userSchemas) : new ArrayList<String>();
        this.applicationParsersMap = new HashMap<String, ApplicationParser>();

        registerDefaultApplicationParsers();
        registerUserApplicationParsers();

        setupJAXP();

        try {
            InputSource processedInputSource = Environment.replaceVariables(descriptor, vContract, xpath,
                    GCM_APPLICATION_NAMESPACE_PREFIX);
            DocumentBuilder documentBuilder = GCMParserHelper.getNewDocumentBuilder(domFactory);
            document = documentBuilder.parse(processedInputSource);

            // sanity check : make sure there isn't a ref to an old schema in the document
            //
            String noNamespaceSchema = document.getDocumentElement().getAttribute(
                    "xsi:noNamespaceSchemaLocation");
            if (noNamespaceSchema != null && noNamespaceSchema.contains(OLD_DESCRIPTOR_SCHEMA)) {
                throw new SAXException("Trying to parse an old descriptor");
            }
        } catch (SAXException e) {
            String msg = "parsing problem with document " + descriptor.toExternalForm();
            throw new SAXException(msg, e);
        } catch (TransformerException e) {
            String msg = "problem when evaluating variables with document " + descriptor.toExternalForm();
            throw new TransformerException(msg, e);
        } catch (XPathExpressionException e) {
            throw e;
        }

    }

    /**
     * override me
     */
    protected void registerUserApplicationParsers() {
    }

    public void registerApplicationParser(ApplicationParser applicationParser) {
        applicationParsersMap.put(applicationParser.getNodeName(), applicationParser);
    }

    private void registerDefaultApplicationParsers() {
        registerApplicationParser(new ApplicationParserProactive());
        registerApplicationParser(new ApplicationParserExecutable());
        registerApplicationParser(new ApplicationParserExecutableMPI());
    }

    public void setupJAXP() throws IOException, ParserConfigurationException, SAXException {
        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        domFactory.setIgnoringComments(true);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        URL applicationSchema = getClass().getResource(APPLICATION_DESC_LOCATION);

        schemas.add(0, applicationSchema.toString());
        Source[] schemaSources = new Source[schemas.size()];

        int idx = 0;
        for (String s : schemas) {
            schemaSources[idx++] = new StreamSource(s);
        }

        Schema extensionSchema = schemaFactory.newSchema(schemaSources);

        domFactory.setSchema(extensionSchema);

        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        xpath.setNamespaceContext(new GCMParserHelper.ProActiveNamespaceContext());
    }

    synchronized public Map<String, NodeProvider> getNodeProviders() throws Exception {
        if (nodeProvidersMap != null) {
            return nodeProvidersMap;
        }

        nodeProvidersMap = new HashMap<String, NodeProvider>();

        NodeList nodeProviderNodes;

        nodeProviderNodes = (NodeList) xpath.evaluate(XPATH_NODE_PROVIDERS, document, XPathConstants.NODESET);

        for (int i = 0; i < nodeProviderNodes.getLength(); ++i) {
            Node nodeProviderNode = nodeProviderNodes.item(i);

            String id = GCMParserHelper.getAttributeValue(nodeProviderNode, "id");
            NodeProvider nodeProvider = new NodeProvider(id);

            NodeList gcmdNodes;
            gcmdNodes = (NodeList) xpath.evaluate(XPATH_FILE, nodeProviderNode, XPathConstants.NODESET);
            for (int j = 0; j < gcmdNodes.getLength(); j++) {
                GCMDeploymentDescriptorParams gcmdParams = new GCMDeploymentDescriptorParams();
                gcmdParams.setId(id);
                String path = GCMParserHelper.getAttributeValue(gcmdNodes.item(j), "path");

                URL fullURL = null;

                // We determine wether we have a Path or a URL
                boolean schemeFound = false;
                String protocolFound = null;
                if (path.indexOf(':') >= 0) {
                    for (String scheme : SUPPORTED_PROTOCOLS) {
                        if (path.toLowerCase().startsWith(scheme + ":")) {
                            schemeFound = true;
                            protocolFound = scheme;
                            break;
                        }
                    }
                }

                if (schemeFound && !protocolFound.equals("file")) {
                    // In case we have an url other than file:
                    fullURL = new URL(path);
                } else {

                    // in case we have a filepath or a url starting with file:

                    if (schemeFound) {
                        // if it's an url starting with file: we remove the protocol
                        URL urlWithFile = new URL(path);
                        if (urlWithFile.getHost().length() > 1) {
                            // the host can include the Windows drive letter (in that case it is of size 1)
                            throw new IOException(
                                urlWithFile +
                                    " is using the form <host>/<path> which is not supported. Other possibility is that the url is of the form file:/<path> and it should be file://<path>");
                        } else if (urlWithFile.getHost().length() == 1) {
                            path = urlWithFile.getHost() + ":" + "/" + urlWithFile.getPath();
                        } else {
                            path = urlWithFile.getPath();
                        }
                    }
                    File file = new File(path);

                    // If this path is absolute, no problem
                    if (file.isAbsolute()) {
                        fullURL = Helpers.fileToURL(file);
                    } else if (descriptor.getProtocol().equals("jar")) {
                        // If this File path is relative and the base descriptor URL protocol is jar,
                        // we need to handle ourselves how we resolve the relative path against the jar
                        JarURLConnection jconn = (JarURLConnection) descriptor.openConnection();
                        URI base = new URI(jconn.getEntryName());
                        URI resolved = base.resolve(new URI(file.getPath().replace("\\", "/")));
                        fullURL = new URL("jar:" + jconn.getJarFileURL().toExternalForm() + "!/" + resolved);
                    } else if (descriptor.toURI().isOpaque()) {
                        // This is very unlikely, but : ff this path is relative and the base url is not hierarchical (and differs from jar)
                        // we just can't handle it
                        throw new IOException(descriptor.toExternalForm() +
                            " is not a hierarchical uri and can't be resolved against the relative path " +
                            path);
                    } else {
                        // We can handle the last case by using URI resolve method
                        URI uriDescriptor = descriptor.toURI();

                        // This ugly code is here for the following reasons:
                        // 1) We need to escape illegal characters which appear in File paths
                        // 2) The toURI() method returns an absolute path and here the File path is relative, so it will prepent to the path the current directory
                        // 3) we need to remove this prepended directory to have the final relative path as an relative URI
                        File basef = new File("");
                        URI messedup = file.toURI();
                        URI baseuri = basef.toURI();
                        String cleaner = messedup.toString().substring(baseuri.toString().length());
                        URI cleaneruri = new URI(cleaner);

                        if (cleaneruri.isAbsolute()) {
                            throw new IOException("Internal error: " + cleaneruri +
                                " is absolute and should be relative");
                        }
                        // now that we have a clean relative, we can resolve it against the base url
                        URI fullUri = uriDescriptor.resolve(cleaneruri);
                        fullURL = fullUri.toURL();
                    }
                }

                gcmdParams.setGCMDescriptor(fullURL);
                gcmdParams.setVContract(vContract);

                GCMDeploymentDescriptor gcmd = new GCMDeploymentDescriptorImpl(fullURL, vContract);
                nodeProvider.addGCMDeploymentDescriptor(gcmd);
            }

            // get fileTransfers
            /*
             * HashSet<FileTransferBlock> fileTransferBlocks = new HashSet<FileTransferBlock>();
             * NodeList fileTransferNodes = (NodeList) xpath.evaluate(XPATH_FILETRANSFER, node,
             * XPathConstants.NODESET); for (int j = 0; j < fileTransferNodes.getLength(); ++j) {
             * Node fileTransferNode = fileTransferNodes.item(j); FileTransferBlock
             * fileTransferBlock = GCMParserHelper.parseFileTransferNode(fileTransferNode);
             * fileTransferBlocks.add(fileTransferBlock); }
             */
            nodeProvidersMap.put(nodeProvider.getId(), nodeProvider);
        }

        return nodeProvidersMap;
    }

    public CommandBuilder getCommandBuilder() throws Exception {
        if (commandBuilder != null) {
            return commandBuilder;
        }

        Node applicationNode = (Node) xpath.evaluate(XPATH_APPLICATION, document, XPathConstants.NODE);

        NodeList appNodes = applicationNode.getChildNodes();

        for (int i = 0; i < appNodes.getLength(); ++i) {
            Node commandNode = appNodes.item(i);
            if (commandNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            ApplicationParser applicationParser = getApplicationParserForNode(commandNode);
            if (applicationParser == null) {
                GCMDeploymentLoggers.GCMA_LOGGER.warn("No application parser registered for node <" +
                    commandNode.getNodeName() + ">");
            } else {
                applicationParser.parseApplicationNode(commandNode, this, xpath);
                commandBuilder = applicationParser.getCommandBuilder();
                appTechnicalServices = applicationParser.getTechnicalServicesProperties();
            }
        }

        return commandBuilder;
    }

    private ApplicationParser getApplicationParserForNode(Node commandNode) {
        ApplicationParser applicationParser = applicationParsersMap.get(commandNode.getNodeName());
        return applicationParser;
    }

    synchronized public Map<String, GCMVirtualNodeInternal> getVirtualNodes() throws Exception {
        if (virtualNodes != null) {
            return virtualNodes;
        }

        virtualNodes = new HashMap<String, GCMVirtualNodeInternal>();

        // make sure these are parsed
        getCommandBuilder();
        getNodeProviders();

        NodeList nodes = (NodeList) xpath.evaluate(XPATH_VIRTUAL_NODE, document, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); ++i) {
            Node xmlNode = nodes.item(i);

            // get Id
            //
            GCMVirtualNodeImpl virtualNode = new GCMVirtualNodeImpl(appTechnicalServices);

            String id = GCMParserHelper.getAttributeValue(xmlNode, "id");
            virtualNode.setName(id);

            // get capacity
            //
            String capacity = GCMParserHelper.getAttributeValue(xmlNode, ATTR_RP_CAPACITY);

            virtualNode.setCapacity(capacityAsLong(capacity));

            // get technical services (if any)
            //
            Node techServices = (Node) xpath.evaluate(XPATH_TECHNICAL_SERVICES, xmlNode, XPathConstants.NODE);
            if (techServices != null) {
                TechnicalServicesProperties vnodeTechnicalServices = GCMParserHelper
                        .parseTechnicalServicesNode(xpath, techServices);
                virtualNode.setTechnicalServicesProperties(vnodeTechnicalServices);
            }

            // get resource providers references
            //
            NodeList nodeProviderNodes = (NodeList) xpath.evaluate(XPATH_NODE_PROVIDER, xmlNode,
                    XPathConstants.NODESET);
            if (nodeProviderNodes.getLength() == 0) {
                // Add all the Node Providers to this Virtual Node
                for (NodeProvider nodeProvider : NodeProvider.getAllNodeProviders()) {
                    virtualNode.addNodeProviderContract(nodeProvider, TechnicalServicesProperties.EMPTY,
                            GCMVirtualNode.MAX_CAPACITY);
                }
            } else {
                for (int j = 0; j < nodeProviderNodes.getLength(); j++) {
                    Node nodeProv = nodeProviderNodes.item(j);

                    String refId = GCMParserHelper.getAttributeValue(nodeProv, "refid");
                    capacity = GCMParserHelper.getAttributeValue(nodeProv, ATTR_RP_CAPACITY);

                    NodeProvider nodeProvider = nodeProvidersMap.get(refId);

                    Node nodeProviderTechServices = (Node) xpath.evaluate(XPATH_TECHNICAL_SERVICES, nodeProv,
                            XPathConstants.NODE);
                    TechnicalServicesProperties nodeProviderTechServicesProperties = TechnicalServicesProperties.EMPTY;
                    if (nodeProviderTechServices != null) {
                        nodeProviderTechServicesProperties = GCMParserHelper.parseTechnicalServicesNode(
                                xpath, nodeProviderTechServices);
                        nodeProvider.setTechnicalServicesProperties(nodeProviderTechServicesProperties);
                    }

                    virtualNode.addNodeProviderContract(nodeProvider, nodeProviderTechServicesProperties,
                            capacityAsLong(capacity));

                }
            }

            virtualNodes.put(virtualNode.getName(), virtualNode);
        }

        return virtualNodes;
    }

    static private long capacityAsLong(String capacity) throws NumberFormatException {
        if (capacity == null) {
            return GCMVirtualNode.MAX_CAPACITY;
        }

        try {
            return Long.parseLong(capacity);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(capacity +
                " is an invalid value for a capacity (should have been checked by the XSD)");
        }
    }

    public TechnicalServicesProperties getAppTechnicalServices() {
        return appTechnicalServices;
    }

    public boolean isDataSpacesEnabled() {
        return dataSpacesEnabled;
    }

    public void setDataSpacesEnabled(boolean dataSpacesEnabled) {
        this.dataSpacesEnabled = dataSpacesEnabled;
    }

    public String getDataSpacesNamingServiceURL() {
        return dataSpacesNamingServiceURL;
    }

    public void setDataSpacesNamingServiceURL(String dataSpacesNamingServiceURL) {
        this.dataSpacesNamingServiceURL = dataSpacesNamingServiceURL;
    }

    public Set<InputOutputSpaceConfiguration> getInputOutputSpacesConfigurations() {
        return inputOutputSpacesConfigurations;
    }

    public void setInputOutputSpacesConfigurations(
            Set<InputOutputSpaceConfiguration> inputOutputSpacesConfigurations) {
        this.inputOutputSpacesConfigurations = inputOutputSpacesConfigurations;
    }
}
