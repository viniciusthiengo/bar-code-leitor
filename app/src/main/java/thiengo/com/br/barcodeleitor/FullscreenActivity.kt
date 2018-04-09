package thiengo.com.br.barcodeleitor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_fullscreen.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import thiengo.com.br.barcodeleitor.util.*


class FullscreenActivity : AppCompatActivity(),
        ZXingScannerView.ResultHandler {

    val KEY_IS_LOCKED = "is_locked"
    var isLocked = false
    var isLightened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Algoritmo de requisição de modo fullscreen. */
        requestWindowFeature( Window.FEATURE_NO_TITLE )
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN )

        setContentView(R.layout.activity_fullscreen)

        if( savedInstanceState != null ){
            isLocked = !savedInstanceState.getBoolean(KEY_IS_LOCKED)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_LOCKED, isLocked)
    }

    override fun onResume() {
        super.onResume()
        z_xing_scanner.setResultHandler(this)
        z_xing_scanner.startCameraForAllDevices(this)

        threadCallWhenCameraIsWorking(z_xing_scanner, {
            runOnUiThread {
                /*
                 * Para manter os status da luz de flash
                 * da câmera como ativa / não ativa,
                 * status vindo da atividade anterior.
                 * */
                if( intent != null ){
                    isLightened = !intent.getBooleanExtra(Database.KEY_IS_LIGHTENED, false)
                    flashLight()
                }

                /*
                 * Necessário para manter o status da trava
                 * da câmera, ativa / não ativa, quando houver
                 * reconstrução de atividade.
                 * */
                if(isLocked){
                    isLocked = false
                    lockUnlock()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        z_xing_scanner.stopCameraForAllDevices()
    }

    /*
     * Para que o compartamento de exit activity do
     * domínio do problema seja respeitado.
     * */
    override fun onBackPressed() {
        closeFullscreen()
    }


    /* *** Algoritmos de interpretação de barra de código *** */
    override fun handleResult( result: Result? ) {
        /*
         * Padrão Cláusula de Guarda - Caso o resultado seja
         * null, apresente um mensagem e finaliza o processamento
         * do método handleResult().
         * */
        if( result == null ){
            unrecognizedCode(this, {})
            return
        }

        proccessBarcodeResult( result )
        z_xing_scanner.resumeCameraPreview(this)
    }

    fun proccessBarcodeResult( result: Result? = null ){
        /*
         * Padrão Claúsula de Guarda sendo utilizado para
         * que a leitura do código não continue caso o
         * usuário tenha travado a CameraPreview.
         * */
        if( isLocked ){
            return
        }

        val text = result?.text
        val barcodeName = result?.barcodeFormat?.name

        val i = Intent()
        i.putExtra( Database.KEY_NAME, text )
        i.putExtra( Database.KEY_BARCODE_NAME, barcodeName )
        finish( i, Activity.RESULT_OK )
    }

    /*
     * Volta para a atividade principal sempre com o isLightened
     * fazendo parte do conteúdo de resposta.
     * */
    fun finish(intent: Intent, resultAction: Int) {
        intent.putExtra(Database.KEY_IS_LIGHTENED, isLightened)
        setResult( resultAction, intent )
        finish()
    }


    /* *** Algoritmos de listeners de clique *** */

    /*
     * Para voltar ao modo "não fullscreen" antes que algum
     * código de barra seja interpretado.
     * */
    fun closeFullscreen(view: View? = null){
        unlockCamera()
        finish( Intent(), Activity.RESULT_CANCELED )
    }

    /*
     * Como o lock não é mantido, o método abaixo é
     * necessário para que o usuário veja o unlock
     * ocorrendo, caso esteja como lock, a câmera.
     * */
    private fun unlockCamera(){
        isLocked = true
        lockUnlock()
    }

    /*
     * Ativa e desativa a luz de flash do celular caso esteja
     * disponível.
     * */
    fun flashLight(view: View? = null){
        isLightened = !isLightened
        intent.putExtra(Database.KEY_IS_LIGHTENED, isLightened)

        if(isLightened){
            z_xing_scanner.flash = true
            ib_flashlight.setImageResource(R.drawable.ic_flashlight_white_24dp)
        }
        else{
            z_xing_scanner.flash = false
            ib_flashlight.setImageResource(R.drawable.ic_flashlight_off_white_24dp)
        }
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
            /*
             * Para funcionar deve ser invocado antes do
             * stopCameraPreview().
             * */
            turnOffFlashlight()

            z_xing_scanner.stopCameraPreview()
            ib_lock.setImageResource(R.drawable.ic_lock_white_24dp)
            ib_flashlight.isEnabled = false
        }
        else{
            z_xing_scanner.resumeCameraPreview(this)
            ib_lock.setImageResource(R.drawable.ic_lock_open_white_24dp)
            ib_flashlight.isEnabled = true
        }
    }

    /*
     * Método necessário, pois não faz sentido deixar a luz
     * de flash ligada quando a tela não mais está lendo
     * códigos, está travada. Método invocado quando o lock
     * de tela ocorre.
     * */
    private fun turnOffFlashlight(){
        isLightened = true
        flashLight()
    }
}
