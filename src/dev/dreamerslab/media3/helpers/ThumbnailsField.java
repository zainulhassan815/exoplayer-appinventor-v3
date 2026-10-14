package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum ThumbnailsField implements OptionList<String> {
  /** Required. URL or asset name of a WebVTT thumbnail track (like storyboard.vtt), or of a sprite sheet with Columns and Rows. */
  Uri("uri"),
  /** For a sprite sheet: the number of thumbnails across. */
  Columns("columns"),
  /** For a sprite sheet: the number of thumbnails down. */
  Rows("rows"),
  /** For a sprite sheet: milliseconds between thumbnails. */
  IntervalMs("intervalMs");

  private static final Map<String, ThumbnailsField> lookup = new HashMap<>();

  static {
    for (ThumbnailsField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  ThumbnailsField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static ThumbnailsField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
