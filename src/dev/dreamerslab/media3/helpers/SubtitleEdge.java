package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum SubtitleEdge implements OptionList<String> {
  /** No edge around the text. */
  None("None"),
  /** A thin outline around each letter. */
  Outline("Outline"),
  /** A soft shadow below the text. */
  DropShadow("Drop shadow"),
  /** Letters look raised from the video. */
  Raised("Raised"),
  /** Letters look pressed into the video. */
  Depressed("Depressed");

  private static final Map<String, SubtitleEdge> lookup = new HashMap<>();

  static {
    for (SubtitleEdge option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  SubtitleEdge(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static SubtitleEdge fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
