package com.projetointegrador.reuse.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.databinding.CardviewGavetaBinding
import androidx.navigation.findNavController

class GavetaAdapter(
    private val gavetaList: List<Gaveta>
) : RecyclerView.Adapter<GavetaAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewGavetaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = gavetaList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val gaveta = gavetaList[position]

        holder.binding.drawerImage.setImageResource(gaveta.image)
        holder.binding.drawerName.text = gaveta.name
        holder.binding.itemCount.text = gaveta.number

        holder.binding.bttThreePoint.setOnClickListener { view ->
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putBoolean("VISUALIZAR_INFO", true)
            }
            navController.navigate(R.id.action_closetFragment_to_criarGavetaFragment,bundle)
        }

        holder.binding.gaveta.setOnClickListener { view ->
            val navController = view.findNavController()
            navController.navigate(R.id.action_closetFragment_to_gavetaFragment)
        }
    }

    inner class MyViewHolder(val binding: CardviewGavetaBinding) :
        RecyclerView.ViewHolder(binding.root)
}
