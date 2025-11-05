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
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentCadRoupaBinding
import com.projetointegrador.reuse.util.initToolbar
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.getValue

class CadRoupaFragment : Fragment() {
    private var _binding: FragmentCadRoupaBinding? = null
    private val binding get() = _binding!!

    // Variáveis para Imagem
    private var imageUri: Uri? = null
    private var imageBase64: String? = null
    private var isImageSelected = false

    // Flag para modo de edição
    private var editando = false

    private val args: CadRoupaFragmentArgs by navArgs()

    // Objeto para armazenar os dados da peça, que será passado via Safe Args
    private var pecaEmAndamento: PecaCadastro = PecaCadastro()

    // ActivityResultLauncher para seleção de imagem
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.imageView2.setImageURI(uri) // Exibe a imagem selecionada

                // Tenta converter para Base64 (USANDO A FUNÇÃO CORRIGIDA)
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

        val gavetauid = args.gavetaUID

        binding.imageView2.setOnClickListener {
            // Só permite abrir a galeria se o campo estiver habilitado
            if (it.isEnabled) {
                openImageChooser()
            }
        }

        // Obtém flags e ID da peça
        val isCreating = arguments?.getBoolean("CRIANDO_ROUPA") ?: false
        val pecaId = arguments?.getString("ROUPA_ID") // UID da peça se for edição/visualização

        if (isCreating) {
            // MODO DE CRIAÇÃO (Novo Cadastro)
            binding.buttonEditar.visibility = View.GONE
            setFieldsEnabled(true)
            isImageSelected = false // Imagem deve ser selecionada
            pecaEmAndamento = PecaCadastro()

            binding.Proximo.setOnClickListener {
                if (validarDados()) {
                    // Coleta dados da primeira etapa
                    pecaEmAndamento.apply {
                        fotoBase64 = imageBase64
                        cores = getSelecionarCores()
                        categoria = getSelecionarCategorias()
                        tamanho = getSelecionarTamanho()
                    }

                    // Navega para CadRoupa2Fragment
                    val action = CadRoupaFragmentDirections.actionCadRoupaFragmentToCadRoupa2Fragment(
                        pecaEmAndamento,
                        isCreating = true,
                        isEditing = false,
                        pecaUID = null,
                        gavetaUID = gavetauid,
                    )
                    findNavController().navigate(action)
                }
            }
        } else {
            // MODO DE VISUALIZAÇÃO/EDIÇÃO (A partir da Gaveta)
            binding.buttonEditar.visibility = View.VISIBLE
            setFieldsEnabled(false)

            // TODO: Aqui deveria vir a lógica para carregar a peça do Firebase usando pecaId
            // Por enquanto, assumimos que pecaEmAndamento e campos serão preenchidos.
            isImageSelected = true // Assume que a imagem já existe

            // Configura o botão Editar
            binding.buttonEditar.setOnClickListener {
                editando = !editando
                setFieldsEnabled(editando)
                binding.buttonEditar.text = if (editando) "Cancelar Edição" else "Editar"
                if (!editando) isImageSelected = true
            }

            binding.Proximo.setOnClickListener {
                val isEditingNow = editando

                if (isEditingNow) {
                    if (!validarDados()) return@setOnClickListener

                    // Atualiza o objeto com os dados editados
                    pecaEmAndamento.apply {
                        fotoBase64 = imageBase64 ?: pecaEmAndamento.fotoBase64 // Mantém o original se não alterado
                        cores = getSelecionarCores()
                        categoria = getSelecionarCategorias()
                        tamanho = getSelecionarTamanho()
                    }
                }

                // Navega para CadRoupa2Fragment, passando o UID
                val action = CadRoupaFragmentDirections.actionCadRoupaFragmentToCadRoupa2Fragment(
                    pecaEmAndamento,
                    isCreating = false,
                    isEditing = isEditingNow,
                    pecaUID = pecaId,
                    gavetaUID = gavetauid
                )
                findNavController().navigate(action)
            }
        }
    }

    // --- Funções de Seleção de Imagem ---

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    /**
     * CORREÇÃO CRÍTICA PARA O QUADRADO PRETO:
     * Converte a URI da imagem para Base64, aplicando compressão e garantindo Base64.NO_WRAP.
     */
    private fun convertImageUriToBase64(uri: Uri): String? {
        try {
            // 1. Obtém o Bitmap
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream()

            // 2. Comprime a imagem em JPEG com qualidade 80 (Otimização de tamanho/memória)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // 3. Codifica para Base64 usando a flag NO_WRAP (Essencial para strings limpas no Firebase)
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

    // --- Funções de Validação e Coleta de Dados ---

    /**
     * Habilita ou desabilita todos os campos de seleção do formulário.
     */
    private fun setFieldsEnabled(isEnabled: Boolean) {
        binding.radioCores.children.forEach { if (it is CheckBox) it.isEnabled = isEnabled }
        binding.categoria.children.forEach { if (it is CheckBox) it.isEnabled = isEnabled }
        binding.Tamanho.children.forEach { if (it is RadioButton) it.isEnabled = isEnabled }
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
        if (!isImageSelected) {
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

    // --- Funções Auxiliares de Coleta de Dados ---

    private fun getSelecionarCores(): String {
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