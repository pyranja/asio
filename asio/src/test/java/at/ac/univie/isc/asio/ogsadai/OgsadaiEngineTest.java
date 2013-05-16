package at.ac.univie.isc.asio.ogsadai;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.common.MockSerializationFormat;
import at.ac.univie.isc.asio.transport.FileResultRepository;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiEngineTest {

	private static final ResourceID RESOURCE = new ResourceID("test");
	private static final SerializationFormat MOCK_FORMAT = new MockSerializationFormat();

	private OgsadaiEngine subject;
	@Mock private OgsadaiAdapter ogsadai;
	@Mock private FileResultRepository results;

	@Before
	public void setUp() throws IOException {
		subject = new OgsadaiEngine(ogsadai, results, RESOURCE);
	}

	@Test(expected = DatasetUsageException.class)
	public void reject_null_query() throws Exception {
		final DatasetOperation op = DatasetOperation.query(null, MOCK_FORMAT);
		subject.submit(op);
	}

	@Test(expected = DatasetUsageException.class)
	public void reject_empty_query() throws Exception {
		final DatasetOperation op = DatasetOperation.query("", MOCK_FORMAT);
		subject.submit(op);
	}
}
