# Flixvora — Bug Audit (v9.1 / versionCode 40)

Audit performed against the live official API (tabId catalogue dump) and a full read of
`ui/`, `data/repository/`, `data/network/`, `data/model/`, `data/preferences/`.

## Verified data facts (used by the fixes)

Probed `GET /wefeed-mobile-bff/tab-operating?tabId=9` — the adult tab — and compared
every subject against tabs 1/2/5/8/18:

| signal | adult tab (9) | normal tabs (1/2/5/8/18) |
|---|---|---|
| `restrictKid` | `1` on 91 of 130 subjects | `0` on **all** 635 subjects |
| `genre` | contains `Adult`, `Erotic`, `AiAdult`, `Hentai` | never contains those words |
| subjectId overlap with adult tab | — | **0 of 635** |

So adult titles never leak into the normal tabs by id, but 39 of the 130 adult-tab
subjects carry `restrictKid=0` and a bland genre (`Drama`, `Comedy`, `Horror`).
A genre-only or `restrictKid`-only test therefore misses real adult titles —
the reliable rule is **genre marker OR `restrictKid=1` OR adult-tab provenance**.

---

## Bug list

### B1 — Adult content has no user control and leaks through uncategorised paths (P0)
`ManageCategoriesScreen.kt:31` exposes `9 to "18+"` as a toggleable category. When a
user adds it, `getCategoryList(9)` / `getCategorySections(9)` and `getTrending()` all
return adult rows, and the interleaved home feed shows them with no gate, no warning
and no way to turn them off. Nothing in `PreferencesManager` records an adult
preference at all.
**Fix:** add an `adultContentEnabled` preference (default **false**) plus a Settings
toggle, and filter adult subjects centrally.

### B2 — `mapItem()` drops `genre` and `restrictKid`, so adult rows are unidentifiable (P0)
`LocalMovieBoxServer.kt:3038-3076` returns only `subjectId/title/poster/rating/year/
subjectType/runtime/description`. The official adult discriminators (`genre`,
`restrictKid`) are discarded before the client ever sees them, so no downstream filter
can work.
**Fix:** pass `genre`, `restrictKid` and a derived `isAdult` flag through `mapItem`.

### B3 — `MovieSubject` model has no adult flag, and the JSON adapter loses it (P0)
`MovieModels.kt:32-50` has no `isAdult`; `MovieSubjectAdapter.fromJson` never reads it
and `toJson` never writes it, so cached feed files silently drop the flag.
**Fix:** add `isAdult: Boolean = false` to the model, read it in `fromJson`
(genre markers + `restrictKid` + explicit `isAdult`), and write it in `toJson`.

### B4 — Every repository read path returns adult rows unconditionally (P0)
`getTrending`, `getCategoryList`, `getCategorySections`, `getDetailRec`, `search`,
`getRankingList`, `getGenreTop`, `getDailyMovieRec`, `getTabOperatingSubjects`,
`getAnimeRails`, and the `getCached*` readers all return raw API rows.
**Fix:** funnel them through one `filterAdult()` helper driven by the preference.

### B5 — Disk/memory caches are keyed without the adult preference (P1)
`getCacheFile("feed_mov_${key}.json")` and `memoryCategoryCache` are keyed by category
id only. After toggling the setting the user keeps seeing stale rows until the 5-minute
TTL expires, and a cache written while adult was ON keeps serving adult rows after it
is turned OFF.
**Fix:** purge feed caches on preference change, and re-filter on every cache read.

### B6 — `getCachedTrending()` returns a stale copy while the real list mutates (P1)
`MovieRepository.kt:150-159`: when the memory cache is non-empty it returns
`memoryTrendingCache.toList()`, but `getTrending()` appends to that same list, so the
"cached" path can return a partially-filled list and the UI shows fewer rows than
were fetched.
**Fix:** return a filtered snapshot consistently.

### B7 — Watch-hours stat is integer-truncated per item, then summed (P2)
`SettingsScreen.kt:71` `groupedHistory.sumOf { it.seeTime } / 3_600_000L`. `seeTime` is
milliseconds, so the division is right, but the result is `Long` hours — a user with
59 minutes of history sees `0`. The demo shows a decimal (`6.4`).
**Fix:** compute in minutes and format with one decimal.

### B8 — `formatTabSections()` filters on the substring `"Star"` and `"Short"` (P1)
`LocalMovieBoxServer.kt:2787-2791` drops any section whose *title* contains "Star" or
"Short". That silently removes legitimate rows — the official tab 1 response contains
`Indian Stars`, but also real content rails such as `Late-night Shorts` on other tabs,
and any future "Star Wars" style rail.
**Fix:** match the exact promo section titles instead of substrings.

### B9 — `search()` builds a fresh OkHttp/Retrofit client per call path (P2)
`MovieBoxRetrofitClient.getApi` is cached, but `getSearchSuggestions` and several
extended calls create ad-hoc `OkHttpClient` instances; every keystroke in Search can
spin up a new connection pool.
**Fix:** reuse the cached client.

### B10 — `loadSearchSuggestions()` has no cancellation, so results race (P1)
`MovieViewModel.kt:965-975` launches a new coroutine per keystroke with no job handle.
A slow response for `"av"` can overwrite the suggestions for `"avengers"`.
**Fix:** keep a job handle and cancel the previous request.

### B11 — Search pagination is permanently disabled after a short result page (P2)
`searchMovies` sets `canSearchLoadMore = results.size >= 10`, but `search()` returns
`movieBox + anime` *before* de-duplication is counted for the threshold and page 1 is
capped at 30. Once a page returns 9 items, `loadMore` can never run again even though
later pages exist.
**Fix:** derive the flag from the raw page size, not the merged list.

