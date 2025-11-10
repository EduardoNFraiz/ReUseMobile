package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Task (
    var fotoBase64: String? = null,
    var nomeCompleto: String,
    var nomeDeUsuario: String,
    val rating: Float,
    val conta: TipoConta,
):Parcelable
