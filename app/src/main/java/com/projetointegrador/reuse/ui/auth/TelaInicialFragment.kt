package com.projetointegrador.reuse.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentTelaInicialBinding

class TelaInicialFragment : Fragment() {
    private var _binding: FragmentTelaInicialBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelaInicialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners(){
        binding.buttonLogin.setOnClickListener {
            findNavController().navigate(R.id.action_telaInicialFragment_to_loginFragment)
        }
        binding.buttonCadastro.setOnClickListener {
            findNavController().navigate(R.id.action_telaInicialFragment_to_escolherPerfilCFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}