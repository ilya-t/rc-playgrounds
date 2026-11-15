#pragma once
#include "Arduino.h"
struct MicroViewMock {
  void begin(){}
  void clear(int){}
  void setFontType(int){}
  void setCursor(int, int){}
  void display(){}
  void print(const char*){}
  void print(char){}
  void print(unsigned int){}
};
extern MicroViewMock uView;