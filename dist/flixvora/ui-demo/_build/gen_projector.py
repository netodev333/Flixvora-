#!/usr/bin/env python3
# Generates "Projector Booth" - a 35mm film-transport UI study for Flixvora.
#
# Hard rules (learned the hard way):
#   * all content is static HTML; JS only enhances
#   * navigation is CSS-only (hidden radios + :checked ~)
#   * never use 100dvh / aspect-ratio / clip-path / position:sticky
import json

# id, title, year, genre, rating, runtime, kind, tag, tint, pattern, tagline,
# synopsis, cast, director, language, quality
TITLES = [
 ("t1","Neon Monsoon",2025,"Neo-Noir",8.7,"2h 14m","Feature","PREMIERE","#FFB347","arc",
  "Every drop remembers a debt.",
  "A rain-drowned Bombay detective takes one last case and finds the city has been keeping his own file. Shot on anamorphic glass, the film turns every puddle into a mirror and every neon sign into a witness.",
  "Aditya Kulkarni, Rhea Menon, Sana Bhatt","Ira Sengupta","Hindi, English","4K HDR"),
 ("t2","The Salt Archive",2024,"Drama",8.2,"1h 58m","Feature","","#6FD8D8","rings",
  "What the tide keeps, the family inherits.",
  "Three siblings return to a coastal salt pan to settle an estate and instead reopen a ledger of debts written in their mother's hand.",
  "Meera Iyer, Joseph Fernandes","Nandita Roy","Malayalam","4K"),
 ("t3","Zero Gravity Club",2025,"Sci-Fi",7.9,"2h 06m","Feature","NEW","#A78BFA","orb",
  "Nine kids. One orbit. No adults.",
  "A school trip to a decommissioned orbital station goes wrong, and nine teenagers have to run a decaying habitat with a manual written for engineers twice their age.",
  "Kabir Shah, Anya Desai","Faraz Ali","Hindi","4K HDR"),
 ("t4","Bitter Orange",2023,"Romance",8.4,"1h 44m","Feature","","#FF7A45","diag",
  "Love, preserved.",
  "A marmalade maker and a food critic spend one winter arguing about bitterness in a shop that smells like a whole orchard.",
  "Nikhil Bose, Leela Raghavan","Priya Chandra","Bengali, Hindi","4K"),
 ("t5","Iron Lullaby",2024,"Thriller",8.0,"2h 21m","Series","SERIES","#9CA3AF","bars",
  "The machine sings at 3 a.m.",
  "A night-shift supervisor at a steel plant starts hearing a lullaby in the rolling mill. Eight episodes, one factory floor, and a tune everybody knows but nobody taught.",
  "Dev Anand Joshi, Saira Khan","Arjun Bhasin","Hindi","4K"),
 ("t6","Paper Tigers",2025,"Comedy",7.6,"1h 51m","Feature","","#FBBF24","arc",
  "They came to conquer. They stayed for the buffet.",
  "A failing regional wrestling league accidentally books a reality TV crew, and the wrestlers discover that being a character pays better than being a champion.",
  "Bunty Sharma, Farida Sheikh","Sneha Kulkarni","Hindi, Marathi","4K"),
 ("t7","Blue Hour",2024,"Drama",8.8,"2h 02m","Feature","AWARD","#60A5FA","rings",
  "Nineteen minutes between day and night.",
  "A cab driver works the exact nineteen minutes of dusk when the city turns blue, ferrying strangers who all seem to be going to the same place.",
  "Zoya Ahmed, Prakash Menon","Ritwik Basu","Hindi, Urdu","4K HDR"),
 ("t8","The Long Field",2023,"Sports",7.8,"2h 17m","Feature","","#4ADE80","diag",
  "Eleven players. One field. Forty years.",
  "A village cricket team that has never won a match keeps showing up, season after season, until the field itself becomes the reason.",
  "Suresh Yadav, Kamla Devi","Harsh Vardhan","Hindi","4K"),
 ("t9","Static Bloom",2025,"Horror",7.4,"1h 47m","Feature","NEW","#F87171","dust",
  "Don't adjust the antenna.",
  "A family inherits a television that only receives one channel: a live feed of their own house, four minutes in the past.",
  "Ishita Rane, Vivek Sethi","Anand Kamath","Hindi","4K"),
 ("t10","Copper Sky",2024,"Sci-Fi",8.1,"2h 09m","Series","SERIES","#FB923C","bars",
  "The colony votes at dawn.",
  "A Mars settlement of four hundred people must decide, every morning, whether to keep breathing the air they make or the air they were promised.",
  "Naina Kapoor, Ibrahim Qureshi","Tara Malhotra","English, Hindi","4K HDR"),
 ("t11","Small Hours",2023,"Romance",8.5,"1h 39m","Feature","","#FCD34D","arc",
  "Two insomniacs, one pharmacy queue.",
  "Every night at 3 a.m. the same two people wait for the same prescription. Neither of them is sick. Both of them are lonely.",
  "Anjali Verma, Sameer Dutta","Kavya Nair","Hindi","4K"),
 ("t12","Grand Bazaar",2025,"Thriller",7.7,"2h 24m","Feature","PREMIERE","#C084FC","diag",
  "Everything is for sale. Including the exit.",
  "A currency trader discovers the antique shop she inherited is a front for the oldest black market in the city, and the inventory list is a hit list.",
  "Farhan Mirza, Deepa Shetty","Imran Sheikh","Hindi, English","4K"),
 ("t13","Salt & Signal",2024,"Documentary",8.9,"1h 32m","Feature","AWARD","#5EEAD4","rings",
  "The last lighthouse keeper of the Konkan coast.",
  "Fourteen months with a man who has kept a lamp burning for forty-one years, and the radio voice he has spoken to every night without ever meeting.",
  "Narayan Sawant","Aditi Rao","Marathi","4K"),
 ("t14","Midnight Tiffin",2023,"Comedy",7.5,"1h 46m","Feature","","#FCA5A5","orb",
  "Dinner is served at 2 a.m. sharp.",
  "A tiffin service that only delivers after midnight accidentally becomes the most exclusive restaurant in the city, and its owner cannot cook.",
  "Guddu Pandit, Shalini Iyer","Rohit Bhatia","Hindi","4K"),
 ("t15","Glass Country",2025,"Drama",8.3,"2h 11m","Series","SERIES","#93C5FD","bars",
  "A family business, built on sand.",
  "Three generations of a glassblowing family in Firozabad, told backwards from the day the furnace goes cold.",
  "Rukmini Jain, Hari Prasad","Sunita Agarwal","Hindi","4K"),
 ("t16","The Quiet Heist",2024,"Thriller",8.6,"2h 03m","Feature","TOP 10","#E5E7EB","arc",
  "No guns. No shouting. No witnesses.",
  "Six deaf thieves plan a robbery in complete silence, using only light, shadow and a shared rhythm. The security system was never the hard part.",
  "Ayesha Noor, Karan Bhat","Neeraj Pillai","Hindi, ISL","4K HDR"),
 ("t17","Monsoon Radio",2023,"Drama",8.0,"1h 55m","Feature","","#34D399","diag",
  "Broadcasting through the flood.",
  "A community radio station keeps transmitting through the worst monsoon in memory, and becomes the only map anyone has.",
  "Vasant Kale, Priya Deshmukh","Mihir Joshi","Marathi, Hindi","4K"),
 ("t18","Red Ledger",2025,"Crime",7.9,"2h 18m","Series","SERIES","#EF4444","bars",
  "Every entry is a name. Every name is a debt.",
  "A forensic accountant is handed a single notebook and eighteen months to prove that the city's cleanest businessman is its dirtiest.",
  "Sameer Qadri, Lata Menon","Aakash Rane","Hindi","4K"),
 ("t19","Feather Weight",2024,"Sports",8.2,"1h 58m","Feature","","#FDE047","rings",
  "Forty-nine kilos of stubborn.",
  "A teenage boxer from a mill town trains for the nationals using a punchbag made of sand and a coach who has never won anything either.",
  "Pooja Rathi, Baliram Singh","Gauri Deshpande","Hindi","4K"),
 ("t20","Night Bus 41",2023,"Horror",7.3,"1h 41m","Feature","","#A78BFA","dust",
  "Last stop is not on the map.",
  "The 41 bus runs one extra route after midnight. The conductor has been doing this shift for eleven years and has never once reached the depot.",
  "Ramesh Teli, Sonal Bhatt","Vikas Dubey","Hindi","4K"),
 ("t21","The Cartographer",2025,"Drama",8.7,"2h 26m","Feature","AWARD","#7DD3FC","arc",
  "He mapped everything except himself.",
  "A surveyor spends thirty years drawing a district that keeps changing shape, and slowly realises the maps are drawing him back.",
  "Girish Kulkarni, Anuradha Sen","Sudha Ranganathan","Hindi, Kannada","4K HDR"),
 ("t22","Copperline",2024,"Crime",7.8,"2h 07m","Series","SERIES","#FB7185","bars",
  "The wire taps back.",
  "A telephone lineman in 1998 starts listening to the conversations on his own exchange, and hears a murder being planned in three parts.",
  "Rajan Mehta, Sushma Rao","Kunal Bhardwaj","Hindi","4K"),
 ("t23","Golden Hour",2023,"Romance",8.1,"1h 48m","Feature","","#FBBF24","diag",
  "Sixty minutes of perfect light.",
  "A wedding photographer who has never been in love agrees to shoot the wedding of the only person she ever loved.",
  "Tanvi Shah, Adil Sheikh","Rhea Kapadia","Hindi, Gujarati","4K"),
 ("t24","Deep Field",2025,"Sci-Fi",8.5,"2h 12m","Feature","NEW","#22D3EE","orb",
  "Ten thousand galaxies. One message.",
  "An observatory in the Himalayas receives a signal that is not a signal but a memory, and the astronomer who decodes it starts remembering a childhood she never had.",
  "Ira Bhatt, Neeraj Sood","Aparna Iyengar","English, Hindi","4K HDR"),
]

