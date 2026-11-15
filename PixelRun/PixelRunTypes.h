#pragma once
#include <stdint.h>

// Shared type kept in a header so Arduino's auto-generated prototypes
// can see Obj before declaring functions that use it.
struct Obj {
  uint8_t type;
  bool act;
  float x, y, t;
  int16_t w, h;
  uint8_t st;
};
