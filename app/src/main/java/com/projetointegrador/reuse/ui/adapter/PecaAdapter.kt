package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.CardviewPecaBinding
import com.projetointegrador.reuse.util.displayBase64Image

// 🛑 A lista agora aceita PecaCadastro e o String é o UID
class PecaAdapter (
    private var pecas: List<Pair<PecaCadastro, String>>,
    private val onClick: (String) -> Unit // Usado para a navegação ou ação principal
) : RecyclerView.Adapter<PecaAdapter.PecaViewHolder> () {

    // 🛑 NOVO: Variável para rastrear o UID do item selecionado
    private var selectedPecaUid: String? = null

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
            binding.itemPrice.text = if (!peca.preco.isNullOrEmpty()) "R$${peca.preco}" else "R$0,00"

            // ----------------------------------------------------
            // 🛑 LÓGICA DE SELEÇÃO E MUDANÇA DE BACKGROUND 🛑
            // ----------------------------------------------------

            // 1. Verifica se esta peça é a selecionada
            val isSelected = uid == selectedPecaUid

            // 2. Define a cor do CardView com base no estado
            val backgroundColor = if (isSelected) {
                // Se selecionado, usa VerdeEscuro
                ContextCompat.getColor(context, R.color.VerdeEscuro)
            } else {
                // Se não selecionado, usa a cor padrão (Geralmente branco, ou use R.color.white)
                ContextCompat.getColor(context, R.color.white)
            }
            binding.cardViewItem.setCardBackgroundColor(backgroundColor)

            // 3. Opcional: Altera a cor do texto para melhor contraste
            val textColor = if (isSelected) {
                ContextCompat.getColor(context, R.color.white) // Ou outra cor clara
            } else {
                ContextCompat.getColor(context, android.R.color.black)
            }
            binding.itemTitle.setTextColor(textColor)
            binding.itemPrice.setTextColor(textColor)


            // 4. Configura o clique
            binding.root.setOnClickListener {
                // 4a. Atualiza o estado de seleção
                setSelected(uid)

                // 4b. Executa a ação original (navegação, se for o caso)
                onClick(uid)
            }
        }
    }

    /**
     * 🛑 NOVO MÉTODO: Atualiza o item selecionado e notifica o RecyclerView para redesenhar.
     * Isso garante que a cor do item anterior seja resetada.
     */
    fun setSelected(uid: String) {
        val previousSelectedUid = selectedPecaUid

        // Verifica se o item clicado já era o selecionado. Se sim, deseleciona.
        if (uid == previousSelectedUid) {
            selectedPecaUid = null
        } else {
            selectedPecaUid = uid
        }

        // Se havia um item selecionado anteriormente, notifica-o para rebind (resetar a cor).
        previousSelectedUid?.let { notifyItemChangedByUid(it) }

        // Notifica o novo item selecionado (ou o item deselecionado) para rebind (mudar a cor).
        selectedPecaUid?.let { notifyItemChangedByUid(it) } ?: notifyItemChangedByUid(uid)

    }

    /**
     * Auxiliar para encontrar a posição do item pelo UID.
     */
    private fun notifyItemChangedByUid(uid: String) {
        val index = pecas.indexOfFirst { it.second == uid }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    /**
     * Retorna o UID da peça atualmente selecionada (útil para a função "Próximo").
     */
    fun getSelectedPecaUid(): String? {
        return selectedPecaUid
    }

    // --- Método de atualização assíncrona (mantido) ---
    fun updateList(newList: List<Pair<PecaCadastro, String>>) {
        this.pecas = newList
        notifyDataSetChanged()
    }
}