package thiengo.com.br.barcodeleitor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Patterns
import android.view.View
import android.webkit.URLUtil
import kotlinx.android.synthetic.main.activity_main.*
import me.dm7.barcodescanner.zbar.BarcodeFormat
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import thiengo.com.br.barcodeleitor.util.Database
import thiengo.com.br.barcodeleitor.util.unrecognizedCode


class MainActivity : AppCompatActivity(),
    ZBarScannerView.ResultHandler,
    EasyPermissions.PermissionCallbacks {

    val REQUEST_CODE_CAMERA = 182
    val REQUEST_CODE_FULLSCREEN = 184
    var isLocked = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askCameraPermission()

        /*
         * Caso tenha algum último resultado lido e
         * salvo no SharedPreferences, utilizamos
         * este último resultado já em tela.
         * */
        val result = Database.getSavedResult(this)
        isLocked = Database.getSavedIsLocked(this)

        if( result != null ){
            Log.i("LOG", "${result.contents} - ${result.barcodeFormat.id}")
            proccessBarcodeResult( result.contents, result.barcodeFormat.id )
        }
        /*else{
            /*
             * Para testes sem necessidade de uso de
             * câmera.
             */
            proccessBarcodeResult()
        }*/
    }

    override fun onResume() {
        super.onResume()

        /*
         * Registrando a atividade para que ela possa
         * trabalhar os resultados de scan. Como na
         * documentação, o onResume() é o local ideal
         * para este registro.
         * */
        z_bar_scanner.setResultHandler(this)

        /*
         * Útil principalmente na volta da
         * FullscreenActivity.
         * */
        if( EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA) ){
            z_bar_scanner.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()

        /*
         * Para liberação de recursos, pare a câmera
         * logo no onPause().
         * */
        z_bar_scanner.stopCamera()
    }

    /*
     * Método herdado utilizado para que seja possível
     * interpretar qualquer valor vindo da atividade
     * de fullscreen cam.
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if( requestCode == REQUEST_CODE_FULLSCREEN ){

            /*
             * Garantindo que o botão de lock e o status
             * controlado por ele continuem com os valores
             * corretos.
             * */
            isLocked = !data!!.getBooleanExtra(Database.KEY_IS_LOCKED, false)
            lockUnlock()

            if( resultCode == Activity.RESULT_OK ){
                proccessBarcodeResult(
                    data.getStringExtra(Database.KEY_CONTENTS),
                    data.getIntExtra(Database.KEY_BARCODE_ID, Database.DEFAULT_BARCODE_ID) )
            }
            else if( resultCode == Activity.RESULT_CANCELED ){
                isLocked = !data.getBooleanExtra(Database.KEY_IS_LOCKED, false)
                lockUnlock()
            }
        }
    }


    /* *** Algoritmos de requisição de permissão *** */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        /* Encaminhando resultados para EasyPermissions */
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this )
    }

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: MutableList<String>) {

        askCameraPermission()
    }

    override fun onPermissionsGranted(
        requestCode: Int,
        perms: MutableList<String>) {

        /*
         * Iniciando o funcionamento da câmera na View
         * ZBarScannerView somente depois de assegurada
         * a permissão de uso desse recurso.
         * */
        z_bar_scanner.startCamera()
    }

    private fun askCameraPermission(){
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, REQUEST_CODE_CAMERA, Manifest.permission.CAMERA)
                .setRationale("A permissão de uso de camera é necessária para que o aplicativo funcione.")
                .setPositiveButtonText("Ok")
                .setNegativeButtonText("Cancelar")
                .build() )
    }


    /* *** Algoritmos de interpretação de barra de código *** */
    override fun handleResult(result: Result?) {
        /* Padrão Cláusula */
        if( result == null ){
            unrecognizedCode(this, {clearContent()})
            return
        }

        proccessBarcodeResult(
            result.contents,
            result.barcodeFormat.id)

        /*
         * Retomar a verificação de códigos de barra com
         * a câmera.
         * */
        z_bar_scanner.resumeCameraPreview(this)
    }

    private fun proccessBarcodeResult(
        content: String? = null,
        barcodeFormatId: Int? = null ){

        /*
         * Padrão Claúsula de Guarda sendo utilizado para
         * que a leitura do código não continue caso o
         * usuário tenha trancado está funcionalidade.
         * */
        if( isLocked ){
            return
        }

        val result = Result()
        result.contents = content
        result.barcodeFormat = BarcodeFormat.getFormatById(barcodeFormatId ?: -1)

        /*/*
         * Para testes sem o uso real da câmera.
         * */
        if(content == null || barcodeFormatId == null){
            result.contents = "Lorem Ipsum."
            result.contents = "http://www.thiengo.com.br"
            result.contents = "thiengocalopsita@gmail.com"
            result.contents = "5527999887766"
            result.barcodeFormat = BarcodeFormat.CODABAR
        }*/

        Database.saveResult(this, result)
        Database.saveIsLocked(this, isLocked)

        tv_content.text = result.contents
        tv_bar_code_type.text = "Tipo barra de código: ${result.barcodeFormat.name}"

        processButtonOpen(result)
    }

    /*
     * Verificação de tipo de conteúdo lido em barra
     * de código para o correto trabalho com o botão
     * de ação.
     * */
    private fun processButtonOpen(result: Result){
        when{
            URLUtil.isValidUrl(result.contents) ->
                setButtonOpenAction("ABRIR URL") {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(result.contents)
                    startActivity(i)
                }
            Patterns.EMAIL_ADDRESS.matcher(result.contents).matches() ->
                setButtonOpenAction("ABRIR EMAIL") {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("mailto:?body=${result.contents}")
                    startActivity(i)
                }
            Patterns.PHONE.matcher(result.contents).matches() ->
                setButtonOpenAction("LIGAR") {
                    val i = Intent(Intent.ACTION_DIAL)
                    i.data = Uri.parse("tel:${result.contents}")
                    startActivity(i)
                }
            else -> setButtonOpenAction(status = false)
        }
    }

    /*
     * Método de configuração de status e conteúdo do
     * botão de acionamento de ação caso o conteúdo da
     * barra de código seja: email, url ou telefone.
     * */
    private fun setButtonOpenAction(
        label: String = "",
        status: Boolean = true,
        callbackClick:()->Unit = {} ){

        bt_open.text = label
        bt_open.visibility = if(status) View.VISIBLE else View.GONE
        bt_open.setOnClickListener { callbackClick() }
    }


    /* *** Algoritmos de listeners de clique *** */

    /*
     * Método para limpar a interface do usuário.
     * */
    fun clearContent(view: View? = null){
        tv_content.text = "Nada lido ainda"
        tv_bar_code_type.visibility = View.GONE
        bt_open.visibility = View.GONE
        Database.saveResult(this, null)
    }

    /*
     * Abre a atividade que permite o uso da câmera
     * em toda a tela do device.
     * */
    fun openFullscreen(view: View){
        /* Cláusula de Gurda */
        if( !EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA) ){
            return
        }

        val i = Intent(this, FullscreenActivity::class.java)
        i.putExtra(Database.KEY_IS_LOCKED, isLocked)
        startActivityForResult(i, REQUEST_CODE_FULLSCREEN)
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
