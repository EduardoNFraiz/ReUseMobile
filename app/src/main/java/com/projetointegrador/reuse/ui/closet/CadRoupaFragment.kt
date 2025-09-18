package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentCadRoupaBinding
import com.projetointegrador.reuse.util.initToolbar

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
            binding.roupa.isEnabled = true
            binding.acess.isEnabled = true
            binding.calcado.isEnabled = true
            binding.traje.isEnabled = true
            binding.esporte.isEnabled = true
            binding.masc.isEnabled = true
            binding.fem.isEnabled = true
            binding.infantil.isEnabled = true
            binding.intimo.isEnabled = true
            binding.uni.isEnabled = true
            binding.rbPP.isEnabled = true
            binding.rbP.isEnabled = true
            binding.rbM.isEnabled = true
            binding.rbG.isEnabled = true
            binding.rbGG.isEnabled = true
            binding.rbXGG.isEnabled = true
        }
        val info = arguments?.getBoolean("VISUALIZAR_INFO") ?: false
        if (info) {
            binding.roupa.isEnabled = false
            binding.acess.isEnabled = false
            binding.calcado.isEnabled = false
            binding.traje.isEnabled = false
            binding.esporte.isEnabled = false
            binding.masc.isEnabled = false
            binding.fem.isEnabled = false
            binding.infantil.isEnabled = false
            binding.intimo.isEnabled = false
            binding.uni.isEnabled = false
            binding.rbPP.isEnabled = false
            binding.rbP.isEnabled = false
            binding.rbM.isEnabled = false
            binding.rbG.isEnabled = false
            binding.rbGG.isEnabled = false
            binding.rbXGG.isEnabled = false
        }
        barraDeNavegacao()
        initToolbar(binding.toolbar)
    }
    private fun modoEditor() {
        var editando = false
        binding.buttonEditar.setOnClickListener {
            // Alterna o valor da variável editando
            editando = !editando

            // Habilita ou desabilita os componentes com base no estado de 'editando'
            val isEnabled = editando

            binding.roupa.isEnabled = isEnabled
            binding.acess.isEnabled = isEnabled
            binding.calcado.isEnabled = isEnabled
            binding.traje.isEnabled = isEnabled
            binding.esporte.isEnabled = isEnabled
            binding.masc.isEnabled = isEnabled
            binding.fem.isEnabled = isEnabled
            binding.infantil.isEnabled = isEnabled
            binding.intimo.isEnabled = isEnabled
            binding.uni.isEnabled = isEnabled
            binding.rbPP.isEnabled = isEnabled
            binding.rbP.isEnabled = isEnabled
            binding.rbM.isEnabled = isEnabled
            binding.rbG.isEnabled = isEnabled
            binding.rbGG.isEnabled = isEnabled
            binding.rbXGG.isEnabled = isEnabled
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