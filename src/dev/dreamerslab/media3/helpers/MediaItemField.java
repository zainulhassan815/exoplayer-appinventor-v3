package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum MediaItemField implements OptionList<String> {
  /** Required. URL or asset name of the media. */
  Uri("uri"),
  /** Text shown as the media's title. */
  Title("title"),
  /** Text shown as the media's artist. */
  Artist("artist"),
  /** Text describing the media, shown in the media notification. */
  Description("description"),
  /** URL or asset name of an image for the media, shown in the media notification. */
  ArtworkUri("artworkUri"),
  /** A MediaType: the format, for URLs that don't end in a known extension. */
  MimeType("mimeType"),
  /** A dictionary with DrmConfigurationField keys: plays the media with DRM. */
  DrmConfiguration("drmConfiguration"),
  /** A list of dictionaries with SubtitleConfigurationField keys: subtitle files for the media. */
  SubtitleConfigurations("subtitleConfigurations"),
  /** A dictionary with ClippingConfigurationField keys: plays only part of the media. */
  ClippingConfiguration("clippingConfiguration"),
  /** A dictionary with ThumbnailsField keys: thumbnails shown while scrubbing. */
  Thumbnails("thumbnails");

  private static final Map<String, MediaItemField> lookup = new HashMap<>();

  static {
    for (MediaItemField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  MediaItemField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static MediaItemField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
