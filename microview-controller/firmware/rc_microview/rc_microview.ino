// RC MicroView: six RC-servo outputs, UART commands, OLED, hardware failsafe.
// Target: SparkFun MicroView (ATmega328P, 5 V, 16 MHz, 64x48 OLED).
#include <Arduino.h>
#include <MicroView.h>
#include <Servo.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

constexpr uint8_t CHANNEL_COUNT = 6;
const uint8_t OUTPUT_PINS[CHANNEL_COUNT] = {2, 3, 5, 6, A0, A1};
// Motor neutral assumes a reversible car ESC. Change here for other ESC types.
const uint16_t FAILSAFE_PWM[CHANNEL_COUNT] = {1500, 1500, 1500, 1500, 1500, 1500};

constexpr uint32_t SERIAL_BAUD = 115200;
constexpr uint32_t FAILSAFE_TIMEOUT_MS = 350;
constexpr uint32_t DISPLAY_UPDATE_MS = 160;
constexpr uint32_t STATUS_INTERVAL_MS = 250;
constexpr uint8_t MAX_LINE_LENGTH = 95;

Servo outputs[CHANNEL_COUNT];
uint16_t commandedPwm[CHANNEL_COUNT] = {1500, 1500, 1500, 1500, 1500, 1500};
uint16_t appliedPwm[CHANNEL_COUNT] = {1500, 1500, 1500, 1500, 1500, 1500};
uint16_t lastSequence = 0;
bool haveCommand = false;
uint32_t lastValidCommandMs = 0;
uint32_t lastDisplayMs = 0;
uint32_t lastStatusMs = 0;
char receivedLine[MAX_LINE_LENGTH + 1];
uint8_t receivedLength = 0;
bool droppingOversizeLine = false;

bool parseUnsigned(const char *input, uint32_t maxValue, uint32_t *value) {
  if (input == nullptr || *input == '\0') return false;
  uint32_t acc = 0;
  for (const char *p = input; *p != '\0'; ++p) {
    if (*p < '0' || *p > '9') return false;
    const uint8_t digit = static_cast<uint8_t>(*p - '0');
    if (digit > maxValue || acc > (maxValue - digit) / 10U) return false;
    acc = acc * 10U + digit;
  }
  *value = acc;
  return true;
}

int hexDigit(char c) {
  if (c >= '0' && c <= '9') return c - '0';
  if (c >= 'A' && c <= 'F') return c - 'A' + 10;
  if (c >= 'a' && c <= 'f') return c - 'a' + 10;
  return -1;
}

bool verifyChecksumAndTrim(char *line) {
  char *separator = strchr(line, '*');
  if (separator == nullptr || separator[1] == '\0' ||
      separator[2] == '\0' || separator[3] != '\0') return false;
  const int hi = hexDigit(separator[1]);
  const int lo = hexDigit(separator[2]);
  if (hi < 0 || lo < 0) return false;
  uint8_t checksum = 0;
  for (const char *p = line; p != separator; ++p) {
    checksum ^= static_cast<uint8_t>(*p);
  }
  if (checksum != static_cast<uint8_t>((hi << 4) | lo)) return false;
  *separator = '\0';
  return true;
}

void parseCommand(char *line) {
  if (!verifyChecksumAndTrim(line)) return;

  // C,seq,motor,steer,yaw,pitch,mode,smooth = 8 tokens.
  const uint8_t REQUIRED_FIELDS = 8;
  char *fields[REQUIRED_FIELDS] = {};
  uint8_t fieldCount = 0;
  char *save = nullptr;
  for (char *part = strtok_r(line, ",", &save); part != nullptr;
       part = strtok_r(nullptr, ",", &save)) {
    if (fieldCount >= REQUIRED_FIELDS) return;
    fields[fieldCount++] = part;
  }
  if (fieldCount != REQUIRED_FIELDS || strcmp(fields[0], "C") != 0) return;

  uint32_t sequence = 0;
  if (!parseUnsigned(fields[1], 65535, &sequence)) return;

  uint16_t nextPwm[CHANNEL_COUNT];
  for (uint8_t i = 0; i < CHANNEL_COUNT; ++i) {
    uint32_t pwm = 0;
    if (!parseUnsigned(fields[i + 2], 2000, &pwm) || pwm < 1000) return;
    nextPwm[i] = static_cast<uint16_t>(pwm);
  }

  // Don't let repeatedly replayed packets keep the watchdog alive.
  if (haveCommand && sequence == lastSequence) return;
  for (uint8_t i = 0; i < CHANNEL_COUNT; ++i) commandedPwm[i] = nextPwm[i];
  lastSequence = static_cast<uint16_t>(sequence);
  lastValidCommandMs = millis();
  haveCommand = true;
}

