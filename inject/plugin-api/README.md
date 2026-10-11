# Stubs for compiling the plugin api

`PluginContext` calls into the mod: `Plugins.addRow`, `Popup.show`,
`Screen.offer`, `Net.bytes` and a few more. Those classes live in the patched
apk, not in the api a plugin author compiles against, so the api compile had
nothing to resolve them with and failed with `cannot find symbol`.

These are the smallest declarations that let it compile. They never reach a
plugin's dex: the plugin is dexed with the api as a classpath, not as input,
so at runtime the real classes in the apk are the ones called.

A signature that drifts here breaks the api build, which is the point: that is
when somebody should look.
