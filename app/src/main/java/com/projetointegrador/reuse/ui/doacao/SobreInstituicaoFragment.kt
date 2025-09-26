package com.projetointegrador.reuse.ui.doacao

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentSobreInstituicaoBinding
import com.projetointegrador.reuse.util.initToolbar

class SobreInstituicaoFragment : Fragment() {
    private var _binding: FragmentSobreInstituicaoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSobreInstituicaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners(){
        binding.btnDoacao.setOnClickListener {
            findNavController().navigate(R.id.action_sobreInstituicaoFragment_to_realizarDoacaoFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}