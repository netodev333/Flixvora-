# 🛡️ Flixvora Backend Admin UI Specifications

> Admin Panel Design System  
> Content Management | User Management | Analytics | System Settings  
> Dark Theme | Desktop First | Responsive  
> Version: 10.0

---

## 🏗️ ADMIN LAYOUT STRUCTURE

### Overall Layout (Desktop 1920x1080)
```
┌─────────────────────────────────────────────────────────────────────┐
│  🛡️  FLIXVORA ADMIN              👤 Admin  ▾  🔔  🌙              │
├──────────┬──────────────────────────────────────────────────────────┤
│          │                                                          │
│  SIDEBAR │                                                          │
│  260dp   │           MAIN CONTENT AREA                              │
│          │                                                          │
│  📊      │  ┌─────────────────────────────────────────────────┐     │
│  Dashboard│  │  Page Title                                     │     │
│           │  │  Subtitle / Description                         │     │
│           │  ├─────────────────────────────────────────────────┤     │
│  🎬      │  │                                                 │     │
│  Content │  │  [ Main Content ]                               │     │
│           │  │                                                 │     │
│  👥      │  │                                                 │     │
│  Users   │  │                                                 │     │
│           │  │                                                 │     │
│  💳      │  │                                                 │     │
│  Subscrip│  │                                                 │     │
│           │  │                                                 │     │
│  📈      │  │                                                 │     │
│  Analytics│  │                                                 │     │
│           │  │                                                 │     │
│  ⚙️      │  │                                                 │     │
│  System  │  └─────────────────────────────────────────────────┘     │
│           │                                                          │
│  🚪      │                                                          │
│  Logout  │                                                          │
│          │                                                          │
└──────────┴──────────────────────────────────────────────────────────┘
```

### Layout Specs
- **Sidebar Width:** 260dp (collapsed: 80dp)
- **Top Bar Height:** 64dp
- **Content Area:** Full remaining width
- **Content Padding:** `space/24` (32dp)
- **Breakpoints:** Desktop (1200dp+), Tablet (768dp-1199dp), Mobile (<768dp)

---

## 🎨 ADMIN DESIGN TOKENS (Extended)

### Admin Colors
| Token | Hex | Usage |
|-------|-----|-------|
| `color/admin/sidebar` | `#0F0F11` | Sidebar background |
| `color/admin/sidebar-hover` | `#1C1C21` | Sidebar item hover |
| `color/admin/sidebar-active` | `#6D28D9` | Active item background |
| `color/admin/card` | `#141417` | Card background |
| `color/admin/table-header` | `#1C1C21` | Table header |
| `color/admin/table-row` | `#141417` | Table row |
| `color/admin/table-row-hover` | `#1C1C21` | Row hover |
| `color/admin/status-active` | `#22C55E` | Active status |
| `color/admin/status-inactive` | `#EF4444` | Inactive status |
| `color/admin/status-pending` | `#F59E0B` | Pending status |
| `color/admin/primary-button` | `#6D28D9` | Primary actions |
| `color/admin/danger-button` | `#EF4444` | Delete actions |

---

## 📊 ADMIN SCREEN 1: DASHBOARD

**Route:** `/admin/dashboard`

