package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.databinding.CardviewPerfilBinding
import com.projetointegrador.reuse.util.displayBase64Image

class TaskAdapter(
    private val taskList: MutableList<Task>,
    // Interface de clique: aceita uma String (o UID)
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<TaskAdapter.MyViewHolder>() {

    fun updateList(newList: List<Task>) {
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
        holder.binding.textViewUsername.text = "@${task.nomeDeUsuario}"

        // 2. RATING
        holder.binding.ratingBar.rating = task.rating

        // 3. IMAGEM
        if (!task.fotoBase64.isNullOrEmpty()) {
            displayBase64Image(task.fotoBase64!!, holder.binding.imageProfile)
        } else {
            holder.binding.imageProfile.setImageResource(R.drawable.person) // Placeholder
        }

        // 🛑 AJUSTE: Uso seguro do 'let' para lidar com o UID nullable (String?)
        holder.binding.root.setOnClickListener {
            task.uid?.let { uidNaoNulo ->
                // O 'let' só é executado se task.uid não for nulo.
                // 'uidNaoNulo' é tratado como String (não-null), resolvendo o erro de mutabilidade.
                onItemClicked(uidNaoNulo)
            }
        }
    }

    inner class MyViewHolder(val binding: CardviewPerfilBinding) : RecyclerView.ViewHolder(binding.root)
}