package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import com.google.common.io.ByteSource;

public interface Assembler {
  Container assemble(Schema name, ByteSource source);
}
