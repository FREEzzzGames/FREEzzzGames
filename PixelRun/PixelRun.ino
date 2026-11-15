/*
 * PIXEL RUN - endless monochrome pixel runner for M5Stack Cardputer ADV
 *
 * Board:     M5Stack -> M5Cardputer (ESP32-S3)
 * Libraries: M5Cardputer (+ M5Unified, M5GFX) - latest versions (ADV support)
 *
 * CONTROLS
 *   E / SPACE / ENTER ... jump / up
 *   P / ` / DEL ........ pause menu
 *   M .................. sound on/off
 *   C .................. change color
 *   A / D ............... left / right
 *   Z ................... crouch (hold)
 *
 * Score = steps. Best score, color, character and sound are saved in NVS.
 * 10 tracks x 30 s play one after another in a loop while you run.
 */
#include <M5Cardputer.h>
#include "PixelRunTypes.h"
#include <Preferences.h>
#include <vector>
#include <math.h>

// ============================ CONSTANTS ============================
static const int   SW = 240, SH = 135;
static const int   GY = 108;              // ground surface (screen y)
static const int   PX_SCR = 56;           // hero screen x
static const float GRAV = 900.f;
static const float JUMP_V = -300.f;
static const float SPRING_V = -430.f;
static const float STEP_LEN = 16.f;       // 1 step = 16 px of distance
static const int   NTRACKS = 10;
static const float TRACK_MS = 30000.f;

M5Canvas cv(&M5Cardputer.Display);
Preferences prefs;

// ============================ SETTINGS =============================
static const uint8_t PAL[6][3] = {
  {60, 255, 90}, {255, 176, 0}, {235, 235, 235}, {0, 225, 255}, {255, 70, 70}, {255, 90, 255}
};
static const char* PALN[6] = {"GREEN", "AMBER", "WHITE", "CYAN", "RED", "PINK"};
static const char* CHN[3] = {"MALE", "FEMALE", "ROBOT"};

uint16_t FG, DIM, DIM2, BG = 0;
int colorIdx = 0, charIdx = 0;
bool sndOn = true;
uint32_t hiScore = 0;

enum State { S_MENU, S_PLAY, S_PAUSE, S_OVER };
State state = S_MENU;

// ============================== INPUT ==============================
struct Keys {
  bool space = false, up = false, down = false, left = false, right = false;
  bool crouch = false;
  bool esc = false, mute = false, color = false, enter = false;
};

static std::vector<char> prevWord;
static bool prevEnter = false, prevDel = false, prevTab = false;

static void mapChar(char c, Keys& k) {
  if (c >= 'A' && c <= 'Z') c += 32;
  switch (c) {
    case ' ': k.space = true; break;
    case 'e': case ';': case 'w': k.up = true; break;
    case '.': case 's': k.down = true; break;
    case 'z': k.crouch = true; break;
    case ',': case 'a': k.left = true; break;
    case '/': case 'd': k.right = true; break;
    case 'p': case '`': k.esc = true; break;
    case 'm': k.mute = true; break;
    case 'c': k.color = true; break;
  }
}

Keys readKeys() {
  Keys k;
  M5Cardputer.update();
  auto st = M5Cardputer.Keyboard.keysState();
  for (char c : st.word) if (c == 'z' || c == 'Z') k.crouch = true;
  if (M5Cardputer.Keyboard.isChange()) {
    for (char c : st.word) {
      bool was = false;
      for (char p : prevWord) if (p == c) was = true;
      if (!was) mapChar(c, k);
    }
    if (st.enter && !prevEnter) k.enter = true;
    if (st.del && !prevDel) k.esc = true;
    if (st.tab && !prevTab) k.esc = true;
    prevWord = st.word;
    prevEnter = st.enter;
    prevDel = st.del;
    prevTab = st.tab;
  }
  return k;
}

// ============================== SAVE ===============================
void saveSettings() {
  prefs.putUChar("col", colorIdx);
  prefs.putUChar("chr", charIdx);
  prefs.putBool("snd", sndOn);
}
void saveScore(uint32_t last) {
  prefs.putUInt("hi", hiScore);
  prefs.putUInt("last", last);
}

void applyColor() {
  const uint8_t* c = PAL[colorIdx];
  auto& d = M5Cardputer.Display;
  FG = d.color565(c[0], c[1], c[2]);
  DIM = d.color565(c[0] * 0.30f, c[1] * 0.30f, c[2] * 0.30f);
  DIM2 = d.color565(c[0] * 0.13f, c[1] * 0.13f, c[2] * 0.13f);
}
void applyVolume() {
  M5Cardputer.Speaker.setVolume(sndOn ? 170 : 0);
  if (!sndOn) M5Cardputer.Speaker.stop();
}
void cycleColor(int dir) {
  colorIdx = (colorIdx + dir + 6) % 6;
  applyColor();
  saveSettings();
}
void toggleSound() {
  sndOn = !sndOn;
  applyVolume();
  saveSettings();
}

