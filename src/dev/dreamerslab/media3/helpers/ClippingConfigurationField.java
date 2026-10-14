package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum ClippingConfigurationField implements OptionList<String> {
  /** Where playback starts, in milliseconds. */
  StartPositionMs("startPositionMs"),
  /** Where playback ends, in milliseconds. 0 plays to the end. */
  EndPositionMs("endPositionMs");

  private static final Map<String, ClippingConfigurationField> lookup = new HashMap<>();

  static {
    for (ClippingConfigurationField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  ClippingConfigurationField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static ClippingConfigurationField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
