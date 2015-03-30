package at.ac.univie.isc.asio.tool;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.net.URI;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings({"unchecked", "UnusedDeclaration"})
public class BeanToMapTest {

  private final BeanToMap subject = BeanToMap.noPrefix();
  private Map<String, Object> result;

  @Test
  public void should_yield_empty_map_for_empty_bean() throws Exception {
    class Empty {}
    result = subject.convert(new Empty());
    assertThat(result.values(), empty());
  }

  @Test
  public void should_yield_map_with_string_properties() throws Exception {
    class StringBean {
      public String getFirst() { return "first"; }
      public String getSecond() { return "second"; }
    }
    result = subject.convert(new StringBean());
    assertThat(result.values(), Matchers.<Object>hasItems("first", "second"));
  }

  @Test
  public void should_ignore_non_getter_methods() throws Exception {
    class BeanWithoutProperty {
      public String notAGetter() { return "not-a-property"; }
    }
    result = subject.convert(new BeanWithoutProperty());
    assertThat(result.values(), empty());
  }

  @Test
  public void should_use_bean_property_name_as_map_key() throws Exception {
    class BeanWithProperty {
      public String getProperty() { return "value"; }
    }
    result = subject.convert(new BeanWithProperty());
    assertThat(result.keySet(), contains("property"));
  }

  @Test
  public void should_retain_camel_casing_in_keys() throws Exception {
    class BeanWithCamelCasedProperty {
      public String getPropertyWithCamelCase() { return "value"; }
    }
    result = subject.convert(new BeanWithCamelCasedProperty());
    assertThat(result.keySet(), contains("propertyWithCamelCase"));
  }

  @Test
  public void should_omit_null_values() throws Exception {
    class BeanWithNullValue {
      public String getNullProperty() { return null; }
    }
    result = subject.convert(new BeanWithNullValue());
    assertThat(result.values(), empty());
  }

  @Test
  public void should_include_primitive_properties() throws Exception {
    class BeanWithPrimitives {
      public byte getByteProperty() { return Byte.MAX_VALUE; }
      public short getShortProperty() { return Short.MAX_VALUE; }
      public int getIntProperty() { return Integer.MAX_VALUE; }
      public long getLongProperty() { return Long.MAX_VALUE; }
      public float getFloatProperty() { return Float.MAX_VALUE; }
      public double getDoubleProperty() { return Double.MAX_VALUE; }
      public boolean getBooleanProperty() { return Boolean.TRUE; }
      public char getCharProperty() { return Character.MAX_VALUE; }
    }
    result = subject.convert(new BeanWithPrimitives());
    assertThat(result, allOf(
        has("byteProperty", Byte.MAX_VALUE),
        has("shortProperty", Short.MAX_VALUE),
        has("intProperty", Integer.MAX_VALUE),
        has("longProperty", Long.MAX_VALUE),
        has("floatProperty", Float.MAX_VALUE),
        has("doubleProperty", Double.MAX_VALUE),
        has("booleanProperty", Boolean.TRUE),
        has("charProperty", Character.MAX_VALUE)
    ));
  }

  @Test
  public void should_include_primitive_wrappers() throws Exception {
    class BeanWithPrimitiveWrappers {
      public Byte getByteProperty() { return Byte.MAX_VALUE; }
      public Short getShortProperty() { return Short.MAX_VALUE; }
      public Integer getIntProperty() { return Integer.MAX_VALUE; }
      public Long getLongProperty() { return Long.MAX_VALUE; }
      public Float getFloatProperty() { return Float.MAX_VALUE; }
      public Double getDoubleProperty() { return Double.MAX_VALUE; }
      public Boolean getBooleanProperty() { return Boolean.TRUE; }
      public Character getCharProperty() { return Character.MAX_VALUE; }
    }
    result = subject.convert(new BeanWithPrimitiveWrappers());
    assertThat(result, allOf(
        has("byteProperty", Byte.MAX_VALUE),
        has("shortProperty", Short.MAX_VALUE),
        has("intProperty", Integer.MAX_VALUE),
        has("longProperty", Long.MAX_VALUE),
        has("floatProperty", Float.MAX_VALUE),
        has("doubleProperty", Double.MAX_VALUE),
        has("booleanProperty", Boolean.TRUE),
        has("charProperty", Character.MAX_VALUE)
    ));
  }

  @Test
  public void should_include__is__getter_for_booleans() throws Exception {
    class BeanWithIsGetter {
      public boolean isPrimitiveBoolean() { return true; }
      public Boolean isWrapperBoolean() { return true; }
    }
    result = subject.convert(new BeanWithIsGetter());
    assertThat(result, allOf(
        has("primitiveBoolean", true),
        has("wrapperBoolean", true)
    ));
  }

  @Test
  public void should_exclude_complex_properties() throws Exception {
    class NonPrimitive {}
    class BeanWithComplexProperty {
      public NonPrimitive getComplexProperty() { return new NonPrimitive(); }
    }
    result = subject.convert(new BeanWithComplexProperty());
    assertThat(result.values(), empty());
  }