LIVE = [
 ("Star Sports 1", "Sports", "SS1", 1, "#FFB347"),
 ("Sony Ten 2", "Sports", "ST2", 1, "#6FD8D8"),
 ("NDTV 24x7", "News", "ND", 1, "#60A5FA"),
 ("Republic Bharat", "News", "RB", 1, "#FBBF24"),
 ("Zee Cinema", "Movies", "ZC", 1, "#C084FC"),
 ("Colors HD", "Entertainment", "CO", 0, "#FF7A45"),
 ("Sun Music", "Music", "SM", 1, "#4ADE80"),
 ("DD National", "General", "DD", 1, "#9CA3AF"),
 ("Discovery Turbo", "Factual", "DT", 0, "#FDE047"),
 ("Star Gold", "Movies", "SG", 1, "#EF4444"),
]

GENRES = ["All", "Drama", "Thriller", "Sci-Fi", "Romance", "Comedy",
          "Crime", "Horror", "Sports", "Documentary"]
SAVED = {"t7", "t16", "t24"}
T = {t[0]: t for t in TITLES}

EPISODES = [("The Machine Sings", "48 min"), ("Night Shift", "52 min"),
            ("What the Mill Keeps", "46 min"), ("Third Shift", "51 min"),
            ("The Lullaby", "49 min")]


def art(t, cls="fart"):
    return ('<div class="%s p-%s" style="--tint:%s"><i></i><u></u></div>'
            % (cls, t[9], t[8]))


def frame_no(i):
    """Real film strips count in 4-perf increments: 1, 1A, 2, 2A ..."""
    n = i // 2 + 1
    return str(n) + ("A" if i % 2 else "")


def strip(items, start=1, ranks=False, cls="strip"):
    out = ['<div class="%s">' % cls]
    out.append('<div class="perf"></div>')
    out.append('<div class="frames">')
    for i, t in enumerate(items):
        badge = ('<em class="fbadge">%s</em>' % t[7]) if t[7] else ""
        rank = ('<b class="frank">%d</b>' % (i + 1)) if ranks else ""
        saved = '<em class="fbadge save">VAULT</em>' if t[0] in SAVED else ""
        blob = " ".join([t[1].replace("&amp;", "&"), t[3], t[6], t[12], t[13]]).lower()
        out.append(
            '<article class="frame" data-open="%s" data-genre="%s" data-search="%s">'
            '<div class="frame-win">%s%s%s%s'
            '<div class="frame-cap"><h4>%s</h4><span>%s &middot; %s</span></div>'
            '</div>'
            '<div class="frame-no">%s</div>'
            '</article>'
            % (t[0], t[3], blob.replace('"', ""), art(t), rank, badge, saved,
               t[1], t[2], t[3], frame_no(i + start - 1)))
    out.append('</div>')
    out.append('<div class="perf"></div>')
    out.append('</div>')
    return "".join(out)


def grid(items):
    out = ['<div class="sheet">']
    for i, t in enumerate(items):
        badge = ('<em class="fbadge">%s</em>' % t[7]) if t[7] else ""
        saved = '<em class="fbadge save">VAULT</em>' if t[0] in SAVED else ""
        blob = " ".join([t[1].replace("&amp;", "&"), t[3], t[6], t[12], t[13]]).lower()
        out.append(
            '<article class="cell" data-open="%s" data-genre="%s" data-search="%s">'
            '<div class="cell-win">%s%s%s'
            '<div class="frame-cap"><h4>%s</h4><span>%s &middot; %s</span></div></div>'
            '<div class="frame-no">%s</div></article>'
            % (t[0], t[3], blob.replace('"', ""), art(t), badge, saved,
               t[1], t[2], t[3], frame_no(i)))
    out.append('</div>')
    return "".join(out)


def setting(label, value=None, toggle=None):
    if toggle is True:
        v = '<i class="sw on" data-toggle></i>'
    elif toggle is False:
        v = '<i class="sw" data-toggle></i>'
    else:
        v = value
    return ('<div class="set-row"><span class="set-k">%s</span>'
            '<span class="set-v">%s</span></div>' % (label, v))


def nav_icon(kind):
    d = {
        "reel": '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="2"/>'
                '<path d="M12 4v4M12 16v4M4 12h4M16 12h4"/>',
        "index": '<path d="M4 5h6v6H4zM14 5h6v6h-6zM4 15h6v4H4zM14 15h6v4h-6z"/>',
        "live": '<path d="M3 8h18v10H3z"/><path d="M8 4l4 4 4-4"/>',
        "vault": '<path d="M5 4h14v16l-7-4-7 4z"/>',
        "booth": '<circle cx="12" cy="9" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/>',
    }[kind]
    return ('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" '
            'stroke-linecap="round" stroke-linejoin="round">%s</svg>' % d)


NAV = [("reel", "REEL", "home"), ("index", "INDEX", "browse"), ("live", "LIVE", "live"),
       ("vault", "VAULT", "saved"), ("booth", "BOOTH", "you")]


