package com.rc.playgrounds.server.controllink

import androidx.annotation.IntDef

/**
 * `state` in a MicroView status line `S,<seq>,<uptime-ms>,<age-ms>,<state>`.
 * Value 0 is reserved and this firmware does not emit it.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FUNCTION,
)
@IntDef(DeviceState.RECEIVING, DeviceState.FAILSAFE)
annotation class DeviceState {
    companion object {
        /** MicroView is receiving commands and applying them. */
        const val RECEIVING = 1

        /**
         * Safe idle: no valid new command for more than 350 ms, or the board is still booting.
         * Servo and motor outputs are held at neutral (1500 us by default), not at the last command.
         */
        const val FAILSAFE = 2
    }
}
