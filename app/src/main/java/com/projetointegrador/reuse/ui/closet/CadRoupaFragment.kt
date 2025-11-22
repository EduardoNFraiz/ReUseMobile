package com.projetointegrador.reuse.ui.closet

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.projetointegrador.reuse.R
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentCadRoupaBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.displayBase64Image // Assumido que essa função existe

import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.joinToString

class CadRoupaFragment : Fragment() {
    private var _binding: FragmentCadRoupaBinding? = null
    private val binding get() = _binding!!

    // Variáveis do Firebase
    private lateinit var reference: DatabaseReference

    // Variáveis para Imagem
    private var imageUri: Uri? = null
    private var imageBase64: String? = null
    private var isImageSelected = false

    // Flag de controle de estado (visualização vs. edição ativa)
    private var isEditingActive = false

    // Objeto para armazenar os dados da peça, que será passado via Safe Args
    private var pecaEmAndamento: PecaCadastro = PecaCadastro()

    // Variáveis de contexto (inicializadas em onViewCreated)
    private var pecaUID: String? = null
    private var gavetaUID: String? = null
    private var isCreating: Boolean = true // Flag final de modo: true=Criação, false=Visualização/Edição

    // Safe Args
    private val args: CadRoupaFragmentArgs by navArgs()

    // ActivityResultLauncher para seleção de imagem (Mantido)
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.imageView2.setImageURI(uri)

                imageBase64 = convertImageUriToBase64(uri)

                if (imageBase64 != null) {
                    isImageSelected = true
                } else {
                    isImageSelected = false
                    Toast.makeText(requireContext(), "Erro ao converter imagem.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🛑 LÓGICA DE ARGUMENTOS REMOVIDA: Usaremos navArgs exclusivamente em onViewCreated.
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadRoupaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        barraDeNavegacao()

        reference = Firebase.database.reference

        // 🟢 INICIALIZAÇÃO CORRETA: Define as variáveis de estado de forma consistente,
        // usando apenas os Safe Args (args).
        pecaUID = args.pecaUID
        gavetaUID = args.gavetaUID
        isCreating = pecaUID.isNullOrEmpty()

        binding.imageView2.setOnClickListener {
            // Só permite abrir a galeria se o campo estiver habilitado
            if (it.isEnabled) {
                openImageChooser()
            }
        }

        if (isCreating) {
            // 🚀 MODO DE CRIAÇÃO (Novo Cadastro)
            binding.toolbar.title = "Cadastrar Peça (1/2)"
            binding.buttonEditar.visibility = View.GONE
            setFieldsEnabled(true)
            isImageSelected = false
            pecaEmAndamento = PecaCadastro()

            // O gavetaUID já foi definido corretamente acima a partir de args.gavetaUID

            binding.Proximo.setOnClickListener {
                handleNavigation(isEditingNow = false)
            }
        } else {
            // 👀 MODO DE VISUALIZAÇÃO/EDIÇÃO (A partir da Gaveta)
            binding.toolbar.title = "Visualizar Peça (1/2)"
            binding.buttonEditar.visibility = View.VISIBLE
            setFieldsEnabled(false)

            if (pecaUID != null) {
                loadPecaDetails(pecaUID!!)
            } else {
                Toast.makeText(requireContext(), "Erro de navegação: ID da peça ausente.", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            }

            // Configura o botão Editar/Cancelar
            binding.buttonEditar.setOnClickListener {
                isEditingActive = !isEditingActive
                setFieldsEnabled(isEditingActive)
                binding.buttonEditar.text = if (isEditingActive) "Cancelar Edição" else "Editar"
                binding.toolbar.title = if (isEditingActive) "Editar Peça (1/2)" else "Visualizar Peça (1/2)"

                if (!isEditingActive) {
                    // Se cancelou a edição, reverte os campos para o estado carregado (re-popula)
                    populateUIStage1(pecaEmAndamento)
                }
            }

            binding.Proximo.setOnClickListener {
                handleNavigation(isEditingActive)
            }
        }

        setupToolbarNavigation()
    }

    private fun setupToolbarNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackNavigation()
        }
    }

