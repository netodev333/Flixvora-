# 📺 Flixvora Android TV Screen Specifications

> 10-foot UI Design for Android TV  
> D-Pad Remote Navigation | Focus States | Leanback  
> Resolution: 1920x1080 (1080p)  
> Version: 10.0

---

## 🎯 TV DESIGN PRINCIPLES

| Principle | Specification |
|-----------|--------------|
| **Viewing Distance** | 10 feet (3 meters) from screen |
| **Minimum Text Size** | 28sp for body, 48sp+ for titles |
| **Focus States** | Scale 1.05x, shadow `elevation/3`, border 2dp brand primary |
| **Navigation** | D-Pad (Up, Down, Left, Right, Select, Back) |
| **Touch Target** | N/A (remote only) |
| **Safe Area** | 60dp overscan margin on all sides |
| **Grid** | 12-column TV grid |

---

## TV SCREEN 1: HOME (Leanback Browse)

**Route:** `/tv/home`  
**Layout:** BrowseFragment pattern

### Layout (1920x1080)
```
┌─────────────────────────────────────────────────────────────────┐
│  ⬅  🎬 Flixvora    🔍 Search   👤 Profile   ⚙️ Settings        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    HERO BANNER                          │   │
│  │                                                          │   │
│  │  ┌──────────────┐                                        │   │
│  │  │   POSTER     │  Title of Featured Content             │   │
│  │  │   (2:3)      │                                        │   │
│  │  │              │  [▶ Play]  [ℹ Info]  [📥 Add]          │   │
│  │  └──────────────┘                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Trending Now                    [See All ▶]                   │
├─────────────────────────────────────────────────────────────────┤
│  [Card] [Card] [Card] [Card] [Card] [Card] [Card] [Card] >     │
│   ↑ Focus: scale 1.05x, shadow, border                          │
├─────────────────────────────────────────────────────────────────┤
│  Adult Content: ON  🔞                                          │
├─────────────────────────────────────────────────────────────────┤
│  Recommended For You          [See All ▶]                      │
├─────────────────────────────────────────────────────────────────┤
│  [Card] [Card] [Card] [Card] [Card] [Card] [Card] [Card] >     │
├─────────────────────────────────────────────────────────────────┤
│  TV Channels                        [See All ▶]                │
├─────────────────────────────────────────────────────────────────┤
│  [Ch] [Ch] [Ch] [Ch] [Ch] [Ch] [Ch] [Ch] >                     │
└─────────────────────────────────────────────────────────────────┘
```

### TV Specifications

#### Top Navigation Bar
- **Height:** 80dp
- **Items:** Home, Search, Profile, Settings (4 items max)
- **Focus:** Underline 6dp `color/brand/primary`
- **Text:** `type/headline-medium` (24sp min)

#### Hero Banner
- **Height:** 540dp
- **Width:** Full (1920dp minus margins)
- **Poster:** 450dp x 675dp (2:3 aspect)
- **Title:** `type/display-large` (48sp+)
- **Action Buttons:** 3 buttons row - Play (primary), Info (outline), Add (outline)
- **Focus:** Left side focus on poster card

#### Content Shelves (Rows)
- **Header Height:** 64dp
- **Card Width:** 320dp (poster), 16:9 landscape also supported
- **Card Height:** 480dp (poster), 180dp (landscape)
- **Item Spacing:** `space/24` (32dp)
- **Row Height:** 540dp (includes focus scaling)
- **Scroll:** Horizontal, D-pad Left/Right
- **Focus Behavior:** Card scales to 1.05x, moves to center row
- **Peek:** Last card shows 200dp

---

## TV SCREEN 2: CONTENT DETAIL