### Layout
```
┌─────────────────────────────────────────────────────────────────────┐
│  📊 Dashboard                                                       │
│  Overview of your streaming platform                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │  Total      │ │  Active     │ │  Revenue    │ │  Content    │   │
│  │  Users      │ │  Users      │ │  (Today)    │ │  Items      │   │
│  │             │ │             │ │             │ │             │   │
│  │  12,450     │ │  3,200      │ │  $4,550     │ │  1,240      │   │
│  │             │ │             │ │             │ │             │   │
│  │  ▲ 12%      │ │  ▲ 5%       │ │  ▲ 8%       │ │  ▲ 3%       │   │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘   │
│                                                                     │
│  ┌─────────────────────────┐ ┌─────────────────────────────────┐   │
│  │  User Growth Chart      │ │  Content by Genre               │   │
│  │                         │ │                                 │   │
│  │  📈 Line Chart          │ │  🍩 Donut Chart                 │   │
│  │  (Last 30 days)         │ │                                 │   │
│  └─────────────────────────┘ └─────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Recent Activity                                            │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │  • John Doe subscribed to Premium    2 hours ago            │   │
│  │  • Movie "Action Film" uploaded      5 hours ago            │   │
│  │  • Sarah Wilson cancelled subscription 1 day ago            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Components
- **Stat Cards:** 4 cards, grid layout, `color/admin/card`, `radius/lg`, padding `space/16`
- **Stat Value:** `type/display-medium` (28sp), `color/text/primary`
- **Stat Label:** `type/body-medium` (14sp), `color/text/secondary`
- **Trend Indicator:** Green ▲ or Red ▼, `type/caption`
- **Charts:** Placeholder containers, 400dp height
- **Activity List:** List items, avatar + text + timestamp

---

## 🎬 ADMIN SCREEN 2: CONTENT MANAGEMENT

**Route:** `/admin/content`

### Layout
```
┌─────────────────────────────────────────────────────────────────────┐
│  🎬 Content Management                                              │
│  Manage movies, TV shows, and channels                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [ Search Content... ]  [ Filter ▼ ]  [ + Add Content ]             │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Movie Title    │  Genre  │  Rating │  Status  │  Actions   │   │
│  ├─────────────────┼─────────┼─────────┼──────────┼───────────┤   │
│  │  [img] Action   │  Action │  ★ 8.2  │  🟢 Live │  ✏️ 🗑    │   │
│  │    Film         │         │         │          │           │   │
│  ├─────────────────┼─────────┼─────────┼──────────┼───────────┤   │
│  │  [img] Drama    │  Drama  │  ★ 7.5  │  🟢 Live │  ✏️ 🗑    │   │
│  │    Series       │         │         │          │           │   │
│  ├─────────────────┼─────────┼─────────┼──────────┼───────────┤   │
│  │  [img] Horror   │  Horror │  ★ 6.8  │  🔴 Draft│  ✏️ 🗑    │   │
│  │    Movie        │         │         │          │           │   │
│  └─────────────────┴─────────┴─────────┴──────────┴───────────┘   │
│                                                                     │
│  ◀ 1  2  3  4  5  ▶   Showing 1-25 of 124 items                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Components
- **Search Bar:** Height 44dp, full width or 300dp
- **Filter Dropdown:** Height 44dp, chevron down
- **Add Button:** Primary, icon + text, height 44dp
- **Table:** Full width, sticky header, zebra stripes optional
- **Status Badges:** Live (green), Draft (red), Pending (orange)
- **Action Buttons:** Edit (pencil icon), Delete (trash icon), icon-only
- **Pagination:** Numbered buttons, previous/next

---

## 👥 ADMIN SCREEN 3: USER MANAGEMENT

**Route:** `/admin/users`

### Layout
```
┌─────────────────────────────────────────────────────────────────────┐
│  👥 User Management                                                 │
│  Manage user accounts and subscriptions                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [ Search Users... ]  [ Filter ▼ ]  [ Role ▼ ]                      │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Avatar   │  Name        │  Email              │  Plan    │   │
│  │           │              │                     │  Status  │   │
│  ├───────────┼──────────────┼─────────────────────┼──────────┤   │
│  │  [img]    │  John Doe    │  john@example.com   │  Premium │   │
│  │           │              │                     │  🟢 Active│   │
│  ├───────────┼──────────────┼─────────────────────┼──────────┤   │
│  │  [img]    │  Sarah Wilson│  sarah@example.com  │  Free    │   │
│  │           │              │                     │  🟢 Active│   │
│  ├───────────┼──────────────┼─────────────────────┼──────────┤   │
│  │  [img]    │  Mike Ross   │  mike@example.com   │  Premium │   │
│  │           │              │                     │  🔴 Expired│   │
│  └───────────┴──────────────┴─────────────────────┴──────────┘   │
│                                                                     │
│  [ Bulk Actions ▼ ]  [ Export CSV ]                                 │
└─────────────────────────────────────────────────────────────────────┘
```

