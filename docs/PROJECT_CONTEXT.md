# TikTokElite — полный контекст проекта (для нейросети)

> Дата среза: 2026-10-14. Отвечать пользователю на русском. Без эмодзи, коротко и по делу.
> Этот файл — единственный источник контекста. Если что-то ниже противоречит памяти модели — верить файлу.

---

## 1. Проект одной строкой

Android-мод официального TikTok, форк MargyT (@narezany). Локально `D:\projects\margyt`, GitHub `neverjoskiy/tiktokelite`, ветка `main`. База полного мода: **TikTok 47.2.41 universal**. Пакет приложения не переименовывается (`com.zhiliaoapp.musically` / Lite `.go`); собственный неймспейс мода — **`com.neverocheg.elite`** (апстрим `cat.narezany.margyt`). Сборка: `py -m margyt tiktok-47-2-41.apk` (JDK 17; холодная ~19 мин, тёплая ~90 сек) → `build/tiktokolite.apk` (~395 МБ, debug-подпись — **ключ менять нельзя**, иначе у пользователей ручная переустановка).

Три продукта:
1. **TikTokElite** — полный мод (VERSION `3.0.3`).
2. **TikeLite** — лайт-мод на базе TikTok Lite 47.1.4 (`VERSION-LITE` `0.1`, пакет `com.zhiliaoapp.musically.go`, лейбл `TikeLite`). Статус: код портирован, релизов ещё не было.
3. **Elite Patcher** — приложение-патчер (`com.neverocheg.patcher`, v1.0, Kotlin/Compose). Статус: код портирован и ребренднут, APK ещё не собран (нужен Android SDK/Gradle), `patcher/catalog.json` пока без релизов.

---

## 2. Присланная сводка прошлого диалога (вложена целиком, как просили)

# TikTokElite — Full Conversation Summary

## Project
Android mod of official TikTok (rebranded fork of MargyT by @narezany). Local repo `D:\projects\margyt`, GitHub `neverjoskiy/tiktokelite` (branch `main`). Base APK: TikTok 47.2.41 universal (arm64, nodpi). Build: `py -m margyt tiktok-47-2-41.apk` (JDK 17, ~90s cached) → `build/tiktokolite.apk` (~395 MB, debug-signed — key must never change or users face manual reinstall).

## Rebrand (done)
- Footer now "TikTokElite by @neverocheg" + "TikTokElite · GPL-3.0" (user claims GPL as their own); removed "based on MargyT by @narezany" and old nick strings.
- Removed donate banner, 3rd-visit support popup, and "Thanks" section in About.
- Links card = 3 rows, all to `t.me/tiktelite`. Old constants/methods left as dead code.

## Features added/changed
- Launcher icon = crown PNG (`icon_out/play/ic_launcher-512.png`, actually 1254px). First attempt (dropping adaptive XML) broke the icon; fixed properly via `res/elite/crown.png` (package 0x30) + manifest repoint (`set_application_icon`).
- Disabled server-dependent buttons ("Gradient on nick", "Prove account") → "Feature unavailable" toast (3 languages).
- Comment timestamps: absolute dates (today `HH:mm`, this year `dd.MM HH:mm`, older with year), matched by RU/UK/EN relative-time regex, stamped post-bind with recycle guard.
- Bottom nav: labels hidden (bottom-60% screen guard protects same-word headers), icons nudged +3dp.
- Settings home redesigned as 2-column grid of rounded tiles.
- Granted `supporter` badge to UID 7473334787610215425 (`--no-push`; delivery requires server to pick up `badges.json`).

## Upstream port (MargyT 2.0.1–2.0.2, same TikTok base)
Ported: Sliced incremental screen walks, Dim chat detection fix, plugin restart bar, banner-shader repaint fix, TabBadges/TabCount (hold tab = mark read), AvatarShape (10 shapes + story ring), Flags rewrite (comment/self repost, new profile, table-driven `X/09tK` reader interception, FlagGuard), settings UI + tests, build hook gate update. Deliberately skipped: Dep mini-game (needs their server), Screamers, dev flag browser UI, donate rework, CatWidget.

