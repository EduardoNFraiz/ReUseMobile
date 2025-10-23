package com.projetointegrador.reuse.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.databinding.CardviewPecaclosetBinding
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController

class PecaClosetAdapter(
    private val pecaclosetList: List<PecaCloset>
) : RecyclerView.Adapter<PecaClosetAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewPecaclosetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = pecaclosetList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val pecacloset = pecaclosetList[position]

        holder.binding.imagePeca.setImageResource(pecacloset.image)
        holder.binding.itemTitle.text = pecacloset.descricao
        holder.binding.itemPrice.text = pecacloset.preco

        holder.binding.cardViewItem.setOnClickListener { view ->
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putBoolean("VISUALIZAR_INFO", true)
            }
            navController.navigate(R.id.action_gavetaFragment_to_cadRoupaFragment,bundle)
        }
    }

    inner class MyViewHolder(val binding: CardviewPecaclosetBinding) :
        RecyclerView.ViewHolder(binding.root)
}