    /**
     * Gerencia a navegação de volta, usando o gavetaUID.
     */
    private fun handleBackNavigation() {
        // 🟢 CORREÇÃO CRUCIAL: Agora a variável de classe gavetaUID está garantida.
        val targetGavetaUID = gavetaUID

        if (!targetGavetaUID.isNullOrEmpty()) {

            // 1. Crie o Bundle com o UID da gaveta
            val bundle = Bundle().apply {
                // A chave deve ser a mesma que o GavetaFragment espera no seu onCreate (GAVETA_ID)
                putString("GAVETA_ID", targetGavetaUID)
            }

            // 2. Navega para o GavetaFragment
            findNavController().navigate(
                R.id.gavetaFragment, // ID do GavetaFragment
                bundle,
                // O NavOptions permite limpar a back stack.
                androidx.navigation.navOptions {
                    popUpTo(R.id.gavetaFragment) {
                        inclusive = true // Remove a própria instância antiga do GavetaFragment também, se estiver na pilha
                    }
                }
            )
        } else {
            // Se o UID da gaveta está ausente (erro), volta para a tela principal (Closet)
            Toast.makeText(requireContext(), "Erro: ID da gaveta ausente. Voltando para o Closet.", Toast.LENGTH_SHORT).show()
            // Assumindo que R.id.closetFragment está no nível mais alto ou é o ponto de partida seguro.
            findNavController().popBackStack(R.id.closetFragment, false)
        }
    }

    // --- LÓGICA DE CARREGAMENTO (EDIÇÃO) ---

