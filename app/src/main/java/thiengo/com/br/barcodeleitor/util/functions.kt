package thiengo.com.br.barcodeleitor.util

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import kotlin.concurrent.thread

fun unrecognizedCode( contrext: Context, callbackClear: ()->Unit ){
    Toast
        .makeText(contrext, "Código não reconhecido.", Toast.LENGTH_SHORT)
        .show()

    callbackClear()
}

fun threadCallWhenCameraIsWorking(view: View, callback: ()->Unit){
    thread {
        /*
         * A câmera somente pode ser travada / liberada depois
         * que está já em funcionamento na interface do usuário.
         * O método isShown garante está verificação.
         * */
        while( !view.isShown ){
            SystemClock.sleep(1000)
        }

        callback()
    }
}