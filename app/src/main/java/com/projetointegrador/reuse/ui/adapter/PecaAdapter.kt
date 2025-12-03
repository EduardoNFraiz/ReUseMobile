package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.CardviewPecaBinding
import com.projetointegrador.reuse.util.displayBase64Image

class PecaAdapter (
    private var pecas: List<Pair<PecaCadastro, String>>,
    private val onClick: (String) -> Unit // Usado para a navegação ou ação principal
) : RecyclerView.Adapter<PecaAdapter.PecaViewHolder> () {

    // 🛑 Variável selectedPecaUid REMOVIDA

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PecaViewHolder {
        val view = CardviewPecaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PecaViewHolder(view)
    }

    override fun getItemCount() = pecas.size

    override fun onBindViewHolder(holder: PecaViewHolder, position: Int) {
        holder.bind(pecas[position])
    }

    inner class PecaViewHolder(val binding : CardviewPecaBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(pecaPair: Pair<PecaCadastro, String>) {
            val (peca, uid) = pecaPair
            val context = binding.root.context

            // --- Bind dos Dados (mantido) ---
            if (!peca.fotoBase64.isNullOrEmpty()) {
                // Utiliza a função utilitária para exibir o Base64
                displayBase64Image(peca.fotoBase64!!, binding.imagePeca)
            } else {
                binding.imagePeca.setImageResource(R.drawable.closeticon)
            }
            binding.itemTitle.text = peca.titulo ?: "Item sem título"
            binding.itemPrice.text = if (!peca.preco.isNullOrEmpty()) "${peca.preco}" else "R$0,00"

            // 🛑 GARANTE CORES PADRÃO NO RECYCLE
            binding.cardViewItem.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            binding.itemTitle.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            binding.itemPrice.setTextColor(ContextCompat.getColor(context, android.R.color.black))


            // 4. Configura o clique
            binding.root.setOnClickListener {
                // Executa APENAS a ação de clique principal
                onClick(uid)
            }
        }
    }

    // 🛑 Funções de gerenciamento de seleção (setSelected, notifyItemChangedByUid, getSelectedPecaUid) REMOVIDAS.

    // --- Método de atualização assíncrona (mantido) ---
    fun updateList(newList: List<Pair<PecaCadastro, String>>) {
        this.pecas = newList
        notifyDataSetChanged()
    }
}