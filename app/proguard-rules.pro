# Views are instantiated by name from the layout XML, so they must keep both
# their name and their AttributeSet constructor. The stock Android rules only
# cover *public* View subclasses, and ColorButton is package-private.
-keep class * extends android.view.View {
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
}

# The units-in-java engine is wired up by assigning to public static fields on
# Env (see Application.initUnits) and read back from the .dat parser.
-keep class net.sourceforge.unitsinjava.Env { *; }
-keep class net.sourceforge.unitsinjava.Env$* { *; }
