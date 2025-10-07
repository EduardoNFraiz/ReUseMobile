package com.projetointegrador.reuse.ui.compra

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentComprarPecaBinding
import com.projetointegrador.reuse.databinding.FragmentConfirmarCompraBinding
import com.projetointegrador.reuse.util.initToolbar

class ConfirmarCompraFragment : Fragment() {
    private var _binding: FragmentConfirmarCompraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmarCompraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initToolbar(binding.toolbar)
    }

    private fun initListeners() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}