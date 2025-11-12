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

        // 🛑 Inicialmente, desabilita o botão, caso não tenha sido feito no XML.
        // Se estiver definido no XML, esta linha é redundante, mas garante a funcionalidade.
        binding.btnDoacao.isEnabled = false

        initListeners()

        binding.radioGroupOpcoes.setOnCheckedChangeListener { group, checkedId ->
            // 🛑 HABILITA O BOTÃO SEMPRE QUE QUALQUER OPÇÃO FOR SELECIONADA
            binding.btnDoacao.isEnabled = checkedId != -1

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
        binding.btnDoacao.setOnClickListener {
            // O botão só será clicado se estiver habilitado (ou seja, checkedId != -1)
            val selectedId = binding.radioGroupOpcoes.checkedRadioButtonId

            if (selectedId != -1) {
                // Aqui você pode pegar a opção selecionada se necessário (e.g., para salvar no banco)
                val selectedOption = view?.findViewById<RadioButton>(selectedId)?.text.toString()

                val bundle = Bundle().apply {
                    putBoolean("REALIZEI_DOACAO", true)
                }
                findNavController().navigate(R.id.doacao, bundle)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}