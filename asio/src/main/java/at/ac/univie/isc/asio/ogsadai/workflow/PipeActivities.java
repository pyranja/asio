package at.ac.univie.isc.asio.ogsadai.workflow;

import static at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.both;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.consumer;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.producer;
import static uk.org.ogsadai.activity.delivery.DeliverToStreamActivity.INPUT_STREAM_ID;
import static uk.org.ogsadai.activity.transform.DynamicSerializationActivity.INPUT_TRANSFORMER;
import static uk.org.ogsadai.activity.transform.TupleToCSVActivity.INPUT_FIELDSESCAPED;
import static uk.org.ogsadai.activity.transform.TupleToCSVActivity.INPUT_HEADERINCLUDED;
import uk.org.ogsadai.activity.ActivityName;
import uk.org.ogsadai.activity.pipeline.ActivityDescriptor;
import uk.org.ogsadai.activity.pipeline.ActivityInputLiteral;
import uk.org.ogsadai.activity.pipeline.Literal;
import uk.org.ogsadai.activity.pipeline.SimpleActivityDescriptor;
import uk.org.ogsadai.activity.pipeline.SimpleLiteral;
import uk.org.ogsadai.activity.transform.BlockTransformer;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.Consumer;
import at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.Producer;
import at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.ProducerAndConsumer;

/**
 * Collection of static factory methods for {@link Producer} and
 * {@link Consumer} in an OGSADAI pipeline workflow.
 * 
 * @author Chris Borckholder
 */
public final class PipeActivities {

	// HELPER
	// type safety and eases tests
	private static ActivityName asName(final String plain) {
		return new ActivityName(plain);
	}

	// LITERALS
	// must use generator methods here, as literals are mutated while processing
	private static Literal TRUE() {
		return new SimpleLiteral(Boolean.TRUE);
	}

	@SuppressWarnings("unused")
	private static Literal FALSE() {
		return new SimpleLiteral(Boolean.FALSE);
	}

	private static Literal from(final Object that) {
		return new SimpleLiteral(that);
	}

	// PRODUCER
	static final ActivityName SQL_QUERY_ACTIVITY = asName("uk.org.ogsadai.SQLQuery");

	public static Producer sqlQuery(final ResourceID target, final String query) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(target,
				SQL_QUERY_ACTIVITY);
		product.addInput(new ActivityInputLiteral("expression", query));
		return producer(product, "data");
	}

	static final ActivityName SQL_SCHEMA_ACTIVITY = asName("uk.org.ogsadai.ExtractTableSchema");

	public static Producer extractSchema(final ResourceID target) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(target,
				SQL_SCHEMA_ACTIVITY);
		product.addInput(new ActivityInputLiteral("name", "%"));// wildcard
		return producer(product, "data");
	}

	static final ActivityName SQL_UPDATE_ACTIVITY = asName("uk.org.ogsadai.SQLUpdate");

	public static Producer sqlUpdate(final ResourceID target,
			final String update) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(target,
				SQL_UPDATE_ACTIVITY);
		product.addInput(new ActivityInputLiteral("expression", update));
		return producer(product, "result");
	}

	// BOTH
	static final ActivityName TUPLE_WEBROWSET_TRANSFORMER_ACTIVITY = asName("uk.org.ogsadai.TupleToWebRowSetCharArrays");

	public static ProducerAndConsumer tupleToWebRowSetCharArrays() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				TUPLE_WEBROWSET_TRANSFORMER_ACTIVITY);
		return both(product, "data", "result");
	}

	static final ActivityName TUPLE_CSV_TRANSFORMER_ACTIVITY = asName("uk.org.ogsadai.TupleToCSV");

	public static ProducerAndConsumer tupleToCsv() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				TUPLE_CSV_TRANSFORMER_ACTIVITY);
		product.addInput(new ActivityInputLiteral(INPUT_HEADERINCLUDED, TRUE()));
		product.addInput(new ActivityInputLiteral(INPUT_FIELDSESCAPED, TRUE()));
		return both(product, "data", "result");
	}

	static final ActivityName TABLEMETADATA_XML_TRANSFORMER_ACTIVITY = asName("uk.org.ogsadai.TableMetadataToXMLCharArraysList");

	public static ProducerAndConsumer metadataToXml() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				TABLEMETADATA_XML_TRANSFORMER_ACTIVITY);
		return both(product, "data", "result");
	}

	static final ActivityName DYNAMIC_TRANSFORMER_ACTIVITY = asName("at.ac.univie.isc.DynamicSerialization");

	public static ProducerAndConsumer dynamicSerializer(
			final BlockTransformer transformer) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				DYNAMIC_TRANSFORMER_ACTIVITY);
		product.addInput(new ActivityInputLiteral(INPUT_TRANSFORMER,
				from(transformer)));
		return both(product, "data", "result");
	}

	// CONSUMER
	static final ActivityName REQUESTSTATUS_DELIVERY = asName("uk.org.ogsadai.DeliverToRequestStatus");

	public static Consumer deliverToRequestStatus() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				REQUESTSTATUS_DELIVERY);
		return consumer(product, "input");
	}

	static final ActivityName STREAM_DELIVERY = asName("at.ac.univie.isc.DeliverToStream");

	public static Consumer deliverToStream(final String streamId) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				STREAM_DELIVERY);
		product.addInput(new ActivityInputLiteral(INPUT_STREAM_ID, streamId));
		return consumer(product, "input");
	}

	private PipeActivities() {/* static helper */};
}
