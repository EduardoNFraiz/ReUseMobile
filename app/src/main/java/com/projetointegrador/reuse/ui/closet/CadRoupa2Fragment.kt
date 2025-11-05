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
import com.projetointegrador.reuse.data.model.PecaCadastro // Suponha que esta classe exista
import com.projetointegrador.reuse.data.model.Gaveta // Adicionado para compilar as transações e lógica de gaveta
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
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

// ⚠️ Adicionei um import dummy para a classe Gaveta, necessária para a classe Transaction.
// Você precisa ter esta classe no seu projeto, mesmo que ela não seja usada diretamente aqui.
// data class Gaveta(val name: String? = null, var number: String? = null)

class CadRoupa2Fragment : Fragment() {
    private var _binding: FragmentCadRoupa2Binding? = null
    private val binding get() = _binding!!

    private val args: CadRoupa2FragmentArgs by navArgs()
    private lateinit var pecaEmAndamento: PecaCadastro

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val gavetaDoar = listOf("Doação")
    private val gavetaVender = listOf("Vendas")
    // Manter a lista de transação para filtragem na função fetchAllGavetaDetails
    private val gavetasDeTransacao = listOf("Vendas", "Doação", "Carrinho")

    // Adicionado para manter a referência à gaveta original na edição
    private var gavetaOriginalUid: String? = null

    private var nameToUidMap: Map<String, String> = emptyMap()
    private var gavetaSelecionada: String? = null // UID da gaveta de destino

    // Armazenamento pré-carregado dos UIDs de transação
    private var uidGavetaDoacao: String? = null
    private var uidGavetaVenda: String? = null

