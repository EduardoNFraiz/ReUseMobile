package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Peca(
    val image: Int,
    val descricao: String,
    val preco: String,
):Parcelable