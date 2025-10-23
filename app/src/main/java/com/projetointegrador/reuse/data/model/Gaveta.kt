package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Gaveta (
    val image: Int,
    val name: String,
    val number: String,
):Parcelable
