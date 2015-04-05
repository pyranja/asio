package at.ac.univie.isc.asio.engine.sql;

import com.google.common.collect.ImmutableMap;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Map;

/**
 * Supported XML schema types.
 * <p>
 *   Note : The XML schema definition uses the namespace {@code http://www.w3.org/2001/XMLSchema}
 *   for data types, but to reference a datatype the name must be appended as fragment. As this
 *   cannot be represented by QName, this class appends {@code '#'} to the XML schema namespace URI.
 *   This compromise ensures, that expanded QNames are valid URI references and at the same time
 *   have the correct XML schema namespaceURI. This corresponds with the usage of XML datatype URIs
 *   in RDF.
 * </p>
 *
 * @see <a href='http://www.w3.org/TR/xmlschema-2/#namespaces'>XML Schema - part 2</a>
 * @see <a href='http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-datatyped-literals'>RDF syntax</a>
 */
public enum XmlSchemaType {
  BOOLEAN("boolean"),
  STRING("string"),
  DATETIME("dateTime"),
  DATE("date"),
  TIME("time"),
  LONG("long"),
  DECIMAL("decimal"),
  DOUBLE("double"),
  BASE64BINARY("base64Binary"),
  NIL("nil");

  public static final String XML_DATATYPE_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

  private final QName qname;

  public QName qname() {
    return qname;
  }

  XmlSchemaType(final String xmlType) {
    assert xmlType != null;
    this.qname = constructXmlDatatypeName(xmlType);
  }

  private QName constructXmlDatatypeName(final String xmlType) {
    return new QName(XML_DATATYPE_NAMESPACE, xmlType);
  }

  private static final Map<Class<?>, XmlSchemaType> JAVA_TYPE_LOOKUP =
      ImmutableMap.<Class<?>, XmlSchemaType>builder()
          .put(Boolean.class, BOOLEAN)
          .put(String.class, STRING)
          .put(Byte.class, LONG).put(Short.class, LONG).put(Integer.class, LONG).put(Long.class, LONG)
          .put(BigInteger.class, DECIMAL).put(BigDecimal.class, DECIMAL)
          .put(Double.class, DOUBLE).put(Float.class, DOUBLE)
          .put(java.sql.Date.class, DATE)
          .put(java.sql.Time.class, TIME)
          .put(java.sql.Timestamp.class, DATETIME).put(java.util.Date.class, DATETIME)
          .put(byte[].class, BASE64BINARY)
          .build();

  public static XmlSchemaType fromJavaType(final Class<?> clazz) {
    final XmlSchemaType xsdType = JAVA_TYPE_LOOKUP.get(clazz);
    return xsdType == null ? NIL : xsdType;
  }

  private static final Map<Integer, XmlSchemaType> SQL_TYPE_LOOKUP =
      ImmutableMap.<Integer, XmlSchemaType>builder()
          // boolean
          .put(Types.BIT, BOOLEAN).put(Types.BOOLEAN, BOOLEAN)
          // string
          .put(Types.VARCHAR, STRING).put(Types.CHAR, STRING)
          .put(Types.LONGVARCHAR, STRING).put(Types.CLOB, STRING)
          .put(Types.NVARCHAR, STRING).put(Types.NCHAR, STRING)
          .put(Types.LONGNVARCHAR, STRING).put(Types.NCLOB, STRING)
          // integral
          .put(Types.TINYINT, LONG).put(Types.SMALLINT, LONG)
          .put(Types.INTEGER, LONG).put(Types.BIGINT, LONG)
          // decimal
          .put(Types.NUMERIC, DECIMAL).put(Types.DECIMAL, DECIMAL)
              // double
          .put(Types.DOUBLE, DOUBLE).put(Types.FLOAT, DOUBLE)
          .put(Types.REAL, DOUBLE)
              // temporal
          .put(Types.DATE, DATE).put(Types.TIME, TIME)
          .put(Types.TIMESTAMP, DATETIME)
              // binary
          .put(Types.BINARY, BASE64BINARY).put(Types.VARBINARY, BASE64BINARY)
          .put(Types.LONGVARBINARY, BASE64BINARY).put(Types.BLOB, BASE64BINARY)
          .build();

  public static XmlSchemaType fromSqlType(final int typeId) {
    final XmlSchemaType sqlType = SQL_TYPE_LOOKUP.get(typeId);
    return sqlType == null ? NIL : sqlType;
  }
}
