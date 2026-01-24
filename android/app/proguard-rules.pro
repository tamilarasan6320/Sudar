# Flutter ProGuard Rules
# Keep Flutter classes
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }

# Keep in_app_update classes
-keep class com.google.android.play.core.** { *; }

# Keep package_info_plus
-keep class dev.fluttercommunity.plus.** { *; }

# Keep OneSignal
-keep class com.onesignal.** { *; }

# Prevent stripping of native libraries
-keep class **.R$* { *; }

# Keep annotations
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Suppress warnings
-dontwarn com.google.android.play.core.**
-dontwarn io.flutter.embedding.**

# Razorpay SDK
-keep class com.razorpay.** { *; }
-keepclassmembers class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# Truecaller SDK
-keep class com.truecaller.android.sdk.** { *; }
-keepclassmembers class com.truecaller.android.sdk.** { *; }
-dontwarn com.truecaller.android.sdk.**

# Facebook SDK (Meta App Events)
-keep class com.facebook.** { *; }
-keepclassmembers class com.facebook.** { *; }
-dontwarn com.facebook.**
-keep class com.facebook.appevents.** { *; }
-keepclassmembers class com.facebook.appevents.** { *; }
-keep class com.facebook.FacebookContentProvider { *; }
-keep class com.facebook.CampaignTrackingReceiver { *; }
-keep class com.facebook.internal.** { *; }

# Install Referrer (required for Facebook install tracking)
-keep class com.android.installreferrer.** { *; }
-keep class com.google.android.finsky.** { *; }
