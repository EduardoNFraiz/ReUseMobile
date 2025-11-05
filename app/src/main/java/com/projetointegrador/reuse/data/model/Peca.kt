package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Peca(
    var fotoBase64: String? = null,
    var cores: String? = null,
    var categoria: String? = null,
    var tamanho: String? = null,
    var finalidade: String? = null,
    var preco: String? = null,
    var titulo: String? = null,
    var detalhe: String? = null
):Parcelable