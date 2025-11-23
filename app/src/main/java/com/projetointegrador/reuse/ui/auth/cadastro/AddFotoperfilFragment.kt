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
import com.projetointegrador.reuse.data.model.Gaveta // Import necessário

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
                Toast.makeText(requireContext(), R.string.aviso_foto, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(),
                getString(R.string.error_usuario_nao_autenticado), Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val contaFisica = args.contaPessoaFisica
        val contaJuridica = args.contaPessoaJuridica
        val enderecoASerSalvo = args.endereco // Objeto Endereco

        val contaASerSalva = contaFisica ?: contaJuridica

        if (contaASerSalva == null) {
            Toast.makeText(requireContext(),
                getString(R.string.error_dados_de_conta_nao_encontrados), Toast.LENGTH_LONG).show()
            return
        }

        // --- 🎯 AJUSTE DO NÓ RAIZ COM BASE NO TIPO DE CONTA E TIPO DE USUÁRIO (Brechós/Instituições) ---
        val dbRootNode: String
        var isPessoaFisica = false // Adiciona a flag para simplificar a criação de gavetas

        if (contaFisica != null) {
            // Caminho para Pessoa Física: usuarios/pessoaFisica/{UID}
            dbRootNode = "usuarios/pessoaFisica"
            isPessoaFisica = true
        } else if (contaJuridica != null) {
            // Caminho para Pessoa Jurídica: usuarios/pessoaJuridica/{tipoUsuario_plural}/{UID}
            val tipoUsuario = contaJuridica.tipoUsuario // Deve ser "brecho" ou "instituicao"

            // Adiciona o plural 's' para o nó do banco de dados
            val tipoUsuarioPlural = when (tipoUsuario) {
                "brecho" -> "brechos"
                "instituicao" -> "instituicoes"
                else -> {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_tipo_de_usuario_pj_desconhecido), Toast.LENGTH_LONG).show()
                    return // Sai da função em caso de erro
                }
            }
            dbRootNode = "usuarios/pessoaJuridica/$tipoUsuarioPlural"
            isPessoaFisica = false
        } else {
            Toast.makeText(requireContext(),
                getString(R.string.error__tipo_de_conta_desconhecido), Toast.LENGTH_LONG).show()
            return
        }
        // ------------------------------------------------------------------------------------------

        // --- SALVAMENTO DO ENDEREÇO (ASYNC) ---

        // Assume-se que o Endereço será salvo em um nó separado para normalização
        val enderecoRef = database.getReference("enderecos").push()
        val enderecoId = enderecoRef.key

        if (enderecoId == null) {
            Toast.makeText(requireContext(),
                getString(R.string.error_gerar_id_para_o_endereco), Toast.LENGTH_LONG).show()
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
                    handlePhotoBase64AndSaveAccount(user.uid, contaASerSalva, dbRootNode, shouldUploadPhoto, isPessoaFisica)
                }

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    getString(R.string.falha_ao_salvar_endereco, e.message), Toast.LENGTH_LONG).show()
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
        shouldUploadPhoto: Boolean,
        isPessoaFisica: Boolean // Passa a flag para a próxima função
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
        saveFinalAccountData(uid, contaASerSalva, dbRootNode, isPessoaFisica)
    }


    /**
     * PASSO 3: Salva o objeto final da conta no Realtime Database e navega.
     * Corrigido: Agora chama createDefaultGavetas para PF e PJ, usando o caminho correto.
     */
    private fun saveFinalAccountData(uid: String, contaASerSalva: Any, dbRootNode: String, isPessoaFisica: Boolean) {
        // userRef aponta para o caminho correto:
        val userRef = database.getReference(dbRootNode).child(uid)

        // Salva o objeto completo, incluindo o novo campo 'fotoBase64' e o ID do endereço
        userRef.setValue(contaASerSalva)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    // 1. Lógica de Criação de Gavetas (para PF e PJ)
                    // Chamada universal com o nó raiz.
                    createDefaultGavetas(uid, dbRootNode)

                    // 2. Navegação
                    Toast.makeText(requireContext(),
                        getString(R.string.cadastro_concluido), Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_global_closetFragment)
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.falha_ao_salvar_dados_finais_da_conta), Toast.LENGTH_LONG).show()
                    result.exception?.printStackTrace()
                }
            }
    }

    /**
     * Função para criar as gavetas padrão (Vendas, Doação, Carrinho) e vincular ao usuário (PF ou PJ).
     * CORRIGIDO: Adiciona o 'ownerUid' à gaveta para o modelo desnormalizado.
     */
    private fun createDefaultGavetas(userId: String, userRootPath: String) { // <-- Recebe o path correto
        val defaultGavetas = listOf("Vendas", "Doação", "Carrinho", "Recebidos")
        val gavetasRef = database.getReference("gavetas")

        // Caminho da referência do usuário: {userRootPath}/{userId}/gavetas
        val userGavetasRef = database.getReference(userRootPath).child(userId).child("gavetas")

        for (gavetaName in defaultGavetas) {
            val gavetaUid = gavetasRef.push().key // Gera um UID único para a gaveta

            if (gavetaUid != null) {
                // ✅ CORREÇÃO APLICADA: Inclui o 'ownerUid' no objeto Gaveta para a estrutura desnormalizada
                val novaGaveta = Gaveta(
                    id = gavetaUid,
                    name = gavetaName,
                    ownerUid = userId, // <-- CRUCIAL: Vincula a gaveta ao usuário no nó /gavetas
                    fotoBase64 = "",
                    privado = true,
                )

                // 1. Salva a gaveta no nó principal: /gavetas/{gavetaUid}
                gavetasRef.child(gavetaUid).setValue(novaGaveta)
                    .addOnSuccessListener {
                        // 2. Salva a referência da gaveta no nó do usuário: {userRootPath}/{userId}/gavetas/{gavetaUid} = true
                        userGavetasRef.child(gavetaUid).setValue(true)
                    }
                    .addOnFailureListener { e ->
                        // Log em caso de falha, mas o fluxo principal de cadastro continua
                        Toast.makeText(requireContext(),
                            getString(R.string.error_falha_criar_gaveta, gavetaName), Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}