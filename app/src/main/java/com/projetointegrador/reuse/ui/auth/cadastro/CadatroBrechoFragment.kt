package com.projetointegrador.reuse.ui.auth.cadastro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.ContaPessoaFisica
import com.projetointegrador.reuse.data.model.ContaPessoaJuridica
import com.projetointegrador.reuse.databinding.FragmentCadatroBrechoBinding
import com.projetointegrador.reuse.util.initToolbar
import kotlin.toString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class CadatroBrechoFragment : Fragment() {
    private var _binding: FragmentCadatroBrechoBinding? = null
    private val binding get() = _binding!!
    private lateinit var contaPessoaFisica: ContaPessoaFisica
    private lateinit var contaPessoaJuridica: ContaPessoaJuridica

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadatroBrechoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners() {
        binding.bttProximo.setOnClickListener{
            valideData()
        }
    }
    private fun gerarDataAtual(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val dataAtual = Date()

        return formatter.format(dataAtual)
    }

    private fun valideData() {
        val nomeFantasia = binding.editTextNome.text.toString().trim()
        val usuario = binding.editTextUsuario.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val senha = binding.editTextSenha.text.toString().trim()
        val confSenha = binding.editTextConfsenha.text.toString().trim()
        val telefone = binding.editTextTelefone.text.toString().trim()
        val cnpj = binding.editTextCpf.text.toString().trim()
        val endereco = ""
        val dataCadastro = gerarDataAtual()
        val tipoPessoa = "PessoaJuridica"
        val tipoUsuario = "brecho"

        val campos = listOf(
            "Nome da Loja" to nomeFantasia,
            "Usuário" to usuario,
            "E-mail" to email,
            "Senha" to senha,
            "Confirmação de Senha" to confSenha,
            "Telefone" to telefone,
            "CNPJ" to cnpj // Alterado para CNPJ
        )

        // 4. Verificar se todos os campos estão preenchidos
        for ((nomeCampo, valorCampo) in campos) {
            if (valorCampo.isBlank()) {
                Toast.makeText(requireContext(), "Preencha o campo $nomeCampo!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 5. Verificar se a senha e a confirmação de senha coincidem
        if (senha != confSenha) {
            Toast.makeText(requireContext(), "As senhas não coincidem!", Toast.LENGTH_SHORT).show()
            return
        }

        // 6. Se todas as validações passarem, registra o usuário e navega
        registerUser(email, senha)

        // Instanciar ContaPessoaJuridica, pois é um Brechó
        contaPessoaJuridica = ContaPessoaJuridica(
            nomeCompleto = nomeFantasia,
            nomeDeUsuario = usuario,
            email = email,
            telefone = telefone,
            cnpj = cnpj,
            endereço = endereco,
            dataCadastro = dataCadastro,
            tipoPessoa = tipoPessoa,
            tipoUsuario = tipoUsuario
        )

        // Ação para o próximo fragmento (CadastroEnderecoFragment)
        // Note que você deve passar a contaPessoaJuridica e passar 'null' para contaPessoaFisica.
        // E vice-versa para o fragmento de Usuário Comum.
        val action = CadatroBrechoFragmentDirections.actionCadatroBrechoFragmentToCadastroEnderecoFragment(null,contaPessoaJuridica)
        findNavController().navigate(action)
    }

    private fun registerUser(email: String, password: String){
        try{
            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener() { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        } catch(e: Exception) {
            Toast.makeText(requireContext(), e.message.toString(), Toast.LENGTH_SHORT).show()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}