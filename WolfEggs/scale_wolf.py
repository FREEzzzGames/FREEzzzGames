from pathlib import Path

p = Path("WolfEggs/WolfEggs.ino")
src = p.read_text(encoding="utf-8")
start = src.index("void drawWolf() {")
end = src.index("\nvoid drawHUD()", start)
replacement = """static int wolfX(int x) { return 120 + (x - 120) / 2; }
static int wolfY(int y) { return 70 + (y - 70) / 2; }
static void wolfRect(int x, int y, int w, int h, uint16_t c) {
  int x1 = wolfX(x), y1 = wolfY(y);
  int x2 = wolfX(x + w), y2 = wolfY(y + h);
  cv.fillRect(x1, y1, max(1, x2 - x1), max(1, y2 - y1), c);
}
static void wolfTriangle(int x1, int y1, int x2, int y2, int x3, int y3, uint16_t c) {
  cv.fillTriangle(wolfX(x1), wolfY(y1), wolfX(x2), wolfY(y2), wolfX(x3), wolfY(y3), c);
}

void drawWolf() {
  uint16_t c = FG;
  int f = wSide;
  // 50% scale, centered on the original wolf position.
  wolfTriangle(110, 34, 114, 26, 118, 34, c);
  wolfTriangle(122, 34, 126, 26, 130, 34, c);
  wolfRect(109, 34, 22, 16, c);
  if (f == 0) { wolfRect(103, 39, 8, 7, c); wolfRect(103, 39, 2, 2, BG); }
  else        { wolfRect(129, 39, 8, 7, c); wolfRect(135, 39, 2, 2, BG); }
  wolfRect(f == 0 ? 113 : 125, 38, 2, 2, BG);
  wolfRect(110, 50, 20, 34, c);
  wolfRect(110, 68, 20, 2, BG);
  wolfRect(112, 84, 6, 26, c);
  wolfRect(122, 84, 6, 26, c);
  wolfRect(108, 110, 10, 4, c);
  wolfRect(122, 110, 10, 4, c);
  int by = (wRow ? 66 : 30) + 18;
  for (int side = 0; side < 2; side++) {
    bool act = (mode == 2) || (side == wSide);
    if (act) {
      if (side == 0) wolfRect(105, by + 4, 6, 3, c);
      else           wolfRect(129, by + 4, 6, 3, c);
    } else {
      if (side == 0) wolfRect(107, 58, 3, 18, c);
      else           wolfRect(130, 58, 3, 18, c);
    }
  }
}
"""
p.write_text(src[:start] + replacement + src[end:], encoding="utf-8")
