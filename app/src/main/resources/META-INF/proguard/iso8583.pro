# iso8583 consumer keep rules (plan T16).
# R8/ProGuard in Android apps apply META-INF/proguard/*.pro from dependency jars automatically;
# JVM services ignore this file.

# Field codecs are instantiated by class name from JSON layouts
# (GenericJSONPackager.setFieldsFromJson → Class.forName("com.base.iso.fields." + class)).
-keep class com.base.iso.fields.** {
    public <init>(...);
    public void setPad(boolean);
}

# Packagers are instantiated by class name in ISOMsg.readPackager (Externalizable).
-keep class * implements com.base.iso.interfaces.iPackager {
    public <init>();
}
