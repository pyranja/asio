package at.ac.univie.isc.asio.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Convert plain java beans to flattened {@link java.util.Map maps} containing all of their
 * properties.
 * <p>
 * The set of a bean's properties consists of all methods on the bean, which...
 * <ul>
 * <li>are {@code public}</li>
 * <li>have no argument</li>
 * <li>have a name starting with {@code get*}, or {@code is*} in case of a boolean property</li>
 * <li>are returning a primitive type, a wrapper of a primitive type or a {@code String}</li>
 * </ul>
 * The property name is the lower-camel-cased method name without the {@code get} or {@code is}
 * prefix.
 * If a method returns {@code null}, the property is omitted from the map.
 * </p>
 * <p>
 * A bean may have nested beans, if the return type of an otherwise matching method is not a
 * primitive (wrapper) or {@code String}.
 * Properties of nested beans are discovered and added in the same way as properties of top-level
 * beans, but each nested property name is prefixed with the name of the nested bean getter.
 * </p>
 * <p>
 * <strong>Note:</strong>This implementation is <strong>NOT</strong> thread-safe.
 * </p>
 */
@NotThreadSafe
public final class BeanToMap {

  public static final Collection<Class<?>> SIMPLE_PROPERTY_TYPES = ImmutableSet.<Class<?>>builder()
      .add(byte.class, short.class, int.class, long.class, float.class, double.class, boolean.class, char.class)
      .add(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class, Character.class)
      .add(String.class).build();

  public static final Collection<Class<?>> STRING_CONVERTIBLE_TYPES = ImmutableSet.<Class<?>>builder()
      .add(CharSequence.class, Enum.class, URI.class, TypedValue.class).build();

  private static final Joiner PROPERTY_PATH_JOINER = Joiner.on('.');

  public static BeanToMap noPrefix() {
    return new BeanToMap(null);
  }

  public static BeanToMap withPrefix(final String prefix) {
    requireNonNull(prefix);
    return new BeanToMap(prefix);
  }

  private final ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();
  private final Deque<String> path = new ArrayDeque<>();
  private final Set<Object> seen = new HashSet<>();

  private BeanToMap(final String prefix) {
    if (prefix != null) {
      path.push(prefix);
    }
  }

  public Map<String, Object> convert(final Object bean) {
    requireNonNull(bean, "cannot convert <null>");
    try {
      handleBean(bean);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new ReflectiveConversionFailure(bean, e);
    }
    return properties.build();
  }

  private void handleBean(final Object bean) throws InvocationTargetException, IllegalAccessException {
    detectCircularReference(bean);
    for (final BeanProperty property : BeanProperty.propertiesOf(bean)) {
      final Object value = property.getter().invoke(bean);
      if (value != null) {
        path.push(property.name());
        handleProperty(value);
        path.pop();
      }
    }
  }

  private void handleProperty(final Object value) throws InvocationTargetException, IllegalAccessException {
    final Class<?> type = value.getClass();
    if (SIMPLE_PROPERTY_TYPES.contains(type)) {
      properties.put(propertyPath(), value);
    } else if (isStringConvertible(type)) {
      properties.put(propertyPath(), Objects.toString(value));
    } else if (Class.class.isAssignableFrom(type)) {
      properties.put(propertyPath(), ((Class) value).getName());
    } else if (type.isArray()) {
      throw new ReflectiveConversionFailure(value, new UnsupportedOperationException("cannot serialize arrays"));
    } else {
      handleBean(value);
    }
  }

  private boolean isStringConvertible(final Class<?> type) {
    for (Class<?> any : STRING_CONVERTIBLE_TYPES) {
      if (any.isAssignableFrom(type)) {
        return true;
      }
    }
    return false;
  }

  private String propertyPath() {
    return PROPERTY_PATH_JOINER.join(path.descendingIterator());
  }

  private void detectCircularReference(final Object bean) {
    if (!seen.add(bean)) {
      throw new ReflectiveConversionFailure(bean, new IllegalArgumentException("circular reference"));
    }
  }

  public static class ReflectiveConversionFailure extends RuntimeException {
    public ReflectiveConversionFailure(final Object source, final Throwable cause) {
      super("failed to convert bean of type <" + source.getClass() + "> to map : "
          + cause.getMessage(), cause);
    }
  }
}
