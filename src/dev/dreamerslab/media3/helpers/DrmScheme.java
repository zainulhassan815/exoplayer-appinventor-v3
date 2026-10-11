package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum DrmScheme implements OptionList<String> {
  /** Google Widevine, supported on almost all Android devices. */
  Widevine("Widevine"),
  /** W3C Clear Key, for content without a hardware-backed license. */
  ClearKey("ClearKey"),
  /** Microsoft PlayReady, supported on some devices. */
  PlayReady("PlayReady");

  private static final Map<String, DrmScheme> lookup = new HashMap<>();

  static {
    for (DrmScheme option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  DrmScheme(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static DrmScheme fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
