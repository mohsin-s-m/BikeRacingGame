-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.android.gms.ads.** <methods>;
}
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.badlogic.gdx.** { *; }
-keep class com.badlogic.gdx.physics.box2d.** { *; }
-dontwarn com.badlogic.gdx.**
-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
   <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}
-keepclassmembers class com.badlogic.gdx.physics.box2d.World {
   boolean contactFilter(long, long);
   void beginContact(long);
   void endContact(long);
   void preSolve(long, long);
   void postSolve(long, long);
   boolean reportFixture(long);
   float reportRayFixture(long, float, float, float, float, float);
}
