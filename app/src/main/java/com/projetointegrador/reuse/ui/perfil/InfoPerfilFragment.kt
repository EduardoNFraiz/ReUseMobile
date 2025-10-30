package com.projetointegrador.reuse.ui.perfil

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import com.projetointegrador.reuse.databinding.FragmentInfoPerfilBinding
import com.projetointegrador.reuse.util.showBottomSheet
import com.bumptech.glide.Glide
// Novo import necessário para o Activity Result API
import androidx.activity.result.contract.ActivityResultContracts


class InfoPerfilFragment : Fragment() {
    private var _binding: FragmentInfoPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference

    private var isEditing: Boolean = false
    private var userPath: String? = null // Caminho do usuário no Firebase (ex: usuarios/pessoaFisica/{uid})

    // NOVO: Registro do Activity Result Launcher (Substitui startActivityForResult e onActivityResult)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.profileImage.setImageURI(it)
            // TODO: Aqui você deve adicionar a lógica para fazer o upload da nova imagem para o Firebase Storage
            // e atualizar a 'fotoUrl' no Realtime Database.
            showBottomSheet(message = "Nova foto selecionada! Lembre-se de salvar as alterações.")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        reference = Firebase.database.reference

        // Esconde o botão salvar por padrão
        binding.bttSalvar.visibility = View.GONE

        // Inicializa o modo de visualização
        toggleEditMode(false)

