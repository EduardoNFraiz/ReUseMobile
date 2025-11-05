package com.projetointegrador.reuse.util

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.BottomSheetBinding
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy


fun Fragment.initToolbar(toolbar: Toolbar){
    (activity as AppCompatActivity).setSupportActionBar(toolbar)
    (activity as AppCompatActivity).title=""
    (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}

fun Fragment.showBottomSheet(
    titleDialog: Int? = null,
    titleButton: Int? = null,
    message: String,
    onClick: () -> Unit = {},
){
    val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialog)
    val binding: BottomSheetBinding =
        BottomSheetBinding.inflate(layoutInflater, null, false)

    binding.textViewTitle.text=getText(titleDialog ?: R.string.text_tile_warning)
    binding.textViewMessage.text = message
    binding.buttonOk.text = getText(titleButton ?: R.string.text_button_warning)
    binding.buttonOk.setOnClickListener {
        onClick()
        bottomSheetDialog.dismiss()
    }

    bottomSheetDialog.setContentView(binding.root)
    bottomSheetDialog.show()
}



fun displayBase64Image(base64String: String, imageView: ImageView) {
    if (base64String.isNullOrEmpty()) {
        imageView.setImageResource(R.drawable.baseline_image_24)
        return
    }

    try {
        // Remove o prefixo, se houver
        val pureBase64 = base64String.substringAfter("base64,", base64String)
        val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)

        Glide.with(imageView.context)
            .asBitmap()
            .load(imageBytes)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.baseline_image_24)
            .error(R.drawable.baseline_image_24)
            .into(imageView)

    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        imageView.setImageResource(R.drawable.baseline_image_24)
    }
}