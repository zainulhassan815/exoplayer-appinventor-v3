package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum SubtitleConfigurationField implements OptionList<String> {
  /** Required. URL or asset name of the subtitle file (WebVTT, SRT, TTML or SSA). */
  Uri("uri"),
  /** A language code like "en". */
  Language("language"),
  /** The name shown in the subtitles menu. */
  Label("label");

  private static final Map<String, SubtitleConfigurationField> lookup = new HashMap<>();

  static {
    for (SubtitleConfigurationField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  SubtitleConfigurationField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static SubtitleConfigurationField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
