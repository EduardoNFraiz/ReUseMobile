package com.projetointegrador.reuse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Instituicao
import com.projetointegrador.reuse.databinding.CardviewInstituicaoBinding
import com.projetointegrador.reuse.util.displayBase64Image // 🛑 IMPORT ESSENCIAL

class InstituicaoAdapter(
    private var instituicaoList: List<Instituicao>
): RecyclerView.Adapter<InstituicaoAdapter.MyViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = CardviewInstituicaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount() = instituicaoList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(instituicaoList[position])
    }

    inner class MyViewHolder(val binding: CardviewInstituicaoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(instituicao: Instituicao) {

            // 🛑 CARREGAMENTO DA IMAGEM BASE64
            if (!instituicao.fotoBase64.isNullOrEmpty()) {
                displayBase64Image(instituicao.fotoBase64!!, binding.imageProfile)
            } else {
                // Imagem Padrão (se não houver foto)
                binding.imageProfile.setImageResource(R.drawable.person)
            }

            binding.textViewName.text = instituicao.name
            binding.textViewDistancia.text = instituicao.distancia

            // Adicione um setOnClickListener aqui, se necessário, usando instituicao.uid
            binding.root.setOnClickListener {
                // Ação ao clicar na instituição
            }
        }
    }

    fun updateList(newList: List<Instituicao>) {
        this.instituicaoList = newList
        notifyDataSetChanged()
    }
}