package com.projetointegrador.reuse.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.databinding.CardviewGavetaBinding
import com.projetointegrador.reuse.ui.adapter.GavetaAdapter.MyViewHolder

class GavetaAdapter (
    private val gavetaList: List<Gaveta>
): RecyclerView.Adapter<GavetaAdapter.MyViewHolder> () {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewGavetaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }
    override fun getItemCount() = gavetaList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val gavetaImage = gavetaList[position]
        val gavetaName = gavetaList[position]
        val gavetaNum = gavetaList[position]

        holder.binding.drawerImage.setImageResource(gavetaImage.image)
        holder.binding.drawerName.text = gavetaName.name
        holder.binding.itemCount.text = gavetaNum.number
        holder.binding.bttThreePoint.setOnClickListener { view ->
            val navController = view.findNavController()
            navController.navigate(R.id.action_historicoFragment_to_adicionarAvaliacaoFragment)
        }
        holder.binding.gaveta.setOnClickListener { view ->
            val navController = view.findNavController()
            navController.navigate(R.id.action_historicoFragment_to_adicionarAvaliacaoFragment)
        }
    }
    inner class MyViewHolder(val binding : CardviewGavetaBinding): RecyclerView.ViewHolder(binding.root){

    }
}