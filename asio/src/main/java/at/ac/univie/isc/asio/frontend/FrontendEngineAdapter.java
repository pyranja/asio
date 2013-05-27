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

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.Result;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Enhances a {@link DatasetEngine} with JAXRS-aware functionality.
 * 
 * @author Chris Borckholder
 */
public class FrontendEngineAdapter implements DatasetEngine {

	private static final String UNSUPPORTED_MESSAGE = "%s not supported";

	private final DatasetEngine delegate;
	private final VariantConverter converter;
	private Map<Action, Map<Variant, SerializationFormat>> mappingsByAction;

	FrontendEngineAdapter(final DatasetEngine delegate,
			final VariantConverter converter) {
		super();
		this.delegate = delegate;
		this.converter = converter;
		mappingsByAction = initializeMappings();
	}

	@Override
	public ListenableFuture<Result> submit(final DatasetOperation operation) {
		try {
			return delegate.submit(operation);
		} catch (final DatasetException e) {
			e.setFailedOperation(operation);
			return Futures.immediateFailedFuture(e);
		} catch (final Exception e) {
			final DatasetFailureException wrapper = new DatasetFailureException(
					e);
			wrapper.setFailedOperation(operation);
			return Futures.immediateFailedFuture(wrapper);
		}
	}

	@Override
	public Set<SerializationFormat> supportedFormats() {
		return delegate.supportedFormats();
	}

	@VisibleForTesting
	void repopulateMappings() {
		mappingsByAction = initializeMappings();
	}

	/**
	 * Attempts to find a {@link SerializationFormat} that is supported by the
	 * backing engine for the given {@link Action} and is compatible to the
	 * content types accepted by the given request.
	 * 
	 * @param request
	 *            holding acceptable variants
	 * @param action
	 *            of operation
	 * @return selected variant
	 * @throws WebApplicationException
	 *             with status 406 (Not Acceptable) if no variant matches or
	 *             status 405 (Method Not Allowed) if the given action is not
	 *             supported
	 */
	public SerializationFormat selectFormat(final Request request,
			final Action action) {
		final Map<Variant, SerializationFormat> mapping = mappingsByAction
				.get(action);
		if (mapping.isEmpty()) {
			throw new WebApplicationException(Response
					.status(METHOD_NOT_ALLOWED)
					.allow(Collections.<String> emptySet())
					.entity(format(ENGLISH, UNSUPPORTED_MESSAGE, action))
					.build());
		}
		final List<Variant> candidates = ImmutableList.copyOf(mapping.keySet());
		final Variant selected = request.selectVariant(candidates);
		if (selected == null) {
			throw new WebApplicationException(Response
					.notAcceptable(candidates).build());
		}
		return mapping.get(selected);
	}

	/**
	 * @return {@link Variant}->{@link SerializationFormat} mappings for each
	 *         {@link Action}.
	 */
	private Map<Action, Map<Variant, SerializationFormat>> initializeMappings() {
		final Builder<Action, Map<Variant, SerializationFormat>> partial = ImmutableMap
				.builder();
		for (final Action action : Action.values()) {
			final Builder<Variant, SerializationFormat> variant2Format = ImmutableMap
					.builder();
			for (final SerializationFormat each : filterFormats(action)) {
				variant2Format.put(converter.asVariant(each.asMediaType()),
						each);
			}
			partial.put(action, variant2Format.build());
		}
		return partial.build();
	}

	/**
	 * @param by
	 *            action to be supported
	 * @return formats that are applicable to given action
	 */
	private Iterable<SerializationFormat> filterFormats(final Action by) {
		return Iterables.filter(delegate.supportedFormats(),
				new Predicate<SerializationFormat>() {
					@Override
					public boolean apply(final SerializationFormat input) {
						return input.applicableOn(by);
					}
				});
	}
}
