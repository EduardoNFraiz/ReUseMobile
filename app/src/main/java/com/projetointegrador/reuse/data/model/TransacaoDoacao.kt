package com.projetointegrador.reuse.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TransacaoDoacao(
    var doadorUID: String = "",
    var instituicaoUID: String = "",
    var dataDaTransacao: String = "",
    var pecaUID: String = "",
    var enderecoDestino: String = "",
    val formaEnvio: String = "",
    val avaliacaoUID: String = "",
): Parcelable


