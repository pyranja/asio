package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Id;
import com.google.common.io.ByteSource;

public interface Assembler {
  Container assemble(Id name, ByteSource source);
}
