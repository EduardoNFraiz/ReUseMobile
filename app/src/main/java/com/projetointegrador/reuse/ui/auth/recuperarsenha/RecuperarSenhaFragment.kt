package com.projetointegrador.reuse.ui.auth.recuperarsenha

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.projetointegrador.reuse.databinding.FragmentRecuperarSenhaBinding
import com.projetointegrador.reuse.util.initToolbar

class RecuperarSenhaFragment : Fragment() {
    private var _binding: FragmentRecuperarSenhaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecuperarSenhaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}