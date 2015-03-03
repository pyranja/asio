package at.ac.univie.isc.asio.metadata;

import net.atos.AtosDataset;
import net.atos.AtosLink;
import net.atos.AtosRelatedResource;
import net.atos.AtosSemanticConcept;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MetadataMapperTest {
    @Test
    public void should_convert_atos_dataset_to_descriptor() throws Exception {
      final AtosDataset input = new AtosDataset()
          .withAuthor("author")
          .withCategory("category")
          .withDescription("description")
          .withLicence("license")
          .withGlobalID("44bab742-2c27-4dad-8419-fae8a848358b")
          .withLocalID("http://example.com/local-id/")
          .withLinkedTo(new AtosLink().withLinkID("link-id").withLinkType("link-type").withLinkURI("link-uri"))
          .withCreationDate("2014-04-14 16:49:54.000")
          .withUpdateDate("2014-04-14 17:02:53.000")
          .withMetadataCreationDate("2014-04-14 16:49:54.945")
          .withMetadataUpdateDate("2015-02-23 11:41:51.307")
          .withName("name")
          .withProvenance("provenance")
          .withRating("0")
          .withRelatedResources(new AtosRelatedResource().withDescription("related-resource-description").withResourceID("related-resource-id"))
          .withResourceURL("http://example.com/resource-url/")
          .withSemanticAnnotations(new AtosSemanticConcept().withConceptURI("concept-uri").withLabel("concept-label"))
          .withStatus("active")
          .withTags("tag-one, tag-two")
          .withType("Dataset")
          .withViews("18")
          .withSparqlEndpoint("http://example.com/sparql-endpoint")
      ;
      final DatasetDescription expected = DatasetDescription.empty()
          .withGlobalIdentifier("44bab742-2c27-4dad-8419-fae8a848358b")
          .withLocalIdentifier("http://example.com/local-id/")
          .withAuthor("author")
          .withCategory("category")
          .withDescription("description")
          .withLicense("license")
          .withLabel("name")
          .withActive(true)
          .withTags("tag-one", "tag-two")
          .withType("Dataset")
          .withCreated(ZonedDateTime.of(2014, 4, 14, 16, 49, 54, 0, ZoneOffset.UTC))
          .withUpdated(ZonedDateTime.of(2014, 4, 14, 17, 2, 53, 0, ZoneOffset.UTC));
      expected.add(Arrays.asList(
          new Link("http://example.com/sparql-endpoint", "http://rdfs.org/ns/void#sparqlEndpoint"),
          new Link("http://example.com/resource-url/"),
          new Link("related-resource-id", "related"),
          new Link("link-uri", "link-type"),
          new Link("concept-uri", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")));
      assertThat(MetadataMapper.newInstance().convert(input), is(expected));
    }
}
