package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.databinding.CardviewPerfilBinding
import com.projetointegrador.reuse.util.displayBase64Image // IMPORTANTE: Assumindo que esta função existe

class TaskAdapter(
    private val taskList: List<Task> // Agora recebe uma lista
) : RecyclerView.Adapter<TaskAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewPerfilBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = taskList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val task = taskList[position] // Obtém o objeto Task

        // 1. NOME COMPLETO E NOME DE USUÁRIO
        holder.binding.textViewName.text = task.nomeCompleto
        holder.binding.textViewUsername.text = "@${task.nomeDeUsuario}" // Adiciona o '@'

        // 2. RATING
        holder.binding.ratingBar.rating = task.rating

        // 3. IMAGEM (Usando fotoBase64)
        if (!task.fotoBase64.isNullOrEmpty()) {
            // Usa a função utilitária para carregar a imagem
            displayBase64Image(task.fotoBase64!!, holder.binding.imageProfile)
        } else {
            // Se não houver foto, usa um placeholder (assumindo R.drawable.person é seu placeholder)
            holder.binding.imageProfile.setImageResource(R.drawable.person)
        }
    }

    inner class MyViewHolder(val binding: CardviewPerfilBinding) : RecyclerView.ViewHolder(binding.root)
}