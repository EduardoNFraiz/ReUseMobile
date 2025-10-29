package com.projetointegrador.reuse.ui.auth.cadastro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.ContaPessoaFisica
import com.projetointegrador.reuse.data.model.ContaPessoaJuridica
import com.projetointegrador.reuse.databinding.FragmentCadastroUsuarioBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.MaskEditUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class CadastroUsuarioFragment : Fragment() {

    private var _binding: FragmentCadastroUsuarioBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    private lateinit var contaPessoaFisica: ContaPessoaFisica
    private lateinit var contaPessoaJuridica: ContaPessoaJuridica

    // Expressões Regulares
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    )
    private val USERNAME_PATTERN = Pattern.compile(
        "^\\S+$" // Nenhuma letra de espaço em branco
    )
    // ATUALIZADO: Aceita (xx) xxxx-xxxx ou (xx) xxxxx-xxxx
    private val PHONE_PATTERN = Pattern.compile(
        "^\\(\\d{2}\\) \\d{4,5}-\\d{4}$"
    )
    private val CPF_PATTERN = Pattern.compile(
        "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$" // Formato xxx.xxx.xxx-xx
    )
    // NOVO: Formato dd/mm/aaaa
    private val DATE_PATTERN = Pattern.compile(
        "^\\d{2}/\\d{2}/\\d{4}$"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastroUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners() {
        // Aplicar máscaras de formatação automática
        binding.editTextTelefone.addTextChangedListener(MaskEditUtil.mask(binding.editTextTelefone, MaskEditUtil.FORMAT_PHONE_BR))
        binding.editTextCpf.addTextChangedListener(MaskEditUtil.mask(binding.editTextCpf, MaskEditUtil.FORMAT_CPF))
        // NOVO: Máscara para Data de Nascimento dd/mm/aaaa
        binding.editTextDatanasc.addTextChangedListener(MaskEditUtil.mask(binding.editTextDatanasc, MaskEditUtil.FORMAT_DATE))


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
        // 1. Obter e limpar os valores dos campos
        val nome = binding.editTextNome.text.toString().trim()
        val usuario = binding.editTextUsuario.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val senha = binding.editTextSenha.text.toString().trim()
        val confSenha = binding.editTextConfsenha.text.toString().trim()
        val telefone = binding.editTextTelefone.text.toString().trim()
        val dataNascimento = binding.editTextDatanasc.text.toString().trim()
        val cpf = binding.editTextCpf.text.toString().trim()

        // 2. Definir uma lista de campos para verificação de preenchimento
        val campos = listOf(
            "Nome" to nome, "Usuário" to usuario, "E-mail" to email,
            "Senha" to senha, "Confirmação de Senha" to confSenha,
            "Telefone" to telefone, "Data de Nascimento" to dataNascimento, "CPF" to cpf
        )

        // 3. Verificar se todos os campos estão preenchidos
        for ((nomeCampo, valorCampo) in campos) {
            if (valorCampo.isBlank()) {
                Toast.makeText(requireContext(), "Preencha o campo $nomeCampo!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 4. Validações de Formato e Requisitos Mínimos

        // Nome de Usuário: Sem espaços em branco
        if (!USERNAME_PATTERN.matcher(usuario).matches()) {
            Toast.makeText(requireContext(), "O nome de usuário não pode conter espaços em branco!", Toast.LENGTH_SHORT).show()
            return
        }

        // E-mail: Formato válido
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(requireContext(), "E-mail inválido. Verifique o formato.", Toast.LENGTH_SHORT).show()
            return
        }

        // Senha: Mínimo 6 caracteres e confirmação
        if (senha.length < 6) {
            Toast.makeText(requireContext(), "A senha deve ter pelo menos 6 caracteres!", Toast.LENGTH_SHORT).show()
            return
        }
        if (senha != confSenha) {
            Toast.makeText(requireContext(), "As senhas não coincidem!", Toast.LENGTH_SHORT).show()
            return
        }

        // Telefone: Formato (xx) xxxxx-xxxx ou (xx) xxxx-xxxx
        if (!PHONE_PATTERN.matcher(telefone).matches()) {
            Toast.makeText(requireContext(), "Telefone inválido. Formato esperado: (xx) xxxxx-xxxx", Toast.LENGTH_SHORT).show()
            return
        }

        // Data de Nascimento: Formato dd/mm/aaaa
        if (!DATE_PATTERN.matcher(dataNascimento).matches()) {
            Toast.makeText(requireContext(), "Data de Nascimento inválida. Formato esperado: dd/mm/aaaa", Toast.LENGTH_SHORT).show()
            return
        }

        // CPF: Formato xxx.xxx.xxx-xx
        if (!CPF_PATTERN.matcher(cpf).matches()) {
            Toast.makeText(requireContext(), "CPF inválido. Formato esperado: xxx.xxx.xxx-xx", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Verificar Unicidade (Email, Usuário, CPF)
        checkUnicidadeAndProceed(email, usuario, cpf)
    }

    private fun checkUnicidadeAndProceed(email: String, usuario: String, cpf: String) {
        // Assumimos que seus dados de ContaPessoaFisica estão em um nó pai como 'contasPessoaFisica'
        val refContas = database.child("contasPessoaFisica") // Use o nome exato do seu nó no RTDB

        // Etapa 1: Checagem de CPF
        // ATENÇÃO: Para consultas no Realtime Database que não são a chave primária,
        // você PRECISA de uma regra de indexação (.indexOn) no seu arquivo firebase.json ou nas Regras do Database.
        refContas.orderByChild("cpf").equalTo(cpf).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Este CPF já está cadastrado!", Toast.LENGTH_SHORT).show()
                    return
                }

                // Etapa 2: Checagem de Nome de Usuário (somente se o CPF for único)
                refContas.orderByChild("nomeDeUsuario").equalTo(usuario).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        if (userSnapshot.exists()) {
                            Toast.makeText(requireContext(), "Este Nome de Usuário já está em uso!", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // Etapa 3: Se CPF e Usuário são únicos, prossegue com o cadastro no Firebase Auth
                        registerUser(email, binding.editTextSenha.text.toString().trim())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Erro ao verificar Nome de Usuário: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao verificar CPF: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerUser(email: String, password: String){
        val nome = binding.editTextNome.text.toString().trim()
        val usuario = binding.editTextUsuario.text.toString().trim()
        val telefone = binding.editTextTelefone.text.toString().trim()
        val dataNascimento = binding.editTextDatanasc.text.toString().trim()
        val cpf = binding.editTextCpf.text.toString().trim()
        val dataCadastro = gerarDataAtual()
        val tipoPessoa = "pessoaFisica"
        val tipoUsuario = "comum"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    // 6. Criar objeto e salvar dados no Realtime Database (RTDB)
                    contaPessoaFisica = ContaPessoaFisica(
                        nomeCompleto = nome,
                        nomeDeUsuario = usuario,
                        email = email,
                        telefone = telefone,
                        dataNascimento = dataNascimento,
                        cpf = cpf,
                        endereço = "",
                        dataCadastro = dataCadastro,
                        tipoPessoa = tipoPessoa,
                        tipoUsuario = tipoUsuario,
                    )

                    // Salvar no RTDB usando o UID como chave
                    if (userId.isNotEmpty()) {
                        database.child("contasPessoaFisica").child(userId).setValue(contaPessoaFisica)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Dados salvos no Database com sucesso!", Toast.LENGTH_SHORT).show()

                                // Navegar após salvar no RTDB
                                val action = CadastroUsuarioFragmentDirections.actionCadastroUsuarioFragmentToCadastroEnderecoFragment(contaPessoaFisica,null)
                                findNavController().navigate(action)
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Erro ao salvar dados no Database: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "Erro interno: UID do usuário não encontrado.", Toast.LENGTH_LONG).show()
                    }

                } else {
                    // Trata falhas de cadastro no Firebase Auth (Ex: E-mail já cadastrado, senha fraca)
                    val errorMessage = task.exception?.message
                    if (errorMessage != null && errorMessage.contains("email address is already in use")) {
                        Toast.makeText(requireContext(), "Este E-mail já está cadastrado!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), errorMessage ?: "Erro desconhecido no cadastro.", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}