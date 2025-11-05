package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.FilterItem
import com.projetointegrador.reuse.data.model.Peca
import com.projetointegrador.reuse.data.model.toChip
import com.projetointegrador.reuse.databinding.FragmentPesquisaVendasBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter

class PesquisaVendasFragment : Fragment(){
    private var _binding: FragmentPesquisaVendasBinding? = null
    private val binding get() = _binding!!
    private lateinit var pecaAdapter: PecaAdapter

    private var filter = arrayOf(
        FilterItem(1, "Cor", closeIcon = R.drawable.outline_arrow_drop_down_24 ),
        FilterItem(2, "Categoria", closeIcon = R.drawable.outline_arrow_drop_down_24),
        FilterItem(3, "Tamanho", closeIcon = R.drawable.outline_arrow_drop_down_24),
        FilterItem(4, "Faixa de preço", closeIcon = R.drawable.money)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesquisaVendasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.let {
            filter.forEach { filter ->
                it.chipGroupFilter.addView(filter.toChip(requireContext()))
            }
        }

        initListeners()
        //initRecyclerViewTask(getPeca())
    }

    private fun initListeners() {

    }

    //private fun initRecyclerViewTask(pecaList: List<Peca>){
        //pecaAdapter = PecaAdapter(pecaList)
        //binding.recyclerViewTask.setHasFixedSize(true)
        //binding. recyclerViewTask.adapter = pecaAdapter
    //}


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}