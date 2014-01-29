/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.idv.IdvResourceManager.XmlIdvResource;
import ucar.unidata.util.ResourceCollection.Resource;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.util.Contract;

/**
 * Documentation is still forthcoming, but remember that <b>no methods accept 
 * {@code null} parameters!</b>
 */
public final class XPathUtils {

    /** Maps (and caches) the XPath {@link String} to its compiled {@link XPathExpression}. */
    private static final Map<String, XPathExpression> pathMap = new ConcurrentHashMap<String, XPathExpression>();

    /**
     * Thou shalt not create an instantiation of this class!
     */
    private XPathUtils() {}

    public static XPathExpression expr(String xPath) {
        Contract.notNull(xPath, "Cannot compile a null string");

        XPathExpression expr = pathMap.get(xPath);
        if (expr == null) {
            try {
                expr = XPathFactory.newInstance().newXPath().compile(xPath);
                pathMap.put(xPath, expr);
            } catch (XPathExpressionException e) {
                throw new RuntimeException("Error compiling xpath", e);
            }
        }
        return expr;
    }

    public static List<Node> eval(final XmlResourceCollection collection, final String xPath) {
        Contract.notNull(collection, "Cannot search a null resource collection");
        Contract.notNull(xPath, "Cannot search using a null XPath query");

        try {
            List<Node> nodeList = new ArrayList<Node>();
            XPathExpression expression = expr(xPath);

            // Resources are the only things added to the list returned by 
            // getResources().
            @SuppressWarnings("unchecked")
            List<Resource> files = collection.getResources();

            for (int i = 0; i < files.size(); i++) {
                if (!collection.isValid(i))
                    continue;

                InputStream in = XPathUtils.class.getResourceAsStream(files.get(i).toString());
                if (in == null)
                    continue;

                NodeList tmpList = (NodeList)expression.evaluate(loadXml(in), XPathConstants.NODESET);
                for (int j = 0; j < tmpList.getLength(); j++) {
                    nodeList.add(tmpList.item(j));
                }
            }
            return nodeList;
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluating xpath", e);
        }
    }

    public static NodeList eval(final String xmlFile, final String xPath) {
        Contract.notNull(xmlFile, "Null path to a XML file");
        Contract.notNull(xPath, "Cannot search using a null XPath query");

        try {
            return (NodeList)expr(xPath).evaluate(loadXml(xmlFile), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluation xpath", e);
        }
    }

    public static NodeList eval(final Node root, final String xPath) {
        Contract.notNull(root, "Cannot search a null root node");
        Contract.notNull(xPath, "Cannot search using a null XPath query");

        try {
            return (NodeList)expr(xPath).evaluate(root, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluation xpath", e);
        }
    }

    public static List<Node> nodes(final IntegratedDataViewer idv, final IdvResource collectionId, final String xPath) {
        Contract.notNull(idv);
        Contract.notNull(collectionId);
        Contract.notNull(xPath);

        XmlResourceCollection collection = idv.getResourceManager().getXmlResources(collectionId);
        return nodes(collection, xPath);
    }

    public static List<Node> nodes(final XmlResourceCollection collection, final String xPath) {
        Contract.notNull(collection);
        Contract.notNull(xPath);
        return eval(collection, xPath);
    }

    public static NodeListIterator nodes(final String xmlFile, final String xPath) {
        Contract.notNull(xmlFile);
        Contract.notNull(xPath);
        return new NodeListIterator(eval(xmlFile, xPath));
    }

    public static NodeListIterator nodes(final Node root, final String xPath) {
        Contract.notNull(root);
        Contract.notNull(xPath);
        return new NodeListIterator(eval(root, xPath));
    }

    public static NodeListIterator nodes(final Node root) {
        Contract.notNull(root);
        return nodes(root, "//*");
    }

    public static List<Element> elements(final IntegratedDataViewer idv, final IdvResource collectionId, final String xPath) {
        Contract.notNull(idv);
        Contract.notNull(collectionId);
        Contract.notNull(xPath);

        XmlResourceCollection collection = idv.getResourceManager().getXmlResources(collectionId);
        return elements(collection, xPath);
    }

    public static List<Element> elements(final XmlResourceCollection collection, final String xPath) {
        Contract.notNull(collection);
        Contract.notNull(xPath);
        List<Element> elements = new ArrayList<Element>();
        for (Node n : eval(collection, xPath))
            elements.add((Element)n);
        return elements;
    }

    public static ElementListIterator elements(final String xmlFile, final String xPath) {
        Contract.notNull(xmlFile);
        Contract.notNull(xPath);
        return new ElementListIterator(eval(xmlFile, xPath));
    }

    public static ElementListIterator elements(final Node root) {
        Contract.notNull(root);
        return elements(root, "//*");
    }

    public static ElementListIterator elements(final Node root, final String xPath) {
        Contract.notNull(root);
        Contract.notNull(xPath);
        return new ElementListIterator(eval(root, xPath));
    }

    public static Document loadXml(final String xmlFile) {
        Contract.notNull(xmlFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file: "+e.getMessage(), e);
        }
    }

    public static Document loadXml(final File xmlFile) {
        Contract.notNull(xmlFile);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file: "+e.getMessage(), e);
        }
    }

    public static Document loadXml(final InputStream in) {
        Contract.notNull(in);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML from input stream: "+e.getMessage(), e);
        }
    }

    public static class NodeListIterator implements Iterable<Node>, Iterator<Node> {
        private final NodeList nodeList;
        private int index = 0;

        public NodeListIterator(final NodeList nodeList) {
            Contract.notNull(nodeList);
            this.nodeList = nodeList;
        }

        public Iterator<Node> iterator() {
            return this;
        }

        public boolean hasNext() {
            return (index < nodeList.getLength());
        }

        public Node next() {
            return nodeList.item(index++);
        }

        public void remove() {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public static class ElementListIterator implements Iterable<Element>, Iterator<Element> {
        private final NodeList nodeList;
        private int index = 0;

        public ElementListIterator(final NodeList nodeList) {
            Contract.notNull(nodeList);
            this.nodeList = nodeList;
        }

        public Iterator<Element> iterator() {
            return this;
        }

        public boolean hasNext() {
            return (index < nodeList.getLength());
        }

        public Element next() {
            return (Element)nodeList.item(index++);
        }

        public void remove() {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
