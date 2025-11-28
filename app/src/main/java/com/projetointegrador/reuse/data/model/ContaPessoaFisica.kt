package com.projetointegrador.reuse.data.model
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ContaPessoaFisica(
    var nomeCompleto: String= "",
    var nomeDeUsuario: String = "",
    var email: String = "",
    var telefone: String = "",
    var dataNascimento: String = "",
    var cpf: String = "",
    var endereco: String = "",
    var dataCadastro: String = "",
    var tipoPessoa: String = "",
    var tipoUsuario: String = "",
    var fotoBase64: String? = null,
):Parcelable