package com.projetointegrador.reuse.ui.avaliacao

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentAdicionarAvaliacaoBinding
import com.projetointegrador.reuse.util.initToolbar

class AdicionarAvaliacaoFragment : Fragment() {
    private var _binding: FragmentAdicionarAvaliacaoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionarAvaliacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners(){
        findNavController().navigate(R.id.action_adicionarAvaliacaoFragment_to_historicoFragment)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}