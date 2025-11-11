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
import com.projetointegrador.reuse.databinding.FragmentCadatroBrechoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.MaskEditUtil // Importar para as máscaras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class CadatroBrechoFragment : Fragment() {

    private var _binding: FragmentCadatroBrechoBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    // Mantém a referência apenas para a checagem de unicidade, mas o salvamento foi removido.
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // Expressões Regulares
    private val EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    private val USERNAME_PATTERN = Pattern.compile("^\\S+$")
    private val PHONE_PATTERN = Pattern.compile("^\\(\\d{2}\\) \\d{4,5}-\\d{4}$")
    private val CNPJ_PATTERN = Pattern.compile("^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

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
        // Aplicar máscaras de formatação automática
        binding.editTextTelefone.addTextChangedListener(MaskEditUtil.mask(binding.editTextTelefone, MaskEditUtil.FORMAT_PHONE_BR))
        // Usando CNPJ
        binding.editTextCpf.addTextChangedListener(MaskEditUtil.mask(binding.editTextCpf, MaskEditUtil.FORMAT_CNPJ))

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
        val nomeFantasia = binding.editTextNome.text.toString().trim()
        val usuario = binding.editTextUsuario.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val senha = binding.editTextSenha.text.toString().trim()
        val confSenha = binding.editTextConfsenha.text.toString().trim()
        val telefone = binding.editTextTelefone.text.toString().trim()
        val cnpj = binding.editTextCpf.text.toString().trim() // CNPJ no campo 'CPF'

        val dataCadastro = gerarDataAtual()
        val tipoPessoa = "PessoaJuridica"
        val tipoUsuario = "brecho"

        // 2. Definir uma lista de campos para verificação de preenchimento
        val campos = listOf(
            "Nome da Loja" to nomeFantasia, "Usuário" to usuario, "E-mail" to email,
            "Senha" to senha, "Confirmação de Senha" to confSenha,
            "Telefone" to telefone, "CNPJ" to cnpj
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

        // CNPJ: Formato xx.xxx.xxx/xxxx-xx
        if (!CNPJ_PATTERN.matcher(cnpj).matches()) {
            Toast.makeText(requireContext(), "CNPJ inválido. Formato esperado: xx.xxx.xxx/xxxx-xx", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Verificar Unicidade (Email, Usuário, CNPJ)
        checkUnicidadeAndProceed(email, usuario, cnpj, nomeFantasia, telefone, dataCadastro, tipoPessoa, tipoUsuario)
    }

    private fun checkUnicidadeAndProceed(
        email: String, usuario: String, cnpj: String,
        nomeFantasia: String, telefone: String, dataCadastro: String,
        tipoPessoa: String, tipoUsuario: String
    ) {
        val senha = binding.editTextSenha.text.toString().trim()
        // Referência correta para o nó de Instituições
        val refInstituicoes = database.child("usuarios").child("pessoaJuridica").child("instituicoes")
        // Referência correta para o nó de Pessoa Física (para checagem cruzada de usuário)
        val refPessoaFisica = database.child("usuarios").child("pessoaFisica")

        // Etapa 1: Checagem de CNPJ (Somente no nó de PJ/Instituições)
        refInstituicoes.orderByChild("cnpj").equalTo(cnpj).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Este CNPJ já está cadastrado!", Toast.LENGTH_SHORT).show()
                    return
                }

                // Etapa 2: Checagem de Nome de Usuário (CHECA NAS DUAS TABELAS)
                // 2a. Checagem em Pessoa Jurídica (Instituições)
                refInstituicoes.orderByChild("nomeDeUsuario").equalTo(usuario).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userPjSnapshot: DataSnapshot) {
                        if (userPjSnapshot.exists()) {
                            Toast.makeText(requireContext(), "Este Nome de Usuário já está em uso por outra Instituição!", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // 2b. Checagem em Pessoa Física (para garantir unicidade global)
                        refPessoaFisica.orderByChild("nomeDeUsuario").equalTo(usuario).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userPfSnapshot: DataSnapshot) {
                                if (userPfSnapshot.exists()) {
                                    Toast.makeText(requireContext(), "Este Nome de Usuário já está em uso por um Usuário Comum!", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                // Etapa 3: Se CNPJ e Usuário são únicos, prossegue com o cadastro
                                registerUser(email, senha, nomeFantasia, usuario, telefone, cnpj, dataCadastro, tipoPessoa, tipoUsuario)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Erro ao verificar Nome de Usuário (PF): ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Erro ao verificar Nome de Usuário (PJ): ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao verificar CNPJ: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerUser(
        email: String, password: String, nomeFantasia: String,
        usuario: String, telefone: String, cnpj: String,
        dataCadastro: String, tipoPessoa: String, tipoUsuario: String
    ){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    // 1. Criar objeto ContaPessoaJuridica sem salvar no RTDB
                    val contaPessoaJuridica = ContaPessoaJuridica(
                        nomeCompleto = nomeFantasia,
                        nomeDeUsuario = usuario,
                        email = email,
                        telefone = telefone,
                        cnpj = cnpj,
                        // Endereço e Foto Base64 serão adicionados no próximo fragmento.
                        endereço = "",
                        dataCadastro = dataCadastro,
                        tipoPessoa = tipoPessoa,
                        tipoUsuario = tipoUsuario,
                    )

                    Toast.makeText(requireContext(), "Autenticação criada! Continue para o endereço.", Toast.LENGTH_SHORT).show()

                    // 2. Navegar para a próxima tela, passando o objeto ContaPessoaJuridica (PJ)
                    // O salvamento no RTDB será feito na última etapa (AddFotoperfilFragment)
                    val action = CadatroBrechoFragmentDirections.actionCadatroBrechoFragmentToCadastroEnderecoFragment(null, contaPessoaJuridica)
                    findNavController().navigate(action)

                } else {
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