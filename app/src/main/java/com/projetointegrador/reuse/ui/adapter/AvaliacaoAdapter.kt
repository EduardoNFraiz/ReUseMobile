package com.projetointegrador.reuse.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.data.model.Avaliacao
import com.projetointegrador.reuse.databinding.CardviewAvaliacaoBinding
import com.projetointegrador.reuse.ui.adapter.AvaliacaoAdapter.MyViewHolder

class AvaliacaoAdapter (
    private val avaliacaoList: List<Avaliacao>
    ): RecyclerView.Adapter<AvaliacaoAdapter.MyViewHolder> () {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewAvaliacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }
    override fun getItemCount() = avaliacaoList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val taskImage = avaliacaoList[position]
        val taskName = avaliacaoList[position]
        val taskUsername = avaliacaoList[position]
        val taskRating = avaliacaoList[position]

        holder.binding.imgPerfil.setImageResource(taskImage.image)
        holder.binding.txtNome.text = taskName.name
        holder.binding.txtComentario.text = taskUsername.description
        holder.binding.ratingBar.rating = taskRating.rating
    }

    inner class MyViewHolder(val binding : CardviewAvaliacaoBinding): RecyclerView.ViewHolder(binding.root){

    }
}