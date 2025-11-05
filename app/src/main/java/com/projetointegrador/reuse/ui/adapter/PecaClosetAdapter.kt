package com.projetointegrador.reuse.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.databinding.CardviewPecaclosetBinding // Assumindo que este é o layout do item
import com.projetointegrador.reuse.util.displayBase64Image // Importa a função utilitária com Glide

class PecaClosetAdapter (
    // Lista mutável que carrega pares: (Objeto PecaCloset, UID da Peça)
    private var pecas: List<Pair<PecaCloset, String>>,
    // Lambda para lidar com o clique (passa o UID da peça)
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

            // --- 1. Lógica de Imagem (Usando o Utilitário Otimizado com Glide) ---

            // Verifica se a string Base64 existe e a exibe
            if (!pecacloset.fotoBase64.isNullOrEmpty()) {
                // Chama a função utilitária. Ela agora usa o Glide,
                // resolvendo o problema de memória/quadrado preto.
                displayBase64Image(pecacloset.fotoBase64!!, binding.imagePeca)
            } else {
                // Placeholder padrão
                binding.imagePeca.setImageResource(R.drawable.baseline_image_24)
            }

            // --- 2. Binding dos Textos (Usando 'titulo' e 'preco') ---

            binding.itemTitle.text = pecacloset.titulo ?: "Sem Título"
            binding.itemPrice.text = pecacloset.preco ?: "R$ 0,00"

            // --- 3. Listener de Clique ---

            binding.root.setOnClickListener {
                onClick(uid) // Retorna o UID da peça clicada
            }
        }
    }


    fun updateList(newList: List<Pair<PecaCloset, String>>) {
        this.pecas = newList // Substitui a lista de dados
        notifyDataSetChanged() // Força o redesenho
    }
}