package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Gaveta (
    var fotoBase64: String? = null,
    val name: String,
    val number: String,
):Parcelable
