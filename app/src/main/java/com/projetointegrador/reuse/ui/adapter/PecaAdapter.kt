package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro // 🛑 MODELO CORRIGIDO
import com.projetointegrador.reuse.databinding.CardviewPecaBinding // Seu layout do item
import com.projetointegrador.reuse.util.displayBase64Image // Função de utilidade para Base64 (MANTIDA)

// 🛑 A LISTA AGORA ACEITA PecaCadastro
class PecaAdapter (
    private var pecas: List<Pair<PecaCadastro, String>>,
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

        // 🛑 RECEBE Pair<PecaCadastro, String>
        fun bind(pecaPair: Pair<PecaCadastro, String>) {
            val (peca, uid) = pecaPair

            // --- Bind dos Dados Usando os Nomes Corretos dos Atributos ---

            // 1. Imagem: Usa fotoBase64
            if (!peca.fotoBase64.isNullOrEmpty()) {
                // Utiliza a função utilitária para exibir o Base64
                displayBase64Image(peca.fotoBase64!!, binding.imagePeca)
            } else {
                // Se a imagem for nula, define uma imagem padrão (ou a remove)
                binding.imagePeca.setImageResource(R.drawable.closeticon)
            }

            // 2. Título: Usa o campo 'titulo'
            binding.itemTitle.text = peca.titulo ?: "Item sem título"

            // 3. Preço: Usa o campo 'preco'
            // Adiciona R$ se o valor não estiver vazio, senão mostra 0.00
            binding.itemPrice.text = if (!peca.preco.isNullOrEmpty()) "R$${peca.preco}" else "R$0,00"

            // 4. Configura o clique, passando o UID
            binding.root.setOnClickListener {
                onClick(uid)
            }
        }
    }

    // --- Método CRUCIAL para atualização assíncrona ---

    /**
     * Atualiza a lista de peças no adaptador e notifica o RecyclerView para redesenhar.
     * Agora aceita List<Pair<PecaCadastro, String>>
     */
    fun updateList(newList: List<Pair<PecaCadastro, String>>) {
        this.pecas = newList // Substitui a lista de dados
        notifyDataSetChanged() // ESSENCIAL: Força o RecyclerView a redesenhar
    }
}