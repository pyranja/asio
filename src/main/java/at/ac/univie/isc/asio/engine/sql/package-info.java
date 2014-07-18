/**
 * JAXB directives to generate top level namespace prefix declarations.
 */
@XmlSchema(namespace = "http://isc.univie.ac.at/2014/asio",
    xmlns = {
        @XmlNs(prefix = "", namespaceURI = "http://isc.univie.ac.at/2014/asio"),
        @XmlNs(prefix = "xsd", namespaceURI = XMLConstants.W3C_XML_SCHEMA_NS_URI),
        @XmlNs(prefix = "xsi", namespaceURI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
    },
    elementFormDefault = XmlNsForm.QUALIFIED,
    attributeFormDefault = XmlNsForm.UNQUALIFIED)
package at.ac.univie.isc.asio.engine.sql;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
