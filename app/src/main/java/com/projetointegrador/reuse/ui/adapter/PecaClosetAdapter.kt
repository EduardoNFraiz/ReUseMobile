package com.projetointegrador.reuse.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil // Import necessário
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.databinding.CardviewPecaclosetBinding
import com.projetointegrador.reuse.util.displayBase64Image

class PecaClosetAdapter (
    private var pecas: List<Pair<PecaCloset, String>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PecaClosetAdapter.PecaClosetViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PecaClosetViewHolder {
        val view = CardviewPecaclosetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PecaClosetViewHolder(view)
    }

    override fun getItemCount() = pecas.size

    override fun onBindViewHolder(holder: PecaClosetViewHolder, position: Int) {
        Log.d("PecaClosetAdapter", "Bind item: ${pecas[position].second}")
        holder.bind(pecas[position])
    }

    inner class PecaClosetViewHolder(val binding : CardviewPecaclosetBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(pecaPair: Pair<PecaCloset, String>) {
            val (pecacloset, uid) = pecaPair

            // --- 1. Lógica de Imagem ---
            if (!pecacloset.fotoBase64.isNullOrEmpty()) {
                displayBase64Image(pecacloset.fotoBase64!!, binding.imagePeca)
            } else {
                binding.imagePeca.setImageResource(R.drawable.baseline_image_24)
            }

            // --- 2. Binding dos Textos ---
            binding.itemTitle.text = pecacloset.titulo ?: "Sem Título"
            binding.itemPrice.text = pecacloset.preco ?: "R$ 0,00"

            // --- 3. Listener de Clique ---
            binding.root.setOnClickListener {
                onClick(uid)
            }
        }
    }


    // ✅ Implementação do DiffUtil para substituir notifyDataSetChanged()
    fun updateList(newList: List<Pair<PecaCloset, String>>) {
        val diffCallback = PecaClosetDiffCallback(this.pecas, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.pecas = newList // Atualiza a lista interna
        diffResult.dispatchUpdatesTo(this) // Aplica as atualizações de forma eficiente
    }

    // ✅ CLASSE INTERNA PARA CÁLCULO DE DIFERENÇAS (DEVE ESTAR DENTRO DO ARQUIVO)
    class PecaClosetDiffCallback(
        private val oldList: List<Pair<PecaCloset, String>>,
        private val newList: List<Pair<PecaCloset, String>>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        // Verifica se são o mesmo item (usando o UID da peça)
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Compara o UID (segundo elemento do Pair)
            return oldList[oldItemPosition].second == newList[newItemPosition].second
        }

        // Verifica se o conteúdo do item é o mesmo (se a peça foi modificada)
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Compara o objeto PecaCloset (primeiro elemento do Pair)
            return oldList[oldItemPosition].first == newList[newItemPosition].first
        }
    }
}