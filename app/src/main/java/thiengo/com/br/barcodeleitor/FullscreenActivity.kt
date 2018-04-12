package thiengo.com.br.barcodeleitor

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Algoritmo de requisição de modo fullscreen. */
        requestWindowFeature( Window.FEATURE_NO_TITLE )
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN )

        setContentView(R.layout.activity_fullscreen)

        /*
         * O código abaixo é para garantir que ib_lock.tag
         * sempre terá um valor Boolean depois do onCreate().
         * */
        ib_lock.tag = if(ib_lock.tag == null) false else (ib_lock.tag as Boolean)
        if( savedInstanceState != null ){
            ib_lock.tag = savedInstanceState.getBoolean(KEY_IS_LOCKED)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_LOCKED, ib_lock.tag as Boolean)
    }

    override fun onResume() {
        super.onResume()
        z_xing_scanner.setResultHandler(this)
        startCamera()

        /*
         * Para manter os status da luz de flash
         * da câmera como ativa / não ativa,
         * status vindo da atividade anterior.
         * */
        ib_flashlight.tag = false /* Garantindo um valor inicial. */
        if( intent != null ){
            ib_flashlight.tag = !intent.getBooleanExtra(Database.KEY_IS_LIGHTENED, false)
            flashLight()
        }

        /*
         * Necessário para manter o status da trava
         * da câmera, ativa / não ativa, quando houver
         * reconstrução de atividade.
         * */
        if(ib_lock.tag as Boolean){
            ib_lock.tag = !(ib_lock.tag as Boolean)
            lockUnlock()
        }

        z_xing_scanner.threadCallWhenCameraIsWorking{
            runOnUiThread {
                /*
                 * Caso a linha de código abaixo não esteja presente,
                 * em algumas versões do Android está atividade também
                 * ficará travada em portrait screen devido ao uso
                 * desta trava no AndroidManifest.xml, mesmo que a trava
                 * esteja definida somente para a atividade principal.
                 * Outra, a invocação da linha abaixo deve ocorrer
                 * depois que a câmera já está em funcionamento
                 * na tela, caso contrário há a possibilidade de a
                 * câmera não funcionar em alguns devices.
                 * */
                setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        z_xing_scanner.stopCameraForAllDevices()
    }

    private fun startCamera(){
        if( !z_xing_scanner.isFlashSupported(this) ){
            ib_flashlight.visibility = View.GONE
        }

        z_xing_scanner.startCameraForAllDevices(this)
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
            unrecognizedCode(this)
            return
        }

        proccessBarcodeResult( result )
    }

    fun proccessBarcodeResult( result: Result ){
        val text = result.text
        val barcodeName = result.barcodeFormat.name

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
        intent.putExtra(Database.KEY_IS_LIGHTENED, ib_flashlight.tag as Boolean)
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
        ib_lock.tag = true
        lockUnlock()
    }

    /*
     * Ativa e desativa a luz de flash do celular caso esteja
     * disponível.
     * */
    fun flashLight(view: View? = null){
        ib_flashlight.tag = !(ib_flashlight.tag as Boolean)

        /*
         * A linha de código abaixo é necessária, pois caso haja
         * uma reconstrução de atividade o valor obtido para
         * ib_flashlight.tag vem da intent em memória, então a
         * intent tem que estar com o valor atual.
         * */
        intent.putExtra(Database.KEY_IS_LIGHTENED, ib_flashlight.tag as Boolean)

        if(ib_flashlight.tag as Boolean){
            z_xing_scanner.enableFlash(this, true)
            ib_flashlight.setImageResource(R.drawable.ic_flashlight_white_24dp)
        }
        else{
            z_xing_scanner.enableFlash(this, false)
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
        ib_lock.tag = !(ib_lock.tag as Boolean)

        if(ib_lock.tag as Boolean){
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
        ib_flashlight.tag = true
        flashLight()
    }
}