// ============================== SOUND FX ===========================
enum { FX_JUMP, FX_SPRING, FX_BONK, FX_THUD, FX_DEATH };
void sfx(int t) {
  if (!sndOn) return;
  auto& s = M5Cardputer.Speaker;
  switch (t) {
    case FX_JUMP:   s.tone(520, 35, 1, true); s.tone(780, 45, 1, false); break;
    case FX_SPRING: s.tone(500, 40, 1, true); s.tone(900, 50, 1, false); s.tone(1400, 80, 1, false); break;
    case FX_BONK:   s.tone(180, 60, 1, true); break;
    case FX_THUD:   s.tone(120, 80, 1, true); break;
    case FX_DEATH:  s.tone(440, 110, 1, true); s.tone(330, 110, 1, false); s.tone(220, 110, 1, false); s.tone(130, 300, 1, false); break;
  }
}

// ============================== MUSIC ==============================
// 10 tracks x 30 s. Each track = scale + tempo + seed -> two 16-slot phrases
// (A and B), played as A A B A with a simple bass line.
struct Track { uint8_t root, scale, bpm; uint16_t seed; const char* name; };
static const Track TRACKS[NTRACKS] = {
  {60, 0, 150, 11,  "GREEN HILLS"},
  {57, 1, 130, 23,  "NIGHT RUN"},
  {62, 2, 140, 37,  "CASTLE KEEP"},
  {64, 3, 160, 41,  "SKY ROAD"},
  {59, 5, 120, 53,  "DEEP CAVE"},
  {60, 4, 170, 67,  "DESERT WIND"},
  {66, 1, 110, 71,  "ICE CAVE"},
  {62, 3, 150, 89,  "NEON CITY"},
  {65, 0, 100, 97,  "SUNSET"},
  {57, 2, 180, 101, "FINAL DASH"}
};
static const uint8_t SC[6][7] = {
  {0, 2, 4, 5, 7, 9, 11}, {0, 2, 3, 5, 7, 8, 10}, {0, 2, 3, 5, 7, 9, 10},
  {0, 2, 4, 5, 7, 9, 10}, {0, 2, 4, 7, 9, 0, 0},  {0, 3, 5, 7, 10, 0, 0}
};
static const uint8_t SCN[6] = {7, 7, 7, 7, 5, 5};

int trackIdx = 0;
int8_t phr[2][16];
float slotMs = 200, musicMs = 0, banner = 0;
int lastSlot = -1;

void buildPhrase(int8_t* ph, uint32_t seed, bool tonicEnd, int sc) {
  uint32_t s = seed;
  int n = SCN[sc], maxD = n + 3, d = 0;
  static const int8_t mv[8] = {-2, -1, -1, 0, 1, 1, 2, 3};
  for (int i = 0; i < 16; i++) {
    s = s * 1664525u + 1013904223u;
    uint32_t r = s >> 16;
    if (i == 0) { ph[i] = 0; d = 0; continue; }
    if (i == 15) { d = tonicEnd ? 0 : 1; ph[i] = d; continue; }
    int roll = r % 100;
    if (i % 4 != 0 && roll < 14) { ph[i] = -1; continue; }                       // rest
    if (i % 4 != 0 && roll < 30 && ph[i - 1] != -1) { ph[i] = -2; continue; }    // tie
    d += mv[(r >> 8) % 8];
    if (d < 0) d = 0;
    if (d > maxD) d = maxD;
    ph[i] = d;
  }
}

void buildTrack(int i) {
  trackIdx = i;
  const Track& t = TRACKS[i];
  slotMs = 30000.f / t.bpm;     // eighth note
  buildPhrase(phr[0], t.seed, false, t.scale);
  buildPhrase(phr[1], (uint32_t)t.seed * 7u + 13u, true, t.scale);
  lastSlot = -1;
}

float degFreq(int deg, int semis) {
  const Track& t = TRACKS[trackIdx];
  int n = SCN[t.scale];
  int midi = t.root + 12 * (deg / n) + SC[t.scale][deg % n] + semis;
  return 440.f * powf(2.f, (midi - 69) / 12.f);
}

