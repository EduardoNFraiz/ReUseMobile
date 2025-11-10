package com.projetointegrador.reuse.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Utilitário para formatar um EditText como valor monetário brasileiro (R$ X.XXX,XX).
 * O formato retornado para salvar no banco já inclui o "R$".
 */
class MoneyTextWatcher(editText: EditText) : TextWatcher {
    private val editTextWeakReference: WeakReference<EditText> = WeakReference(editText)

    // Configura o formato de moeda para o padrão brasileiro (R$, vírgula)
    private val currencyFormat: DecimalFormat =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")) as DecimalFormat

    init {
        // Define o símbolo de moeda (R$) e garante o agrupamento (milhar)
        currencyFormat.maximumFractionDigits = 2
        currencyFormat.roundingMode = RoundingMode.HALF_UP
        currencyFormat.currency = Currency.getInstance("BRL")
    }

    private var current: String = ""

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable) {
        val editText = editTextWeakReference.get() ?: return
        val s = editable.toString()

        if (s.isEmpty() || s == current) {
            return
        }

        editText.removeTextChangedListener(this)

        // Limpa a string de qualquer caractere não numérico (incluindo R$ e .)
        val cleanString = s.replace("[^\\d]".toRegex(), "")

        if (cleanString.isNotEmpty()) {
            // Converte a string limpa (ex: "12345" para 123.45)
            val parsed = BigDecimal(cleanString).setScale(2, RoundingMode.HALF_UP).divide(BigDecimal(100))

            // Formata para o padrão R$ X.XXX,XX
            val formatted = currencyFormat.format(parsed)

            current = formatted
            editText.setText(formatted)
            editText.setSelection(formatted.length)
        } else {
            current = ""
            editText.setText("")
        }

        editText.addTextChangedListener(this)
    }

    /**
     * Retorna o valor formatado com o "R$" para salvar no banco, conforme solicitado.
     * Ex: "R$1.234,50"
     */
    fun getFormattedValueForSave(): String {
        val editText = editTextWeakReference.get() ?: return "R$0,00"
        val text = editText.text.toString()
        return if (text.isEmpty()) "R$0,00" else text
    }
}