### Components
- **User Avatar:** 40dp circle, `radius/full`, placeholder initials
- **Plan Badge:** Premium (purple), Free (gray), Basic (blue)
- **Status Badge:** Active (green), Expired (red), Suspended (orange)
- **Role Filter:** Admin, Moderator, User
- **Bulk Actions:** Select all, delete selected, export

---

## 💳 ADMIN SCREEN 4: SUBSCRIPTIONS

**Route:** `/admin/subscriptions`

### Layout
```
┌─────────────────────────────────────────────────────────────────────┐
│  💳 Subscriptions                                                   │
│  Manage subscription plans and billing                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │  Total      │ │  Active     │ │  MRR        │ │  Churn      │   │
│  │  Subs       │ │  Subs       │ │  (Monthly)  │ │  Rate       │   │
│  │             │ │             │ │             │ │             │   │
│  │  5,420      │ │  3,200      │ │  $15,600    │ │  2.1%       │   │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Plan         │  Price    │  Billing  │  Users  │  Actions   │   │
│  ├─────────────┼─────────────┼───────────┼─────────┼───────────┤   │
│  │  Free         │  $0       │  N/A      │  2,220  │  ✏️ 🗑    │   │
│  │  Basic        │  $4.99    │  Monthly  │  1,500  │  ✏️ 🗑    │   │
│  │  Premium      │  $9.99    │  Monthly  │  1,200  │  ✏️ 🗑    │   │
│  │  Premium      │  $99.99   │  Yearly   │  500    │  ✏️ 🗑    │   │
│  └─────────────┴─────────────┴───────────┴─────────┴───────────┘   │
│                                                                     │
│  [ + Add Plan ]                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Components
- **Stat Cards:** Revenue-focused, green for positive metrics
- **Plan Table:** Plan name, price, billing cycle, user count, actions
- **Add Plan Button:** Primary, top right of table

---

## 📈 ADMIN SCREEN 5: ANALYTICS

**Route:** `/admin/analytics`

### Layout
```
┌─────────────────────────────────────────────────────────────────────┐
│  📈 Analytics                                                       │
│  Track platform performance and user engagement                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [ Date Range: Last 30 days ▼ ]  [ Export Report ]                  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Views Over Time                                            │   │
│  │  ┌─────────────────────────────────────────────────────┐   │   │
│  │  │                                                     │   │   │
│  │  │                    📈 Chart                         │   │   │
│  │  │                                                     │   │   │
│  │  └─────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────┐ ┌─────────────────────────────────┐   │
│  │  Top Content            │ │  User Demographics              │   │
│  │                         │ │                                 │   │
│  │  1. Movie A - 15K views │ │  👨 65%  👩 35%               │   │
│  │  2. Show B - 12K views  │ │  📱 Mobile 45%                  │   │
│  │  3. Movie C - 9K views  │ │  📺 TV 35%                      │   │
│  │                         │ │  💻 Web 20%                     │   │
│  └─────────────────────────┘ └─────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Components
- **Date Range Picker:** Dropdown, preset ranges (7d, 30d, 90d, 1y)
- **Line Chart:** Placeholder, 400dp height, grid lines
- **Top Content List:** Numbered list, title + view count
- **Demographics:** Gender split, device breakdown with percentages

---

## ⚙️ ADMIN SCREEN 6: SYSTEM SETTINGS

**Route:** `/admin/settings`