**Route:** `/tv/detail/:id`

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│  ⬅  🎬 Flixvora    🔍  👤  ⚙️                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    POSTER (Large)                       │   │
│  │                                                          │   │
│  │  ┌──────────────────┐                                     │   │
│  │  │   MOVIE POSTER   │                                     │   │
│  │  │   (4:5 aspect)   │                                     │   │
│  │  └──────────────────┘                                     │   │
│  │  🔞 16+  ★ 8.2  2024  Action  Drama  120 min             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Movie Title Here                                               │
│                                                                 │
│  A gripping story about...                                      │
│  (Full description text, scrollable)                            │
│                                                                 │
│  Cast: Actor 1  •  Actor 2  •  Actor 3  •  Director: Name       │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │   ▶ Play     │  │   📥 Download│  │   ➕ Add to   │        │
│  │   Trailer    │  │     Now      │  │   Watchlist  │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
│                                                                 │
│  Seasons (if TV show)                                           │
│  [ S1 ] [ S2 ] [ S3 ] [ S4 ]                                    │
│                                                                 │
│  Similar Content                                                │
│  [Card] [Card] [Card] [Card] [Card] >                           │
└─────────────────────────────────────────────────────────────────┘
```

### TV Specifications

#### Focus Order (D-Pad)
1. Back button (top left)
2. Poster card
3. Play button
4. Download button
5. Add to watchlist
6. Season tabs
7. Similar content cards

#### Buttons (TV Optimized)
- **Height:** 80dp
- **Width:** 300dp each
- **Text:** `type/headline-medium` (24sp)
- **Icon:** 32dp leading
- **Focus:** Scale 1.1x, brand primary background

---

## TV SCREEN 3: LIVE TV CHANNELS

**Route:** `/tv/live`

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│  ⬅  🎬 Flixvora    🔍  👤  ⚙️                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Live TV                    [All Categories ▶]                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Ch] [Ch] [Ch] [Ch] [Ch] [Ch] [Ch] [Ch]                        │
│  HBO   CNN   ESPN  BBC  Disney  NatGeo  Discovery  CNN Intl     │
│  ●LIVE ●LIVE   ●     ●LIVE  ●LIVE   ●     ●      ●LIVE          │
│                                                                 │
│                                                                 │
│  Now Playing: Movie Title                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    PREVIEW WINDOW                        │   │
│  │                                                          │   │
│  │  [⏯ Play/Pause] [⏹ Stop] [🔊 Volume] [📺 Fullscreen]   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  EPG Guide                                                      │
│  ┌──────────┬─────────────────────────────────────────────┐   │
│  │  8:00 PM │  Movie Title                                │   │
│  │  9:00 PM │  News Hour                                  │   │
│  │  10:00 PM│  Sports Live                                │   │
│  └──────────┴─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### TV Specifications

#### Channel Grid
- **Layout:** 2 rows x 8 columns (adjusts to screen width)
- **Card Size:** 200dp x 240dp (includes label)
- **Channel Logo:** 120dp x 120dp circle
- **Live Dot:** 16dp circle, `color/adult/restricted`, top-right
- **Focus:** Channel scales 1.1x, preview updates

#### Preview Window
- **Position:** Bottom right, 640dp x 360dp
- **Controls:** Overlay bottom, auto-hide 3 seconds
- **Focus:** Play/Pause button focused by default

#### EPG Guide
- **Row Height:** 64dp
- **Time Column:** 120dp width
- **Program Column:** Rest of width
- **Scroll:** Vertical D-pad

---

## TV SCREEN 4: SETTINGS (TV)

**Route:** `/tv/settings`

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│  ⬅  Settings                                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Account                                                        │
├─────────────────────────────────────────────────────────────────┤
│  👤  Profile                                                      │
│  📧  Email                                                        │
│  🔑  Change Password                                              │
├─────────────────────────────────────────────────────────────────┤
│  Content                                                        │
├─────────────────────────────────────────────────────────────────┤
│  🔞  Adult Content    [ OFF  ○●  ON ]                          │
│     Filter adult content                                        │
├─────────────────────────────────────────────────────────────────┤
│  App                                                            │
├─────────────────────────────────────────────────────────────────┤
│  🔄  Check for Updates                                          │
│  v9.7 → v10.0 available                                         │
│  [ Install Now ]                                                │
├─────────────────────────────────────────────────────────────────┤
│  Support                                                        │
├─────────────────────────────────────────────────────────────────┤
│  📞  Contact Support                                            │
│  📄  Privacy Policy                                             │
│  📄  Terms of Service                                           │
│  🚪  Logout                                                     │
└─────────────────────────────────────────────────────────────────┘
```

### TV Specifications

