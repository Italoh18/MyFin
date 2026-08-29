package com.example

import com.example.data.model.ExpenseCategory
import com.example.service.NotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testParseNubankNotification() {
        val title = "Nubank"
        val text = "Compra de R$ 42,90 aprovada no iFood"
        assertTrue(NotificationParser.isFinancialNotification(title, text))

        val result = NotificationParser.parse(title, text)
        assertNotNull(result)
        assertEquals(42.90, result!!.amount, 0.001)
        assertEquals("IFood", result.description)
        assertEquals(ExpenseCategory.ALIMENTACAO, result.category)
    }

    @Test
    fun testParseItauPixNotification() {
        val title = "Itaú"
        val text = "Pix enviado no valor de R$ 120,00 para Posto Shell"
        assertTrue(NotificationParser.isFinancialNotification(title, text))

        val result = NotificationParser.parse(title, text)
        assertNotNull(result)
        assertEquals(120.00, result!!.amount, 0.001)
        assertEquals(ExpenseCategory.TRANSPORTE, result.category)
    }

    @Test
    fun testParseLargeAmount() {
        val title = "Cartão de Crédito"
        val text = "Compra de R$ 1.450,50 aprovada em Mercado Livre"
        val result = NotificationParser.parse(title, text)
        assertNotNull(result)
        assertEquals(1450.50, result!!.amount, 0.001)
        assertEquals(ExpenseCategory.COMPRAS, result.category)
    }
}
