package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Gaveta(
    var id: String? = null,
    val name: String? = null,
    val number: String? = null,
    val fotoBase64: String? = null,
    val public: Boolean = false,
):Parcelable
