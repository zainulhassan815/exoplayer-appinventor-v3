package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum ResizeMode implements OptionList<String> {
  /** Shows the whole video, with black bars where its shape differs from the player. */
  Fit("Fit"),
  /** Stretches the video to fill the player, distorting it if the shapes differ. */
  Fill("Fill"),
  /** Fills the player without distortion, cropping the edges of the video. */
  Zoom("Zoom"),
  /** Uses the full width of the player and adjusts the height to the video. */
  FixedWidth("Fixed width"),
  /** Uses the full height of the player and adjusts the width to the video. */
  FixedHeight("Fixed height");

  private static final Map<String, ResizeMode> lookup = new HashMap<>();

  static {
    for (ResizeMode option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  ResizeMode(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static ResizeMode fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
