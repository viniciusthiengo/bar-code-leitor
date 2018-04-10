package thiengo.com.br.barcodeleitor.util

import android.content.Context
import android.media.RingtoneManager
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

/*
 * Para que seja possível lançar um sinal sonoro logo
 * após a leitura de algum código de barra.
 * */
fun notification(context: Context){
    try {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), notification)
        ringtone.play()
    }
    catch(e: Exception) { }
}