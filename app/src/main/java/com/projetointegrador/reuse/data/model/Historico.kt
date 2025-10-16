package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Historico (
    val image: Int,
    val name: String,
    val description: String,
    val button: Boolean,
):Parcelable
