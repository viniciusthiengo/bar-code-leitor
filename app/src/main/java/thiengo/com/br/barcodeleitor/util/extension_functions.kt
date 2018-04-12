package thiengo.com.br.barcodeleitor.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Camera
import com.google.zxing.BarcodeFormat
import me.dm7.barcodescanner.core.CameraUtils
import me.dm7.barcodescanner.zxing.ZXingScannerView


fun ZXingScannerView.startCameraForAllDevices(context: Context){
    this.configCameraForAllDevices(context)

    /*
     * Sem nenhum parâmetro definido em startCamera(),
     * digo, o parâmetro idCamera, a câmera de ID 0
     * será a utilizada, ou seja, a câmera de tras
     * (rear-facing) do device. A câmera da frente
     * (front-facing) é a de ID 1.
     * */
    this.startCamera() /* Da API ZXingScannerView */

    /*
     * Para saber sobre recursos alocados - via
     * isCameraStarted()
     * */
    this.setTag(this.id, true)
}

/*
 * Método com algumas configurações iniciais possíveis
 * de serem aplicadas a interface da câmera. Todas as
 * configurações no método a seguir são opcionais e têm
 * seus valores padrões.
 * */
private fun ZXingScannerView.configCameraForAllDevices(context: Context){
    /*
     * Somente funciona se a câmera não estiver
     * ativa.
     * */
    this.setBackgroundColor(Color.TRANSPARENT)

    this.setBorderColor(Color.RED) /* Cor das extremidades */
    this.setLaserColor(Color.YELLOW) /* Cor da linha central de alinhamento com código de barra */
    //this.setMaskColor(Color.BLUE) /* Cor de todo o restante fora do quadrante de leitura de código. */

    /*
     * Sem o auto focus como true o poder de leitura de
     * código de barras reduz consideravelmente,
     * principalmente em devices que não têm uma câmera
     * de alta qualidade.
     * */
    this.setAutoFocus(true)

    /*
     * Tome cuidado com o uso da linha a seguir, pois
     * dependendo da rotação, a leitura perde em eficiência.
     * */
    this.rotation = 0.0F
    //this.rotation = 45.0F

    /*
     * Para definir os códigos que podem ser lidos - por
     * padrão todos os códigos suportados são passíveis de
     * serem lidos.
     * */
    //this.setFormats( listOf(BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX) )

    /*
     * Deve ser utilizado somente para o correto funcionamento
     * do CameraPreview em devices HUAWEI.
     * */
    this.setAspectTolerance(0.5F)
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
    (camera as Camera).release()
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
 * Como alguns devices não têm a luz de flash, é
 * necessária a verificação para a não geração de
 * exception.
 * */
fun ZXingScannerView.isFlashSupported(context: Context) =
    context
        .packageManager
        .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

/*
 * A ativação e desativação do flash somente pode
 * ocorrer caso haja suporte a este hardware.
 * */
fun ZXingScannerView.enableFlash(
    context: Context,
    status: Boolean) {

    if( this.isFlashSupported(context) ){
        this.flash = status
    }
}