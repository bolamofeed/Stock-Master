package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun barcodeGenerator_generatesValidCode128Bits() {
    val bits = com.example.util.BarcodeGenerator.encodeCode128("8901234567890")
    assertTrue(bits.isNotEmpty())
    assertTrue(bits.startsWith("0000000000")) // Quiet zone
    assertTrue(bits.contains("1"))
  }

  @Test
  fun userRole_permissionsAreCorrect() {
    assertFalse(com.example.data.model.UserRole.CASHIER.canSeePurchasePrice)
    assertTrue(com.example.data.model.UserRole.ADMIN.canEditInventory)
    assertTrue(com.example.data.model.UserRole.VENDOR.isVendorOnly)
  }
}