## Releases & infra
- README rebranded (no banner, GPL-3.0, new URLs, Windows note); `version.json` + `Updater.SOURCE` wired to repo raw file; APK downloads to app-internal `files/elite/update.apk`, auto-deleted after install.
- 2.0, 2.1, 2.1.1 released (VERSION file → build → APK to GitHub Release `vX` as `tiktokolite.apk` → bump `version.json` → push; user uploads APK manually via web UI).
- Dev (`--test`) builds: faint account-ID watermark, settings locked to `supporter` badge, logcat perf probes.
- Pre-existing test failures (Png, loading hook, account_appearance, comment_selection, recolour_cache) verified unrelated via stash.

## Open / unverified
- User uploads 2.1.1 APK to GitHub Release (then auto-update goes live).
- On-device verification pending: comment-date regex vs real format, tab label names, tile grid look, new flags/anchors in the wild.
- Offered, not started: sponsored-comment hiding, copy-link button, LIVE-dot hide, recommendation-card toggles, theme schedule, mark-all-notifications-read, default download quality, OLED black preset.
- Streak-flame recolor: investigated (prime suspect: ±20° hue window catching flame reds), user declined the fix; stopgap is default pink accent.
- New TikTok base versions: usually just rebuild; obfuscation-anchor breakage fixed per build-log errors in `margyt/dexpatch.py` (build fails loudly, never ships half-modded).

---

## 3. История версий (git log, новое сверху)

- `3.4` (сборка готова, к релизу): скрытие набора текста + скрытие «прочитано» в личке; скрытие просмотренных. Блок списка приложений удалён — TikTok 47.2.41 не зовёт PackageManager, хук не давал попаданий
- `3.3.1`: иконки везде монохром-контур, заголовкам возвращён обычный регистр (заливка APK и version.json — уточнить у пользователя)
- `3.3`: настройки-карточки; патчер раздаёт 3.3, НО version.json=3.2 (полный апдейтер предлагает 3.2 — несогласовано, поднять)
- `3.2`: счётчики на пути записи + `no compact word` (version.json 3.2 live)
- `ef3a0c4` Release 3.1: version.json (live); `b495bfd` TikTokElite 3.1: порт 2.0.5, TikeLite, Elite Patcher
- `7383d5c` Release 3.0.3: version.json
- `3041dae` TikTokElite 3.0.3: counts that work, comment cards, no dislike, update retry
- `d783fb1` Release 3.0.2: version.json
- `ba42516` TikTokElite 3.0.2: short counts (Counts.java), Material 3 comments (CommentSheets.java)
- `b99a825` TikTokElite 3.0.1: keep-the-bar (NavBar KEY_KEEP), owner badge + gradient (Badges.OWNER_UID)
- `8e29113` Release 3.0: version.json
- `26513e4` TikTokElite 3.0: порт MargyT 2.0.3+2.0.4 (NavBar+NavPlan, Md3, SettingsCell, Brightness, FEED_RING, лаг-пакет, Safely, Launcher.route, CrashReports)
- `e78db6a` Release 2.1.1, `a4bbc61` плитка-сетка настроек, `8207f1d` Release 2.1, `3a80dff` TikTokElite 2.1, далее 2.0.x.
- Релиз 3.0.1 пропущен (на устройства не попадал; покрыт 3.0.3).
- Сборка 3.4 готова 2026-10-16 (`build/tiktokolite.apk`, 396 551 001 Б, sha256 `f6e0b318…6ca7aa`, 56023 правки, гейт прошёл). Lite-база `tiktok-lite-47-1-4.apk` скачана (sha256 сошёлся).

## 4. Детали фич по версиям

