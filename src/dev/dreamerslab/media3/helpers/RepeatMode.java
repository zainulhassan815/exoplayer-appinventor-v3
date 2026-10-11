package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum RepeatMode implements OptionList<String> {
  /** Stops at the end of the queue. */
  Off("Off"),
  /** Repeats the current media. */
  One("One"),
  /** Repeats the whole queue. */
  All("All");

  private static final Map<String, RepeatMode> lookup = new HashMap<>();

  static {
    for (RepeatMode option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  RepeatMode(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static RepeatMode fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
