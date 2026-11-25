package com.rc.playgrounds.config.view

import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UpdateTextKeepingCursorTest {
    @Test
    fun `first text starts the cursor at the beginning`() {
        val edit = EditText(RuntimeEnvironment.getApplication())

        edit.updateTextKeepingCursor("""{"a": 1}""")

        assertEquals(0, edit.selectionStart)
        assertEquals(0, edit.selectionEnd)
    }

    @Test
    fun `later replacement keeps the cursor`() {
        val edit = EditText(RuntimeEnvironment.getApplication())
        edit.setText("abc")
        edit.setSelection(1)

        edit.updateTextKeepingCursor("abcd")

        assertEquals(2, edit.selectionStart)
    }
}
