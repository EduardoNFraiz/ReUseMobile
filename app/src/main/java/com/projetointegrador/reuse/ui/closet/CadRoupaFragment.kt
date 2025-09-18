package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentCadRoupaBinding

class CadRoupaFragment : Fragment() {
    private var _binding: FragmentCadRoupaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadRoupaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        modoEditor()

        val hideButtons = arguments?.getBoolean("HIDE_EDIT_BUTTONS") ?: false
        if (hideButtons) {
            binding.buttonEditar.visibility = View.INVISIBLE
            binding.Proximo.isEnabled = true
            binding.categoria.isEnabled = true
            binding.Tamanho.isEnabled = true
        }
        barraDeNavegacao()
    }
    private fun modoEditor(){
        binding.buttonEditar.setOnClickListener {
            binding.buttonEditar.visibility = View.VISIBLE
            binding.Proximo.isEnabled = true
            binding.categoria.isEnabled = true
            binding.Tamanho.isEnabled = true
        }
        binding.buttonEditar.setOnClickListener {
            binding.Proximo.isEnabled = false
            binding.categoria.isEnabled = false
            binding.Tamanho.isEnabled = false
            findNavController().navigate(R.id.closet)
        }
    }
    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.pesquisar.setOnClickListener {
            findNavController().navigate(R.id.pesquisar)
        }
        binding.cadastrarRoupa.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.doacao.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.perfil.setOnClickListener {
            findNavController().navigate(R.id.perfil)
        }
    }

    private fun initListeners(){
        binding.Proximo.setOnClickListener {
            findNavController().navigate(R.id.action_cadRoupaFragment_to_cadRoupa2Fragment)
        }
    }
}