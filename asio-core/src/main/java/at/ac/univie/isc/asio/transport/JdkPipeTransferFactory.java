package at.ac.univie.isc.asio.transport;

import com.google.common.base.Supplier;

public class JdkPipeTransferFactory implements Supplier<Transfer> {

  @Override
  public Transfer get() {
    return new JdkPipeTransfer();
  }
}
