package com.projetointegrador.reuse.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentEscolherPerfilCBinding
import com.projetointegrador.reuse.util.initToolbar

class EscolherPerfilCFragment : Fragment() {
    private var _binding: FragmentEscolherPerfilCBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEscolherPerfilCBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners(){
        binding.bttContaUsuario.setOnClickListener {
            findNavController().navigate(R.id.action_escolherPerfilCFragment_to_cadastroUsuarioFragment)
        }
        binding.bttContaBrecho.setOnClickListener {
            findNavController().navigate(R.id.action_escolherPerfilCFragment_to_cadatroBrechoFragment)
        }
        binding.bttContaInstituicao.setOnClickListener {
            findNavController().navigate(R.id.action_escolherPerfilCFragment_to_cadastroInstituicaoFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}