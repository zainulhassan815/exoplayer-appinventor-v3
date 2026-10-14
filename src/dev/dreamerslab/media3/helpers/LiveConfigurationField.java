package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum LiveConfigurationField implements OptionList<String> {
  /** Milliseconds behind the live edge to play at. Overrides the player's TargetLiveOffset. */
  TargetOffsetMs("targetOffsetMs"),
  /** The smallest offset from the live edge the player may adjust to, in milliseconds. */
  MinOffsetMs("minOffsetMs"),
  /** The largest offset from the live edge the player may adjust to, in milliseconds. */
  MaxOffsetMs("maxOffsetMs"),
  /** The slowest speed the player may use to stay at the target offset, like 0.97. */
  MinPlaybackSpeed("minPlaybackSpeed"),
  /** The fastest speed the player may use to stay at the target offset, like 1.03. */
  MaxPlaybackSpeed("maxPlaybackSpeed");

  private static final Map<String, LiveConfigurationField> lookup = new HashMap<>();

  static {
    for (LiveConfigurationField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  LiveConfigurationField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static LiveConfigurationField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