void pollSerial() {
  while (Serial.available() > 0) {
    const char ch = static_cast<char>(Serial.read());
    if (ch == '\r') continue;
    if (ch == '\n') {
      if (!droppingOversizeLine && receivedLength > 0) {
        receivedLine[receivedLength] = '\0';
        parseCommand(receivedLine);
      }
      receivedLength = 0;
      droppingOversizeLine = false;
      continue;
    }
    if (droppingOversizeLine) continue;
    if (receivedLength >= MAX_LINE_LENGTH) {
      receivedLength = 0;
      droppingOversizeLine = true;
      continue;
    }
    receivedLine[receivedLength++] = ch;
  }
}

uint8_t deviceState(uint32_t now) {
  if (!haveCommand || now - lastValidCommandMs > FAILSAFE_TIMEOUT_MS) return 2;
  return 1;
}

void applyOutputs(uint8_t state) {
  for (uint8_t i = 0; i < CHANNEL_COUNT; ++i) {
    const uint16_t value = (state == 2) ? FAILSAFE_PWM[i] : commandedPwm[i];
    if (appliedPwm[i] != value) {
      appliedPwm[i] = value;
      outputs[i].writeMicroseconds(value);
    }
  }
}

void displayValues(uint8_t state) {
  const char *labels[CHANNEL_COUNT] = {"M", "S", "Y", "P", "Md", "Sm"};
  uView.clear(PAGE);
  uView.setFontType(0);  // Six 8px rows on the 64x48 OLED.
  for (uint8_t i = 0; i < CHANNEL_COUNT; ++i) {
    uView.setCursor(0, i * 8);
    uView.print(labels[i]);
    uView.print(' ');
    uView.print(static_cast<unsigned int>(appliedPwm[i]));
  }
  uView.setCursor(55, 0);
  uView.print(state == 2 ? '!' : '+');
  uView.display();
}

// Send an ASCII status line with XOR checksum (everything before '*').
void sendStatus(uint32_t now, uint8_t state) {
  uint32_t age = haveCommand ? (now - lastValidCommandMs) : 65535UL;
  if (age > 65535UL) age = 65535UL;
  char payload[62];
  const int n = snprintf(payload, sizeof(payload), "S,%u,%lu,%lu,%u",
                         static_cast<unsigned>(lastSequence),
                         static_cast<unsigned long>(now),
                         static_cast<unsigned long>(age),
                         static_cast<unsigned>(state));
  if (n <= 0 || n >= static_cast<int>(sizeof(payload))) return;
  uint8_t checksum = 0;
  for (int i = 0; i < n; ++i) checksum ^= static_cast<uint8_t>(payload[i]);
  char tail[5];
  snprintf(tail, sizeof(tail), "*%02X\n", static_cast<unsigned>(checksum));
  Serial.print(payload);
  Serial.print(tail);
}

void setup() {
  for (uint8_t i = 0; i < CHANNEL_COUNT; ++i) {
    outputs[i].attach(OUTPUT_PINS[i]);
    outputs[i].writeMicroseconds(FAILSAFE_PWM[i]);
  }
  Serial.begin(SERIAL_BAUD);
  uView.begin();
  uView.clear(PAGE);
  displayValues(2);
}

void loop() {
  pollSerial();
  const uint32_t now = millis();
  const uint8_t state = deviceState(now);
  applyOutputs(state);
  if (now - lastDisplayMs >= DISPLAY_UPDATE_MS) {
    lastDisplayMs = now;
    displayValues(state);
  }
  if (now - lastStatusMs >= STATUS_INTERVAL_MS) {
    lastStatusMs = now;
    sendStatus(now, state);
  }
}