"""SettingsGlyph parses only M/L/C/Z; every glyph a settings row asks for must exist.

4.0 shipped four glyphs written in full SVG syntax (`l`, `c`, `s`, `a`, `v`, `h`)
straight into the parser, whose tokenizer silently drops those letters and then
reads numbers as commands. That is an ArrayIndexOutOfBounds/NumberFormatException
inside the constructor, i.e. a crash on opening the settings tab.
"""
import pathlib, re, shutil, subprocess, tempfile, unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
ELITE = ROOT / 'inject' / 'java' / 'com' / 'neverocheg' / 'elite'
GLYPH = ELITE / 'SettingsGlyph.java'

# The exact grammar the runtime tokenizer understands.
TOKEN = re.compile(r'[MLCZ]|-?[0-9]+(?:\.[0-9]+)?')
ENTRY = re.compile(r'paths\.put\("([a-z_]*)"\s*,\s*(?:"([^"]*)"|paths\.get\("([a-z_]*)"\))\)')
CALL = re.compile(r'(?:toggleRow|liveToggleRow|actionRow|linkRow|betaRow|icon|glyph|make)\(\s*"([a-z_]+)"')
ARRAY = re.compile(r'(?:railIcons|icons)\s*=\s*\{([^}]*)\}')
LITERAL = re.compile(r'"([a-z_]+)"')

STUBS = {
    'android/graphics/Path.java':
        'package android.graphics;public class Path {public int points;'
        'public void moveTo(float x,float y){points++;}public void lineTo(float x,float y){points++;}'
        'public void close(){}public void cubicTo(float a,float b,float c,float d,float e,float f){points++;}}',
    'android/graphics/Paint.java':
        'package android.graphics;public class Paint {public enum Style {STROKE,FILL}'
        'public enum Cap {ROUND}public enum Join {ROUND}public Paint(int f){}'
        'public void setStyle(Style s){}public void setStrokeCap(Cap c){}public void setStrokeJoin(Join j){}'
        'public void setColor(int c){}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}}',
    'android/graphics/Rect.java':
        'package android.graphics;public class Rect {public int left,top,right,bottom;'
        'public int width(){return right-left;}public int height(){return bottom-top;}}',
    'android/graphics/Canvas.java':
        'package android.graphics;public class Canvas {public int save(){return 0;}'
        'public void restoreToCount(int n){}public void translate(float x,float y){}'
        'public void scale(float x,float y){}public void drawPath(Path p,Paint q){}}',
    'android/graphics/ColorFilter.java': 'package android.graphics;public class ColorFilter {}',
    'android/graphics/PixelFormat.java':
        'package android.graphics;public class PixelFormat {public static final int TRANSLUCENT=-3;}',
    'android/graphics/drawable/Drawable.java':
        'package android.graphics.drawable;public abstract class Drawable {'
        'public android.graphics.Rect getBounds(){return new android.graphics.Rect();}'
        'public void setBounds(int l,int t,int r,int b){android.graphics.Rect b2=getBounds();'
        'b2.left=l;b2.top=t;b2.right=r;b2.bottom=b;}'
        'public abstract void draw(android.graphics.Canvas c);public abstract void setAlpha(int a);'
        'public abstract void setColorFilter(android.graphics.ColorFilter f);'
        'public abstract int getOpacity();public void invalidateSelf(){}}',
}

HARNESS = """package com.neverocheg.elite;
public class GlyphHarness {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] args){
  String[] names = {%s};
  for(String name : names){
   android.graphics.drawable.Drawable glyph = SettingsGlyph.make(name, 0xff336699);
   check(glyph != null, "no glyph registered for " + name);
   glyph.setBounds(0, 0, 24, 24);
   glyph.draw(new android.graphics.Canvas());
  }
 }
}
"""


def registry():
    """name -> path data, with aliases resolved against the entries above them."""
    found = {}
    for name, literal, alias in ENTRY.findall(GLYPH.read_text(encoding='utf-8')):
        found[name] = literal if literal else found.get(alias)
    return found


def requested():
    """Every literal glyph name the mod asks for, including the rail's array."""
    names = set()
    for path in ELITE.glob('*.java'):
        text = path.read_text(encoding='utf-8')
        names.update(CALL.findall(text))
        for group in ARRAY.findall(text):
            names.update(LITERAL.findall(group))
    return names


class SettingsGlyphGrammarTest(unittest.TestCase):
    def test_every_registered_path_uses_only_the_supported_grammar(self):
        found = registry()
        self.assertGreater(len(found), 60, 'the registry looks truncated')
        for name, data in found.items():
            self.assertTrue(data, '%s has no path data' % name)
            leftover = re.sub(TOKEN, '', data).strip()
            self.assertEqual('', leftover, '%s uses unsupported commands: %s' % (name, leftover))
            tokens = TOKEN.findall(data)
            index, drawn = 0, 0
            while index < len(tokens):
                command = tokens[index]
                index += 1
                self.assertIn(command, ('M', 'L', 'C', 'Z'), name)
                if command == 'Z':
                    drawn += 1
                    continue
                count = 6 if command == 'C' else 2
                self.assertLessEqual(index + count, len(tokens),
                                     '%s: %s is short of arguments' % (name, command))
                for offset in range(count):
                    self.assertNotIn(tokens[index + offset], ('M', 'L', 'C', 'Z'),
                                     '%s: a command sits inside %s arguments' % (name, command))
                index += count
                drawn += 1
            self.assertGreater(drawn, 0, '%s draws nothing' % name)

    def test_every_requested_glyph_is_registered(self):
        missing = requested() - set(registry())
        self.assertEqual(set(), missing,
                         'settings rows ask for glyphs that are not registered: %s' % sorted(missing))

    @unittest.skipUnless(shutil.which('java') and shutil.which('javac'), 'JDK required')
    def test_every_glyph_builds_and_draws(self):
        names = sorted(set(registry()) | requested())
        with tempfile.TemporaryDirectory() as directory:
            work = pathlib.Path(directory)
            sources = dict(STUBS)
            sources['com/neverocheg/elite/SettingsGlyph.java'] = GLYPH.read_text(encoding='utf-8')
            sources['com/neverocheg/elite/GlyphHarness.java'] = (
                HARNESS % ','.join('"%s"' % name for name in names))
            for name, value in sources.items():
                target = work / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(value, encoding='utf-8')
            built = subprocess.run(
                ['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-d', str(work)]
                + [str(work / name) for name in sources],
                capture_output=True, text=True)
            self.assertEqual(0, built.returncode, built.stderr)
            run = subprocess.run(['java', '-cp', str(work), 'com.neverocheg.elite.GlyphHarness'],
                                 capture_output=True, text=True)
            self.assertEqual(0, run.returncode, run.stderr)


if __name__ == '__main__':
    unittest.main()