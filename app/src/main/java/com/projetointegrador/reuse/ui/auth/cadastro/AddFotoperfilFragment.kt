package com.projetointegrador.reuse.ui.auth.cadastro

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentAddFotoperfilBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.data.model.ContaPessoaFisica
import com.projetointegrador.reuse.data.model.ContaPessoaJuridica
import com.projetointegrador.reuse.data.model.Endereco

// IMPORTS NECESSÁRIOS PARA BASE64 E COROUTINES
import android.util.Base64
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddFotoperfilFragment : Fragment() {

    private var _binding: FragmentAddFotoperfilBinding? = null
    private val binding get() = _binding!!

    // Inicialização do Firebase
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Acessa os argumentos passados pelo Safe Args
    private val args: AddFotoperfilFragmentArgs by navArgs()
    private var uriFotoPerfil: Uri? = null // URI local da foto selecionada
    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // --- Lógica executada após a seleção da imagem ---
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    uriFotoPerfil = uri // 1. Guarda a URI para a conversão Base64

                    // 2. Exibe a imagem no ImageButton 'avatarImage'
                    Glide.with(requireContext())
                        .load(uri)
                        .circleCrop() // Aplica o corte circular
                        .into(binding.avatarImage)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFotoperfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners() {
        binding.btnCamera.setOnClickListener {
            openImageChooser()
        }
        binding.avatarImage.setOnClickListener {
            openImageChooser()
        }

        // Opção 1: Continuar SEM foto (salva os dados sem Base64)
        binding.buttonContSemFoto.setOnClickListener {
            uploadAndFinalizeRegistration(shouldUploadPhoto = false)
        }

        // Opção 2: Criar conta com foto
        binding.bttCriarConta.setOnClickListener {
            val hasPhotoSelected = uriFotoPerfil != null
            if (!hasPhotoSelected) {
                // Se o usuário clicou em 'Criar Conta' mas não selecionou a foto
                Toast.makeText(requireContext(), "Selecione uma foto ou continue sem foto.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadAndFinalizeRegistration(shouldUploadPhoto = true)
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*" // Filtra apenas arquivos de imagem
        galleryLauncher.launch(intent)
    }

    /**
     * Converte a URI da imagem para uma string Base64.
     * Deve ser executada em uma thread de I/O.
     */
    private fun convertUriToBase64(uri: Uri): String? {
        try {
            // Usa ContentResolver para abrir a imagem
            requireContext().contentResolver.openInputStream(uri).use { inputStream ->
                val bytes = inputStream?.readBytes()
                // FLAG_NO_WRAP é crucial para evitar quebras de linha no Base64
                return Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * PASSO 1: Inicia o fluxo assíncrono. Salva o Endereço e, se houver sucesso, chama o próximo passo (Base64).
     */
    private fun uploadAndFinalizeRegistration(shouldUploadPhoto: Boolean) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Erro: Usuário não autenticado. Redirecionando para login.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val contaFisica = args.contaPessoaFisica
        val contaJuridica = args.contaPessoaJuridica
        val enderecoASerSalvo = args.endereco // Objeto Endereco

        val contaASerSalva = contaFisica ?: contaJuridica

        if (contaASerSalva == null) {
            Toast.makeText(requireContext(), "Erro: Dados de conta não encontrados.", Toast.LENGTH_LONG).show()
            return
        }

        // --- 🎯 AJUSTE DO NÓ RAIZ COM BASE NO TIPO DE CONTA E TIPO DE USUÁRIO (Brechós/Instituições) ---
        val dbRootNode: String

        if (contaFisica != null) {
            // Caminho para Pessoa Física: usuarios/pessoaFisica/{UID}
            dbRootNode = "usuarios/pessoaFisica"
        } else if (contaJuridica != null) {
            // Caminho para Pessoa Jurídica: usuarios/pessoaJuridica/{tipoUsuario_plural}/{UID}
            val tipoUsuario = contaJuridica.tipoUsuario // Deve ser "brecho" ou "instituicao"

            // Adiciona o plural 's' para o nó do banco de dados
            val tipoUsuarioPlural = when (tipoUsuario) {
                "brecho" -> "brechos"
                "instituicao" -> "instituicoes"
                else -> {
                    Toast.makeText(requireContext(), "Erro: Tipo de usuário PJ desconhecido.", Toast.LENGTH_LONG).show()
                    return // Sai da função em caso de erro
                }
            }
            dbRootNode = "usuarios/pessoaJuridica/$tipoUsuarioPlural"
        } else {
            Toast.makeText(requireContext(), "Erro interno: Tipo de conta desconhecido.", Toast.LENGTH_LONG).show()
            return
        }
        // ------------------------------------------------------------------------------------------

        // --- SALVAMENTO DO ENDEREÇO (ASYNC) ---

        // Assume-se que o Endereço será salvo em um nó separado para normalização
        val enderecoRef = database.getReference("enderecos").push()
        val enderecoId = enderecoRef.key

        if (enderecoId == null) {
            Toast.makeText(requireContext(), "Erro ao gerar ID para o endereço.", Toast.LENGTH_LONG).show()
            return
        }

        // Salva o objeto Endereco no caminho /enderecos/{enderecoId}
        enderecoRef.setValue(enderecoASerSalvo)
            .addOnSuccessListener {
                // Sucesso no salvamento do Endereço

                // Atualiza a conta com o ID gerado (normalização)
                when (contaASerSalva) {
                    is ContaPessoaFisica -> contaASerSalva.endereço = enderecoId
                    is ContaPessoaJuridica -> contaASerSalva.endereço = enderecoId
                }

                // ✅ CHAMA O PRÓXIMO PASSO: Lógica Base64 em uma Coroutine
                viewLifecycleOwner.lifecycleScope.launch {
                    handlePhotoBase64AndSaveAccount(user.uid, contaASerSalva, dbRootNode, shouldUploadPhoto)
                }

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Falha ao salvar endereço: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
    }


    /**
     * PASSO 2: Lida com a conversão da foto para Base64.
     */
    private suspend fun handlePhotoBase64AndSaveAccount(
        uid: String,
        contaASerSalva: Any,
        dbRootNode: String,
        shouldUploadPhoto: Boolean
    ) {
        var base64String: String? = null

        // 1. Executa a conversão para Base64 em thread de I/O
        if (shouldUploadPhoto && uriFotoPerfil != null) {
            base64String = withContext(Dispatchers.IO) {
                convertUriToBase64(uriFotoPerfil!!)
            }
        }

        // 2. Atualiza o campo Base64 na conta
        when (contaASerSalva) {
            is ContaPessoaFisica -> contaASerSalva.fotoBase64 = base64String
            is ContaPessoaJuridica -> contaASerSalva.fotoBase64 = base64String
        }

        // 3. CHAMA O ÚLTIMO PASSO: SALVAR A CONTA FINALIZADA
        saveFinalAccountData(uid, contaASerSalva, dbRootNode)
    }


    /**
     * PASSO 3: Salva o objeto final da conta no Realtime Database e navega.
     */
    private fun saveFinalAccountData(uid: String, contaASerSalva: Any, dbRootNode: String) {
        // userRef aponta para o caminho correto:
        // PF: /usuarios/pessoaFisica/{UID}
        // PJ: /usuarios/pessoaJuridica/brechos/{UID} ou /usuarios/pessoaJuridica/instituicoes/{UID}
        val userRef = database.getReference(dbRootNode).child(uid)

        // Salva o objeto completo, incluindo o novo campo 'fotoBase64' e o ID do endereço
        userRef.setValue(contaASerSalva)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    Toast.makeText(requireContext(), "✅ Cadastro concluído!", Toast.LENGTH_LONG).show()
                    // SUCESSO FINAL: Navega para a tela principal (closet)
                    findNavController().navigate(R.id.action_global_closetFragment)
                } else {
                    Toast.makeText(requireContext(), "❌ Falha ao salvar dados finais da conta.", Toast.LENGTH_LONG).show()
                    result.exception?.printStackTrace()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}