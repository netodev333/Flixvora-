#!/usr/bin/env python3
# MovieBox app UI clone (study). Colours and layout lifted from the official
# movieboxhd.net stylesheet: green #1dd171, bg #101114, surface #28292e.
#
# Constraints kept from earlier studies (a blank screen was reported once):
#   * all content is static HTML, JS only enhances
#   * navigation is CSS-only (hidden radios + :checked ~)
#   * no 100dvh / aspect-ratio / clip-path / position:sticky / color-mix
import json

# id, title, year, genres, rating, kind, badge, tint
T = [
 ("t1","The Beast",2026,"Action,Drama,Thriller",8.4,"Movie","","#8B5CF6"),
 ("t2","12 12 12",2026,"Action,Crime,Drama",7.9,"Movie","","#EF4444"),
 ("t3","The Rings of Power",2022,"Action,Adventure,Drama",8.1,"Series","","#F59E0B"),
 ("t4","The Gospel of Christmas",2026,"Drama,Family,Music",7.6,"Movie","","#10B981"),
 ("t5","Street Fighter",2026,"Action",7.2,"Movie","","#F97316"),
 ("t6","The Body",2026,"Drama,Horror,Mystery",7.8,"Movie","","#64748B"),
 ("t7","Other Mommy",2026,"Horror,Thriller",7.4,"Movie","","#BE123C"),
 ("t8","The Odyssey",2026,"Action,Adventure,Drama",8.6,"Movie","","#0EA5E9"),
 ("t9","The Remarried Empress",2026,"Drama,Fantasy,History",8.3,"Series","","#A855F7"),
 ("t10","Tulsa King",2022,"Action,Crime,Drama",8.8,"Series","","#78716C"),
 ("t11","Black Clover",2017,"Anime,Action,Adventure",8.5,"Series","","#22C55E"),
 ("t12","Love in Disguise",2026,"Romance,Drama",7.7,"Series","","#EC4899"),
 ("t13","100 Days of Deception",2026,"Drama,Mystery",7.5,"Series","","#6366F1"),
 ("t14","MobLand",2025,"Crime,Drama",8.2,"Series","","#B91C1C"),
 ("t15","Campus Drivers",2026,"Romance,Teen",7.3,"Series","","#F472B6"),
 ("t16","Homestead",2025,"Drama,Family",7.1,"Series","","#84CC16"),
 ("t17","Black Doves",2024,"Action,Thriller",8.0,"Series","","#334155"),
 ("t18","All's Fair",2025,"Drama,Legal",7.6,"Series","","#D97706"),
 ("t19","The Covenant",2025,"Action,War",7.9,"Series","","#78350F"),
 ("t20","The Terminal List",2022,"Action,Thriller",8.4,"Series","","#475569"),
 ("t21","Lupin",2021,"Crime,Mystery",8.3,"Series","","#1D4ED8"),
 ("t22","Spinners",2025,"Drama,Crime",7.8,"Series","","#0891B2"),
 ("t23","VisionQuest",2026,"Action,Sci-Fi",7.7,"Series","","#7C3AED"),
 ("t24","Doctor X",2026,"Drama,Medical",8.0,"Series","","#059669"),
 ("t25","Guarding Stars",2026,"Drama,Romance",7.9,"Movie","Limited Free","#F43F5E"),
 ("t26","Heart of the Beast",2026,"Action,Adventure",7.5,"Movie","","#A16207"),
 ("t27","Resident Evil",2026,"Horror,Action",7.3,"Movie","","#991B1B"),
 ("t28","Forgotten Island",2026,"Adventure,Fantasy",7.4,"Movie","","#0D9488"),
 ("t29","Hope",2026,"Drama,Sci-Fi",8.2,"Movie","","#3B82F6"),
 ("t30","The Dog Stars",2026,"Sci-Fi,Drama",8.0,"Movie","","#7E22CE"),
 ("t31","Verity",2026,"Thriller,Mystery",7.8,"Movie","","#1E293B"),
 ("t32","Heartland",2026,"Drama,Family",7.2,"Movie","","#65A30D"),
 ("t33","Warlord: Olori Ogun",2026,"Action,Drama",7.6,"Movie","","#92400E"),
 ("t34","Chasing Shadows",2026,"Crime,Thriller",7.4,"Movie","","#1F2937"),
 ("t35","Time Bound",2026,"Sci-Fi,Action",7.7,"Movie","","#4338CA"),
 ("t36","Beyond Blood",2026,"Drama,Crime",7.5,"Movie","","#7F1D1D"),
 ("t37","Avatar: Seven Havens",2026,"Animation,Fantasy",8.4,"Series","","#0284C7"),
 ("t38","Cyberpunk: Edgerunners",2022,"Animation,Sci-Fi",8.7,"Series","","#EAB308"),
 ("t39","Rick and Morty",2013,"Animation,Comedy",8.6,"Series","","#16A34A"),
 ("t40","Invincible",2021,"Animation,Action",8.5,"Series","","#DC2626"),
 ("t41","Game of Thrones",2011,"Fantasy,Drama",9.0,"Series","","#4B5563"),
 ("t42","House of the Dragon",2022,"Fantasy,Drama",8.4,"Series","","#B45309"),
 ("t43","The Witcher",2019,"Fantasy,Action",8.0,"Series","","#9A3412"),
 ("t44","Wednesday",2022,"Comedy,Horror",8.3,"Series","","#3730A3"),
 ("t45","Friends",1994,"Comedy,Romance",8.9,"Series","","#F59E0B"),
 ("t46","The Big Bang Theory",2007,"Comedy,Romance",8.6,"Series","","#0891B2"),
 ("t47","Modern Family",2009,"Comedy,Family",8.5,"Series","","#0D9488"),
 ("t48","Young Sheldon",2017,"Comedy,Family",8.2,"Series","","#CA8A04"),
 ("t49","The Office",2005,"Comedy,Mockumentary",8.9,"Series","","#475569"),
 ("t50","Percy Jackson",2023,"Fantasy,Adventure",7.8,"Series","","#0EA5E9"),
 ("t51","Stranger Things",2016,"Sci-Fi,Horror",8.7,"Series","","#B91C1C"),
 ("t52","The Umbrella Academy",2019,"Action,Fantasy",7.9,"Series","","#7C3AED"),
 ("t53","Peaky Blinders",2013,"Crime,Drama",8.8,"Series","","#292524"),
 ("t54","Snowfall",2017,"Crime,Drama",8.5,"Series","","#1E3A8A"),
 ("t55","The Sopranos",1999,"Crime,Drama",9.2,"Series","","#3F3F46"),
 ("t56","Gangs of London",2020,"Crime,Action",8.1,"Series","","#7F1D1D"),
 ("t57","Reacher",2022,"Action,Crime",8.3,"Series","","#0F766E"),
 ("t58","Lioness",2023,"Action,Drama",8.2,"Series","","#92400E"),
 ("t59","The Night Agent",2023,"Action,Thriller",7.9,"Series","","#1E40AF"),
 ("t60","Euphoria",2019,"Drama,Teen",8.1,"Series","","#9333EA"),
 ("t61","XO, Kitty",2023,"Romance,Teen",7.4,"Series","","#F472B6"),
 ("t62","Ginny & Georgia",2021,"Drama,Comedy",8.0,"Series","","#0E7490"),
 ("t63","The Summer I Turned Pretty",2022,"Romance,Teen",7.6,"Series","","#FB7185"),
 ("t64","Maxton Hall",2024,"Romance,Drama",7.9,"Series","","#BE185D"),
 ("t65","Black Clover S2",2017,"Anime,Action",8.6,"Series","","#15803D"),
 ("t66","One Piece",2023,"Anime,Adventure",8.8,"Series","","#DC2626"),
 ("t67","Demon Slayer",2019,"Anime,Action",8.7,"Series","","#065F46"),
 ("t68","Jujutsu Kaisen",2020,"Anime,Action",8.6,"Series","","#4C1D95"),
 ("t69","Attack on Titan",2013,"Anime,Action",9.1,"Series","","#44403C"),
 ("t70","Solo Leveling",2024,"Anime,Fantasy",8.4,"Series","","#1E1B4B"),
 ("t71","Sistas",2019,"Drama,Romance",7.8,"Series","","#A21CAF"),
 ("t72","The Oval",2019,"Drama,Thriller",7.5,"Series","","#7C2D12"),
 ("t73","Zatima",2022,"Drama,Romance",7.6,"Series","","#9D174D"),
 ("t74","Ruthless",2020,"Drama,Crime",7.4,"Series","","#78350F"),
 ("t75","Sunshine Through the Rain",2026,"Drama,History",7.9,"Series","","#F59E0B"),
 ("t76","When I Meet the Moon",2026,"Romance,Fantasy",8.1,"Series","","#8B5CF6"),
 ("t77","Tigers Sniff the Rose",2026,"Drama,Historical",7.7,"Series","","#B45309"),
 ("t78","The Lament of Autumn",2026,"Drama,Romance",7.8,"Series","","#D97706"),
 ("t79","Blossoms of Power",2026,"Drama,History",8.0,"Series","","#BE123C"),
 ("t80","Princess Zhaoyang",2026,"Drama,History",7.6,"Series","","#9F1239"),
]

