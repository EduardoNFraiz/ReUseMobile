package com.projetointegrador.reuse.ui.doacao

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Peca
import com.projetointegrador.reuse.databinding.FragmentRealizarDoacaoBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter
import com.projetointegrador.reuse.util.initToolbar

class RealizarDoacaoFragment : Fragment() {
    private var _binding: FragmentRealizarDoacaoBinding? = null
    private val binding get() = _binding!!
    private lateinit var pecaAdapter: PecaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealizarDoacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
        initRecyclerViewTask(getPeca())
    }

    private fun initRecyclerViewTask(pecaList: List<Peca>){
        pecaAdapter = PecaAdapter(pecaList)
        binding.recyclerViewTask.setHasFixedSize(true)
        binding. recyclerViewTask.adapter = pecaAdapter
    }

    private fun initListeners(){
        binding.btnDoacao.setOnClickListener {
            findNavController().navigate(R.id.action_realizarDoacaoFragment_to_confirmDoacaoFragment)
        }
    }

    private fun getPeca() = listOf(
        Peca(R.drawable.person),
        Peca(R.drawable.baseline_arrow_circle_right_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.person),
        Peca(R.drawable.baseline_arrow_circle_right_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.baseline_image_24),
        Peca(R.drawable.baseline_image_24),
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}