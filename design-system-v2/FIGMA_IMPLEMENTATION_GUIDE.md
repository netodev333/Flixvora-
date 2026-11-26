# 🎯 Figma Implementation Guide

> Step-by-step guide to build Flixvora Design System in Figma  
> For professional UI/UX designers  
> Version: 1.0

---

## 📋 PHASE 1: SETUP FIGMA FILE

### Step 1: Create New Figma File
1. Open Figma → New File
2. Name it: `Flixvora Design System v10.0`
3. Create 4 pages:
   - `0. Design System` (tokens, colors, typography)
   - `1. Components` (buttons, cards, nav, etc.)
   - `2. Mobile Screens` (all mobile screens)
   - `3. TV Screens` (Android TV layouts)
   - `4. Admin Panel` (backend UI)

---

## 🎨 PHASE 2: CREATE DESIGN TOKENS

### Step 2: Color Variables (Figma Variables)

**Create Variable Collection: "Colors"**

| Variable Name | Type | Value | Mode: Dark |
|---------------|------|-------|------------|
| `brand/primary` | Color | `#6D28D9` | |
| `brand/secondary` | Color | `#EC4899` | |
| `brand/accent` | Color | `#10B981` | |
| `surface/background` | Color | `#0A0A0B` | |
| `surface/elevated` | Color | `#141417` | |
| `surface/raised` | Color | `#1C1C21` | |
| `text/primary` | Color | `#FFFFFF` | |
| `text/secondary` | Color | `#A1A1AA` | |
| `text/tertiary` | Color | `#71717A` | |
| `border` | Color | `#27272A` | |
| `adult/restricted` | Color | `#EF4444` | |
| `adult/safe` | Color | `#22C55E` | |

**How to create:**
1. In Figma, go to **Assets** → **Local Variables** → **Create Variable**
2. Select **Color** type
3. Create collection named "Flixvora Colors"
4. Add all variables above
5. Create mode named "Dark"

### Step 3: Typography (Text Styles)

**Create Text Styles:**

| Style Name | Font | Weight | Size | Line Height | Letter Spacing |
|------------|------|--------|------|-------------|----------------|
| `Display/Large` | Roboto | Bold | 32 | 40 | 0 |
| `Display/Medium` | Roboto | Bold | 28 | 36 | 0 |
| `Headline/Large` | Roboto | Bold | 24 | 32 | 0 |
| `Headline/Medium` | Roboto | SemiBold | 20 | 28 | 0 |
| `Body/Large` | Roboto | Regular | 16 | 24 | 0.25 |
| `Body/Medium` | Roboto | Regular | 14 | 20 | 0.25 |
| `Label/Large` | Roboto | SemiBold | 14 | 20 | 0.1 |
| `Label/Medium` | Roboto | Medium | 12 | 16 | 0.1 |
| `Caption` | Roboto | Regular | 11 | 16 | 0.15 |

**How to create:**
1. Select text → Right-click → **Create Text Style**
2. Name using slash notation: `Display/Large`
3. Apply to all text elements in components

### Step 4: Spacing & Border Radius (Number Variables)

**Create Variable Collection: "Spacing"**

| Variable | Type | Value |
|----------|------|-------|
| `space/4` | Number | 8 |
| `space/8` | Number | 16 |
| `space/12` | Number | 24 |
| `space/16` | Number | 32 |
| `space/20` | Number | 40 |
| `space/24` | Number | 48 |

**Create Variable Collection: "BorderRadius"**

| Variable | Type | Value |
|----------|------|-------|
| `radius/sm` | Number | 4 |
| `radius/md` | Number | 8 |
| `radius/lg` | Number | 12 |
| `radius/xl` | Number | 16 |
| `radius/full` | Number | 999 |

---

## 🧩 PHASE 3: BUILD COMPONENTS

### Step 4: Button Component (Main Component)

**Create Frame: `Button/Primary/Default`**

```
Properties:
- Type: Component (Main)
- Size: 48dp height (hug contents horizontally)
- Padding: 16dp horizontal, 12dp vertical
- Fill: brand/primary (#6D28D9)
- Corner Radius: radius/md (8dp)
- Layout: Horizontal, center aligned
- Gap: 8dp (between icon and text)
```

