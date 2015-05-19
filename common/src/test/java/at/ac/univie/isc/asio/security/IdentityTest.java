/*
 * #%L
 * asio common
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
        .addEqualityGroup(Identity.from("anonymous", "")) // use special undefined name
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
  public void undefined_name_is_special__anonymous__() throws Exception {
    assertThat(Identity.undefined().getName(), is("anonymous"));
  }

  @Test
  public void cannot_get_secret_from_undefined() throws Exception {
    error.expect(Identity.UndefinedIdentity.class);
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
