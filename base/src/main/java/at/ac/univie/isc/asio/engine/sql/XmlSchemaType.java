package at.ac.univie.isc.asio.engine.sql;

import com.google.common.collect.ImmutableMap;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * supported XML Schema datatypes
 */
public enum XmlSchemaType {
  BOOLEAN(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "boolean"), SQLDataType.BOOLEAN),
  STRING(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "string"), SQLDataType.VARCHAR),
  DATETIME(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "dateTime"), SQLDataType.TIMESTAMP),
  DATE(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "date"), SQLDataType.DATE),
  TIME(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "time"), SQLDataType.TIME),
  LONG(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "long"), SQLDataType.BIGINT),
  DECIMAL(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "decimal"), SQLDataType.DECIMAL),
  DOUBLE(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "double"), SQLDataType.DOUBLE),
  BASE64BINARY(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "base64Binary"), SQLDataType.VARBINARY),
  NIL(new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil"), SQLDataType.OTHER);

  private final QName qname;
  private final DataType<?> sqlType;

  public QName qname() {
    return qname;
  }

  public DataType<?> sqlType() {
    return sqlType;
  }

  public Class<?> javaType() {
    return sqlType.getType();
  }

  XmlSchemaType(final QName qname, final DataType<?> sqlType) {
    assert qname != null;
    this.qname = qname;
    assert sqlType != null;
    this.sqlType = sqlType;
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
    if(xsdType == null) {
      return NIL;
    } else {
      return xsdType;
    }
  }

  private static final Map<DataType<?>, XmlSchemaType> SQL_LOOKUP =
      ImmutableMap.<DataType<?>, XmlSchemaType>builder()
      // boolean
      .put(SQLDataType.BIT, BOOLEAN).put(SQLDataType.BOOLEAN, BOOLEAN)
      // string
      .put(SQLDataType.VARCHAR, STRING).put(SQLDataType.CHAR, STRING)
      .put(SQLDataType.LONGVARCHAR, STRING).put(SQLDataType.CLOB, STRING)
      .put(SQLDataType.NVARCHAR, STRING).put(SQLDataType.NCHAR, STRING)
      .put(SQLDataType.LONGNVARCHAR, STRING).put(SQLDataType.NCLOB, STRING)
      // integral
      .put(SQLDataType.TINYINT, LONG).put(SQLDataType.SMALLINT, LONG)
      .put(SQLDataType.INTEGER, LONG).put(SQLDataType.BIGINT, LONG)
      .put(SQLDataType.TINYINTUNSIGNED, LONG).put(SQLDataType.SMALLINTUNSIGNED, LONG)
      .put(SQLDataType.INTEGERUNSIGNED, LONG)
      // decimal
      .put(SQLDataType.DECIMAL_INTEGER, DECIMAL).put(SQLDataType.BIGINTUNSIGNED, DECIMAL)
      .put(SQLDataType.NUMERIC, DECIMAL).put(SQLDataType.DECIMAL, DECIMAL)
      // double
      .put(SQLDataType.DOUBLE, DOUBLE).put(SQLDataType.FLOAT, DOUBLE).put(SQLDataType.REAL, DOUBLE)
      // temporal
      .put(SQLDataType.DATE, DATE).put(SQLDataType.TIME, TIME).put(SQLDataType.TIMESTAMP, DATETIME)
      // binary
      .put(SQLDataType.BINARY, BASE64BINARY).put(SQLDataType.VARBINARY, BASE64BINARY)
      .put(SQLDataType.LONGVARBINARY, BASE64BINARY).put(SQLDataType.BLOB, BASE64BINARY)
      .build();

  public static XmlSchemaType fromSqlType(final DataType sqlType) {
    final XmlSchemaType xsdType = SQL_LOOKUP.get(sqlType);
    if (xsdType == null) {
      return NIL;
    } else {
      return xsdType;
    }
  }
}