- **3.0.1**: тумблер «Не прятать нижнюю панель» (NavBar `KEY_KEEP`); владелец в клиенте: UID `7473334787610215425` = `@neverocheg` всегда получает бейдж «Владелец TikTokElite» + градиент `E2FF70→FFB561` (цвета взяты из публичного `/api/badges` narezany). Реализация: `Badges.OWNER_UID` + инъекция в `apply()`, `Looks.learn()`. Бейдж рисуется системной note с тинтом `8DD1B0`, без картинки; тексты через `Text.OWNER_*`.
- **3.0.2**: `Counts.java` — «100,8 тыс.»→«100,8K» (млн→M, млрд→B, укр. тис., широкие пробелы), тумблер default ON, тест `test_counts_runtime.py`. `CommentSheets.java` — карточки строк, скругление шторки, тумблер default ON, встройка в `Comments.bindCell`.
- **3.0.3**: счётчики — расширены пробелы, снято исключение кнопок, бюджет обхода 30k, добивка через 1.5с, `Diary.detail` при первом срабатывании. Комменты — замена (не пропуск) фона строк; лайк на диске; дизлайк скрыт (дескрипшены: не нравится/dislike/не подобається); «ответы» на карточке; шапка (число+меню) на карточке. Апдейтер — ретрай оффера 3×30с при холодном старте (`Updater.homeless`).
- **Жалобы с устройства (3.0.2)**: (а) K-счётчики не работали вообще — точная причина не найдена (подозрения: узкий паттерн пробелов, бюджет 4k, пропуск строк с фоном); исправлено вслепую в 3.0.3 — ждать проверку + дневник с меткой `counts:`; (б) карточки не видны — вероятен пропуск строк с не-null фоном (исправлено заменой); (в) «автообновление не работает» — код цел, URL жив; на 3.0.2 при `version.json`=3.0.2 тишина = норма. Решение пользователя: счётчики отложить в очередь после переноса остального.

## 5. Порт MargyT 2.0.5 (коммиты `4220bd4`, `24ddad6`, `b2a39ef`, `a13737e`, `69afcc4`; рабочий, незакоммичен)

Счётчиков/комментариев в апстриме **нет** — порт 2.0.5 жалобы с устройства не чинит.

- **Перф («меньше работы на каждом кадре»)**: `DateOverlay.panelOpen` кэш 250мс; `Dim.onPreDraw` троттлинг 48мс + `flat()` (`forceHasOverlappingRendering(false)`, один раз на вью); `NavBar.settle` чтение имени таба/`Motion.enabled` раз в 250мс + alpha fast-path (`tab.shown`); `FeedRail` тег `KNOWN=0x7e0c0399`, `setVisibility` чужих вью напрямую, `icons()` спит 500мс при выкл. иконках; `Md3` цвет карточки считается раз (`card.dressed`); `Repost.TAG` стал package-visible.
- **Видимое/диагностика**: флаг `circle_search_block` (убирает secure-окно от Circle to Search — чёрный экран в ресентах у части акков); `Feed.searchItems` пишет в дневник `search: a page of X from TikTok, Y after the mod`; `Diagnostics` раздел `region` (что сказали TikTok / SIM / сеть / локаль); `Lag` называет плагин-виновник (`Plugins.claim/owner`, `Lag.charge/cost`, порог 3с, в карточке плагина красным `Text.PLUGIN_SLOW`); `Badge.nickname(User)` (чтение поля напрямую для Lite) + логи тапов в test/lite; `Text.LITE_GOOGLE`.
- **Lite/TikeLite**: новые `LiteLogin` (глушит Google-вход, провайдер `elite_none`, тост), `LiteMenu` (строка `TikeLite` в шторке `MoreActionDialog` через прокси `Function1`), `LiteNet` (не дропать запросы); `Version.LITE`; гарды в `Md3/NavBar/Loading/Watch/MargyProvider/SettingsRow`; `SettingsActivity` титул/футер под Lite; `USER.nickname` field-hook; реврайтеры `rewrite_lite_menu` / `rewrite_lite_drops` (метки `:elite_drop_N`); `build.py` lite-детект по пакету `.go`, `mod_version()` (`VERSION`/`VERSION-LITE`), мета `com.neverocheg.elite.mod/version` (`elite`/`tikelite`), `apk.deflater = tools.deflate_many`, lite-гейт (требует SIM-хуки + `menu_rows`, иначе падение); `find_anchors` терпит missing; `apkzip.deflater`, `axml value=0x01010024`, `manifest.add_meta_data`, `toolchain jzlib/deflate_many`, `delta.py`, `java/Deflate.java`.
- **Патчер-инфра апстрима**: `server/patcher.py` (скопирован как есть; TG-вход отложен — делаем публичный без входа); `patcher/catalog.json` переписан под нас (см. §8).
- Тесты обновлены зеркально апстриму (`feed_rail`, `ad_filter`+Diary, `date_overlay` часы, `dim` flat/Build/часы). Наш `Corners.java` отсутствует — гард не применим. `SettingsActivity` shelf/filter под Lite не портированы осознанно (нумерация групп своя; только титул+футер) — follow-up после первой Lite-сборки.

