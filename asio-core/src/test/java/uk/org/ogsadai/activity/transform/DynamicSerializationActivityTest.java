package uk.org.ogsadai.activity.transform;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.ControlBlock;
import uk.org.ogsadai.exception.DAIException;

@RunWith(MockitoJUnitRunner.class)
public class DynamicSerializationActivityTest {

  private DynamicSerializationActivity subject;
  @Mock
  BlockTransformer transformer;
  @Mock
  BlockWriter writer;

  @Before
  public void setUp() throws DAIException {
    subject = new DynamicSerializationActivity();
    subject.addOutput(DynamicSerializationActivity.OUTPUT, writer);
    subject.preprocess();
  }

  // invariances

  @Test(expected = NullPointerException.class)
  public void null_block_fails() throws Exception {
    subject.processIteration(asArgs(null, transformer));
  }

  @Test(expected = NullPointerException.class)
  public void null_transformer_fails() throws Exception {
    subject.processIteration(asArgs(5, null));
  }

  // behavior

  @Test
  public void writes_list_start_and_end() throws Exception {
    subject.processIteration(asArgs(5, transformer));
    final InOrder list_wrapping = inOrder(writer);
    list_wrapping.verify(writer).write(ControlBlock.LIST_BEGIN);
    list_wrapping.verify(writer).write(ControlBlock.LIST_END);
  }

  @Test
  public void invokes_transformer() throws Exception {
    final Integer block = Integer.valueOf(5);
    subject.processIteration(asArgs(block, transformer));
    final ArgumentCaptor<OutputStream> stream = ArgumentCaptor.forClass(OutputStream.class);
    verify(transformer).writeObject(stream.capture(), same(block));
    assertTrue(stream.getValue() instanceof BlockWriterStreamAdapter);
    assertSame(writer, ((BlockWriterStreamAdapter) stream.getValue()).getBlockWriter());
  }

  @Test
  public void closes_output() throws Exception {
    subject.processIteration(asArgs(5, transformer));
    verify(writer).closeForWriting();
  }

  // failures

  @Test(expected = ActivityIOException.class)
  public void wraps_io_exception_from_transformer() throws Exception {
    Mockito.doThrow(IOException.class).when(transformer)
        .writeObject(any(OutputStream.class), any(Integer.class));
    try {
      subject.processIteration(asArgs(5, transformer));
    } finally {
      verify(writer).closeForWritingDueToError();
    }
  }

  private Object[] asArgs(final Integer count, final BlockTransformer transformer) {
    return new Object[] {count, transformer};
  }
}
