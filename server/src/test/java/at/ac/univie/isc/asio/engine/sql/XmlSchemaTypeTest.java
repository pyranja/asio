package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.tool.Pretty;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class XmlSchemaTypeTest {

  @DataPoints
  public static XmlSchemaType[] types() {
    return XmlSchemaType.values();
  }

  @Theory
  public void qualified_name_has_xml_schema_namespace(final XmlSchemaType type) {
    assertThat(type.qname().getNamespaceURI(), is("http://www.w3.org/2001/XMLSchema#"));
  }

  @Theory
  public void datatype_name_is_appended_as_fragment_in_qualified_name(final XmlSchemaType type) {
    assertThat(Pretty.expand(type.qname()), startsWith("http://www.w3.org/2001/XMLSchema#"));
  }
}
