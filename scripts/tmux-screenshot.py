#!/usr/bin/env python3
"""Turns a colour capture of a tmux pane into a PNG (used for the README screenshot).

    tmux capture-pane -t SESSION -e -p > shot.ansi
    python3 scripts/tmux-screenshot.py shot.ansi docs/images/agentfather.png

Needs Pillow and the DejaVu fonts. Reads the 24-bit and 256-colour escape codes tmux prints; wide characters take two cells.
"""
import glob
import re
import sys
import unicodedata

from PIL import Image, ImageDraw, ImageFont

SC = 2
CW, CH, FS = 9 * SC, 19 * SC, 30
DEF_FG, DEF_BG = (233, 237, 239), (11, 20, 26)
ANSI16 = [(0, 0, 0), (205, 49, 49), (13, 188, 121), (229, 229, 16), (36, 114, 200), (188, 63, 188), (17, 168, 205), (229, 229, 229),
          (102, 102, 102), (241, 76, 76), (35, 209, 139), (245, 245, 67), (59, 142, 234), (214, 112, 214), (41, 184, 219), (255, 255, 255)]
SGR = re.compile(r'\x1b\[([0-9;]*)m')
OTHER = re.compile(r'\x1b\[[0-9;?]*[A-Za-z]')


def c256(n):
    if n < 16:
        return ANSI16[n]
    if n < 232:
        n -= 16
        f = lambda v: 0 if v == 0 else 55 + 40 * v
        return f(n // 36), f((n // 6) % 6), f(n % 6)
    v = 8 + 10 * (n - 232)
    return v, v, v


def width(ch):
    return 2 if unicodedata.east_asian_width(ch) in ('W', 'F') else 1


def parse(lines):
    rows = []
    ends = []
    for line in lines:
        fg, bg, bold, rev = DEF_FG, None, False, False
        cells, i, col = [], 0, 0
        while i < len(line):
            m = SGR.match(line, i)
            if m:
                p = [int(x) if x else 0 for x in m.group(1).split(';')]
                k = 0
                while k < len(p):
                    c = p[k]
                    if c == 0:
                        fg, bg, bold, rev = DEF_FG, None, False, False
                    elif c == 1:
                        bold = True
                    elif c == 7:
                        rev = True
                    elif c == 22:
                        bold = False
                    elif c == 27:
                        rev = False
                    elif 30 <= c <= 37:
                        fg = ANSI16[c - 30]
                    elif 90 <= c <= 97:
                        fg = ANSI16[c - 82]
                    elif 40 <= c <= 47:
                        bg = ANSI16[c - 40]
                    elif 100 <= c <= 107:
                        bg = ANSI16[c - 92]
                    elif c == 39:
                        fg = DEF_FG
                    elif c == 49:
                        bg = None
                    elif c in (38, 48):
                        if p[k + 1] == 2:
                            v = tuple(p[k + 2:k + 5])
                            k += 4
                        else:
                            v = c256(p[k + 2])
                            k += 2
                        if c == 38:
                            fg = v
                        else:
                            bg = v
                    k += 1
                i = m.end()
                continue
            if line[i] == '\x1b':
                o = OTHER.match(line, i)
                i = o.end() if o else i + 1
                continue
            ch = line[i]
            i += 1
            w = width(ch)
            f, b = ((bg or DEF_BG), fg) if rev else (fg, bg)
            cells.append((col, ch, w, f, b, bold))
            col += w
        rows.append(cells)
        ends.append((bg, col))
    return rows, ends


def main(src, dst):
    lines = open(src, encoding='utf-8').read().split('\n')
    while lines and not lines[-1].strip():
        lines.pop()
    rows, ends = parse(lines)
    cols = max((c[0] + c[2] for r in rows for c in r), default=80)
    # tmux drops the blanks at the end of a line, but they keep the background that was active: put them back
    for r, (bg, col) in zip(rows, ends):
        if bg is not None:
            for x in range(col, cols):
                r.append((x, ' ', 1, DEF_FG, bg, False))
    pad = 16
    img = Image.new('RGB', (cols * CW + 2 * pad, len(rows) * CH + 2 * pad), DEF_BG)
    d = ImageDraw.Draw(img)
    base = '/usr/share/fonts/truetype/dejavu/'
    reg = ImageFont.truetype(base + 'DejaVuSansMono.ttf', FS)
    bld = ImageFont.truetype(base + 'DejaVuSansMono-Bold.ttf', FS)
    fall = [ImageFont.truetype(base + 'DejaVuSans.ttf', FS)]
    for f in glob.glob('/usr/share/fonts/truetype/noto/*Symbols2*.ttf'):
        fall.append(ImageFont.truetype(f, FS))
    tofu = reg.getmask('￿').getbbox()

    def has(font, ch):
        try:
            bb = font.getmask(ch).getbbox()
        except Exception:
            return False
        return bb is not None and bb != tofu

    for r, cells in enumerate(rows):
        y = pad + r * CH
        for (col, ch, w, f, b, bold) in cells:
            if b is not None and b != DEF_BG:
                d.rectangle([pad + col * CW, y, pad + (col + w) * CW - 1, y + CH - 1], fill=b)
        for (col, ch, w, f, b, bold) in cells:
            if ch == ' ':
                continue
            x = pad + col * CW
            font = bld if bold else reg
            if ch == '＋':
                ch = '+'
            if not has(font, ch):
                font = next((ff for ff in fall if has(ff, ch)), None)
            if font is None:
                continue
            d.text((x + (w * CW - font.getlength(ch)) / 2 if w == 2 else x, y + 2), ch, font=font, fill=f)
    img.save(dst, optimize=True)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
