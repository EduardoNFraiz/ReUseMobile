package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Avaliacao (
    var fotoBase64: String? = null,
    val name: String,
    val description: String,
    val rating: Float,
):Parcelable
