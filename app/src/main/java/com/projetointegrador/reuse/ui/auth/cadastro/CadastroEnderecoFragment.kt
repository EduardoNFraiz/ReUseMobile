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
import com.projetointegrador.reuse.util.MaskEditUtil // Importar para usar a máscara de CEP
import java.util.regex.Pattern

class CadastroEnderecoFragment : Fragment() {
    private var _binding: FragmentCadastroEnderecoBinding? = null
    private val binding get() = _binding!!

    private val args: CadastroEnderecoFragmentArgs by navArgs()
    private lateinit var endereco: Endereco

    // Expressões Regulares
    private val CEP_PATTERN = Pattern.compile("^\\d{5}-\\d{3}$")
    private val NUMERO_PATTERN = Pattern.compile("^\\d{1,5}$") // Mínimo 1, Máximo 5 dígitos

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
        // Aplicar máscara de CEP
        binding.editTextCep.addTextChangedListener(MaskEditUtil.mask(binding.editTextCep, MaskEditUtil.FORMAT_CEP))

        binding.bttProximo.setOnClickListener {
            valideData()
        }
    }

    private fun valideData() {
        val cep = binding.editTextCep.text.toString().trim()
        val rua = binding.editTextRua.text.toString().trim()
        val numero = binding.editTextNmrrua.text.toString().trim()
        val complemento = binding.editTextComplemento.text.toString().trim() // Opcional, não precisa de trim
        val bairro = binding.editTextBairro.text.toString().trim()
        val cidade = binding.editTextCidade.text.toString().trim()
        val estado = binding.editTextEstado.text.toString().trim()
        val pais = binding.editTextPais.text.toString().trim()

        // 1. Campos obrigatórios
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

        // 2. Validação de Formato

        // CEP: Formato xxxxx-xxx
        if (!CEP_PATTERN.matcher(cep).matches()) {
            Toast.makeText(requireContext(), "CEP inválido. Formato esperado: xxxxx-xxx", Toast.LENGTH_SHORT).show()
            return
        }

        // Número: Máximo 5 dígitos
        if (!NUMERO_PATTERN.matcher(numero).matches()) {
            Toast.makeText(requireContext(), "Número inválido. Deve conter no máximo 5 dígitos numéricos.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Criação do Objeto Endereco
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

        // 4. Navegação para o próximo fragmento
        val contaPessoaFisica = args.contaPessoaFisica
        val contaPessoaJuridica = args.contaPessoaJuridica

        val action = CadastroEnderecoFragmentDirections.actionCadastroEnderecoFragmentToAddFotoperfilFragment(contaPessoaFisica, contaPessoaJuridica, endereco)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}