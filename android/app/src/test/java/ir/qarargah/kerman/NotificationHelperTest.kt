package ir.qarargah.kerman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun stableIdIsDeterministic() {
        assertEquals(
            NotificationHelper.stableId("یادآوری مراسم: شب شعر"),
            NotificationHelper.stableId("یادآوری مراسم: شب شعر")
        )
    }

    @Test
    fun stableIdIsNonNegative() {
        listOf("", "الف", "مراسم-۱۲۳", "x".repeat(200)).forEach {
            assertTrue(NotificationHelper.stableId(it) >= 0)
        }
    }
}
