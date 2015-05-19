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
package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.tool.ValueOrError;
import net.atos.AtosDataset;
import net.atos.AtosLink;
import net.atos.AtosRelatedResource;
import net.atos.AtosSemanticConcept;
import org.junit.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.chrono.ChronoZonedDateTime;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class DescriptorConversionTest {
  /**
   * input dataset with minimal set of property values
   */
  static AtosDataset sample() {
    return new AtosDataset().withGlobalID("44bab742-2c27-4dad-8419-fae8a848358b").withType("Dataset");
  }

  private SchemaDescriptor result;
  private DescriptorConversion.IllegalConversion error;

  @Test
  public void use_globalID_as_identifier() throws Exception {
    result = expectSuccess(sample().withGlobalID("test-id"));
    assertThat(result.getIdentifier(), is("test-id"));
  }

  @Test
  public void use_name_as_label() throws Exception {
    result = expectSuccess(sample().withName("test-name"));
    assertThat(result.getLabel(), is("test-name"));
  }

  @Test
  public void use_globalID_as_label_if_name_missing() throws Exception {
    result = expectSuccess(sample().withGlobalID("test-id").withName(null));
    assertThat(result.getLabel(), is("test-id"));
  }

  @Test
  public void mark_as_active_if_status_is__active__text() throws Exception {
    result = expectSuccess(sample().withStatus("active"));
    assertThat(result.isActive(), is(true));
  }

  @Test
  public void mark_as_inactive_if_status_is__not_active__text() throws Exception {
    result = expectSuccess(sample().withStatus("not active"));
    assertThat(result.isActive(), is(false));
  }

  @Test
  public void mark_as_inactive_if_status_is_unexpected_text() throws Exception {
    result = expectSuccess(sample().withStatus("someText ?"));
    assertThat(result.isActive(), is(false));
  }

  @Test
  public void copy_simple_properties() throws Exception {
    final AtosDataset input = sample()
        .withDescription("test-description")
        .withCategory("test-category")
        .withAuthor("test-author")
        .withLicence("test-license");
    result = expectSuccess(input);
    assertThat("mismatch on property <description>", result.getDescription(), equalTo(input.getDescription()));
    assertThat("mismatch on property <author>", result.getAuthor(), equalTo(input.getAuthor()));
    assertThat("mismatch on property <license>", result.getLicense(), equalTo(input.getLicence()));
    assertThat("mismatch on property <category>", result.getCategory(), equalTo(input.getCategory()));
  }

  @Test
  public void parse_xml_date_formats() throws Exception {
    result = expectSuccess(sample()
            .withCreationDate("2014-04-14 16:49:54.000")
            .withUpdateDate("2014-04-14 17:02:53.000")
    );
    final ZonedDateTime expectedCreationDate =
        ZonedDateTime.of(2014, 4, 14, 16, 49, 54, 0, ZoneOffset.UTC);
    final ZonedDateTime expectedUpdateDate =
        ZonedDateTime.of(2014, 4, 14, 17, 2, 53, 0, ZoneOffset.UTC);
    assertThat("mismatch on property <created>", result.getCreated(), equalTo(expectedCreationDate));
    assertThat("mismatch on property <updated>", result.getUpdated(), equalTo(expectedUpdateDate));
  }

  @Test
  public void replace_unparseable_date_properties_with_now() throws Exception {
    final ChronoZonedDateTime beforeConversion = ZonedDateTime.now(ZoneOffset.UTC);
    result = expectSuccess(sample()
            .withCreationDate("not valid")
            .withUpdateDate("not valid")
    );
    final ChronoZonedDateTime afterConversion = ZonedDateTime.now(ZoneOffset.UTC);
    assertThat("mismatch on property <created>", result.getCreated(),
        is(both(greaterThanOrEqualTo(beforeConversion)).and(lessThanOrEqualTo(afterConversion)))
    );
    assertThat("mismatch on property <updated>", result.getUpdated(),
        is(both(greaterThanOrEqualTo(beforeConversion)).and(lessThanOrEqualTo(afterConversion)))
    );
  }

  @Test
  public void split_up_comma_separated_tags() throws Exception {
    result = expectSuccess(sample().withTags("one,two,three"));
    assertThat(result.getTags(), contains("one", "two", "three"));
  }

  @Test
  public void use_resourceURL_and_localID_as_about_links() throws Exception {
    result = expectSuccess(sample()
            .withResourceURL("http://example.com/resource")
            .withLocalID("http://example.com/local")
    );
    assertThat(result.getLinks(), hasItem(Link.untitled("http://example.com/resource", "about")));
    assertThat(result.getLinks(), hasItem(Link.untitled("http://example.com/local", "about")));
  }

  @Test
  public void describe_as_sparql_service_if_sparql_endpoint_link_present() throws Exception {
    result = expectSuccess(sample().withSparqlEndpoint("http://example.com/sparql"));
    assertThat(result.getLinks(), hasItems(
        Link.untitled("http://www.w3.org/ns/sparql-service-description#Service", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
        , Link.untitled("http://example.com/sparql", "http://www.w3.org/ns/sparql-service-description#endpoint")
        , Link.untitled("http://example.com/sparql", "http://rdfs.org/ns/void#sparqlEndpoint")
    ));
  }

  @Test
  public void copy_LinkedTo_children_as_links() {
    result = expectSuccess(sample().withLinkedTo(
        new AtosLink().withLinkID("link-id").withLinkURI("link-uri").withLinkType("link-type")
    ));
    assertThat(result.getLinks(), hasItem(Link.create("link-uri", "link-type", "link-id")));
  }

  @Test
  public void contains_all_RelatedResources_from_input() {
    result = expectSuccess(sample().withRelatedResources(
        new AtosRelatedResource().withResourceID("resource-id").withDescription("resource-description")
    ));
    assertThat(result.getLinks(), hasItem(Link.create("resource-id", "related", "resource-description")));
  }

  @Test
  public void contains_all_SemanticConcepts_from_input() {
    result = expectSuccess(sample().withSemanticAnnotations(
        new AtosSemanticConcept().withConceptURI("concept-uri").withLabel("concept-label")
    ));
    assertThat(result.getLinks(), hasItem(Link.create("concept-uri", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "concept-label")));
  }

  @Test
  public void missing_globalID() throws Exception {
    error = expectFail(sample().withGlobalID(null));
    assertThat(error.getViolations(), hasItem(containsString("field globalID missing")));
  }

  @Test
  public void unexpected_resource_type() throws Exception {
    error = expectFail(sample().withType("not-supported"));
    assertThat(error.getViolations(), hasItem(containsString("unsupported resource type found")));
  }

  @Test
  public void missing_resource_type() throws Exception {
    error = expectFail(sample().withType(null));
    assertThat(error.getViolations(), hasItem(containsString("unsupported resource type found")));
  }

  private SchemaDescriptor expectSuccess(final AtosDataset input) {
    final ValueOrError<SchemaDescriptor> conversion = DescriptorConversion.from(input);
    assertThat("expected successful conversion", conversion.hasError(), is(false));
    return conversion.get();
  }

  private DescriptorConversion.IllegalConversion expectFail(final AtosDataset input) {
    final ValueOrError<SchemaDescriptor> conversion = DescriptorConversion.from(input);
    assertThat("expected failed conversion", conversion.hasError(), is(true));
    return (DescriptorConversion.IllegalConversion) conversion.error();
  }
}