#### List Items
- **Height:** 80dp
- **Padding:** `space/24` (32dp) horizontal
- **Icon:** 40dp leading
- **Text:** `type/headline-medium` (24sp)
- **Chevron:** 32dp trailing
- **Toggle:** 72dp x 36dp, trailing on toggle rows
- **Focus:** Background `color/surface/raised`, left accent bar 8dp brand primary

---

## TV SCREEN 5: UPDATE DIALOG (TV)

**Route:** Modal overlay

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│          ⬇  Update Available                                    │
│                                                                 │
│    Flixvora v10.0 is ready to install.                         │
│                                                                 │
│    New in this version:                                         │
│    • Adult content filter fixed                                 │
│    • TV channels complete                                       │
│    • Install button persists                                    │
│    • GitHub Releases API updates                                │
│                                                                 │
│          [ Install Update ]   [ Later ]                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
              Centered, 900dp x 600dp
```

### TV Specifications
- **Width:** 900dp, Height: 600dp
- **Background:** `color/surface/elevated` with scrim behind
- **Border Radius:** `radius/xl` (24dp for TV)
- **Title:** `type/display-medium` (36sp)
- **Body:** `type/body-large` (24sp)
- **Buttons:** 300dp width, 80dp height, side by side
- **Focus:** Default focus on "Install Update" (primary)
- **D-Pad:** Left/Right moves between buttons, Select activates

---

## TV SCREEN 6: FORCE UPDATE FULL SCREEN (TV)

**Route:** `/tv/force-update`

### Layout
```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                                                                 │
│              🔄                                                 │
│                                                                 │
│          UPDATE REQUIRED                                        │
│                                                                 │
│  A new version of Flixvora is required to continue.             │
│                                                                 │
│  Current: v9.7         Latest: v10.0                            │
│                                                                 │
│                                                                 │
│          [ INSTALL NOW - 6.7 MB ]                               │
│                                                                 │
│                                                                 │
│  ↳ Downloading... 78%                                           │
│  ▓▓▓▓▓▓▓▓▓▓░░░░░░                                                │
│                                                                 │
│                                                                 │
│  Install button appears after download completes                │
└─────────────────────────────────────────────────────────────────┘
```

### TV Specifications
- **Centered card:** 1000dp x 700dp
- **Icon:** 80dp refresh icon
- **Title:** `type/display-large` (48sp)
- **Install Button:** 500dp width, 100dp height
- **Progress Bar:** 600dp width, 8dp height
- **Focus:** Install button (disabled until download complete)

---

## 📺 TV COMPONENT SIZE REFERENCE

| Component | Mobile | Tablet | TV |
|-----------|--------|--------|-----|
| Base Text | 16sp | 18sp | 28sp+ |
| Button Height | 48dp | 52dp | 80dp |
| Card Radius | 12dp | 16dp | 24dp |
| Focus Scale | 1.0x | 1.0x | 1.05x-1.1x |
| Touch Target | 48dp | 48dp | N/A |
| Focus Indicator | None | None | Border + Shadow |
| Navigation | Touch | Touch | D-Pad Remote |

---

## 🎮 D-PAD FOCUS MAP (TV)

```
[ Home ]  [ Search ]  [ Profile ]  [ Settings ]
    ↓
[ Hero Banner Content ]
    ↓
[ Shelf 1: Card 1 ] → [ Card 2 ] → [ Card 3 ] → ...
    ↓ (Down)
[ Shelf 2: Card 1 ] → [ Card 2 ] → ...
    ↓
[ Settings / Actions ]
```

**Focus Rules:**
1. D-Pad Right/Left: Move within row
2. D-Pad Down: Move to next row (first item)
3. D-Pad Up: Move to previous row (aligned item)
4. Select: Activate focused component
5. Back: Return to previous screen

---

## 🎨 TV COLOR ADAPTATIONS

| Token | Mobile | TV | Reason |
|-------|--------|-----|--------|
| Background | #0A0A0B | #0A0A0B | Same deep black |
| Surface Elevated | #141417 | #141417 | Same |
| Text Primary | #FFFFFF | #FFFFFF | Same |
| Border | #27272A | #27272A | Same |
| Focus Border | N/A | #6D28D9 | TV needs visible focus |
| Card Shadow | elevation/1 | elevation/2 | TV needs more depth |
| Text Size | 16sp | 28sp+ | 10-foot viewing |

---
