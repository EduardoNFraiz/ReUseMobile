package com.projetointegrador.reuse.ui.auth.cadastro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.ContaPessoaJuridica
import com.projetointegrador.reuse.data.model.Endereco
import com.projetointegrador.reuse.databinding.FragmentCadastroEnderecoBinding
import com.projetointegrador.reuse.util.initToolbar

class CadastroEnderecoFragment : Fragment() {
    private var _binding: FragmentCadastroEnderecoBinding? = null
    private val binding get() = _binding!!

    private val args: CadastroEnderecoFragmentArgs by navArgs()
    private lateinit var endereco: Endereco

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastroEnderecoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners() {
        binding.bttProximo.setOnClickListener {
            valideData()
        }
    }

    private fun valideData() {
        val cep = binding.editTextCep.text.toString().trim()
        val rua = binding.editTextRua.text.toString().trim()
        val numero = binding.editTextNmrrua.text.toString().trim()
        val complemento = binding.editTextComplemento.text.toString().trim()
        val bairro = binding.editTextBairro.text.toString().trim()
        val cidade = binding.editTextCidade.text.toString().trim()
        val estado = binding.editTextEstado.text.toString().trim()
        val pais = binding.editTextPais.text.toString().trim()

        val camposObrigatorios = listOf(
            "CEP" to cep,
            "Rua" to rua,
            "Número" to numero,
            "Bairro" to bairro,
            "Cidade" to cidade,
            "Estado" to estado,
            "País" to pais
        )

        for ((nomeCampo, valorCampo) in camposObrigatorios) {
            if (valorCampo.isBlank()) {
                Toast.makeText(requireContext(), "Preencha o campo $nomeCampo!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        endereco = Endereco(
            cep = cep,
            rua = rua,
            numero = numero,
            complemento = complemento,
            bairro = bairro,
            cidade = cidade,
            estado = estado,
            pais = pais,
        )

        var contaPessoaFisica = args.contaPessoaFisica
        var contaPessoaJuridica = args.contaPessoaJuridica
        val action = CadastroEnderecoFragmentDirections.actionCadastroEnderecoFragmentToAddFotoperfilFragment(contaPessoaFisica,contaPessoaJuridica,endereco)
        findNavController().navigate(action)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}