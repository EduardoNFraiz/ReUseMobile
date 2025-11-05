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

    // FLAG para controlar se o feedback já foi mostrado
    private var feedbackShown = false

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
        reference = Firebase.database.reference
        auth = Firebase.auth

        val vizualizarInfo = arguments?.getBoolean("VISUALIZAR_INFO") ?: false
        if (vizualizarInfo) {
            binding.bttEditar.visibility = View.VISIBLE
            binding.bttCriarGaveta.visibility = View.GONE
            binding.editTextGaveta.isEnabled = false
            binding.rbPrivado.isEnabled = false
            binding.rbPublico.isEnabled = false
        } else {
            binding.bttEditar.visibility = View.GONE
            binding.bttCriarGaveta.visibility = View.VISIBLE
            binding.editTextGaveta.isEnabled = true
            binding.rbPrivado.isEnabled = true
            binding.rbPublico.isEnabled = true
        }

        initListeners()
        modoEditor()
    }

    private fun initListeners() {
        binding.bttCriarGaveta.setOnClickListener {
            valideData()
        }
        binding.imagePlaceholderCard.setOnClickListener {
            openImageChooser()
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    private fun convertImageUriToBase64(uri: Uri): String? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: IOException) {
            showError("Erro ao processar imagem: ${e.message}")
            null
        }
    }

    private fun valideData() {
        val nome = binding.editTextGaveta.text.toString().trim()
        val isPublic = binding.rbPublico.isChecked
        val isPrivate = binding.rbPrivado.isChecked

        if (feedbackShown) return

        if (imageBase64.isNullOrBlank() && newGaveta) {
            showError("Selecione uma imagem para a gaveta!")
            return
        }

        if (nome.isNotBlank() && (isPublic || isPrivate)) {
            if (newGaveta) {
                gaveta = Gaveta(
                    name = nome,
                    number = "0",
                    fotoBase64 = imageBase64,
                    public = isPublic
                )
            }
            saveGaveta()
        } else {
            showError("Preencha o nome e escolha a visibilidade da gaveta!")
        }
    }

    private fun saveGaveta() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("Erro: Usuário não autenticado. Faça login novamente.")
            return
        }

        val gavetaId = reference.child("gavetas").push().key
        if (gavetaId.isNullOrBlank()) {
            showError("Erro interno: Falha ao gerar ID único da gaveta. Tente novamente.")
            return
        }

        binding.bttCriarGaveta.isEnabled = false
        reference.child("gavetas")
            .child(gavetaId)
            .setValue(gaveta)
            .addOnCompleteListener { taskGaveta ->
                if (taskGaveta.isSuccessful) {
                    getUserAccountType(userId, gavetaId)
                } else {
                    binding.bttCriarGaveta.isEnabled = true
                    showError("Erro ao salvar os detalhes da gaveta: ${taskGaveta.exception?.message}")
                }
            }
    }

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
                            showError("Não foi possível determinar o tipo de conta do usuário para vincular a gaveta.")
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
                        showError("Erro ao vincular gaveta ao usuário: ${taskUser.exception?.message}")
                    }
                }
        } else {
            binding.bttCriarGaveta.isEnabled = true
            showError("Erro: Tipo de conta do usuário inválido ou não encontrado.")
        }
    }

    private fun showSuccessAndNavigate(gavetaId: String) {
        if (!feedbackShown) {
            feedbackShown = true
            Toast.makeText(requireContext(), "Gaveta criada com sucesso!", Toast.LENGTH_SHORT).show()
            val bundle = Bundle().apply {
                putString("GAVETA_ID", gavetaId)
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
        }
    }

    private fun modoEditor() {
        var editando = false
        binding.bttEditar.setOnClickListener {
            editando = !editando
            val isEnabled = editando
            binding.editTextGaveta.isEnabled = isEnabled
            binding.rbPrivado.isEnabled = isEnabled
            binding.rbPublico.isEnabled = isEnabled

            if (isEnabled) {
                binding.bttSalvar.visibility = View.VISIBLE
            } else {
                binding.bttSalvar.visibility = View.INVISIBLE
            }
        }
        binding.bttSalvar.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}