package at.ac.univie.isc.asio.security;

import com.google.common.testing.EqualsTester;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class IdentityTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  @Test
  public void equals_hashcode_contract() throws Exception {
    new EqualsTester()
        .addEqualityGroup(Identity.undefined(), Identity.from(null, null), Identity.from("name", null))
        .addEqualityGroup(Identity.from("name", "password"), Identity.from("name", "password"))
        .addEqualityGroup(Identity.from("other-name", "other-password"))
        .testEquals()
    ;
  }

  @Test
  public void holds_given_name() throws Exception {
    final Identity subject = Identity.from("name", "secret");
    assertThat(subject.getName(), is("name"));
  }

  @Test
  public void holds_given_secret() throws Exception {
    final Identity subject = Identity.from("name", "secret");
    assertThat(subject.getSecret(), is("secret"));
  }

  @Test
  public void is_undefined_on_null_secret() throws Exception {
    final Identity subject = Identity.from("name", null);
    assertThat(subject.isDefined(), is(false));
  }

  @Test
  public void is_undefined_on_null_name() throws Exception {
    final Identity subject = Identity.from(null, "secret");
    assertThat(subject.isDefined(), is(false));
  }

  @Test
  public void cannot_get_name_from_undefined() throws Exception {
    error.expect(IllegalStateException.class);
    Identity.undefined().getName();
  }

  @Test
  public void cannot_get_secret_from_undefined() throws Exception {
    error.expect(IllegalStateException.class);
    Identity.undefined().getSecret();
  }

  @Test
  public void undefined_factory_yields_undefined_identity() throws Exception {
    assertThat(Identity.undefined().isDefined(), is(false));
  }

  @Test
  public void safe_getter_returns_actual_if_defined() throws Exception {
    final Identity subject = Identity.from("name", "secret");
    final Identity fallback = Identity.from("fallback", "other-secret");
    assertThat(subject.orIfUndefined(fallback), is(sameInstance(subject)));
  }

  @Test
  public void safe_getter_returns_fallback_if_undefined() throws Exception {
    final Identity fallback = Identity.from("fallback", "other-secret");
    assertThat(Identity.undefined().orIfUndefined(fallback), is(sameInstance(fallback)));
  }

  @Test
  public void safe_name_getter_returns_actual_name() throws Exception {
    final Identity subject = Identity.from("name", "secret");
    assertThat(subject.nameOrIfUndefined("fallback"), is("name"));
  }

  @Test
  public void safe_name_getter_returns_fallback_if_undefined() throws Exception {
    assertThat(Identity.undefined().nameOrIfUndefined("fallback"), is("fallback"));
  }
}
