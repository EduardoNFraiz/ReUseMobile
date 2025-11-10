package com.projetointegrador.reuse.ui.pesquisar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedSearchViewModel : ViewModel() {

    // O LiveData armazena a última string de pesquisa e notifica os observadores.
    // Inicializamos com "" para garantir que a listagem inicial ocorra.
    private val _searchText = MutableLiveData<String>("")

    // Apenas leitura (imutável) para consumo pelos Fragments
    val searchText: LiveData<String> = _searchText

    /**
     * Chamado pelo PesquisaFragment (o remetente) para atualizar o texto.
     */
    fun updateSearchText(newText: String) {
        // Usa .value para setar o valor no thread principal (UI thread)
        _searchText.value = newText
    }
}