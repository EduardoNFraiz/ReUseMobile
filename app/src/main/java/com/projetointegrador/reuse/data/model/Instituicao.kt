package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Instituicao(
    val image: Int,
    val name: String,
    val distancia: String,
    val conta: TipoConta = TipoConta.INSTITUICAO
): Parcelable
