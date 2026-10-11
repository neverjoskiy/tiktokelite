"""The loader's views are found by what they are, and hooked at the top of onDraw."""
import os, re, sys, tempfile, unittest
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)
from margyt import dexpatch  # noqa: E402

VIEW = """.class public final LX/0pWh;
.super Landroid/view/View;
.source "SourceFile"

.field public final LLJJIJIIJIL:Landroid/graphics/PorterDuffXfermode;

.method public final onDraw(Landroid/graphics/Canvas;)V
    .registers 20

    .prologue
    .line 16777216
    move-object/from16 v11, p1

    return-void
.end method

.method public final setProgressBarInfo(I)V
    .registers 5

    return-void
.end method
"""


class LoadingHookTest(unittest.TestCase):
    def rewrite(self, text):
        with tempfile.TemporaryDirectory() as root:
            path = os.path.join(root, "0pWh.smali")
            with open(path, "w", encoding="utf-8") as handle:
                handle.write(text)
            counts = dexpatch.rewrite_loading(root)
            with open(path, encoding="utf-8") as handle:
                return handle.read(), counts

    def test_the_call_goes_before_anything_the_method_does(self):
        out, counts = self.rewrite(VIEW)
        self.assertEqual(counts, {"native loading indicator": 1})
        hook = out.index("Lcom/neverocheg/elite/Loading;->draw(Landroid/view/View;Landroid/graphics/Canvas;)Z")
        self.assertLess(out.index(".prologue"), hook)
        self.assertLess(hook, out.index("move-object/from16 v11, p1"))
        # two registers past fifteen cannot be named one by one
        self.assertIn("invoke-static/range {p0 .. p1}, Lcom/neverocheg/elite/Loading;", out)
        self.assertIn("    if-eqz v0, :margyt_loading_native\n\n    return-void\n\n    :margyt_loading_native\n", out)

    def test_a_second_pass_changes_nothing(self):
        once, _ = self.rewrite(VIEW)
        twice, counts = self.rewrite(once)
        self.assertEqual(once, twice)
        self.assertEqual(counts, {"native loading indicator": 1})

    def test_other_views_are_left_alone(self):
        for other in (VIEW.replace(".super Landroid/view/View;", ".super Landroid/widget/ImageView;"),
                      VIEW.replace("Landroid/graphics/PorterDuffXfermode;", "Landroid/graphics/Paint;"),
                      VIEW.replace(".method public final setProgressBarInfo(I)V", ".method public final setOther(I)V"),
                      # calling it is not having it
                      VIEW.replace(".method public final setProgressBarInfo(I)V", ".method public final other(I)V")
                          .replace("    return-void\n.end method\n\n.method public final other",
                                   "    invoke-virtual {p0, v0}, LX/0pWh;->setProgressBarInfo(I)V\n    return-void\n.end method\n\n.method public final other")):
            out, counts = self.rewrite(other)
            self.assertEqual((out, counts), (other, {}))

    def test_a_method_with_no_register_to_spare_is_left_alone(self):
        tight = VIEW.replace("    .registers 20\n\n    .prologue", "    .registers 2\n\n    .prologue")
        out, counts = self.rewrite(tight)
        self.assertEqual((out, counts), (tight, {}))

    def test_a_rule_is_only_run_where_its_text_is(self):
        for _label, pattern, _target in dexpatch.model_rules():
            needle = dexpatch._required(pattern)
            self.assertTrue(needle, pattern.pattern)
            # the needle is in the pattern as plain text, so it is in any match
            self.assertIn(needle, re.sub(r"\\(.)", r"\1", pattern.pattern))


if __name__ == "__main__":
    unittest.main()
