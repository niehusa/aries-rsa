/** 
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements. See the NOTICE file 
 * distributed with this work for additional information 
 * regarding copyright ownership. The ASF licenses this file 
 * to you under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations 
 * under the License. 
 */
package org.apache.cxf.dosgi.discovery.local;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.osgi.framework.Bundle;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class LocalDiscoveryUtilsTest extends TestCase {
    public void testNoRemoteServicesXMLFiles() {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(0, rsElements.size());        
    }
    
    public void testRemoteServicesXMLFiles() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(rs1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(2, rsElements.size());
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/sd/v1.0.0");
        assertEquals("SomeService", rsElements.get(0).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("SomeOtherService", rsElements.get(1).getChild("provide", ns).getAttributeValue("interface"));
    }
    
    public void testMultiRemoteServicesXMLFiles() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        URL rs2URL = getClass().getResource("/rs2.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(rs1URL, rs2URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(3, rsElements.size());
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/sd/v1.0.0");
        assertEquals("SomeService", rsElements.get(0).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("SomeOtherService", rsElements.get(1).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("org.example.Service", rsElements.get(2).getChild("provide", ns).getAttributeValue("interface"));
    }
    
    @SuppressWarnings("unchecked")
    public void testRemoteServicesXMLFileAlternateLocation() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        Dictionary headers = new Hashtable();        
        headers.put("Remote-Service", "META-INF/osgi/");
        headers.put("Bundle-Name", "testing bundle");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getHeaders()).andReturn(headers).anyTimes();
        EasyMock.expect(b.findEntries(
            EasyMock.eq("META-INF/osgi"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(rs1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> rsElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(2, rsElements.size());
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/sd/v1.0.0");
        assertEquals("SomeService", rsElements.get(0).getChild("provide", ns).getAttributeValue("interface"));
        assertEquals("SomeOtherService", rsElements.get(1).getChild("provide", ns).getAttributeValue("interface"));
    }
    
    /* public void testAllRemoteReferences() {
        URL rs1URL = getClass().getResource("/rs1.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                andReturn(Collections.enumeration(Arrays.asList(rs1URL)));
        EasyMock.replay(b);
        
        List<ServiceEndpointDescription> seds = LocalDiscoveryUtils.getAllRemoteReferences(b);
        assertEquals(2, seds.size());
        Map<Collection<String>, String> eids = getEndpointIDs(seds);
        
        List<String> interfaces = Arrays.asList("SomeService");
        Map<String, Object> sed1Props = new HashMap<String, Object>();
        sed1Props.put("osgi.remote.requires.intents", "confidentiality");
        sed1Props.put(ServicePublication.ENDPOINT_ID, eids.get(
                Collections.singleton("SomeService")));
        sed1Props.put(ServicePublication.SERVICE_INTERFACE_NAME, interfaces);
        ServiceEndpointDescription sed1 = 
            new ServiceEndpointDescriptionImpl(interfaces, sed1Props);

        List<String> interfaces2 = Arrays.asList("SomeOtherService", "WithSomeSecondInterface");
        Map<String, Object> sed2Props = new HashMap<String, Object>();
        sed2Props.put(ServicePublication.ENDPOINT_ID, eids.get(
                new HashSet<String>(interfaces2)));
        sed2Props.put(ServicePublication.SERVICE_INTERFACE_NAME, interfaces2);
        ServiceEndpointDescription sed2 = 
            new ServiceEndpointDescriptionImpl(interfaces2, sed2Props);
        assertTrue(seds.contains(sed1));
        assertTrue(seds.contains(sed2));
    } 

    @SuppressWarnings("unchecked")
    private Map<Collection<String>, String> getEndpointIDs(
            List<ServiceEndpointDescription> seds) {
        Map<Collection<String>, String> map = new HashMap<Collection<String>, String>();
        
        for (ServiceEndpointDescription sed : seds) {
            map.put((Collection<String>) sed.getProvidedInterfaces(), sed.getEndpointID());
        }
        
        return map;
    } */
    
    public void testEndpointDescriptionXMLFiles() {
        URL ed1URL = getClass().getResource("/ed1.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<Element> edElements = LocalDiscoveryUtils.getAllDescriptionElements(b);
        assertEquals(4, edElements.size());
    }
    
    public void testAllEndpoints1() {
        URL ed1URL = getClass().getResource("/ed1.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<EndpointDescription> eds = LocalDiscoveryUtils.getAllEndpointDescriptions(b);
        assertEquals(4, eds.size());
        EndpointDescription ed0 = eds.get(0);
        assertEquals("http://somewhere:12345", ed0.getRemoteURI());
        assertEquals(Arrays.asList("SomeService"), ed0.getInterfaces());
        assertEquals(Arrays.asList("confidentiality"), 
            ed0.getProperties().get("osgi.remote.requires.intents"));
        assertEquals("testValue", ed0.getProperties().get("testKey"));
        
        EndpointDescription ed1 = eds.get(1);
        assertEquals("myScheme://somewhere:12345", ed1.getRemoteURI());
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), ed1.getInterfaces());
        
        EndpointDescription ed2 = eds.get(2);
        assertEquals("http://somewhere", ed2.getRemoteURI());
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), ed2.getInterfaces());

        EndpointDescription ed3 = eds.get(3);
        assertEquals("http://somewhere:1/2/3/4?5", ed3.getRemoteURI());
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), ed3.getInterfaces());
    }
    
    @SuppressWarnings("unchecked")
    public void testAllEndpoints2() throws Exception {
        URL ed1URL = getClass().getResource("/ed2.xml");
        
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed1URL))).anyTimes();
        EasyMock.replay(b);
        
        List<EndpointDescription> eds = LocalDiscoveryUtils.getAllEndpointDescriptions(b);
        assertEquals(2, eds.size());
        EndpointDescription ed0 = eds.get(0);
        assertEquals("foo:bar", ed0.getRemoteURI());
        assertEquals(Arrays.asList("com.acme.HelloService"), ed0.getInterfaces());
        assertEquals(Arrays.asList("SOAP"), ed0.getIntents());
        assertEquals("org.apache.cxf.ws", ed0.getProperties().get("service.exported.configs"));
        
        EndpointDescription ed1 = eds.get(1);
        Map<String, Object> props = ed1.getProperties();
        assertEquals(Arrays.asList("com.acme.HelloService", "some.other.Service"), ed1.getInterfaces());
        assertFalse("Should not be exactly the same. The value should contain a bunch of newlines", 
            "org.apache.cxf.ws".equals(props.get("service.exported.configs")));
        assertEquals("org.apache.cxf.ws", props.get("service.exported.configs").toString().trim());
        
        assertEquals(normXML("<other:t1 xmlns:other='http://www.acme.org/xmlns/other/v1.0.0'><foo type='bar'>haha</foo></other:t1>"), 
            normXML((String) props.get("someXML")));
        
        assertEquals(Long.MAX_VALUE, props.get("long"));
        assertEquals(new Long(-1), props.get("long2"));
        assertEquals(Double.MAX_VALUE, props.get("double"));
        assertEquals(new Double(1.0d), props.get("Double2"));
        assertEquals(new Float(42.24f), props.get("float"));
        assertEquals(new Float(1.0f), props.get("Float2"));
        assertEquals(new Integer(17), props.get("int"));
        assertEquals(new Integer(42), props.get("Integer2"));
        assertEquals(new Byte((byte) 127), props.get("byte"));
        assertEquals(new Byte((byte) -128), props.get("Byte2"));
        assertEquals(new Boolean(true), props.get("boolean"));
        assertEquals(new Boolean(true), props.get("Boolean2"));
        assertEquals(new Short((short) 99), props.get("short"));
        assertEquals(new Short((short) -99), props.get("Short2"));
        int [] intArray = (int []) props.get("int-array");
        assertTrue(Arrays.equals(new int[] {1, 2}, intArray));
        
        Integer [] integerArray = (Integer []) props.get("Integer-array");
        assertTrue(Arrays.equals(new Integer[] {2, 1}, integerArray));
        
        assertEquals(Arrays.asList(true, false), props.get("bool-list"));
        assertEquals(new HashSet<Object>(), props.get("long-set"));
        assertEquals("Hello", props.get("other1").toString().trim());
        
        List l = (List) props.get("other2");
        assertEquals(1, l.size());
        assertEquals(normXML("<other:t2 xmlns:other='http://www.acme.org/xmlns/other/v1.0.0'/>"),
            normXML((String) l.get(0)));
    }
    
    private static String normXML(String s) throws JDOMException, IOException {
        String s2 = stripComment(s);
        String s3 = stripProlog(s2);
        Document d = new SAXBuilder().build(new ByteArrayInputStream(s3.getBytes()));
        XMLOutputter outputter  = new XMLOutputter(Format.getPrettyFormat());
        return outputter.outputString(d);
    }
    
    private static String stripComment(String s) { 
        return s.replaceAll("<!--(.*?)-->", "");
    }
    
    private static String stripProlog(String s) {
        return s.replaceAll("<\\?(.*?)\\?>", "");
    }         
}
