package com.projetointegrador.reuse.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.databinding.CardviewPecaclosetBinding
import androidx.navigation.findNavController

class PecaClosetAdapter(
    // 1. Recebe a lista de pares (PecaCloset, UID)
    private val pecaclosetList: List<Pair<PecaCloset, String>>,
    // 2. Adiciona o listener de clique que retorna APENAS o UID (String)
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PecaClosetAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewPecaclosetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = pecaclosetList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // 3. Desempacota o par: O objeto PecaCloset e o UID
        val (pecacloset, uid) = pecaclosetList[position]

        // --- Bind dos Dados ---

        // Assumindo que o campo 'image' é um ID de recurso drawable (Int)
        // Se a imagem vier de um Base64, a lógica de 'displayBase64Image' deve ser adicionada aqui.
        holder.binding.imagePeca.setImageResource(pecacloset.image)
        holder.binding.itemTitle.text = pecacloset.descricao
        holder.binding.itemPrice.text = pecacloset.preco

        // --- Tratamento de Cliques ---

        holder.binding.cardViewItem.setOnClickListener { view ->
            // 4. Em vez de navegar diretamente, chama a função de clique,
            //    passando o UID para o GavetaFragment gerenciar a navegação.
            onClick(uid)

            // Removido a navegação direta, pois ela é gerenciada pelo GavetaFragment
            /*
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putBoolean("VISUALIZAR_INFO", true)
            }
            navController.navigate(R.id.action_gavetaFragment_to_cadRoupaFragment,bundle)
            */
        }
    }

    inner class MyViewHolder(val binding: CardviewPecaclosetBinding) :
        RecyclerView.ViewHolder(binding.root)
}