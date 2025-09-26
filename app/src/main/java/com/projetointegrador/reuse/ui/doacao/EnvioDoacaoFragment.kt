package com.projetointegrador.reuse.ui.doacao

import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentEnvioDoacaoBinding
import com.projetointegrador.reuse.util.initToolbar


class EnvioDoacaoFragment : Fragment() {
    private var _binding: FragmentEnvioDoacaoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnvioDoacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()

        binding.radioGroupOpcoes.setOnCheckedChangeListener { group, checkedId ->
            for (i in 0 until group.childCount) {
                val radio = group.getChildAt(i) as RadioButton
                if (radio.id == checkedId) {
                    radio.setTypeface(null, Typeface.BOLD)   // selecionado → negrito
                } else {
                    radio.setTypeface(null, Typeface.NORMAL) // não selecionado → normal
                }
            }
        }
    }

    private fun initListeners(){

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}