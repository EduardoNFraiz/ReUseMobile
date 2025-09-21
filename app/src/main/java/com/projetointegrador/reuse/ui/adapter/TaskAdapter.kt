package com.projetointegrador.reuse.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.databinding.CardviewPerfilBinding

class TaskAdapter(
    private val taskList: List<Task>

): RecyclerView.Adapter<TaskAdapter.MyViewHolder> () {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewPerfilBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = taskList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val taskImage = taskList[position]
        val taskName = taskList[position]
        val taskUsername = taskList[position]
        val taskRating = taskList[position]

        holder.binding.imageProfile.setImageResource(taskImage.image)
        holder.binding.textViewName.text = taskName.name
        holder.binding.textViewUsername.text = taskUsername.username
        holder.binding.ratingBar.rating = taskRating.rating
    }

    inner class MyViewHolder(val binding : CardviewPerfilBinding): RecyclerView.ViewHolder(binding.root){

    }
}