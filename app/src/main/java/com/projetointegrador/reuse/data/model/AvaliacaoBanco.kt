package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AvaliacaoBanco (
    var avaliadorUid: String = "",
    var avaliadoUid: String = "",
    var transacaoUid: String = "",
    var description: String = "",
    var rating: Float = 0f,
    var avaliado: Boolean = false //false = não avaliado e true = avaliado
):Parcelable