  @Test
  public void should_exclude_non_public_properties() throws Exception {
    class BeanWithHiddenProperties {
      private String getPrivateProperty() { return "private"; }
      protected String getProtectedProperty() { return "protected"; }
      String getPackagePrivateProperty() { return "package-private"; }
      public String getPublicProperty() { return "public"; }
    }
    result = subject.convert(new BeanWithHiddenProperties());
    assertThat(result, has("publicProperty", "public"));
    assertThat(result.keySet(), contains("publicProperty"));
    assertThat(result.values(), Matchers.<Object>contains("public"));
  }

  @Test
  public void should_exclude_void_methods_that_look_like_getters() throws Exception {
    class BeanWithFakeVoidProperty {
      public void getFakeProperty() {}
    }
    result = subject.convert(new BeanWithFakeVoidProperty());
    assertThat(result.values(), empty());
  }

  @Test
  public void should_include_nested_bean() throws Exception {
    class InnerBean {
      public String getNestedProperty() { return "nested-value"; }
    }
    class OuterBean {
      public InnerBean getNestedBean() { return new InnerBean(); }
    }
    result = subject.convert(new OuterBean());
    assertThat(result, has("nestedBean.nestedProperty", "nested-value"));
  }

  @Test
  public void should_omit_null_nested_bean() throws Exception {
    class InnerBean {
      public String getNestedProperty() { return "nested-value"; }
    }
    class OuterWithNullNested {
      public InnerBean getNestedBean() { return null; }
    }
    result = subject.convert(new OuterWithNullNested());
    assertThat(result.values(), empty());
  }

  @Test
  public void should_use_global_prefix_if_given() throws Exception {
    class InnerBean {
      public String getProperty() { return "value"; }
    }
    class OuterBean {
      public String getProperty() { return "value"; }
      public InnerBean getNested() { return new InnerBean(); }
    }
    final BeanToMap prefixed = BeanToMap.withPrefix("prefix");
    result = prefixed.convert(new OuterBean());
    assertThat(result, allOf(
        has("prefix.property", "value"),
        has("prefix.nested.property", "value")
    ));
  }

  @Test
  public void should_include_inherited_properties() throws Exception {
    class ParentBean {
      public String getParentProperty() { return "parent"; }
    }
    class ChildBean extends ParentBean {
      public String getChildProperty() { return "child"; }
    }
    result = subject.convert(new ChildBean());
    assertThat(result, has("childProperty", "child"));
    assertThat(result, has("parentProperty", "parent"));
  }

  @Test(expected = BeanToMap.ReflectiveConversionFailure.class)
  public void should_detect_circular_reference() throws Exception {
    class Holder {
      Object held;
      public Object getHeld() { return held; }
    }
    final Holder first = new Holder();
    final Holder second = new Holder();
    first.held = second;
    second.held = first;
    result = subject.convert(first);
  }

  static enum TestEnum {
    TEST;
  }

  @Test
  public void should_map_string_convertible_properties_as_string() throws Exception {
    class BeanWithStringConvertibles {
      public URI getUriProperty() { return URI.create("asio://test"); }
      public TypedValue<String> getTypedValueProperty() { return new TypedValue<String>("typed-value") {}; }
      public TestEnum getEnumProperty() { return TestEnum.TEST; }
      public CharSequence getCharSequenceProperty() { return new StringBuilder("char-sequence"); }
    }
    result = subject.convert(new BeanWithStringConvertibles());
    assertThat(result, allOf(
        has("uriProperty", "asio://test"),
        has("typedValueProperty", "typed-value"),
        has("enumProperty", TestEnum.TEST.toString()),
        has("charSequenceProperty", "char-sequence")
    ));
  }

  @Test
  public void should_map_class_property_to_class_name() throws Exception {
    class BeanWithClassProperty {
      public Class<?> getClassProperty() { return String.class; }
    }
    result = subject.convert(new BeanWithClassProperty());
    assertThat(result, has("classProperty", "java.lang.String"));
  }

  @Test(expected = BeanToMap.ReflectiveConversionFailure.class)
  public void should_fail_on_array_property() throws Exception {
    class BeanWithArray {
      public String[] getArrayProperty() { return new String[] { "element" }; }
    }
    subject.convert(new BeanWithArray());
  }

  @Test
  public void property_descriptor_finds_read_only_properties() throws Exception {
    class ReadOnlyBean {
      public String getStringProperty() { return "test"; }
    }
    final PropertyDescriptor[] properties = Introspector
        .getBeanInfo(ReadOnlyBean.class, Object.class).getPropertyDescriptors();
    assertThat(properties, arrayWithSize(1));
    assertThat(properties[0].getPropertyType(), Matchers.<Class<?>>equalTo(String.class));
    assertThat(properties[0].getName(), equalTo("stringProperty"));
  }

  private static Matcher<Map<? extends String, ?>> has(final String key, final Object value) {
    return Matchers.hasEntry(key, value);
  }
}
