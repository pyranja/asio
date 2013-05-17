package at.ac.univie.isc.asio.ogsadai;

import static uk.org.ogsadai.activity.delivery.DeliverToStreamActivity.INPUT_STREAM_ID;
import static uk.org.ogsadai.activity.transform.TupleToCSVActivity.INPUT_FIELDSESCAPED;
import static uk.org.ogsadai.activity.transform.TupleToCSVActivity.INPUT_HEADERINCLUDED;
import uk.org.ogsadai.activity.pipeline.ActivityDescriptor;
import uk.org.ogsadai.activity.pipeline.ActivityInputLiteral;
import uk.org.ogsadai.activity.pipeline.Literal;
import uk.org.ogsadai.activity.pipeline.SimpleActivityDescriptor;
import uk.org.ogsadai.activity.pipeline.SimpleLiteral;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.ogsadai.PipeElements.Consumer;
import at.ac.univie.isc.asio.ogsadai.PipeElements.Producer;
import at.ac.univie.isc.asio.ogsadai.PipeElements.ProducerAndConsumer;

/**
 * Collection of static factory methods for {@link Producer} and
 * {@link Consumer} in an OGSADAI pipeline workflow.
 * 
 * @author Chris Borckholder
 */
public final class PipeActivities {

	// LITERALS
	// must use generator methods here, as literals are mutated while processing
	private static Literal TRUE() {
		return new SimpleLiteral(true);
	}

	private static Literal FALSE() {
		return new SimpleLiteral(false);
	}

	// PRODUCER
	private static final String SQL_QUERY_ACTIVITY = "uk.org.ogsadai.SQLQuery";

	public static Producer sqlQuery(final ResourceID target, final String query) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(target,
				SQL_QUERY_ACTIVITY);
		product.addInput(new ActivityInputLiteral("expression", query));
		return PipeElements.producer(product, "data");
	}

	private static final String SQL_SCHEMA_ACTIVITY = "uk.org.ogsadai.ExtractTableSchema";

	public static Producer extractSchema(final ResourceID target) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(target,
				SQL_SCHEMA_ACTIVITY);
		product.addInput(new ActivityInputLiteral("name", "%"));// wildcard
		return PipeElements.producer(product, "data");
	}

	// BOTH
	private static final String WEBROWSET_TRANSFORMER_ACTIVITY = "uk.org.ogsadai.TupleToWebRowSetCharArrays";

	public static ProducerAndConsumer tupleToWebRowSetCharArrays() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				WEBROWSET_TRANSFORMER_ACTIVITY);
		return PipeElements.both(product, "data", "result");
	}

	private static final String CSV_TRANSFORMER_ACTIVITY = "uk.org.ogsadai.TupleToCSV";

	public static ProducerAndConsumer tupleToCsv() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				CSV_TRANSFORMER_ACTIVITY);
		product.addInput(new ActivityInputLiteral(INPUT_HEADERINCLUDED, TRUE()));
		product.addInput(new ActivityInputLiteral(INPUT_FIELDSESCAPED, TRUE()));
		return PipeElements.both(product, "data", "result");
	}

	private static final String XML_TABLEMETADATA_TRANSFORMER_ACTIVITY = "uk.org.ogsadai.TableMetadataToXMLCharArraysList";

	public static ProducerAndConsumer metadataToXml() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				XML_TABLEMETADATA_TRANSFORMER_ACTIVITY);
		return PipeElements.both(product, "data", "result");
	}

	// CONSUMER
	private static final String REQUESTSTATUS_DELIVERY = "uk.org.ogsadai.DeliverToRequestStatus";

	public static Consumer deliverToRequestStatus() {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				REQUESTSTATUS_DELIVERY);
		return PipeElements.consumer(product, "input");
	}

	private static final String STREAM_DELIVERY = "at.ac.univie.isc.DeliverToStream";

	public static Consumer deliverToStream(final String streamId) {
		final ActivityDescriptor product = new SimpleActivityDescriptor(
				STREAM_DELIVERY);
		product.addInput(new ActivityInputLiteral(INPUT_STREAM_ID, streamId));
		return PipeElements.consumer(product, "input");
	}

	private PipeActivities() {/* static helper */};
}
