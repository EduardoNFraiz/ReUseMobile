package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentCadRoupa2Binding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.projetointegrador.reuse.util.MoneyTextWatcher

class CadRoupa2Fragment : Fragment() {
    private var _binding: FragmentCadRoupa2Binding? = null
    private val binding get() = _binding!!
    private val args: CadRoupa2FragmentArgs by navArgs()
    private lateinit var pecaEmAndamento: PecaCadastro
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Constantes
    private val gavetaDoarName = "Doação"
    private val gavetaVenderName = "Vendas"
    private val gavetasDeTransacao = listOf("Vendas", "Doação", "Carrinho", "Recebidos")

    // Variáveis de Estado
    private var gavetaOriginalUid: String? = null
    private var nameToUidMap: Map<String, String> = emptyMap()
    private var gavetaSelecionada: String? = null
    private var uidGavetaDoacao: String? = null
    private var uidGavetaVenda: String? = null
    private var isSavingPeca = false
    private lateinit var precoTextWatcher: MoneyTextWatcher

    // Adapters e NavOptions
    private val navOptionsPopToCad1 by lazy {
        androidx.navigation.navOptions {
            popUpTo(R.id.closetFragment) {
                inclusive = false
            }
        }
    }

    // --- Extensão para facilitar a atualização ---
    fun PecaCadastro.toMap(): Map<String, Any?> {
        return mapOf(
            "ownerUid" to ownerUid,
            "gavetaUid" to gavetaUid,
            "fotoBase64" to fotoBase64,
            "cores" to cores,
            "categoria" to categoria,
            "tamanho" to tamanho,
            "finalidade" to finalidade,
            "preco" to preco,
            "titulo" to titulo,
            "descricao" to descricao
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadRoupa2Binding.inflate(inflater, container, false)
        database = Firebase.database.reference
        auth = Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        // Mantenha a busca dos UIDs de transação separada, pois usa o orderByChild().equalTo() no nome.
        loadTransacaoGavetaUids()

        pecaEmAndamento = args.PecaCadastro
        setupViewMode()
        setupListeners()
        barraDeNavegacao()
        val editTextPreco = binding.editEditText
        precoTextWatcher = MoneyTextWatcher(editTextPreco)
        editTextPreco.addTextChangedListener(precoTextWatcher)
    }

    private fun loadTransacaoGavetaUids() {
        val ownerUid = auth.currentUser?.uid
        if (ownerUid == null) {
            Toast.makeText(requireContext(),
                getString(R.string.error_usuario_nao_logado), Toast.LENGTH_LONG).show()
            return
        }

        fetchGavetaUidByName(gavetaDoarName, ownerUid) { uid ->
            uidGavetaDoacao = uid
            if (uid == null) {
                Toast.makeText(requireContext(),
                    getString(R.string.aviso_gaveta_doacao_nao_encontrada), Toast.LENGTH_LONG).show()
            }
        }

        fetchGavetaUidByName(gavetaVenderName, ownerUid) { uid ->
            uidGavetaVenda = uid
            if (uid == null) {
                Toast.makeText(requireContext(),
                    getString(R.string.aviso_gaveta_vendas_nao_encontrada), Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Lógica de Visualização e Edição ---
    private fun setupViewMode() {
        val isCreating = args.isCreating
        val isEditing = args.isEditing
        val pecaUid = args.pecaUID

        gavetaOriginalUid = args.gavetaUID

        binding.btnCadastrarPeca.visibility = if (isCreating) View.VISIBLE else View.GONE
        binding.bttSalvar.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.trash2.visibility = if (!isCreating && pecaUid != null) View.VISIBLE else View.GONE

        val shouldEnableFields = isCreating || isEditing
        setFieldsEnabled(shouldEnableFields)

        if (!isCreating && pecaUid != null) {
            binding.toolbar.title = if (isEditing) getString(R.string.status_edit_peca2) else getString(
                R.string.status_visualizar_peca2
            )

            if (isEditing) {
                populateUIStage2(pecaEmAndamento)
            } else {
                loadPecaDetails(pecaUid)
            }

        } else if (isCreating) {
            binding.toolbar.title = getString(R.string.status_cadastrar_peca2)
            // Definições padrão para criação
            binding.radioButton5.isChecked = true // Organizar
            findCustomGavetas() // CHAMA A NOVA FUNÇÃO DE BUSCA DE GAVETAS CUSTOMIZADAS
            updatePrecoFieldsVisibility(getString(R.string.option_finalidade_organizar))
        }
    }

    private fun populateUIStage2(peca: PecaCadastro) {
        binding.editEditText.setText(peca.preco)
        binding.editTextTitulo.setText(peca.titulo)
        binding.editTextDetalhes.setText(peca.descricao)

        val finalidade = peca.finalidade ?: getString(R.string.option_finalidade_organizar)
        when (finalidade) {
            getString(R.string.option_finalidade_organizar) -> binding.radioButton5.isChecked = true
            getString(R.string.option_finalidade_doar) -> binding.radioButton6.isChecked = true
            getString(R.string.option_finalidade_vender) -> binding.radioButton7.isChecked = true
            else -> binding.radioButton5.isChecked = true
        }
        updatePrecoFieldsVisibility(finalidade)

        // Se a peça já está numa gaveta, tente pré-selecionar a gaveta dela.
        if (!peca.gavetaUid.isNullOrEmpty()) {
            gavetaOriginalUid = peca.gavetaUid
        }

        findCustomGavetas() // CHAMA A NOVA FUNÇÃO DE BUSCA DE GAVETAS CUSTOMIZADAS
    }

    private fun loadPecaDetails(pecaUid: String) {
        database.child("pecas").child(pecaUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val peca = snapshot.getValue(PecaCadastro::class.java)
                if (peca != null) {
                    pecaEmAndamento = peca

                    binding.editEditText.setText(peca.preco)
                    binding.editTextTitulo.setText(peca.titulo)
                    binding.editTextDetalhes.setText(peca.descricao)

                    val finalidade = peca.finalidade ?: getString(R.string.option_finalidade_organizar)
                    when (finalidade) {
                        getString(R.string.option_finalidade_organizar) -> binding.radioButton5.isChecked = true
                        getString(R.string.option_finalidade_doar) -> binding.radioButton6.isChecked = true
                        getString(R.string.option_finalidade_vender) -> binding.radioButton7.isChecked = true
                        else -> binding.radioButton5.isChecked = true
                    }
                    updatePrecoFieldsVisibility(finalidade)

                    findCustomGavetas() // CHAMA A NOVA FUNÇÃO DE BUSCA DE GAVETAS CUSTOMIZADAS

                } else {
                    showBottomSheet(message = getString(R.string.error_detalhes_peca_nao_encontrados))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showBottomSheet(message = getString(
                    R.string.error_ao_carregar_detalhes_peca,
                    error.message
                ))
            }
        })
    }

    private fun setFieldsEnabled(isEnabled: Boolean) {
        binding.radioButton5.isEnabled = isEnabled
        binding.radioButton6.isEnabled = isEnabled
        binding.radioButton7.isEnabled = isEnabled
        binding.spinner.isEnabled = isEnabled
        binding.editEditText.isEnabled = isEnabled
        binding.editTextTitulo.isEnabled = isEnabled
        binding.editTextDetalhes.isEnabled = isEnabled
    }

    private fun updatePrecoFieldsVisibility(finalidade: String) {
        val isVenda = finalidade == getString(R.string.option_finalidade_vender)
        binding.editEditText.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.editTextTitulo.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.editTextDetalhes.visibility = if (isVenda) View.VISIBLE else View.GONE
    }

    private fun updateSpinner(opcoes: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter
    }

    // --- FUNÇÕES DE BUSCA DE GAVETAS ---

    private fun findCustomGavetas() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), R.string.error_usuario_nao_logado, Toast.LENGTH_SHORT).show()
            updateSpinner(emptyList())
            return
        }
        fetchCustomGavetasByOwner(userId)
    }

