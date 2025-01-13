all: Java-units.1.89.J01.dist

%.thumb.png: %.png
	convert -scale 150x150 $< $@

UNITS_IN_JAVA_OUTPUT=../app/src/main/java/net/sourceforge/unitsinjava/
%.dist: %.tar.gz
	mkdir $@
	tar -xvf $< --strip-components=1 -C $@
	# Do some bulk edits. 
	# Make all classes public, then change package name.
	# Replace String.isEmpty() with String.length() == 0, to preserve compatibility with pre-Gingerbread Android.
	sed -r -i -e 's/^(\s*)class/\1 public class/; s/^(\s*(abstract\s*)?)static/\1public static/; s/^(\s*)package units/\1package net.sourceforge.unitsinjava/; s/^(\s*import )units\./\1net.sourceforge.unitsinjava./; s/^(.*class.*(extends|implements) )units\./\1net.sourceforge.unitsinjava./; s/^(\s*protected )units\./\1net.sourceforge.unitsinjava./; /[rl]hs\([0-9]+\)/!{ s/\.isEmpty\(\)/\.length\(\) == 0/ }' $@/src/*.java
	cd $@/src/ && rm manifest.txt UnitsWindow.java GUI.java Browser.java applet.java convert.java
	mv $@/src/units.dat ../app/src/main/assets/
	rm -rf $(UNITS_IN_JAVA_OUTPUT)
	mkdir -p $(UNITS_IN_JAVA_OUTPUT)
	cp -a -t $(UNITS_IN_JAVA_OUTPUT) $@/src/* 
	rm -rf $@