void playSlot(int s) {
  auto& sp = M5Cardputer.Speaker;
  int bar = (s / 16) % 4, i = s % 16;
  int ph = (bar == 2) ? 1 : 0;
  int8_t d = phr[ph][i];
  if (d >= 0) {
    int len = 1;
    while (i + len < 16 && phr[ph][i + len] == -2) len++;
    sp.tone(degFreq(d, 0), (uint32_t)(slotMs * len * 0.9f), 0, true);
  }
  if (i % 4 == 0) {
    static const int8_t BASS[4] = {0, 3, 4, 0};
    int n = SCN[TRACKS[trackIdx].scale];
    int deg = BASS[bar] + (((i / 4) % 2) ? (n == 7 ? 4 : 2) : 0);
    sp.tone(degFreq(deg, -12), (uint32_t)(slotMs * 3), 2, true);
  }
}

void startMusic() {
  musicMs = 0;
  buildTrack(0);
  banner = 3.f;
}

void updateMusic(float dt) {
  musicMs += dt * 1000.f;
  if (banner > 0) banner -= dt;
  if (musicMs >= TRACK_MS) {
    musicMs -= TRACK_MS;
    buildTrack((trackIdx + 1) % NTRACKS);
    banner = 3.f;
  }
  if (!sndOn) return;
  int slot = (int)(musicMs / slotMs);
  if (slot != lastSlot) {
    lastSlot = slot;
    playSlot(slot);
  }
}

// ============================== WORLD ==============================
enum { T_BLOCK, T_PIT, T_SPIKE, T_PLAT, T_SPRING, T_BEAM, T_CRUMB, T_CRUSH, T_SAW, T_MUD, T_BOOST };
Obj objs[48];

float px, fy, vy, baseSpeed, speedMul, mulTarget, dist, playTime, nextX;
float jumpBuf, coyote, overTimer;
int steps;
bool onGround, dead, newBest;
bool crouching = false;
int pIdx = 0;

Obj* addObj(uint8_t t, float x, float y, int w, int h) {
  for (auto& o : objs) {
    if (!o.act) {
      o = Obj{t, true, x, y, 0.f, (int16_t)w, (int16_t)h, 0};
      return &o;
    }
  }
  return nullptr;
}

float spawnPattern(float x) {
  int maxP = steps < 30 ? 6 : (steps < 80 ? 9 : 12);
  switch (random(maxP)) {
    case 0: {   // 1) solid block: land on top, side hit = crash
      int w = 14 + random(12) * 2;
      int h = (w > 26) ? 12 + random(5) : 12 + random(16);
      addObj(T_BLOCK, x, GY - h, w, h);
      return w;
    }
    case 1: {   // 2) spikes: any touch = crash
      int n = 2 + random(3);
      addObj(T_SPIKE, x, GY - 8, n * 8, 8);
      return n * 8;
    }
    case 2: {   // 3) pit: fall = crash
      int pw = 26 + random(15) + (int)((baseSpeed - 80.f) * 0.1f);
      if (pw > 52) pw = 52;
      addObj(T_PIT, x, GY, pw, 0);
      return pw;
    }
    case 3: {   // 4) one-way platform bridge over a pit
      int pw = 44 + random(21);
      addObj(T_PIT, x, GY, pw + 8, 0);
      addObj(T_PLAT, x + 4, GY - 20, pw, 5);
      return pw + 8;
    }
    case 4: {   // 5) spring + tall wall: bounce over it
      addObj(T_SPRING, x, GY - 6, 12, 6);
      addObj(T_BLOCK, x + 35, GY - 64, 14, 64);
      return 49;
    }
    case 5: {   // 6) stairs: two steps up, then drop
      addObj(T_BLOCK, x, GY - 12, 22, 12);
      addObj(T_BLOCK, x + 22, GY - 24, 22, 24);
      return 44;
    }
    case 6: {   // 7) mud zone: slows you down
      int w = 50 + random(40);
      addObj(T_MUD, x, GY - 3, w, 3);
      return w;
    }
    case 7: {   // 8) boost pad: speeds you up
      addObj(T_BOOST, x, GY - 6, 40, 6);
      return 40;
    }
    case 8: {   // 9) overhead beam: run under, bonk from below, land on top
      int w = 44 + random(31);
      addObj(T_BEAM, x, GY - 32, w, 6);
      return w;
    }
    case 9: {   // 10) falling crusher: drops when you get close
      addObj(T_CRUSH, x, GY - 70, 14, 14);
      return 14;
    }
    case 10: {  // 11) saw on a rail: circle collision, time it
      Obj* o = addObj(T_SAW, x + 8, 0, 16, 0);
      if (o) o->t = random(100) / 10.f;
      return 16;
    }
    default: {  // 12) crumbling tiles over a long pit
      addObj(T_PIT, x, GY, 128, 0);
      for (int i = 0; i < 3; i++) addObj(T_CRUMB, x + i * 50, GY - 10, 28, 5);
      return 128;
    }
  }
}

