package com.projetointegrador.reuse.ui.avaliacao

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Avaliacao
import com.projetointegrador.reuse.databinding.FragmentAvaliacoesBinding
import com.projetointegrador.reuse.ui.adapter.AvaliacaoAdapter
import com.projetointegrador.reuse.util.initToolbar

class AvaliacoesFragment : Fragment() {
    private var _binding: FragmentAvaliacoesBinding? = null
    private val binding get() = _binding!!
    private lateinit var AvaliacaoAdapter: AvaliacaoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvaliacoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initToolbar(binding.toolbar)
        initRecyclerViewTask(getTask())
    }
    private fun initListeners() {

    }

    private fun initRecyclerViewTask(taskList: List<Avaliacao>){
        AvaliacaoAdapter = AvaliacaoAdapter(taskList)
        binding.recyclerViewAvaliacao.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAvaliacao.setHasFixedSize(true)
        binding. recyclerViewAvaliacao.adapter = AvaliacaoAdapter
    }

    private fun getTask() = listOf(
        Avaliacao(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 1.0F),
        Avaliacao(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 2.0F),
        Avaliacao(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 3.0F),
        Avaliacao(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 4.0F),
        Avaliacao(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 5.0F),
        Avaliacao(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 2.0F),

        )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}