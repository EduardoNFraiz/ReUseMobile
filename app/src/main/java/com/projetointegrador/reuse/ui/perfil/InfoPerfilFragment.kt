package com.projetointegrador.reuse.ui.perfil

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64 // Import necessário para Base64
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
import androidx.activity.result.contract.ActivityResultContracts
import com.projetointegrador.reuse.ui.closet.CriarGavetaFragmentDirections
import java.io.ByteArrayOutputStream
import java.io.IOException


class InfoPerfilFragment : Fragment() {
    private var _binding: FragmentInfoPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference

    private var isEditing: Boolean = false
    private var userPath: String? = null // Caminho do usuário no Firebase (ex: usuarios/pessoaFisica/{uid})
    private var newProfileImageBase64: String? = null // NOVO: Armazena a string Base64 da nova foto
    private var isInstitution: Boolean = false

    // NOVO: Registro do Activity Result Launcher (Substitui startActivityForResult e onActivityResult)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // 1. Carrega a imagem na tela
            binding.profileImage.setImageURI(it)

            // 2. Converte a Uri para Base64 e armazena
            newProfileImageBase64 = uriToBase64(requireContext(), it)

            if (newProfileImageBase64 != null) {
                showBottomSheet(message = getString(R.string.sucesso_nova_foto_selecionada_lembre_de_salvar))
            } else {
                showBottomSheet(message = getString(R.string.error_converter_imagem_tente_novamente))
            }
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
        binding.bttAnuncio.visibility = View.GONE

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

        binding.bttAnuncio.setOnClickListener {
            if (!isEditing) {
                // Navega para a tela de edição do anúncio (Configurar no NavGraph!)
                findNavController().navigate(R.id.action_infoPerfilFragment_to_editarAnuncioFragment)
            } else {
                showBottomSheet(message = getString(R.string.aviso_finalize_ou_cancele_edicao_perfil_para_editar_anuncio))
            }
        }

        // COMPORTAMENTO: Lidar com Edição/Cancelamento
        binding.bttEditar.setOnClickListener {
            if (isEditing) {
                // Se estiver editando e clicar em "Cancelar Edição"
                toggleEditMode(false)
                loadUserData() // Recarrega os dados originais do Firebase
                newProfileImageBase64 = null // Cancela a nova imagem
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

    // Função auxiliar para converter URI para Base64
    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                // Codifica para Base64
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Função auxiliar para carregar Base64 no Glide
    private fun loadBase64Image(base64String: String) {
        if (base64String.isNotEmpty() && context != null) {
            try {
                // Decodifica a string Base64 para um array de bytes
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

                // Usa Glide para carregar os bytes diretamente no ImageView
                Glide.with(requireContext())
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.profileImage)
            } catch (e: IllegalArgumentException) {
                // Lidar com strings Base64 inválidas
                e.printStackTrace()
                binding.profileImage.setImageResource(R.drawable.ic_launcher_background) // Fallback
            }
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_launcher_background) // Fallback
        }
    }


    // NOVO: Função para abrir o seletor de imagens
    private fun openImageChooser() {
        // Lança o contrato de resultado de atividade
        galleryLauncher.launch("image/*")
    }

    // --- LÓGICA DE CARREGAMENTO E EDIÇÃO ---

    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable

        binding.etNome.isEnabled = enable
        binding.etUsuario.isEnabled = enable
        binding.etEmail.isEnabled = false
        binding.etCpfCnpj.isEnabled = false
        binding.etTelefone.isEnabled = enable

        binding.bttSalvar.visibility = if (enable) View.VISIBLE else View.GONE
        binding.bttEndereco.isEnabled = !enable

        // 🛑 Lógica para o botão de Anúncio
        if (isInstitution) {
            // Só exibe o botão se for instituição e NÃO estiver editando o perfil principal
            binding.bttAnuncio.visibility = if (enable) View.GONE else View.VISIBLE
        } else {
            binding.bttAnuncio.visibility = View.GONE
        }


        binding.bttEditar.text = if (enable) "Cancelar Edição" else "Editar"
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showBottomSheet(message = getString(R.string.error_usuario_nao_logado))
            return
        }

        // 🛑 Reseta o flag da instituição antes de buscar
        isInstitution = false
        binding.bttAnuncio.visibility = View.GONE

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
                    showBottomSheet(message = getString(R.string.error_buscar_tipo_de_conta, error.message))
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

                            // 🛑 É UMA INSTITUIÇÃO (PJ)
                            isInstitution = true
                            binding.bttAnuncio.visibility = View.VISIBLE

                            fetchUserDetails(snapshot)
                        }

                        if (subtipo == subtipos.last() && !found) {
                            showBottomSheet(message = getString(R.string.error__tipo_de_conta_desconhecido))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = getString(R.string.error_buscar_subtipo, error.message))
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

        // LÓGICA DE CARREGAMENTO DA FOTO DE PERFIL COM BASE64
        // Assumindo que o campo no banco seja 'fotoBase64' ou 'fotoUrl'
        val fotoBase64 = snapshot.child("fotoUrl").getValue(String::class.java)
            ?: snapshot.child("fotoBase64").getValue(String::class.java) // Verifica a chave correta


        if (!fotoBase64.isNullOrEmpty()) {
            // Usa a nova função para carregar a imagem em Base64
            loadBase64Image(fotoBase64)
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_launcher_background) // Fallback
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
            showBottomSheet(message = getString(R.string.error_caminho_do_usuario_nao_definido_nao_salvou))
            return
        }

        val nome = binding.etNome.text.toString()
        val username = binding.etUsuario.text.toString()
        val telefone = binding.etTelefone.text.toString()
        // Documento não é editado
        val documento = binding.etCpfCnpj.text.toString()

        // Crie o mapa de atualização para o Realtime Database
        val updateMap = mutableMapOf<String, Any>()
        updateMap["nomeCompleto"] = nome
        updateMap["nomeDeUsuario"] = username
        updateMap["telefone"] = telefone

        // NOVO: Adiciona a string Base64 da nova foto, se existir
        if (newProfileImageBase64 != null) {
            // **IMPORTANTE**: Use a chave que você realmente usa no Firebase, que parece ser "fotoUrl" ou "fotoBase64".
            // Vou usar "fotoUrl" como no seu código original, mas saiba que é Base64.
            updateMap["fotoUrl"] = newProfileImageBase64!!
            // Limpa a variável local após adicionar ao mapa
            newProfileImageBase64 = null
        }


        // Validação básica
        if (nome.isBlank() || username.isBlank() || telefone.isBlank() || documento.isBlank()) {
            showBottomSheet(message = getString(R.string.aviso_preencha_todos_os_campos_obritorios))
            return
        }

        reference.child(userPath!!)
            .updateChildren(updateMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showBottomSheet(message = getString(R.string.sucesso_dados_atualizados))
                    toggleEditMode(false)
                    // O loadUserData() chamado em toggleEditMode(false) irá agora carregar a nova foto Base64.
                } else {
                    showBottomSheet(message = getString(
                        R.string.error_salvar_dados,
                        task.exception?.message
                    ))
                }
            }
    }


    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val action = CriarGavetaFragmentDirections.actionGlobalCadRoupaFragment(
                pecaUID = null,
                gavetaUID = null
            )
            findNavController().navigate(action)
        }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}