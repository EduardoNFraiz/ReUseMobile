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

    // Expressões Regulares (mantidas)
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    )
    private val USERNAME_PATTERN = Pattern.compile(
        "^\\S+$"
    )
    private val PHONE_PATTERN = Pattern.compile(
        "^\\(\\d{2}\\) \\d{4,5}-\\d{4}$"
    )
    private val CPF_PATTERN = Pattern.compile(
        "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$"
    )
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

        val dataCadastro = gerarDataAtual()
        val tipoPessoa = "pessoaFisica"
        val tipoUsuario = "comum"

        // 2. Definir uma lista de campos para verificação de preenchimento
        val campos = listOf(
            "Nome" to nome, "Usuário" to usuario, "E-mail" to email,
            "Senha" to senha, "Confirmação de Senha" to confSenha,
            "Telefone" to telefone, "Data de Nascimento" to dataNascimento, "CPF" to cpf
        )

        // 3. Verificar se todos os campos estão preenchidos
        for ((nomeCampo, valorCampo) in campos) {
            if (valorCampo.isBlank()) {
                Toast.makeText(requireContext(), R.string.aviso_preencha_o_campo, Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 4. Validações de Formato e Requisitos Mínimos (mantidas)
        if (!USERNAME_PATTERN.matcher(usuario).matches()) {
            Toast.makeText(requireContext(), R.string.aviso_nome_usuario, Toast.LENGTH_SHORT).show()
            return
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.aviso_formato_email, Toast.LENGTH_SHORT).show()
            return
        }
        if (senha.length < 6) {
            Toast.makeText(requireContext(), R.string.aviso_tamanho_senha, Toast.LENGTH_SHORT).show()
            return
        }
        if (senha != confSenha) {
            Toast.makeText(requireContext(), R.string.aviso_senhas_diferentes, Toast.LENGTH_SHORT).show()
            return
        }
        if (!PHONE_PATTERN.matcher(telefone).matches()) {
            Toast.makeText(requireContext(), R.string.aviso_formato_telefone, Toast.LENGTH_SHORT).show()
            return
        }
        if (!DATE_PATTERN.matcher(dataNascimento).matches()) {
            Toast.makeText(requireContext(),
                getString(R.string.aviso_formato_data_de_nascimento), Toast.LENGTH_SHORT).show()
            return
        }
        if (!CPF_PATTERN.matcher(cpf).matches()) {
            Toast.makeText(requireContext(),
                getString(R.string.aviso_formato_cpf), Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Verificar Unicidade (Email, Usuário, CPF)
        checkUnicidadeAndProceed(email, usuario, cpf)
    }

    private fun checkUnicidadeAndProceed(email: String, usuario: String, cpf: String) {
        // Referência correta para o nó de Pessoa Física
        val refPessoaFisica = database.child("usuarios").child("pessoaFisica")
        // Referência correta para o nó de Instituições (dentro de Pessoa Jurídica)
        val refInstituicoes = database.child("usuarios").child("pessoaJuridica").child("instituicoes")

        // Etapa 1: Checagem de CPF (Só precisa checar em Pessoa Física)
        refPessoaFisica.orderByChild("cpf").equalTo(cpf).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(),
                        getString(R.string.aviso_cpf_ja_cadastrado), Toast.LENGTH_SHORT).show()
                    return
                }

                // Etapa 2: Checagem de Nome de Usuário (CHECA NAS DUAS TABELAS)
                // 2a. Checagem em Pessoa Física
                refPessoaFisica.orderByChild("nomeDeUsuario").equalTo(usuario).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userPfSnapshot: DataSnapshot) {
                        if (userPfSnapshot.exists()) {
                            Toast.makeText(requireContext(), R.string.aviso_nome_usuario_ja_cadastrado, Toast.LENGTH_SHORT).show()
                            return
                        }

                        // 2b. Checagem em Pessoa Jurídica (Instituições)
                        refInstituicoes.orderByChild("nomeDeUsuario").equalTo(usuario).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userPjSnapshot: DataSnapshot) {
                                if (userPjSnapshot.exists()) {
                                    Toast.makeText(requireContext(), R.string.aviso_nome_usuario_ja_cadastrado, Toast.LENGTH_SHORT).show()
                                    return
                                }

                                // Etapa 3: Se CPF e Usuário são únicos, prossegue com o cadastro no Firebase Auth
                                registerUser(email, binding.editTextSenha.text.toString().trim())
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), R.string.erro_ao_verificar_nome_de_usuario, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), R.string.erro_ao_verificar_nome_de_usuario, Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_verificar_cpf, error.message), Toast.LENGTH_SHORT).show()
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

                    // 1. Cria o objeto ContaPessoaFisica sem salvar no RTDB
                    val contaPessoaFisica = ContaPessoaFisica(
                        nomeCompleto = nome,
                        nomeDeUsuario = usuario,
                        email = email,
                        telefone = telefone,
                        dataNascimento = dataNascimento,
                        cpf = cpf,
                        endereco = "",
                        dataCadastro = dataCadastro,
                        tipoPessoa = tipoPessoa,
                        tipoUsuario = tipoUsuario,
                    )

                    Toast.makeText(requireContext(), R.string.sucesso_autenticacao, Toast.LENGTH_SHORT).show()

                    // 2. Navegar para a próxima tela, passando o objeto ContaPessoaFisica
                    // O salvamento no RTDB ocorrerá na última etapa.
                    val action = CadastroUsuarioFragmentDirections.actionCadastroUsuarioFragmentToCadastroEnderecoFragment(contaPessoaFisica,null)
                    findNavController().navigate(action)

                } else {
                    // Trata falhas de cadastro no Firebase Auth
                    val errorMessage = task.exception?.message
                    if (errorMessage != null && errorMessage.contains("email address is already in use")) {
                        Toast.makeText(requireContext(), R.string.aviso_email_ja_cadastrado, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), errorMessage ?: getString(R.string.error_desconhecido_no_cadastro), Toast.LENGTH_LONG).show()
                    }
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}