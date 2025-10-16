package com.projetointegrador.reuse.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Historico
import com.projetointegrador.reuse.databinding.CardviewTransacaoBinding
import com.projetointegrador.reuse.ui.adapter.HistoricoAdapter.MyViewHolder

class HistoricoAdapter (
    private val historicoList: List<Historico>
): RecyclerView.Adapter<HistoricoAdapter.MyViewHolder> () {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewTransacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }
    override fun getItemCount() = historicoList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val historicoImage = historicoList[position]
        val historicoName = historicoList[position]
        val historicoUsername = historicoList[position]
        val historicoBtn = historicoList[position]

        holder.binding.imgProduto.setImageResource(historicoImage.image)
        holder.binding.tvNomeProduto.text = historicoName.name
        holder.binding.tvDescricao.text = historicoUsername.description
        holder.binding.bttAdd.visibility = if (historicoBtn.button) View.VISIBLE else View.GONE
        holder.binding.bttAdd.setOnClickListener { view ->
            val navController = view.findNavController()
            navController.navigate(R.id.action_historicoFragment_to_adicionarAvaliacaoFragment)
        }
    }

    inner class MyViewHolder(val binding : CardviewTransacaoBinding): RecyclerView.ViewHolder(binding.root){

    }
}