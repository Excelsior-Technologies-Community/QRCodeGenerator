package com.ext.codegenerator

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout

class QRGeneratorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_qr_generator, this, true)
    }

}
