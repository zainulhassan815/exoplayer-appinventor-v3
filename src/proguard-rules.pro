# Prevents extension classes (annotated with `@Extension`) from being removed, renamed or repackged.
-keep @com.google.appinventor.components.annotations.Extension public class * {
  public *;
}

# Prevents helper classes from being removed, renamed or repackaged
-keepnames class * implements com.google.appinventor.components.common.OptionList {
  *;
}
