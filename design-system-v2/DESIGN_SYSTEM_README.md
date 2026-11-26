# 🎨 Flixvora v10.0 - Complete Figma Design System

> Professional UI/UX Design System for Flixvora Streaming App  
> Built following Netflix, Disney+, Prime Video 2025 design patterns  
> Android TV + Mobile + Tablet + Backend Admin  
> Version: 10.0 | Design System Version: 1.0  
> Platform: Figma | Material Design 3 | Android TV Design

---

## 📁 File Structure

```
flixvora-design-system/
├── DESIGN_SYSTEM_README.md          # This file - Overview & Documentation
├── DESIGN_TOKENS.json                # Design tokens (colors, type, spacing, shadows)
├── COMPONENTS_LIBRARY.md             # All UI components (buttons, cards, shelves, nav)
├── SCREEN_SPECS.md                   # All mobile screens specs
├── ANDROID_TV_SPECS.md               # TV-optimized screens specs
├── BACKEND_ADMIN_SPECS.md            # Admin panel & backend UI specs
└── FIGMA_IMPLEMENTATION_GUIDE.md     # How to build in Figma step-by-step
```

---

## 🎯 Design Philosophy

Flixvora follows **cinematic dark theme** optimized for:
- **Low-light viewing environments** (TV in dark room)
- **10-foot UI** for TV, **thumb-zone** for mobile
- **Content-first** - artwork drives the experience
- **Fast navigation** - minimal taps/clicks to content
- **Accessibility** - large text, high contrast, screen reader support

---

## 🎨 DESIGN TOKENS (Figma Variables)

### Color Palette

| Token | Name | Hex | Usage |
|-------|------|-----|-------|
| `color/brand/primary` | Flixvora Purple | `#6D28D9` | Primary buttons, active states, brand |
| `color/brand/secondary` | Flixvora Pink | `#EC4899` | Secondary accents, CTAs |
| `color/brand/accent` | Neon Green | `#10B981` | Success, install, play |
| `color/surface/background` | Deep Black | `#0A0A0B` | Main background |
| `color/surface/elevated` | Dark Surface | `#141417` | Cards, modals, drawers |
| `color/surface/raised` | Raised Surface | `#1C1C21` | Hover states, selected items |
| `color/text/primary` | Pure White | `#FFFFFF` | Primary text, titles |
| `color/text/secondary` | Light Gray | `#A1A1AA` | Secondary text, subtitles |
| `color/text/tertiary` | Muted Gray | `#71717A` | Captions, metadata |
| `color/text/inverse` | Black on Brand | `#000000` | Text on primary color |
| `color/border` | Subtle Border | `#27272A` | Dividers, card borders |
| `color/adult/restricted` | Red Warning | `#EF4444` | Adult content indicator |
| `color/adult/safe` | Safe Green | `#22C55E` | Kid-safe content |

### Typography (Roboto - Android System)

| Token | Size | Weight | Line Height | Letter Spacing | Usage |
|-------|------|--------|-------------|----------------|-------|
| `type/display-large` | 32sp | Bold | 40sp | 0 | Main headings, hero titles |
| `type/display-medium` | 28sp | Bold | 36sp | 0 | Section headings |
| `type/headline-large` | 24sp | Bold | 32sp | 0 | Card titles, shelf headers |
| `type/headline-medium` | 20sp | SemiBold | 28sp | 0 | Subtitles, labels |
| `type/body-large` | 16sp | Regular | 24sp | 0.25 | Body text, descriptions |
| `type/body-medium` | 14sp | Regular | 20sp | 0.25 | Metadata, small text |
| `type/label-large` | 14sp | SemiBold | 20sp | 0.1 | Button text, tags |
| `type/label-medium` | 12sp | Medium | 16sp | 0.1 | Captions, badges |
| `type/caption` | 11sp | Regular | 16sp | 0.15 | Fine print, timestamps |

### Spacing (8pt Grid)

| Token | Value | Usage |
|-------|-------|-------|
| `space/0` | 0dp | |
| `space/2` | 4dp | Tight grouping |
| `space/4` | 8dp | Component padding |
| `space/6` | 12dp | Section spacing |
| `space/8` | 16dp | Card padding, gutter |
| `space/10` | 20dp | Section margins |
| `space/12` | 24dp | Large section gaps |
| `space/16` | 32dp | Major layout breaks |
| `space/20` | 40dp | Hero section padding |

### Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `radius/sm` | 4dp | Badges, small pills |
| `radius/md` | 8dp | Buttons, input fields |
| `radius/lg` | 12dp | Cards, modals |
| `radius/xl` | 16dp | Hero cards, large containers |
| `radius/full` | 999dp | Avatars, circular buttons |

### Shadows (Elevation)

| Token | Shadow | Usage |
|-------|--------|-------|
| `elevation/0` | None | Flat surfaces |
| `elevation/1` | 0dp 2dp 4dp rgba(0,0,0,0.25) | Cards, shelves |
| `elevation/2` | 0dp 4dp 8dp rgba(0,0,0,0.30) | Modals, drawers |
| `elevation/3` | 0dp 8dp 16dp rgba(0,0,0,0.35) | Full-screen overlays |

---

## 📱 PLATFORMS

| Platform | Breakpoint | Grid | Navigation |
|----------|-----------|------|------------|
| Mobile | 360dp - 840dp | 4-column | Bottom nav |
| Tablet | 600dp - 1200dp | 8-column | Side rail + bottom |
| Android TV | 1920x1080 | 12-column TV grid | D-pad remote, focus states |
| Web/Desktop | 1200dp+ | 12-column | Left sidebar + top bar |

---