void startGame() {
  for (auto& o : objs) o.act = false;
  px = 1000; fy = GY; vy = 0;
  baseSpeed = 80; speedMul = 1; mulTarget = 1;
  dist = 0; steps = 0; playTime = 0;
  nextX = px + 260;
  jumpBuf = 0; coyote = 0.1f;
  crouching = false;
  onGround = true; dead = false; newBest = false;
  startMusic();
  state = S_PLAY;
}

void toOver() {
  state = S_OVER;
  dead = true;
  M5Cardputer.Speaker.stop();
  sfx(FX_DEATH);
  newBest = ((uint32_t)steps > hiScore);
  if (newBest) hiScore = steps;
  saveScore(steps);
  overTimer = 0.7f;
}

void updateGame(float dt) {
  playTime += dt;
  baseSpeed = 80.f + fminf(90.f, playTime * 1.0f);
  speedMul += (mulTarget - speedMul) * fminf(1.f, dt * 8.f);
  float v = baseSpeed * speedMul;

  // spawn ahead
  while (nextX < px + SW + 80) {
    float w = spawnPattern(nextX);
    nextX += w + 40.f + baseSpeed * 0.7f + random(40);
  }

  // dynamic objects
  for (auto& o : objs) {
    if (!o.act) continue;
    if (o.type == T_CRUSH) {
      if (o.st == 0 && px > o.x - 80) o.st = 1;
      if (o.st == 1) {
        o.y += 330.f * dt;
        if (o.y + o.h >= GY) { o.y = GY - o.h; o.st = 2; sfx(FX_THUD); }
      }
    } else if (o.type == T_SAW || o.type == T_SPRING) {
      o.t += dt;
    } else if (o.type == T_CRUMB) {
      if (o.st == 1) { o.t += dt; if (o.t > 0.45f) { o.st = 2; o.t = 0; } }
      else if (o.st == 2) { o.t += dt; o.y += 250.f * o.t * dt * 4.f + 60.f * dt; if (o.y > SH) o.act = false; }
    }
    if (o.x + o.w < px - PX_SCR - 40) o.act = false;
  }

  // jump handling
  if (jumpBuf > 0) jumpBuf -= dt;
  if (!onGround) coyote -= dt;
  if (jumpBuf > 0 && coyote > 0) {
    vy = JUMP_V; onGround = false; coyote = 0; jumpBuf = 0;
    sfx(FX_JUMP);
  }

  // integrate
  float prevFy = fy;
  vy += GRAV * dt;
  fy += vy * dt;
  px += v * dt;
  dist += v * dt;
  steps = (int)(dist / STEP_LEN);

  float l = px + 1, r = px + 9, cx = px + 5;
  const float playerH = crouching ? 7.f : 13.f;
  const float playerTop = fy - playerH;
  bool grounded = false;
  float zoneMul = 1.f;

  // ground (with pits)
  bool overPit = false;
  for (auto& o : objs) if (o.act && o.type == T_PIT && cx > o.x && cx < o.x + o.w) overPit = true;
  if (!overPit && vy >= 0 && prevFy <= GY + 0.5f && fy >= GY) { fy = GY; vy = 0; grounded = true; }

  for (auto& o : objs) {
    if (!o.act) continue;
    switch (o.type) {
      case T_BLOCK: case T_BEAM: case T_CRUSH: {
        float ol = o.x, orr = o.x + o.w, ot = o.y, ob = o.y + o.h;
        if (r <= ol || l >= orr) break;
        float top = playerTop;
        if (fy <= ot || top >= ob) break;
        if (vy >= 0 && prevFy <= ot + 0.5f) { fy = ot; vy = 0; grounded = true; }
        else if (vy < 0 && prevFy - playerH >= ob - 0.5f) { fy = ob + playerH; vy = 0; sfx(FX_BONK); }
        else dead = true;
        break;
      }
      case T_PLAT: case T_CRUMB: {
        if (o.type == T_CRUMB && o.st == 2) break;
        if (r <= o.x || l >= o.x + o.w) break;
        if (vy >= 0 && prevFy <= o.y + 0.5f && fy >= o.y) {
          fy = o.y; vy = 0; grounded = true;
          if (o.type == T_CRUMB && o.st == 0) { o.st = 1; o.t = 0; }
        }
        break;
      }
      case T_SPRING: {
        if (r > o.x && l < o.x + o.w && fy > GY - 6 && !(o.st == 1 && o.t < 0.15f)) {
          vy = SPRING_V; grounded = false; o.st = 1; o.t = 0;
          sfx(FX_SPRING);
        }
        break;
      }
      case T_SPIKE: {
        if (r - 1 > o.x + 2 && l + 1 < o.x + o.w - 2 && fy > GY - o.h + 2) dead = true;
        break;
      }
      case T_SAW: {
        float sx = o.x, sy = GY - (21.f + 13.f * sinf(o.t * 2.4f));
        float top = playerTop;
        float nx = fminf(fmaxf(sx, l + 1), r - 1);
        float ny = fminf(fmaxf(sy, top + 1), fy);
        float dx = sx - nx, dy = sy - ny;
        if (dx * dx + dy * dy < 5.5f * 5.5f) dead = true;
        break;
      }
      case T_MUD:   if (cx > o.x && cx < o.x + o.w) zoneMul = 0.6f; break;
      case T_BOOST: if (cx > o.x && cx < o.x + o.w) zoneMul = 1.45f; break;
    }
  }

  onGround = grounded;
  if (grounded) { coyote = 0.08f; mulTarget = zoneMul; }
  if (fy > GY + 16) dead = true;
}

