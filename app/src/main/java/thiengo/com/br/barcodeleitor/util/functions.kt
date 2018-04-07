package thiengo.com.br.barcodeleitor.util

import android.content.Context
import android.widget.Toast

fun unrecognizedCode( contrext: Context, callbackClear: ()->Unit ){
    Toast
        .makeText(contrext, "Código não reconhecido.", Toast.LENGTH_SHORT)
        .show()

    callbackClear()
}