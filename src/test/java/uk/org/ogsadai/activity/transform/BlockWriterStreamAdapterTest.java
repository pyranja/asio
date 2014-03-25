package uk.org.ogsadai.activity.transform;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.PipeIOException;
import uk.org.ogsadai.activity.io.PipeTerminatedException;

import com.google.common.base.Charsets;

@RunWith(MockitoJUnitRunner.class)
public class BlockWriterStreamAdapterTest {

  private static final int BLOCK_SIZE = 30;
  private static final byte[] PAYLOAD = "THIS IS A TEST PAYLOAD OF SOME LENGTH > BLOCK_SIZE"
      .getBytes(Charsets.UTF_8);

  private BlockWriterStreamAdapter subject;
  @Mock
  private BlockWriter writer;
  @Captor
  private ArgumentCaptor<byte[]> block;

  @Before
  public void setUp() {
    subject = new BlockWriterStreamAdapter(writer, BLOCK_SIZE);
  }

  // invariances

  @Test(expected = NullPointerException.class)
  public void fail_on_null_writer() throws Exception {
    subject = new BlockWriterStreamAdapter(null, BLOCK_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_negative_block_size() throws Exception {
    subject = new BlockWriterStreamAdapter(writer, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_zero_block_size() throws Exception {
    subject = new BlockWriterStreamAdapter(writer, 0);
  }

  // behavior

  @Test
  public void single_byte_is_buffered() throws Exception {
    subject.write(1);
    verifyZeroInteractions(writer);
  }

  @Test
  public void up_to_block_size_bytes_are_buffered() throws Exception {
    final byte[] data = Arrays.copyOf(PAYLOAD, BLOCK_SIZE);
    subject.write(data);
    verifyZeroInteractions(writer);
  }

  @Test
  public void single_bytes_are_written_when_block_size_exceeded() throws Exception {
    final byte[] expected = Arrays.copyOf(PAYLOAD, BLOCK_SIZE);
    for (final byte each : expected) {
      subject.write(each);
    }
    subject.write(1);
    verify(writer).write(block.capture());
    assertArrayEquals(expected, block.getValue());
  }

  @Test
  public void block_size_plus_one_bytes_are_written() throws Exception {
    final byte[] expected = Arrays.copyOf(PAYLOAD, BLOCK_SIZE);
    subject.write(expected);
    subject.write(1);
    verify(writer).write(block.capture());
    assertArrayEquals(expected, block.getValue());
  }

  @Test
  public void writes_double_and_a_bit_bytes_divided() throws Exception {
    final int factor = 4;
    final byte[] data = Arrays.copyOf(PAYLOAD, BLOCK_SIZE * factor);
    subject.write(data);
    subject.write(1);
    verify(writer, times(factor)).write(block.capture());
    int offset = 0;
    for (final byte[] each : block.getAllValues()) {
      final byte[] expected = Arrays.copyOfRange(data, offset, offset + BLOCK_SIZE);
      assertArrayEquals(expected, each);
      offset += BLOCK_SIZE;
    }
    subject.close();
  }

  @Test
  public void flushes_partial_buffer_on_close() throws Exception {
    final byte[] expected = Arrays.copyOf(PAYLOAD, BLOCK_SIZE - 1);
    subject.write(expected);
    subject.close();
    verify(writer).write(block.capture());
    assertArrayEquals(expected, block.getValue());
  }

  // failures

  @Test(expected = IOException.class)
  public void propagates_exceptions() throws Exception {
    final byte[] data = Arrays.copyOf(PAYLOAD, BLOCK_SIZE + 1);
    final PipeIOException error = new PipeIOException();
    doThrow(error).when(writer).write(any());
    try {
      subject.write(data);
    } catch (final IOException e) {
      assertEquals(error, e.getCause());
      throw e;
    }
  }

  @Test(expected = IOException.class)
  public void propagates_termination() throws Exception {
    final byte[] data = Arrays.copyOf(PAYLOAD, BLOCK_SIZE + 1);
    final PipeTerminatedException error = new PipeTerminatedException("");
    doThrow(error).when(writer).write(any());
    try {
      subject.write(data);
    } catch (final IOException e) {
      assertEquals(error, e.getCause());
      throw e;
    }
  }
}
