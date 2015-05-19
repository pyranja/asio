/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
