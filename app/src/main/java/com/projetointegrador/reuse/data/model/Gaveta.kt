package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Gaveta(
    var ownerUid: String? = null,
    var nome: String? = null,
    var fotoBase64: String? = null,
    var privado: Boolean = false,
):Parcelable
