package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Avaliacao
import com.projetointegrador.reuse.databinding.CardviewAvaliacaoBinding
import com.projetointegrador.reuse.util.displayBase64Image

class AvaliacaoAdapter (
    // 🛑 Mudar para var e Mutavel se for fazer o updateList
    private var avaliacaoList: List<Avaliacao>
): RecyclerView.Adapter<AvaliacaoAdapter.MyViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewAvaliacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = avaliacaoList.size

    // 🛑 Adicionando método para atualizar a lista
    fun updateList(newList: List<Avaliacao>) {
        this.avaliacaoList = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = avaliacaoList[position]

        // 1. Carregamento da Imagem Base64
        if (!item.fotoBase64.isNullOrEmpty()) {
            displayBase64Image(item.fotoBase64!!, holder.binding.imgPerfil)
        } else {
            // Defina um drawable padrão adequado
            holder.binding.imgPerfil.setImageResource(R.drawable.exemplo)
        }

        // 2. Definir textos e rating
        holder.binding.txtNome.text = item.name
        holder.binding.txtComentario.text = item.description
        holder.binding.ratingBar.rating = item.rating
    }

    inner class MyViewHolder(val binding : CardviewAvaliacaoBinding): RecyclerView.ViewHolder(binding.root){

    }
}