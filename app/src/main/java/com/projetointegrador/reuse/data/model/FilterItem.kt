package com.projetointegrador.reuse.data.model

import android.R.attr.closeIcon
import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.chip.Chip
import com.projetointegrador.reuse.R

data class FilterItem(
    val id: Int,
    val text: String,
    @DrawableRes val closeIcon: Int? = null,
    @DrawableRes val icon: Int? = null,
    val iconSize: Float = 52.0f
)

fun FilterItem.toChip(context: Context) : Chip {
    val chip = if(closeIcon == null) {
        LayoutInflater.from(context).inflate(R.layout.chip_choice, null, false) as Chip
    } else {
            Chip(ContextThemeWrapper(context, R.style.Theme_ReUse))
        }

    if (icon != null)
        chip.setChipBackgroundColorResource(R.color.white)

    chip.chipStrokeWidth = 2f

    if (icon != null) {
        chip.chipIconSize = iconSize
        chip.setChipIconResource(icon)
        chip.chipStartPadding = 20f
    } else {
        chip.chipIcon = null
    }

    closeIcon?.let {
        chip.setCloseIconResource(it)
        chip.isCloseIconVisible = true
    }

    chip.text = text

    return chip
}
