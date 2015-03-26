package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.tool.TimeoutSpec;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DefaultTimeoutTest {
  static final TimeoutSpec FALLBACK = TimeoutSpec.from(30, TimeUnit.SECONDS);
  private final NestConfig config = NestConfig.empty();

  private final DefaultTimeout subject = new DefaultTimeout(FALLBACK);

  @Test
  public void should_keep_configured_explicit_timeout() throws Exception {
    final TimeoutSpec configured = TimeoutSpec.from(100, TimeUnit.MILLISECONDS);
    assert !configured.equals(FALLBACK) : "illegal test definition";
    config.getDataset().setTimeout(configured);
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(configured));
  }

  @Test
  public void should_override_undefined_timeout() throws Exception {
    config.getDataset().setTimeout(TimeoutSpec.undefined());
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