### B12 — `getCachedEpisodes()` returns an empty list when only the cache is warm (P2)
`MovieRepository.kt:433-449` reads `detailCache[subjectId]`, which is only populated by
`getDetail`. A subject opened from a cached feed shows a spinner until the network
detail call completes.
**Fix:** fall back to the disk-cached detail.

### B13 — `isAniFamSubject()` treats every non-`mb_` id as AniFam when the flag is set (P1)
`MovieRepository.kt:452-454`: `(isAniFam && !subjectId.startsWith("mb_"))`. MovieBox
subject ids are numeric strings with no prefix, so any numeric id is classified as
AniFam whenever `isAniFam` is true, which routes playback and downloads to the wrong
API.
**Fix:** classify strictly on the `anifam_` prefix.

### B14 — `AppPreferences.isAniFam` is hard-wired `false` but the ViewModel derives its own (P2)
`PreferencesManager.kt:33-35` always returns `false`, while
`MovieViewModel.kt:217-218` derives it from the selected subject. Two sources of truth
for the same concept; the repository consults the (always-false) preference, the
ViewModel consults the subject.
**Fix:** document the single source and make the repository use the same predicate.

### B15 — Settings has no Playback / Subtitle / Appearance rows the demo shows (P2)
The demo `Me` tab lists `Playback Settings`, `Subtitle Language`, `Appearance` and
`About`. The app shows only Location, Manage Categories and Check for Updates.
`defaultStreamQuality` and `defaultDownloadQuality` exist in preferences but no UI
exposes them.
**Fix:** add the rows and wire them to the existing preferences.

### B16 — Downloads screen never refreshes storage meter after a delete (P2)
`MbStorageMeter` is composed without a key that changes on delete, so the bar keeps the
pre-delete value until the screen is re-entered.

### B17 — `LiveTvScreen` swallows every failure as "No channels available" (P2)
`errorMessage = e.localizedMessage ?: "Failed to load channels"` — a network timeout
and an empty catalogue are indistinguishable, and there is no retry affordance.

### B18 — `ManageCategoriesScreen` offers the adult category with no gate (P1)
Even with B1 fixed, the 18+ row stays visible and toggleable regardless of the
preference, which contradicts the toggle's meaning.
**Fix:** hide the 18+ row unless adult content is enabled.

### B19 — Update manifest failure is silent on manual check (P3)
`checkForUpdates(isManualCheck = true)` surfaces a toast, but a thrown exception inside
the `try` bypasses `onComplete` entirely in the network-error branch.
**Fix:** always invoke the callback.

### B20 — `DetailScreen` genre chips render raw API tokens (P3)
Genres come through as `"Adult, Erotic\t"` (trailing tab) from the API. The adapter
splits on `,` and trims, but the trailing `\t` survives `trim()` only for spaces.

### B21 — Home feed has no "Continue Watching" ordering guarantee (P3)
`continueWatching` is built from `history` without sorting by `timestamp` in the
composable, so the rail order depends on Room's query order rather than most-recent.

### B22 — `PlayerScreen` progress is not persisted on back press (P3)
Exiting the player via the system back button skips the `saveProgress` path that the
in-player back arrow uses, so progress is lost for that session.

---

## Fix status (v9.1)

| Bug | Status |
|---|---|
| B1, B2, B3, B4, B5, B18 | Fixed — `adultContentEnabled` pref (default off), `isAdult` on the model + JSON adapter, central `filterAdult*` helpers, caches cleared on toggle, 18+ row hidden unless enabled. |
| B6 | Fixed — cached readers return filtered snapshots. |
| B7 | Fixed — `formatWatchHours()` keeps one decimal under 10h. |
| B8 | Fixed — exact promo titles, case-insensitive, no substring match. |
| B9 | Fixed — shared cached client. |
| B10 | Fixed — `searchJob` cancelled per keystroke. |
| B11 | Fixed — pagination flag from raw page size. |
| B12 | Fixed — detail written to disk; `getCachedDetail`/`getCachedEpisodes` read it back. |
| B13 | Fixed — routing is `startsWith("anifam_")` only. |
| B14 | Fixed — documented single source; repo predicate matches the ViewModel. |
| B15 | Fixed — Playback + Subtitle rows wired to `defaultStreamQuality` / `subtitleLanguage`. |
| B16 | Fixed — storage meter keyed on the download count. |
| B17 | Fixed — retry re-runs the load; empty catalogue and network failure report differently. |
| B19 | Fixed — every branch invokes `onComplete`. |
| B20 | Fixed — genre tokens trimmed of `\t`. |
| B21 | Fixed — Continue Watching sorted by `timestamp` before de-duplication. |
| B22 | Fixed — progress flushed in the player's `onDispose`. |
| B23 | Fixed (9.3) — detail screen no longer invents Screenshots, Production Timeline, Trivia or Subtitle Preview; those sections had no API field. Download Quality reads real `StreamItem`s via `viewModel.detailStreams`; Details table built from real subject fields. |
| B24 | Fixed (9.4) — adult filter leaked because marking relied on genre text only; the catalogue labels 18+ titles "UllU Drama"/"Vivamax". `/home/v2/get-list` now passes `forceAdult = categoryId == 9` through `formatTabSections` into `mapItem`, and the adult tab no longer falls back to tabId=1. |

## Status legend

- **P0** — must fix before release (adult gate correctness)
- **P1** — user-visible wrong behaviour
- **P2** — latent/edge-case or quality
- **P3** — polish
