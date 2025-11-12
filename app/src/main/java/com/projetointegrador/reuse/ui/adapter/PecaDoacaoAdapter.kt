package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.CardviewPecaBinding
import com.projetointegrador.reuse.util.displayBase64Image

// 🛑 CORRIGIDO: O tipo do Adapter deve ser PecaDoacaoAdapter.PecaViewHolder
class PecaDoacaoAdapter (
    private var pecas: List<Pair<PecaCadastro, String>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PecaDoacaoAdapter.PecaViewHolder> () { // <- Ajuste aqui

    // Rastreia múltiplos UIDs de peças selecionadas
    private val selectedPecaUids = mutableSetOf<String>()

    // 🛑 CORRIGIDO: O ViewHolder deve ser resolvido como PecaViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PecaViewHolder {
        val view = CardviewPecaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PecaViewHolder(view)
    }

    override fun getItemCount() = pecas.size

    override fun onBindViewHolder(holder: PecaViewHolder, position: Int) {
        holder.bind(pecas[position])
    }

    // 🛑 CORRIGIDO: A classe interna é PecaViewHolder e não mais PecaAdapter.PecaViewHolder
    inner class PecaViewHolder(val binding : CardviewPecaBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(pecaPair: Pair<PecaCadastro, String>) {
            val (peca, uid) = pecaPair
            val context = binding.root.context

            // --- Bind dos Dados ---
            if (!peca.fotoBase64.isNullOrEmpty()) {
                displayBase64Image(peca.fotoBase64!!, binding.imagePeca)
            } else {
                binding.imagePeca.setImageResource(R.drawable.closeticon)
            }

            // 🛑 LÓGICA DE SELEÇÃO E MUDANÇA DE BACKGROUND 🛑
            val isSelected = selectedPecaUids.contains(uid)
            // Define a cor do CardView
            val backgroundColor = if (isSelected) {
                ContextCompat.getColor(context, R.color.VerdeEscuro)
            } else {
                ContextCompat.getColor(context, R.color.white)
            }
            binding.cardViewItem.setCardBackgroundColor(backgroundColor)

            // Configura o clique
            binding.root.setOnClickListener {
                toggleSelection(uid)
                onClick(uid)
            }
        }
    }

    /**
     * Adiciona ou remove o UID da lista de seleção e notifica o item para redesenho.
     */
    private fun toggleSelection(uid: String) {
        val index = pecas.indexOfFirst { it.second == uid }
        if (index == -1) return

        if (selectedPecaUids.contains(uid)) {
            selectedPecaUids.remove(uid)
        } else {
            selectedPecaUids.add(uid)
        }
        notifyItemChanged(index)
    }

    /**
     * Retorna o Array de UIDs das peças atualmente selecionadas para passar via Safe Args.
     */
    fun getSelectedPecaUids(): Array<String> {
        return selectedPecaUids.toTypedArray()
    }

    /**
     * Atualiza a lista de peças.
     */
    fun updateList(newList: List<Pair<PecaCadastro, String>>) {
        this.pecas = newList
        notifyDataSetChanged()
    }
}