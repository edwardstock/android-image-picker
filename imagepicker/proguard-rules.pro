-dontobfuscate

# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions, InnerClasses
-keepattributes EnclosingMethod

-keep class com.esafirm.imagepicker.** { *; }