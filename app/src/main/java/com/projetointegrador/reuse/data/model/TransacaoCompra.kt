package com.projetointegrador.reuse.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TransacaoCompra(
    var vendedorUID: String = "",
    var compradorUID: String = "",
    var dataDaTransacao: String = "",
    var pecaUID: String = "",
    var precoTotal: String = "",
    var formaPagamento: String = "",
    var formaEnvio: String = "",
    var enderecoDestino: String = "",
    var avaliacaoUID: String = "",
): Parcelable