package edu.wisc.ssec.mcidasv.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

public class XPathUtils {

    private static final Map<String, XPathExpression> pathMap = new HashMap<String, XPathExpression>();

    private XPathUtils() {}

    public static XPathExpression expr(String xPath) {
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

    public static NodeList eval(final String xmlFile, final String xPath) {
        try {
            return (NodeList)expr(xPath).evaluate(loadXml(xmlFile), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluation xpath", e);
        }
    }

    public static NodeList eval(final Node root, final String xPath) {
        try {
            return (NodeList)expr(xPath).evaluate(root, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluation xpath", e);
        }
    }

    public static NodeListIterator nodes(final String xmlFile, final String xPath) {
        return new NodeListIterator(eval(xmlFile, xPath));
    }

    public static NodeListIterator nodes(final Node root) {
        return new NodeListIterator(eval(root, "//*"));
    }

    public static ElementListIterator elements(final String xmlFile, final String xPath) {
        return new ElementListIterator(eval(xmlFile, xPath));
    }

    public static ElementListIterator elements(final Node root) {
        return new ElementListIterator(eval(root, "//*"));
    }

    public static Document loadXml(final String xmlFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Error loading XML file", e);
        }
    }

    public static class NodeListIterator implements Iterable<Node>, Iterator<Node> {
        private final NodeList nodeList;
        private int index = 0;

        public NodeListIterator(final NodeList nodeList) {
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