### Layout
```
┌─────────────────────────────────────────────────────────────────────┐
│  ⚙️ System Settings                                                 │
│  Configure platform settings and integrations                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  General                                                            │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Site Name         [ Flixvora         ]                     │   │
│  │  Site URL          [ https://flixvora.com ]                   │   │
│  │  Support Email     [ support@flixvora.com ]                   │   │
│  │  Maintenance Mode  [ OFF   ○● ON ]                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Content Settings                                                   │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Adult Content Default  [ OFF   ○● ON ]                     │   │
│  │  Max Download Quality   [ 1080p ▼ ]                           │   │
│  │  Auto Transcode         [ ON   ●○ OFF ]                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Update Settings                                                    │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Auto Check Updates   [ ON   ●○ OFF ]                       │   │
│  │  Update Channel       [ Stable ▼ ]                            │   │
│  │  GitHub Repo          [ netodev333/Flixvora- ]               │   │
│  │  Last Check           [ 2 hours ago ]                         │   │
│  │  [ Check Now ] [ Force Update All ]                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  [ Save Changes ]                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### Components
- **Form Fields:** Text inputs, toggles, dropdowns
- **Section Headers:** `type/headline-medium`, bottom border 1dp
- **Save Button:** Primary, full width or 200dp, bottom right
- **Update Section:** Special styling for update-related settings

---

## 🧩 ADMIN COMPONENT LIBRARY

### Data Table
```
┌─────────────────────────────────────────────────────────────┐
│  Column 1    │  Column 2    │  Column 3    │  Actions       │
├─────────────────────────────────────────────────────────────┤
│  Row Data    │  Row Data    │  Row Data    │  ✏️  🗑        │
│  (hover:     │  (hover:     │  (hover:     │                │
│   #1C1C21)   │   #1C1C21)   │   #1C1C21)   │                │
└─────────────────────────────────────────────────────────────┘
```
- **Header:** `color/admin/table-header`, text `color/text/secondary`, 14sp SemiBold
- **Row:** `color/admin/table-row`, hover `color/admin/table-row-hover`
- **Padding:** `space/12` vertical, `space/16` horizontal
- **Border:** Bottom 1dp `color/border`

### Status Badge (Admin)
```
┌─────┐
│ Live│
└─────┘
```
- **Live:** Background `#22C55E`, text white
- **Draft:** Background `#EF4444`, text white
- **Pending:** Background `#F59E0B`, text white
- **Inactive:** Background `#52525B`, text white
- **Size:** 64dp x 24dp, `radius/full`

### Admin Button Variants
| Variant | Background | Text | Usage |
|---------|-----------|------|-------|
| Primary | `#6D28D9` | White | Main actions |
| Secondary | `#1C1C21` | White | Secondary actions |
| Danger | `#EF4444` | White | Delete, remove |
| Ghost | Transparent | `#A1A1AA` | Tertiary |
| Outline | Transparent + `#27272A` border | White | Bordered actions |

---

## 📱 ADMIN RESPONSIVE BREAKPOINTS

| Breakpoint | Layout | Sidebar |
|------------|--------|---------|
| Desktop (1200dp+) | Sidebar + Main | Expanded |
| Tablet (768-1199dp) | Sidebar + Main | Collapsible |
| Mobile (<768dp) | Main only | Drawer (hamburger) |

---

## 🔐 ADMIN AUTHENTICATION UI

### Login Screen
```
┌─────────────────────────────┐
│                             │
│     🛡️  FLIXVORA ADMIN     │
│                             │
│     Sign In                 │
│                             │
│  ┌─────────────────────┐   │
│  │  Email              │   │
│  │  admin@flixvora.com │   │
│  └─────────────────────┘   │
│                             │
│  ┌─────────────────────┐   │
│  │  Password           │   │
│  │  ••••••••    👁️      │   │
│  └─────────────────────┘   │
│                             │
│  ┌─────────────────────┐   │
│  │     [ SIGN IN ]     │   │
│  └─────────────────────┘   │
│                             │
│  [ Forgot Password? ]       │
│                             │
└─────────────────────────────┘
```

### Components
- **Card:** Centered, 400dp max-width, `radius/xl`
- **Logo:** Admin logo with shield icon
- **Inputs:** Full width of card
- **Primary Button:** Full width, brand primary
- **Error Message:** Red text below inputs if invalid

---
