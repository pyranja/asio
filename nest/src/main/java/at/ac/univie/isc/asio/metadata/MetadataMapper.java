package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Pretty;
import org.slf4j.Logger;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Map {@link at.ac.univie.isc.asio.metadata.DatasetMetadata} to
 * {@link DatasetDescription}.
 */
@Component
public final class MetadataMapper {
  private static final Logger log = getLogger(MetadataMapper.class);

  private MetadataMapper() {}

  public static MetadataMapper newInstance() {
    return new MetadataMapper();
  }

  public static class IllegalConversion extends IllegalArgumentException {
    public IllegalConversion(final String message) {
      super(message);
    }
  }

  public DatasetDescription convert(final DatasetMetadata meta) {
    final Violations collector = Violations.newInstance();
    final DatasetDescription result = new DatasetDescription();

    result.setGlobalIdentifier(safeGet(meta.getGlobalID()));
    result.setLocalIdentifier(require(meta.getLocalID(), "localID", collector));
    result.setType(require(safeGet(meta.getType()), "type", collector));
    final MetadataStatus status = require(meta.getStatus(), "status", collector);
    result.setActive(status == MetadataStatus.ACTIVE);

    result.setLabel(require(meta.getName(), "name", collector));
    result.setDescription(meta.getDescription().getValue());
    result.setCategory(safeGet(safeGet(meta.getCategory())));
    result.setAuthor(safeGet(meta.getAuthor()));
    result.setLicense(safeGet(safeGet(meta.getLicence())));
    result.setCreated(convertXmlDate(safeGet(meta.getCreationDate())));
    result.setUpdated(convertXmlDate(safeGet(meta.getUpdateDate())));
    result.setTags(splitTags(safeGet(meta.getTags())));

    addLink(safeGet(meta.getIcon()), "icon", result);
    addLink(meta.getSparqlEndPoint(), "http://rdfs.org/ns/void#sparqlEndpoint", result);
    addLink(safeGet(meta.getResourceURL()), Link.REL_SELF, result);
    linkToRelated(safeGet(meta.getRelatedResources()), safeGet(meta.getLinkedTo()), safeGet(meta.getSemanticAnnotations()), result);

    if (collector.hasErrors()) {
      log.debug("conversion from {} to Dataset failed due to {}", meta, collector.getErrors());
      throw new IllegalConversion(Pretty.format("cannot convert to Dataset : %s", collector.getErrors()));
    }

    return result;
  }

  private void addLink(final String path, final String relation, final DatasetDescription container) {
    if (hasText(path)) {
      container.add(new Link(path, relation));
    }
  }

  private void linkToRelated(final RelatedResourceList relatedList, final LinkedToList linkedToList, final SemanticConceptList semanticConceptList, final DatasetDescription result) {
    if (relatedList != null && relatedList.getRelatedResource() != null) {
      final List<String> related = relatedList.getRelatedResource();
      for (final String each : related) {
        if (hasText(each)) {
          addLink(each, "related", result);
        }
      }
    }
    if (linkedToList != null && linkedToList.getLink() != null) {
      final List<LinkedTo> links = linkedToList.getLink();
      for (final LinkedTo link : links) {
        if (validLinkedTo(link)) {
          addLink(link.getLinkURI(), link.getLinkType().value(), result);
        }
      }
    }
    if (semanticConceptList != null && semanticConceptList.getSemanticConcept() != null) {
      final List<SemanticConcept> concepts = semanticConceptList.getSemanticConcept();
      for (final SemanticConcept concept : concepts) {
        if (concept != null && hasText(concept.getConceptURI())) {
          addLink(concept.conceptURI, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", result);
        }
      }
    }
  }

  private boolean validLinkedTo(final LinkedTo link) {
    if (link != null) {
      final LinkType linkType = link.getLinkType();
      final String uri = link.getLinkURI();
      return linkType != null && hasText(linkType.value()) && hasText(uri);
    }
    return false;
  }

  private ZonedDateTime convertXmlDate(final XMLGregorianCalendar xml) {
    if (xml.isValid()) {
      final Date utc =
          xml.toGregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH, null).getTime();
      return ZonedDateTime.ofInstant(Instant.ofEpochMilli(utc.getTime()), ZoneOffset.UTC);
    }
    return null;
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

  private <ATTRIBUTE> ATTRIBUTE safeGet(final JAXBElement<ATTRIBUTE> element) {
    return element == null ? null : element.getValue();
  }

  private List<String> splitTags(final String tags) {
    return tags == null ? Collections.<String>emptyList() : Arrays.asList(tags.split(","));
  }

  private String safeGet(final ResourceType type) {
    return type == null ? null : type.value();
  }

  private String safeGet(final Category category) {
    return category == null ? null : category.value();
  }

  private String safeGet(final LicenceType license) {
    return license == null ? null : license.value();
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
