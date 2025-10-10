package com.projetointegrador.reuse.ui.auth.recuperarsenha

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentReceberCodigoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import kotlin.toString


class ReceberCodigoFragment : Fragment() {
    private var _binding: FragmentReceberCodigoBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceberCodigoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners(){
        binding.buttonEnvCod.setOnClickListener {
            valideData()
        }

    }

    private fun valideData(){
        val email = binding.editTextEmail.text.toString().trim()
        if(email.isNotBlank()) {
            recoverAccountUser(email)
        } else {
            showBottomSheet(message = getString(R.string.password_empty))
        }
    }

    private fun recoverAccountUser(email: String){
        try {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        showBottomSheet(message = "Enviamos uma mensagem no seu email para redefinir a senha!")
                    }else{
                        Toast.makeText(requireContext(),task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }catch (e: Exception){
            Toast.makeText(requireContext(), e.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}