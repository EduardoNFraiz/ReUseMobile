package com.projetointegrador.reuse.ui.adapter

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Base64
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.databinding.CardviewGavetaBinding
import androidx.navigation.findNavController
import android.widget.ImageView // Necessário para o parâmetro da função displayBase64Image

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

        val base64String = gaveta.fotoBase64

        if (!base64String.isNullOrEmpty()) {
            displayBase64Image(base64String, holder.binding.drawerImage)
        } else {
            holder.binding.drawerImage.setImageDrawable(null)
        }

        holder.binding.drawerName.text = gaveta.name
        holder.binding.itemCount.text = gaveta.number

        holder.binding.bttThreePoint.setOnClickListener { view ->
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putBoolean("VISUALIZAR_INFO", true)
            }
            navController.navigate(R.id.action_closetFragment_to_criarGavetaFragment, bundle)
        }

        holder.binding.gaveta.setOnClickListener { view ->
            val navController = view.findNavController()
            navController.navigate(R.id.action_closetFragment_to_gavetaFragment)
        }
    }

    inner class MyViewHolder(val binding: CardviewGavetaBinding) :
        RecyclerView.ViewHolder(binding.root)


    private fun displayBase64Image(base64String: String, imageView: ImageView) {
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(decodedBitmap)

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            imageView.setImageDrawable(null)
        }
    }
}