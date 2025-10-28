package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ContaPessoaJuridica(
    var nomeCompleto: String,
    var nomeDeUsuario: String,
    var email: String,
    var telefone: String,
    var cnpj: String,
    var endereço: String,
    var dataCadastro: String,
    var tipoPessoa: String,
    var tipoUsuario: String,
    var fotoBase64: String? = null,
):Parcelable
