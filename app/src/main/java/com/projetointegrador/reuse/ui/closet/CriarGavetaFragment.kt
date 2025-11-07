package com.projetointegrador.reuse.ui.closet

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
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
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.databinding.FragmentCriarGavetaBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import java.io.ByteArrayOutputStream
import java.io.IOException

class CriarGavetaFragment : Fragment() {
    private var _binding: FragmentCriarGavetaBinding? = null
    private val binding get() = _binding!!

    // Variáveis da Gaveta
    private lateinit var gaveta: Gaveta
    private var newGaveta: Boolean = true
    private var gavetaId: String? = null

    private lateinit var reference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var imageUri: Uri? = null
    private var imageBase64: String? = null

    private var feedbackShown = false

    // O ActivityResultLauncher DEVE estar aqui
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.imageViewGaveta.setImageURI(uri)
                binding.imageViewGaveta.visibility = View.VISIBLE
                binding.iconPlaceholder.visibility = View.GONE
                imageBase64 = convertImageUriToBase64(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriarGavetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        barraDeNavegacao()
        reference = Firebase.database.reference
        auth = Firebase.auth

        val vizualizarInfo = arguments?.getBoolean("VISUALIZAR_INFO") ?: false
        gavetaId = arguments?.getString("GAVETA_ID")

        newGaveta = gavetaId.isNullOrBlank()

        setupViewMode(vizualizarInfo)
        initListeners()

        if (!newGaveta) {
            loadGavetaData(gavetaId!!)
        }
    }

    // --- SETUP DO MODO VISUALIZAÇÃO/EDIÇÃO/CRIAÇÃO ---
    private fun setupViewMode(vizualizarInfo: Boolean) {
        if (vizualizarInfo) {
            binding.bttEditar.visibility = View.VISIBLE
            binding.bttCriarGaveta.visibility = View.GONE
            binding.bttSalvar.visibility = View.GONE
            setFieldsEnabled(false)
        } else {
            binding.bttEditar.visibility = View.GONE
            binding.bttCriarGaveta.visibility = View.VISIBLE
            binding.bttSalvar.visibility = View.GONE
            setFieldsEnabled(true)
        }
    }

    // Função unificada para habilitar/desabilitar campos
    private fun setFieldsEnabled(isEnabled: Boolean) {
        binding.editTextGaveta.isEnabled = isEnabled
        binding.rbPrivado.isEnabled = isEnabled
        binding.rbPublico.isEnabled = isEnabled
        binding.imagePlaceholderCard.isEnabled = isEnabled
    }

    private fun initListeners() {
        binding.bttCriarGaveta.setOnClickListener {
            valideData(isCreation = true)
        }
        binding.imagePlaceholderCard.setOnClickListener {
            openImageChooser()
        }
        binding.bttEditar.setOnClickListener {
            modoEditor(true)
        }
        binding.bttSalvar.setOnClickListener {
            valideData(isCreation = false)
        }
    }

