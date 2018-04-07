package thiengo.com.br.barcodeleitor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView
import thiengo.com.br.barcodeleitor.util.Database
import thiengo.com.br.barcodeleitor.util.unrecognizedCode


class FullscreenActivity : AppCompatActivity(),
        ZBarScannerView.ResultHandler {

    var isLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Algoritmo de requisição de modo fullscreen. */
        requestWindowFeature( Window.FEATURE_NO_TITLE )
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN )

        setContentView(R.layout.activity_fullscreen)

        /*/*
         * Para testes sem o uso efetivo da câmera.
         * */
        thread {
            SystemClock.sleep(2000)
            proccessBarcodeResult()
        }*/

        if( intent != null ){
            isLocked = !intent.getBooleanExtra(Database.KEY_IS_LOCKED, false)
            lockUnlock()
        }
    }

    override fun onResume() {
        super.onResume()
        z_bar_scanner.setResultHandler(this)
        z_bar_scanner.startCamera()
    }

    override fun onPause() {
        super.onPause()
        z_bar_scanner.stopCamera()
    }


    /* *** Algoritmos de interpretação de barra de código *** */
    override fun handleResult( result: Result? ) {
        /* Padrão Cláusula */
        if( result == null ){
            unrecognizedCode(this, {})
            return
        }

        proccessBarcodeResult( result )
        z_bar_scanner.resumeCameraPreview(this)
    }

    fun proccessBarcodeResult( result: Result? = null ){
        /* Padrão Cláusula */
        if( isLocked ){
            return
        }

        val contents = result?.contents
        val barcodeId = result?.barcodeFormat?.id

        /*/*
         * Para testes sem o uso efetivo da câmera.
         * */
        val contents = result?.contents ?: "https://www.thiengo.com.br"
        val barcodeId = result?.barcodeFormat?.id ?: BarcodeFormat.CODE128.id */

        val i = Intent()
        i.putExtra( Database.KEY_CONTENTS, contents )
        i.putExtra( Database.KEY_BARCODE_ID, barcodeId )
        finish( i, Activity.RESULT_OK )
    }

    /*
     * Volta para a atividade principal sempre com o isLocked
     * fazendo parte do conteúdo de resposta.
     * */
    fun finish(intent: Intent, resultAction: Int) {
        intent.putExtra(Database.KEY_IS_LOCKED, isLocked)
        setResult( resultAction, intent )
        finish()
    }


    /* *** Algoritmos de listeners de clique *** */

    /*
     * Para voltar ao modo "não fullscreen" antes que algum
     * código de barra seja interpretado.
     * */
    fun closeFullscreen(view: View){
        finish( Intent(), Activity.RESULT_CANCELED )
    }

    /*
     * Função responsável por mudar o status de funcionamento
     * do algoritmo de interpretação de código de barra
     * lido, incluindo a mudança do ícone de apresentação,
     * ao usuário, de status do algoritmo de interpretação de
     * código.
     * */
    fun lockUnlock(view: View? = null){
        isLocked = !isLocked

        if(isLocked){
            ib_lock.setImageResource(R.drawable.ic_lock_white_24dp)
        }
        else{
            ib_lock.setImageResource(R.drawable.ic_lock_open_white_24dp)
        }
    }
}
