# RC MicroView UART protocol v2 (no ARM/DISARM)

Both Android and MicroView firmware in this repo must use protocol v2.
**The firmware from protocol v1 is incompatible.**

UART: `115200 8N1`. ASCII lines ending with `\n`; `\r` is ignored.
XOR checksum: uppercase 2-digit hex over ASCII bytes *before* `*`.

## Android → MicroView (30 Hz)

```text
C,<seq>,<motor>,<steer>,<yaw>,<pitch>,<mode>,<smooth>*<XX>\n
```

- `seq`: 0..65535, wraps to zero.
- All 6 PWM are integer microseconds 1000..2000, order as shown.
- There is **no ARM/DISARM field or control**. Motor is sent directly.
- A repeated sequence number is ignored (it does not renew watchdog).
- On startup or when no valid new commands arrive for >350 ms, MicroView
  emits failsafe values (all 1500 us by default).
- On app pause / disconnect all 4 joystick axes are centered. Mode and
  smoothness settings remain at selected PWM values until MCU failsafe.

## MicroView → Android (4 Hz)

```text
S,<last-seq>,<uptime-ms>,<last-command-age-ms>,<state>*<XX>\n
```

- `state=1`: MicroView is receiving commands.
- `state=2`: failsafe (new commands missing for >350 ms, or boot).
- `state=0`: reserved (not emitted by this firmware).
- `uptime-ms`: 0..4294967295; `age`: saturated to 65535 ms.

## Safety

No arming step. **Motor may move immediately after moving the throttle stick.**
Default ESC neutral is 1500 us (reversible car ESC); calibrate before use.
Servo outputs and USB adapter must share signal ground. Never power servos
from the MicroView power rail. Test with motor wheels unloaded first.