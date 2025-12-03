package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCarrinho
import com.projetointegrador.reuse.databinding.CardviewPecaBinding
import com.projetointegrador.reuse.ui.closet.GavetaFragmentDirections
import com.projetointegrador.reuse.util.displayBase64Image

// O adaptador PecaCarrinhoAdapter permanece inalterado em sua estrutura
class PecaCarrinhoAdapter (
    private var pecas: List<Pair<PecaCarrinho, String>>,
) : RecyclerView.Adapter<PecaCarrinhoAdapter.PecaCarrinhoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PecaCarrinhoViewHolder {
        val view = CardviewPecaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PecaCarrinhoViewHolder(view)
    }

    override fun getItemCount() = pecas.size

    override fun onBindViewHolder(holder: PecaCarrinhoViewHolder, position: Int) {
        holder.bind(pecas[position])
    }

    inner class PecaCarrinhoViewHolder(val binding: CardviewPecaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pecaPair: Pair<PecaCarrinho, String>) {
            val (peca, uidCopia) = pecaPair
            val context = binding.root.context

            // --- Bind dos Dados ---
            if (!peca.fotoBase64.isNullOrEmpty()) {
                displayBase64Image(peca.fotoBase64!!, binding.imagePeca)
            } else {
                binding.imagePeca.setImageResource(R.drawable.closeticon)
            }
            binding.itemTitle.text = peca.titulo ?: "Item sem título"
            binding.itemPrice.text = if (!peca.preco.isNullOrEmpty()) "${peca.preco}" else "R$0,00"

            // 🛑 Configura o clique para a navegação
            binding.root.setOnClickListener {
                val pecaOriginalUid = peca.pecaOriginalUid

                if (pecaOriginalUid != null) {
                    try {
                        val action = GavetaFragmentDirections.actionGavetaFragmentToComprarPecaFragment(pecaOriginalUid)
                        binding.root.findNavController().navigate(action)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro de navegação: Ação para ComprarPeca não encontrada.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Erro: UID da peça original não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- Método de atualização da lista ---
    fun updateList(newList: List<Pair<PecaCarrinho, String>>) {
        this.pecas = newList
        notifyDataSetChanged()
    }
}