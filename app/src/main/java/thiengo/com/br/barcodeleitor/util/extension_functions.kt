package thiengo.com.br.barcodeleitor.util

import android.content.Context
import android.graphics.Color
import android.hardware.Camera
import android.util.Log
import me.dm7.barcodescanner.core.CameraUtils
import me.dm7.barcodescanner.zxing.ZXingScannerView


fun ZXingScannerView.startCameraForAllDevices(context: Context){
    this.confiCameraForAllDevices(context)
    this.startCamera() /* Da API ZXingScannerView */

    /*
     * Para saber sobre recursos alocados - via
     * isCameraStarted()
     * */
    this.setTag(this.id, true)
}

/*
 * Método com algumas configurações iniciais possíveis
 * de serem aplicadas a interface da câmera.
 * */
private fun ZXingScannerView.confiCameraForAllDevices(context: Context){
    /*
     * Somente funciona se a câmera não estiver
     * ativa.
     * */
    this.setBackgroundColor(Color.TRANSPARENT)

    this.setBorderColor(Color.RED) /* Cor das extremidades */
    this.setLaserColor(Color.YELLOW) /* Cor da linha central de alinhamento com código de barra */
    // this.setMaskColor(Color.GRAY) /* Cor de todo o restante fora do quadrante de leitura de código. */

    /*
     * Não surtiu efeito algum em testes.
     * */
    // this.setAutoFocus(true)  /* true é padrão */

    /*
     * Tome cuidado com o uso da linha a seguir, pois
     * dependendo da rotação, a leitura perde em eficiência.
     * */
    // this.rotation = 0.0F /* 0.0F é padrão */
}

fun ZXingScannerView.stopCameraForAllDevices(){
    /*
     * Para liberação de recursos, pare a câmera
     * logo no onPause() da atividade ou fragmento.
     * */
    this.stopCamera()
    this.releaseForAllDevices()

    /*
     * Para saber sobre recursos liberados - via
     * isCameraStarted()
     * */
    this.setTag(this.id, false)
}

private fun ZXingScannerView.releaseForAllDevices(){
    /*
     * O algoritmo a seguir é necessário, pois caso
     * contrário, em alguns devices, os recursos da
     * câmera não serão liberados e a invocação dela
     * em uma próxima atividade / fragmento não
     * funcionará.
     * */
    val camera = CameraUtils.getCameraInstance()
    if( camera != null ){
        (camera as Camera).release()
    }
}

/*
 * Como não há uma maneira nativa de saber se o método
 * startCamera() já foi invocado, o método abaixo, com
 * apoio de setTag() da View de leitura de código de
 * barra, faz isso para nós.
 * */
fun ZXingScannerView.isCameraStarted(): Boolean{
    val startData = this.getTag(this.id)
    val startStatus = (startData ?: false) as Boolean
    return startStatus
}

/*
 * Como alguns deveices não têm a luz de flash, é
 * necessária a verificação para a não geração de
 * exception.
 * */
fun ZXingScannerView.isFlashSupported(): Boolean{
    val camera = CameraUtils.getCameraInstance()
    //this.getI

    camera.reconnect()
    Log.i("LOG", "1")
    if( camera != null ){
        Log.i("LOG", "2")
        //return CameraUtils.isFlashSupported( camera )
    }
    return false
}