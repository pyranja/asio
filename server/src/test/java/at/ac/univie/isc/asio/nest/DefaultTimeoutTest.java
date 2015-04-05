package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.tool.Timeout;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DefaultTimeoutTest {
  static final Timeout FALLBACK = Timeout.from(30, TimeUnit.SECONDS);
  private final NestConfig config = NestConfig.empty();

  private final DefaultTimeout subject = new DefaultTimeout(FALLBACK);

  @Test
  public void should_keep_configured_explicit_timeout() throws Exception {
    final Timeout configured = Timeout.from(100, TimeUnit.MILLISECONDS);
    assert !configured.equals(FALLBACK) : "illegal test definition";
    config.getDataset().setTimeout(configured);
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(configured));
  }

  @Test
  public void should_override_undefined_timeout() throws Exception {
    config.getDataset().setTimeout(Timeout.undefined());
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(FALLBACK));
  }

  @Test
  public void should_override_missing_timeout() throws Exception {
    config.getDataset().setTimeout(null);
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(FALLBACK));
  }
}
