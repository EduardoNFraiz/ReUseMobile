package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Peca // Seu modelo de dados
import com.projetointegrador.reuse.databinding.CardviewPecaBinding // Seu layout do item
import com.projetointegrador.reuse.util.displayBase64Image // Função de utilidade para Base64 (ESSENCIAL)

class PecaAdapter (
    // Lista de pares (Objeto Peca, UID)
    private var pecas: List<Pair<Peca, String>>,
    // Listener de clique que retorna o UID da peça
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PecaAdapter.PecaViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PecaViewHolder {
        val view = CardviewPecaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PecaViewHolder(view)
    }

    override fun getItemCount() = pecas.size

    override fun onBindViewHolder(holder: PecaViewHolder, position: Int) {
        holder.bind(pecas[position])
    }

    inner class PecaViewHolder(val binding : CardviewPecaBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(pecaPair: Pair<Peca, String>) {
            val (peca, uid) = pecaPair

            // --- Bind dos Dados Usando os Nomes Corretos dos Atributos ---

            // 1. Imagem: Usa fotoBase64
            if (!peca.fotoBase64.isNullOrEmpty()) {
                // Utiliza a função utilitária para exibir o Base64
                displayBase64Image(peca.fotoBase64!!, binding.imagePeca)
            }

            // 2. Título (Antiga 'descrição'): Usa o campo 'titulo'
            binding.itemTitle.text = peca.titulo

            // 3. Preço: Usa o campo 'preco'
            binding.itemPrice.text = peca.preco

            // 4. Configura o clique, passando o UID
            binding.root.setOnClickListener {
                onClick(uid)
            }
        }
    }

    // --- Método CRUCIAL para atualização assíncrona ---

    /**
     * Atualiza a lista de peças no adaptador e notifica o RecyclerView para redesenhar.
     * Isso resolve o problema de dados carregados após a inicialização do Fragment.
     */
    fun updateList(newList: List<Pair<Peca, String>>) {
        this.pecas = newList // Substitui a lista de dados
        notifyDataSetChanged() // ESSENCIAL: Força o RecyclerView a redesenhar
    }
}