# id, title, date, booked, badge, tint
UPCOMING = [
 ("u1","Percy Jackson and the Olympians","Nov 20","53757","","#0EA5E9"),
 ("u2","Steps","Nov 20","2483","Limited Free","#F59E0B"),
 ("u3","Yellowjackets","Nov 20","3959","","#B91C1C"),
 ("u4","Good Partner","Nov 20","3898","","#7C3AED"),
 ("u5","The Blue Road","Nov 20","4600","","#0891B2"),
 ("u6","A Talent for Murder","Nov 24","431","Limited Free","#475569"),
 ("u7","Breeder","Nov 24","595","Limited Free","#991B1B"),
 ("u8","Blade Runner 2099","Nov 25","8439","","#F97316"),
 ("u9","Nine to Six","Nov 25","4415","","#6366F1"),
 ("u10","The Hunting Wives","Nov 26","9544","","#BE185D"),
]

GENRES = ["All", "Action", "Drama", "Comedy", "Thriller", "Romance", "Horror",
          "Sci-Fi", "Fantasy", "Crime", "Anime", "Animation"]

BY = {t[0]: t for t in T}


def rows(ids):
    return [BY[i] for i in ids if i in BY]


# section name -> title ids, mirrors the official page's row order
SECTIONS = [
 ("Popular Series", ["t3", "t2", "t12", "t13", "t6", "t9", "t10", "t14", "t15", "t16", "t17", "t18"]),
 ("Popular Movie", ["t1", "t5", "t25", "t8", "t4", "t26", "t7", "t27", "t28", "t29", "t30", "t31"]),
 ("Nollywood Movie", ["t33", "t34", "t35", "t36"]),
 ("Adult Animation", ["t37", "t38", "t39", "t40"]),
 ("Epic Fantasy", ["t41", "t42", "t3", "t43", "t52", "t44"]),
 ("Comedy", ["t45", "t46", "t47", "t48", "t49"]),
 ("Teen Romance", ["t15", "t61", "t62", "t63", "t64", "t60"]),
 ("Superhero Series", ["t23", "t40", "t52", "t38"]),
 ("Bet+", ["t71", "t72", "t73", "t74"]),
 ("Action&Thriller", ["t57", "t58", "t59", "t17", "t20", "t31"]),
 ("Gangster", ["t54", "t53", "t10", "t14", "t56", "t55"]),
 ("K-Drama", ["t12", "t9", "t24", "t76"]),
 ("C-Drama", ["t75", "t76", "t77", "t78", "t79", "t80"]),
 ("Anime[English Dubbed]", ["t11", "t65", "t66", "t67", "t68", "t69", "t70"]),
 ("Action Movies", ["t1", "t5", "t8", "t19", "t35", "t26"]),
]

HEROES = ["t1", "t8", "t3", "t51"]


PATTERNS = ["arc", "rings", "orb", "bars", "diag", "dust"]


def art(t, cls="poster", idx=0):
    """Procedural two-tone poster. Pattern and second tint vary by index so the
    grid does not look like one repeated swatch."""
    pat = PATTERNS[idx % len(PATTERNS)]
    return ('<div class="%s p-%s" style="--tint:%s"><i></i><u></u><b>%s</b></div>'
            % (cls, pat, t[7], t[1][0]))


_i = [0]


def card(t, rank=None, progress=None):
    badge = ('<em class="pbadge">%s</em>' % t[6]) if t[6] else ""
    blob = " ".join([str(x) for x in (t[1], t[2], t[3], t[5])]).lower().replace('"', "")
    _i[0] += 1
    rk = ('<b class="rank">%d</b>' % rank) if rank else ""
    rate = ('<em class="rate">&#9733; %.1f</em>' % t[4])
    seasons = 1 + (sum(ord(c) for c in t[0]) % 4)
    ep = ('<em class="epn">S%d</em>' % seasons) if t[5] == "Series" else ""
    bar = ('<div class="pbar"><i style="width:%d%%"></i></div>' % progress) if progress else ""
    return (
        '<article class="card" data-open="%s" data-genre="%s" data-search="%s">'
        '<div class="card-win">%s%s'
        '<em class="hd">%s</em>%s%s%s'
        '<div class="card-play"><svg viewBox="0 0 24 24" fill="currentColor">'
        '<path d="M8 5v14l11-7z"/></svg></div>%s</div>'
        '<h4>%s</h4><span>%s &middot; %s</span></article>'
        % (t[0], t[3], blob, art(t, idx=_i[0]), rk, "4K" if t[4] >= 8.4 else "HD",
           badge, rate, ep, bar, t[1], t[2], t[3]))


def row(name, items):
    cards = "".join(card(t) for t in items)
    return ('<div class="sec"><div class="sec-head"><h3>%s</h3>'
            '<button class="more">More <svg viewBox="0 0 24 24" fill="none" '
            'stroke="currentColor" stroke-width="2.4"><path d="M9 6l6 6-6 6"/></svg>'
            '</button></div><div class="railcards">%s</div></div>' % (name, cards))


def upcoming_row():
    out = ['<div class="sec"><div class="sec-head"><h3>Coming Soon</h3>'
           '<button class="more">More <svg viewBox="0 0 24 24" fill="none" '
           'stroke="currentColor" stroke-width="2.4"><path d="M9 6l6 6-6 6"/></svg>'
           '</button></div><div class="railcards">']
    for (uid, title, date, booked, badge, tint) in UPCOMING:
        b = ('<em class="pbadge">%s</em>' % badge) if badge else ""
        out.append(
            '<article class="card up">'
            '<div class="card-win"><div class="poster" style="--tint:%s">'
            '<i></i><u></u><b>%s</b></div>%s'
            '<div class="up-date">%s</div></div>'
            '<h4>%s</h4><span class="booked">%s booked</span></article>'
            % (tint, title[0], b, date, title, booked))
    out.append('</div></div>')
    return "".join(out)


def hero_slides():
    out = []
    for i, hid in enumerate(HEROES):
        t = BY[hid]
        out.append(
            '<div class="slide%s" data-slide="%d">'
            '<div class="slide-art" style="--tint:%s"><i></i><u></u></div>'
            '<div class="slide-veil"></div>'
            '<div class="slide-body">'
            '<div class="slide-tag">%s</div>'
            '<h2>%s</h2>'
            '<div class="slide-meta"><span>%s</span><i></i><span>%s</span>'
            '<i></i><span class="star">&#9733; %.1f</span></div>'
            '<div class="slide-act">'
            '<label class="btn-play" for="r-detail">'
            '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>'
            'Play Now</label>'
            '<button class="btn-add" data-save="%s">'
            '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">'
            '<path d="M12 5v14M5 12h14"/></svg></button>'
            '</div></div></div>'
            % (" on" if i == 0 else "", i, t[7], t[5].upper(), t[1], t[2], t[3], t[4], t[0]))
    return "".join(out)


CAST_NAMES = [
    ("Aditya Kulkarni", "Detective Rao"), ("Rhea Menon", "Meera"),
    ("Sana Bhatt", "Inspector Qureshi"), ("Ira Sengupta", "Director"),
    ("Dev Joshi", "Supervisor"), ("Saira Khan", "Nurse Farida"),
]


