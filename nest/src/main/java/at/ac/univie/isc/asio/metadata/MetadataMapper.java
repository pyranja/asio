package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Pretty;
import com.hp.hpl.jena.sparql.util.DateTimeStruct;
import net.atos.AtosDataset;
import net.atos.AtosLink;
import net.atos.AtosRelatedResource;
import net.atos.AtosSemanticConcept;
import org.slf4j.Logger;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Map {@link at.ac.univie.isc.asio.metadata.DatasetMetadata} to
 * {@link DatasetDescription}.
 */
@Component
public final class MetadataMapper {
  private static final Logger log = getLogger(MetadataMapper.class);

  private MetadataMapper() {
  }

  public static MetadataMapper newInstance() {
    return new MetadataMapper();
  }

  public static class IllegalConversion extends IllegalArgumentException {
    public IllegalConversion(final String message) {
      super(message);
    }
  }

  public DatasetDescription convert(final AtosDataset meta) {
    final Violations collector = Violations.newInstance();
    final DatasetDescription result = new DatasetDescription();

    result.setGlobalIdentifier(meta.getGlobalID());
    result.setLocalIdentifier(require(meta.getLocalID(), "localID", collector));
    result.setType(require(meta.getType(), "type", collector));
    final String status = require(meta.getStatus(), "status", collector);
    result.setActive("active".equalsIgnoreCase(status));

    result.setLabel(require(meta.getName(), "name", collector));
    result.setDescription(meta.getDescription());
    result.setCategory(meta.getCategory());
    result.setAuthor(meta.getAuthor());
    result.setLicense((meta.getLicence()));
    result.setCreated(convertXmlDate(meta.getCreationDate()));
    result.setUpdated(convertXmlDate(meta.getUpdateDate()));
    result.setTags(splitTags(meta.getTags()));

    addLink(meta.getSparqlEndpoint(), "http://rdfs.org/ns/void#sparqlEndpoint", result);
    addLink(meta.getResourceURL(), Link.REL_SELF, result);
    linkToRelated(meta.getRelatedResources(), meta.getLinkedTo(), meta.getSemanticAnnotations(), result);

    if (collector.hasErrors()) {
      log.debug("conversion from {} to Dataset failed due to {}", meta, collector.getErrors());
      throw new IllegalConversion(Pretty.format("cannot convert to Dataset : %s", collector.getErrors()));
    }

    return result;
  }

  private void addLink(final String path, final String relation, final DatasetDescription container) {
    if (hasText(path) && hasText(relation)) {
      container.add(new Link(path, relation));
    }
  }

  private void linkToRelated(final List<AtosRelatedResource> relatedList, final List<AtosLink> linkedToList, final List<AtosSemanticConcept> semanticConceptList, final DatasetDescription result) {
    if (relatedList != null) {
      for (final AtosRelatedResource each : relatedList) {
        addLink(each.getResourceID(), "related", result);
      }
    }
    if (linkedToList != null) {
      for (final AtosLink link : linkedToList) {
        addLink(link.getLinkURI(), link.getLinkType(), result);
      }
    }
    if (semanticConceptList != null) {
      for (final AtosSemanticConcept concept : semanticConceptList) {
        addLink(concept.getConceptURI(), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", result);
      }
    }
  }

  private ZonedDateTime convertXmlDate(final String xml) {
    try {
      return ZonedDateTime.parse(xml, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withLocale(Locale.ENGLISH).withZone(ZoneOffset.UTC));
    } catch (final DateTimeStruct.DateTimeParseException invalid) {
      log.debug("invalid xsd:datetime input '{}'", xml);
      return null;  // null is allowed
    }
  }

  private <ATTRIBUTE> ATTRIBUTE require(final ATTRIBUTE value, final String field, final Violations collector) {
    if (value == null) {
      collector.add(Pretty.format("field %s missing", field));
    }
    return value;
  }

  private boolean hasText(final String value) {
    return value != null && !value.trim().isEmpty();
  }

  private List<String> splitTags(final String tags) {
    if (tags == null) {
      return Collections.emptyList();
    }
    else {
      final String[] tagArray = tags.split(",");
      for (int i = 0; i < tagArray.length; i++) {
        tagArray[i] = tagArray[i].trim();
      }
      return Arrays.asList(tagArray);
    }
  }

  /**
   * Collect errors, e.g. while validating an object.
   */
  static class Violations {
    private final List<String> errors = new ArrayList<>();

    static Violations newInstance() {
      return new Violations();
    }

    private Violations() {
    }

    public void add(final String violation) {
      errors.add(violation);
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public List<String> getErrors() {
      return errors;
    }
  }
}