def build():
    d = T["t1"]

    nav_html = "".join(
        '<label class="nf" for="r-%s">%s<span>%s</span></label>' % (screen, nav_icon(icon), label)
        for icon, label, screen in NAV)

    hero = ('<div class="gate">'
            '<div class="gate-bracket tl"></div><div class="gate-bracket tr"></div>'
            '<div class="gate-bracket bl"></div><div class="gate-bracket br"></div>'
            '<div class="gate-win">%s'
            '<div class="gate-veil"></div>'
            '<div class="gate-body">'
            '<div class="gate-kicker">FRAME 0001 &middot; PREMIERE</div>'
            '<h1>NEON<br><em>MONSOON</em></h1>'
            '<div class="gate-meta"><span>2025</span><i></i><span>NEO-NOIR</span>'
            '<i></i><span>2H 14M</span><i></i><span class="rt">&#9733; 8.7</span></div>'
            '<div class="gate-act">'
            '<label class="btn-lamp" for="r-detail">'
            '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>'
            'PROJECT</label>'
            '<button class="btn-ghost" data-save="t1">+ VAULT</button>'
            '<label class="btn-icon" for="r-detail">'
            '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">'
            '<path d="M12 5v14M5 12h14"/></svg></label>'
            '</div></div></div></div>' % art(d, "gart"))

    editorial = ('<div class="cue-card">'
                 '<div class="cue-tape"></div>'
                 '<div class="cue-stamp">APPROVED<br>FOR SCREENING</div>'
                 '<div class="cue-k">DIRECTOR&rsquo;S NOTE &middot; REEL 03</div>'
                 '<h3>Salt &amp;<br>Signal</h3>'
                 '<p>&ldquo;Fourteen months with a man who has kept a lamp burning for '
                 'forty-one years, and the radio voice he has spoken to every night '
                 'without ever meeting.&rdquo;</p>'
                 '<label class="cue-link" for="r-detail">OPEN THE FRAME &rarr;</label>'
                 '</div>')

    live_rows = "".join(
        '<div class="ch-row" data-cat="%s">'
        '<div class="ch-logo" style="--tint:%s"><span>%s</span></div>'
        '<div class="ch-txt"><h5>%s</h5><span>%s</span></div>'
        '<div class="ch-state">%s</div></div>'
        % (cat, tint, mono, name, cat,
           '<span class="dot-on">ON AIR</span>' if on else '<span class="dot-off">OFF</span>')
        for (name, cat, mono, on, tint) in LIVE)

    tokens = {
        "nav": nav_html,
        "hero": hero,
        "strip1": strip(TITLES[0:8], start=1),
        "strip2": strip(sorted(TITLES[6:14], key=lambda x: -x[4]), start=40, ranks=True),
        "strip3": strip(TITLES[15:23], start=90),
        "editorial": editorial,
        "chips": "".join('<button class="chip%s" data-genre-filter="%s">%s</button>'
                         % (" on" if i == 0 else "", g, g) for i, g in enumerate(GENRES)),
        "live_chips": "".join(
            '<button class="chip%s" data-live-filter="%s">%s</button>'
            % (" on" if i == 0 else "", c, c)
            for i, c in enumerate(["All", "Sports", "News", "Movies", "Entertainment",
                                   "Music", "General", "Factual"])),
        "grid": grid(TITLES),
        "live_rows": live_rows,
        "vault": grid([t for t in TITLES if t[0] in SAVED]),
        "vault_n": len(SAVED),
        "settings": "".join([
            setting("AUTO-ADVANCE REEL", None, True),
            setting("DOWNLOAD ON WI-FI", None, True),
            setting("DATA SAVER", None, False),
        ]),
        "appearance": "".join([
            setting("LAMP TEMPERATURE", "TUNGSTEN &rsaquo;"),
            setting("SUBTITLE TRACK", "ENGLISH &rsaquo;"),
            setting("GATE SIZE", "4K DOLBY &rsaquo;"),
        ]),
        "about": "".join([
            setting("BUILD", "7.7 (26)"),
            setting("TRANSPORT", "OPTICAL &middot; OK"),
        ]),
        "episodes": "".join(
            '<div class="ep"><b>%02d</b><div><h5>%s</h5><span>%s &middot; 4K</span></div>'
            '<i>&#9654;</i></div>' % (i + 1, n, dur)
            for i, (n, dur) in enumerate(EPISODES)),
        "detail_art": art(d, "gart"),
        "player_art": art(d, "gart"),
        "data_json": json.dumps({
            k: {"title": v[1].replace("&amp;", "&"), "year": v[2], "genre": v[3],
                "rating": v[4], "runtime": v[5], "kind": v[6], "tag": v[7],
                "tint": v[8], "pattern": v[9], "tagline": v[10], "syn": v[11],
                "cast": v[12], "director": v[13], "lang": v[14], "quality": v[15]}
            for k, v in T.items()}, ensure_ascii=False),
    }

    html = TEMPLATE
    for k, v in tokens.items():
        html = html.replace("%(" + k + ")s", str(v))
    return html


TEMPLATE = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#0A0806">
<title>Flixvora &mdash; Projector Booth</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Anton&family=IBM+Plex+Mono:wght@300;400;500;600&family=Inter+Tight:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --lamp:#FFB347;
  --lamp-hot:#FFE0B0;
  --lamp-dim:#6B4A22;
  --film:#191309;
  --film-2:#0E0A06;
  --ink:#070503;
  --paper:#F3EADC;
  --paper-2:#C6B9A4;
  --paper-3:#857A69;
  --cyan:#6FD8D8;
  --edge:rgba(243,234,220,.11);
  --edge-soft:rgba(243,234,220,.055);
  --display:"Anton","Arial Narrow",Impact,sans-serif;
  --mono:"IBM Plex Mono",ui-monospace,"Courier New",monospace;
  --body:"Inter Tight","Helvetica Neue",Helvetica,Arial,sans-serif;
}
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent;}
html,body{margin:0;padding:0;}
body{
  background:#050403;color:var(--paper);font-family:var(--body);
  min-height:100vh;display:flex;align-items:center;justify-content:center;padding:22px 10px;
}
.stage{width:100%;max-width:394px;}
.phone{
  position:relative;width:100%;height:824px;max-height:calc(100vh - 44px);min-height:560px;
  border-radius:40px;overflow:hidden;background:var(--ink);
  border:1px solid rgba(243,234,220,.14);box-shadow:0 44px 90px -34px #000;
}
.navradio{position:absolute;left:-9999px;top:0;width:1px;height:1px;opacity:0;}

/* ══ GRAIN + VIGNETTE (pure CSS, no SVG) ══════════════════════ */
.grain{position:absolute;left:0;top:0;right:0;bottom:0;z-index:70;pointer-events:none;opacity:.5;
  background-image:
    repeating-radial-gradient(circle at 19% 31%,rgba(255,225,180,.05) 0 1px,transparent 1px 3px),
    repeating-radial-gradient(circle at 71% 63%,rgba(255,225,180,.04) 0 1px,transparent 1px 4px);
  background-size:6px 6px,9px 9px;
  animation:grain .9s steps(2) infinite;}
@keyframes grain{0%{background-position:0 0,0 0}50%{background-position:2px 1px,-1px 2px}100%{background-position:0 0,0 0}}
.vignette{position:absolute;left:0;top:0;right:0;bottom:0;z-index:69;pointer-events:none;
  background:radial-gradient(115% 78% at 50% 44%,transparent 52%,rgba(0,0,0,.55) 100%);}

/* ══ STATUS BAR ══════════════════════════════════════════════ */
.sbar{position:absolute;top:0;left:0;right:0;height:42px;z-index:60;
  display:flex;align-items:center;justify-content:space-between;padding:13px 22px 0;
  pointer-events:none;font-family:var(--mono);font-size:10px;letter-spacing:.1em;color:var(--paper-2);}
.sbar .rec{display:flex;align-items:center;gap:5px;font-size:8px;letter-spacing:.22em;}
.sbar .rec::before{content:"";width:5px;height:5px;border-radius:50%;background:#FF4D3D;
  animation:blink 1.4s ease-in-out infinite;}
@keyframes blink{0%,100%{opacity:1}50%{opacity:.15}}
.bars{display:flex;gap:3px;align-items:flex-end;}
.bars i{display:block;width:2.5px;background:currentColor;border-radius:1px;}

/* ══ SCREENS ═════════════════════════════════════════════════ */
.screens{position:absolute;top:0;left:0;right:0;bottom:0;}
.screen{display:none;position:absolute;top:0;left:0;right:0;bottom:0;overflow-y:auto;
  overflow-x:hidden;padding-bottom:104px;-webkit-overflow-scrolling:touch;}
.screen::-webkit-scrollbar{width:0;height:0;}
#r-home:checked~.screens>#home,
#r-browse:checked~.screens>#browse,
#r-live:checked~.screens>#live,
#r-saved:checked~.screens>#saved,
#r-you:checked~.screens>#you,
#r-detail:checked~.screens>#detail,
#r-player:checked~.screens>#player{display:block;}

