#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include "mock_arduino/Arduino.h"
#include "mock_arduino/MicroView.h"

uint32_t testMillis = 0;
SerialMock Serial;
MicroViewMock uView;
#include "../firmware/rc_microview/rc_microview.ino"

void send(const char *payload) {
  uint8_t crc = 0;
  for (const char *p = payload; *p; ++p) crc ^= static_cast<uint8_t>(*p);
  char msg[128];
  std::snprintf(msg, sizeof(msg), "%s*%02X", payload, crc);
  parseCommand(msg);
}

int main() {
  assert(deviceState(testMillis) == 2);
  applyOutputs(2);
  assert(appliedPwm[0] == 1500);

  testMillis = 100;
  send("C,1,1900,1700,1600,1400,1100,1900");
  assert(deviceState(testMillis) == 1);
  applyOutputs(1);
  assert(appliedPwm[0] == 1900 && appliedPwm[1] == 1700);
  assert(appliedPwm[2] == 1600 && appliedPwm[3] == 1400);

  // Broken checksum must be ignored.
  char broken[] = "C,2,2000,2000,2000,2000,2000,2000*00";
  parseCommand(broken);
  assert(lastSequence == 1 && commandedPwm[0] == 1900);

  // A v1 packet containing an extra ARM field must be rejected.
  send("C,2,1,2000,1500,1500,1500,1500,1500");
  assert(lastSequence == 1);

  // Replay cannot refresh watchdog.
  testMillis = 400;
  send("C,1,2000,1700,1600,1400,1100,1900");
  assert(lastValidCommandMs == 100);
  testMillis = 451;
  assert(deviceState(testMillis) == 2);
  applyOutputs(2);
  for (auto value : appliedPwm) assert(value == 1500);

  // New command revives PWM immediately (no arming).
  send("C,2,2000,1700,1600,1400,1100,1900");
  assert(deviceState(testMillis) == 1);
  applyOutputs(1);
  assert(appliedPwm[0] == 2000 && appliedPwm[1] == 1700);

  // Range rejection.
  send("C,3,2001,1700,1600,1400,1100,1900");
  assert(lastSequence == 2);
  send("C,3,0,1700,1600,1400,1100,1900");
  assert(lastSequence == 2);

  // Sequence wraps.
  send("C,65535,1500,1500,1500,1500,1500,1500");
  send("C,0,1500,1500,1500,1500,1500,1500");
  assert(lastSequence == 0);
  std::puts("OK: firmware v2 failsafe/parser tests");
}