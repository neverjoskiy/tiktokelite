import pathlib, shutil, subprocess, tempfile, unittest
from test_account_appearance_runtime import STUBS
from margyt import dexpatch
ROOT = pathlib.Path(__file__).resolve().parents[1]

CALLER = '''.class public LX/0Abc;
.super Ljava/lang/Object;
.method public ping()V
    .registers 3
    sget-object v0, LX/0Abc;->manager:Landroid/content/pm/PackageManager;
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/content/pm/PackageManager;->getInstalledPackages(I)Ljava/util/List;
    move-result-object v0
    return-void
.end method
'''


class PackagesHookTests(unittest.TestCase):
    def test_package_list_calls_come_to_the_mod(self):
        with tempfile.TemporaryDirectory() as room:
            target = pathlib.Path(room) / 'X' / '0Abc.smali'
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(CALLER, encoding='utf-8')
            counts = dexpatch.rewrite_models(room)
            self.assertGreater(counts.get('PackageManager->getInstalledPackages', 0), 0)
            patched = target.read_text(encoding='utf-8')
            self.assertIn('Lcom/neverocheg/elite/Packages;->installedPackages(Landroid/content/pm/PackageManager;I)Ljava/util/List;',
                          patched)

    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_block_returns_empty_list(self):
        sources = {k: STUBS[k] for k in ['android/content/Context.java',
                                         'android/content/SharedPreferences.java']}
        sources.update({
            'android/os/SystemClock.java': 'package android.os;public class SystemClock{public static long uptimeMillis(){return 100000;}}',
            'android/content/pm/PackageManager.java': 'package android.content.pm;import java.util.*;'
                                                      'public class PackageManager{public List getInstalledPackages(int f)'
                                                      '{return Arrays.asList("tiktok", "max");}'
                                                      'public List getInstalledApplications(int f){return new ArrayList();}}',
            'com/neverocheg/elite/Margy.java': 'package com.neverocheg.elite;'
                                               'class Margy{static final String PREFS="p";'
                                               'static android.content.Context context(){return null;}}',
            'com/neverocheg/elite/Packages.java': (ROOT / 'inject/java/com/neverocheg/elite/Packages.java').read_text(encoding='utf-8'),
            'com/neverocheg/elite/Watched.java': (ROOT / 'inject/java/com/neverocheg/elite/Watched.java').read_text(encoding='utf-8'),
            'com/neverocheg/elite/Test.java': '''package com.neverocheg.elite;import java.util.*;public class Test{
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
static android.content.pm.PackageManager pm=new android.content.pm.PackageManager();
public static void main(String[] a){
check(Packages.installedPackages(pm,0).size()==2,"open passes through");
Packages.setBlocked(true);
check(Packages.installedPackages(pm,0).isEmpty(),"blocked list is empty");
Packages.setBlocked(false);
Watched.saw("1");Watched.saw("2");Watched.saw("1");
check(Watched.seen("1")&&Watched.seen("2")&&!Watched.seen("3"),"seen remembers");
}}''',
        })
        with tempfile.TemporaryDirectory() as folder:
            room = pathlib.Path(folder)
            files = []
            for name, source in sources.items():
                f = room / name
                f.parent.mkdir(parents=True, exist_ok=True)
                f.write_text(source, encoding='utf-8')
                files.append(str(f))
            result = subprocess.run(['javac', '-encoding', 'UTF-8', '-d', folder, *files],
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
            result = subprocess.run(['java', '-cp', folder, 'com.neverocheg.elite.Test'],
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
