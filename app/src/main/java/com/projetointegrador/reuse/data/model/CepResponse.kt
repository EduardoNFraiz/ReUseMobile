package com.projetointegrador.reuse.data.model

import com.google.gson.annotations.SerializedName

data class CepResponse(
    @SerializedName("cep")
    val cep: String = "",
    @SerializedName("logradouro")
    val logradouro: String = "",
    @SerializedName("complemento")
    val complemento: String = "",
    @SerializedName("bairro")
    val bairro: String = "",
    @SerializedName("localidade")
    val localidade: String = "",
    @SerializedName("uf")
    val uf: String = "",
    @SerializedName("ibge")
    val ibge: String = "",
    // Campo de erro da API
    @SerializedName("erro")
    val erro: Boolean = false
)