**Variants to create:**
1. `Button/Primary/Default`
2. `Button/Primary/Hover` → Fill: `brand/primary-hover` (#7C3AED)
3. `Button/Primary/Pressed` → Fill: darker shade
4. `Button/Primary/Disabled` → Fill: `#52525B`, Opacity 50%
5. `Button/Secondary/Default` → Fill: transparent, Stroke: 1dp `border`
6. `Button/Ghost/Default` → Fill: transparent, Text: `text/secondary`
7. `Button/Danger/Default` → Fill: `adult/restricted` (#EF4444)

**Component Properties:**
- Boolean: `Show Icon` (default: true)
- Instance swap: `Icon` (24dp icon set)
- Text: `Label` (default: "Button")

### Step 5: Content Card Component

**Create Frame: `Card/Movie/Poster/Default`**

```
Properties:
- Size: 160dp width × 240dp height (2:3 aspect)
- Fill: surface/elevated (#141417)
- Corner Radius: radius/lg (12dp)
- Layout: Vertical, top aligned
- Children:
  1. Poster Image: 160dp × 220dp, radius/lg
  2. Badge Group (top-right): Age rating + New tag
  3. Title: Headline/Large, text/primary
  4. Metadata: Caption, text/tertiary
```

**Variants:**
- `Card/Movie/Poster/Default`
- `Card/Movie/Poster/Focused` (TV: add border 2dp brand/primary, shadow elevation/3, scale 1.05x)
- `Card/Movie/Landscape/Default` (16:9 aspect)
- `Card/Movie/Square/Default` (1:1 aspect)
- `Card/Channel/Default` (square with live dot)

### Step 6: Content Shelf Component

**Create Frame: `Shelf/Content/Default`**

```
Properties:
- Size: Full width × 320dp height
- Layout: Vertical
- Children:
  1. Header Row: Title (Headline/Medium) + See All (Button/Ghost)
  2. Scroll Area: Frame with horizontal layout, clip content
  3. Cards: Instances of Card/Movie/Poster
```

**Variants:**
- `Shelf/Content/Default` (horizontal scroll)
- `Shelf/Channel/Default` (TV channels, square cards)
- `Shelf/Season/Default` (season chips)

### Step 7: Navigation Components

**Bottom Navigation:**
```
Frame: Nav/Bottom/Default
- Size: 375dp × 64dp + safe area
- Fill: surface/elevated
- Layout: Horizontal, space-between, center vertically
- Items: 4-5 icon+label buttons
- Active: brand/primary color
- Inactive: text/tertiary color
```

**Top Navigation:**
```
Frame: Nav/Top/Default
- Size: Full width × 56dp
- Fill: surface/elevated with blur background
- Layout: Horizontal, space-between
- Left: Logo (32dp height)
- Right: Search, Profile, Notifications icons (24dp)
```

**TV Top Navigation:**
```
Frame: Nav/Top/TV
- Size: 1920dp × 80dp
- Items: 4-5 text buttons with underline focus state
- Active: brand/primary underline 6dp
```

### Step 8: Input Components

**Text Input:**
```
Frame: Input/Text/Default
- Size: Full width × 52dp height
- Fill: surface/elevated
- Stroke: 1dp border (#27272A)
- Corner Radius: radius/md (8dp)
- Padding: 16dp horizontal
- Placeholder: Caption, text/tertiary
- Leading: Icon 20dp (optional)
- Trailing: Icon 20dp (optional)
```

**Variants:**
- `Input/Text/Default`
- `Input/Text/Focus` → Stroke: 2dp `border/focus` (brand/primary)
- `Input/Text/Error` → Stroke: 2dp `adult/restricted`
- `Input/Search/Default` → With search icon and clear button

### Step 9: Toggle Switch (Adult Content)

```
Frame: Toggle/Adult/Default
- Size: 52dp × 28dp track
- Track Fill: OFF: border (#27272A), ON: brand/primary (#6D28D9)
- Track Corner: radius/full
- Thumb: 20dp circle, Fill: white
- Thumb Position: OFF: left (4dp), ON: right (28dp)
- Animation: Smart Animate, 200ms ease-out
```

---

## 📱 PHASE 4: MOBILE SCREENS

### Step 9: Create Mobile Frames

**Frame Sizes:**
- Mobile: 375dp × 812dp (iPhone 12/13/14)
- Mobile Wide: 390dp × 844dp
- Tablet: 768dp × 1024dp

**Create screens in order:**
1. `Splash`
2. `Sign In`
3. `Home`
4. `Search`
5. `Detail`
6. `TV Channels`
7. `Settings`
8. `Downloads`
9. `Profile`
10. `Update Dialog`
11. `Force Update`

**For each screen:**
1. Create frame with correct size
2. Add background: `surface/background`
3. Add components from Component Library (instances)
4. Apply text styles
5. Apply color variables
6. Set up Auto Layout for responsive behavior
7. Add constraints for different screen sizes

### Step 10: Navigation Prototyping

**Set up prototyping:**
1. Bottom nav items → Frames (Home, Search, TV, Settings)
2. Buttons → Target frames
3. Smart Animate for transitions (300ms, ease-out)
4. Overlay for dialogs (Update Dialog, Force Update)
5. Back navigation for all screens

---

## 📺 PHASE 5: TV SCREENS

### Step 11: Create TV Frames

**Frame Size:** 1920dp × 1080dp

**Create screens:**
1. `TV/Home` (BrowseFragment layout)
2. `TV/Detail`
3. `TV/Live Channels`
4. `TV/Settings`
5. `TV/Update Dialog`
6. `TV/Force Update`

**TV-specific considerations:**
- **Safe Area:** Add 60dp margin all sides (overscan)
- **Text Size:** Minimum 28sp for all text
- **Focus States:** All interactive components need focus variant
- **D-Pad Navigation:** Set up prototyping for remote navigation
- **Component Scaling:** TV components 1.5x-2x mobile sizes

**TV Focus Component Example:**
```
Frame: Card/Movie/Poster/Focused/TV
- Size: 336dp × 504dp (scaled from 320dp × 480dp)
- Fill: surface/elevated
- Stroke: 2dp brand/primary
- Corner Radius: radius/lg (12dp)
- Effects: Drop shadow, elevation/3
- Scale: 1.05x from default
```

---

## 🛡️ PHASE 6: ADMIN PANEL

### Step 12: Create Admin Frames

**Frame Size:** 1440dp × 900dp (Desktop)

**Create screens:**
1. `Admin/Login`
2. `Admin/Dashboard`
3. `Admin/Content`
4. `Admin/Users`
5. `Admin/Subscriptions`
6. `Admin/Analytics`
7. `Admin/Settings`

**Admin Layout:**
- Sidebar: 260dp width, full height
- Top Bar: 64dp height, full width minus sidebar
- Content: Remaining area, padding `space/24` (32dp)

---

## 🔗 PHASE 7: CONNECT DESIGN TO CODE

### Step 13: Export for Developers

**Figma Dev Mode settings:**
1. Enable **Dev Mode** in Figma
2. Add **CSS** code output
3. Add **Android XML** code output
4. Add **Compose** code output

**Token Mapping (Figma → Code):**

| Figma Token | Android Code | Compose Code |
|-------------|-------------|--------------|
| `color/brand/primary` | `@color/flixvora_primary` | `Color(0xFF6D28D9)` |
| `type/headline-large` | `@style/TextAppearance.HeadlineLarge` | `MaterialTheme.typography.headlineLarge` |
| `space/16` | `@dimen/space_16` | `16.dp` |
| `radius/lg` | `@dimen/radius_lg` | `12.dp` |

### Step 14: Create Component Documentation

**For each component, document:**
1. **Usage:** When to use this component
2. **Variants:** All variant combinations
3. **States:** Default, hover, pressed, disabled, focused
4. **Platform:** Mobile, TV, Web
5. **Accessibility:** Contrast ratios, touch targets, screen reader labels

---

## 📦 FIGMA FILE STRUCTURE (FINAL)

```
Flixvora Design System v10.0.fig
├── 0. Design System
│   ├── Colors (Variables)
│   ├── Typography (Text Styles)
│   ├── Spacing (Variables)
│   ├── BorderRadius (Variables)
│   └── Elevation (Effects)
├── 1. Components
│   ├── Buttons/
│   │   ├── Primary/
│   │   ├── Secondary/
│   │   ├── Ghost/
│   │   └── Danger/
│   ├── Cards/
│   │   ├── Movie Poster/
│   │   ├── Movie Landscape/
│   │   └── Channel/
│   ├── Navigation/
│   │   ├── Bottom/
│   │   ├── Top/
│   │   └── TV Top/
│   ├── Inputs/
│   │   ├── Text/
│   │   └── Search/
│   ├── Toggles/
│   │   └── Adult Content/
│   ├── Dialogs/
│   │   ├── Update/
│   │   └── Force Update/
│   └── Player/
│       └── Controls/
├── 2. Mobile Screens
│   ├── Splash
│   ├── Sign In
│   ├── Home
│   ├── Search
│   ├── Detail
│   ├── TV Channels
│   ├── Settings
│   ├── Downloads
│   ├── Profile
│   └── Modals/
├── 3. TV Screens
│   ├── Home
│   ├── Detail
│   ├── Live TV
│   ├── Settings
│   └── Modals/
└── 4. Admin Panel
    ├── Login
    ├── Dashboard
    ├── Content
    ├── Users
    ├── Subscriptions
    ├── Analytics
    └── Settings
```

---

## 🚀 QUICK START CHECKLIST

- [ ] Create Figma file with 5 pages
- [ ] Set up Color Variables collection
- [ ] Set up Typography Text Styles
- [ ] Set up Spacing & BorderRadius Variables
- [ ] Create Button component with all variants
- [ ] Create Card components (Poster, Landscape, Channel)
- [ ] Create Shelf component
- [ ] Create Navigation components (Bottom, Top, TV)
- [ ] Create Input components
- [ ] Create Toggle component (Adult Content)
- [ ] Create Dialog components (Update, Force Update)
- [ ] Design all 11 mobile screens
- [ ] Design all 6 TV screens
- [ ] Design all 7 admin screens
- [ ] Set up prototyping connections
- [ ] Add focus states for TV components
- [ ] Enable Dev Mode
- [ ] Document component usage guidelines

---

## 📞 SUPPORT

For questions about the design system:
- Check `DESIGN_SYSTEM_README.md` for overview
- Check `DESIGN_TOKENS.json` for token values
- Check `COMPONENTS_LIBRARY.md` for component specs
- Check `SCREEN_SPECS.md` for mobile screen layouts
- Check `ANDROID_TV_SPECS.md` for TV layouts
- Check `BACKEND_ADMIN_SPECS.md` for admin UI

**Design System Version:** 1.0  
**App Version:** 10.0  
**Last Updated:** Nov 25, 2026
