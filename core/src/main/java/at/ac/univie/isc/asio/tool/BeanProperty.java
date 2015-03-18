package at.ac.univie.isc.asio.tool;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Describe a readable java bean property, consisting of a property name and a getter method.
 */
final class BeanProperty extends Pair<String, Method> {
  /**
   * Find all bean properties on given object.
   */
  static Collection<BeanProperty> propertiesOf(final Object bean) {
    requireNonNull(bean, "cannot find properties of <null>");
    return PROPERTY_FINDER_CACHE.getUnchecked(bean.getClass());
  }

  private static final LoadingCache<Class<?>, Collection<BeanProperty>> PROPERTY_FINDER_CACHE =
          CacheBuilder.newBuilder()
              .concurrencyLevel(4)
              .maximumWeight(500)
              .weigher(new CollectionSizeWeigher<Class<?>, Collection<BeanProperty>>())
              .build(new PropertyFinder());

  private static final Converter<String, String> DECAPITALIZER =
      CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

  private static List<BeanProperty> findAllProperties(final Class<?> clazz) {
    final ImmutableList.Builder<BeanProperty> found = ImmutableList.builder();
    for (Method method : clazz.getMethods()) {
      if (isProperty(method)) {
        found.add(new BeanProperty(propertyNameFor(method), method));
      }
    }
    return found.build();
  }

  private static String propertyNameFor(final Method method) {
    final String name = isBoolGetter(method)
        ? method.getName().substring(2)
        : method.getName().substring(3);
    return DECAPITALIZER.convert(name);
  }

  private static boolean isProperty(final Method method) {
    return Modifier.isPublic(method.getModifiers())       // is not a 'hidden' property
        && method.getDeclaringClass() != Object.class     // omit Object#getClass()
        && method.getParameterTypes().length == 0         // has no arguments
        && (isGetter(method) || isBoolGetter(method));    // method name satisfies bean conventions
  }

  private static boolean isGetter(final Method method) {
    return method.getReturnType() != void.class && method.getName().startsWith("get");
  }

  private static boolean isBoolGetter(final Method method) {
    return (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)
        && method.getName().startsWith("is");
  }

  private static class PropertyFinder extends CacheLoader<Class<?>, Collection<BeanProperty>> {
    @Override
    public Collection<BeanProperty> load(final Class<?> key) throws Exception {
      return findAllProperties(key);
    }
  }

  private static class CollectionSizeWeigher<ANY, COLLECTION extends Collection<?>>
      implements Weigher<ANY, COLLECTION> {
    @Override
    public int weigh(final ANY ignored, final COLLECTION value) {
      return value.size();
    }
  }

  private BeanProperty(@Nonnull final String s, @Nonnull final Method method) {
    super(s, method);
  }

  /**
   * Bean property name (excluding {@code get} or {@code is}) prefix.
   * @return the simple property name
   */
  public String name() {
    return first();
  }

  /**
   * The getter method of the property.
   * @return reference to the getter
   */
  public Method getter() {
    return second();
  }
}
