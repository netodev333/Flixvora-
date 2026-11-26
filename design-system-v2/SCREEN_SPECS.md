# 📱 Flixvora Mobile Screen Specifications

> All screens designed for Android Mobile (360dp - 840dp width)  
> Dark Theme | 8pt Grid | Bottom Navigation  
> Version: 10.0

---

## SCREEN 1: SPLASH / ONBOARDING

**Route:** `/splash`  
**Status Bar:** Transparent, white icons  
**Background:** `color/surface/background` (#0A0A0B)

### Layout (375dp x 812dp)
```
┌─────────────────────────────────────┐
│                                     │
│         🎬  FLIXVORA                │
│      (Logo 120dp x 40dp)            │
│                                     │
│      Stream Unlimited               │
│     Movies & TV Shows               │
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
│      [ Get Started ]  [ Sign In ]   │
│                                     │
└─────────────────────────────────────┘
```

### Components
- **Logo:** Center top, 120dp width
- **Headline:** `type/display-large`, `color/text/primary`, center
- **Subhead:** `type/body-large`, `color/text/secondary`, center, max 280dp
- **CTA Buttons:** Stacked vertically, bottom 40dp padding
  - Primary: "Get Started" (brand primary)
  - Secondary: "Sign In" (ghost)
- **Spacing:** `space/24` between major sections

---

## SCREEN 2: SIGN IN / LOGIN

**Route:** `/auth/login`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  ╭──────────────────────────────╮  │
│  │       🎬  FLIXVORA           │  │
│  │                              │  │
│  │      Welcome Back            │  │
│  │   Sign in to continue        │  │
│  │                              │  │
│  │  ┌──────────────────────┐   │  │
│  │  │  Email               │   │  │
│  │  │  you@example.com     │   │  │
│  │  └──────────────────────┘   │  │
│  │                              │  │
│  │  ┌──────────────────────┐   │  │
│  │  │  Password            │   │  │
│  │  │  ••••••••    👁️      │   │  │
│  │  └──────────────────────┘   │  │
│  │                              │  │
│  │  ┌──────────────────────┐   │  │
│  │  │     [ SIGN IN ]      │   │  │
│  │  └──────────────────────┘   │  │
│  │                              │  │
│  │  Don't have an account?      │  │
│  │         [ Sign Up ]          │  │
│  │                              │  │
│  │  Or continue with:           │  │
│  │  [ Google ] [ Facebook ]     │  │
│  ╰──────────────────────────────╯  │
└─────────────────────────────────────┘
```

### Components
- **Card:** Centered, 300dp max-width, `color/surface/elevated`, `radius/lg`, padding `space/16`
- **Input Fields:** Height 52dp, `color/border`, focus `color/border/focus`
- **Password Toggle:** Eye icon, right side, `color/text/tertiary`
- **Primary Button:** Full width, 48dp, brand primary
- **Social Buttons:** Outline style, `color/border`, icon + text

---

## SCREEN 3: HOME (Dashboard)

**Route:** `/home`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  🎬 Flixvora    🔍  👤  🔔         │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │  Featured Movie Poster      │   │
│  │  [▶ Play] [ℹ Info] [📥 DL]  │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  Trending Now        ▶ See All >    │
├─────────────────────────────────────┤
│  [C] [C] [C] [C] [C] [C] [C] >      │
├─────────────────────────────────────┤
│  Adult Content: ON  🔞              │
├─────────────────────────────────────┤
│  Recommended For You   ▶ See All >  │
├─────────────────────────────────────┤
│  [C] [C] [C] [C] [C] [C] [C] >      │
├─────────────────────────────────────┤
│  TV Channels                        │
├─────────────────────────────────────┤
│  [📺] [📺] [📺] [📺] [📺] [📺] >     │
└─────────────────────────────────────┘
     [🏠 Home] [🔍 Search] [📺 TV] [⚙️]
```

### Sections

#### Hero Banner (Full Width)
- **Height:** 500dp
- **Background:** Movie poster (16:9), linear gradient overlay bottom
- **Play Button:** 60dp circle, brand primary, center-left
- **Info Button:** Outline, right of play
- **Download Button:** Outline, right of info
- **Title Overlay:** `type/display-large`, bottom 60dp padding
- **Metadata Row:** Rating, Year, Age Badge, Duration

#### Content Shelves
- **Header:** `type/headline-medium` + "See All" (right)
- **Shelf Height:** 260dp (card + title)
- **Card Width:** 120dp (mobile), aspect 2:3
- **Horizontal Scroll:** Snap to items
- **Adult Filter Banner:** When adult ON, show red warning banner `color/adult/restricted`

#### TV Channels Shelf
- **Card:** Square 100dp, channel logo, channel name below
- **Live Indicator:** Red dot `color/adult/restricted` if live

---

## SCREEN 4: SEARCH

**Route:** `/search`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  🎬 Flixvora    🔍  👤  🔔         │
├─────────────────────────────────────┤
│                                     │
│  🔍  Search movies, shows...        │
│                                     │
│  Recent Searches                    │
│  ┌─────┐ ┌─────┐ ┌─────┐            │
│  │ Action│ │ Drama│ │ Horror│        │
│  └─────┘ └─────┘ └─────┘            │
├─────────────────────────────────────┤
│  Categories                         │
├─────────────────────────────────────┤
│  [🎬 Movies] [📺 TV Shows] [📺 Live] │
│  [🔞 Adult] [👶 Kids] [💎 Premium]   │
├─────────────────────────────────────┤
│  Results (when typing)              │
│  [C] [C] [C] [C] [C] >              │
└─────────────────────────────────────┘
     [🏠] [🔍 Active] [📺] [⚙️]
```

### Components
- **Search Bar:** Sticky top, 56dp height, icon leading, clear (x) trailing
- **Recent Chips:** `radius/full`, height 32dp, `color/surface/raised`
- **Category Grid:** 2 columns, cards with icon + label
- **Results:** Horizontal shelves, debounced 300ms

---

## SCREEN 5: MOVIE / SHOW DETAIL

**Route:** `/detail/:id`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  ⬅  🔍                           │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │         POSTER              │   │
│  │         (2:3)               │   │
│  │   🔞 16+  ★ 8.2  NEW        │   │
│  └─────────────────────────────┘   │
│                                     │
│  Movie Title Here                   │
│  Action • Drama • 2024 • 120 min    │
│                                     │
│  A gripping story about...          │
│  (description text, expandable)     │
│                                     │
│  Cast: Actor 1, Actor 2, Actor 3    │
│  Director: Name                     │
│                                     │
│  ┌──────────┐  ┌──────────┐        │
│  │  ▶ Play  │  │  📥 Download │    │
│  │  Trailer │  │    (5 GB)    │    │
│  └──────────┘  └──────────┘        │
│                                     │
│  Episodes / Seasons                 │
│  ┌─────┐ ┌─────┐ ┌─────┐            │
│  │ S1  │ │ S2  │ │ S3  │            │
│  └─────┘ └─────┘ └─────┘            │
│                                     │
│  Similar Movies                     │
│  [C] [C] [C] [C] >                  │
└─────────────────────────────────────┘
```

### Components
- **Poster:** 180dp width, 2:3 aspect, `radius/lg`
- **Badges:** Top-left corner, age rating + "NEW" tag
- **Action Buttons:** Two buttons side by side (Play primary, Download secondary)
- **Seasons:** Horizontal scroll chips
- **Similar Content:** Horizontal shelf below

---

## SCREEN 6: TV CHANNELS (Live)

**Route:** `/tv`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  🎬 Flixvora    🔍  👤  🔔         │
├─────────────────────────────────────┤
│  Live TV                            │
│  12 channels available              │
├─────────────────────────────────────┤
│  [📺] [📺] [📺] [📺] [📺] [📺]       │
│  HBO    CNN   ESPN  BBC  Disney  Nat │
│  ●LIVE  ●LIVE   ●     ●LIVE  ●LIVE   │
├─────────────────────────────────────┤
│  Categories                         │
│  [News] [Sports] [Movies] [Kids]     │
├─────────────────────────────────────┤
│  EPG - TV Guide                     │
│  ┌─────────────────────────────┐   │
│  │  8:00 PM  Movie Title       │   │
│  │  9:00 PM  News Hour         │   │
│  │  10:00 PM  Sports Live      │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Components
- **Channel Card:** Square 80dp, logo center, name below, live dot top-right
- **Category Filter:** Horizontal scroll chips
- **EPG List:** List items, time + title, `type/body-medium`
- **Live Indicator:** Red animated dot, `color/adult/restricted`

---

## SCREEN 7: SETTINGS

**Route:** `/settings`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  ⬅  Settings                      │
├─────────────────────────────────────┤
│  Account                            │
├─────────────────────────────────────┤
│  👤  Profile                        │
│  📧  Email                          │
│  🔑  Change Password                │
├─────────────────────────────────────┤
│  Content                            │
├─────────────────────────────────────┤
│  🔞  Adult Content    [ ON  ●○ ]   │
│     Filter adult movies & shows     │
├─────────────────────────────────────┤
│  App                                │
├─────────────────────────────────────┤
│  🌙  Theme       [ Dark ☐ Light ]   │
│  📱  Download Quality               │
│  🔔  Notifications   [ ON ☐ ]       │
│  🔄  Check for Updates              │
│  v9.7 → v10.0 available             │
├─────────────────────────────────────┤
│  Support                            │
├─────────────────────────────────────┤
│  📞  Contact Support                │
│  📄  Privacy Policy                 │
│  📄  Terms of Service               │
│  🚪  Logout                         │
└─────────────────────────────────────┘
```

### Components
- **Section Header:** `type/headline-medium`, `color/text/secondary`, uppercase
- **List Item:** Height 56dp, icon left, text, chevron right
- **Toggle:** Switch component, trailing
- **Version Row:** Shows current vs available version
- **Logout:** `color/adult/restricted` text color

---

## SCREEN 8: DOWNLOADS

**Route:** `/downloads`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  ⬅  Downloads                     │
├─────────────────────────────────────┤
│  3 downloads • 12.4 GB used         │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │  [Poster]  Movie Title      │   │
│  │     ✓ Complete  2.1 GB      │   │
│  │     [▶ Play] [🗑 Delete]    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  [Poster]  Show Episode     │   │
│  │     ↓ Downloading 65%       │   │
│  │     ▓▓▓▓▓▓▓░░░              │   │
│  │     [⏸ Pause] [🗑 Delete]   │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Components
- **Download Item:** Poster left, info center, actions right
- **Progress Bar:** Height 4dp, `color/brand/accent`, buffered gray
- **Status Badges:** Complete (green), Downloading (orange), Failed (red)
- **Empty State:** Illustration + "No downloads yet"

---

## SCREEN 9: UPDATE DIALOG (Overlay)

**Route:** Modal over any screen  
**Trigger:** App update check

### Layout
```
┌─────────────────────────────────────┐
│  ⬇  Update Available                │
│                                     │
│  Flixvora v10.0 is ready to install.│
│                                     │
│  New in this version:               │
│  • Adult content filter fixed       │
│  • TV channels complete             │
│  • Install button persists          │
│  • GitHub Releases API updates      │
│                                     │
│  [ Install Update ]  [ Later ]      │
└─────────────────────────────────────┘
```

### Components
- **Dialog:** `color/surface/elevated`, `radius/xl`, centered
- **Title:** `type/headline-large`, brand primary icon
- **Body:** `type/body-medium`, bullet points
- **Changelog:** `type/caption`, `color/text/tertiary`
- **Buttons:** Primary (Install) + Secondary (Later)

---

## SCREEN 10: FORCE UPDATE FULL SCREEN

**Route:** `/force-update`  
**Trigger:** Critical update required

### Layout (Full Screen)
```
┌─────────────────────────────────────┐
│                                     │
│              🔄                     │
│         UPDATE REQUIRED             │
│                                     │
│  A new version of Flixvora is       │
│  required to continue using the app.│
│                                     │
│  Current: v9.7                      │
│  Latest:  v10.0                     │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  [ INSTALL NOW - 6.7 MB ]   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ↳ Downloading... 78%               │
│  ▓▓▓▓▓▓▓▓▓▓░░                        │
│                                     │
│  Install button appears after       │
│  download completes                 │
└─────────────────────────────────────┘
```

### Components
- **Icon:** 64dp, refresh icon, brand primary
- **Title:** `type/display-large`, center
- **Message:** `type/body-large`, center
- **Version Info:** `type/caption`, `color/text/tertiary`
- **Install Button:** Full width, 56dp, brand primary, disabled until download complete
- **Progress Bar:** Full width, 6dp height, below button

---

## SCREEN 11: ADULT CONTENT TOGGLE (in Settings)

**Route:** `/settings`  
**Component:** Toggle switch

### Layout
```
┌─────────────────────────────────────┐
│  🔞  Adult Content                  │
│                                     │
│  When ON, adult movies and shows    │
│  will appear in search results and  │
│  recommendations.                   │
│                                     │
│  [ OFF  ○●  ON ]                    │
│                                     │
│  Requires PIN to change             │
└─────────────────────────────────────┘
```

### Components
- **Toggle:** 52dp x 28dp track, 20dp thumb
- **ON State:** `color/brand/primary`, thumb right
- **OFF State:** `color/border`, thumb left
- **Warning Text:** `color/adult/restricted`, `type/caption`
- **PIN Protection:** Modal dialog when toggling

---

## SCREEN 12: PROFILE

**Route:** `/profile`  
**Background:** `color/surface/background`

### Layout
```
┌─────────────────────────────────────┐
│  ⬅  Profile                       │
├─────────────────────────────────────┤
│                                     │
│         [ Avatar 120dp ]            │
│                                     │
│      John Doe                       │
│      john@example.com               │
│                                     │
│  🎬 Premium Member                  │
│     Expires: Dec 2026               │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Watch History              │   │
│  │  [Movie] [Show] [Movie] >   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Watchlist                  │   │
│  │  [Movie] [Show] [Movie] >   │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Components
- **Avatar:** 120dp circle, `radius/full`, placeholder or image
- **Name:** `type/headline-large`, center
- **Email:** `type/body-medium`, `color/text/secondary`, center
- **Membership Badge:** `color/brand/accent` background, white text
- **Sections:** Watch History, Watchlist, Continue Watching

---

## NAVIGATION MAP

| Route | Screen | Nav Tab |
|-------|--------|---------|
| `/splash` | Splash | - |
| `/auth/login` | Sign In | - |
| `/home` | Home | Home |
| `/search` | Search | Search |
| `/detail/:id` | Detail | - |
| `/tv` | Live TV | TV |
| `/settings` | Settings | Settings |
| `/downloads` | Downloads | - |
| `/profile` | Profile | - |
| `/force-update` | Force Update | - |
| `/dialog/update` | Update Dialog | - |

---