    // CORREÇÃO: Função de escolher a imagem
    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    // --- MANIPULAÇÃO DE IMAGEM E BASE64 ---
    private fun convertImageUriToBase64(uri: Uri): String? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: IOException) {
            showError(getString(R.string.erro_processar_imagem, e.message))
            null
        }
    }

    private fun displayBase64Image(base64String: String) {
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imageViewGaveta.setImageBitmap(decodedBitmap)
            binding.imageViewGaveta.visibility = View.VISIBLE
            binding.iconPlaceholder.visibility = View.GONE

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            binding.imageViewGaveta.setImageDrawable(null)
            binding.imageViewGaveta.visibility = View.GONE
            binding.iconPlaceholder.visibility = View.VISIBLE
        }
    }

    // --- LÓGICA DE CARREGAMENTO DE DADOS ---
    private fun loadGavetaData(uid: String) {
        // Busca direta no nó /gavetas/{uid}
        reference.child("gavetas").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val loadedGaveta = snapshot.getValue(Gaveta::class.java)
                    if (loadedGaveta != null) {
                        gaveta = loadedGaveta

                        binding.editTextGaveta.setText(gaveta.name)
                        if (gaveta.public) {
                            binding.rbPublico.isChecked = true
                        } else {
                            binding.rbPrivado.isChecked = true
                        }

                        if (!gaveta.fotoBase64.isNullOrEmpty()) {
                            displayBase64Image(gaveta.fotoBase64!!)
                            imageBase64 = gaveta.fotoBase64
                        }
                    } else {
                        showError("Gaveta não encontrada ou dados inválidos.")
                        findNavController().navigateUp()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Erro ao carregar dados da gaveta: ${error.message}")
                    findNavController().navigateUp()
                }
            })
    }

    // --- VALIDAÇÃO E SALVAMENTO/EDIÇÃO ---

    private fun valideData(isCreation: Boolean) {
        if (feedbackShown) return

        val nome = binding.editTextGaveta.text.toString().trim()
        val isPublic = binding.rbPublico.isChecked
        val isPrivate = binding.rbPrivado.isChecked

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError(getString(R.string.erro_usuario_nao_autenticado))
            return
        }

        if (isCreation && imageBase64.isNullOrBlank()) {
            showError(getString(R.string.msg_erro_imagem_vazia_gaveta))
            return
        }

        if (nome.isBlank() || (!isPublic && !isPrivate)) {
            showError(getString(R.string.msg_erro_visibilidade_vazia_gaveta))
            return
        }

        if (isCreation) {
            // ✅ AJUSTE AQUI: Cria novo objeto Gaveta, incluindo o 'ownerUid'
            gaveta = Gaveta(
                name = nome,
                ownerUid = userId, // <-- CRUCIAL: Adiciona o UID do proprietário
                number = "0",
                fotoBase64 = imageBase64,
                public = isPublic
            )
            saveGaveta(userId)
        } else {
            // Atualiza objeto Gaveta existente e salva
            gaveta.name = nome
            gaveta.public = isPublic
            gaveta.fotoBase64 = imageBase64
            // O ownerUid já deve estar na gaveta carregada, não precisa ser setado novamente.
            updateGaveta()
        }
    }

    // ✅ CORRIGIDO: Usa updateChildren() para APENAS atualizar os campos editáveis.
    // Isso garante que o nó "peças" não seja apagado.
    private fun updateGaveta() {
        if (gavetaId.isNullOrBlank()) {
            showError("Erro: ID da gaveta para edição não encontrado.")
            return
        }

        binding.bttSalvar.isEnabled = false

        // CRIA o mapa com APENAS os campos que devem ser atualizados.
        val updateMap = mapOf<String, Any?>(
            "name" to gaveta.name,
            "public" to gaveta.public,
            "fotoBase64" to gaveta.fotoBase64,
            "number" to gaveta.number
            // O ownerUid NUNCA deve ser atualizado.
        )

        reference.child("gavetas").child(gavetaId!!)
            .updateChildren(updateMap) // Usa updateChildren em vez de setValue
            .addOnCompleteListener { task ->
                binding.bttSalvar.isEnabled = true
                if (task.isSuccessful) {
                    showSuccessAndReturnToView()
                } else {
                    showError(getString(R.string.erro_salvar_detalhes_gaveta, task.exception?.message))
                }
            }
    }

    private fun showSuccessAndReturnToView() {
        if (!feedbackShown) {
            feedbackShown = true
            Toast.makeText(requireContext(), getString(R.string.sucesso_gaveta_atualizada), Toast.LENGTH_SHORT).show()
            modoEditor(false)
            feedbackShown = false
        }
    }

    private fun saveGaveta(userId: String) {
        // Lógica de CRIAÇÃO

        val tempGavetaId = reference.child("gavetas").push().key
        if (tempGavetaId.isNullOrBlank()) {
            showError(getString(R.string.erro_falha_gerar_id_gaveta))
            return
        }

        // 1. Garante que o ID da gaveta está no objeto ANTES de salvar (para o campo 'id')
        gaveta.id = tempGavetaId


        binding.bttCriarGaveta.isEnabled = false
        // 2. Salva a gaveta completa, incluindo 'ownerUid', no nó /gavetas
        reference.child("gavetas")
            .child(tempGavetaId)
            .setValue(gaveta)
            .addOnCompleteListener { taskGaveta ->
                if (taskGaveta.isSuccessful) {
                    // 3. Vincula a gaveta ao índice do usuário
                    getUserAccountType(userId, tempGavetaId)
                } else {
                    binding.bttCriarGaveta.isEnabled = true
                    showError(getString(R.string.erro_salvar_detalhes_gaveta, taskGaveta.exception?.message))
                }
            }
    }

    // --- CONTROLE DE FEEDBACK E NAVEGAÇÃO ---

    private fun showSuccessAndNavigate(id: String) {
        if (!feedbackShown) {
            feedbackShown = true
            Toast.makeText(requireContext(), getString(R.string.sucesso_gaveta_criada), Toast.LENGTH_SHORT).show()
            val bundle = Bundle().apply {
                putString("GAVETA_ID", id)
            }
            findNavController().navigate(R.id.action_criarGavetaFragment_to_gavetaFragment, bundle)
        }
    }

    private fun showError(message: String) {
        if (!feedbackShown) {
            feedbackShown = true
            showBottomSheet(
                titleDialog = R.string.atencao,
                message = message,
                titleButton = R.string.entendi
            )
            feedbackShown = false
        }
    }

    // --- MODO EDITOR (Habilitar/Desabilitar campos e botões) ---
    private fun modoEditor(startEditing: Boolean) {
        if (startEditing) {
            setFieldsEnabled(true)
            binding.bttSalvar.visibility = View.VISIBLE
            binding.bttEditar.visibility = View.INVISIBLE
        } else {
            setFieldsEnabled(false)
            binding.bttSalvar.visibility = View.INVISIBLE
            binding.bttEditar.visibility = View.VISIBLE
        }
    }

    // --- FUNÇÕES DE VINCULAÇÃO DE USUÁRIO (inalteradas e corretas para a estrutura) ---

    private fun getUserAccountType(userId: String, gavetaId: String) {
        reference.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        updateUserGavetaReference(userId, gavetaId, "pessoaFisica", null)
                    } else {
                        searchPessoaJuridica(userId, gavetaId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.bttCriarGaveta.isEnabled = true
                    showError("Erro ao buscar tipo de conta: ${error.message}")
                }
            })
    }

    private fun searchPessoaJuridica(userId: String, gavetaId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false
        var checkedCount = 0

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        checkedCount++
                        if (snapshot.exists() && !found) {
                            found = true
                            updateUserGavetaReference(userId, gavetaId, "pessoaJuridica", subtipo)
                        }
                        if (checkedCount == subtipos.size && !found) {
                            binding.bttCriarGaveta.isEnabled = true
                            showError(getString(R.string.erro_tipo_conta_nao_encontrado))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkedCount++
                        if (checkedCount == subtipos.size && !found) {
                            binding.bttCriarGaveta.isEnabled = true
                            showError("Erro ao buscar subtipo: ${error.message}")
                        }
                    }
                })
        }
    }

    private fun updateUserGavetaReference(
        userId: String, gavetaId: String,
        tipoConta: String, subtipoJuridico: String?
    ) {
        var userPath = ""
        if (tipoConta == "pessoaFisica") {
            userPath = "usuarios/pessoaFisica/$userId"
        } else if (tipoConta == "pessoaJuridica" && subtipoJuridico != null) {
            userPath = "usuarios/pessoaJuridica/$subtipoJuridico/$userId"
        }

        if (userPath.isNotEmpty()) {
            // Vincula a gaveta ao nó de índice rápido do usuário
            val userUpdateMap = mapOf(
                "gavetas/$gavetaId" to true
            )
            reference.child(userPath)
                .updateChildren(userUpdateMap)
                .addOnCompleteListener { taskUser ->
                    binding.bttCriarGaveta.isEnabled = true
                    if (taskUser.isSuccessful) {
                        showSuccessAndNavigate(gavetaId)
                    } else {
                        showError(getString(R.string.erro_vincular_gaveta_usuario, taskUser.exception?.message))
                    }
                }
        } else {
            binding.bttCriarGaveta.isEnabled = true
            showError(getString(R.string.erro_tipo_conta_invalido))
        }
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.cadastrarRoupa,bundle) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}