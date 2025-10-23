package com.projetointegrador.reuse.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
@Parcelize
data class PecaCloset(
    val image: Int,
    val descricao: String? = null,
    val preco: String? = null
):Parcelable
