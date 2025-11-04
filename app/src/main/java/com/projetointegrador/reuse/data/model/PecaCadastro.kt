package com.projetointegrador.reuse.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PecaCadastro(
    var fotoBase64: String? = null,
    var cores: String? = null,
    var categoria: String? = null,
    var tamanho: String? = null,
    var finalidade: String? = null,
    var preco: String? = null,
    var titulo: String? = null,
    var detalhe: String? = null
) : Parcelable