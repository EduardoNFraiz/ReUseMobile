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

    private lateinit var gaveta: Gaveta
    private var newGaveta: Boolean = true
    private lateinit var reference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var imageUri: Uri? = null
    private var imageBase64: String? = null

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri

                // 1. Exibir a imagem e ocultar o placeholder
                binding.imageViewGaveta.setImageURI(uri)
                binding.imageViewGaveta.visibility = View.VISIBLE
                binding.iconPlaceholder.visibility = View.GONE

                // 2. Converte para Base64
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
        reference = Firebase.database.reference
        auth = Firebase.auth

        val vizualizarInfo = arguments?.getBoolean("VISUALIZAR_INFO") ?: false
        if (vizualizarInfo) {
            binding.bttEditar.visibility = View.VISIBLE
            binding.bttCriarGaveta.visibility = View.GONE
            binding.editTextGaveta.isEnabled = false
            binding.rbPrivado.isEnabled = false
            binding.rbPublico.isEnabled = false
        }
        else {
            binding.bttEditar.visibility = View.GONE
            binding.bttCriarGaveta.visibility = View.VISIBLE
            binding.editTextGaveta.isEnabled = true
            binding.rbPrivado.isEnabled = true
            binding.rbPublico.isEnabled = true
        }

        initListeners() // Inicializa os listeners após configurar o modo de visualização
        modoEditor()
    }

    private fun initListeners() {
        binding.bttCriarGaveta.setOnClickListener {
            valideData()
        }

        // Listener para abrir a seleção de imagem
        binding.imagePlaceholderCard.setOnClickListener {
            openImageChooser()
        }
    }

    // Função para abrir a seleção de imagem
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
            // Usando Toast para erros de processamento interno
            Toast.makeText(requireContext(), "Erro ao processar imagem: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return null
    }

    private fun valideData(){
        val nome = binding.editTextGaveta.text.toString().trim()
        val isPublic = binding.rbPublico.isChecked
        val isPrivate = binding.rbPrivado.isChecked

        // Validação da imagem
        if (imageBase64.isNullOrBlank() && newGaveta) {
            // Usando showBottomSheet para erro de validação de campo
            showBottomSheet(
                titleDialog = R.string.atencao, // Use o seu recurso string correto para "Atenção"
                message = "Selecione uma imagem para a gaveta!",
                titleButton = R.string.entendi // Use o seu recurso string correto para "Entendi"
            )
            return
        }

        if(nome.isNotBlank() && (isPublic || isPrivate)){
            if(newGaveta) gaveta = Gaveta(
                name = nome,
                number = "0",
                fotoBase64 = imageBase64, // Usa a string Base64
                public = isPublic
            )
            saveGaveta()
        }else{
            // Usando showBottomSheet para erro de validação de campo
            showBottomSheet(
                titleDialog = R.string.atencao, // Use o seu recurso string correto para "Atenção"
                message = "Preencha o nome e escolha a visibilidade da gaveta!",
                titleButton = R.string.entendi // Use o seu recurso string correto para "Entendi"
            )
        }
    }

    /**
     * CORRIGIDO: Substituído showBottomSheet por Toast para mensagens de erro do Firebase.
     * Garantia de mensagens claras.
     */
    private fun saveGaveta(){
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // MENSAGEM CLARA
            Toast.makeText(requireContext(), "Erro: Usuário não autenticado. Faça login novamente.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Gerar um novo UID para a gaveta (Este UID será a chave no DB)
        val gavetaId = reference.child("gavetas").push().key
        if (gavetaId == null) {
            // CORREÇÃO CRÍTICA: Erro de ID interno tratado com Toast
            Toast.makeText(requireContext(), "Erro interno: Falha ao gerar ID único da gaveta. Tente novamente.", Toast.LENGTH_LONG).show()
            return
        }

        // 3. Salvar o objeto Gaveta na tabela 'gavetas'
        reference.child("gavetas")
            .child(gavetaId)
            .setValue(gaveta)
            .addOnCompleteListener { taskGaveta ->
                if (taskGaveta.isSuccessful) {

                    // 4. Buscar o tipo de conta do usuário antes de salvar a referência
                    getUserAccountType(userId, gavetaId)

                } else {
                    // MENSAGEM CLARA
                    Toast.makeText(requireContext(), "Erro ao salvar os detalhes da gaveta: ${taskGaveta.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Função CORRIGIDA para buscar o tipo de conta, garantindo que falhas usem Toast
    private fun getUserAccountType(userId: String, gavetaId: String) {
        // 1. Tentar encontrar o usuário em 'pessoaFisica'
        reference.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Usuário é Pessoa Física
                        updateUserGavetaReference(userId, gavetaId, "pessoaFisica", null)
                    } else {
                        // 2. Se não for Pessoa Física, tentar encontrar em 'pessoaJuridica'
                        searchPessoaJuridica(userId, gavetaId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Usa Toast
                    Toast.makeText(requireContext(), "Erro ao buscar tipo de conta: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // Função auxiliar para buscar subtipos de Pessoa Jurídica, garantindo que falhas usem Toast
    private fun searchPessoaJuridica(userId: String, gavetaId: String) {
        val subtipos = listOf("brechos", "instituicoes") // Adicione todos os seus subtipos aqui
        var found = false
        var checkedCount = 0

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        checkedCount++
                        // Verifica se o usuário foi encontrado e se ainda não processamos outro subtipo
                        if (snapshot.exists() && !found) {
                            found = true // Garante que só um será processado
                            // Usuário é Pessoa Jurídica com o subtipo encontrado
                            updateUserGavetaReference(userId, gavetaId, "pessoaJuridica", subtipo)
                        }

                        // Se terminou de buscar em todos e não encontrou, mostra a mensagem de erro
                        if (checkedCount == subtipos.size && !found) {
                            // Usa Toast
                            Toast.makeText(requireContext(), "Não foi possível determinar o tipo de conta do usuário para vincular a gaveta.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkedCount++
                        // Usa Toast
                        Toast.makeText(requireContext(), "Erro ao buscar subtipo: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }


    /**
     * CORRIGIDO: Substituído showBottomSheet por Toast para mensagens de erro do Firebase.
     */
    private fun updateUserGavetaReference(userId: String, gavetaId: String, tipoConta: String, subtipoJuridico: String?) {
        var userPath = ""

        // Constrói o caminho baseado no tipo de conta
        if (tipoConta == "pessoaFisica") {
            userPath = "usuarios/pessoaFisica/$userId"
        } else if (tipoConta == "pessoaJuridica" && subtipoJuridico != null) {
            userPath = "usuarios/pessoaJuridica/$subtipoJuridico/$userId"
        }

        if (userPath.isNotEmpty()) {
            val userUpdateMap = mapOf<String, Any>(
                "gavetas/$gavetaId" to true // Salva apenas a referência do UID
            )
            reference.child(userPath)
                .updateChildren(userUpdateMap)
                .addOnCompleteListener { taskUser ->
                    if (taskUser.isSuccessful) {
                        // SUCESSO COMPLETO
                        Toast.makeText(requireContext(), "Gaveta criada com sucesso!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_criarGavetaFragment_to_gavetaFragment)
                    } else {
                        // MENSAGEM CLARA
                        Toast.makeText(requireContext(), "Erro ao vincular gaveta ao usuário: ${taskUser.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // MENSAGEM CLARA
            Toast.makeText(requireContext(), "Erro: Tipo de conta do usuário inválido ou não encontrado.", Toast.LENGTH_LONG).show()
        }
    }

    private fun modoEditor(){
        var editando = false
        binding.bttEditar.setOnClickListener {
            editando = !editando
            val isEnabled = editando

            binding.editTextGaveta.isEnabled = isEnabled
            binding.rbPrivado.isEnabled = isEnabled
            binding.rbPublico.isEnabled = isEnabled

            if(isEnabled) {
                binding.bttSalvar.visibility = View.VISIBLE
            }
            else{
                binding.bttSalvar.visibility = View.INVISIBLE
            }
        }
        binding.bttSalvar.setOnClickListener {
            // Lógica para salvar a edição deve ser implementada aqui
            // Por enquanto, apenas navega
            findNavController().navigate(R.id.closet)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}