        loadUserData()
        initListeners()
        barraDeNavegacao()
    }

    private fun initListeners() {
        binding.btnLogout.setOnClickListener {
            showBottomSheet(
                titleButton = R.string.text_button_dialog_confirm_logout,
                titleDialog = R.string.text_title_dialog_confirm_logout,
                message = getString(R.string.text_message_dialog_confirm_logout),
                onClick = {
                    auth.signOut()
                    findNavController().navigate(R.id.action_infoPerfilFragment_to_loginFragment)
                }
            )
        }

        binding.bttEndereco.setOnClickListener {
            // Só navega se NÃO estiver em modo de edição
            if (!isEditing) {
                findNavController().navigate(R.id.action_infoPerfilFragment_to_editEnderecoFragment)
            }
        }

        // COMPORTAMENTO: Lidar com Edição/Cancelamento
        binding.bttEditar.setOnClickListener {
            if (isEditing) {
                // Se estiver editando e clicar em "Cancelar Edição"
                toggleEditMode(false)
                loadUserData() // Recarrega os dados originais do Firebase
            } else {
                // Se não estiver editando e clicar em "Editar"
                toggleEditMode(true)
            }
        }

        binding.bttSalvar.setOnClickListener {
            saveUserData()
        }

        // Listener para a imagem de perfil
        binding.profileCardView.setOnClickListener {
            if (isEditing) {
                openImageChooser()
            }
        }
    }

    // NOVO: Função para abrir o seletor de imagens
    private fun openImageChooser() {
        // Lança o contrato de resultado de atividade
        galleryLauncher.launch("image/*")
    }

    // O método onActivityResult foi removido, pois o Activity Result API o substituiu.


    // --- LÓGICA DE CARREGAMENTO E EDIÇÃO ---

    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable

        // Habilita/desabilita campos
        binding.etNome.isEnabled = enable
        binding.etUsuario.isEnabled = enable
        // O Email e o CPF/CNPJ não podem ser alterados
        binding.etEmail.isEnabled = false
        binding.etCpfCnpj.isEnabled = false // REGRA: CPF/CNPJ bloqueado mesmo em edição
        binding.etTelefone.isEnabled = enable

        // Habilita/desabilita botões
        binding.bttSalvar.visibility = if (enable) View.VISIBLE else View.GONE
        binding.bttEndereco.isEnabled = !enable // Trava o botão Endereço no modo edição

        // Atualiza o texto do botão de Edição/Cancelamento
        binding.bttEditar.text = if (enable) "Cancelar Edição" else "Editar"
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showBottomSheet(message = "Usuário não autenticado.")
            return
        }

        // 1. Tentar encontrar o usuário em 'pessoaFisica'
        reference.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userPath = "usuarios/pessoaFisica/$userId"
                        fetchUserDetails(snapshot)
                    } else {
                        // 2. Se não for Pessoa Física, tentar encontrar em 'pessoaJuridica'
                        searchPessoaJuridica(userId)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao buscar tipo de conta: ${error.message}")
                }
            })
    }

    private fun searchPessoaJuridica(userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && !found) {
                            found = true
                            userPath = "usuarios/pessoaJuridica/$subtipo/$userId"
                            fetchUserDetails(snapshot)
                        }

                        if (subtipo == subtipos.last() && !found) {
                            showBottomSheet(message = "Tipo de conta do usuário não identificado.")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = "Erro ao buscar subtipo: ${error.message}")
                    }
                })
        }
    }

    // 3. Preenche os campos de texto com os dados do usuário
    private fun fetchUserDetails(snapshot: DataSnapshot) {
        // Dados Pessoais/Perfil (usando as chaves CORRETAS do seu Firebase)
        val nome = snapshot.child("nomeCompleto").getValue(String::class.java) ?: ""
        val username = snapshot.child("nomeDeUsuario").getValue(String::class.java) ?: ""
        val email = auth.currentUser?.email
        val telefone = snapshot.child("telefone").getValue(String::class.java) ?: ""

        // Busca CPF (se for PF) ou CNPJ (se for PJ)
        val documento = snapshot.child("cnpj").getValue(String::class.java)
            ?: snapshot.child("cpf").getValue(String::class.java)
            ?: ""

        // LÓGICA DE CARREGAMENTO DA FOTO DE PERFIL COM GLIDE
        val fotoUrl = snapshot.child("fotoUrl").getValue(String::class.java)

        if (!fotoUrl.isNullOrEmpty() && context != null) {
            // Usa Glide para carregar a imagem da URL no ImageView
            Glide.with(requireContext())
                .load(fotoUrl)
                .placeholder(R.drawable.ic_launcher_background) // Use um placeholder seu
                .error(R.drawable.ic_launcher_background)       // Use um ícone de erro seu
                .into(binding.profileImage)
        }


        // Preenche os EditTexts
        binding.etNome.setText(nome)
        binding.etUsuario.setText(username)
        binding.etEmail.setText(email)
        binding.etTelefone.setText(telefone)
        binding.etCpfCnpj.setText(documento)
    }

    private fun saveUserData() {
        if (userPath == null) {
            showBottomSheet(message = "Caminho do usuário não definido. Não é possível salvar.")
            return
        }

        val nome = binding.etNome.text.toString()
        val username = binding.etUsuario.text.toString()
        val telefone = binding.etTelefone.text.toString()
        // Documento não é editado, mas checamos para validação
        val documento = binding.etCpfCnpj.text.toString()

        // Crie o mapa de atualização para o Realtime Database
        val updateMap = mutableMapOf<String, Any>()
        updateMap["nomeCompleto"] = nome
        updateMap["nomeDeUsuario"] = username
        updateMap["telefone"] = telefone
        // O documento (cpf/cnpj) e a fotoUrl devem ser atualizados em outras chamadas

        // Validação básica
        if (nome.isBlank() || username.isBlank() || telefone.isBlank() || documento.isBlank()) {
            showBottomSheet(message = "Preencha todos os campos obrigatórios.")
            return
        }

        reference.child(userPath!!)
            .updateChildren(updateMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showBottomSheet(message = "Dados atualizados com sucesso!")
                    toggleEditMode(false)
                } else {
                    showBottomSheet(message = "Erro ao salvar dados: ${task.exception?.message}")
                }
            }
    }


    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.pesquisar.setOnClickListener {
            findNavController().navigate(R.id.pesquisar)
        }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.cadastrarRoupa,bundle)
        }
        binding.doacao.setOnClickListener {
            findNavController().navigate(R.id.doacao)
        }
        binding.perfil.setOnClickListener {
            findNavController().navigate(R.id.perfil)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}