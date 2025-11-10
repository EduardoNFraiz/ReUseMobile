package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.databinding.CardviewPerfilBinding
import com.projetointegrador.reuse.util.displayBase64Image

class TaskAdapter(
    // 🛑 1. CORREÇÃO: Mude de List<Task> para MutableList<Task>
    private val taskList: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.MyViewHolder>() {

    // 🛑 2. ADICIONE: Método para atualizar a lista após a pesquisa
    fun updateList(newList: List<Task>) {
        // Para evitar bugs de animação e manter a eficiência,
        // é melhor usar DiffUtil se a lista for muito grande,
        // mas para pesquisa simples, essa abordagem funciona.
        taskList.clear()
        taskList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewPerfilBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = taskList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val task = taskList[position]

        // 1. NOME COMPLETO E NOME DE USUÁRIO
        holder.binding.textViewName.text = task.nomeCompleto
        // 2. MELHORIA: Garante que o '@' esteja sempre presente (se não for passado no nomeDeUsuario)
        holder.binding.textViewUsername.text = "@${task.nomeDeUsuario}"

        // 3. RATING
        holder.binding.ratingBar.rating = task.rating

        // 4. IMAGEM (Usando fotoBase64)
        if (!task.fotoBase64.isNullOrEmpty()) {
            // Usa a função utilitária
            displayBase64Image(task.fotoBase64!!, holder.binding.imageProfile)
        } else {
            // Se não houver foto, usa um placeholder (assumindo R.drawable.person é seu placeholder)
            holder.binding.imageProfile.setImageResource(R.drawable.person)
        }
    }

    inner class MyViewHolder(val binding: CardviewPerfilBinding) : RecyclerView.ViewHolder(binding.root)
}