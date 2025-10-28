package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Endereco(
    var cep: String,
    var rua: String,
    var numero: String,
    var complemento: String,
    var bairro: String,
    var cidade: String,
    var estado: String,
    var pais: String,
):Parcelable