def continue_watching():
    picks = [(BY["t8"], 62), (BY["t3"], 28), (BY["t51"], 81), (BY["t10"], 15)]
    out = ['<div class="sec"><div class="sec-head"><h3>Continue Watching</h3>'
           '<button class="more">More <svg viewBox="0 0 24 24" fill="none" '
           'stroke="currentColor" stroke-width="2.4"><path d="M9 6l6 6-6 6"/></svg>'
           '</button></div><div class="cw">']
    for i, (t, pct) in enumerate(picks):
        out.append(
            '<article class="cwcard" data-open="%s">'
            '<div class="cw-win">%s'
            '<div class="cw-play"><svg viewBox="0 0 24 24" fill="currentColor">'
            '<path d="M8 5v14l11-7z"/></svg></div>'
            '<div class="cw-bar"><i style="width:%d%%"></i></div></div>'
            '<h5>%s</h5><span>%d min left</span></article>'
            % (t[0], art(t, idx=i + 2), pct, t[1], (100 - pct) // 3))
    out.append('</div></div>')
    return "".join(out)


def top_ten():
    ids = ["t69", "t55", "t41", "t49", "t45", "t66", "t51", "t53", "t38", "t1"]
    out = ['<div class="sec"><div class="sec-head"><h3>Top 10 This Week</h3>'
           '<button class="more">More <svg viewBox="0 0 24 24" fill="none" '
           'stroke="currentColor" stroke-width="2.4"><path d="M9 6l6 6-6 6"/></svg>'
           '</button></div><div class="t10">']
    for i, tid in enumerate(ids):
        t = BY[tid]
        out.append(
            '<article class="t10card" data-open="%s">'
            '<div class="t10num">%d</div>'
            '<div class="t10col"><div class="t10post">%s</div>'
            '<h5>%s</h5></div></article>'
            % (t[0], i + 1, art(t, idx=i), t[1]))
    out.append('</div></div>')
    return "".join(out)


def cast_row():
    return ('<div class="cast">' + "".join(
        '<div class="castc"><div class="av">%s</div><b>%s</b><span>%s</span></div>'
        % (art(BY["t1"], idx=k), n, r)
        for k, (n, r) in enumerate(CAST_NAMES)) + '</div>')


def shots_row():
    ids = ["t1", "t8", "t3", "t51"]
    return ('<div class="shots">' + "".join(
        '<div class="shot">%s</div>' % art(BY[i], idx=k + 1)
        for k, i in enumerate(ids)) + '</div>')


def storage_card():
    return ('<div class="store">'
            '<div class="store-top"><b>Device Storage</b><span>48.2 GB of 64 GB</span></div>'
            '<div class="store-bar">'
            '<i style="width:38%;background:var(--g)"></i>'
            '<i style="width:22%;background:var(--blue)"></i>'
            '<i style="width:15%;background:#F59E0B"></i>'
            '<i style="width:25%;background:var(--surface-2)"></i></div>'
            '<div class="store-key">'
            '<span><em style="background:var(--g)"></em>Downloads 18.3 GB</span>'
            '<span><em style="background:var(--blue)"></em>Cache 10.6 GB</span>'
            '<span><em style="background:#F59E0B"></em>App 7.2 GB</span>'
            '</div></div>')


MOODS = [("Slow Burn", 78, "Drama", "Tense", 42), ("Adrenaline", 91, "Action", "Fast", 88),
         ("Comfort", 34, "Comedy", "Warm", 71), ("Dread", 82, "Horror", "Dark", 95)]


def watch_party():
    parties = [
        ("Friday Horror Night", "The Body", 5, "8 min in", True, ["#1DD171", "#2166E5", "#F59E0B", "#BE123C"]),
        ("Slow Burn Club", "The Remarried Empress", 3, "Starts 9:30 PM", False, ["#A855F7", "#0EA5E9", "#EC4899"]),
        ("Action Rewatch", "The Odyssey", 7, "22 min in", True, ["#EF4444", "#F97316", "#22C55E", "#6366F1"]),
    ]
    out = ['<div class="sec"><div class="sec-head"><h3>Watch Party</h3>'
           '<button class="more">More <svg viewBox="0 0 24 24" fill="none" '
           'stroke="currentColor" stroke-width="2.4"><path d="M9 6l6 6-6 6"/></svg>'
           '</button></div><div class="party">']
    for name, title, n, when, live, cols in parties:
        faces = "".join('<i style="background:%s">%s</i>' % (c, "SKV"[k % 3])
                        for k, c in enumerate(cols[:4]))
        more = '<i class="p-more">+%d</i>' % (n - 4) if n > 4 else ""
        out.append(
            '<div class="pcard%s">%s<h5>%s</h5><span>%s</span>'
            '<div class="p-faces">%s%s</div>'
            '<div class="p-time">%s</div></div>'
            % (" live" if live else "",
               '<div class="p-live">LIVE</div>' if live else
               '<div class="p-live" style="color:var(--txt-3)">SCHEDULED</div>',
               name, title, faces, more, when))
    out.append('</div></div>')
    return "".join(out)


def subtitle_preview():
    return ('<div class="subs">'
            '<div class="scene">%s</div>'
            '<div class="sub-meta">EN &middot; CC</div>'
            '<div class="sub-line"><b>&ldquo;The lamp has been burning for forty-one years.<br>'
            'Nobody ever asked who lights it.&rdquo;</b></div></div>'
            % art(BY["t1"], idx=3))


def quality_ladder():
    rows = [("4K", "Dolby Vision &middot; Atmos", 4, "7.8 GB", True),
            ("1080p", "HDR10 &middot; 5.1", 3, "2.1 GB", False),
            ("720p", "Standard &middot; Stereo", 2, "980 MB", False),
            ("480p", "Data saver", 1, "420 MB", False)]
    out = []
    for badge, desc, bars, size, top in rows:
        lb = "".join('<i class="on" style="height:%dpx"></i>' % (3 + k * 2)
                     if k < bars else '<i style="height:%dpx"></i>' % (3 + k * 2)
                     for k in range(4))
        out.append('<div class="lrow%s"><span class="lbadge">%s</span>'
                   '<span>%s</span><div class="lbars">%s</div>'
                   '<em class="lsize">%s</em></div>'
                   % (" top" if top else "", badge, desc, lb, size))
    return '<div class="ladder">%s</div>' % "".join(out)


def timeline():
    steps = [("Development", "Announced with the original slate", False),
             ("Production", "Shot across three coastal districts", False),
             ("Post", "Score recorded with a 41-piece orchestra", False),
             ("Release", "Streaming now in 4K Dolby Vision", True),
             ("Next", "Commentary track arriving in December", False)]
    return '<div class="tl">%s</div>' % "".join(
        '<div class="tlrow%s"><div class="tldot"></div><div class="tltxt">'
        '<b>%s</b><span>%s</span></div></div>' % (" now" if now else "", t, d)
        for t, d, now in steps)


def trivia():
    items = ["The lighthouse was built as a set, then donated to the village.",
             "Every radio voice in the film is the same actor, uncredited.",
             "Shot in 34 days, mostly between 4 and 6 in the morning."]
    return '<div class="trivia">%s</div>' % "".join(
        '<div class="tq"><b>&#9679;</b><p>%s</p></div>' % t for t in items)


def build():
    secs = "".join(row(n, rows(ids)) for n, ids in SECTIONS[:6])
    secs2 = "".join(row(n, rows(ids)) for n, ids in SECTIONS[6:])
    all_cards = "".join(card(t) for t in T)

    tokens = {
        "hero": hero_slides(),
        "dots": "".join('<i class="dot%s"></i>' % (" on" if i == 0 else "")
                        for i in range(len(HEROES))),
        "sections_a": secs,
        "sections_b": secs2,
        "upcoming": upcoming_row(),
        "continue": continue_watching(),
        "top10": top_ten(),
        "cast": cast_row(),
        "shots": shots_row(),
        "storage": storage_card(),
        "party": watch_party(),
        "subs": subtitle_preview(),
        "ladder": quality_ladder(),
        "timeline": timeline(),
        "trivia": trivia(),
        "allcards": all_cards,
        "chips": "".join('<button class="chip%s" data-genre-filter="%s">%s</button>'
                         % (" on" if i == 0 else "", g, g) for i, g in enumerate(GENRES)),
        "data_json": json.dumps({
            k: {"title": v[1], "year": v[2], "genres": v[3], "rating": v[4],
                "kind": v[5], "tint": v[7]}
            for k, v in BY.items()}, ensure_ascii=False),
    }

    html = TPL
    for k, v in tokens.items():
        html = html.replace("%(" + k + ")s", str(v))
    return html


TPL = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#101114">
<title>MovieBox &mdash; Watch Free Movies &amp; TV Shows</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
<style>
:root{
  --g:#1DD171;
  --g-light:#2FF58B;
  --g-dark:#07B84E;
  --bg:#101114;
  --bg-2:#0B0C0E;
  --surface:#1A1B1F;
  --surface-2:#28292E;
  --line:#26272C;
  --txt:#FFFFFF;
  --txt-2:#A9ABB3;
  --txt-3:#6E7078;
  --blue:#2166E5;
  --r:8px;
}
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent;}
html,body{margin:0;padding:0;}
body{
  background:#000;color:var(--txt);font-family:Inter,-apple-system,"Segoe UI",Roboto,sans-serif;
  min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px 8px;
}
.stage{width:100%;max-width:400px;}
.phone{
  position:relative;width:100%;height:830px;max-height:calc(100vh - 40px);min-height:560px;
  border-radius:34px;overflow:hidden;background:var(--bg);
  border:1px solid #2A2B30;box-shadow:0 40px 80px -30px #000;
}
.nr{position:absolute;left:-9999px;width:1px;height:1px;opacity:0;}

.screens{position:absolute;left:0;top:0;right:0;bottom:0;}
.screen{display:none;position:absolute;left:0;top:0;right:0;bottom:0;overflow-y:auto;
  overflow-x:hidden;padding-bottom:96px;-webkit-overflow-scrolling:touch;}
.screen::-webkit-scrollbar{width:0;}
#r-home:checked~.screens>#home,
#r-search:checked~.screens>#search,
#r-downloads:checked~.screens>#downloads,
#r-me:checked~.screens>#me,
#r-detail:checked~.screens>#detail,
#r-player:checked~.screens>#player{display:block;}

/* ══ STATUS ══ */
.sbar{position:absolute;left:0;top:0;right:0;height:34px;z-index:70;
  display:flex;align-items:center;justify-content:space-between;padding:11px 20px 0;
  font-size:10.5px;font-weight:600;color:#fff;pointer-events:none;}
.sbar .r{display:flex;gap:4px;align-items:center;}
.sbar .r i{display:block;width:2.5px;background:#fff;border-radius:1px;}

/* ══ TOP BAR ══ */
.top{display:flex;align-items:center;gap:9px;padding:40px 16px 10px;}
.logo{display:flex;align-items:center;gap:7px;flex:0 0 auto;}
.logo-mark{width:25px;height:25px;border-radius:7px;flex:0 0 25px;
  background:linear-gradient(135deg,var(--g-light),var(--g-dark));
  display:flex;align-items:center;justify-content:center;
  box-shadow:0 3px 12px -3px rgba(29,209,113,.6);}
.logo-mark svg{width:13px;height:13px;color:#06170E;}
.logo b{font-size:16px;font-weight:800;letter-spacing:-.03em;}
.logo b em{font-style:normal;color:var(--g);}
.topact{display:flex;align-items:center;gap:6px;margin-left:auto;}
.icobtn{width:32px;height:32px;border:none;background:none;color:var(--txt);
  display:flex;align-items:center;justify-content:center;cursor:pointer;padding:0;}
.icobtn svg{width:18px;height:18px;}
.prem{font-size:9.5px;font-weight:700;letter-spacing:.02em;padding:6px 10px;border-radius:100px;
  background:linear-gradient(135deg,var(--g-light),var(--g));color:#06170E;border:none;
  cursor:pointer;white-space:nowrap;box-shadow:0 3px 12px -4px rgba(29,209,113,.7);}

.searchbar{margin:2px 16px 0;display:flex;align-items:center;gap:8px;
  background:var(--surface);border-radius:100px;padding:9px 14px;}
.searchbar svg{width:15px;height:15px;color:var(--txt-3);flex:0 0 15px;}
.searchbar input{flex:1;background:none;border:none;outline:none;color:var(--txt);
  font-size:12px;font-family:inherit;}
.searchbar input::placeholder{color:var(--txt-3);}

/* ══ HERO ══ */
.hero{position:relative;margin:12px 0 0;height:330px;overflow:hidden;}
.slide{position:absolute;left:0;top:0;right:0;bottom:0;opacity:0;transition:opacity .5s;}
.slide.on{opacity:1;}
.slide-art{position:absolute;left:0;top:0;right:0;bottom:0;overflow:hidden;}
.slide-art::before{content:"";position:absolute;left:-10%;top:-10%;right:-10%;bottom:-10%;
  background:
    radial-gradient(60% 52% at 22% 18%,var(--tint) 0%,transparent 62%),
    radial-gradient(72% 66% at 84% 82%,rgba(29,209,113,.16) 0%,transparent 66%),
    linear-gradient(158deg,#1D1F26 6%,#0B0C0E 92%);}
.slide-art::after{content:"";position:absolute;left:0;top:0;right:0;bottom:0;
  background-image:radial-gradient(circle,rgba(255,255,255,.12) .8px,transparent .8px);
  background-size:5px 5px;opacity:.4;}
.slide-art u{position:absolute;left:0;top:0;right:0;bottom:0;display:block;
  box-shadow:inset 0 0 60px -10px rgba(0,0,0,.8);}
.slide-veil{position:absolute;left:0;top:0;right:0;bottom:0;
  background:linear-gradient(to top,var(--bg) 2%,rgba(16,17,20,.94) 24%,rgba(16,17,20,.42) 58%,rgba(16,17,20,.1) 100%);}
.slide-body{position:absolute;left:0;right:0;bottom:0;padding:0 16px 16px;}
.slide-tag{display:inline-block;font-size:8.5px;font-weight:800;letter-spacing:.1em;
  padding:3px 7px;border-radius:3px;background:var(--g);color:#06170E;margin-bottom:7px;}
.slide-body h2{margin:0 0 7px;font-size:27px;line-height:1.04;font-weight:800;
  letter-spacing:-.035em;text-shadow:0 2px 16px rgba(0,0,0,.7);}
.slide-meta{display:flex;align-items:center;gap:6px;font-size:10px;color:var(--txt-2);
  font-weight:500;margin-bottom:12px;flex-wrap:wrap;}
.slide-meta i{width:2.5px;height:2.5px;border-radius:50%;background:var(--txt-3);}
.slide-meta .star{color:var(--g);font-weight:700;}
.slide-act{display:flex;gap:7px;}
.btn-play{display:inline-flex;align-items:center;gap:6px;cursor:pointer;
  background:var(--g);color:#06170E;font-size:11.5px;font-weight:700;
  padding:9px 18px;border-radius:100px;border:none;
  box-shadow:0 4px 16px -4px rgba(29,209,113,.75);}
.btn-play svg{width:13px;height:13px;}
.btn-add{width:34px;height:34px;border-radius:50%;border:none;cursor:pointer;
  background:rgba(255,255,255,.14);color:#fff;display:flex;align-items:center;
  justify-content:center;backdrop-filter:blur(6px);}
.btn-add svg{width:15px;height:15px;}
.btn-add.on{background:var(--g);color:#06170E;}
.dots{position:absolute;left:0;right:0;bottom:7px;display:flex;justify-content:center;
  gap:4px;z-index:5;}
.dot{display:block;width:4.5px;height:4.5px;border-radius:50%;background:rgba(255,255,255,.32);}
.dot.on{width:14px;border-radius:100px;background:var(--g);}

/* ══ SECTION ══ */
.sec{margin-top:20px;}
.sec-head{display:flex;align-items:center;justify-content:space-between;
  padding:0 16px 9px;}
.sec-head h3{margin:0;font-size:14px;font-weight:800;letter-spacing:-.02em;}
.more{display:flex;align-items:center;gap:1px;background:none;border:none;
  color:var(--txt-3);font-size:10.5px;font-weight:600;cursor:pointer;
  font-family:inherit;padding:0;}
.more svg{width:11px;height:11px;}
.railcards{display:flex;gap:9px;overflow-x:auto;padding:0 16px 2px;}
.railcards::-webkit-scrollbar{height:0;}

/* ══ CARD ══ */
.card{flex:0 0 104px;cursor:pointer;}
.card-win{position:relative;height:152px;border-radius:var(--r);overflow:hidden;
  background:var(--surface);}
.card-win::after{content:"";position:absolute;left:0;top:0;right:0;bottom:0;
  border-radius:var(--r);box-shadow:inset 0 0 0 1px rgba(255,255,255,.055);}
.poster{position:absolute;left:0;top:0;right:0;bottom:0;overflow:hidden;}
.poster::before{content:"";position:absolute;left:-10%;top:-10%;right:-10%;bottom:-10%;
  background:
    radial-gradient(58% 50% at 26% 16%,var(--tint) 0%,transparent 62%),
    radial-gradient(74% 68% at 82% 86%,rgba(29,209,113,.14) 0%,transparent 66%),
    linear-gradient(160deg,#1E2027 6%,#0B0C0E 94%);}
.poster::after{content:"";position:absolute;left:0;top:0;right:0;bottom:0;
  background-image:radial-gradient(circle,rgba(255,255,255,.1) .7px,transparent .7px);
  background-size:4.5px 4.5px;opacity:.4;}
.poster i{position:absolute;display:block;width:58%;height:0;padding-bottom:58%;
  border-radius:50%;right:-14%;top:10%;border:1.3px solid rgba(255,255,255,.24);}
.poster u{position:absolute;left:0;top:0;right:0;bottom:0;display:block;
  box-shadow:inset 0 -40px 50px -30px rgba(0,0,0,.9);}
.poster b{position:absolute;left:8px;bottom:7px;font-size:29px;font-weight:800;
  line-height:1;color:rgba(255,255,255,.9);letter-spacing:-.05em;
  text-shadow:0 2px 14px rgba(0,0,0,.85);}
.hd{position:absolute;top:5px;left:5px;font-size:6.5px;font-weight:800;
  letter-spacing:.06em;padding:2px 4px;border-radius:2.5px;
  background:rgba(0,0,0,.62);color:var(--g);font-style:normal;
  box-shadow:inset 0 0 0 1px rgba(29,209,113,.45);}
.pbadge{position:absolute;top:5px;right:5px;font-size:6.5px;font-weight:700;
  padding:2px 4.5px;border-radius:2.5px;background:var(--blue);color:#fff;
  font-style:normal;letter-spacing:.01em;}
.card-play{position:absolute;left:50%;top:50%;width:30px;height:30px;margin:-15px 0 0 -15px;
  border-radius:50%;background:rgba(0,0,0,.5);display:flex;align-items:center;
  justify-content:center;opacity:0;transition:opacity .2s;backdrop-filter:blur(3px);}
.card:hover .card-play{opacity:1;}
.card-play svg{width:11px;height:11px;color:#fff;}
.rank{position:absolute;left:5px;bottom:5px;font-size:26px;font-weight:800;line-height:1;
  color:#fff;letter-spacing:-.05em;text-shadow:0 2px 10px rgba(0,0,0,.9),0 0 18px rgba(0,0,0,.7);z-index:3;}
.rate{position:absolute;right:5px;bottom:5px;font-size:7.5px;font-weight:800;font-style:normal;
  padding:2px 4.5px;border-radius:3px;background:rgba(0,0,0,.7);color:var(--g);z-index:3;
  letter-spacing:.01em;}
.epn{position:absolute;right:5px;top:19px;font-size:6.5px;font-weight:800;font-style:normal;
  padding:2px 4px;border-radius:2.5px;background:rgba(0,0,0,.7);color:#fff;
  letter-spacing:.04em;z-index:3;}
.pbar{position:absolute;left:0;right:0;bottom:0;height:3px;background:rgba(255,255,255,.22);z-index:4;}
.pbar i{display:block;height:100%;background:var(--g);}
/* poster pattern variants */
.p-arc i{width:70%;height:0;padding-bottom:70%;border-radius:50%;left:-14%;top:6%;
  border:1.2px solid rgba(255,255,255,.3);
  box-shadow:0 0 0 11px rgba(255,255,255,.055),0 0 0 26px rgba(255,255,255,.028);}
.p-rings i{width:56%;height:0;padding-bottom:56%;border-radius:50%;right:-10%;top:14%;
  border:1.2px solid rgba(255,255,255,.32);
  box-shadow:0 0 0 9px rgba(255,255,255,.06),0 0 0 22px rgba(255,255,255,.03);}
.p-orb i{width:62%;height:0;padding-bottom:62%;border-radius:50%;right:-12%;bottom:14%;
  background:radial-gradient(circle at 34% 30%,rgba(255,255,255,.42),rgba(255,255,255,.04) 62%,transparent 72%);
  border:none;box-shadow:none;}
.p-bars i{left:0;top:0;right:0;bottom:0;width:auto;height:auto;padding:0;border:none;
  border-radius:0;box-shadow:none;
  background:repeating-linear-gradient(102deg,rgba(255,255,255,.15) 0 1.3px,transparent 1.3px 13px);}
.p-diag i{width:190%;height:34%;left:-45%;top:30%;border:none;border-radius:0;padding:0;
  box-shadow:none;transform:rotate(-24deg);
  background:linear-gradient(90deg,transparent,rgba(255,255,255,.2),transparent);}
.p-dust i{left:0;top:0;right:0;bottom:0;width:auto;height:auto;padding:0;border:none;
  border-radius:0;box-shadow:none;opacity:.55;
  background-image:radial-gradient(circle,rgba(255,255,255,.55) .8px,transparent .8px);
  background-size:12px 15px;}
/* continue watching */
.cw{display:flex;gap:9px;overflow-x:auto;padding:0 16px 2px;}
.cw::-webkit-scrollbar{height:0;}
.cwcard{flex:0 0 148px;cursor:pointer;}
.cw-win{position:relative;height:84px;border-radius:var(--r);overflow:hidden;}
.cw-win::after{content:"";position:absolute;left:0;top:0;right:0;bottom:0;border-radius:var(--r);
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.06);}
.cw-play{position:absolute;left:50%;top:50%;width:28px;height:28px;margin:-14px 0 0 -14px;
  border-radius:50%;background:rgba(0,0,0,.5);display:flex;align-items:center;
  justify-content:center;backdrop-filter:blur(3px);z-index:3;}
.cw-play svg{width:10px;height:10px;color:#fff;}
.cw-bar{position:absolute;left:0;right:0;bottom:0;height:3px;background:rgba(255,255,255,.25);z-index:3;}
.cw-bar i{display:block;height:100%;background:var(--g);}
.cwcard h5{margin:5px 0 1px;font-size:10px;font-weight:600;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.cwcard span{font-size:8.5px;color:var(--txt-3);}
/* top10 strip */
.t10{display:flex;gap:4px;overflow-x:auto;padding:0 16px 2px;}
.t10::-webkit-scrollbar{height:0;}
.t10card{flex:0 0 118px;display:flex;align-items:flex-end;gap:0;cursor:pointer;}
.t10num{font-size:46px;font-weight:800;line-height:.8;color:transparent;
  -webkit-text-stroke:1.6px var(--surface-2);letter-spacing:-.06em;margin-right:-9px;
  position:relative;z-index:2;}
.t10col{width:74px;flex:0 0 74px;}
.t10post{width:74px;height:106px;border-radius:6px;overflow:hidden;position:relative;
  box-shadow:0 4px 14px -6px #000;}
.t10col h5{margin:5px 0 0;font-size:8.5px;font-weight:600;line-height:1.25;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
/* cast */
.cast{display:flex;gap:12px;overflow-x:auto;padding:4px 16px 0;}
.cast::-webkit-scrollbar{height:0;}
.castc{flex:0 0 52px;text-align:center;}
.castc .av{width:52px;height:52px;border-radius:50%;overflow:hidden;position:relative;
  margin-bottom:5px;box-shadow:inset 0 0 0 1px rgba(255,255,255,.09);}
.castc b{display:block;font-size:8.5px;font-weight:600;line-height:1.25;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.castc span{display:block;font-size:7.5px;color:var(--txt-3);
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
/* screenshots */
.shots{display:flex;gap:8px;overflow-x:auto;padding:4px 16px 0;}
.shots::-webkit-scrollbar{height:0;}
.shot{flex:0 0 148px;height:84px;border-radius:6px;overflow:hidden;position:relative;
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.07);}
/* storage */
.store{margin:14px 16px 0;background:var(--surface);border-radius:var(--r);padding:13px 14px;}
.store-top{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:9px;}
.store-top b{font-size:11px;font-weight:700;}
.store-top span{font-size:9.5px;color:var(--txt-3);font-weight:600;}
.store-bar{height:6px;border-radius:3px;background:var(--surface-2);display:flex;
  overflow:hidden;gap:1.5px;}
.store-bar i{display:block;height:100%;}
.store-key{display:flex;gap:14px;margin-top:9px;flex-wrap:wrap;}
.store-key span{display:flex;align-items:center;gap:5px;font-size:8.5px;color:var(--txt-3);
  font-weight:600;}
.store-key em{width:6px;height:6px;border-radius:2px;display:block;font-style:normal;}
/* badge pill on detail */
.dbadges{display:flex;gap:6px;align-items:center;margin-bottom:9px;flex-wrap:wrap;}
.db{font-size:8px;font-weight:800;letter-spacing:.04em;padding:3px 6px;border-radius:3px;}
.db.k{background:rgba(29,209,113,.16);color:var(--g);}
.db.q{background:rgba(255,255,255,.1);color:#fff;}
.db.f{background:var(--blue);color:#fff;}
.card h4{margin:6px 0 1px;font-size:10.5px;font-weight:600;line-height:1.28;
  overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;}
.card > span{display:block;font-size:8.5px;color:var(--txt-3);font-weight:500;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.up-date{position:absolute;left:0;right:0;bottom:0;padding:13px 5px 4px;
  font-size:8px;font-weight:800;letter-spacing:.04em;text-align:center;color:#fff;
  background:linear-gradient(to top,rgba(0,0,0,.92),transparent);}
.booked{color:var(--g)!important;font-weight:600!important;}

/* ══ GRID (search / downloads) ══ */
.chips{display:flex;gap:6px;overflow-x:auto;padding:12px 16px 2px;}
.chips::-webkit-scrollbar{height:0;}
.chip{flex:0 0 auto;cursor:pointer;font-family:inherit;font-size:10.5px;font-weight:600;
  padding:6px 12px;border-radius:100px;background:var(--surface);color:var(--txt-2);
  border:1px solid transparent;}
.chip.on{background:var(--g);color:#06170E;font-weight:700;}
.grid{display:flex;flex-wrap:wrap;gap:12px 9px;padding:14px 16px 0;}
.grid .card{flex:0 0 calc(33.333% - 6px);}
.grid .card-win{height:auto;padding-bottom:150%;}
.cnt{padding:12px 16px 0;font-size:10.5px;color:var(--txt-3);font-weight:600;}

/* ══ DOWNLOADS ══ */
.dl-row{display:flex;align-items:center;gap:11px;padding:11px 16px;
  border-bottom:1px solid var(--line);}
.dl-thumb{width:52px;height:74px;flex:0 0 52px;border-radius:6px;overflow:hidden;
  position:relative;}
.dl-txt{flex:1;min-width:0;}
.dl-txt h5{margin:0 0 3px;font-size:12px;font-weight:600;}
.dl-txt span{display:block;font-size:9.5px;color:var(--txt-3);margin-bottom:5px;}
.dl-bar{height:3px;border-radius:2px;background:var(--surface-2);overflow:hidden;}
.dl-bar i{display:block;height:100%;background:var(--g);border-radius:2px;}
.dl-pct{font-size:9.5px;font-weight:700;color:var(--g);flex:0 0 auto;}
.empty{padding:60px 30px;text-align:center;}
.empty svg{width:44px;height:44px;color:var(--surface-2);margin-bottom:14px;}
.empty h4{margin:0 0 6px;font-size:14px;font-weight:700;}
.empty p{margin:0;font-size:11.5px;color:var(--txt-3);line-height:1.6;}

/* ══ ME ══ */
.me-head{padding:46px 16px 0;display:flex;align-items:center;gap:12px;}
.avatar{width:56px;height:56px;border-radius:50%;flex:0 0 56px;
  background:linear-gradient(135deg,var(--g),#0E7A45);display:flex;align-items:center;
  justify-content:center;font-size:21px;font-weight:800;color:#06170E;}
.me-head h2{margin:0 0 3px;font-size:17px;font-weight:800;letter-spacing:-.02em;}
.me-head span{font-size:10.5px;color:var(--txt-3);}
.stats{display:flex;margin:18px 16px 0;background:var(--surface);border-radius:var(--r);
  padding:13px 0;}
.stat{flex:1;text-align:center;border-right:1px solid var(--line);}
.stat:last-child{border-right:none;}
.stat b{display:block;font-size:15px;font-weight:800;margin-bottom:2px;}
.stat span{font-size:8.5px;color:var(--txt-3);font-weight:600;}
.menu{margin-top:16px;}
.mrow{display:flex;align-items:center;gap:12px;padding:13px 16px;
  border-bottom:1px solid var(--line);cursor:pointer;}
.mrow svg{width:17px;height:17px;color:var(--txt-2);flex:0 0 17px;}
.mrow span{flex:1;font-size:12.5px;font-weight:500;}
.mrow em{font-size:10.5px;color:var(--txt-3);font-style:normal;}
.chev{width:13px!important;height:13px!important;color:var(--txt-3)!important;}

/* ══ DETAIL ══ */
.dhero{position:relative;height:400px;overflow:hidden;}
.dhero-veil{position:absolute;left:0;top:0;right:0;bottom:0;
  background:linear-gradient(to top,var(--bg) 3%,rgba(16,17,20,.95) 30%,rgba(16,17,20,.35) 66%,rgba(16,17,20,.55));}
.dhero-body{position:absolute;left:0;right:0;bottom:0;padding:0 16px 16px;}
.dhero-body h1{margin:0 0 8px;font-size:28px;line-height:1.05;font-weight:800;
  letter-spacing:-.035em;text-shadow:0 2px 16px rgba(0,0,0,.6);}
.dmeta{display:flex;align-items:center;gap:6px;font-size:10.5px;color:var(--txt-2);
  margin-bottom:13px;flex-wrap:wrap;font-weight:500;}
.dmeta i{width:2.5px;height:2.5px;border-radius:50%;background:var(--txt-3);}
.dmeta .star{color:var(--g);font-weight:700;}
.dact{display:flex;gap:8px;}
.btn-play.wide{flex:1;justify-content:center;padding:12px 0;font-size:12.5px;}
.btn-ghost{display:inline-flex;align-items:center;justify-content:center;gap:6px;
  padding:12px 15px;border-radius:100px;border:none;cursor:pointer;
  background:var(--surface-2);color:#fff;font-size:11.5px;font-weight:600;
  font-family:inherit;}
.btn-ghost svg{width:14px;height:14px;}
.btn-ghost.on{background:var(--g);color:#06170E;}
.dsec{padding:18px 16px 0;}
.dsec h3{margin:0 0 8px;font-size:12.5px;font-weight:800;}
.dsec p{margin:0;font-size:11.5px;line-height:1.72;color:var(--txt-2);}
.tags{display:flex;gap:6px;flex-wrap:wrap;padding:12px 16px 0;}
.tag{font-size:9.5px;font-weight:600;padding:4.5px 9px;border-radius:100px;
  background:var(--surface);color:var(--txt-2);}
.eps{padding:8px 16px 0;}
.ep{display:flex;align-items:center;gap:11px;padding:10px 0;border-bottom:1px solid var(--line);}
.ep-thumb{width:76px;height:44px;flex:0 0 76px;border-radius:5px;overflow:hidden;position:relative;}
.ep-txt{flex:1;min-width:0;}
.ep-txt h5{margin:0 0 2px;font-size:11.5px;font-weight:600;}
.ep-txt span{font-size:9px;color:var(--txt-3);}
.ep > svg{width:15px;height:15px;color:var(--g);flex:0 0 15px;}

/* ══ PLAYER ══ */
#player{background:#000;padding-bottom:0;}
.pwrap{position:relative;height:100%;display:flex;flex-direction:column;
  align-items:center;justify-content:center;}
.pwin{position:relative;width:100%;height:224px;overflow:hidden;background:#000;}
.pui{position:absolute;left:0;top:0;right:0;bottom:0;z-index:6;display:flex;
  flex-direction:column;justify-content:space-between;padding:40px 14px 16px;
  background:linear-gradient(to bottom,rgba(0,0,0,.7),transparent 26%,transparent 54%,rgba(0,0,0,.9));}
.ptop{display:flex;align-items:flex-start;justify-content:space-between;gap:10px;}
.ptop h4{margin:0 0 2px;font-size:12.5px;font-weight:700;}
.ptop span{font-size:9px;color:var(--txt-3);}
.pbot{display:flex;flex-direction:column;gap:8px;}
.scrub{height:3px;border-radius:2px;background:rgba(255,255,255,.24);position:relative;}
.scrub i{position:absolute;left:0;top:0;bottom:0;width:32%;border-radius:2px;background:var(--g);}
.scrub i::after{content:"";position:absolute;right:-4px;top:50%;margin-top:-4px;
  width:8px;height:8px;border-radius:50%;background:var(--g-light);}
.ptime{display:flex;justify-content:space-between;font-size:9px;color:var(--txt-2);}
.pctl{display:flex;align-items:center;justify-content:center;gap:26px;padding-top:2px;}
.pc{background:none;border:none;color:#fff;cursor:pointer;padding:0;
  display:flex;align-items:center;justify-content:center;}
.pc svg{width:16px;height:16px;}
.pc.big{width:46px;height:46px;border-radius:50%;background:var(--g);color:#06170E;}
.pc.big svg{width:18px;height:18px;}
.pbig{position:absolute;left:0;top:0;right:0;bottom:0;z-index:5;display:flex;
  align-items:center;justify-content:center;background:none;border:none;cursor:pointer;}
.pbig i{width:52px;height:52px;border-radius:50%;display:flex;align-items:center;
  justify-content:center;background:rgba(29,209,113,.2);
  box-shadow:inset 0 0 0 1.5px rgba(29,209,113,.6);backdrop-filter:blur(4px);}
.pbig svg{width:19px;height:19px;color:var(--g-light);}

/* ══ MOOD MIXER (original) ══════════════════════════════════ */
  position:relative;overflow:hidden;}
  border-radius:50%;background:radial-gradient(circle,rgba(29,209,113,.16),transparent 70%);}
  text-transform:uppercase;letter-spacing:.04em;}
  background:linear-gradient(90deg,var(--g-dark),var(--g-light));}
  border-radius:50%;background:#fff;box-shadow:0 2px 8px rgba(0,0,0,.6);}
  font-style:normal;flex:0 0 26px;}
  border-top:1px solid var(--line);position:relative;z-index:2;}
  font-family:inherit;font-size:9.5px;font-weight:800;padding:7px 12px;cursor:pointer;
  box-shadow:0 3px 12px -4px rgba(29,209,113,.7);}

/* ══ WATCH PARTY (original) ═════════════════════════════════ */
.party{display:flex;gap:9px;overflow-x:auto;padding:0 16px 2px;}
.party::-webkit-scrollbar{height:0;}
.pcard{flex:0 0 176px;background:var(--surface);border-radius:10px;padding:11px;
  position:relative;overflow:hidden;border:1px solid rgba(29,209,113,.16);}
.pcard.live{border-color:rgba(29,209,113,.5);}
.p-live{display:inline-flex;align-items:center;gap:4px;font-size:7px;font-weight:800;
  letter-spacing:.1em;color:#FF4D3D;margin-bottom:7px;}
.p-live::before{content:"";width:4.5px;height:4.5px;border-radius:50%;background:#FF4D3D;
  animation:blink 1.5s ease-in-out infinite;}
.pcard h5{margin:0 0 2px;font-size:11px;font-weight:700;line-height:1.3;}
.pcard > span{font-size:8.5px;color:var(--txt-3);}
.p-faces{display:flex;margin-top:9px;}
.p-faces i{width:21px;height:21px;border-radius:50%;border:1.5px solid var(--surface);
  margin-right:-6px;display:flex;align-items:center;justify-content:center;
  font-size:7.5px;font-weight:800;color:#06170E;font-style:normal;position:relative;}
.p-more{background:var(--surface-2)!important;color:var(--txt-2)!important;}
.p-time{margin-top:8px;font-size:8px;color:var(--txt-3);font-weight:600;}

/* ══ SUBTITLE PREVIEW (original) ════════════════════════════ */
.subs{margin:12px 16px 0;border-radius:10px;overflow:hidden;position:relative;
  height:88px;background:#000;display:flex;align-items:center;justify-content:center;}
.subs .scene{position:absolute;left:0;top:0;right:0;bottom:0;opacity:.5;}
.sub-line{position:relative;z-index:2;text-align:center;padding:0 18px;}
.sub-line b{display:inline-block;background:rgba(0,0,0,.72);padding:4px 9px;border-radius:4px;
  font-size:11px;font-weight:600;line-height:1.45;}
.sub-meta{position:absolute;top:6px;right:8px;z-index:3;font-size:6.5px;font-weight:800;
  letter-spacing:.1em;color:var(--g);background:rgba(0,0,0,.6);padding:2px 5px;border-radius:3px;}

/* ══ QUALITY LADDER (original) ══════════════════════════════ */
.ladder{margin:12px 16px 0;}
.lrow{display:flex;align-items:center;gap:9px;padding:8px 0;
  border-bottom:1px solid var(--line);}
.lrow:last-child{border-bottom:none;}
.lbadge{font-size:7.5px;font-weight:800;padding:3px 6px;border-radius:3px;
  background:var(--surface-2);color:var(--txt-2);flex:0 0 auto;letter-spacing:.03em;}
.lrow.top .lbadge{background:var(--g);color:#06170E;}
.lrow > span{flex:1;font-size:9.5px;color:var(--txt-3);}
.lbars{display:flex;gap:2px;align-items:flex-end;flex:0 0 auto;}
.lbars i{width:3px;background:var(--surface-2);border-radius:1px;display:block;}
.lrow.top .lbars i.on{background:var(--g);}
.lbars i.on{background:var(--txt-3);}
.lsize{font-size:8px;color:var(--txt-3);font-weight:600;flex:0 0 auto;width:40px;
  text-align:right;}

/* ══ TIMELINE (original) ════════════════════════════════════ */
.tl{padding:6px 16px 0;position:relative;}
.tl::before{content:"";position:absolute;left:21px;top:10px;bottom:12px;width:1px;
  background:linear-gradient(to bottom,var(--g),var(--line));}
.tlrow{display:flex;gap:11px;padding:7px 0;position:relative;}
.tldot{width:11px;height:11px;border-radius:50%;flex:0 0 11px;margin-top:3px;
  background:var(--surface-2);border:1.5px solid var(--line);position:relative;z-index:2;}
.tlrow.now .tldot{background:var(--g);border-color:var(--g);
  box-shadow:0 0 0 3px rgba(29,209,113,.18);}
.tltxt{flex:1;min-width:0;}
.tltxt b{display:block;font-size:10px;font-weight:700;margin-bottom:1px;}
.tltxt span{font-size:8.5px;color:var(--txt-3);line-height:1.4;}

/* ══ TRIVIA (original) ══════════════════════════════════════ */
.trivia{margin:12px 16px 0;display:flex;flex-direction:column;gap:7px;}
.tq{display:flex;gap:9px;background:var(--surface);border-radius:8px;padding:10px 11px;
  align-items:flex-start;}
.tq b{font-size:13px;color:var(--g);line-height:1;flex:0 0 auto;font-weight:800;}
.tq p{margin:0;font-size:10px;line-height:1.6;color:var(--txt-2);}

/* ══ BOTTOM NAV ══ */
.bnav{position:absolute;left:0;right:0;bottom:0;z-index:65;display:flex;
  background:rgba(16,17,20,.97);border-top:1px solid var(--line);
  padding:8px 0 12px;backdrop-filter:blur(14px);}
.nb{flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;
  background:none;border:none;cursor:pointer;color:var(--txt-3);padding:0;font-family:inherit;}
.nb svg{width:19px;height:19px;}
.nb span{font-size:8.5px;font-weight:600;letter-spacing:.01em;}
#r-home:checked~.bnav .nb[for="r-home"],
#r-search:checked~.bnav .nb[for="r-search"],
#r-downloads:checked~.bnav .nb[for="r-downloads"],
#r-me:checked~.bnav .nb[for="r-me"]{color:var(--g);}
#r-detail:checked~.bnav,#r-player:checked~.bnav{display:none;}
.back{position:absolute;top:44px;left:14px;z-index:80;width:32px;height:32px;
  display:none;align-items:center;justify-content:center;cursor:pointer;border-radius:50%;
  background:rgba(0,0,0,.55);color:#fff;backdrop-filter:blur(8px);}
.back svg{width:15px;height:15px;}
#r-detail:checked~.back,#r-player:checked~.back{display:flex;}

@media (max-width:520px){
  body{padding:0;display:block;background:var(--bg);}
  .stage{max-width:none;}
  .phone{height:100vh;min-height:100vh;max-height:none;border-radius:0;border:none;box-shadow:none;}
}
</style>
</head>
<body>
<div class="stage">
<div class="phone">

  <input class="nr" type="radio" name="nav" id="r-home" checked>
  <input class="nr" type="radio" name="nav" id="r-search">
  <input class="nr" type="radio" name="nav" id="r-downloads">
  <input class="nr" type="radio" name="nav" id="r-me">
  <input class="nr" type="radio" name="nav" id="r-detail">
  <input class="nr" type="radio" name="nav" id="r-player">

  <div class="sbar">
    <span>9:41</span>
    <div class="r"><i style="height:4px"></i><i style="height:6px"></i>
      <i style="height:8px"></i><i style="height:10px"></i></div>
  </div>

  <div class="screens">

    <!-- HOME -->
    <section class="screen" id="home">
      <div class="top">
        <div class="logo">
          <div class="logo-mark"><svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z"/></svg></div>
          <b>Movie<em>Box</em></b>
        </div>
        <div class="topact">
          <button class="icobtn" aria-label="Downloads">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
              <path d="M12 4v10"/><path d="M7.5 10.5L12 15l4.5-4.5"/>
              <path d="M4.5 19.5h15"/></svg>
          </button>
          <button class="icobtn" aria-label="Profile">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
              <circle cx="12" cy="8" r="3.6"/><path d="M4.5 20c0-3.8 3.4-5.8 7.5-5.8s7.5 2 7.5 5.8"/></svg>
          </button>
        </div>
      </div>
      <label class="searchbar" for="r-search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="7"/><path d="m20 20-3.6-3.6"/></svg>
        <input placeholder="Search movies/ TV Shows" readonly>
      </label>

      <div class="hero">%(hero)s<div class="dots">%(dots)s</div></div>

      %(continue)s
      %(sections_a)s
      %(top10)s
      %(party)s
      %(upcoming)s
      %(sections_b)s

      <div style="height:20px"></div>
    </section>

    <!-- SEARCH -->
    <section class="screen" id="search">
      <div class="top">
        <div class="logo"><b style="font-size:15px">Search</b></div>
      </div>
      <label class="searchbar" style="margin-top:0">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="7"/><path d="m20 20-3.6-3.6"/></svg>
        <input id="q" placeholder="Search movies/ TV Shows" autocomplete="off">
      </label>
      <div class="chips">%(chips)s</div>
      <div class="cnt" id="cnt">80 results</div>
      <div class="grid" id="allgrid">%(allcards)s</div>
      <div style="height:20px"></div>
    </section>

    <!-- DOWNLOADS -->
    <section class="screen" id="downloads">
      <div class="top"><div class="logo"><b style="font-size:15px">Downloads</b></div></div>
      <div class="cnt" style="padding-top:4px">2 downloading &middot; 1 complete</div>
      %(storage)s
      <div class="dl-row">
        <div class="dl-thumb"><div class="poster" style="--tint:#8B5CF6"><i></i><u></u><b>T</b></div></div>
        <div class="dl-txt"><h5>The Beast</h5><span>2026 &middot; 1080p &middot; 2.1 GB</span>
          <div class="dl-bar"><i style="width:68%"></i></div></div>
        <div class="dl-pct">68%</div>
      </div>
      <div class="dl-row">
        <div class="dl-thumb"><div class="poster" style="--tint:#0EA5E9"><i></i><u></u><b>T</b></div></div>
        <div class="dl-txt"><h5>The Odyssey</h5><span>2026 &middot; 720p &middot; 1.4 GB</span>
          <div class="dl-bar"><i style="width:24%"></i></div></div>
        <div class="dl-pct">24%</div>
      </div>
      <div class="dl-row">
        <div class="dl-thumb"><div class="poster" style="--tint:#B91C1C"><i></i><u></u><b>S</b></div></div>
        <div class="dl-txt"><h5>Stranger Things</h5><span>2016 &middot; 1080p &middot; 4.8 GB &middot; Ready</span>
          <div class="dl-bar"><i style="width:100%"></i></div></div>
        <div class="dl-pct">100%</div>
      </div>
      <div class="empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4">
          <path d="M12 3v12m0 0l-4-4m4 4l4-4"/><path d="M4 17v2a2 2 0 002 2h12a2 2 0 002-2v-2"/></svg>
        <h4>No more downloads</h4>
        <p>Download movies and shows to watch offline, anywhere.</p>
      </div>
    </section>

    <!-- ME -->
    <section class="screen" id="me">
      <div class="me-head">
        <div class="avatar">S</div>
        <div><h2>Sameer Khan</h2>
          <span>Member since 2024</span></div>
      </div>
      <div class="stats">
        <div class="stat"><b>128</b><span>Watched</span></div>
        <div class="stat"><b>24</b><span>Saved</span></div>
        <div class="stat"><b>6.4</b><span>Hrs / week</span></div>
      </div>
      <div class="menu">
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M5 4h14v16l-7-4-7 4z"/></svg><span>My List</span>
          <em>24</em><svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="8"/><path d="M12 8v4l3 2"/></svg><span>Watch History</span>
          <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M12 3l2.6 5.6 6.1.8-4.5 4.2 1.2 6-5.4-3-5.4 3 1.2-6L3.3 9.4l6.1-.8z"/></svg>
          <span>Downloads</span>
          <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M4 6h16M4 12h16M4 18h10"/></svg><span>Playback Settings</span>
          <em>1080p</em><svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M12 4v16M4 12h16"/></svg><span>Subtitle Language</span>
          <em>English</em><svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="3.2"/><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6L17 7M7 17l-1.4 1.4"/></svg>
          <span>Appearance</span><em>Dark</em>
          <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
        <div class="mrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="8.5"/><path d="M12 16v-4M12 8.5v.5"/></svg><span>About</span>
          <em>7.7 (26)</em><svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6"/></svg></div>
      </div>
      <div style="height:20px"></div>
    </section>

    <!-- DETAIL -->
    <section class="screen" id="detail">
      <div class="dhero">
        <div class="slide-art" id="d-art" style="--tint:#8B5CF6"><i></i><u></u></div>
        <div class="dhero-veil"></div>
        <div class="dhero-body">
          <div class="dbadges">
            <span class="db k">HD</span>
            <span class="db q">4K DOLBY</span>
            <span class="db f">LIMITED FREE</span>
          </div>
          <h1 id="d-title">The Beast</h1>
          <div class="dmeta" id="d-meta"><span>2026</span><i></i>
            <span>Action,Drama,Thriller</span><i></i>
            <span class="star">&#9733; 8.4</span></div>
          <div class="dact">
            <label class="btn-play wide" for="r-player">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
              Play Now</label>
            <button class="btn-ghost" data-save="t1">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M5 12h14"/></svg>My List</button>
            <button class="btn-ghost" aria-label="Download">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 3v12m0 0l-4-4m4 4l4-4"/><path d="M4 18h16"/></svg></button>
          </div>
        </div>
      </div>
      <div class="tags" id="d-tags"></div>
      <div class="dsec"><h3>Subtitle Preview</h3>
        <p style="margin-bottom:2px">See how captions sit on the frame before you commit.</p></div>
      %(subs)s
      <div class="dsec"><h3>Cast</h3></div>
      %(cast)s
      <div class="dsec"><h3>Storyline</h3>
        <p id="d-syn">A father and his two daughters find themselves hunted by a massive rogue lion intent on proving that the savannah has but one apex predator.</p></div>
      <div class="dsec" id="d-epsec" style="display:none"><h3>Episodes</h3></div>
      <div class="eps" id="d-eps" style="display:none">
        <div class="ep"><div class="ep-thumb"><div class="poster" style="--tint:#1D4ED8"><i></i><u></u></div></div>
          <div class="ep-txt"><h5>E01 &middot; The Opening</h5><span>48 min &middot; 1080p</span></div>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg></div>
        <div class="ep"><div class="ep-thumb"><div class="poster" style="--tint:#1D4ED8"><i></i><u></u></div></div>
          <div class="ep-txt"><h5>E02 &middot; Night Shift</h5><span>52 min &middot; 1080p</span></div>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg></div>
        <div class="ep"><div class="ep-thumb"><div class="poster" style="--tint:#1D4ED8"><i></i><u></u></div></div>
          <div class="ep-txt"><h5>E03 &middot; What the Mill Keeps</h5><span>46 min &middot; 1080p</span></div>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg></div>
      </div>
      <div class="dsec"><h3>Screenshots</h3></div>
      %(shots)s
      <div class="dsec"><h3>Download Quality</h3>
        <p style="margin-bottom:2px">Pick the copy that fits your data plan.</p></div>
      %(ladder)s
      <div class="dsec"><h3>Production Timeline</h3></div>
      %(timeline)s
      <div class="dsec"><h3>Trivia</h3></div>
      %(trivia)s
      <div class="dsec"><h3>Details</h3></div>
      <div class="menu" style="margin-top:0">
        <div class="mrow"><span style="color:var(--txt-3);font-size:11px">Director</span><em>Ira Sengupta</em></div>
        <div class="mrow"><span style="color:var(--txt-3);font-size:11px">Cast</span><em>Aditya Kulkarni, Rhea Menon</em></div>
        <div class="mrow"><span style="color:var(--txt-3);font-size:11px">Language</span><em>Hindi, English</em></div>
        <div class="mrow"><span style="color:var(--txt-3);font-size:11px">Quality</span><em>1080p &middot; HD</em></div>
      </div>
      <div style="height:24px"></div>
    </section>

    <!-- PLAYER -->
    <section class="screen" id="player">
      <div class="pwrap">
        <div class="pwin">
          <div class="slide-art" style="--tint:#8B5CF6"><i></i><u></u></div>
          <button class="pbig"><i><svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z"/></svg></i></button>
          <div class="pui">
            <div class="ptop">
              <div><h4>The Beast</h4><span>2026 &middot; 1080p</span></div>
              <label class="icobtn" for="r-detail" style="color:#fff">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 6l12 12M18 6L6 18"/></svg></label>
            </div>
            <div class="pbot">
              <div class="scrub"><i></i></div>
              <div class="ptime"><span>00:38:14</span><span>02:04:00</span></div>
              <div class="pctl">
                <button class="pc"><svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M11 5L4 12l7 7V5zm9 0l-7 7 7 7V5z"/></svg></button>
                <button class="pc big"><svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg></button>
                <button class="pc"><svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13 5l7 7-7 7V5zM4 5l7 7-7 7V5z"/></svg></button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

  </div>

  <label class="back" for="r-home">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
      <path d="M15 5l-7 7 7 7"/></svg>
  </label>

  <nav class="bnav">
    <label class="nb" for="r-home">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
        <path d="M4 10.5L12 4l8 6.5V20a1 1 0 01-1 1h-4v-6H9v6H5a1 1 0 01-1-1z"/></svg>
      <span>Home</span></label>
    <label class="nb" for="r-search">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
        <circle cx="11" cy="11" r="7"/><path d="m20 20-3.6-3.6"/></svg>
      <span>Search</span></label>
    <label class="nb" for="r-downloads">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
        <path d="M12 3v12m0 0l-4-4m4 4l4-4"/><path d="M4 18h16"/></svg>
      <span>Downloads</span></label>
    <label class="nb" for="r-me">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
        <circle cx="12" cy="8" r="3.6"/><path d="M4.5 20c0-3.8 3.4-5.8 7.5-5.8s7.5 2 7.5 5.8"/></svg>
      <span>Me</span></label>
  </nav>

</div>
</div>

<script>window.MB = %(data_json)s;</script>
<script>
/* Enhancement only. Page is complete without this. */
(function(){
  try{
    var SYN = {
      Movie:"A father and his two daughters find themselves hunted by a massive rogue lion intent on proving that the savannah has but one apex predator.",
      Series:"A season of slow-burning pressure, where every quiet decision turns out to have been the loudest one."
    };
    function go(id){var r=document.getElementById('r-'+id);if(r)r.checked=true;}
    function set(id,v){var e=document.getElementById(id);if(e)e.textContent=v;}

    document.addEventListener('click',function(e){
      var o=e.target.closest('[data-open]');
      if(o){var d=window.MB[o.getAttribute('data-open')];if(d){fill(d);go('detail');}return;}
      var s=e.target.closest('[data-save]');
      if(s){s.classList.toggle('on');return;}
      var g=e.target.closest('[data-genre-filter]');
      if(g){
        var all=document.querySelectorAll('[data-genre-filter]');
        for(var i=0;i<all.length;i++)all[i].classList.remove('on');
        g.classList.add('on');filter(g.getAttribute('data-genre-filter'));
      }
    });

    function fill(d){
      set('d-title',d.title);
      var m=document.getElementById('d-meta');
      if(m)m.innerHTML='<span>'+d.year+'</span><i></i><span>'+d.genres+
        '</span><i></i><span class="star">\u2605 '+Number(d.rating).toFixed(1)+'</span>';
      var a=document.getElementById('d-art');
      if(a)a.style.setProperty('--tint',d.tint);
      var t=document.getElementById('d-tags');
      if(t)t.innerHTML=d.genres.split(',').map(function(x){
        return '<span class="tag">'+x+'</span>';}).join('')+
        '<span class="tag">'+d.kind+'</span><span class="tag">HD</span>';
      set('d-syn',SYN[d.kind]||SYN.Movie);
      var show=d.kind==='Series'?'':'none';
      var es=document.getElementById('d-epsec'),el=document.getElementById('d-eps');
      if(es)es.style.display=show; if(el)el.style.display=show;
      var sc=document.getElementById('detail'); if(sc)sc.scrollTop=0;
    }

    function filter(genre){
      var q=(document.getElementById('q')||{}).value||'';
      q=q.trim().toLowerCase();
      var cards=document.querySelectorAll('#allgrid .card'),n=0;
      for(var i=0;i<cards.length;i++){
        var cg=cards[i].getAttribute('data-genre')||'',cs=cards[i].getAttribute('data-search')||'';
        var okG=genre==='All'||cg.indexOf(genre)>-1;
        var okQ=!q||cs.indexOf(q)>-1;
        cards[i].style.display=(okG&&okQ)?'':'none'; if(okG&&okQ)n++;
      }
      set('cnt',n+' results');
    }
    var q=document.getElementById('q');
    if(q)q.addEventListener('input',function(){
      var on=document.querySelector('[data-genre-filter].on');
      filter(on?on.getAttribute('data-genre-filter'):'All');
    });

    /* hero auto-rotate; first slide is already visible without this */
    var slides=document.querySelectorAll('.slide'),dots=document.querySelectorAll('.dot'),idx=0;
    if(slides.length>1){
      setInterval(function(){
        slides[idx].classList.remove('on'); if(dots[idx])dots[idx].classList.remove('on');
        idx=(idx+1)%slides.length;
        slides[idx].classList.add('on'); if(dots[idx])dots[idx].classList.add('on');
      },4200);
    }
  }catch(err){}
})();
</script>
</body>
</html>
"""

if __name__ == "__main__":
    out = build()
    with open("index.html", "w") as f:
        f.write(out)
    print("wrote index.html", len(out), "bytes")
