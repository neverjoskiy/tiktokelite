import os, tempfile, unittest

from margyt import dexpatch

SENDER = (".class public final LX/0S4d;\n.super Ljava/lang/Object;\n\n"
          ".method public final LIZ(Ljava/lang/String;)V\n    .registers 3\n\n"
          "    .prologue\n    .line 16777216\n    const-string v0, \"sendTypingEvent\"\n\n"
          "    .line 16777217\n    invoke-virtual {p0, v0}, LX/0S4d;->LJJII(Ljava/lang/String;)LX/06Wg;\n\n"
          "    move-result-object v0\n\n    return-void\n.end method\n")

OTHER = (".class public final LX/0Abc;\n.super Ljava/lang/Object;\n\n"
         ".method public final LIZ(Ljava/lang/String;)V\n    .registers 3\n\n"
         "    const-string v0, \"something else\"\n\n    return-void\n.end method\n")


class TypingSendHookTests(unittest.TestCase):
    def write(self, room, name, text):
        os.makedirs(os.path.join(room, "X"), exist_ok=True)
        path = os.path.join(room, "X", name + ".smali")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        return path

    def test_typing_sender_asks_the_mod_first(self):
        with tempfile.TemporaryDirectory() as room:
            sender = self.write(room, "0S4d", SENDER)
            other = self.write(room, "0Abc", OTHER)
            self.assertEqual(dexpatch.rewrite_typing_send(room),
                             {dexpatch.TYPING_SEND_LABEL: 1})
            with open(sender, encoding="utf-8") as handle:
                patched = handle.read()
            self.assertIn("invoke-static {}, Lcom/neverocheg/elite/Typing;->sends()Z\n\n"
                          "    move-result v0\n\n    if-nez v0, :elite_typing\n\n"
                          "    return-void\n\n    :elite_typing\n", patched)
            self.assertIn('const-string v0, "sendTypingEvent"', patched)
            with open(other, encoding="utf-8") as handle:
                self.assertEqual(handle.read(), OTHER)
            # second pass only counts
            self.assertEqual(dexpatch.rewrite_typing_send(room),
                             {dexpatch.TYPING_SEND_LABEL: 1})

    def test_wrong_shape_is_left_for_the_gate(self):
        with tempfile.TemporaryDirectory() as room:
            stub = ".class public final LX/0S4d;\n.super Ljava/lang/Object;\n\n"
            self.write(room, "0S4d", stub)
            self.assertEqual(dexpatch.rewrite_typing_send(room), {})
