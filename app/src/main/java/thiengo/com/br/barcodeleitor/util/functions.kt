package thiengo.com.br.barcodeleitor.util

import android.content.Context
import android.media.RingtoneManager
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import thiengo.com.br.barcodeleitor.R
import kotlin.concurrent.thread


fun unrecognizedCode( context: Context, callbackClear: ()->Unit ){
    Toast
        .makeText(
            context,
            context.getString(R.string.unrecognized_code),
            Toast.LENGTH_SHORT )
        .show()

    callbackClear()
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