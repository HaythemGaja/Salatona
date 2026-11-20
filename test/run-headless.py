#!/usr/bin/env python3
"""Compile real core sources with minimal Android stand-ins, then run JVM tests.
These stand-ins only support core tests; they do not validate Android rendering.
Requires Python 3 and JDK 11+ (no Android SDK or third-party packages).
"""
import pathlib, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[1]
stubs = {
'android/content/Context.java': '''package android.content;
public class Context {
 public static final int MODE_PRIVATE=0;
 private final SharedPreferences store=new SharedPreferences();
 public SharedPreferences getSharedPreferences(String n,int m){return store;}
}''',
'android/content/SharedPreferences.java': '''package android.content;
import java.util.*;
public class SharedPreferences {
 private final Map<String,Object> m=new HashMap<>();
 public int getInt(String k,int d){return (Integer)m.getOrDefault(k,d);}
 public long getLong(String k,long d){return (Long)m.getOrDefault(k,d);}
 public boolean getBoolean(String k,boolean d){return (Boolean)m.getOrDefault(k,d);}
 public String getString(String k,String d){return (String)m.getOrDefault(k,d);}
 @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return (Set<String>)m.getOrDefault(k,d);}
 public Editor edit(){return new Editor();}
 public class Editor {
  public Editor putInt(String k,int v){m.put(k,v);return this;}
  public Editor putLong(String k,long v){m.put(k,v);return this;}
  public Editor putBoolean(String k,boolean v){m.put(k,v);return this;}
  public Editor putString(String k,String v){m.put(k,v);return this;}
  public Editor putStringSet(String k,Set<String>v){m.put(k,v);return this;}
  public void apply(){}
 }
}''',
'android/text/format/DateFormat.java': '''package android.text.format;
public class DateFormat {public static boolean is24HourFormat(android.content.Context c){return false;}}''',
'android/view/View.java':'package android.view; public class View {}',
'android/view/ViewGroup.java':'''package android.view; public class ViewGroup extends View {
public int getChildCount(){return 0;} public View getChildAt(int i){return null;}}''',
'android/widget/TextView.java':'''package android.widget; public class TextView extends android.view.View {
private CharSequence text=""; public CharSequence getText(){return text;}
public void setText(CharSequence s){text=s;} public void setTextLocale(java.util.Locale l){}
public void setLetterSpacing(float f){} }''',
'android/widget/EditText.java':'''package android.widget; public class EditText extends TextView {
public CharSequence getHint(){return null;} public void setHint(CharSequence s){}}'''
}
with tempfile.TemporaryDirectory(prefix='salatona-tests-') as tmp:
 tmp=pathlib.Path(tmp)
 sources=[]
 for name,text in stubs.items():
  p=tmp/name; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text); sources.append(str(p))
 for name in ['PrayerTimes','PrayerCalc','Daylight','Prefs','Lang','Cities','RamadanWindow']:
  sources.append(str(root/'app/java/com/leo/salah'/f'{name}.java'))
 sources += [str(root/'test/DstTest.java'),str(root/'test/headless/CoreRegressionTest.java')]
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',str(tmp),*sources],check=True)
 for test in ['DstTest','CoreRegressionTest']:
  subprocess.run(['java','-cp',str(tmp),test],check=True)
 # Parse every production Java file with the JDK compiler, without Android type resolution.
 subprocess.run(['java',str(root/'test/headless/ParseSources.java'),str(root/'app/java')],check=True)
