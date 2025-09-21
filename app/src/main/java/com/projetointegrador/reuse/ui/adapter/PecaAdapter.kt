package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.data.model.Peca
import com.projetointegrador.reuse.databinding.CardviewPecaBinding

class PecaAdapter (
    private val pecaList: List<Peca>

): RecyclerView.Adapter<PecaAdapter.MyViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewPecaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = pecaList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val pecaImage = pecaList[position]
        val pecaDescricao = pecaList[position]
        val pecaPreco = pecaList[position]

        holder.binding.imagePeca.setImageResource(pecaImage.image)
        holder.binding.itemTitle.text = pecaDescricao.descricao
        holder.binding.itemPrice.text = pecaPreco.preco
    }

    inner class MyViewHolder(val binding : CardviewPecaBinding): RecyclerView.ViewHolder(binding.root){

    }
}