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
import android.widget.ImageView

class GavetaAdapter(
    // A lista de pares (Gaveta e UID)
    private var gavetaList: List<Pair<Gaveta, String>>,
    // 🛑 NOVO: Mapa de contagens de peças (UID da Gaveta -> Quantidade de Peças)
    private var pecaCountMap: Map<String, Int>,
    // O listener de clique passa o UID da gaveta (String)
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<GavetaAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewGavetaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = gavetaList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // Desempacota o par: Gaveta e UID
        val (gaveta, uid) = gavetaList[position]

        val base64String = gaveta.fotoBase64

        // --- Bind dos Dados ---

        if (!base64String.isNullOrEmpty()) {
            displayBase64Image(base64String, holder.binding.drawerImage)
        } else {
            holder.binding.drawerImage.setImageDrawable(null)
        }

        holder.binding.drawerName.text = gaveta.nome

        // 🛑 ATUALIZADO: Busca a contagem de peças no mapa usando o UID da gaveta
        val pecaCount = pecaCountMap[uid]?.toString() ?: "0"
        holder.binding.itemCount.text = pecaCount

        holder.binding.bttThreePoint.setOnClickListener { view ->
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putBoolean("VISUALIZAR_INFO", true)
                // É CRUCIAL PASSAR O UID DA GAVETA AQUI para o modo de EDIÇÃO/VISUALIZAÇÃO
                putString("GAVETA_ID", uid)
            }
            navController.navigate(R.id.action_closetFragment_to_criarGavetaFragment, bundle)
        }

        // Clique na Gaveta (Responsável pela navegação para GavetaFragment - listagem de peças)
        holder.binding.gaveta.setOnClickListener {
            // Chama o listener de clique, passando APENAS o UID (a String)
            onClick(uid)
        }
    }

    inner class MyViewHolder(val binding: CardviewGavetaBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * 🛑 ATUALIZADO: Função para atualizar a lista de gavetas E o mapa de contagens.
     */
    fun updateList(newList: List<Pair<Gaveta, String>>, newCountMap: Map<String, Int>) {
        this.gavetaList = newList
        this.pecaCountMap = newCountMap // Atualiza o mapa de contagens
        notifyDataSetChanged()
    }


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