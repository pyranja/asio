package at.ac.univie.isc.asio.ogsadai;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.transport.FileResult;
import at.ac.univie.isc.asio.transport.FileResultRepository;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiEngineTest {

	private static final ResourceID RESOURCE = new ResourceID("test");

	private OgsadaiEngine subject;
	@Mock private OgsadaiAdapter ogsadai;
	@Mock private FileResultRepository results;
	@Mock private FileResult handler;
	@Mock private OutputStream stream;

	@Before
	public void setUp() throws IOException {
		subject = new OgsadaiEngine(ogsadai, results, RESOURCE);
		when(results.newResult()).thenReturn(handler);
		when(handler.getOutput()).thenReturn(stream);
	}

	@Test(expected = DatasetUsageException.class)
	public void reject_null_query() throws Exception {
		subject.submit(null);
	}

	@Test(expected = DatasetUsageException.class)
	public void reject_empty_query() throws Exception {
		subject.submit("");
	}
}
