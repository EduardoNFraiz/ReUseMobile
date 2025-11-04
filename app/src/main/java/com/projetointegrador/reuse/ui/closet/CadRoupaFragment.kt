package com.projetointegrador.reuse.ui.closet

import android.app.Activity
import android.content.Intent
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
import com.projetointegrador.reuse.R
// Importe a sua data class PecaCadastro
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentCadRoupaBinding
import com.projetointegrador.reuse.util.initToolbar
import java.io.ByteArrayOutputStream
import java.io.IOException

class CadRoupaFragment : Fragment() {
    private var _binding: FragmentCadRoupaBinding? = null
    private val binding get() = _binding!!

    // Variáveis para Imagem
    private var imageUri: Uri? = null
    private var imageBase64: String? = null
    private var isImageSelected = false

    // Flag para modo de edição
    private var editando = false

    // Objeto para armazenar os dados da peça
    private var pecaEmAndamento: PecaCadastro = PecaCadastro()

    // ActivityResultLauncher para seleção de imagem
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.imageView2.setImageURI(uri) // Exibe a imagem selecionada

                // Tenta converter para Base64
                imageBase64 = convertImageUriToBase64(uri)

                if (imageBase64 != null) {
                    isImageSelected = true // Flag de validação
                } else {
                    isImageSelected = false
                    Toast.makeText(requireContext(), "Erro ao converter imagem.", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        // --- Configuração da Seleção de Imagem ---
        binding.imageView2.setOnClickListener {
            // Só permite abrir a galeria se o campo estiver habilitado
            if (it.isEnabled) {
                openImageChooser()
            }
        }

        // --- Lógica de Modos (Criando vs. Visualizando/Editando) ---
        val isCreating = arguments?.getBoolean("CRIANDO_ROUPA") ?: false
        val isVisualizing = arguments?.getBoolean("VISUALIZAR_INFO") ?: false

        if (isCreating) {
            // MODO DE CRIAÇÃO
            binding.buttonEditar.visibility = View.GONE
            setFieldsEnabled(true)
            isImageSelected = false // Começa falso, deve selecionar uma imagem
            pecaEmAndamento = PecaCadastro() // Garante um objeto limpo

            binding.Proximo.setOnClickListener {
                if (validarDados()) { // <-- VALIDAÇÃO OBRIGATÓRIA DA IMAGEM
                    // Coleta os dados e preenche o objeto
                    pecaEmAndamento.apply {
                        fotoBase64 = imageBase64
                        cores = getSelecionarCores()
                        categoria = getSelecionarCategorias()
                        tamanho = getSelecionarTamanho()
                        finalidade = ""
                        preco = ""
                        titulo = ""
                        detalhe = ""
                    }

                    // Navega usando Safe Args
                    val action = CadRoupaFragmentDirections.actionCadRoupaFragmentToCadRoupa2Fragment(pecaEmAndamento)
                    findNavController().navigate(action)
                }
            }

        } else if (isVisualizing) {
            // MODO DE VISUALIZAÇÃO/EDIÇÃO
            setFieldsEnabled(false)

            // TODO: Se esta tela recebe uma peça para visualizar/editar,
            // você deve recebê-la via Safe Args aqui e atribuir a 'pecaEmAndamento'.
            // Ex: val args: CadRoupaFragmentArgs by navArgs()
            //     pecaEmAndamento = args.pecaParaEditar
            // TODO: Preencher os campos (CheckBoxes, Radios) com os dados de 'pecaEmAndamento'

            isImageSelected = true // Assume que a imagem existe ao visualizar

            // Configura o botão Editar
            binding.buttonEditar.setOnClickListener {
                editando = !editando
                setFieldsEnabled(editando)

                // Se o usuário cancelar a edição, a imagem original é considerada selecionada
                if (!editando) {
                    isImageSelected = true
                }
            }

            // Botão Próximo
            binding.Proximo.setOnClickListener {
                if (editando) {
                    // Se está editando, valida os dados (incluindo imagem)
                    if (validarDados()) {
                        // Atualiza o objeto com os dados editados
                        pecaEmAndamento.apply {
                            fotoBase64 = imageBase64
                            cores = getSelecionarCores()
                            categoria = getSelecionarCategorias()
                            tamanho = getSelecionarTamanho()
                            finalidade = ""
                            preco = ""
                            titulo = ""
                            detalhe = ""
                        }

                        // Navega usando Safe Args
                        val action = CadRoupaFragmentDirections.actionCadRoupaFragmentToCadRoupa2Fragment(pecaEmAndamento)
                        findNavController().navigate(action)
                    }
                } else {
                    // Se está apenas visualizando, avança com os dados existentes
                    val action = CadRoupaFragmentDirections.actionCadRoupaFragmentToCadRoupa2Fragment(pecaEmAndamento)
                    findNavController().navigate(action)
                }
            }
        }
    }

    // --- Funções de Seleção de Imagem ---

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    private fun convertImageUriToBase64(uri: Uri): String? {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            // Comprime a imagem em JPEG. 80 é a qualidade.
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Erro ao processar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    // --- Funções de Validação e Coleta de Dados ---

    /**
     * Habilita ou desabilita todos os campos de seleção do formulário.
     */
    private fun setFieldsEnabled(isEnabled: Boolean) {
        // Cores
        binding.radioCores.children.forEach { if (it is CheckBox) it.isEnabled = isEnabled }
        // Categoria
        binding.categoria.children.forEach { if (it is CheckBox) it.isEnabled = isEnabled }
        // Tamanho
        binding.Tamanho.children.forEach { if (it is RadioButton) it.isEnabled = isEnabled }
        // Habilita a seleção de imagem
        binding.imageView2.isEnabled = isEnabled
    }

    /**
     * Verifica se algum CheckBox dentro de um ViewGroup (como GridLayout) está marcado.
     */
    private fun isAnyCheckBoxChecked(viewGroup: ViewGroup): Boolean {
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                return true
            }
        }
        return false
    }

    /**
     * Valida os dados obrigatórios do formulário, incluindo a imagem.
     */
    private fun validarDados(): Boolean {
        // 1. VALIDAÇÃO OBRIGATÓRIA DA IMAGEM
        if (!isImageSelected) {
            Toast.makeText(requireContext(), "Por favor, clique no ícone para selecionar uma foto.", Toast.LENGTH_SHORT).show()
            return false
        }

        // 2. Validação das Cores
        if (!isAnyCheckBoxChecked(binding.radioCores)) {
            Toast.makeText(requireContext(), "Por favor, selecione ao menos uma cor.", Toast.LENGTH_SHORT).show()
            return false
        }

        // 3. Validação da Categoria
        if (!isAnyCheckBoxChecked(binding.categoria)) {
            Toast.makeText(requireContext(), "Por favor, selecione ao menos uma categoria.", Toast.LENGTH_SHORT).show()
            return false
        }

        // 4. Validação do Tamanho
        if (binding.Tamanho.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Por favor, selecione um tamanho.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true // Todos os campos são válidos
    }

    // --- Funções Auxiliares de Coleta de Dados ---

    private fun getSelecionarCores(): String {
        val cores = mutableListOf<String>()
        for (i in 0 until binding.radioCores.childCount) {
            val view = binding.radioCores.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                cores.add(view.text.toString())
            }
        }
        // Retorna uma string separada por vírgula, ex: "Branco, Azul, Preto"
        return cores.joinToString(", ")
    }

    private fun getSelecionarCategorias(): String {
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

    // --- Navegação e Ciclo de Vida ---

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.closet) }
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