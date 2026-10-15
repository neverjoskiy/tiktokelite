import os, tempfile, unittest

from margyt import dexpatch

SENDER = (".class public final LX/0RcG;\n.super LX/0Rcs;\n\n"
          ".method public final LJIIJJI(ILcom/bytedance/im/core/proto/MarkConversationReadRequestBody;Ljava/lang/String;I)V\n"
          "    .registers 9\n\n"
          "    .prologue\n    .line 67108864\n    new-instance v0, LX/0SNi;\n\n"
          "    .line 67108865\n    invoke-direct {v0}, LX/0SNi;-><init>()V\n\n"
          "    .line 67108870\n    iput-object p2, v0, LX/0SNi;->LJIJJ:Lcom/bytedance/im/core/proto/MarkConversationReadRequestBody;\n\n"
          "    .line 67108874\n    move-result-object v3\n\n"
          "    invoke-virtual {p0, p1, v3, v0, v2}, LX/0Rcs;->LJIIIZ(ILcom/bytedance/im/core/proto/RequestBody;LX/0RbW;[Ljava/lang/Object;)J\n\n"
          "    return-void\n.end method\n")

OTHER = (".class public final LX/0RcG;\n.super LX/0Rcs;\n\n"
         ".method public final LJIILJJIL(ILjava/lang/String;)V\n    .registers 5\n\n"
         "    const-string v0, \"something else\"\n\n    return-void\n.end method\n")


class ReadReceiptHookTests(unittest.TestCase):
    def write(self, room, name, text):
        os.makedirs(os.path.join(room, "X"), exist_ok=True)
        path = os.path.join(room, "X", name + ".smali")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        return path

    def test_read_sender_asks_the_mod_first(self):
        with tempfile.TemporaryDirectory() as room:
            sender = self.write(room, "0RcG.2", SENDER)
            other = self.write(room, "0RcG.1", OTHER)
            self.assertEqual(dexpatch.rewrite_read_receipt(room),
                             {dexpatch.READ_SEND_LABEL: 1})
            with open(sender, encoding="utf-8") as handle:
                patched = handle.read()
            self.assertIn("invoke-static {}, Lcom/neverocheg/elite/Read;->sends()Z\n\n"
                          "    move-result v0\n\n    if-nez v0, :elite_read\n\n"
                          "    return-void\n\n    :elite_read\n", patched)
            self.assertIn("new-instance v0, LX/0SNi;", patched)
            with open(other, encoding="utf-8") as handle:
                self.assertEqual(handle.read(), OTHER)
            # second pass only counts
            self.assertEqual(dexpatch.rewrite_read_receipt(room),
                             {dexpatch.READ_SEND_LABEL: 1})

    def test_wrong_shape_is_left_for_the_gate(self):
        with tempfile.TemporaryDirectory() as room:
            stub = ".class public final LX/0RcG;\n.super LX/0Rcs;\n\n"
            self.write(room, "0RcG.2", stub)
            self.assertEqual(dexpatch.rewrite_read_receipt(room), {})


if __name__ == "__main__":
    unittest.main()
