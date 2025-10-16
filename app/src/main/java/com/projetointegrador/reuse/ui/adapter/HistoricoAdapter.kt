package com.projetointegrador.reuse.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.data.model.Historico
import com.projetointegrador.reuse.databinding.CardviewHistoricoBinding
import com.projetointegrador.reuse.ui.adapter.HistoricoAdapter.MyViewHolder

class HistoricoAdapter (
    private val historicoList: List<HistoricoAdapter>
): RecyclerView.Adapter<HistoricoAdapter.MyViewHolder> () {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewHistoricoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }
    override fun getItemCount() = historicoList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val taskImage = historicoList[position]
        val taskName = historicoList[position]
        val taskUsername = historicoList[position]
        val taskRating = historicoList[position]

        holder.binding.imgProduto.setImageResource(taskImage.image)
        holder.binding.txtNome.text = taskName.name
        holder.binding.txtComentario.text = taskUsername.description
    }

    inner class MyViewHolder(val binding : CardviewHistoricoBinding): RecyclerView.ViewHolder(binding.root){

    }
}