// ============================== DRAWING ============================
static void ctxt(const char* s, int cx, int y, int sz, uint16_t c) {
  cv.setTextSize(sz); cv.setTextColor(c);
  int w = cv.textWidth(s);
  cv.setCursor(cx - w / 2, y);
  cv.print(s);
}
static void ltxt(const char* s, int x, int y, int sz, uint16_t c) {
  cv.setTextSize(sz); cv.setTextColor(c);
  cv.setCursor(x, y);
  cv.print(s);
}

// Hero is 10x14 logical pixels, scaled by s. frame: 0/1 = run, 2 = jump
void drawHero(int x, int feet, int frame, int ch, int s) {
  int t = feet - 14 * s;
  auto R = [&](int rx, int ry, int rw, int rh, uint16_t c) {
    cv.fillRect(x + rx * s, t + ry * s, rw * s, rh * s, c);
  };
  bool air = (frame == 2);
  if (ch == 0) {            // MALE: cap, shirt, trousers
    R(2, 0, 6, 2, FG); R(7, 1, 3, 1, FG);
    R(2, 2, 6, 3, FG); R(6, 3, 1, 1, BG);
    R(2, 5, 6, 5, FG); R(2, 8, 6, 1, BG);
    if (air) { R(0, 4, 2, 2, FG); R(8, 4, 2, 2, FG); R(1, 10, 3, 3, FG); R(6, 11, 3, 3, FG); }
    else if (frame == 0) { R(8, 6, 2, 3, FG); R(2, 10, 2, 4, FG); R(6, 10, 2, 4, FG); }
    else { R(0, 6, 2, 3, FG); R(3, 10, 4, 4, FG); }
  } else if (ch == 1) {     // FEMALE: hair, bow, skirt
    R(2, 0, 6, 2, FG); R(1, 1, 2, 6, FG); R(7, 0, 3, 2, FG);
    R(3, 2, 5, 3, FG); R(6, 3, 1, 1, BG);
    R(3, 5, 4, 3, FG);
    R(2, 8, 6, 2, FG); R(1, 10, 8, 1, FG);
    if (air) { R(7, 4, 2, 2, FG); R(2, 11, 1, 2, FG); R(7, 11, 1, 2, FG); }
    else if (frame == 0) { R(7, 5, 1, 3, FG); R(3, 11, 1, 3, FG); R(6, 11, 1, 3, FG); }
    else { R(2, 5, 1, 3, FG); R(4, 11, 1, 3, FG); R(5, 11, 1, 3, FG); }
  } else {                  // ROBOT: antenna, visor, boxy body
    R(3, 0, 3, 1, FG); R(4, 1, 1, 1, FG);
    R(1, 2, 8, 5, FG); R(6, 3, 2, 2, BG); R(3, 5, 1, 1, BG); R(5, 5, 1, 1, BG);
    R(2, 7, 6, 4, FG); R(4, 8, 2, 2, BG);
    if (air) { R(0, 7, 1, 3, FG); R(9, 7, 1, 3, FG); R(2, 11, 2, 2, FG); R(6, 11, 2, 2, FG); }
    else if (frame == 0) { R(0, 8, 1, 3, FG); R(9, 7, 1, 3, FG); R(2, 11, 2, 3, FG); R(6, 11, 2, 3, FG); }
    else { R(0, 7, 1, 3, FG); R(9, 8, 1, 3, FG); R(3, 11, 2, 3, FG); R(5, 11, 2, 3, FG); }
  }
}

