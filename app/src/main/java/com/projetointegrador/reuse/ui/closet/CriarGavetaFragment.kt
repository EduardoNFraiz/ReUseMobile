package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentCriarGavetaBinding
import com.projetointegrador.reuse.util.initToolbar


class CriarGavetaFragment : Fragment() {
    private var _binding: FragmentCriarGavetaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriarGavetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
        modoEditor()
        initToolbar(binding.toolbar)

        val vizualizarInfo = arguments?.getBoolean("VISUALIZAR_INFO") ?: false
        if (vizualizarInfo) {
            binding.bttEditar.visibility = View.VISIBLE
            binding.bttCriarGaveta.visibility = View.GONE
            binding.editTextGaveta.isEnabled = false
            binding.rbPrivado.isEnabled = false
            binding.rbPublico.isEnabled = false
        }
        else {
            binding.bttEditar.visibility = View.GONE
            binding.bttCriarGaveta.visibility = View.VISIBLE
            binding.editTextGaveta.isEnabled = true
            binding.rbPrivado.isEnabled = true
            binding.rbPublico.isEnabled = true
        }
    }

    private fun initListeners() {
        binding.bttCriarGaveta.setOnClickListener {
            findNavController().navigate(R.id.action_criarGavetaFragment_to_gavetaFragment)
        }
    }

    private fun modoEditor(){
        var editando = false
        binding.bttEditar.setOnClickListener {
            editando = !editando
            val isEnabled = editando

            binding.editTextGaveta.isEnabled = isEnabled
            binding.rbPrivado.isEnabled = isEnabled
            binding.rbPublico.isEnabled = isEnabled

            if(isEnabled) {
                binding.bttSalvar.visibility = View.VISIBLE
            }
            else{
                binding.bttSalvar.visibility = View.INVISIBLE
            }
        }
        binding.bttSalvar.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}