/* ══ HEADER ══════════════════════════════════════════════════ */
.head{padding:50px 18px 0;}
.head-top{display:flex;align-items:flex-start;justify-content:space-between;}
.brand{display:flex;align-items:center;gap:9px;}
.brand-mark{width:26px;height:26px;border-radius:50%;flex:0 0 26px;
  background:radial-gradient(circle at 34% 30%,var(--lamp-hot),var(--lamp) 42%,#5C3A12 78%);
  box-shadow:0 0 18px -2px rgba(255,179,71,.7),inset 0 0 0 1px rgba(255,224,176,.6);}
.brand-txt h1{margin:0;font-family:var(--display);font-size:19px;letter-spacing:.055em;
  line-height:1;color:var(--paper);}
.brand-txt span{display:block;font-family:var(--mono);font-size:7px;letter-spacing:.3em;
  color:var(--paper-3);margin-top:4px;}
.counter{text-align:right;font-family:var(--mono);font-size:8px;letter-spacing:.16em;
  color:var(--paper-3);line-height:1.8;}
.counter b{display:block;font-size:15px;letter-spacing:.06em;color:var(--lamp);font-weight:500;}

/* ══ FILM RAIL (vertical sprocket edge) ══════════════════════ */
.rail{position:absolute;top:0;bottom:0;width:13px;z-index:5;pointer-events:none;
  background:linear-gradient(90deg,#0A0704,#120D08 55%,#0A0704);}
.rail::before{content:"";position:absolute;left:2px;top:0;bottom:0;width:9px;
  background-image:radial-gradient(ellipse 3px 4px at 50% 50%,rgba(255,179,71,.5) 0 100%,transparent 0);
  background-size:9px 17px;}
.rail.l{left:0;} .rail.r{right:0;}

/* ══ GATE (the projector aperture / hero) ════════════════════ */
.gate{position:relative;margin:20px 22px 0;}
.gate-win{position:relative;height:400px;border-radius:3px;overflow:hidden;
  box-shadow:0 0 0 1px rgba(255,179,71,.3),0 0 44px -12px rgba(255,179,71,.45),
             0 0 90px -30px rgba(255,179,71,.35);}
.gate-veil{position:absolute;left:0;top:0;right:0;bottom:0;z-index:2;
  background:linear-gradient(to top,#070503 3%,rgba(7,5,3,.9) 32%,rgba(7,5,3,.22) 66%,transparent 92%);}
.gate-body{position:absolute;left:0;right:0;bottom:0;z-index:3;padding:0 17px 18px;}
.gate-kicker{font-family:var(--mono);font-size:7.5px;letter-spacing:.28em;color:var(--lamp);
  margin-bottom:9px;}
.gate h1{font-family:var(--display);font-size:46px;line-height:.86;letter-spacing:.005em;
  margin:0 0 11px;color:var(--paper);}
.gate h1 em{font-style:normal;color:var(--lamp);}
.gate-meta{display:flex;align-items:center;gap:7px;flex-wrap:wrap;font-family:var(--mono);
  font-size:8px;letter-spacing:.16em;color:var(--paper-2);margin-bottom:15px;}
.gate-meta i{width:2.5px;height:2.5px;border-radius:50%;background:var(--lamp-dim);}
.gate-meta .rt{color:var(--lamp);}
.gate-act{display:flex;gap:7px;align-items:center;}
/* gate corner brackets */
.gate-bracket{position:absolute;width:13px;height:13px;z-index:6;border-color:var(--lamp);border-style:solid;border-width:0;}
.gate-bracket.tl{top:-5px;left:-5px;border-top-width:1.5px;border-left-width:1.5px;}
.gate-bracket.tr{top:-5px;right:-5px;border-top-width:1.5px;border-right-width:1.5px;}
.gate-bracket.bl{bottom:-5px;left:-5px;border-bottom-width:1.5px;border-left-width:1.5px;}
.gate-bracket.br{bottom:-5px;right:-5px;border-bottom-width:1.5px;border-right-width:1.5px;}

/* ══ BUTTONS ═════════════════════════════════════════════════ */
.btn-lamp,.btn-ghost,.btn-icon{font-family:var(--mono);font-size:9px;letter-spacing:.16em;
  border:none;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;
  gap:6px;text-decoration:none;transition:transform .18s ease,background .18s;}
.btn-lamp,.btn-ghost{border-radius:2px;padding:11px 14px;}
.btn-lamp{background:var(--lamp);color:#150D02;font-weight:600;
  box-shadow:0 0 24px -6px rgba(255,179,71,.8);}
.btn-lamp svg{width:11px;height:11px;}
.btn-ghost{background:rgba(255,179,71,.09);color:var(--lamp);
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.34);}
.btn-ghost.on{background:var(--lamp);color:#150D02;box-shadow:none;}
.btn-icon{width:38px;height:38px;border-radius:2px;background:rgba(255,179,71,.09);
  color:var(--lamp);box-shadow:inset 0 0 0 1px rgba(255,179,71,.34);}
.btn-icon svg{width:13px;height:13px;}
.btn-lamp:active,.btn-ghost:active,.btn-icon:active{transform:scale(.95);}

/* ══ SECTION HEADS ═══════════════════════════════════════════ */
.shead{display:flex;align-items:center;gap:10px;padding:30px 22px 12px;}
.shead h2{margin:0;font-family:var(--display);font-size:15px;letter-spacing:.16em;
  color:var(--paper);white-space:nowrap;}
.shead .ln{flex:1;height:1px;background:repeating-linear-gradient(90deg,var(--edge) 0 5px,transparent 5px 10px);}
.shead .n{font-family:var(--mono);font-size:7.5px;letter-spacing:.2em;color:var(--paper-3);}

/* ══ FILM STRIP ══════════════════════════════════════════════ */
.strip{position:relative;background:var(--film);padding:0;
  border-top:1px solid rgba(255,179,71,.09);border-bottom:1px solid rgba(255,179,71,.09);}
.perf{height:12px;position:relative;
  background-image:radial-gradient(ellipse 3.2px 3.6px at 50% 50%,rgba(255,179,71,.62) 0 100%,transparent 0);
  background-size:15px 100%;background-position:5px 0;
  box-shadow:inset 0 0 14px -4px rgba(255,179,71,.35);}
.frames{display:flex;gap:6px;overflow-x:auto;padding:0 7px 3px;}
.frames::-webkit-scrollbar{height:0;}
.frame{flex:0 0 112px;cursor:pointer;position:relative;}
.frame-win{position:relative;height:150px;border-radius:1px;overflow:hidden;
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.18);}
.frame-cap{position:absolute;left:0;right:0;bottom:0;z-index:3;padding:16px 7px 6px;
  background:linear-gradient(to top,rgba(6,4,2,.97) 34%,transparent);}
.frame-cap h4{margin:0 0 2px;font-family:var(--display);font-size:10.5px;letter-spacing:.06em;
  line-height:1.1;color:var(--paper);}
.frame-cap span{font-family:var(--mono);font-size:6.5px;letter-spacing:.13em;color:var(--paper-3);}
.frame-no{font-family:var(--mono);font-size:6.5px;letter-spacing:.16em;color:var(--lamp-dim);
  padding:4px 2px 5px;text-align:center;}
.fbadge{position:absolute;top:5px;right:5px;z-index:4;font-family:var(--mono);font-size:5.5px;
  letter-spacing:.14em;font-style:normal;padding:2.5px 4px;border-radius:1px;
  background:var(--lamp);color:#150D02;font-weight:600;}
.fbadge.save{background:rgba(7,5,3,.8);color:var(--lamp);
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.5);top:auto;bottom:26px;}
.frank{position:absolute;top:2px;left:5px;z-index:4;font-family:var(--display);font-size:26px;
  line-height:1;color:var(--lamp);text-shadow:0 1px 12px #000,0 0 22px #000;font-weight:400;}

/* ══ PROCEDURAL FRAME ART ════════════════════════════════════ */
.fart,.gart{position:absolute;left:0;top:0;right:0;bottom:0;overflow:hidden;}
.fart::before,.gart::before{content:"";position:absolute;left:-12%;top:-12%;right:-12%;bottom:-12%;
  background:
    radial-gradient(62% 56% at 24% 14%,var(--tint) 0%,transparent 60%),
    radial-gradient(78% 72% at 86% 88%,rgba(255,179,71,.16) 0%,transparent 64%),
    linear-gradient(166deg,#1A1409 6%,#070503 94%);}
.fart::after,.gart::after{content:"";position:absolute;left:0;top:0;right:0;bottom:0;
  background-image:radial-gradient(circle at center,rgba(7,5,3,.72) 1px,transparent 1.3px);
  background-size:4px 4px;opacity:.35;}
.fart i,.gart i{position:absolute;display:block;}
.fart u,.gart u{position:absolute;display:block;left:0;top:0;right:0;bottom:0;
  box-shadow:inset 0 0 34px -6px rgba(0,0,0,.85);}
.p-arc i{width:72%;height:0;padding-bottom:72%;border-radius:50%;left:-15%;top:8%;
  border:1.4px solid rgba(243,234,220,.42);
  box-shadow:0 0 0 16px rgba(243,234,220,.055),0 0 0 40px rgba(243,234,220,.028);}
.p-rings i{width:62%;height:0;padding-bottom:62%;border-radius:50%;right:-12%;top:12%;
  border:1.4px solid rgba(243,234,220,.4);
  box-shadow:0 0 0 12px rgba(243,234,220,.06),0 0 0 30px rgba(243,234,220,.032);}
.p-orb i{width:56%;height:0;padding-bottom:56%;border-radius:50%;right:-6%;bottom:16%;
  background:radial-gradient(circle at 34% 30%,rgba(243,234,220,.5),rgba(243,234,220,.04) 64%,transparent 74%);}
.p-bars i{left:0;top:0;right:0;bottom:0;
  background:repeating-linear-gradient(100deg,rgba(243,234,220,.16) 0 1.4px,transparent 1.4px 15px);}
.p-diag i{width:180%;height:36%;left:-40%;top:28%;
  background:linear-gradient(90deg,transparent,rgba(243,234,220,.22),transparent);transform:rotate(-23deg);}
.p-dust i{left:0;top:0;right:0;bottom:0;
  background-image:radial-gradient(circle,rgba(243,234,220,.5) .8px,transparent .8px);
  background-size:13px 17px;opacity:.5;}

/* ══ SPLICE (section divider) ════════════════════════════════ */
.splice{margin:30px 0 0;height:30px;position:relative;background:var(--film-2);}
.splice::before{content:"";position:absolute;left:0;right:0;top:50%;height:11px;margin-top:-5.5px;
  background:repeating-linear-gradient(90deg,rgba(255,179,71,.22) 0 3px,transparent 3px 9px);}
.splice::after{content:"SPLICE";position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);
  font-family:var(--mono);font-size:6px;letter-spacing:.34em;color:var(--lamp-dim);
  background:var(--film-2);padding:0 7px;}

/* ══ CUE CARD (editorial) ════════════════════════════════════ */
.cue-card{position:relative;margin:0 22px;padding:19px 17px 17px;background:#12100B;
  border:1px solid rgba(255,179,71,.2);box-shadow:inset 0 0 40px -14px rgba(255,179,71,.22);}
.cue-tape{position:absolute;top:-7px;left:50%;margin-left:-26px;width:52px;height:14px;
  background:rgba(255,179,71,.16);transform:rotate(-1.5deg);
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.22);}
.cue-stamp{position:absolute;right:11px;top:11px;border:1.2px solid rgba(255,77,61,.55);
  color:rgba(255,77,61,.8);font-family:var(--mono);font-size:5.5px;letter-spacing:.18em;
  padding:3px 5px;line-height:1.5;text-align:center;transform:rotate(-6deg);}
.cue-k{font-family:var(--mono);font-size:7px;letter-spacing:.26em;color:var(--lamp);
  margin-bottom:10px;}
.cue-card h3{margin:0 0 10px;font-family:var(--display);font-size:27px;line-height:.9;
  letter-spacing:.02em;color:var(--paper);}
.cue-card p{margin:0 0 13px;font-size:11.5px;line-height:1.72;color:var(--paper-2);
  font-weight:300;max-width:88%;}
.cue-link{font-family:var(--mono);font-size:7.5px;letter-spacing:.22em;color:var(--lamp);
  cursor:pointer;border-bottom:1px solid rgba(255,179,71,.45);padding-bottom:2px;display:inline-block;}

/* ══ CHIPS ═══════════════════════════════════════════════════ */
.chips{display:flex;gap:6px;overflow-x:auto;padding:14px 22px 2px;}
.chips::-webkit-scrollbar{height:0;}
.chip{flex:0 0 auto;cursor:pointer;font-family:var(--mono);font-size:7.5px;letter-spacing:.18em;
  padding:7px 10px;border-radius:1px;background:transparent;color:var(--paper-3);
  border:1px solid var(--edge);}
.chip.on{background:var(--lamp);color:#150D02;border-color:var(--lamp);font-weight:600;}

/* ══ SHEET (grid) ════════════════════════════════════════════ */
.sheet{display:flex;flex-wrap:wrap;gap:8px 7px;padding:16px 22px 0;}
.cell{flex:0 0 calc(50% - 4px);cursor:pointer;}
.cell-win{position:relative;height:172px;border-radius:1px;overflow:hidden;
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.16);}
.count{font-family:var(--mono);font-size:7.5px;letter-spacing:.22em;color:var(--paper-3);
  padding:15px 22px 0;}
.searchbox{margin:16px 22px 0;display:flex;align-items:center;gap:9px;padding:11px 13px;
  background:#100D08;border:1px solid var(--edge);border-radius:1px;}
.searchbox svg{width:13px;height:13px;color:var(--paper-3);flex:0 0 13px;}
.searchbox input{flex:1;background:none;border:none;outline:none;font-family:var(--mono);
  font-size:10.5px;letter-spacing:.06em;color:var(--paper);}
.searchbox input::placeholder{color:var(--paper-3);letter-spacing:.1em;}

/* ══ CHANNELS ════════════════════════════════════════════════ */
.ch-row{display:flex;align-items:center;gap:11px;padding:11px 22px;
  border-bottom:1px solid var(--edge-soft);}
.ch-logo{width:44px;height:44px;flex:0 0 44px;display:flex;align-items:center;
  justify-content:center;border-radius:1px;border:1px solid var(--edge);
  background:linear-gradient(150deg,rgba(255,179,71,.1),rgba(7,5,3,.9));}
.ch-logo span{font-family:var(--display);font-size:12px;letter-spacing:.05em;color:var(--tint);}
.ch-txt{flex:1;min-width:0;}
.ch-txt h5{margin:0 0 3px;font-size:11.5px;font-weight:500;color:var(--paper);}
.ch-txt span{font-family:var(--mono);font-size:6.5px;letter-spacing:.18em;color:var(--paper-3);}
.dot-on{display:inline-flex;align-items:center;gap:5px;font-family:var(--mono);font-size:6.5px;
  letter-spacing:.16em;color:#FF4D3D;}
.dot-on::before{content:"";width:4.5px;height:4.5px;border-radius:50%;background:#FF4D3D;
  animation:blink 1.5s ease-in-out infinite;}
.dot-off{font-family:var(--mono);font-size:6.5px;letter-spacing:.16em;color:var(--paper-3);}

/* ══ BOOTH (profile) ═════════════════════════════════════════ */
.booth{padding:52px 22px 0;}
.booth-name{margin:0 0 5px;font-family:var(--display);font-size:34px;line-height:.9;
  letter-spacing:.02em;color:var(--paper);}
.booth-sub{font-family:var(--mono);font-size:7.5px;letter-spacing:.24em;color:var(--lamp);}
.meters{display:flex;margin:20px 22px 0;border-top:1px solid var(--edge);
  border-bottom:1px solid var(--edge);}
.meter{flex:1;padding:16px 3px;text-align:center;border-right:1px solid var(--edge-soft);}
.meter:last-child{border-right:none;}
.meter b{display:block;font-family:var(--display);font-size:24px;line-height:1;margin-bottom:4px;
  color:var(--lamp);}
.meter span{font-family:var(--mono);font-size:6.5px;letter-spacing:.18em;color:var(--paper-3);}
.set-row{display:flex;align-items:center;justify-content:space-between;padding:13px 22px;
  border-bottom:1px solid var(--edge-soft);}
.set-k{font-family:var(--mono);font-size:8px;letter-spacing:.16em;color:var(--paper-2);}
.set-v{font-family:var(--mono);font-size:7.5px;letter-spacing:.16em;color:var(--paper-3);
  display:flex;align-items:center;gap:7px;}
.sw{display:block;width:31px;height:17px;border-radius:100px;position:relative;
  background:rgba(255,179,71,.14);}
.sw::after{content:"";position:absolute;top:2px;left:2px;width:13px;height:13px;border-radius:50%;
  background:var(--paper-3);transition:transform .24s ease;}
.sw.on{background:var(--lamp);}
.sw.on::after{transform:translateX(14px);background:#150D02;}

/* ══ DETAIL (vertical strip) ═════════════════════════════════ */
.vstrip{position:relative;background:var(--film);}
.vperf{height:13px;
  background-image:radial-gradient(ellipse 3.2px 3.6px at 50% 50%,rgba(255,179,71,.6) 0 100%,transparent 0);
  background-size:15px 100%;background-position:5px 0;}
.detail-hero{position:relative;height:392px;overflow:hidden;margin:0 0 0 0;}
.detail-veil{position:absolute;left:0;top:0;right:0;bottom:0;z-index:2;
  background:linear-gradient(to top,#070503 4%,rgba(7,5,3,.92) 32%,rgba(7,5,3,.3) 68%,rgba(7,5,3,.5));}
.detail-body{position:absolute;left:0;right:0;bottom:0;z-index:3;padding:0 22px 18px;}
.detail-body h1{margin:7px 0 9px;font-family:var(--display);font-size:39px;line-height:.88;
  letter-spacing:.01em;color:var(--paper);}
.tagline{margin:0 0 12px;font-size:11.5px;line-height:1.55;color:var(--lamp);
  font-weight:300;letter-spacing:.01em;}
.strip-stats{display:flex;margin:0 22px;border:1px solid var(--edge);background:#0D0A07;}
.st{flex:1;padding:11px 3px;text-align:center;border-right:1px solid var(--edge-soft);}
.st:last-child{border-right:none;}
.st b{display:block;font-family:var(--display);font-size:15px;margin-bottom:2px;color:var(--lamp);}
.st span{font-family:var(--mono);font-size:6px;letter-spacing:.15em;color:var(--paper-3);}
.copy{padding:19px 22px 0;font-size:11.5px;line-height:1.78;color:var(--paper-2);font-weight:300;}
.credits{padding:17px 22px 0;}
.cred{display:flex;justify-content:space-between;gap:14px;padding:9px 0;
  border-bottom:1px solid var(--edge-soft);font-size:11px;}
.cred span:first-child{font-family:var(--mono);font-size:7px;letter-spacing:.2em;
  color:var(--paper-3);padding-top:2px;flex:0 0 auto;}
.cred span:last-child{text-align:right;color:var(--paper);}
.eplist{padding:0 22px;}
.ep{display:flex;align-items:center;gap:11px;padding:11px 0;border-bottom:1px solid var(--edge-soft);}
.ep b{font-family:var(--display);font-size:17px;color:var(--lamp-dim);width:24px;flex:0 0 24px;
  font-weight:400;}
.ep div{flex:1;min-width:0;}
.ep h5{margin:0 0 2px;font-size:11px;font-weight:500;color:var(--paper);}
.ep span{font-family:var(--mono);font-size:6.5px;letter-spacing:.16em;color:var(--paper-3);}
.ep i{width:25px;height:25px;flex:0 0 25px;display:flex;align-items:center;justify-content:center;
  font-size:7px;font-style:normal;border-radius:50%;color:var(--lamp);
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.3);}

/* ══ PLAYER (projection) ═════════════════════════════════════ */
#player{background:#000;padding-bottom:0;}
.proj{position:relative;height:100%;display:flex;flex-direction:column;justify-content:center;}
.proj-win{position:relative;height:216px;overflow:hidden;background:#000;}
.lb{position:absolute;left:0;right:0;height:20px;background:#000;z-index:5;}
.lb.t{top:0;} .lb.b{bottom:0;}
.proj-ui{position:absolute;left:0;top:0;right:0;bottom:0;z-index:7;display:flex;
  flex-direction:column;justify-content:space-between;padding:50px 16px 22px;
  background:linear-gradient(to bottom,rgba(0,0,0,.78) 0%,transparent 24%,transparent 56%,rgba(0,0,0,.92) 100%);}
.proj-top{display:flex;align-items:flex-start;justify-content:space-between;}
.proj-top h4{margin:0 0 3px;font-family:var(--display);font-size:15px;letter-spacing:.05em;}
.proj-top span{font-family:var(--mono);font-size:6.5px;letter-spacing:.2em;color:var(--paper-3);}
.proj-bottom{display:flex;flex-direction:column;gap:10px;}
.scrub{height:2.5px;background:rgba(255,179,71,.2);position:relative;}
.scrub-fill{position:absolute;left:0;top:0;bottom:0;width:34%;background:var(--lamp);}
.scrub-fill::after{content:"";position:absolute;right:-3.5px;top:50%;margin-top:-3.5px;
  width:7px;height:7px;border-radius:50%;background:var(--lamp-hot);}
.trow{display:flex;justify-content:space-between;font-family:var(--mono);font-size:7.5px;
  letter-spacing:.12em;color:var(--paper-2);}
.pctl{display:flex;align-items:center;justify-content:center;gap:22px;}
.pc{background:none;border:none;color:var(--paper);cursor:pointer;opacity:.85;
  display:flex;align-items:center;justify-content:center;padding:0;}
.pc svg{width:15px;height:15px;}
.pc.main{width:44px;height:44px;border-radius:50%;background:var(--lamp);color:#150D02;opacity:1;}
.pc.main svg{width:16px;height:16px;}
.playbig{position:absolute;left:0;top:0;right:0;bottom:0;z-index:6;display:flex;
  align-items:center;justify-content:center;background:none;border:none;cursor:pointer;}
.playbig i{width:56px;height:56px;border-radius:50%;display:flex;align-items:center;
  justify-content:center;background:rgba(255,179,71,.16);
  box-shadow:inset 0 0 0 1px rgba(255,179,71,.45),0 0 34px -8px rgba(255,179,71,.6);}
.playbig svg{width:19px;height:19px;color:var(--lamp-hot);}

/* ══ GATE NAV (film strip nav) ═══════════════════════════════ */
.gnav{position:absolute;left:0;right:0;bottom:0;z-index:62;background:linear-gradient(to top,#060403 62%,rgba(6,4,3,.94) 86%,transparent);padding:0 0 9px;}
.gnav-strip{background:var(--film);border-top:1px solid rgba(255,179,71,.14);}
.gnav .perf{height:8px;
  background-image:radial-gradient(ellipse 2.4px 2.6px at 50% 50%,rgba(255,179,71,.45) 0 100%,transparent 0);
  background-size:13px 100%;background-position:4px 0;}
.navframes{display:flex;}
.nf{flex:1;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:3px;
  padding:8px 1px 7px;color:var(--paper-3);position:relative;}
.nf svg{width:15px;height:15px;}
.nf span{font-family:var(--mono);font-size:5.5px;letter-spacing:.14em;}
#r-home:checked~.gnav .nf[for="r-home"],
#r-browse:checked~.gnav .nf[for="r-browse"],
#r-live:checked~.gnav .nf[for="r-live"],
#r-saved:checked~.gnav .nf[for="r-saved"],
#r-you:checked~.gnav .nf[for="r-you"]{color:var(--lamp);}
#r-home:checked~.gnav .nf[for="r-home"]::before,
#r-browse:checked~.gnav .nf[for="r-browse"]::before,
#r-live:checked~.gnav .nf[for="r-live"]::before,
#r-saved:checked~.gnav .nf[for="r-saved"]::before,
#r-you:checked~.gnav .nf[for="r-you"]::before{
  content:"";position:absolute;left:8%;right:8%;top:0;bottom:0;
  background:rgba(255,179,71,.1);box-shadow:inset 0 0 0 1px rgba(255,179,71,.42);}
#r-detail:checked~.gnav,#r-player:checked~.gnav{display:none;}
.back{position:absolute;top:50px;left:18px;z-index:80;width:34px;height:34px;display:none;
  align-items:center;justify-content:center;cursor:pointer;border-radius:2px;
  background:rgba(7,5,3,.7);color:var(--lamp);box-shadow:inset 0 0 0 1px rgba(255,179,71,.35);}
.back svg{width:13px;height:13px;}
#r-detail:checked~.back{display:flex;}

/* ══ SLATE FOOTER ════════════════════════════════════════════ */
.slate{margin:26px 22px 0;border:1px solid var(--edge);background:#0C0906;}
.slate-row{display:flex;justify-content:space-between;padding:6px 11px;
  border-bottom:1px solid var(--edge-soft);font-family:var(--mono);font-size:6.5px;
  letter-spacing:.18em;color:var(--paper-3);}
.slate-row:last-child{border-bottom:none;}
.slate-row b{color:var(--paper-2);font-weight:400;}

@media (max-width:520px){
  body{padding:0;display:block;background:var(--ink);}
  .stage{max-width:none;}
  .phone{height:100vh;min-height:100vh;max-height:none;border-radius:0;border:none;box-shadow:none;}
}
</style>
</head>
<body>
<div class="stage">
<div class="phone">

  <input class="navradio" type="radio" name="nav" id="r-home" checked>
  <input class="navradio" type="radio" name="nav" id="r-browse">
  <input class="navradio" type="radio" name="nav" id="r-live">
  <input class="navradio" type="radio" name="nav" id="r-saved">
  <input class="navradio" type="radio" name="nav" id="r-you">
  <input class="navradio" type="radio" name="nav" id="r-detail">
  <input class="navradio" type="radio" name="nav" id="r-player">

  <div class="sbar">
    <span class="rec">REC</span>
    <div class="bars"><i style="height:4px"></i><i style="height:7px"></i><i style="height:9px"></i></div>
  </div>

  <div class="screens">

    <!-- ══ REEL (home) ══ -->
    <section class="screen" id="home">
      <div class="rail l"></div><div class="rail r"></div>
      <div class="head">
        <div class="head-top">
          <div class="brand">
            <div class="brand-mark"></div>
            <div class="brand-txt"><h1>FLIXVORA</h1><span>PROJECTOR BOOTH</span></div>
          </div>
          <div class="counter">FRAME<b id="fc">0001</b>REEL 01</div>
        </div>
      </div>
      %(hero)s
      <div class="shead"><h2>REEL 01</h2><div class="ln"></div><span class="n">NOW SHOWING</span></div>
      %(strip1)s
      <div class="shead"><h2>REEL 02</h2><div class="ln"></div><span class="n">TOP OF WEEK</span></div>
      %(strip2)s
      <div class="splice"></div>
      <div class="shead"><h2>CUE CARD</h2><div class="ln"></div><span class="n">EDITOR</span></div>
      %(editorial)s
      <div class="shead"><h2>REEL 03</h2><div class="ln"></div><span class="n">FOR YOU</span></div>
      %(strip3)s
      <div class="slate">
        <div class="slate-row"><span>STUDIO</span><b>FLIXVORA</b></div>
        <div class="slate-row"><span>FORMAT</span><b>35MM &middot; 4K DOLBY</b></div>
        <div class="slate-row"><span>ASPECT</span><b>1.85 : 1</b></div>
        <div class="slate-row"><span>BUILD</span><b>7.7 / 26</b></div>
      </div>
    </section>

    <!-- ══ INDEX (browse) ══ -->
    <section class="screen" id="browse">
      <div class="rail l"></div><div class="rail r"></div>
      <div class="head">
        <div class="head-top">
          <div class="brand">
            <div class="brand-mark"></div>
            <div class="brand-txt"><h1>INDEX</h1><span>FRAME CATALOGUE</span></div>
          </div>
          <div class="counter">TOTAL<b>24</b>FRAMES</div>
        </div>
      </div>
      <label class="searchbox">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
        <input id="search" placeholder="SEARCH FRAMES" autocomplete="off">
      </label>
      <div class="chips">%(chips)s</div>
      <div class="count" id="count">24 FRAMES</div>
      <div id="gridwrap">%(grid)s</div>
      <div class="slate">
        <div class="slate-row"><span>INDEXED</span><b id="scount">24</b></div>
        <div class="slate-row"><span>FILTER</span><b id="sfilter">ALL</b></div>
      </div>
    </section>

    <!-- ══ LIVE ══ -->
    <section class="screen" id="live">
      <div class="rail l"></div><div class="rail r"></div>
      <div class="head">
        <div class="head-top">
          <div class="brand">
            <div class="brand-mark"></div>
            <div class="brand-txt"><h1>BROADCAST</h1><span>288 CHANNELS &middot; IN</span></div>
          </div>
          <div class="counter">LIVE<b>14</b>ON AIR</div>
        </div>
      </div>
      <div class="chips">%(live_chips)s</div>
      <div id="chlist">%(live_rows)s</div>
    </section>

    <!-- ══ VAULT (saved) ══ -->
    <section class="screen" id="saved">
      <div class="rail l"></div><div class="rail r"></div>
      <div class="head">
        <div class="head-top">
          <div class="brand">
            <div class="brand-mark"></div>
            <div class="brand-txt"><h1>VAULT</h1><span>PRIVATE SCREENING</span></div>
          </div>
          <div class="counter">HELD<b>%(vault_n)s</b>FRAMES</div>
        </div>
      </div>
      <div class="count">%(vault_n)s FRAMES IN VAULT</div>
      %(vault)s
    </section>

    <!-- ══ BOOTH (profile) ══ -->
    <section class="screen" id="you">
      <div class="rail l"></div><div class="rail r"></div>
      <div class="booth">
        <h1 class="booth-name">SAMEER<br>KHAN</h1>
        <div class="booth-sub">OPERATOR &middot; PREMIUM &middot; 2024</div>
      </div>
      <div class="meters">
        <div class="meter"><b>128</b><span>SCREENED</span></div>
        <div class="meter"><b>24</b><span>VAULTED</span></div>
        <div class="meter"><b>6.4</b><span>PER WEEK</span></div>
      </div>
      <div class="shead"><h2>TRANSPORT</h2><div class="ln"></div><span class="n">01</span></div>
      %(settings)s
      <div class="shead"><h2>OPTICS</h2><div class="ln"></div><span class="n">02</span></div>
      %(appearance)s
      <div class="shead"><h2>MACHINE</h2><div class="ln"></div><span class="n">03</span></div>
      %(about)s
    </section>

    <!-- ══ FRAME (detail) ══ -->
    <section class="screen" id="detail">
      <div class="vstrip">
        <div class="vperf"></div>
        <div class="detail-hero">
          %(detail_art)s
          <div class="detail-veil"></div>
          <div class="detail-body">
            <div class="gate-kicker" id="d-kicker">FEATURE &middot; PREMIERE</div>
            <h1 id="d-title">NEON MONSOON</h1>
            <p class="tagline" id="d-tag">Every drop remembers a debt.</p>
            <div class="gate-meta" id="d-meta"><span>2025</span><i></i><span>NEO-NOIR</span>
              <i></i><span>2H 14M</span><i></i><span class="rt">&#9733; 8.7</span></div>
            <div class="gate-act">
              <label class="btn-lamp" for="r-player">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                PROJECT</label>
              <button class="btn-ghost" data-save="t1">+ VAULT</button>
            </div>
          </div>
        </div>
        <div class="vperf"></div>
      </div>
      <div class="strip-stats">
        <div class="st"><b id="s1">8.7</b><span>RATING</span></div>
        <div class="st"><b id="s2">2025</b><span>YEAR</span></div>
        <div class="st"><b id="s3">4K</b><span>GATE</span></div>
        <div class="st"><b id="s4">1</b><span>FRAMES</span></div>
      </div>
      <p class="copy" id="d-syn">A rain-drowned Bombay detective takes one last case and finds the city has been keeping his own file.</p>
      <div class="credits">
        <div class="cred"><span>DIRECTOR</span><span id="c-dir">Ira Sengupta</span></div>
        <div class="cred"><span>CAST</span><span id="c-cast">Aditya Kulkarni, Rhea Menon</span></div>
        <div class="cred"><span>LANGUAGE</span><span id="c-lang">Hindi, English</span></div>
        <div class="cred"><span>GATE</span><span id="c-qual">4K HDR &middot; DOLBY ATMOS</span></div>
      </div>
      <div class="shead" id="epsec" style="display:none"><h2>REEL SPLICES</h2>
        <div class="ln"></div><span class="n">SEASON 01</span></div>
      <div class="eplist" id="eplist" style="display:none">%(episodes)s</div>
      <div class="shead"><h2>ADJACENT FRAMES</h2><div class="ln"></div><span class="n">SAME STOCK</span></div>
      %(strip3)s
      <div class="slate">
        <div class="slate-row"><span>SOURCE</span><b>OFFICIAL CATALOGUE</b></div>
        <div class="slate-row"><span>TRANSPORT</span><b>ADAPTIVE HLS</b></div>
      </div>
    </section>

    <!-- ══ PROJECTION (player) ══ -->
    <section class="screen" id="player">
      <div class="proj">
        <div class="proj-win">
          %(player_art)s
          <div class="lb t"></div><div class="lb b"></div>
          <button class="playbig"><i>
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
          </i></button>
        </div>
        <div class="proj-ui">
          <div class="proj-top">
            <div><h4>NEON MONSOON</h4><span>REEL 01 &middot; SPLICE 03</span></div>
            <label class="btn-icon" for="r-detail" style="width:30px;height:30px">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 6l12 12M18 6L6 18"/></svg></label>
          </div>
          <div class="proj-bottom">
            <div class="scrub"><div class="scrub-fill"></div></div>
            <div class="trow"><span>00:41:12</span><span>02:14:00</span></div>
            <div class="pctl">
              <button class="pc"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M19 12H5M11 6l-6 6 6 6"/></svg></button>
              <button class="pc"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M11 5L4 12l7 7V5zm9 0l-7 7 7 7V5z"/></svg></button>
              <button class="pc main"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg></button>
              <button class="pc"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M13 5l7 7-7 7V5zM4 5l7 7-7 7V5z"/></svg></button>
              <button class="pc"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 12h14M13 6l6 6-6 6"/></svg></button>
            </div>
          </div>
        </div>
      </div>
    </section>

  </div>

  <label class="back" for="r-home">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M15 5l-7 7 7 7"/></svg>
  </label>

  <nav class="gnav">
    <div class="gnav-strip">
      <div class="perf"></div>
      <div class="navframes">%(nav)s</div>
      <div class="perf"></div>
    </div>
  </nav>

  <div class="grain"></div>
  <div class="vignette"></div>

</div>
</div>

<script>
window.FLIXVORA = %(data_json)s;
</script>
<script>
/* Enhancement only - the page is complete without this. */
(function(){
  try{
    function go(id){var r=document.getElementById('r-'+id);if(r)r.checked=true;}

    document.addEventListener('click',function(e){
      var p=e.target.closest('[data-open]');
      if(p){var d=window.FLIXVORA[p.getAttribute('data-open')];if(d)fill(d);go('detail');return;}
      var s=e.target.closest('[data-save]');
      if(s){var on=s.classList.toggle('on');s.textContent=on?'\u2713 VAULTED':'+ VAULT';return;}
      var t=e.target.closest('[data-toggle]');
      if(t){t.classList.toggle('on');return;}
    });

    function set(id,v){var el=document.getElementById(id);if(el)el.textContent=v;}

    function fill(d){
      set('d-title',d.title.toUpperCase());
      set('d-tag',d.tagline);
      set('d-syn',d.syn);
      set('c-dir',d.director);
      set('c-cast',d.cast);
      set('c-lang',d.lang);
      set('c-qual',(d.quality||'4K')+' \u00b7 DOLBY ATMOS');
      set('s1',Number(d.rating).toFixed(1));
      set('s2',d.year);
      set('s3',(d.quality||'4K').split(' ')[0]);
      set('s4',d.kind==='Series'?'8':'1');
      var k=document.getElementById('d-kicker');
      if(k){
        var dup=d.tag&&d.tag.toUpperCase()===d.kind.toUpperCase();
        k.innerHTML=d.kind.toUpperCase()+' &middot; '+((d.tag&&!dup)?d.tag:'NOW STREAMING');
      }
      var m=document.getElementById('d-meta');
      if(m)m.innerHTML='<span>'+d.year+'</span><i></i><span>'+
        d.genre.toUpperCase()+'</span><i></i><span>'+d.runtime.toUpperCase()+
        '</span><i></i><span class="rt">\u2605 '+Number(d.rating).toFixed(1)+'</span>';
      var a=document.querySelector('#detail .detail-hero .gart');
      if(a){a.className='gart p-'+d.pattern;a.style.setProperty('--tint',d.tint);}
      var show=d.kind==='Series'?'':'none';
      var es=document.getElementById('epsec'),el=document.getElementById('eplist');
      if(es)es.style.display=show; if(el)el.style.display=show;
      var scr=document.getElementById('detail'); if(scr)scr.scrollTop=0;
    }

    var input=document.getElementById('search'),genre='All';
    if(input)input.addEventListener('input',filter);
    document.addEventListener('click',function(e){
      var c=e.target.closest('[data-genre-filter]');
      if(c){
        genre=c.getAttribute('data-genre-filter');
        var all=document.querySelectorAll('[data-genre-filter]');
        for(var i=0;i<all.length;i++)all[i].classList.remove('on');
        c.classList.add('on');filter();
      }
      var l=e.target.closest('[data-live-filter]');
      if(l){
        var cat=l.getAttribute('data-live-filter');
        var chips=document.querySelectorAll('[data-live-filter]');
        for(var j=0;j<chips.length;j++)chips[j].classList.remove('on');
        l.classList.add('on');
        var rows=document.querySelectorAll('#chlist .ch-row');
        for(var q=0;q<rows.length;q++){
          rows[q].style.display=(cat==='All'||rows[q].getAttribute('data-cat')===cat)?'':'none';
        }
      }
    });

    function filter(){
      var q=(input&&input.value||'').trim().toLowerCase(),cards=document.querySelectorAll('.sheet .cell'),n=0;
      for(var i=0;i<cards.length;i++){
        var g=cards[i].getAttribute('data-genre'),s=cards[i].getAttribute('data-search')||'';
        var ok=(genre==='All'||g===genre)&&(!q||s.indexOf(q)>-1);
        cards[i].style.display=ok?'':'none'; if(ok)n++;
      }
      var c=document.getElementById('count');
      if(c)c.textContent=n+(n===1?' FRAME':' FRAMES');
      set('scount',n); set('sfilter',genre.toUpperCase());
    }

    /* frame counter follows scroll, like a real transport */
    var home=document.getElementById('home'),fc=document.getElementById('fc');
    if(home&&fc){
      home.addEventListener('scroll',function(){
        var p=home.scrollTop/(home.scrollHeight-home.clientHeight||1);
        var f=Math.max(1,Math.round(p*1440)+1);
        fc.textContent=('0000'+f).slice(-4);
      });
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
