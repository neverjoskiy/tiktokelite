import pathlib, shutil, subprocess, tempfile, unittest
from test_account_appearance_runtime import STUBS
from margyt import dexpatch
ROOT = pathlib.Path(__file__).resolve().parents[1]


class WatchedHookTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_block_returns_empty_list(self):
        sources = {k: STUBS[k] for k in ['android/content/Context.java',
                                         'android/content/SharedPreferences.java']}
        sources.update({
            'android/os/SystemClock.java': 'package android.os;public class SystemClock{public static long uptimeMillis(){return 100000;}}',
            'com/neverocheg/elite/Margy.java': 'package com.neverocheg.elite;'
                                               'class Margy{static final String PREFS="p";'
                                               'static android.content.Context context(){return null;}}',
            'com/neverocheg/elite/Watched.java': (ROOT / 'inject/java/com/neverocheg/elite/Watched.java').read_text(encoding='utf-8'),
            'com/neverocheg/elite/Test.java': '''package com.neverocheg.elite;import java.util.*;public class Test{
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
public static void main(String[] a){
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
