package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Avaliacao (
    val image: Int,
    val name: String,
    val description: String,
    val rating: Float,
):Parcelable
