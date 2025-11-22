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
    private val gavetaDoar = listOf("Doação")
    private val gavetaVender = listOf("Vendas")
    private val gavetasDeTransacao = listOf("Vendas", "Doação", "Carrinho", "Recebidos")
    private var gavetaOriginalUid: String? = null
    private var nameToUidMap: Map<String, String> = emptyMap()
    private var gavetaSelecionada: String? = null
    private var uidGavetaDoacao: String? = null
    private var uidGavetaVenda: String? = null
    private var isSavingPeca = false
    private var gavetaOptionsList: List<String> = emptyList()
    private lateinit var precoTextWatcher: MoneyTextWatcher
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
            "detalhe" to detalhe
        )
    }
    private val navOptionsPopToCad1 by lazy {
        androidx.navigation.navOptions {
            popUpTo(R.id.closetFragment) {
                inclusive = false
            }
        }
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
            Toast.makeText(requireContext(), "Erro: Usuário não logado.", Toast.LENGTH_LONG).show()
            return
        }

        fetchGavetaUidByName(gavetaDoar.first(), ownerUid) { uid ->
            uidGavetaDoacao = uid
            if (uid == null) {
                Toast.makeText(requireContext(), "Aviso: Gaveta 'Doação' não encontrada para este usuário.", Toast.LENGTH_LONG).show()
            }
        }

        fetchGavetaUidByName(gavetaVender.first(), ownerUid) { uid ->
            uidGavetaVenda = uid
            if (uid == null) {
                Toast.makeText(requireContext(), "Aviso: Gaveta 'Vendas' não encontrada para este usuário.", Toast.LENGTH_LONG).show()
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
            binding.toolbar.title = if (isEditing) "Editar Peça (2/2)" else "Visualizar Peça (2/2)"

            if (isEditing) {
                populateUIStage2(pecaEmAndamento)
            } else {
                loadPecaDetails(pecaUid)
            }

        } else if (isCreating) {
            binding.toolbar.title = "Cadastrar Peça (2/2)"
            // Definições padrão para criação
            binding.radioButton5.isChecked = true
            findUserAccountType()
            updatePrecoFieldsVisibility(getString(R.string.app_organizar))
        }
    }

    private fun populateUIStage2(peca: PecaCadastro) {
        binding.editEditText.setText(peca.preco)
        binding.editTextTitulo.setText(peca.titulo)
        binding.editTextDetalhes.setText(peca.detalhe)

        val finalidade = peca.finalidade ?: getString(R.string.app_organizar)
        when (finalidade) {
            getString(R.string.app_organizar) -> binding.radioButton5.isChecked = true
            getString(R.string.app_doar) -> binding.radioButton6.isChecked = true
            getString(R.string.app_vender) -> binding.radioButton7.isChecked = true
            else -> binding.radioButton5.isChecked = true
        }
        updatePrecoFieldsVisibility(finalidade)

        // Se a peça já está numa gaveta, tente pré-selecionar a gaveta dela.
        if (!peca.gavetaUid.isNullOrEmpty()) {
            gavetaOriginalUid = peca.gavetaUid
        }

        findUserAccountType()
    }
    private fun loadPecaDetails(pecaUid: String) {
        database.child("pecas").child(pecaUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val peca = snapshot.getValue(PecaCadastro::class.java)
                if (peca != null) {
                    pecaEmAndamento = peca

                    binding.editEditText.setText(peca.preco)
                    binding.editTextTitulo.setText(peca.titulo)
                    binding.editTextDetalhes.setText(peca.detalhe)

                    val finalidade = peca.finalidade ?: getString(R.string.app_organizar)
                    when (finalidade) {
                        getString(R.string.app_organizar) -> binding.radioButton5.isChecked = true
                        getString(R.string.app_doar) -> binding.radioButton6.isChecked = true
                        getString(R.string.app_vender) -> binding.radioButton7.isChecked = true
                        else -> binding.radioButton5.isChecked = true
                    }
                    updatePrecoFieldsVisibility(finalidade)

                    findUserAccountType()

                } else {
                    showBottomSheet(message = "Detalhes da peça não encontrados.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showBottomSheet(message = "Erro ao carregar detalhes da peça: ${error.message}")
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
        val isVenda = finalidade == getString(R.string.app_vender)
        binding.editEditText.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.editTextTitulo.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.editTextDetalhes.visibility = if (isVenda) View.VISIBLE else View.GONE
    }

    private fun updateSpinner(opcoes: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter
    }

    private fun findUserAccountType() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado. Carregando gavetas padrão.", Toast.LENGTH_SHORT).show()
            updateSpinner(emptyList())
            return
        }

        database.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        getGavetaUidsFromUser(userId, "pessoaFisica", null)
                    } else {
                        searchPessoaJuridicaForGavetaUids(userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao buscar tipo de conta: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList())
                }
            })
    }

    private fun searchPessoaJuridicaForGavetaUids(userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false
        var subtiposChecados = 0
        val totalSubtipos = subtipos.size

        for (subtipo in subtipos) {
            database.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        subtiposChecados++
                        if (snapshot.exists() && !found) {
                            found = true
                            getGavetaUidsFromUser(userId, "pessoaJuridica", subtipo)
                            return
                        }
                        if (subtiposChecados == totalSubtipos && !found) {
                            Toast.makeText(requireContext(), "Nenhuma gaveta encontrada ou tipo de conta não identificado.", Toast.LENGTH_LONG).show()
                            updateSpinner(emptyList())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        subtiposChecados++
                        Toast.makeText(requireContext(), "Erro ao buscar subtipo: ${error.message}", Toast.LENGTH_LONG).show()
                        if (subtiposChecados == totalSubtipos && !found) {
                            updateSpinner(emptyList())
                        }
                    }
                })
        }
    }

    private fun getGavetaUidsFromUser(userId: String, tipoConta: String, subtipo: String?) {
        val path = if (tipoConta == "pessoaFisica") {
            "usuarios/pessoaFisica/$userId/gavetas"
        } else {
            "usuarios/pessoaJuridica/$subtipo/$userId/gavetas"
        }

        database.child(path)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaUids = mutableListOf<String>()
                    for (gavetaSnapshot in snapshot.children) {
                        gavetaUids.add(gavetaSnapshot.key!!)
                    }

                    if (gavetaUids.isNotEmpty()) {
                        fetchAllGavetaDetails(gavetaUids)
                    } else {
                        Toast.makeText(requireContext(), "Você não possui gavetas customizadas de organização.", Toast.LENGTH_LONG).show()
                        updateSpinner(emptyList())
                        gavetaSelecionada = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao listar UIDs das gavetas: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList())
                }
            })
    }

    private fun fetchAllGavetaDetails(gavetaUids: List<String>) {
        database.child("gavetas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaNames = mutableListOf<String>()
                    val mutableMap = mutableMapOf<String, String>()
                    var selectedPosition = -1

                    // Itera sobre os UIDs do usuário
                    for (i in gavetaUids.indices) {
                        val uid = gavetaUids[i]
                        // Busca a gaveta específica no snapshot global
                        val gavetaSnapshot = snapshot.child(uid)
                        val name = gavetaSnapshot.child("name").getValue(String::class.java)

                        // Filtra gavetas de transação (mantém apenas as de Organização)
                        if (name != null && !gavetasDeTransacao.contains(name)) {
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
                        gavetaOptionsList = gavetaNames // Armazena a lista de nomes
                        updateSpinner(gavetaNames)
                        setupSpinnerListener()

                        // Aplica a pré-seleção
                        if (selectedPosition != -1) {
                            binding.spinner.setSelection(selectedPosition)
                            gavetaSelecionada = gavetaOriginalUid // Confirma o UID selecionado
                        }
                    } else {
                        Toast.makeText(requireContext(), "Nenhuma gaveta de organização válida encontrada.", Toast.LENGTH_LONG).show()
                        updateSpinner(emptyList())
                        gavetaSelecionada = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao carregar detalhes das gavetas: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList())
                }
            })
    }

    private fun setupSpinnerListener() {
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

        if (binding.spinner.adapter != null && binding.spinner.adapter.count > 0) {
            val nomeSelecionado = binding.spinner.adapter.getItem(0).toString()
            gavetaSelecionada = nameToUidMap[nomeSelecionado]
        } else {
            gavetaSelecionada = null
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
                        it.child("name").getValue(String::class.java) == gavetaName
                    }

                    val uid = gavetaEncontrada?.key
                    onComplete(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao buscar UID da gaveta '$gavetaName': ${error.message}", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            })
    }

    // --- Setup de Listeners ---

    private fun setupListeners() {
        binding.Finalidade.setOnCheckedChangeListener { _, checkedId ->
            val finalidadeSelecionada = when (checkedId) {
                R.id.radioButton5 -> {
                    findUserAccountType()
                    getString(R.string.app_organizar)
                }
                R.id.radioButton6 -> {
                    updateSpinner(gavetaDoar)
                    gavetaSelecionada = uidGavetaDoacao
                    binding.spinner.onItemSelectedListener = null
                    // CORREÇÃO: Use R.string.app_doar
                    getString(R.string.app_doar)
                }
                R.id.radioButton7 -> {
                    updateSpinner(gavetaVender)
                    gavetaSelecionada = uidGavetaVenda
                    binding.spinner.onItemSelectedListener = null
                    getString(R.string.app_vender)
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
                pecaEmAndamento.finalidade = when (binding.Finalidade.checkedRadioButtonId) {
                    R.id.radioButton5 -> getString(R.string.app_organizar)
                    R.id.radioButton6 -> getString(R.string.app_doar)
                    R.id.radioButton7 -> getString(R.string.app_vender)
                    else -> pecaEmAndamento.finalidade
                }

                collectFinalData()

                val pecaUidExistente = args.pecaUID
                updatePecaNoBanco(pecaEmAndamento, pecaUidExistente, gavetaSelecionada, gavetaOriginalUid)
            }
        }

        binding.trash2.setOnClickListener {
            showBottomSheet(
                titleButton = R.string.excluir,
                titleDialog = R.string.deseja_excluir,
                message = getString(R.string.click_para_excluir),
                onClick = {
                    val pecaUid = args.pecaUID
                    val gavetaUid = args.gavetaUID
                    if (pecaUid != null && gavetaUid != null) {
                        deletePeca(pecaUid, gavetaUid)
                    } else {
                        Toast.makeText(requireContext(), "Erro: ID da peça ou gaveta não encontrado para exclusão.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // --- Funções de Persistência (AJUSTADAS) ---

    private fun savePecaNoBanco(peca: PecaCadastro, gavetaUid: String?) {
        if (gavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(), "Erro: ID da gaveta de destino é inválido ou nulo.", Toast.LENGTH_SHORT).show()
            return
        }

        val pecaRef = database.child("pecas").push()
        val pecaUid = pecaRef.key
        peca.gavetaUid = gavetaUid
        peca.ownerUid = auth.currentUser?.uid

        if (pecaUid != null) {
            pecaRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val bundle = Bundle().apply {
                            putString("GAVETA_ID", gavetaUid)
                        }
                        findNavController().navigate(
                            R.id.action_cadRoupa2Fragment_to_gavetaFragment,
                            bundle,
                            navOptionsPopToCad1
                        )
                    } else {
                        isSavingPeca = false
                        binding.btnCadastrarPeca.isEnabled = true
                        Toast.makeText(requireContext(), "Erro ao salvar os detalhes da peça: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(), "Erro ao gerar ID da peça.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePecaNoBanco(peca: PecaCadastro, pecaUid: String?, novaGavetaUid: String?, gavetaAntigaUid: String?) {
        if (pecaUid.isNullOrBlank() || novaGavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.bttSalvar.isEnabled = true
            Toast.makeText(requireContext(), "Erro: ID da peça ou gaveta inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        peca.gavetaUid = novaGavetaUid
        val pecaUpdates = peca.toMap()
        val pecaUpdateRef = database.child("pecas").child(pecaUid)

        pecaUpdateRef.updateChildren(pecaUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (gavetaAntigaUid != null && gavetaAntigaUid != novaGavetaUid) {
                        database.child("gavetas").child(gavetaAntigaUid).child("peças").child(pecaUid).removeValue()
                        database.child("gavetas").child(novaGavetaUid).child("peças").child(pecaUid).setValue(true)
                    }
                    Toast.makeText(requireContext(), "Peça atualizada com sucesso!", Toast.LENGTH_SHORT).show()

                    val bundle = Bundle().apply {
                        putString("GAVETA_ID", novaGavetaUid)
                    }
                    findNavController().navigate(
                        R.id.action_cadRoupa2Fragment_to_gavetaFragment,
                        bundle,
                        navOptionsPopToCad1
                    )
                } else {
                    Toast.makeText(requireContext(), "Erro ao atualizar a peça: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
                isSavingPeca = false
                binding.bttSalvar.isEnabled = true
            }
    }

    private fun deletePeca(pecaUid: String, gavetaUid: String) {
        database.child("gavetas").child(gavetaUid).child("peças").child(pecaUid).removeValue()
            .addOnSuccessListener {
                database.child("pecas").child(pecaUid).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Peça excluída com sucesso!", Toast.LENGTH_SHORT).show()
                        val bundle = Bundle().apply {
                            putString("GAVETA_ID", gavetaUid)
                        }
                        findNavController().navigate(
                            R.id.action_cadRoupa2Fragment_to_gavetaFragment,
                            bundle,
                            navOptionsPopToCad1
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao excluir peça: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao desvincular peça da gaveta: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Validação e Navegação ---
    private fun collectFinalData() {
        pecaEmAndamento.apply {
            if (finalidade == getString(R.string.app_vender)) {
                preco = precoTextWatcher.getFormattedValueForSave()
                titulo = binding.editTextTitulo.text?.toString() ?: ""
                detalhe = binding.editTextDetalhes.text?.toString() ?: ""
            } else {
                preco = ""
                titulo = ""
                detalhe = ""
            }
        }
    }

    private fun validarDados(): Boolean {
        if (binding.Finalidade.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Por favor, selecione a Finalidade da peça.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (gavetaSelecionada.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Erro de seleção: ID da gaveta não foi encontrado. Por favor, selecione novamente e aguarde o carregamento do UID.", Toast.LENGTH_LONG).show()
            return false
        }
        if (pecaEmAndamento.finalidade == getString(R.string.app_vender)) {
            if (binding.editTextTitulo.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Por favor, insira um Título para a peça.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (binding.editEditText.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Por favor, insira o Preço de venda.", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
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