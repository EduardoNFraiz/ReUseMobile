package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Historico
import com.projetointegrador.reuse.databinding.FragmentHistoricoBinding
import com.projetointegrador.reuse.ui.adapter.HistoricoAdapter
import com.projetointegrador.reuse.util.initToolbar

class HistoricoFragment : Fragment() {
    private var _binding: FragmentHistoricoBinding? = null
    private val binding get() = _binding!!
    private lateinit var HistoricoAdapter: HistoricoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoricoBinding.inflate(inflater, container, false)
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

    private fun initRecyclerViewTask(taskList: List<Historico>){
        HistoricoAdapter = HistoricoAdapter(taskList)
        binding.recyclerViewAvaliacao.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAvaliacao.setHasFixedSize(true)
        binding. recyclerViewAvaliacao.adapter = HistoricoAdapter
    }

    private fun getTask() = listOf(
        Historico(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 1.0F),
        Historico(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 2.0F),
        Historico(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 3.0F),
        Historico(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 4.0F),
        Historico(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 5.0F),
        Historico(R.drawable.avatar_background, "Thiago", "Comprei 4 pares de meia do @eduardo_neumann e veio exatamente como estava no anúncio. Valeu o dinheiro gasto.", 2.0F),

        )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}