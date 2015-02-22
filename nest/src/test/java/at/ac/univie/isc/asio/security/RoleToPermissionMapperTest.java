package at.ac.univie.isc.asio.security;

import com.google.common.collect.Sets;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class RoleToPermissionMapperTest {
  private final RoleToPermissionMapper subject = RoleToPermissionMapper.instance();

  @DataPoints
  public static Role[] roles = Role.values();

  @Theory
  public void maps_role_name_to_granted_permissions(final Role role) throws Exception {
    final Set<Permission> actual =
        subject.getGrantedAuthorities(Arrays.asList(role.name()));
    assertThat(actual, is(role.getGrantedAuthorities()));
  }

  @Theory
  public void maps_role_authority_to_granted_permissions(final Role role) throws Exception {
    final Set<Permission> actual =
        subject.getGrantedAuthorities(Arrays.asList(role.getAuthority()));
    assertThat(actual, is(role.getGrantedAuthorities()));
  }

  @Theory
  public void maps_two_roles_to_union_of_granted_permissions(final Role one, final Role two) {
    final Set<Permission> union =
        Sets.union(one.getGrantedAuthorities(), two.getGrantedAuthorities());
    final Set<Permission> actual =
        subject.getGrantedAuthorities(Arrays.asList(one.getAuthority(), two.name()));
    assertThat(actual, is(union));
  }

  @Theory
  public void maps_role_container_to_contained(final Role container) {
    final Object[] expected = container.getGrantedAuthorities().toArray();
    final Collection<? extends GrantedAuthority> actual =
        subject.mapAuthorities(Arrays.asList(container));
    assertThat(actual, containsInAnyOrder(expected));
  }

  @Theory
  public void maps_multiple_role_container_to_union(final Role one, final Role two) {
    final Object[] expected =
        Sets.union(one.getGrantedAuthorities(), two.getGrantedAuthorities()).toArray();
    final Collection<? extends GrantedAuthority> actual =
        subject.mapAuthorities(Arrays.asList(one, two));
    assertThat(actual, containsInAnyOrder(expected));
  }
}
