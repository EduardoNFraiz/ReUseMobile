package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Gaveta(
    var id: String? = null,
    var ownerUid: String? = null,
    var name: String? = null,
    var fotoBase64: String? = null,
    var privado: Boolean = false,
):Parcelable
