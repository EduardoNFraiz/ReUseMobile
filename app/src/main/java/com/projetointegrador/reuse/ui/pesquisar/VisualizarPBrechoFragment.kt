package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.projetointegrador.reuse.databinding.FragmentVisualizarPBrechoBinding
import com.projetointegrador.reuse.util.initToolbar


class VisualizarPBrechoFragment : Fragment() {
    private var _binding: FragmentVisualizarPBrechoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisualizarPBrechoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners(){

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}