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

        val hideButtons = arguments?.getBoolean("HIDE_EDIT_BUTTONS") ?: false
        if (hideButtons) {
            binding.bttEditar.visibility = View.INVISIBLE
            binding.bttEditar.visibility = View.INVISIBLE
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
        binding.bttEditar.setOnClickListener {
            binding.bttSalvar.visibility = View.VISIBLE
            binding.editTextGaveta.isEnabled = true
            binding.rbPrivado.isEnabled = true
            binding.rbPublico.isEnabled = true
        }
        binding.bttSalvar.setOnClickListener {
            binding.editTextGaveta.isEnabled = false
            binding.rbPrivado.isEnabled = false
            binding.rbPublico.isEnabled = false
            findNavController().navigate(R.id.closet)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}