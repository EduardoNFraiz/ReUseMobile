package com.projetointegrador.reuse.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Utilitário para aplicar máscaras de formatação em EditTexts de forma robusta.
 */
object MaskEditUtil {

    // Constantes de formato
    const val FORMAT_CPF = "###.###.###-##"
    // Máscara mais flexível para celular/telefone fixo: (xx) xxxx-xxxx ou (xx) xxxxx-xxxx
    const val FORMAT_PHONE_BR = "(##) #####-####"
    const val FORMAT_DATE = "##/##/####"

    const val FORMAT_CNPJ = "##.###.###/####-##"

    const val FORMAT_CEP = "#####-###"

    /**
     * Aplica uma máscara ao EditText.
     * @param editText O EditText onde a máscara será aplicada.
     * @param mask O padrão da máscara (ex: ###.###.###-##).
     */
    fun mask(editText: EditText, mask: String): TextWatcher {
        return object : TextWatcher {
            var isUpdating: Boolean = false
            var old = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                old = s.toString()
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val str = s.toString().replace("[^\\d]".toRegex(), "")
                var formatted = ""
                var i = 0

                // Evita loops infinitos ou comportamento inesperado
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                // Itera sobre a máscara
                for (m in mask.toCharArray()) {
                    if (m != '#') {
                        // Caractere da máscara (., -, /, (, ))
                        if (i == str.length && mask != FORMAT_PHONE_BR) break

                        // Lógica de telefone para evitar que o hífen extra seja inserido precocemente
                        if(mask == FORMAT_PHONE_BR && i == 10 && str.length < 11){
                            // Não insere o traço se for o formato de 8 dígitos
                            continue
                        }

                        formatted += m
                        continue
                    }

                    // Caractere de dígito (#)
                    try {
                        formatted += str[i]
                    } catch (e: Exception) {
                        break
                    }
                    i++
                }

                isUpdating = true
                editText.setText(formatted)
                editText.setSelection(i + (formatted.length - formatted.replace("[^\\d]".toRegex(), "").length))
            }

            override fun afterTextChanged(s: Editable) {}
        }
    }
}