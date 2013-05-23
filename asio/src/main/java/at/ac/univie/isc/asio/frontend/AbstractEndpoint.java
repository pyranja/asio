package at.ac.univie.isc.asio.frontend;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.OperationFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Provide generic request handling and processing functionality for Endpoints.
 * 
 * @author Chris Borckholder
 */
public class AbstractEndpoint {

	// dependencies
	protected final DatasetEngine engine;
	protected final OperationFactory create;
	// utils
	protected final VariantConverter converter;
	private final Action type;
	private Map<Variant, SerializationFormat> variant2format;

	/**
	 * subclass constructor
	 * 
	 * @param engine
	 *            backing dataset
	 * @param create
	 *            operation factory
	 * @param type
	 *            of concrete endpoint
	 */
	protected AbstractEndpoint(final DatasetEngine engine,
			final OperationFactory create, final Action type) {
		super();
		this.engine = engine;
		this.create = create;
		this.type = type;
		converter = VariantConverter.getInstance();
		initializeVariants();
	}

	/**
	 * Create the reverse mapping between variants and serialization formats.
	 */
	@VisibleForTesting
	void initializeVariants() {
		final Set<SerializationFormat> supported = engine.supportedFormats();
		final Builder<Variant, SerializationFormat> map = ImmutableMap
				.builder();
		for (final SerializationFormat format : supported) {
			if (format.applicableOn(type)) {
				final Variant variant = converter.asVariant(format
						.asMediaType());
				map.put(variant, format);
			}
		}
		variant2format = map.build();
	}

	/**
	 * Select the {@link SerializationFormat format} from the initialized
	 * formats which best matches the given request's accept-header.
	 * 
	 * @param request
	 *            to be handled
	 * @return the best match found
	 * @throws WebApplicationException
	 *             if no format matches the accept header.
	 */
	protected SerializationFormat matchFormat(final Request request) {
		final List<Variant> candidates = ImmutableList.copyOf(variant2format
				.keySet()); // not really copying
		final Variant selected = request.selectVariant(candidates);
		if (selected != null) {
			return variant2format.get(selected);
		} else {
			throw new WebApplicationException(Response
					.notAcceptable(candidates).build());
		}
	}

}