    private fun fetchCustomGavetasByOwner(userId: String) {
        database.child("gavetas")
            .orderByChild("ownerUid")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaNames = mutableListOf<String>()
                    val mutableMap = mutableMapOf<String, String>()
                    var selectedPosition = -1

                    for (gavetaSnapshot in snapshot.children) {
                        val name = gavetaSnapshot.child("nome").getValue(String::class.java)
                        val uid = gavetaSnapshot.key

                        // Filtra gavetas de transação (mantém apenas as de Organização)
                        if (name != null && uid != null && !gavetasDeTransacao.contains(name)) {
                            gavetaNames.add(name)
                            mutableMap[name] = uid

                            // Pré-seleção: Verifica se este é o UID da gaveta original
                            if (uid == gavetaOriginalUid) {
                                selectedPosition = gavetaNames.size - 1
                            }
                        }
                    }

                    if (gavetaNames.isNotEmpty()) {
                        nameToUidMap = mutableMap.toMap()
                        updateSpinner(gavetaNames)
                        setupSpinnerListener()

                        // Aplica a pré-seleção
                        if (selectedPosition != -1) {
                            binding.spinner.setSelection(selectedPosition)
                            // O listener do spinner (setupSpinnerListener) já será acionado,
                            // mas garantimos que a gavetaSelecionada está correta.
                            gavetaSelecionada = gavetaOriginalUid
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.error_sem_gavetas_customizadas_organizacao, Toast.LENGTH_LONG).show()
                        updateSpinner(emptyList())
                        gavetaSelecionada = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(),
                        getString(R.string.erro_carregar_detalhes_das_gavetas, error.message), Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList())
                }
            })
    }

    private fun setupSpinnerListener() {
        // Desvincula o listener antes de re-atribuir para evitar chamadas duplas
        binding.spinner.onItemSelectedListener = null

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val nomeSelecionado = parent?.getItemAtPosition(position).toString()
                gavetaSelecionada = nameToUidMap[nomeSelecionado]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                gavetaSelecionada = null
            }
        }

        // Se houver opções, seleciona o primeiro item como padrão
        if (binding.spinner.adapter != null && binding.spinner.adapter.count > 0 && gavetaSelecionada.isNullOrBlank()) {
            val nomeSelecionado = binding.spinner.adapter.getItem(0).toString()
            gavetaSelecionada = nameToUidMap[nomeSelecionado]
        }
    }

    private fun fetchGavetaUidByName(
        gavetaName: String,
        ownerUid: String,
        onComplete: (String?) -> Unit
    ) {
        database.child("gavetas")
            .orderByChild("ownerUid")
            .equalTo(ownerUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaEncontrada = snapshot.children.firstOrNull {
                        it.child("nome").getValue(String::class.java) == gavetaName
                    }

                    val uid = gavetaEncontrada?.key
                    onComplete(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_ao_buscar_uid_da_gaveta, gavetaName, error.message), Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            })
    }

    // --- Setup de Listeners ---

    private fun setupListeners() {
        binding.Finalidade.setOnCheckedChangeListener { _, checkedId ->
            val finalidadeSelecionada = when (checkedId) {
                R.id.radioButton5 -> { // Organizar
                    findCustomGavetas() // Recarrega gavetas customizadas
                    getString(R.string.option_finalidade_organizar)
                }
                R.id.radioButton6 -> { // Doar
                    updateSpinner(listOf(gavetaDoarName))
                    gavetaSelecionada = uidGavetaDoacao
                    binding.spinner.onItemSelectedListener = null
                    getString(R.string.option_finalidade_doar)
                }
                R.id.radioButton7 -> { // Vender
                    updateSpinner(listOf(gavetaVenderName))
                    gavetaSelecionada = uidGavetaVenda
                    binding.spinner.onItemSelectedListener = null
                    getString(R.string.option_finalidade_vender)
                }
                else -> null
            }

            pecaEmAndamento.finalidade = finalidadeSelecionada
            if (finalidadeSelecionada != null) {
                updatePrecoFieldsVisibility(finalidadeSelecionada)
            }
        }

        binding.btnCadastrarPeca.setOnClickListener {
            if (!isSavingPeca && validarDados()) {
                isSavingPeca = true
                binding.btnCadastrarPeca.isEnabled = false
                collectFinalData()
                savePecaNoBanco(pecaEmAndamento, gavetaSelecionada)
            }
        }

        binding.bttSalvar.setOnClickListener {
            if (!isSavingPeca && validarDados()) {
                isSavingPeca = true
                binding.bttSalvar.isEnabled = false

                // ATUALIZA FINALIDADE ANTES DE COLETAR DADOS
                pecaEmAndamento.finalidade = when (binding.Finalidade.checkedRadioButtonId) {
                    R.id.radioButton5 -> getString(R.string.option_finalidade_organizar)
                    R.id.radioButton6 -> getString(R.string.option_finalidade_doar)
                    R.id.radioButton7 -> getString(R.string.option_finalidade_vender)
                    else -> pecaEmAndamento.finalidade
                }

                collectFinalData()

                val pecaUidExistente = args.pecaUID
                updatePecaNoBanco(pecaEmAndamento, pecaUidExistente, gavetaSelecionada, gavetaOriginalUid)
            }
        }

        binding.trash2.setOnClickListener {
            showBottomSheet(
                titleButton = R.string.showbottonsheet_btn_excluir,
                titleDialog = R.string.showbottonsheet_title_excluir_gaveta,
                message = getString(R.string.showbottonsheet_msg_excluir),
                onClick = {
                    val pecaUid = args.pecaUID
                    // O gavetaUid é necessário aqui apenas para navegação de volta, não para exclusão.
                    val gavetaUid = args.gavetaUID
                    if (pecaUid != null && gavetaUid != null) {
                        deletePeca(pecaUid, gavetaUid)
                    } else {
                        Toast.makeText(requireContext(),
                            getString(R.string.error_uid_peca_ou_gaveta_não_encontrado_p_exclusao), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // --- Funções de Persistência ---

    private fun savePecaNoBanco(peca: PecaCadastro, gavetaUid: String?) {
        if (gavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(),
                getString(R.string.error_uid_gaveta_invalido_nulo), Toast.LENGTH_SHORT).show()
            return
        }

        val pecaRef = database.child("pecas").push()
        val pecaUid = pecaRef.key

        // Garante que os UIDs estão no objeto antes de salvar em /pecas
        peca.gavetaUid = gavetaUid
        peca.ownerUid = auth.currentUser?.uid

        if (pecaUid != null) {
            // Salva no nó principal /pecas (único local)
            pecaRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 🛑 REMOVIDO: Vinculação à gaveta (database.child("gavetas")...)

                        val bundle = Bundle().apply {
                            putString("GAVETA_ID", gavetaUid)
                        }
                        Toast.makeText(requireContext(),
                            getString(R.string.sucesso_peca_cadastrada), Toast.LENGTH_SHORT).show()
                        findNavController().navigate(
                            R.id.action_cadRoupa2Fragment_to_gavetaFragment,
                            bundle,
                            navOptionsPopToCad1
                        )
                    } else {
                        isSavingPeca = false
                        binding.btnCadastrarPeca.isEnabled = true
                        Toast.makeText(requireContext(),
                            getString(
                                R.string.error_salvar_os_detalhes_da_peca,
                                task.exception?.message
                            ), Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(),
                getString(R.string.error_gerar_id_da_peca), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePecaNoBanco(peca: PecaCadastro, pecaUid: String?, novaGavetaUid: String?, gavetaAntigaUid: String?) {
        if (pecaUid.isNullOrBlank() || novaGavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.bttSalvar.isEnabled = true
            Toast.makeText(requireContext(),
                getString(R.string.error_uid_peca_ou_gaveta_invalido), Toast.LENGTH_SHORT).show()
            return
        }

        // Atualiza o objeto peca com o novo gavetaUid
        peca.gavetaUid = novaGavetaUid
        val pecaUpdates = peca.toMap()
        val pecaUpdateRef = database.child("pecas").child(pecaUid)

        // Atualiza a peça no nó principal /pecas
        pecaUpdateRef.updateChildren(pecaUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 🛑 REMOVIDO: Toda a lógica de desvincular/vincular gavetas.
                    // O novo gavetaUid já está no objeto peca salvo em /pecas.

                    Toast.makeText(requireContext(),
                        getString(R.string.sucesso_peca_atualizada), Toast.LENGTH_SHORT).show()

                    val bundle = Bundle().apply {
                        putString("GAVETA_ID", novaGavetaUid)
                    }
                    findNavController().navigate(
                        R.id.action_cadRoupa2Fragment_to_gavetaFragment,
                        bundle,
                        navOptionsPopToCad1
                    )
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_atualizar_peca, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
                isSavingPeca = false
                binding.bttSalvar.isEnabled = true
            }
    }

    private fun deletePeca(pecaUid: String, gavetaUid: String) {
        // 🛑 REMOVIDO: Não há índice para remover em /gavetas.
        // Remove a peça do nó principal /pecas
        database.child("pecas").child(pecaUid).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    getString(R.string.sucesso_peca_excluida), Toast.LENGTH_SHORT).show()
                val bundle = Bundle().apply {
                    putString("GAVETA_ID", gavetaUid) // Usa gavetaUid apenas para navegação de retorno
                }
                findNavController().navigate(
                    R.id.action_cadRoupa2Fragment_to_gavetaFragment,
                    bundle,
                    navOptionsPopToCad1
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_excluir_peca, it.message), Toast.LENGTH_SHORT).show()
            }
    }

    // --- Validação e Navegação ---
    private fun collectFinalData() {
        pecaEmAndamento.apply {
            // Usa o finalidade que foi atualizado no bttSalvar.setOnClickListener ou no setOnCheckedChangeListener
            if (finalidade == getString(R.string.option_finalidade_vender)) {
                preco = precoTextWatcher.getFormattedValueForSave()
                titulo = binding.editTextTitulo.text?.toString() ?: ""
                descricao = binding.editTextDetalhes.text?.toString() ?: ""
            } else {
                // Limpa campos se não for venda
                preco = ""
                titulo = ""
                descricao = ""
            }
        }
    }

    private fun validarDados(): Boolean {
        if (binding.Finalidade.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(),
                getString(R.string.aviso_selecione_finalidade), Toast.LENGTH_SHORT).show()
            return false
        }
        if (gavetaSelecionada.isNullOrBlank()) {
            Toast.makeText(requireContext(),
                getString(R.string.error_uid_gaveta_nao_encontrado_tente_novamente), Toast.LENGTH_LONG).show()
            return false
        }
        if (pecaEmAndamento.finalidade == getString(R.string.option_finalidade_vender)) {
            if (binding.editTextTitulo.text.isNullOrBlank()) {
                Toast.makeText(requireContext(),
                    getString(R.string.aviso_digite_um_titulo_peca), Toast.LENGTH_SHORT).show()
                return false
            }
            // Adiciona validação para o campo de preço formatado
            if (precoTextWatcher.getFormattedValueForSave().isBlank()) {
                Toast.makeText(requireContext(),
                    getString(R.string.aviso_digite_um_preco_peca), Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}