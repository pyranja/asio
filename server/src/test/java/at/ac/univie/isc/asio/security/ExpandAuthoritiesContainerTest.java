package at.ac.univie.isc.asio.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class ExpandAuthoritiesContainerTest {
  private final ExpandAuthoritiesContainer subject = ExpandAuthoritiesContainer.instance();

  @DataPoints
  public static Role[] roles = Role.values();

  @Theory
  public void maps_role_container_to_contained(final Role container) {
    final Object[] expected =
        Lists.asList(container, container.getGrantedAuthorities().toArray()).toArray();
    final Collection<? extends GrantedAuthority> actual =
        subject.mapAuthorities(Arrays.asList(container));
    assertThat(actual, containsInAnyOrder(expected));
  }

  @Theory
  public void maps_multiple_role_container_to_union(final Role one, final Role two) {
    final HashSet<GrantedAuthority> union = Sets.newHashSet();
    union.add(one);
    union.addAll(one.getGrantedAuthorities());
    union.add(two);
    union.addAll(two.getGrantedAuthorities());
    final Object[] expected = union.toArray();
    final Collection<? extends GrantedAuthority> actual =
        subject.mapAuthorities(Arrays.asList(one, two));
    assertThat(actual, containsInAnyOrder(expected));
  }
}
