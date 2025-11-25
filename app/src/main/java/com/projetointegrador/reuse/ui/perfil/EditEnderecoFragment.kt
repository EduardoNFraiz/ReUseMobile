package com.projetointegrador.reuse.ui.perfil

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentEditEnderecoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import androidx.core.content.ContextCompat
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import java.lang.StringBuilder

// Definição da máscara e do limite para serem acessíveis globalmente na classe
private const val CEP_MASK = "#####-###"
private const val NUMERO_MAX_LENGTH = 5 // Limite razoável para número da rua

class EditEnderecoFragment : Fragment() {
    private var _binding: FragmentEditEnderecoBinding? = null
    // O getter deve ser mantido, mas o acesso real deve ser feito com checagem de nulo nas funções
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference

    private var isEditing: Boolean = false
    private var userPath: String? = null
    private var addressUID: String? = null

    // Variável para manter a referência do TextWatcher do CEP (para remover depois)
    private var cepTextWatcher: TextWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditEnderecoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        reference = Firebase.database.reference

        // Inicializa o modo de visualização (desabilitado)
        toggleEditMode(false)

        loadUserData()
        initListeners()
        initToolbar(binding.toolbar)
    }

    private fun initListeners() {
        binding.bttSalvar.setOnClickListener {
            saveUserData()
        }

        binding.bttEditar.setOnClickListener {
            if (isEditing) {
                // Se estiver editando e clicar em "Cancelar"
                toggleEditMode(false)
            } else {
                // Se não estiver editando e clicar em "Editar"
                toggleEditMode(true)
            }
        }
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable

        // TRATAMENTO NULLPOINTER: Verifica se a View ainda existe
        val currentBinding = _binding ?: return

        // Habilita/desabilita campos de texto
        currentBinding.editTextCep.isEnabled = enable
        currentBinding.editTextRua.isEnabled = enable
        currentBinding.editTextNmrrua.isEnabled = enable
        currentBinding.editTextComplemento.isEnabled = enable
        currentBinding.editTextBairro.isEnabled = enable
        currentBinding.editTextCidade.isEnabled = enable
        currentBinding.editTextEstado.isEnabled = enable
        currentBinding.editTextPais.isEnabled = enable

        // Habilita/desabilita visibilidade do botão Salvar
        currentBinding.bttSalvar.visibility = if (enable) View.VISIBLE else View.GONE

        // Controla explicitamente o estado de habilitação do botão Salvar
        currentBinding.bttSalvar.isEnabled = enable

        // SOLUÇÃO DE COR: Força o alpha para 1.0 no modo de edição (enabled=true)
        // para garantir que nenhuma opacidade extra seja aplicada ao botão pelo sistema,
        // mantendo apenas a opacidade definida no seu estilobtt1.xml.
        currentBinding.bttSalvar.alpha = if (enable) 1.0f else 0.5f

        // Atualiza o texto do botão de Edição/Cancelamento
        currentBinding.bttEditar.text = if (enable) "Cancelar Edição" else "Editar"

        // Aplica ou remove os filtros conforme o modo
        if (enable) {
            setupInputFilters()
        } else {
            removeInputFilters()
            loadUserData()
        }
    }

    private fun setupInputFilters() {
        val currentBinding = _binding ?: return

        // 1. CEP: Aplica a máscara (99999-999)
        if (cepTextWatcher == null) {
            cepTextWatcher = object : TextWatcher {
                private var isUpdating = false

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: android.text.Editable) {
                    if (isUpdating) return

                    isUpdating = true
                    val unmasked = s.toString().replace(Regex("[^\\d]"), "")
                    val masked = StringBuilder()
                    var i = 0
                    var j = 0

                    while (i < unmasked.length && j < CEP_MASK.length) {
                        if (CEP_MASK[j] == '#') {
                            masked.append(unmasked[i])
                            i++
                        } else {
                            masked.append(CEP_MASK[j])
                        }
                        j++
                    }

                    s.replace(0, s.length, masked.toString())
                    isUpdating = false
                }
            }
            currentBinding.editTextCep.addTextChangedListener(cepTextWatcher)

            // CORREÇÃO: Remove o argumento nomeado
            currentBinding.editTextCep.filters = arrayOf(InputFilter.LengthFilter(CEP_MASK.length))
            currentBinding.editTextCep.inputType = InputType.TYPE_CLASS_NUMBER
        }

        // 2. Número da Rua: Garante que apenas números sejam inseridos E LIMITA O TAMANHO
        currentBinding.editTextNmrrua.inputType = InputType.TYPE_CLASS_NUMBER
        // CORREÇÃO: Remove o argumento nomeado
        currentBinding.editTextNmrrua.filters = arrayOf(InputFilter.LengthFilter(NUMERO_MAX_LENGTH))
    }

    private fun removeInputFilters() {
        val currentBinding = _binding ?: return

        // Remove o filtro/máscara do CEP
        if (cepTextWatcher != null) {
            currentBinding.editTextCep.removeTextChangedListener(cepTextWatcher)
            cepTextWatcher = null
            currentBinding.editTextCep.filters = arrayOf()
            currentBinding.editTextCep.inputType = InputType.TYPE_CLASS_TEXT
        }

        // Remove a restrição de tamanho do número
        currentBinding.editTextNmrrua.filters = arrayOf()
        currentBinding.editTextNmrrua.inputType = InputType.TYPE_CLASS_TEXT
    }

    /**
     * Passo 1 da busca encadeada: Encontrar o caminho do usuário e obter o addressUID.
     */
    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showBottomSheet(message = getString(R.string.error_usuario_nao_logado))
            return
        }

        clearAddressFields()
        userPath = null
        addressUID = null

        // 1. Tenta encontrar o usuário em 'pessoaFisica'
        reference.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userPath = "usuarios/pessoaFisica/$userId"
                        getAndLoadAddress(snapshot)
                    } else {
                        // 2. Tenta encontrar em 'pessoaJuridica/subtipo'
                        searchPessoaJuridicaAddress(userId)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(R.string.error_buscar_tipo_de_conta, error.message))
                }
            })
    }

    private fun searchPessoaJuridicaAddress(userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && !found) {
                            found = true
                            userPath = "usuarios/pessoaJuridica/$subtipo/$userId"
                            getAndLoadAddress(snapshot)
                        }

                        if (subtipo == subtipos.last() && !found) {
                            showBottomSheet(message = getString(R.string.error_usuario_nao_encontrado_endereco_nao_carregado))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = getString(R.string.subtipo_nao_encontrado, error.message))
                    }
                })
        }
    }

    /**
     * Passo 2 da busca encadeada: Obter o addressUID (chave: "endereço") e carregar o endereço.
     */
    private fun getAndLoadAddress(userSnapshot: DataSnapshot) {
        // Obtendo o UID do endereço do campo EXATO "endereço" (minúsculo e com 'ç')
        val retrievedAddressUID = userSnapshot.child("endereço").getValue(String::class.java)

        if (retrievedAddressUID.isNullOrEmpty()) {
            showBottomSheet(message = getString(R.string.error_endereo_nao_encontrado_nos_dados_do_usuario))
            return
        }

        addressUID = retrievedAddressUID

        // Busca o endereço na tabela 'enderecos' usando o UID
        reference.child("enderecos").child(addressUID!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(addressSnapshot: DataSnapshot) {
                    if (addressSnapshot.exists()) {
                        fetchAddressDetails(addressSnapshot)
                    } else {
                        showBottomSheet(message = getString(
                            R.string.error_endereco_encontrado_detalhes_nao,
                            addressUID
                        ))
                        clearAddressFields()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(
                        R.string.error_buscar_detalhes_endereco,
                        error.message
                    ))
                }
            })
    }

    /**
     * Passo 3: Preencher a UI com os dados do endereço e formatar o CEP.
     * Recebe o snapshot do nó 'enderecos/{addressUID}'.
     */
    private fun fetchAddressDetails(snapshot: DataSnapshot) {
        // TRATAMENTO DE NULLPOINTER: Verifica se a View ainda existe
        val currentBinding = _binding ?: return

        val rawCep = snapshot.child("cep").getValue(String::class.java) ?: ""

        // Formata o CEP ANTES de preencher o campo
        val formattedCep = formatCep(rawCep)

        // Preenche os EditTexts
        currentBinding.editTextCep.setText(formattedCep)
        currentBinding.editTextRua.setText(snapshot.child("rua").getValue(String::class.java) ?: "")
        currentBinding.editTextNmrrua.setText(snapshot.child("numero").getValue(String::class.java) ?: "")
        currentBinding.editTextComplemento.setText(snapshot.child("complemento").getValue(String::class.java) ?: "")
        currentBinding.editTextBairro.setText(snapshot.child("bairro").getValue(String::class.java) ?: "")
        currentBinding.editTextCidade.setText(snapshot.child("cidade").getValue(String::class.java) ?: "")
        currentBinding.editTextEstado.setText(snapshot.child("estado").getValue(String::class.java) ?: "")
        currentBinding.editTextPais.setText(snapshot.child("pais").getValue(String::class.java) ?: "")
    }

    private fun formatCep(rawCep: String): String {
        val unmasked = rawCep.replace(Regex("[^\\d]"), "")
        if (unmasked.length < 5) return rawCep

        // Aplica a máscara #####-###
        val prefix = unmasked.substring(0, 5)
        // Pega o que resta, limitando a 3 caracteres
        val suffix = if (unmasked.length > 5) unmasked.substring(5).take(3) else ""

        return if (suffix.isNotEmpty()) "$prefix-$suffix" else prefix
    }

    private fun clearAddressFields() {
        // TRATAMENTO DE NULLPOINTER: Verifica se a View ainda existe
        val currentBinding = _binding ?: return

        currentBinding.editTextCep.setText("")
        currentBinding.editTextRua.setText("")
        currentBinding.editTextNmrrua.setText("")
        currentBinding.editTextComplemento.setText("")
        currentBinding.editTextBairro.setText("")
        currentBinding.editTextCidade.setText("")
        currentBinding.editTextEstado.setText("")
        currentBinding.editTextPais.setText("")
    }

    private fun saveUserData() {
        // TRATAMENTO DE NULLPOINTER: Verifica se a View ainda existe
        val currentBinding = _binding ?: return

        // Precisamos do UID do endereço para saber onde salvar
        if (addressUID == null) {
            showBottomSheet(message = getString(R.string.error_id_endereco_nao_carregado_nao_possivel_salvar))
            return
        }

        // 1. Captura os dados da tela e REMOVE FORMATOS/MÁSCARAS antes de salvar
        // Limpa a máscara do CEP (deixa apenas dígitos) antes de salvar no Firebase
        val cep = currentBinding.editTextCep.text.toString().trim().replace(Regex("[^\\d]"), "")

        val rua = currentBinding.editTextRua.text.toString().trim()
        val numero = currentBinding.editTextNmrrua.text.toString().trim()
        val complemento = currentBinding.editTextComplemento.text.toString().trim()
        val bairro = currentBinding.editTextBairro.text.toString().trim()
        val cidade = currentBinding.editTextCidade.text.toString().trim()
        val estado = currentBinding.editTextEstado.text.toString().trim()
        val pais = currentBinding.editTextPais.text.toString().trim()

        // 2. Validação básica
        if (cep.isBlank() || rua.isBlank() || numero.isBlank() || bairro.isBlank() || cidade.isBlank() || estado.isBlank() || pais.isBlank()) {
            showBottomSheet(message = getString(R.string.aviso_preencha_todos_os_campos_obrigatorios))
            return
        }

        // 3. Cria o mapa de atualização
        val updateMap = mapOf<String, Any>(
            "cep" to cep,
            "rua" to rua,
            "numero" to numero,
            "complemento" to complemento,
            "bairro" to bairro,
            "cidade" to cidade,
            "estado" to estado,
            "pais" to pais
        )

        // 4. Salva no Firebase, diretamente no nó 'enderecos/{addressUID}'
        reference.child("enderecos").child(addressUID!!)
            .updateChildren(updateMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showBottomSheet(message = getString(R.string.sucesso_endereco_atualizado))
                    toggleEditMode(false)
                    // Navega de volta após o sucesso
                    findNavController().navigate(R.id.action_editEnderecoFragment_to_infoPerfilFragment)
                } else {
                    showBottomSheet(message = getString(
                        R.string.error_salvar_endereco,
                        task.exception?.message
                    ))
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        removeInputFilters()
        _binding = null // CRÍTICO: Zera o binding ao destruir a View
    }
}