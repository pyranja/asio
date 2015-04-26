package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.security.Identity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class InjectJdbcCredentialsTest {

  private MysqlUserRepository repository = Mockito.mock(MysqlUserRepository.class);
  private final InjectJdbcCredentials subject = new InjectJdbcCredentials(repository);
  private final NestConfig input = NestConfig.empty();

  @Before
  public void prepare() throws Exception {
    given(repository.createUserFor("test")).willReturn(Identity.from("username", "password"));
    input.getJdbc().setSchema("test");
  }

  @Test
  public void should_keep_dataset() throws Exception {
    final NestConfig processed = subject.apply(input);
    assertThat(processed.getDataset(), sameInstance(input.getDataset()));
  }

  @Test
  public void should_not_change_non_credential_settings() throws Exception {
    final Jdbc processed = subject.apply(input).getJdbc();
    assertThat(processed.getProperties().values(), empty());
    assertThat(processed.getUrl(), nullValue());
    assertThat(processed.getDriver(), nullValue());
    assertThat(processed.getSchema(), equalTo("test"));
  }

  @Test
  public void should_create_user_for_schema() throws Exception {
    subject.apply(input);
    verify(repository).createUserFor("test");
  }
  
  @Test
  public void should_replace_jdbc_credentials_with_schema_specific() throws Exception {
    final Jdbc processed = subject.apply(input).getJdbc();
    assertThat(processed.getUsername(), equalTo("username"));
    assertThat(processed.getPassword(), equalTo("password"));
  }

  @Test
  public void should_drop_user_on_close() throws Exception {
    subject.cleanUp(input);
    verify(repository).dropUserOf("test");
  }
}
