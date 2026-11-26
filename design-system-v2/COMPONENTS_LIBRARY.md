# 🧩 Flixvora Components Library

> Figma Component Library Specifications  
> All components designed for Dark Theme, 8pt Grid, Touch + Remote friendly  
> Version: 1.0

---

## 1. BUTTONS

### Primary Button
```
┌─────────────────────────────┐
│  ▶ Continue Watching        │
└─────────────────────────────┘
```
- **Background:** `color/brand/primary` (#6D28D9)
- **Text:** `color/text/inverse` (#000000), `type/label-large`, SemiBold
- **Height:** 48dp (mobile), 60dp (TV focus)
- **Border Radius:** `radius/md` (8dp)
- **Padding:** 16dp horizontal, 12dp vertical
- **States:** Default, Hover/Focus, Pressed, Disabled
- **Icon:** Leading icon 20dp, trailing optional

### Secondary Button (Outline)
```
┌─────────────────────────────┐
│  ☰ Install Update           │
└─────────────────────────────┘
```
- **Background:** Transparent
- **Border:** 1dp `color/border` (#27272A)
- **Text:** `color/text/primary` (#FFFFFF)
- **Focus Ring:** 2dp `color/border/focus` (#6D28D9) on TV

### Ghost Button (Text Only)
```
┌─────────────────────────────┐
│  Skip Intro                 │
└─────────────────────────────┘
```
- **Background:** Transparent
- **Text:** `color/text/secondary` (#A1A1AA), hover: `color/text/primary`
- **No border, no fill**

### Danger Button
```
┌─────────────────────────────┐
│  ⚠ Cancel Subscription      │
└─────────────────────────────┘
```
- **Background:** `color/adult/restricted` (#EF4444)
- **Text:** `color/text/inverse` (#000000)

---

## 2. CONTENT CARDS

### Movie/Show Card (Poster)
```
┌─────────────────────────┐
│  ┌─────────────────┐   │
│  │    POSTER       │   │
│  │    IMAGE        │   │
│  └─────────────────┘   │
│  Title of the Show      │
│  ★ 8.2  •  2024  •  16+ │
└─────────────────────────┘
```
- **Width:** 160dp (mobile), 280dp (TV)
- **Aspect Ratio:** 2:3 (poster), 16:9 (landscape)
- **Border Radius:** `radius/lg` (12dp)
- **Overlay:** Linear gradient bottom 40dp, black 60% opacity
- **Badge:** Top-right corner, `radius/sm`, 24dp height
- **Focus State (TV):** Scale 1.05, shadow `elevation/3`, border 2dp brand primary

### Content Shelf (Horizontal Row)
```
┌─────────────────────────────────────────┐
│  Trending Now           ▶ See All  >    │
├─────────────────────────────────────────┤
│  [Card] [Card] [Card] [Card] [Card] >   │
└─────────────────────────────────────────┘
```
- **Header:** `type/headline-medium` + "See All" text button
- **Item Spacing:** `space/8` (16dp)
- **Scroll:** Horizontal, snap to items
- **Peek:** Last card partially visible (50dp)
- **TV:** Focus moves item into center, scale up focused card

---

## 3. NAVIGATION

### Bottom Navigation (Mobile)
```
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│  🏠     │ │  🔍     │ │  📺     │ │  ⚙️     │
│  Home   │ │  Search │ │  TV     │ │  Settings│
└─────────┘ └─────────┘ └─────────┘ └─────────┘
     Active (Purple)
```
- **Height:** 64dp + safe area
- **Items:** 4-5 tabs max
- **Active:** `color/brand/primary`, icon 24dp, label `type/label-medium`
- **Inactive:** `color/text/tertiary`, icon 20dp
- **Badge:** Red dot for notifications

### Top Navigation Bar
```
┌─────────────────────────────────────────────┐
│  🎬 Flixvora     🔍 Search   👤 Profile  🔔 │
└─────────────────────────────────────────────┘
```
- **Height:** 56dp
- **Background:** `color/surface/elevated` with scrim blur
- **Logo:** Leading, 32dp height
- **Actions:** Trailing icons 24dp

### Side Rail (Tablet/Desktop)
```
┌─────────┬─────────────────────────────────┐
│  🎬     │                                   │
│  Home   │   Main Content Area               │
│  TV     │                                   │
│  Search │                                   │
│  Admin  │                                   │
│  ⚙️     │                                   │
└─────────┴─────────────────────────────────┘
 240dp width
```

---

## 4. INPUTS & FORMS

### Text Input
```
┌─────────────────────────────────────┐
│  Search movies, shows...  🔍        │
└─────────────────────────────────────┘
```
- **Height:** 48dp
- **Background:** `color/surface/elevated`
- **Border:** 1dp `color/border`, focus: 2dp `color/border/focus`
- **Border Radius:** `radius/md` (8dp)
- **Placeholder:** `color/text/tertiary`, `type/body-large`
- **Prefix/Suffix:** Icon 20dp

### Search Bar (Hero)
```
┌─────────────────────────────────────────────────┐
│  🔍  What do you want to watch?  [ mic ]  [ ↲ ] │
└─────────────────────────────────────────────────┘
```
- **Full width, 56dp height**
- **Background:** `color/surface/raised`
- **Shadow:** `elevation/2`
- **Voice search icon:** Right side
- **Enter/Submit:** Far right

---

## 5. MODALS & DIALOGS

### Update Dialog
```
┌─────────────────────────────────┐
│  ⬇  Update Available            │
│  Flixvora v10.0 is ready to     │
│  install. New features & fixes. │
│                                 │
│  [ Install Now ]  [ Later ]     │
└─────────────────────────────────┘
```
- **Width:** 280dp (mobile), 400dp (TV)
- **Background:** `color/surface/elevated`
- **Border Radius:** `radius/xl` (16dp)
- **Padding:** `space/16` (24dp)
- **Title:** `type/headline-large`
- **Body:** `type/body-medium`
- **Buttons:** Primary + Secondary, side by side

### Force Update Full Screen
```
┌─────────────────────────────────────────┐
│                                         │
│          🔄  UPDATE REQUIRED            │
│                                         │
│  A new version of Flixvora is           │
│  required to continue.                  │
│                                         │
│  Current: v9.7  →  Latest: v10.0        │
│                                         │
│  [ INSTALL NOW - 6.7 MB ]               │
│                                         │
│  ↳ Downloading... 45% ▓▓▓▓░░░░░░        │
└─────────────────────────────────────────┘
```
- **Background:** `color/surface/background` full screen
- **Centered content, max-width 400dp**
- **Progress bar:** `color/brand/accent`, height 4dp

---

## 6. ADULT CONTROL COMPONENTS

### Adult Toggle Switch
```
┌─────────────────────────────────┐
│  🔞 Adult Content  [ ON ●○ ]    │
│     Content filter is ACTIVE    │
└─────────────────────────────────┘
```
- **Track:** 36dp x 18dp, `radius/full`
- **Thumb:** 14dp circle
- **ON:** `color/brand/primary`, thumb right
- **OFF:** `color/border`, thumb left
- **Warning text:** `color/adult/restricted`, `type/caption`

### Age Rating Badge
```
┌───────┐
│  16+  │
└───────┘
```
- **Size:** 32dp x 20dp
- **Background:** `color/adult/restricted` (red) or `color/adult/safe` (green)
- **Text:** `type/label-medium`, white, centered
- **Border Radius:** `radius/sm`

---

## 7. PLAYER CONTROLS

### Video Player Overlay
```
┌─────────────────────────────────────┐
│  ⏸  ────────────────────────  🔊    │
│  02:15 ───────────●────────  45:30  │
│                                         │
│  [ ⏮ ]  [ ⏯ ]  [ ⏭ ]  [ 📺 ]  [ ⚙️ ] │
└───────────────────────────────────────┘
```
- **Background:** Scrim 60% black
- **Controls:** Bottom 80dp height
- **Progress bar:** 4dp height, buffered gray, played brand primary
- **Icons:** 24dp, white, `color/text/primary`
- **Auto-hide:** 3 seconds after interaction

---

## 8. TV-SPECIFIC COMPONENTS

### Focus State (D-Pad Remote)
```
┌─────────────┐
│  [ Card ]   │  ← Focused: scale 1.05, shadow, border
└─────────────┘
```
- **Scale:** 1.05x on focus
- **Shadow:** `elevation/3`
- **Border:** 2dp `color/brand/primary`
- **Transition:** 150ms ease-out

### TV Navigation Bar (Top)
```
┌─────────────────────────────────────────────────┐
│  🏠 Home   🔍 Search   📺 Live   ⚙️ Settings    │
└─────────────────────────────────────────────────┘
           [ ACTIVE INDICATOR ]
```
- **Height:** 72dp
- **Items:** 5 max, text + icon
- **Focus:** Underline 4dp `color/brand/primary`

---

## 9. BACKEND ADMIN COMPONENTS

### Admin Sidebar
```
┌─────────────────────────┐
│  🛡️  Flixvora Admin    │
├─────────────────────────┤
│  📊 Dashboard           │
│  🎬 Content Management  │
│  👥 Users               │
│  💳 Subscriptions       │
│  📈 Analytics           │
│  ⚙️ System Settings     │
│  🔓 Logout              │
└─────────────────────────┘
```

### Content Table
```
┌──────────┬─────────────────┬──────────┬──────────┬─────────┐
│  Poster  │  Title          │  Genre   │  Rating  │  Action │
├──────────┼─────────────────┼──────────┼──────────┼─────────┤
│  [img]   │  Movie Title    │  Action  │  ★ 8.2   │  ✏️ 🗑  │
└──────────┴─────────────────┴──────────┴──────────┴─────────┘
```

---

## 10. COMPONENT VARIANTS TABLE

| Component | Variants | States | Platforms |
|-----------|----------|--------|-----------|
| Button | Primary, Secondary, Ghost, Danger, Icon | Default, Hover, Pressed, Disabled, Loading | Mobile, TV, Web |
| Card | Poster, Landscape, Square, List | Default, Focused, Selected, Disabled | Mobile, TV, Web |
| Input | Text, Search, Password, Number | Default, Focus, Error, Disabled | Mobile, TV, Web |
| Toggle | Switch, Checkbox, Radio | On, Off, Indeterminate | Mobile, TV, Web |
| Badge | Rating, Age, Category, New, Hot | Default, Selected | Mobile, TV, Web |
| Dialog | Alert, Confirm, Update, Force Update | Default, Loading, Success, Error | Mobile, TV, Web |
| Nav | Bottom, Top, Side Rail, TV Top | Active, Inactive | Mobile, TV, Web |
| Player | Video, Audio, Live, Cast | Playing, Paused, Buffering, Error | Mobile, TV, Web |

---

## 11. ICON SYSTEM (24dp Base)

| Icon | Name | Usage |
|------|------|-------|
| 🏠 | home | Home navigation |
| 🔍 | search | Search |
| 📺 | tv | Live TV / Channels |
| ⚙️ | settings | Settings |
| 👤 | profile | User profile |
| 🔔 | notification | Notifications |
| ⬇ | download | Download / Install |
| ▶ | play | Play content |
| ⏸ | pause | Pause |
| 🔞 | adult | Adult content |
| 🔒 | lock | Restricted / Premium |
| ✅ | check | Success / Complete |
| ⚠ | warning | Warning / Error |
| ⭐ | star | Rating / Favorite |
| ➕ | add | Add to watchlist |
| ✏️ | edit | Edit content |
| 🗑 | delete | Delete content |
| 🔄 | refresh | Refresh / Reload |

---

## 12. FIGMA COMPONENT NAMING CONVENTION

```
Component/Category/Name/State/Platform

Examples:
- Button/Primary/Default/Mobile
- Button/Primary/Hover/TV
- Card/Movie/Poster/Default
- Card/Movie/Poster/Focused/TV
- Nav/Bottom/Active/Mobile
- Dialog/Update/Default/All
- Input/Search/Default/Mobile
- Toggle/Adult/On/All
```

---
