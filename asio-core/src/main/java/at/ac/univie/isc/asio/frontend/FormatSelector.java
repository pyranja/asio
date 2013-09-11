package at.ac.univie.isc.asio.frontend;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;

/**
 * Maintain a mapping of {@link DatasetOperation.Action actions} to {@link Variant variant}->
 * {@link SerializationFormat format} pairs derived from the set of supported formats given on
 * creation.
 * 
 * @author Chris Borckholder
 */
public class FormatSelector {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(FormatSelector.class);

  private static final String UNSUPPORTED_MESSAGE = "%s not supported";

  private final VariantConverter converter;
  private final Map<Action, Map<Variant, SerializationFormat>> mappingsByAction;

  public FormatSelector(final Set<SerializationFormat> supported, final VariantConverter converter) {
    super();
    this.converter = converter;
    mappingsByAction = initializeMappings(supported);
  }

  /**
   * Attempt to match an accepted {@link Variant} from the given request to a
   * {@link SerializationFormat format} supported for the given action.
   * 
   * @param request holding acceptable variants
   * @param action of operation
   * @return selected variant
   * @throws WebApplicationException with status 406 (Not Acceptable) if no variant matches or
   *         status 405 (Method Not Allowed) if the given action is not supported
   */
  public SerializationFormat selectFormat(final Request request, final Action action) {
    final Map<Variant, SerializationFormat> mapping = mappingsByAction.get(action);
    if (mapping.isEmpty()) {
      // TODO throw custom DatasetUsageException
      throw new WebApplicationException(Response.status(METHOD_NOT_ALLOWED)
          .allow(Collections.<String>emptySet())
          .entity(format(ENGLISH, UNSUPPORTED_MESSAGE, action)).build());
    }
    final List<Variant> candidates = ImmutableList.copyOf(mapping.keySet());
    final Variant selected = request.selectVariant(candidates);
    if (selected == null) {
      log.debug("!! no acceptable variant in {}", candidates); // XXX remove when handled in
                                                               // exception
      // TODO throw custom DatsetUsageException
      throw new WebApplicationException(Response.notAcceptable(candidates).build());
    }
    return mapping.get(selected);
  }

  /**
   * @param supported given formats
   * @return {@link Variant}->{@link SerializationFormat} mappings for each {@link Action}.
   */
  private Map<Action, Map<Variant, SerializationFormat>> initializeMappings(
      final Set<SerializationFormat> supported) {
    final Builder<Action, Map<Variant, SerializationFormat>> mappings = ImmutableMap.builder();
    for (final Action action : Action.values()) {
      mappings.put(action, mappingsFrom(filteredBy(action, supported)));
    }
    return mappings.build();
  }

  /**
   * @param iterable
   * @return Variant->format map
   */
  private Map<Variant, SerializationFormat> mappingsFrom(
      final Iterable<SerializationFormat> iterable) {
    final Builder<Variant, SerializationFormat> variant2Format = ImmutableMap.builder();
    for (final SerializationFormat each : iterable) {
      variant2Format.put(converter.asVariant(each.asMediaType()), each);
    }
    return variant2Format.build();
  }

  /**
   * @param by action that must be supported by filtered formats
   * @param formats to be filtered
   * @return iterable with filtered formats
   */
  private Iterable<SerializationFormat> filteredBy(final Action by,
      final Set<SerializationFormat> formats) {
    return Iterables.filter(formats, new Predicate<SerializationFormat>() {
      @Override
      public boolean apply(final SerializationFormat input) {
        return input.applicableOn(by);
      }
    });
  }
}
