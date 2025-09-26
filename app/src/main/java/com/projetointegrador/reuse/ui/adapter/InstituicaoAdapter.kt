package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.data.model.Instituicao
import com.projetointegrador.reuse.databinding.CardviewInstituicaoBinding

class InstituicaoAdapter(
    private val instituicaoList: List<Instituicao>
): RecyclerView.Adapter<InstituicaoAdapter.MyViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewInstituicaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = instituicaoList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val instituicaoImage = instituicaoList[position]
        val instituicaoName = instituicaoList[position]
        val instituicaoDistancia = instituicaoList[position]

        holder.binding.imageProfile.setImageResource(instituicaoImage.image)
        holder.binding.textViewName.text = instituicaoName.name
        holder.binding.textViewDistancia.text = instituicaoDistancia.distancia
    }

    inner class MyViewHolder(val binding: CardviewInstituicaoBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }
}