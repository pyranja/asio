package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Pretty;
import at.ac.univie.isc.asio.tool.ValueOrError;
import at.ac.univie.isc.asio.tool.Violations;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import net.atos.AtosDataset;
import net.atos.AtosLink;
import net.atos.AtosRelatedResource;
import net.atos.AtosSemanticConcept;
import org.slf4j.Logger;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import rx.functions.Func1;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Map {@link net.atos.AtosDataset} to {@link at.ac.univie.isc.asio.metadata.SchemaDescriptor}.
 */
public final class DescriptorConversion {
  private static final Logger log = getLogger(DescriptorConversion.class);

  /**
   * Create a function that applies this conversion to its input.
   *
   * @return a function performing this conversion
   */
  public static Func1<AtosDataset, SchemaDescriptor> asFunction() {
    return new Func1<AtosDataset, SchemaDescriptor>() {
      @Override
      public SchemaDescriptor call(final AtosDataset atosDataset) {
        return DescriptorConversion.from(atosDataset).get();
      }
    };
  }

  /**
   * Create a conversion from the given {@link net.atos.AtosDataset} to a
   * {@link at.ac.univie.isc.asio.metadata.SchemaDescriptor}. The conversion may fail.
   *
   * @param input source metadata object
   * @return conversion result
   */
  static ValueOrError<SchemaDescriptor> from(final AtosDataset input) {
    return new DescriptorConversion(input).result();
  }

  public static class IllegalConversion extends IllegalArgumentException {
    private final List<String> violations;

    public IllegalConversion(final List<String> violations) {
      super(Pretty.format("failed to map AtosDataset to SchemaDescriptor: %s", violations));
      this.violations = violations;
    }

    public List<String> getViolations() {
      return violations;
    }
  }


  private final AtosDataset input;
  private final Violations report;
  private SchemaDescriptor result;
  private ImmutableList.Builder<Link> linkCollector = ImmutableList.builder();

  DescriptorConversion(final AtosDataset input) {
    this.input = input;
    this.report = Violations.newInstance();
    validate(input);
    if (report.currentlyValid()) {
      result = convert(SchemaDescriptor.empty(input.getGlobalID()));
    }
  }

  public ValueOrError<SchemaDescriptor> result() {
    if (report.hasWarnings()) { log.debug("conversion succeeded with warnings : {}", report); }
    return report.isFatal()
        ? ValueOrError.<SchemaDescriptor>invalid(new IllegalConversion(report.getViolations()))
        : ValueOrError.valid(result);
  }

  private void validate(final AtosDataset input) {
    if (!"Dataset".equalsIgnoreCase(input.getType())) {
      report.fail(Pretty.format("unsupported resource type found - expected '%s' but was '%s'", "Dataset", input.getType()));
    }
    require(input.getGlobalID(), "globalID");
  }

  private SchemaDescriptor convert(final SchemaDescriptor.Builder state) {
    state
        .withActive(determineStatus())
        .withLabel(optional(input.getName(), "name"))
        .withDescription(optional(input.getDescription(), "description"))
        .withCategory(optional(input.getCategory(), "category"))
        .withAuthor(optional(input.getAuthor(), "author"))
        .withLicense(optional(input.getLicence(), "license"))
        .withCreated(convertXmlDate(input.getCreationDate(), "created"))
        .withUpdated(convertXmlDate(input.getUpdateDate(), "updated"))
        .withTags(splitTags(input.getTags()))
    ;
    link(input.getLocalID(), "about", null);
    link(input.getResourceURL(), "about", null);
    if (hasText(input.getSparqlEndpoint())) { describeSparqlService(); }
    if (present(input.getRelatedResources())) { attachRelatedResources(); }
    if (present(input.getLinkedTo())) { attachLinkedElements(); }
    if (present(input.getSemanticAnnotations())) { attachSemanticAnnotations(); }
    state.withLinks(linkCollector.build());
    return state.build();
  }

  private boolean determineStatus() {
    return "active".equalsIgnoreCase(optional(input.getStatus(), "status"));
  }

  private void attachSemanticAnnotations() {
    for (AtosSemanticConcept concept : input.getSemanticAnnotations()) {
      link(concept.getConceptURI(), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", concept.getLabel());
    }
  }

  private void attachLinkedElements() {
    for (AtosLink linkedTo : input.getLinkedTo()) {
      link(linkedTo.getLinkURI(), linkedTo.getLinkType(), linkedTo.getLinkID());
    }
  }

  private void attachRelatedResources() {
    for (AtosRelatedResource resource : input.getRelatedResources()) {
      link(resource.getResourceID(), "related", resource.getDescription());
    }
  }

  private void describeSparqlService() {
    link("http://www.w3.org/ns/sparql-service-description#Service", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
    link(input.getSparqlEndpoint(), "http://www.w3.org/ns/sparql-service-description#endpoint", null);
    link(input.getSparqlEndpoint(), "http://rdfs.org/ns/void#sparqlEndpoint", null);
  }

  /**
   * Conditionally add link if given parameters are valid. href and relation must be non-null and
   * non-empty. title is optional.
   */
  private void link(final String href, final String relation, final String title) {
    if (hasText(href) && hasText(relation)) {
      if (hasText(title)) {
        linkCollector.add(Link.create(href, relation, title));
      } else {
        linkCollector.add(Link.untitled(href, relation));
      }
    }
  }

  /**
   * Try to convert jaxb xml date text. Fails silently by returning null.
   */
  private ZonedDateTime convertXmlDate(final String xml, final String property) {
    try {
      return ZonedDateTime.parse(xml, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
          .withLocale(Locale.ENGLISH).withZone(ZoneOffset.UTC));
    } catch (final DateTimeParseException | NullPointerException invalid) {
      report.warn(Pretty.format("invalid %s xsd:datetime input '%s' (%s)", property, xml, invalid.getMessage()));
      return null;  // null is allowed
    }
  }

  private List<String> splitTags(final String tags) {
    final String tagText = hasText(tags) ? tags : "";
    return Splitter.on(',').omitEmptyStrings().trimResults().splitToList(tagText);
  }

  private <ATTRIBUTE> ATTRIBUTE require(final ATTRIBUTE value, final String property) {
    if (value == null) { report.fail(Pretty.format("field %s missing", property)); }
    return value;
  }

  private <ATTRIBUTE> ATTRIBUTE optional(final ATTRIBUTE value, final String property) {
    if (value == null) { report.warn(Pretty.format("field %s missing", property)); }
    return value;
  }

  private boolean present(final Collection<?> values) {
    return values != null && !values.isEmpty();
  }

  private boolean hasText(final String value) {
    return value != null && !value.trim().isEmpty();
  }
}