## 6. Архитектура мода

- `inject/java/com/neverocheg/elite/` — **156 файлов**: весь рантайм мода, компилируется в один dex (`tools.compile_dex`: javac → R8/D8) и добавляется в APK следующим `classesN.dex`. Генерируемые сборкой: `Baked.java` (акцент+таблицы), `Emblem.java` (бейдж-base64), `Icons.java` (иконки настроек), `Version.java` (MOD/TIKTOK/TEST/LITE), `Nightly.java` (цвета темы), `Shots.java` (иконки лаунчера), `Anchors.java` (найденные обфусцированные методы), `BuildInfo.java` (PATCHED_DEX/TOTAL_DEX/REWRITES/STATIC_TARGETS).
- Ключевые модули: `Margy` (контекст/префы), `MargyProvider` (старт), `Watch`/`SettingsRow` (lifecycle), `Region/MargyProvider` (подмена SIM/региона), `Accent/MaterialAccents/Nightly/Baked` (цвета), `Md3/Corners?/Loading/Themes` (оформление), `NavBar/NavPlan/TabBadges/TabCount` (нижняя панель), `Feed/FeedRail/FeedBlacklist` (лента/реклама/поиск), `Comments/CommentSheets/CommentText/CommentClipboard/CommentSelection` (комменты), `Counts` (K/M/B), `DateOverlay/Dates` (даты), `Dim/BurnEye` (затемнение/анти-выгорание), `Badges/Badge/Looks/Gradient` (бейджи), `Download*` (скачивание), `Flags/FlagGuard` (эксперименты TT, перехват `X/09tK`), `Lag/LagTally/Probe/Diagnostics/Diary/CrashReports` (перф и отчёты), `Streaks/StreakAlarm` (серии), `Updater` (обновления), `Plugins/Plugin*` (плагины), `SettingsActivity/SettingsCell/SettingsRow` (настройки), `AvatarShape/Avatars/FriendAvatars` (аватары), `Repost/VideoActions/PlayerTools` (репосты/плеер), `Text` (все RU/UK/EN строки через `pick()`), `Tester/DevMode` (dev).
- `margyt/dexpatch.py` — патчинг smali: якоря по обфусцированным именам (`TARGETS`, `MODEL_SOURCES`, `FIELD_SOURCES`, `DISCOVERED_STATICS/VIRTUALS`, `Anchors.java`); **падает громко, не полумодом**. Критический только-наш фикс: `_class_file` ищет smali по декларации класса, т.к. на Windows baksmali пишет `0MUp.1.smali`/`0MUP.smali` (коллизия регистра) — без этого native settings row = 0 и сборка падает. Апстрим (Linux) этого не видит — передать narezany. Регресс-тест `test_settings_row_hook.py::test_case_numbered_smali_is_found_on_windows`.
- `margyt/build.py` — гейт хуков `feature_hits` (**0 попаданий = падение сборки**); для Lite отдельный гейт (SIM + `menu_rows`). Пайплайн: manifest → акцент → generated → dex → manifest (label, SettingsActivity, Store, PluginEditor, права, ресивер, провайдер, мета, authorities `.elite`, иконки `res/elite/`, crown) → ассеты → dex-патчинг (параллельно через `dexcache`) → `BuildInfo` → второй compile для проверки поверхности → добавление dex → `deflate_many` → подпись.
- `margyt/__main__.py` — CLI: позиционный APK, `-o build/tiktokolite.apk`, `--work work/`, `--tools tools/`, `--test`, `--keystore*` (по умолчанию debug; `ELITE_KEYSTORE_PASSWORD`).
- `server/` — `badges.py` (сервер бейджей; чужой прод `margy.narez.xyz`, наш `badges.json` приложение не читает), `bot.py`, `tiktok.py` (через него вычислен UID владельца), `patcher.py` (auth API, пока не деплоим).
- `site/` — лендинг (пока старые ссылки апстрима; обновление отдельно, не в скоупе Lite/патчера).

