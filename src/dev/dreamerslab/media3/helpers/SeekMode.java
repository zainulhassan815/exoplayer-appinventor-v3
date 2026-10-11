package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum SeekMode implements OptionList<String> {
  /** Seeks to exactly the requested position. */
  Exact("Exact"),
  /** Seeks to the nearest keyframe, which is faster but less precise. */
  ClosestKeyframe("Closest keyframe");

  private static final Map<String, SeekMode> lookup = new HashMap<>();

  static {
    for (SeekMode option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  SeekMode(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static SeekMode fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
