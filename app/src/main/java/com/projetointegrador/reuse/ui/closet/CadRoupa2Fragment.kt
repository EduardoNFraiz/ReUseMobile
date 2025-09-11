package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentCadRoupa2Binding


class CadRoupa2Fragment : Fragment() {
    private var _binding: FragmentCadRoupa2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadRoupa2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // aqui você acessa os componentes direto pelo binding
        val radioGroup = binding.Finalidade
        val spinner = binding.spinner
        val editPreco = binding.editEditText

        val isEditing = arguments?.getBoolean("isEditing", false) ?: false

        val btnCadastrarPeca = binding.btnCadastrarPeca
        val btnSalvarAlteracoes = binding.bttSalvar

        if (isEditing) {
            btnCadastrarPeca.visibility = View.GONE
            btnSalvarAlteracoes.visibility = View.VISIBLE
        } else {
            btnCadastrarPeca.visibility = View.VISIBLE
            btnSalvarAlteracoes.visibility = View.GONE
        }

        fun atualizarSpinner(opcoes: List<String>) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcoes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButton5 -> {
                    atualizarSpinner(listOf("Casacos", "Blusas", "Calças"))
                    editPreco.visibility = View.GONE
                    binding.textview6.visibility = View.GONE
                }
                R.id.radioButton6 -> {
                    atualizarSpinner(listOf("Doações"))
                    editPreco.visibility = View.GONE
                    binding.textview6.visibility = View.GONE
                }
                R.id.radioButton7 -> {
                    atualizarSpinner(listOf("Peças à venda"))
                    editPreco.visibility = View.VISIBLE
                    binding.textview6.visibility = View.VISIBLE
                }
            }
        }
        initListeners()
        barraDeNavegacao()
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
        binding.btnCadastrarPeca.setOnClickListener {
            findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}