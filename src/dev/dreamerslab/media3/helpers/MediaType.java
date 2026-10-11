package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum MediaType implements OptionList<String> {
  /** Detects the format from the URL. */
  Auto("Auto"),
  /** HTTP Live Streaming (.m3u8). */
  Hls("HLS"),
  /** MPEG-DASH (.mpd). */
  Dash("DASH"),
  /** Microsoft Smooth Streaming (.ism). */
  SmoothStreaming("SmoothStreaming"),
  /** Real Time Streaming Protocol, used by IP cameras. */
  Rtsp("RTSP"),
  /** A single file, such as MP4, MKV, WebM or MP3. */
  Progressive("Progressive");

  private static final Map<String, MediaType> lookup = new HashMap<>();

  static {
    for (MediaType option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  MediaType(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static MediaType fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
