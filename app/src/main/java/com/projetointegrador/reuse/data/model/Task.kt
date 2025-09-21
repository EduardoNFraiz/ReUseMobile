package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Task (
    val image: Int,
    val name: String,
    val username: String,
    val rating: Float,
    val conta: TipoConta = TipoConta.USUARIO
):Parcelable
