# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/azamlerc/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-verbose
-dontobfuscate
-dontshrink
-dontwarn java.lang.invoke.*
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn java.nio.file.*

-keep public class * {
    public protected *;
}

-assumenosideeffects class android.util.Log {
  public static *** i(...);
  public static *** d(...);
  public static *** v(...);
}