    private var isSavingPeca = false

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
    }

    // --- FUNÇÕES DE INCREMENTO E DECREMENTO (Transações Atômicas - ADICIONADO) ---

    private fun incrementaContadorGaveta(gavetaId: String) {
        val gavetaNumberRef = database.child("gavetas").child(gavetaId).child("number")

        gavetaNumberRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentNumberStr = mutableData.getValue(String::class.java)
                val currentNumberInt = currentNumberStr?.toIntOrNull() ?: 0
                val newNumberInt = currentNumberInt + 1

                mutableData.value = newNumberInt.toString()
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (!committed) {
                    Toast.makeText(requireContext(), "Aviso: Falha ao incrementar contador da gaveta. Erro: ${databaseError?.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun decrementaContadorGaveta(gavetaId: String) {
        val gavetaNumberRef = database.child("gavetas").child(gavetaId).child("number")

        gavetaNumberRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentNumberStr = mutableData.getValue(String::class.java)
                val currentNumberInt = currentNumberStr?.toIntOrNull() ?: 0

                val newNumberInt = if (currentNumberInt > 0) currentNumberInt - 1 else 0

                mutableData.value = newNumberInt.toString()
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (!committed) {
                    Toast.makeText(requireContext(), "Aviso: Falha ao decrementar contador da gaveta. Erro: ${databaseError?.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    // --- NOVO: Carregamento de UIDs de Gavetas Fixas ---

    private fun loadTransacaoGavetaUids() {
        fetchGavetaUidByName(gavetaDoar.first()) { uid ->
            uidGavetaDoacao = uid
            if (uid == null) {
                // Toast.makeText(requireContext(), "Aviso: Gaveta 'Doação' não encontrada no banco.", Toast.LENGTH_LONG).show()
            }
        }

        fetchGavetaUidByName(gavetaVender.first()) { uid ->
            uidGavetaVenda = uid
            if (uid == null) {
                // Toast.makeText(requireContext(), "Aviso: Gaveta 'Vendas' não encontrada no banco.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Lógica de Visualização e Edição (CORRIGIDO) ---

    private fun setupViewMode() {
        val isCreating = args.isCreating
        val isEditing = args.isEditing
        val pecaUid = args.pecaUID

        // Armazena o UID da gaveta original ANTES da edição (necessário para decrementar se a gaveta mudar)
        gavetaOriginalUid = args.gavetaUID

        binding.btnCadastrarPeca.visibility = if (isCreating) View.VISIBLE else View.GONE
        binding.bttSalvar.visibility = if (isEditing) View.VISIBLE else View.GONE
        // A lixeira só aparece se não estiver criando ou editando, mas a peça deve existir
        binding.trash2.visibility = if (!isCreating && !isEditing && pecaUid != null) View.VISIBLE else View.GONE

        val shouldEnableFields = isCreating || isEditing
        setFieldsEnabled(shouldEnableFields)

        if (!isCreating && pecaUid != null) {
            // Se for edição/visualização, carregar os dados completos
            findUserAccountType()
            loadPecaDetails(pecaUid, gavetaOriginalUid) // ADICIONADO
        } else if (isCreating) {
            binding.radioButton5.isChecked = true
            findUserAccountType()
            updatePrecoFieldsVisibility(getString(R.string.app_organizar))
        }
    }

    // NOVO: Carregar detalhes da peça para edição/visualização
    private fun loadPecaDetails(pecaUid: String, gavetaUid: String?) {
        database.child("pecas").child(pecaUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val peca = snapshot.getValue(PecaCadastro::class.java)
                if (peca != null) {
                    pecaEmAndamento = peca

                    // Preenche campos da UI
                    binding.editEditText.setText(peca.preco)
                    binding.editTextTitulo.setText(peca.titulo)
                    binding.editTextDetalhes.setText(peca.detalhe)

                    // Seta a finalidade correta
                    when (peca.finalidade) {
                        getString(R.string.app_organizar) -> binding.radioButton5.isChecked = true
                        getString(R.string.app_doar) -> binding.radioButton6.isChecked = true
                        getString(R.string.app_vender) -> binding.radioButton7.isChecked = true
                        else -> binding.radioButton5.isChecked = true
                    }

                    // Define a gaveta correta no spinner (após o findUserAccountType carregar as opções)
                    gavetaSelecionada = gavetaUid

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
        binding.textview6.visibility = if (isVenda) View.VISIBLE else View.GONE
    }

    private fun updateSpinner(opcoes: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter
    }

    // --- Lógica de Carregamento de Gavetas (MANTEVE A LÓGICA DO SEU CÓDIGO) ---

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

    // MANTEVE A LÓGICA DO SEU CÓDIGO: Busca detalhes de todas as gavetas de uma vez e filtra
    private fun fetchAllGavetaDetails(gavetaUids: List<String>) {
        database.child("gavetas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaNames = mutableListOf<String>()
                    val mutableMap = mutableMapOf<String, String>()

                    for (uid in gavetaUids) {
                        val gavetaSnapshot = snapshot.child(uid)
                        val name = gavetaSnapshot.child("name").getValue(String::class.java)

                        // Filtra gavetas de transação (Vendas, Doação, Carrinho)
                        if (name != null && !gavetasDeTransacao.contains(name)) {
                            gavetaNames.add(name)
                            mutableMap[name] = uid
                        }
                    }

                    if (gavetaNames.isNotEmpty()) {
                        nameToUidMap = mutableMap.toMap()
                        updateSpinner(gavetaNames)
                        setupSpinnerListener()
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

    // MANTEVE A LÓGICA DO SEU CÓDIGO
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

    private fun fetchGavetaUidByName(gavetaName: String, onComplete: (String?) -> Unit) {
        database.child("gavetas")
            .orderByChild("name")
            .equalTo(gavetaName)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val uid = snapshot.children.firstOrNull()?.key
                    onComplete(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao buscar UID da gaveta '$gavetaName': ${error.message}", Toast.LENGTH_LONG).show()
                    onComplete(null)
                }
            })
    }

    // --- Setup de Listeners (CORRIGIDO) ---

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
                collectFinalData()
                val pecaUidExistente = args.pecaUID
                updatePecaNoBanco(pecaEmAndamento, pecaUidExistente, gavetaSelecionada, gavetaOriginalUid) // ADICIONADO gavetaOriginalUid
            }
        }

        binding.trash2.setOnClickListener {
            showBottomSheet(
                titleButton = R.string.excluir,
                titleDialog = R.string.deseja_excluir,
                message = getString(R.string.click_para_excluir),
                onClick = { // ADICIONADO: Lógica de exclusão dentro do showBottomSheet
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

    // --- Funções de Persistência (CORRIGIDO/ADICIONADO) ---

    private fun savePecaNoBanco(peca: PecaCadastro, gavetaUid: String?) {
        if (gavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(), "Erro: ID da gaveta de destino é inválido ou nulo.", Toast.LENGTH_SHORT).show()
            return
        }

        val pecaRef = database.child("pecas").push()
        val pecaUid = pecaRef.key

        if (pecaUid != null) {
            pecaRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        database.child("gavetas")
                            .child(gavetaUid)
                            .child("peças")
                            .child(pecaUid)
                            .setValue(true)
                            .addOnCompleteListener { gavetaTask ->
                                isSavingPeca = false
                                binding.btnCadastrarPeca.isEnabled = true
                                if (gavetaTask.isSuccessful) {
                                    incrementaContadorGaveta(gavetaUid) // ADICIONADO CHAMADA

                                    Toast.makeText(requireContext(), "Peça cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    val bundle = Bundle().apply {
                                        putString("GAVETA_ID", gavetaUid)
                                    }
                                    findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment, bundle)
                                } else {
                                    Toast.makeText(requireContext(), "Erro ao vincular à gaveta: ${gavetaTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
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

    // CORRIGIDO: Adiciona gavetaAntigaUid para tratar a troca de gavetas e contadores
    private fun updatePecaNoBanco(peca: PecaCadastro, pecaUid: String?, novaGavetaUid: String?, gavetaAntigaUid: String?) {
        if (pecaUid.isNullOrBlank() || novaGavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.bttSalvar.isEnabled = true
            Toast.makeText(requireContext(), "Erro: ID da peça ou gaveta inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        val pecaUpdateRef = database.child("pecas").child(pecaUid)
        pecaUpdateRef.setValue(peca)
            .addOnCompleteListener { task ->
                isSavingPeca = false
                binding.bttSalvar.isEnabled = true
                if (task.isSuccessful) {

                    if (gavetaAntigaUid != null && gavetaAntigaUid != novaGavetaUid) {
                        // 1. Decrementa contador e remove link da gaveta antiga
                        database.child("gavetas").child(gavetaAntigaUid).child("peças").child(pecaUid).removeValue()
                        decrementaContadorGaveta(gavetaAntigaUid)

                        // 2. Incrementa contador e cria link na nova gaveta
                        database.child("gavetas").child(novaGavetaUid).child("peças").child(pecaUid).setValue(true)
                        incrementaContadorGaveta(novaGavetaUid)
                    }

                    Toast.makeText(requireContext(), "Peça atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                    val bundle = Bundle().apply {
                        putString("GAVETA_ID", novaGavetaUid)
                    }
                    findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment, bundle)
                } else {
                    Toast.makeText(requireContext(), "Erro ao atualizar a peça: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // NOVO: Função para excluir a peça
    private fun deletePeca(pecaUid: String, gavetaUid: String) {
        database.child("pecas").child(pecaUid).removeValue()
            .addOnSuccessListener {
                database.child("gavetas").child(gavetaUid).child("peças").child(pecaUid).removeValue()
                    .addOnSuccessListener {
                        decrementaContadorGaveta(gavetaUid) // ADICIONADO CHAMADA

                        Toast.makeText(requireContext(), "Peça excluída com sucesso!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment, Bundle().apply { putString("GAVETA_ID", gavetaUid) })
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao desvincular peça da gaveta: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao excluir peça: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- Validação e Navegação (OK) ---

    private fun collectFinalData() {
        pecaEmAndamento.apply {
            if (finalidade == getString(R.string.app_vender)) {
                preco = binding.editEditText.text?.toString() ?: ""
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
        binding.cadastrarRoupa.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}