#pragma once
#include <stdint.h>
#define A0 14
#define A1 15
#define PAGE 0
struct SerialMock {
  int available(){return 0;}
  int read(){return -1;}
  void begin(uint32_t){}
  void print(const char*){}
};
extern SerialMock Serial;
extern uint32_t testMillis;
inline uint32_t millis(){return testMillis;}