static void drawCloud(int x, int y) {
  cv.fillRect(x + 4, y, 12, 3, DIM);
  cv.fillRect(x, y + 3, 22, 3, DIM);
  cv.fillRect(x + 8, y - 2, 6, 2, DIM);
}

void drawBackground(float cam) {
  int off = (int)(cam * 0.35f);
  for (int x = 0; x < SW; x += 2) {
    int w = (x + off) % 96;
    int h = (w < 48 ? w / 2 : (96 - w) / 2) + 6;
    cv.fillRect(x, GY - h, 2, h, DIM2);
  }
  int co = ((int)(cam * 0.2f)) % 240;
  if (co < 0) co += 240;
  for (int k = 0; k < 2; k++) {
    int bx = k * 240 - co;
    drawCloud(bx + 30, 16);
    drawCloud(bx + 120, 32);
    drawCloud(bx + 200, 12);
  }
}

void drawGround(float cam) {
  int o16 = (((int)cam) % 16 + 16) % 16;
  cv.fillRect(0, GY, SW, 2, FG);
  cv.drawFastHLine(0, GY + 10, SW, DIM);
  cv.drawFastHLine(0, GY + 20, SW, DIM);
  for (int x = -o16; x < SW; x += 16) cv.drawFastVLine(x, GY + 2, 8, DIM);
  for (int x = -o16 + 8; x < SW; x += 16) cv.drawFastVLine(x, GY + 11, 9, DIM);
  for (int x = -o16; x < SW; x += 16) cv.drawFastVLine(x, GY + 21, SH - GY - 21, DIM);
  for (auto& o : objs) {
    if (!o.act || o.type != T_PIT) continue;
    int sx = (int)floorf(o.x - cam);
    if (sx > SW || sx + o.w < 0) continue;
    cv.fillRect(sx, GY, o.w, SH - GY, BG);
    cv.drawFastVLine(sx, GY, SH - GY, FG);
    cv.drawFastVLine(sx + o.w - 1, GY, SH - GY, FG);
  }
}

void drawObj(const Obj& o, float cam) {
  int sx = (int)floorf(o.x - cam);
  if (sx > SW + 10 || sx + o.w < -10) return;
  int y = (int)o.y;
  switch (o.type) {
    case T_BLOCK: case T_CRUSH:
      cv.fillRect(sx, y, o.w, o.h, DIM);
      cv.drawRect(sx, y, o.w, o.h, FG);
      for (int yy = y + 8; yy < y + o.h - 1; yy += 8) cv.drawFastHLine(sx, yy, o.w, FG);
      if (o.type == T_CRUSH) {
        if (o.st == 0) cv.drawFastVLine(sx + o.w / 2, 0, y, DIM);
        cv.fillRect(sx + 3, y + 4, 3, 3, FG);
        cv.fillRect(sx + o.w - 6, y + 4, 3, 3, FG);
        cv.fillRect(sx + 3, y + 9, o.w - 6, 2, FG);
      }
      break;
    case T_SPIKE:
      for (int i = 0; i < o.w / 8; i++)
        cv.fillTriangle(sx + i * 8, GY, sx + i * 8 + 4, GY - 8, sx + i * 8 + 8, GY, FG);
      break;
    case T_PLAT:
      cv.fillRect(sx, y, o.w, o.h, FG);
      for (int xx = sx + 3; xx < sx + o.w - 2; xx += 6) cv.fillRect(xx, y + o.h, 2, 3, DIM);
      break;
    case T_CRUMB: {
      int j = (o.st == 1) ? ((millis() / 40) & 1) : 0;
      cv.fillRect(sx + j, y, o.w, 5, FG);
      cv.fillRect(sx + j + o.w / 2, y, 1, 5, BG);
      cv.fillRect(sx + j + 6, y + 2, 3, 1, BG);
      break;
    }
    case T_SPRING: {
      int h = (o.st == 1 && o.t < 0.2f) ? 3 : 6;
      cv.fillRect(sx, GY - h, 12, 2, FG);
      cv.drawLine(sx + 2, GY - 1, sx + 9, GY - h + 2, FG);
      cv.drawLine(sx + 9, GY - 1, sx + 2, GY - h + 2, FG);
      cv.fillRect(sx, GY - 1, 12, 1, FG);
      break;
    }
    case T_BEAM:
      cv.drawFastVLine(sx + 4, 0, y, DIM);
      cv.drawFastVLine(sx + o.w - 5, 0, y, DIM);
      cv.fillRect(sx, y, o.w, o.h, FG);
      for (int xx = sx + 3; xx < sx + o.w - 3; xx += 6) cv.fillRect(xx, y + 2, 2, 2, BG);
      break;
    case T_SAW: {
      int cxs = sx, cys = (int)(GY - (21.f + 13.f * sinf(o.t * 2.4f)));
      cv.drawFastVLine(cxs, GY - 50, 50, DIM);
      cv.fillCircle(cxs, cys, 5, FG);
      cv.fillCircle(cxs, cys, 2, BG);
      for (int k = 0; k < 8; k++) {
        float a = o.t * 9.f + k * 0.7854f;
        cv.drawLine(cxs + (int)(cosf(a) * 4), cys + (int)(sinf(a) * 4),
                    cxs + (int)(cosf(a) * 8), cys + (int)(sinf(a) * 8), FG);
      }
      break;
    }
    case T_MUD:
      cv.fillRect(sx, GY - 3, o.w, 3, DIM);
      for (int xx = sx; xx < sx + o.w; xx += 4) cv.drawPixel(xx, GY - 3 - ((xx >> 2) & 1), FG);
      break;
    case T_BOOST:
      for (int i = 0; i < o.w / 10; i++) {
        int bx = sx + i * 10;
        cv.drawLine(bx, GY - 6, bx + 4, GY - 3, FG);
        cv.drawLine(bx + 4, GY - 3, bx, GY, FG);
        cv.drawLine(bx + 1, GY - 6, bx + 5, GY - 3, FG);
        cv.drawLine(bx + 5, GY - 3, bx + 1, GY, FG);
      }
      break;
  }
}