## 7. TikeLite (статус и решения)

- База: `tiktok-lite-47-1-4.apk` (90 464 759 Б, sha256 `634c596e…3c6057` — сошёлся), пакет `.go`.
- Неймспейс кода тот же (`com.neverocheg.elite.*`), мета `tikelite`; лейблы/меню `TikeLite`; Google-вход заглушен с тостом.
- В Lite работают только: регион, настройки, тема/акцент, бейджи, хуки Android; `Md3/NavBar/Counts/TabBadges/SocialLibrary/Streaks/Updater/Tutorial` отключены гардами.
- Осталось: первая сборка на Lite-базе → проверка `menu_rows` + SIM → shelf/filter настроек → залить релиз → вписать в каталог.

## 8. Elite Patcher (статус и решения)

- Пакет `com.neverocheg.patcher` (Kotlin/Compose, был `cat.narezany.patcher`), имя `Elite Patcher` 1.0 (`versionCode` 1). Мета-ключ версии мода `com.neverocheg.elite.version` совпадает со сборкой.
- Поддержаны патч-релизы и whole-APK (`Stage.APK`, `ApkHashes`, `ownPackage` — код есть, MargyC выпилен: иконка удалена, ветка в `Bits.kt` убрана).
- `patcher/catalog.json`: канал `t.me/tiktelite`, моды `elite` (`com.zhiliaoapp.musically`) и `tikelite` (`.go`), `releases: []` — заполнить после сборок+`delta.py` (base/patch/result sha256+size, notes RU/UK/EN). Поле патчера `sha256: pending-first-patcher-build`, `size: 0` — заполнить после сборки APK патчера.
- Проверка 2026-10-14: полный мод с портом собирается успешно (`build/tiktokolite.apk`, 395 МБ, 56021 правок против 56012 — ровно новые хуки; `User.nickname x5` на месте). Это пробная сборка из черновика, не релиз.
- Первая сборка TikeLite УСПЕШНА (2026-10-14, 424с): `build/tikelite.apk`, 122 988 831 Б, sha256 `17547e46…82891`; lite-гейт пройден (SIM 43, `menu_rows` найдена, drops 1, 12320 правок). База Lite: 90 464 759 Б, sha256 `634c596e…3c6057`. База полного: 346 017 026 Б, sha256 `4bcad2a1…285ba75c` (совпал с каталогом апстрима).
- 3.1: `VERSION` поднят 3.0.3→3.1; полная сборка успешна (66с, 396 546 905 Б, sha256 `3a3e9c2a…7c794`, 56021 правка); коммит `52ba8e4`→`b495bfd` (порт внутри, `.opencode/` вычищен и заигнорен); каталог патчера заполнен whole-APK релизами elite 3.1 + tikelite 0.1. Пользователь залил оба APK; `version.json` поднят до 3.1 (`ef3a0c4`), запушен.
- Elite Patcher APK: собран через GitHub Actions (Gradle 8.11.1, JDK 17, подпись ключом из секретов): `elite-patcher.apk`, 1 334 923 Б, sha256 `79e2b8a9…5d69e9`, залит в релиз `patcher-v1.0`, вписан в каталог. CI-фикс по пути: `setup-android` выкинут (брали предустановленный SDK), синтаксис `apksigner --ks-pass env:`.
- `3.2` live: счётчики на пути записи + `no compact word`; `version.json` 3.2 запушен; патч `elite.mpatch` 72 654 924 Б (`9737c2a5…730d2d`) в `v3.2`, каталог переключён elite 3.1→3.2.
- `3.3`: настройки-карточки (секции+разделители, строки, пилюля, липкая шапка, дом-список); сборка успешна (76с, 396 546 905 Б, `8dbd089e…bac52a`); патч `elite.mpatch` 72 653 844 Б (`f7d279b7…82d06`, результат сверен с локальной сборкой) в `v3.3`, каталог elite 3.2→3.3. Лайт заморожен (0.1 не трогать, не пересобирать).
- `3.4` (патчер раздаёт, version.json НЕ трогать — раздача только через каталог): скрытие набора текста + «прочитано» в личке, скрытие просмотренных; block_apps вычищен (`c84f3e0`+`fc58f33`); сборка 396 551 001 Б (`f6e0b318…6ca7aa`, 56023 правки, релизный APK сверен байт в байт); патч `elite.mpatch` 72 650 588 Б (`5add4477…1810b`, скачан и сверен) в `v3.4`, каталог elite 3.3→3.4.
- Патчи: собраны через Actions (`.github/workflows/patches.yml`, Python 3.11, проверка `apply` с хэшами): `elite.mpatch` 72 653 532 Б (`102b0e0b…42425c`), `tikelite.mpatch` 33 869 815 Б (`60b0b428…100ce70`), залиты в релизы `v3.1`/`lite-v0.1`. Каталог переведён с `apks[]` на `releases[]` (модель апстрима): патчер качает оригинал + патч и собирает мод на телефоне. ВАЖНО: теги релизов в workflow — inputs при запуске (`full_tag`, `lite_tag`), иначе соберётся патч из старого релиза.
- Блокеры на этой машине: нет Android SDK/Gradle (APK патчера тут не собрать — нужен SDK или другая машина/CI); Python 3.14 без компилятора — `bsdiff4` не ставится, `.mpatch` тут не сгенерировать. Запасной путь: раздача целиком через `apks[]` (код whole-APK портирован), патчи добавить позже.
- Дефолты кода: каталог → `raw.githubusercontent.com/neverjoskiy/tiktokelite/main/patcher/catalog.json`, API/beta → заглушки на raw (сервера нет; вход не используется).
- Осталось: собрать APK патчера (нужен Android SDK + Gradle, `patcher/build.sh`), залить в `releases/download/patcher-v1.0/elite-patcher.apk`, вписать hash/size, сгенерировать `.mpatch` обоих модов, заполнить релизы, обновить `site/`.

