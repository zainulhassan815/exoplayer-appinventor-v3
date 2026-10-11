package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum FullscreenOrientation implements OptionList<String> {
  /** Landscape for landscape videos and portrait for portrait videos. */
  Auto("Auto"),
  /** Always landscape, either way up. */
  Landscape("Landscape"),
  /** Always portrait, either way up. */
  Portrait("Portrait"),
  /** Follows how the device is held. */
  Sensor("Sensor");

  private static final Map<String, FullscreenOrientation> lookup = new HashMap<>();

  static {
    for (FullscreenOrientation option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  FullscreenOrientation(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static FullscreenOrientation fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