void drawHUD() {
  char b[40];
  snprintf(b, sizeof(b), "STEPS %06d", steps);
  ltxt(b, 4, 3, 1, FG);
  snprintf(b, sizeof(b), "HI %06lu", (unsigned long)hiScore);
  ltxt(b, SW - 4 - cv.textWidth(b), 3, 1, DIM);
  ltxt(sndOn ? "SND" : "MUTE", 4, 13, 1, DIM);
  if (banner > 0 && state == S_PLAY) {
    cv.fillRect(0, SH - 11, SW, 11, BG);
    snprintf(b, sizeof(b), "TRACK %d/%d  %s", trackIdx + 1, NTRACKS, TRACKS[trackIdx].name);
    ctxt(b, SW / 2, SH - 9, 1, FG);
  }
}

void drawWorld() {
  float cam = px - PX_SCR;
  cv.fillScreen(BG);
  drawBackground(cam);
  drawGround(cam);
  for (auto& o : objs) if (o.act) drawObj(o, cam);
  int frame = onGround ? (steps & 1) : 2;
  if (crouching && onGround) {
    cv.fillRect(PX_SCR + 1, (int)fy - 7, 9, 5, FG);
    cv.fillRect(PX_SCR + 6, (int)fy - 9, 4, 3, FG);
    cv.fillRect(PX_SCR + 2, (int)fy - 2, 3, 2, FG);
    cv.fillRect(PX_SCR + 7, (int)fy - 2, 3, 2, FG);
  } else {
    drawHero(PX_SCR, (int)fy, frame, charIdx, 1);
  }
  drawHUD();
}

void drawPause() {
  cv.fillRect(50, 22, 140, 92, BG);
  cv.drawRect(50, 22, 140, 92, FG);
  cv.drawRect(52, 24, 136, 88, DIM);
  ctxt("PAUSED", SW / 2, 29, 2, FG);
  char b[32];
  const char* items[4];
  items[0] = "RESUME";
  snprintf(b, sizeof(b), "SOUND: %s", sndOn ? "ON" : "OFF");
  char b2[32];
  snprintf(b2, sizeof(b2), "COLOR: %s", PALN[colorIdx]);
  items[1] = b; items[2] = b2; items[3] = "QUIT TO MENU";
  for (int i = 0; i < 4; i++) {
    int y = 52 + i * 14;
    if (i == pIdx) { cv.fillRect(58, y - 2, 124, 12, FG); ltxt(items[i], 64, y, 1, BG); }
    else ltxt(items[i], 64, y, 1, FG);
  }
}

void drawOver() {
  cv.fillRect(40, 28, 160, 70, BG);
  cv.drawRect(40, 28, 160, 70, FG);
  ctxt("GAME OVER", SW / 2, 34, 2, FG);
  char b[32];
  snprintf(b, sizeof(b), "STEPS: %d", steps);
  ctxt(b, SW / 2, 56, 1, FG);
  snprintf(b, sizeof(b), "BEST:  %lu", (unsigned long)hiScore);
  ctxt(b, SW / 2, 68, 1, FG);
  if (newBest && ((millis() / 400) & 1)) ctxt("NEW BEST!", SW / 2, 80, 1, FG);
  ctxt("ENTER retry   ` menu", SW / 2, 88, 1, DIM);
}

