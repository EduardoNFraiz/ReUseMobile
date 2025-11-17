package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Historico (
    var fotoBase64: String = "",
    val name: String = "",
    val description: String = "",
    val button: Boolean = false,
    val dataHora: String = "",
    val pecaUID: String = "",
    val avaliacaoUID: String = ""
):Parcelable
