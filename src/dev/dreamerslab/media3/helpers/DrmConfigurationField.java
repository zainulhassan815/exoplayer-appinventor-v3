package dev.dreamerslab.media3.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum DrmConfigurationField implements OptionList<String> {
  /** A DrmScheme. Widevine when missing. */
  Scheme("scheme"),
  /** URL of the license server. */
  LicenseUri("licenseUri"),
  /** A dictionary of HTTP headers sent with license requests. */
  LicenseRequestHeaders("licenseRequestHeaders"),
  /** A dictionary of key IDs to keys, each 32 hex digits: plays ClearKey content without a license server. */
  ClearKeys("clearKeys");

  private static final Map<String, DrmConfigurationField> lookup = new HashMap<>();

  static {
    for (DrmConfigurationField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  DrmConfigurationField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static DrmConfigurationField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
