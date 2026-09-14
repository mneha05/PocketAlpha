"""Generate the lightweight animated product tour used by the README."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


W, H = 960, 600
SCALE = 2
OUT = Path(__file__).parent / "assets" / "pocketalpha-demo.gif"

BG = "#080A09"
SURFACE = "#111411"
RAISED = "#191D19"
LINE = "#2A302A"
GREEN = "#B7F64A"
GREEN_DARK = "#263516"
CORAL = "#FF7474"
WHITE = "#F4F7F1"
MUTED = "#9CA59A"

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def font(size: int, bold: bool = False):
    return ImageFont.truetype(BOLD if bold else FONT, size * SCALE)


def box(draw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(tuple(v * SCALE for v in xy), radius * SCALE, fill, outline, width * SCALE)


def text(draw, xy, value, size, fill=WHITE, bold=False, anchor=None):
    draw.text(tuple(v * SCALE for v in xy), value, font=font(size, bold), fill=fill, anchor=anchor)


def line(draw, xy, fill, width=1):
    draw.line(tuple(v * SCALE for v in xy), fill=fill, width=width * SCALE, joint="curve")


def base_frame(step: int, pulse: float) -> Image.Image:
    image = Image.new("RGB", (W * SCALE, H * SCALE), BG)
    draw = ImageDraw.Draw(image)

    # Quiet grid and ambient green glow.
    for x in range(0, W, 40):
        line(draw, (x, 0, x, H), "#101310")
    for y in range(0, H, 40):
        line(draw, (0, y, W, y), "#101310")
    for r in range(210, 10, -10):
        alpha = int(18 * (1 - r / 220) * (0.85 + pulse * 0.15))
        layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.ellipse(((180-r)*SCALE, (300-r)*SCALE, (180+r)*SCALE, (300+r)*SCALE), fill=(183, 246, 74, alpha))
        image = Image.alpha_composite(image.convert("RGBA"), layer).convert("RGB")
        draw = ImageDraw.Draw(image)

    # Brand and progress.
    draw.ellipse((525*SCALE, 54*SCALE, 557*SCALE, 86*SCALE), fill=GREEN)
    line(draw, (534, 70, 541, 77, 550, 61), BG, 3)
    text(draw, (570, 70), "POCKETALPHA", 17, WHITE, True, "lm")
    text(draw, (525, 112), "Paper investing, end to end", 25, WHITE, True)
    text(draw, (525, 157), "Explore. Decide. Simulate. Learn.", 16, MUTED)

    labels = ["Discover", "Watch", "Analyze", "Trade", "Review"]
    for i, label in enumerate(labels):
        x = 525 + i * 78
        fill = GREEN if i <= step else LINE
        box(draw, (x, 195, x + 62, 199), 2, fill)
        text(draw, (x + 31, 215), label, 9, WHITE if i == step else MUTED, i == step, "ma")

    return image


def phone(image: Image.Image):
    draw = ImageDraw.Draw(image)
    box(draw, (92, 22, 426, 578), 44, "#040504", "#343A34", 2)
    box(draw, (104, 34, 414, 566), 34, BG)
    box(draw, (216, 42, 302, 48), 3, LINE)
    text(draw, (125, 68), "9:41", 10, MUTED, True)
    text(draw, (390, 68), "●  5G", 9, MUTED, False, "ra")
    return draw


def nav(draw, selected: int):
    names = ["Home", "Search", "Watch", "Portfolio"]
    icons = ["⌂", "⌕", "☆", "▥"]
    box(draw, (119, 507, 399, 552), 17, SURFACE, LINE)
    for i, (name, icon) in enumerate(zip(names, icons)):
        x = 154 + i * 69
        color = GREEN if i == selected else MUTED
        text(draw, (x, 520), icon, 15, color, True, "mm")
        text(draw, (x, 539), name, 7, color, i == selected, "mm")


def price_row(draw, y, ticker, name, price, change, selected=False):
    fill = GREEN_DARK if selected else SURFACE
    box(draw, (122, y, 396, y + 54), 14, fill, GREEN if selected else LINE)
    box(draw, (135, y + 11, 167, y + 43), 9, RAISED)
    text(draw, (151, y + 27), ticker[0], 13, GREEN, True, "mm")
    text(draw, (178, y + 17), ticker, 12, WHITE, True)
    text(draw, (178, y + 36), name, 8, MUTED)
    text(draw, (383, y + 17), price, 11, WHITE, True, "ra")
    text(draw, (383, y + 37), change, 8, GREEN if change.startswith("+") else CORAL, True, "ra")


def chart(draw, points, xy, color=GREEN, width=3):
    x0, y0, x1, y1 = xy
    lo, hi = min(points), max(points)
    coords = []
    for i, value in enumerate(points):
        x = x0 + (x1 - x0) * i / (len(points) - 1)
        y = y1 - (value - lo) / max(hi - lo, 1) * (y1 - y0)
        coords.extend([int(x), int(y)])
    line(draw, tuple(coords), color, width)


def home(step=0, pulse=0.0):
    image = base_frame(step, pulse)
    draw = phone(image)
    text(draw, (122, 94), "Good morning, Neha", 13, MUTED)
    text(draw, (122, 120), "$10,000.00", 28, WHITE, True)
    text(draw, (123, 156), "+$142.80  ·  1.45% today", 10, GREEN, True)
    box(draw, (122, 184, 396, 275), 18, SURFACE, LINE)
    text(draw, (138, 201), "PORTFOLIO MOVEMENT", 8, MUTED, True)
    chart(draw, [12, 15, 14, 19, 18, 25, 23, 31, 34], (138, 225, 380, 258))
    text(draw, (122, 301), "Market pulse", 15, WHITE, True)
    price_row(draw, 327, "NVDA", "NVIDIA", "$182.68", "+2.84%")
    price_row(draw, 389, "AAPL", "Apple", "$238.15", "+0.73%")
    nav(draw, 0)
    return image


def discover(step=1, pulse=0.0):
    image = base_frame(step, pulse)
    draw = phone(image)
    text(draw, (122, 96), "Discover", 24, WHITE, True)
    box(draw, (122, 139, 396, 184), 14, SURFACE, GREEN, 2)
    text(draw, (140, 162), "⌕", 18, GREEN, True, "lm")
    text(draw, (170, 162), "NVDA|", 13, WHITE, True, "lm")
    text(draw, (122, 211), "Results", 12, MUTED, True)
    price_row(draw, 238, "NVDA", "NVIDIA", "$182.68", "+2.84%", True)
    price_row(draw, 300, "NFLX", "Netflix", "$1,177.89", "−0.41%")
    box(draw, (122, 379, 396, 457), 16, SURFACE, LINE)
    text(draw, (140, 396), "BUILT FOR THE EDGE CASES", 8, MUTED, True)
    text(draw, (140, 420), "Loading, empty, retry, and error", 11, WHITE, True)
    text(draw, (140, 440), "states stay inside the same flow.", 10, MUTED)
    nav(draw, 1)
    return image


def watch(step=2, pulse=0.0):
    image = base_frame(step, pulse)
    draw = phone(image)
    text(draw, (122, 96), "Watchlist", 24, WHITE, True)
    text(draw, (395, 104), "3 assets", 9, GREEN, True, "ra")
    price_row(draw, 140, "NVDA", "NVIDIA", "$182.68", "+2.84%", True)
    price_row(draw, 202, "AAPL", "Apple", "$238.15", "+0.73%")
    price_row(draw, 264, "TSLA", "Tesla", "$421.77", "−1.12%")
    box(draw, (122, 349, 396, 457), 16, SURFACE, LINE)
    text(draw, (140, 368), "PERSISTENT BY ACCOUNT", 8, MUTED, True)
    text(draw, (140, 395), "Your list follows your session", 12, WHITE, True)
    text(draw, (140, 418), "SQLite-backed • API-synchronized", 9, MUTED)
    box(draw, (140, 434, 250, 448), 7, GREEN_DARK)
    text(draw, (195, 441), "Saved", 8, GREEN, True, "mm")
    nav(draw, 2)
    return image


def analyze(step=3, pulse=0.0):
    image = base_frame(step, pulse)
    draw = phone(image)
    text(draw, (122, 92), "‹  NVDA", 13, WHITE, True)
    text(draw, (395, 92), "★", 18, GREEN, True, "ra")
    text(draw, (122, 128), "$182.68", 29, WHITE, True)
    text(draw, (123, 163), "+$5.03  ·  2.84%", 10, GREEN, True)
    box(draw, (122, 190, 396, 324), 18, SURFACE, LINE)
    chart(draw, [18, 22, 20, 28, 26, 36, 33, 40, 38, 49, 55], (140, 218, 378, 298), GREEN, 3)
    for i, label in enumerate(["1D", "1W", "1M", "1Y"]):
        if i == 0:
            box(draw, (138 + i * 54, 338, 180 + i * 54, 365), 9, GREEN)
        text(draw, (159 + i * 54, 351), label, 8, BG if i == 0 else MUTED, True, "mm")
    box(draw, (122, 393, 255, 451), 15, GREEN)
    text(draw, (188, 422), "Buy", 13, BG, True, "mm")
    box(draw, (263, 393, 396, 451), 15, RAISED, LINE)
    text(draw, (329, 422), "Sell", 13, WHITE, True, "mm")
    nav(draw, 1)
    return image


def trade(step=4, pulse=0.0):
    image = base_frame(step, pulse)
    draw = phone(image)
    text(draw, (122, 93), "Review order", 22, WHITE, True)
    box(draw, (122, 132, 396, 332), 18, SURFACE, GREEN if pulse > 0.4 else LINE, 2)
    text(draw, (140, 154), "BUY", 9, GREEN, True)
    text(draw, (378, 154), "NVDA", 12, WHITE, True, "ra")
    line(draw, (140, 178, 378, 178), LINE)
    text(draw, (140, 203), "Shares", 10, MUTED)
    text(draw, (378, 203), "2", 11, WHITE, True, "ra")
    text(draw, (140, 234), "Estimated price", 10, MUTED)
    text(draw, (378, 234), "$182.68", 11, WHITE, True, "ra")
    text(draw, (140, 265), "Estimated total", 10, MUTED)
    text(draw, (378, 265), "$365.36", 11, WHITE, True, "ra")
    line(draw, (140, 286, 378, 286), LINE)
    text(draw, (140, 309), "Buying power after", 10, MUTED)
    text(draw, (378, 309), "$9,634.64", 11, GREEN, True, "ra")
    box(draw, (122, 354, 396, 411), 16, GREEN)
    text(draw, (259, 382), "Place paper order", 12, BG, True, "mm")
    box(draw, (122, 430, 396, 477), 14, GREEN_DARK)
    text(draw, (145, 454), "✓", 14, GREEN, True, "lm")
    text(draw, (174, 454), "Order executed atomically", 10, WHITE, True, "lm")
    nav(draw, 3)
    return image


SCENES = [home, discover, watch, analyze, trade]
COPY = [
    ("See the whole market", "A focused dashboard combines account value,\ndaily movement, and deterministic quotes."),
    ("Search without friction", "Fast symbol discovery stays usable through\nloading, empty, error, and retry states."),
    ("Build a persistent watchlist", "Every saved asset is scoped to the signed-in\nuser and persisted by the backend."),
    ("Inspect price movement", "A custom Compose Canvas chart turns historical\nprice data into a responsive detail view."),
    ("Execute the paper trade", "Server-side validation updates cash, positions,\nand order history in one transaction."),
]


def decorate(image: Image.Image, index: int, pulse: float):
    draw = ImageDraw.Draw(image)
    title, body = COPY[index]
    text(draw, (525, 282), f"0{index + 1}", 13, GREEN, True)
    text(draw, (525, 315), title, 25, WHITE, True)
    for offset, value in enumerate(body.split("\n")):
        text(draw, (525, 361 + offset * 24), value, 13, MUTED)
    box(draw, (525, 444, 880, 513), 16, SURFACE, LINE)
    labels = [
        "Compose UI  •  REST API",
        "Search state  •  Quote service",
        "User scope  •  SQLite",
        "Canvas chart  •  History API",
        "Validation  •  Atomic write",
    ]
    text(draw, (547, 465), "END-TO-END", 8, GREEN, True)
    text(draw, (547, 490), labels[index], 11, WHITE, True)
    radius = 4 + int(pulse * 2)
    draw.ellipse(((896-radius)*SCALE, (479-radius)*SCALE, (896+radius)*SCALE, (479+radius)*SCALE), fill=GREEN)


def render_scene(index: int, pulse: float) -> Image.Image:
    image = SCENES[index](index, pulse)
    decorate(image, index, pulse)
    return image


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    frames = []
    durations = []
    rendered = [render_scene(i, 0.5) for i in range(len(SCENES))]

    for i, current in enumerate(rendered):
        frames.append(current.resize((720, 450), Image.Resampling.LANCZOS).quantize(colors=64))
        durations.append(950)
        next_frame = rendered[(i + 1) % len(rendered)]
        for transition in range(1, 3):
            t = transition / 3
            eased = t * t * (3 - 2 * t)
            mixed = Image.blend(current, next_frame, eased)
            frames.append(mixed.resize((720, 450), Image.Resampling.LANCZOS).quantize(colors=64))
            durations.append(100)

    frames[0].save(
        OUT,
        save_all=True,
        append_images=frames[1:],
        duration=durations,
        loop=0,
        optimize=True,
        disposal=2,
    )
    print(f"wrote {OUT} ({OUT.stat().st_size / 1024:.1f} KiB, {len(frames)} frames)")


if __name__ == "__main__":
    main()