void drawMenu() {
  cv.fillScreen(BG);
  drawGround(1000);
  ctxt("PIXEL RUN", SW / 2, 5, 2, FG);
  ctxt("endless steps", SW / 2, 24, 1, DIM);
  ltxt("<", 66, 46, 2, FG);
  ltxt(">", 160, 46, 2, FG);
  int frame = (millis() / 220) & 1;
  drawHero(105, 78, frame, charIdx, 3);
  ctxt(CHN[charIdx], SW / 2, 84, 1, FG);
  char b[48];
  snprintf(b, sizeof(b), "COLOR %s   SOUND %s", PALN[colorIdx], sndOn ? "ON" : "OFF");
  ctxt(b, SW / 2, 96, 1, FG);
  snprintf(b, sizeof(b), "BEST %lu", (unsigned long)hiScore);
  cv.fillRect(0, GY + 2, SW, SH - GY - 2, BG);
  ctxt(b, SW / 2, GY + 4, 1, FG);
  ctxt("ENTER start  C color  M snd", SW / 2, GY + 16, 1, DIM);
}

// ============================== MAIN ===============================
uint32_t lastMs = 0;

void setup() {
  auto cfg = M5.config();
  M5Cardputer.begin(cfg, true);
  M5Cardputer.Display.setRotation(4);
  cv.setColorDepth(16);
  cv.createSprite(SW, SH);
  prefs.begin("pxrun", false);
  hiScore = prefs.getUInt("hi", 0);
  colorIdx = prefs.getUChar("col", 0) % 6;
  charIdx = prefs.getUChar("chr", 0) % 3;
  sndOn = prefs.getBool("snd", true);
  applyColor();
  M5Cardputer.Speaker.setChannelVolume(0, 200);
  M5Cardputer.Speaker.setChannelVolume(1, 255);
  M5Cardputer.Speaker.setChannelVolume(2, 90);
  applyVolume();
  randomSeed(esp_random());
  lastMs = millis();
}

void loop() {
  uint32_t now = millis();
  float dt = (now - lastMs) / 1000.f;
  lastMs = now;
  if (dt > 0.04f) dt = 0.04f;

  Keys k = readKeys();
  crouching = k.crouch;

  switch (state) {
    case S_MENU:
      if (k.left)  { charIdx = (charIdx + 2) % 3; saveSettings(); }
      if (k.right) { charIdx = (charIdx + 1) % 3; saveSettings(); }
      if (k.color) cycleColor(1);
      if (k.mute)  toggleSound();
      if (k.enter || k.space) startGame();
      drawMenu();
      break;

    case S_PLAY:
      if (k.mute) toggleSound();
      if (k.color) cycleColor(1);
      if (k.esc) {
        state = S_PAUSE; pIdx = 0;
        M5Cardputer.Speaker.stop();
        if ((uint32_t)steps > hiScore) { hiScore = steps; }
        saveScore(steps);
      }
      if (k.space || k.up || k.enter) jumpBuf = 0.12f;
      if (state == S_PLAY) {
        updateGame(dt);
        updateMusic(dt);
        if (dead) toOver();
      }
      drawWorld();
      if (state == S_PAUSE) drawPause();
      if (state == S_OVER) drawOver();
      break;

    case S_PAUSE:
      if (k.mute) toggleSound();
      if (k.color) cycleColor(1);
      if (k.up)   pIdx = (pIdx + 3) % 4;
      if (k.down) pIdx = (pIdx + 1) % 4;
      if (k.esc) { state = S_PLAY; lastSlot = -1; }
      else if (k.enter || k.left || k.right) {
        int dir = k.left ? -1 : 1;
        if (pIdx == 0 && k.enter) { state = S_PLAY; lastSlot = -1; }
        else if (pIdx == 1) toggleSound();
        else if (pIdx == 2) cycleColor(dir);
        else if (pIdx == 3 && k.enter) { state = S_MENU; M5Cardputer.Speaker.stop(); }
      }
      if (state == S_PAUSE) { drawWorld(); drawPause(); }
      else if (state == S_MENU) drawMenu();
      else drawWorld();
      break;

    case S_OVER:
      overTimer -= dt;
      if (overTimer <= 0) {
        if (k.enter || k.space) startGame();
        else if (k.esc) state = S_MENU;
      }
      if (k.color) cycleColor(1);
      if (state == S_MENU) drawMenu();
      else { drawWorld(); if (state == S_OVER) drawOver(); }
      break;
  }

  cv.pushSprite(0, 0);
}