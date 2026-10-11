import pathlib,unittest,tempfile,subprocess,shutil
import sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from margyt import dexpatch
class NativeFlagsTests(unittest.TestCase):
 def test_literal_readers_are_scoped_and_idempotent(self):
  with tempfile.TemporaryDirectory() as folder:
   file=pathlib.Path(folder)/'AFwS227S0000000_3.smali'
   method=lambda key:'.method public static '+key+'()I\n .registers 5\n const-string v3, "'+key+'"\n invoke-virtual {p0, v1, v0, v3, v2}, LX/09tK;->LJIIJJI(IILjava/lang/String;Z)I\n move-result v0\n return v0\n.end method\n'
   file.write_text(''.join(method(k) for k in ['audio_comment_publish','profile_bg_in_allow_list','profile_bg_enable_consumption_group','unrelated']),encoding='utf-8')
   self.assertEqual({'native experiment overrides':3},dexpatch.rewrite_native_flags(folder))
   changed=file.read_text();self.assertEqual(3,changed.count('Flags;->nativeInt'));self.assertEqual(1,changed.count('LX/09tK;->LJIIJJI'));self.assertEqual({},dexpatch.rewrite_native_flags(folder))
 def test_windows_case_collision_suffix_is_patched(self):
  with tempfile.TemporaryDirectory() as folder:
   file=pathlib.Path(folder)/'0A7j.1.smali'
   file.write_text('.method public static banner()I\n .registers 5\n const-string v3, "profile_bg_in_allow_list"\n invoke-virtual {p0, v1, v0, v3, v2}, LX/09tK;->LJIIJJI(IILjava/lang/String;Z)I\n move-result v0\n return v0\n.end method\n',encoding='utf-8')
   self.assertEqual({'native experiment overrides':1},dexpatch.rewrite_native_flags(folder))
   changed=file.read_text();self.assertEqual(1,changed.count('Flags;->nativeInt'));self.assertEqual(0,changed.count('LX/09tK;->LJIIJJI'))
 def test_feed_heart_tint_uses_heart_hook_before_generic_accent(self):
  with tempfile.TemporaryDirectory() as folder:
   file=pathlib.Path(folder)/'0HCj.smali'
   file.write_text('.class public LX/0HCj;\n.super Ljava/lang/Object;\n.method public tint(Lcom/bytedance/tux/icon/TuxIconView;I)V\n .registers 3\n invoke-virtual {p1, p2}, Lcom/bytedance/tux/icon/TuxIconView;->setTintColor(I)V\n return-void\n.end method\n',encoding='utf-8')
   anchored=dexpatch.rewrite_anchored(folder)
   accent=dexpatch.rewrite_accent(folder)
   changed=file.read_text()
   self.assertEqual(1,anchored['TuxIconView->setTintColor (in the class marked LX/0HCj;)'])
   self.assertEqual(0,accent.get('TuxIconView->setTintColor(I)V',0))
   self.assertIn('Heart;->setTintColor(Lcom/bytedance/tux/icon/TuxIconView;I)V',changed)
   self.assertNotIn('Accent;->setTintColor',changed)
 @unittest.skipUnless(shutil.which('javac'),'JDK required')
 def test_native_fallback_and_each_enabled_group(self):
  sources={
   'android/content/Context.java':'package android.content;public class Context {public static int MODE_PRIVATE=0;public SharedPreferences getSharedPreferences(String n,int m){return MargyPrefs.p;}public static class MargyPrefs {public static SharedPreferences p;}}',
   'android/content/SharedPreferences.java':'package android.content;public interface SharedPreferences {boolean getBoolean(String k,boolean d);Editor edit();interface Editor {Editor putBoolean(String k,boolean v);void apply();}}',
   'com/bytedance/ies/abmock/SettingsManager.java':'package com.bytedance.ies.abmock;public class SettingsManager {public static boolean LIZ(String k,boolean v){return v;}public static int LJ(String k,int v){return v;}public static long LJFF(String k,long v){return v;}public static String LJI(String k,String v){return v;}public static float LIZJ(String k,float v){return v;}public static double LIZIZ(String k,double v){return v;}}',
   'com/neverocheg/elite/Margy.java':'package com.neverocheg.elite;class Margy {static String PREFS="test";static android.content.Context context(){return new android.content.Context();}}',
   'com/neverocheg/elite/Feed.java':'package com.neverocheg.elite;class Feed {static boolean isEnabled(){return false;}}',
   'com/neverocheg/elite/Flags.java':(ROOT/'inject/java/com/neverocheg/elite/Flags.java').read_text(encoding='utf-8'),
   'com/neverocheg/elite/FlagHarness.java':"""package com.neverocheg.elite;import android.content.*;import java.util.*;public class FlagHarness {
    static class Prefs implements SharedPreferences,SharedPreferences.Editor {Map<String,Boolean> values=new HashMap<>();public boolean getBoolean(String k,boolean d){return values.containsKey(k)?values.get(k):d;}public Editor edit(){return this;}public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}public void apply(){}}
    public static class Reader {int calls;public int LJIIJJI(int scope,int fallback,String key,boolean cached){calls++;return 7;}}
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[]args){Context.MargyPrefs.p=new Prefs();Reader reader=new Reader();check(Flags.nativeInt(reader,4,0,"audio_comment_publish",true)==1);check(reader.calls==0);Flags.set(Flags.KEY_VOICE,false);check(Flags.nativeInt(reader,4,0,"audio_comment_publish",true)==7);check(reader.calls==1);Flags.set(Flags.KEY_VOICE,true);check(Flags.nativeInt(reader,4,0,"audio_comment_publish",true)==1);check(Flags.nativeInt(reader,4,0,"profile_bg_in_allow_list",true)==1);check(Flags.nativeInt(reader,4,0,"profile_bg_enable_consumption_group",true)==1);Flags.set(Flags.KEY_BANNER,false);check(Flags.nativeInt(reader,4,0,"profile_bg_in_allow_list",true)==7);check(Flags.nativeInt(reader,4,0,"unrelated",true)==7);check(Flags.nativeInt(null,4,3,"unrelated",true)==3);}
   }"""
  }
  with tempfile.TemporaryDirectory() as folder:
   room=pathlib.Path(folder)
   for name,source in sources.items():
    file=room/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(source,encoding='utf-8')
   built=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',folder]+[str(room/name) for name in sources],capture_output=True,text=True);self.assertEqual(0,built.returncode,built.stderr)
   ran=subprocess.run(['java','-cp',folder,'com.neverocheg.elite.FlagHarness'],capture_output=True,text=True);self.assertEqual(0,ran.returncode,ran.stderr)