    private fun loadPecaDetails(pecaId: String) {
        reference.child("pecas").child(pecaId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val peca = snapshot.getValue(PecaCadastro::class.java)

                    if (peca != null) {
                        pecaEmAndamento = peca // Armazena o objeto carregado
                        populateUIStage1(peca)
                    } else {
                        Toast.makeText(requireContext(), "Peça não encontrada.", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao carregar detalhes: ${error.message}", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            })
    }

    private fun populateUIStage1(peca: PecaCadastro) {
        // 1. IMAGEM
        if (!peca.fotoBase64.isNullOrEmpty()) {
            imageBase64 = peca.fotoBase64 // Garante que o Base64 carregado seja armazenado
            displayBase64Image(peca.fotoBase64!!, binding.imageView2)
            isImageSelected = true
        } else {
            binding.imageView2.setImageResource(R.drawable.baseline_image_24)
            isImageSelected = false
        }

        // 2. CORES (Checkboxes)
        val coresList = peca.cores?.split(", ")?.map { it.trim() } ?: emptyList()
        setCheckboxesState(binding.radioCores, coresList)

        // 3. CATEGORIA (Checkboxes)
        val categoriasList = peca.categoria?.split(", ")?.map { it.trim() } ?: emptyList()
        setCheckboxesState(binding.categoria, categoriasList)

        // 4. TAMANHO (RadioButton)
        setRadioButtonState(peca.tamanho)
    }

    // --- LÓGICA DE NAVEGAÇÃO E ATUALIZAÇÃO ---

    private fun handleNavigation(isEditingNow: Boolean) {
        if (isEditingNow || isCreating) {
            if (!validarDados()) return

            // Coleta dados da primeira etapa (editados ou novos)
            // 🛑 Garante que os campos não editados da PecaCadastro sejam preservados
            pecaEmAndamento = pecaEmAndamento.copy(

                // --- Campos Editáveis (Tela 1) ---
                fotoBase64 = imageBase64 ?: pecaEmAndamento.fotoBase64,
                cores = getSelecionarCores(),
                categoria = getSelecionarCategorias(),
                tamanho = getSelecionarTamanho(),

                // --- Campos do Cad2 (PRESERVAR O VALOR CARREGADO DO BANCO) ---
                finalidade = pecaEmAndamento.finalidade,
                preco = pecaEmAndamento.preco,
                titulo = pecaEmAndamento.titulo,
                detalhe = pecaEmAndamento.detalhe,

                // --- Campos de Sistema (PRESERVAR O VALOR CARREGADO DO BANCO) ---
                ownerUid = pecaEmAndamento.ownerUid,
                gavetaUid = pecaEmAndamento.gavetaUid
            )
        }

        // Define o gavetaUID de destino, usando a variável de classe que foi inicializada com args.gavetaUID
        val finalGavetaUID = gavetaUID

        // O ID da gaveta só é obrigatório se NÃO estivermos criando (edição/visualização)
        if (!isCreating && finalGavetaUID.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Erro: ID da gaveta não definido para prosseguir.", Toast.LENGTH_LONG).show()
            return
        }

        // Navega para CadRoupa2Fragment
        val action = CadRoupaFragmentDirections.actionCadRoupaFragmentToCadRoupa2Fragment(
            pecaEmAndamento,
            isCreating = isCreating, // Se é um novo cadastro
            isEditing = isEditingNow, // Se é uma edição ativa
            pecaUID = pecaUID, // O ID da peça (nulo se for criação)
            gavetaUID = finalGavetaUID, // O ID da gaveta original/destino
        )
        findNavController().navigate(action)
    }


    // --- Funções de Seleção de Imagem (Mantidas) ---

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    private fun convertImageUriToBase64(uri: Uri): String? {
        // ... (lógica mantida)
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Erro ao processar imagem (IO): ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Erro ao processar imagem (Geral): ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    // --- Funções de Validação e Coleta de Dados (Mantidas) ---

    private fun setFieldsEnabled(isEnabled: Boolean) {
        binding.radioCores.children.forEach { if (it is CheckBox) it.isEnabled = isEnabled }
        binding.categoria.children.forEach { if (it is CheckBox) it.isEnabled = isEnabled }
        binding.Tamanho.children.forEach { if (it is RadioButton) it.isEnabled = isEnabled }
        binding.imageView2.isEnabled = isEnabled
    }

    private fun isAnyCheckBoxChecked(viewGroup: ViewGroup): Boolean {
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                return true
            }
        }
        return false
    }

    private fun validarDados(): Boolean {
        // Regra: Se não está criando E não está editando ativamente, a validação é ignorada (só visualiza)
        if (!isCreating && !isEditingActive) return true

        // Se está criando ou editando ativamente, valida os campos
        if (!isImageSelected && imageBase64.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Por favor, clique no ícone para selecionar uma foto.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isAnyCheckBoxChecked(binding.radioCores)) {
            Toast.makeText(requireContext(), "Por favor, selecione ao menos uma cor.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isAnyCheckBoxChecked(binding.categoria)) {
            Toast.makeText(requireContext(), "Por favor, selecione ao menos uma categoria.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.Tamanho.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Por favor, selecione um tamanho.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun getSelecionarCores(): String {
        // ... (lógica mantida)
        val cores = mutableListOf<String>()
        for (i in 0 until binding.radioCores.childCount) {
            val view = binding.radioCores.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                cores.add(view.text.toString())
            }
        }
        return cores.joinToString(", ")
    }

    private fun getSelecionarCategorias(): String {
        // ... (lógica mantida)
        val categorias = mutableListOf<String>()
        for (i in 0 until binding.categoria.childCount) {
            val view = binding.categoria.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                categorias.add(view.text.toString())
            }
        }
        return categorias.joinToString(", ")
    }

    private fun getSelecionarTamanho(): String {
        // ... (lógica mantida)
        val checkedId = binding.Tamanho.checkedRadioButtonId
        if (checkedId != -1) {
            return when (checkedId) {
                binding.rbPP.id -> binding.rbPP.text.toString()
                binding.rbP.id -> binding.rbP.text.toString()
                binding.rbM.id -> binding.rbM.text.toString()
                binding.rbG.id -> binding.rbG.text.toString()
                binding.rbGG.id -> binding.rbGG.text.toString()
                binding.rbXGG.id -> binding.rbXGG.text.toString()
                else -> ""
            }
        }
        return ""
    }

    // --- Funções Auxiliares de Preenchimento (Mantidas) ---

    private fun setCheckboxesState(viewGroup: ViewGroup, selectedValues: List<String>) {
        viewGroup.children.forEach { view ->
            if (view is CheckBox) {
                view.isChecked = selectedValues.contains(view.text.toString())
            }
        }
    }

    private fun setRadioButtonState(tamanho: String?) {
        val selectedId = when (tamanho) {
            binding.rbPP.text.toString() -> binding.rbPP.id
            binding.rbP.text.toString() -> binding.rbP.id
            binding.rbM.text.toString() -> binding.rbM.id
            binding.rbG.text.toString() -> binding.rbG.id
            binding.rbGG.text.toString() -> binding.rbGG.id
            binding.rbXGG.text.toString() -> binding.rbXGG.id
            else -> -1
        }
        if (selectedId != -1) {
            binding.Tamanho.check(selectedId)
        }
    }

    // --- Navegação e Ciclo de Vida (Mantidas) ---

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

    // Extensão para simplificar o loop nos 'children' de um ViewGroup
    private val ViewGroup.children: List<View>
        get() = (0 until childCount).map { getChildAt(it) }
}