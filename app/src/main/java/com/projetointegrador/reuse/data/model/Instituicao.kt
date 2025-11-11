package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
@Parcelize
data class Instituicao(
    var fotoBase64: String? = null,
    var uid: String,
    var name: String,
    var username: String,
    var distancia: String,
    var conta: TipoConta = TipoConta.INSTITUICAO
): Parcelable
