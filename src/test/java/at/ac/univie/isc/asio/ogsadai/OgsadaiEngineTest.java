package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.workflow.Workflow;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.MockOperations;
import at.ac.univie.isc.asio.engine.OperatorCallback;
import at.ac.univie.isc.asio.transport.Transfer;

import com.google.common.io.OutputSupplier;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiEngineTest {

  private static final SerializationFormat MOCK_FORMAT = OgsadaiFormats.XML;
  private static final String MOCK_QUERY = "test-query";

  private OgsadaiEngine subject;
  @Mock
  private OgsadaiAdapter ogsadai;
  @Mock
  private Transfer exchange;
  @Mock
  private WorkflowComposer composer;
  @Mock
  private Workflow dummyWorkflow;
  @Mock
  private DaiExceptionTranslator translator;
  @Mock
  private OperatorCallback callback;
  private DatasetOperation operation;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws IOException {
    operation = MockOperations.query(MOCK_QUERY, MOCK_FORMAT);
    when(composer.createFrom(any(DatasetOperation.class), any(OutputSupplier.class))).thenReturn(
        dummyWorkflow);
    subject = new OgsadaiEngine(ogsadai, composer, translator);
  }

  // invariances

  @Test
  public void supports_ogsadai_formats() throws Exception {
    final Set<SerializationFormat> supported = subject.supports();
    final List<OgsadaiFormats> ogsadaiFormats = Arrays.asList(OgsadaiFormats.values());
    assertTrue(supported.containsAll(ogsadaiFormats));
    assertTrue(ogsadaiFormats.containsAll(supported));
  }

  // behavior

  @SuppressWarnings("unchecked")
  @Test
  public void passes_operation_to_composer() throws Exception {
    subject.invoke(operation, exchange, callback);
    verify(composer).createFrom(same(operation), any(OutputSupplier.class));
  }

  @Test
  public void invokes_ogsadai_with_composed_workflow() throws Exception {
    subject.invoke(operation, exchange, callback);
    verify(ogsadai).invoke(anyString(), same(dummyWorkflow), any(CompletionCallback.class));
  }
}
