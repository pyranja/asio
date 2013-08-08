package uk.org.ogsadai.activity.transform;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.io.BlockReader;
import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.ControlBlock;
import uk.org.ogsadai.exception.DAIException;
import at.ac.univie.isc.asio.ogsadai.workflow.XmlUpdateCountTransformer;

// tests complete execution of dynamic serializer as OGSADAI would do it
@RunWith(MockitoJUnitRunner.class)
public class DynamicSerializationIntegrationTest {

  private DynamicSerializationActivity activity;
  @Mock
  private BlockReader dataIn;
  @Mock
  private BlockReader transformerIn;
  @Mock
  private BlockWriter resultOut;
  private byte[] expected_serialization;
  @Captor
  ArgumentCaptor<Object> written;

  @Before
  public void setUp() throws DAIException, IOException {
    Mockito.when(dataIn.read()).thenReturn(Integer.valueOf(5), ControlBlock.NO_MORE_DATA);
    Mockito.when(transformerIn.read()).thenReturn(new XmlUpdateCountTransformer("test-query"),
        ControlBlock.NO_MORE_DATA);
    activity = new DynamicSerializationActivity();
    activity.addInput("data", dataIn);
    activity.addInput("transformer", transformerIn);
    activity.addOutput("result", resultOut);
    create_expected_serialization();
  }

  private void create_expected_serialization() throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new XmlUpdateCountTransformer("test-query").writeObject(out, Integer.valueOf(5));
    expected_serialization = out.toByteArray();
  }

  @Test
  public void test_execution() throws Exception {
    activity.process();
    Mockito.verify(resultOut, atLeast(3)).write(written.capture());
    final Iterator<Object> captured = written.getAllValues().iterator();
    assertEquals(ControlBlock.LIST_BEGIN, captured.next());
    Object current = captured.next();
    final ByteArrayOutputStream collected = new ByteArrayOutputStream();
    while (current instanceof byte[] && captured.hasNext()) {
      collected.write((byte[]) current);
      current = captured.next();
    }
    assertEquals(ControlBlock.LIST_END, current);
    Assert.assertArrayEquals(expected_serialization, collected.toByteArray());
  }
}