## 9. Апдейтер и релизный цикл

- Полный мод: `version.json` **в APK не зашивается** (вшит только `Version.MOD`); читается по сети при старте и каждые 5 мин: `raw.githubusercontent.com/.../main/version.json`. Сейчас `3.0.3`. Тумблер/напоминание в настройках обновлений; ретрай оффера 3×30с (`Updater.homeless`).
- Цикл: `VERSION` + `Version.MOD` → коммит → сборка → пользователь вручную заливает APK в GitHub Release `vX` как `tiktokolite.apk` → коммит `version.json` → push.
- Upstream remote: `upstream → https://git.narez.xyz/narezany/MargyT.git` (подтянут, есть все коммиты; последний портированный `e0556b2`, дальше `4220bd4…69afcc4` — 2.0.5+патчер).

## 10. Тесты

- `py -m unittest discover -s tests` с `PYTHONPATH=root;tests`: **206 тестов**. Падают только 6 известных до-портовых: `Png` (2), `loading hook`, `account_appearance`, `comment_selection`, `recolour_cache`. Новых падений после порта 2.0.5 нет.
- Регресс-тест виндоколлизии: `test_settings_row_hook.py::test_case_numbered_smali_is_found_on_windows`.
- PowerShell: нет `head/tail/sed/awk/grep`; вместо `tail` — `Select-Object -Last N`; пайпы с бинарником портят (`git archive` только в файл); пути с пробелами квотить; `certutil -hashfile` для sha256; фоновые долгие задачи — `background: true`, не поллить.

