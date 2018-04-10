package thiengo.com.br.barcodeleitor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Patterns
import android.view.View
import android.webkit.URLUtil
import kotlinx.android.synthetic.main.activity_main.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import thiengo.com.br.barcodeleitor.util.*


class MainActivity : AppCompatActivity(),
        ZXingScannerView.ResultHandler,
        EasyPermissions.PermissionCallbacks {

    val REQUEST_CODE_CAMERA = 182
    val REQUEST_CODE_FULLSCREEN = 184
    var isLocked = false
    var isLightened = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askCameraPermission()
        lastResultVerification()
    }

    /*
     * Caso tenha algum último resultado lido e
     * salvo no SharedPreferences, utilizamos
     * este último resultado já em tela.
     * */
    private fun lastResultVerification(){
        val result = Database.getSavedResult(this)
        if( result != null ){
            proccessBarcodeResult( result.text, result.barcodeFormat.name )
        }
    }

    override fun onResume() {
        super.onResume()
        /*
         * Registrando a atividade para que ela possa
         * trabalhar os resultados de scan. Seguindo a
         * documentação, o código entra no onResume().
         * */
        z_xing_scanner.setResultHandler(this)

        restartCameraIfInactive()
    }

    /*
     * Método necessário para que a câmera volte a
     * funcionar, tendo em mente que a partir do onPause()
     * até mesmo os recursos destinados a ela, nesta
     * entidade, foram todos liberados. Lembrando também
     * que em caso de volta a esta atividade, sem passar
     * pelo onCreate(), o método onPermissionsGranted()
     * não será invocado novamente assim é preciso restart
     * a câmera caso ela já não esteja ativa.
     * */
    private fun restartCameraIfInactive(){
        if( !z_xing_scanner.isCameraStarted()
                && EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA) ){
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        z_xing_scanner.stopCameraForAllDevices()
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
             * Garantindo que o botão de luz de flash e o
             * status controlado por ele continuem com os
             * valores corretos.
             * */
            isLightened = !data!!.getBooleanExtra(Database.KEY_IS_LIGHTENED, false)
            flashLight()

            if( resultCode == Activity.RESULT_OK ){
                proccessBarcodeResult(
                    data.getStringExtra(Database.KEY_NAME),
                    data.getStringExtra(Database.KEY_BARCODE_NAME) )
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

    private fun askCameraPermission(){
        EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, REQUEST_CODE_CAMERA, Manifest.permission.CAMERA)
                        .setRationale("A permissão de uso de camera é necessária para que o aplicativo funcione.")
                        .setPositiveButtonText("Ok")
                        .setNegativeButtonText("Cancelar")
                        .build() )
    }

    override fun onPermissionsGranted(
            requestCode: Int,
            perms: MutableList<String>) {

        startCamera()
    }

    private fun startCamera(){
        if( !z_xing_scanner.isFlashSupported(this) ){
            ib_flashlight.visibility = View.GONE
        }

        z_xing_scanner.startCameraForAllDevices(this)
    }


    /* *** Algoritmos de interpretação de barra de código *** */
    override fun handleResult(result: com.google.zxing.Result?) {
        /*
         * Padrão Cláusula de Guarda - Caso o resultado seja
         * null, limpa a tela, se houver um último dado lido,
         * apresente uma mensagem e finaliza o processamento
         * do método handleResult().
         * */
        if( result == null ){
            unrecognizedCode(this, { clearContent() })
            return
        }

        proccessBarcodeResult(
                result.text,
                result.barcodeFormat.name)
    }

    private fun proccessBarcodeResult(
            text: String? = null,
            barcodeFormatName: String = "QRCODE" ){
        /*
         * Padrão Claúsula de Guarda sendo utilizado para
         * que a leitura do código não continue caso o
         * usuário tenha travado a CameraPreview.
         * */
        if( isLocked ){
            return
        }

        /*
         * O código a seguir é essencial para que o ringtone
         * não seja invocado para um mesmo código de barra
         * lido seguidas vezes.
         * */
        val resultSaved = Database.getSavedResult(this)
        if( resultSaved == null || !resultSaved.text.equals(text, true) ){
            notification(this)
        }

        val result = com.google.zxing.Result(
            text,
            text!!.toByteArray(), /* Somente para ter algo */
            arrayOf(), /* Somente para ter algo */
            com.google.zxing.BarcodeFormat.valueOf(barcodeFormatName))

        /* Salvando o último resultado lido. */
        Database.saveResult(this, result)

        /* Modificando interface do usuário. */
        tv_content.text = result.text
        processBarcodeType(true, result.barcodeFormat.name)
        processButtonOpen(result)

        /*
         * Caso este trecho não esteja aqui, a câmera
         * permanecerá travada, como em stopCameraPreview().
         * */
        if( !isLocked ){
            z_xing_scanner.resumeCameraPreview(this)
        }
    }

    private fun processBarcodeType(status: Boolean = false, barcode: String = ""){
        tv_bar_code_type.text = "Tipo barra de código: $barcode"
        tv_bar_code_type.visibility = if(status) View.VISIBLE else View.GONE
    }

    /*
     * Verificação de tipo de conteúdo lido em código de
     * barra para o correto trabalho com o botão de ação.
     * */
    private fun processButtonOpen(result: com.google.zxing.Result){
        when{
            URLUtil.isValidUrl(result.text) ->
                setButtonOpenAction("ABRIR URL") {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(result.text)
                    startActivity(i)
                }
            Patterns.EMAIL_ADDRESS.matcher(result.text).matches() ->
                setButtonOpenAction("ABRIR EMAIL") {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("mailto:?body=${result.text}")
                    startActivity(i)
                }
            Patterns.PHONE.matcher(result.text).matches() ->
                setButtonOpenAction("LIGAR") {
                    val i = Intent(Intent.ACTION_DIAL)
                    i.data = Uri.parse("tel:${result.text}")
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
        tv_content.text = getString(R.string.nothing_read)
        processBarcodeType(false)
        bt_open.visibility = View.GONE
        Database.saveResult(this, null)
    }

    /*
     * Abre a atividade que permite o uso da câmera
     * em toda a tela do device.
     * */
    fun openFullscreen(view: View){
        /*
         * Padrão Cláusula de Guarda - Sem permissão
         * de câmera: não abre atividade.
         * */
        if( !EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA) ){
            return
        }

        unlockCamera()

        val i = Intent(this, FullscreenActivity::class.java)
        i.putExtra(Database.KEY_IS_LIGHTENED, isLightened)
        startActivityForResult(i, REQUEST_CODE_FULLSCREEN)
    }

    /*
     * Como o lock não é mantido, o método abaixo é
     * necessário para que o usuário veja o unlock
     * ocorrendo, caso esteja a câmera como lock.
     * */
    private fun unlockCamera(){
        isLocked = true
        lockUnlock()
    }

    /*
     * Ativa e desativa a luz de flash do celular caso esteja
     * disponível no device.
     * */
    fun flashLight(view: View? = null){
        isLightened = !isLightened

        if(isLightened){
            z_xing_scanner.enableFlash(this, true)
            ib_flashlight.setImageResource(R.drawable.ic_flashlight_white_24dp)
        }
        else{
            z_xing_scanner.enableFlash(this, false)
            ib_flashlight.setImageResource(R.drawable.ic_flashlight_off_white_24dp)
        }

        z_xing_scanner.stopCamera()
        z_xing_scanner.startCamera( if(isLightened) 0 else 1 )
    }

    /*
     * Função responsável por mudar o status de funcionamento
     * do algoritmo de interpretação de código de barra
     * lido, incluindo a mudança do ícone de apresentação,
     * ao usuário, de status do algoritmo de interpretação de
     * código. Note que a luz e botão de flash não deve funcionar
     * se a CameraPreview estiver parada, stopped.
     * */
    fun lockUnlock(view: View? = null){
        isLocked = !isLocked

        if(isLocked){
            /*
             * Para funcionar deve ser invocado antes do
             * stopCameraPreview().
             * */
            turnOffFlashlight()

            /*
             * Parar com a verificação de códigos de barra com
             * a câmera.
             * */
            z_xing_scanner.stopCameraPreview()
            ib_lock.setImageResource(R.drawable.ic_lock_white_24dp)
            ib_flashlight.isEnabled = false
        }
        else{
            /*
             * Retomar a verificação de códigos de barra com
             * a câmera.
             * */
            z_xing_scanner.resumeCameraPreview(this)
            ib_lock.setImageResource(R.drawable.ic_lock_open_white_24dp)
            ib_flashlight.isEnabled = true
        }
    }

    /*
     * Método necessário, pois não faz sentido deixar a luz
     * de flash ligada quando a tela não mais está lendo
     * códigos, está travada. Método somente invocado quando
     * o lock de tela ocorre.
     * */
    private fun turnOffFlashlight(){
        isLightened = true
        flashLight()
    }
}