## 11. Конвенции и запреты

- Отвечать пользователю на русском, коротко, по делу, без эмодзи. Ссылки на код — `путь:строка`.
- Не коммитить мусор: `build-*.log`, `work/` (игнор), `.opencode/` (не коммитить). Скачанные базы (`tiktok-*.apk`) и `build/` — артефакты, в коммиты не тащить без спроса.
- Ребренд: ссылки только `t.me/tiktelite`; донат-баннер/попап/`Thanks` не возвращать; кнопки «Градиент на ник»/«Подтвердить аккаунт» — тост «недоступно».
- Реколор пламени стриков НЕ делать (пользователь отказался).
- Счётчики — в очереди после Lite/патчера; следующий шаг по ним — проверка 3.0.3 на устройстве + дневник `counts:`.
- Новую базу TikTok — обычно просто rebuild; поломки якорей чинить по логу в `margyt/dexpatch.py`.

## 12. Чек-лист релиза (выполнять каждый раз, напоминать пользователю)

Нейросеть обязана сама напоминать эти шаги, а не ждать вопросов.

### Релиз полного мода `vX`
1. `VERSION` → `X`, сборка `py -m margyt tiktok-47-2-41.apk`, проверка гейта хуков.
2. Коммит кода. Напомнить: «залей `build/tiktokolite.apk` в релиз `vX` как `tiktokolite.apk`».
3. По слову «залил»: `version.json` → `X` (notes RU/UK/EN), коммит `Release X: version.json`, push.
4. Напомнить про патчи (см. ниже) — без них патчер раздаёт старый релиз.

### Релиз TikeLite `lite-vA`
1. `VERSION-LITE` → `A`, сборка `py -m margyt tiktok-lite-47-1-4.apk -o build/tikelite.apk`, проверка lite-гейта (SIM + `menu_rows`).
2. Коммит. Напомнить: «залей `build/tikelite.apk` в релиз `lite-vA` как `tikelite.apk`».

### Патчи после заливки APK (обязательно напоминать)
1. Напомнить: «запусти Actions `Elite patches`».
2. По готовности: скачать артефакт, залить `elite.mpatch` в `vX`, `tikelite.mpatch` в `lite-vA`.
3. Взять `sha256`/`size` из лога (или скачать и снять локально, файлы потом удалить) → вписать в `patcher/catalog.json` (`releases[]`), коммит, push.

### Релиз Elite Patcher `patcher-vA`
1. Версия в `patcher/app/build.gradle.kts` + код, коммит.
2. Напомнить: «запусти Actions `Elite Patcher APK`» (нужны секреты ключа; ключ не терять).
3. Залить артефакт в релиз `patcher-vA` как `elite-patcher.apk`, `sha256`/`size` из лога → в `patcher/catalog.json`, коммит, push.

## 13. Открытое / предложено, не начато

- Проверка 3.0.3 на устройстве (K-счётчики, карточки, дизлайк, шапка) + regex дат, табы, плитки, флаги «в дикой природе».
- Скрытие спонсорских комментов, кнопка копирования ссылки, скрытие LIVE-точки, тумблеры рекомендаций, расписание темы, «прочитать все уведомления», качество скачивания по умолчанию, OLED-пресет.
- Сборка APK патчера + заливка + хэши; `.mpatch` обоих модов + заполнение каталога; обновление `site/`; `SettingsActivity` shelf/filter под Lite; деплой `server/patcher.py` (только если понадобится вход/бета).
