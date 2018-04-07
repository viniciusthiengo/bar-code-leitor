package thiengo.com.br.barcodeleitor.util

import android.content.Context
import me.dm7.barcodescanner.zbar.BarcodeFormat
import me.dm7.barcodescanner.zbar.Result

class Database {
    companion object {

        private val SP_NAME = "SP"
        val KEY_CONTENTS = "contents"
        val KEY_BARCODE_ID = "barcode_id"
        val DEFAULT_BARCODE_ID = -1
        val KEY_IS_LOCKED = "is_locked"

        fun saveResult(context: Context, result: Result?){
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            val contents = result?.contents
            val barcodeId = result?.barcodeFormat?.id ?: DEFAULT_BARCODE_ID

            sp.edit()
                .putString(KEY_CONTENTS, contents)
                .putInt(KEY_BARCODE_ID, barcodeId)
                .apply()
        }

        fun saveIsLocked(context: Context, isLocked: Boolean){
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

            sp.edit()
                    .putBoolean(KEY_IS_LOCKED, isLocked)
                    .apply()
        }

        fun getSavedResult(context: Context): Result? {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            val result = Result()

            result.contents = sp.getString(KEY_CONTENTS, null)
            result.barcodeFormat = BarcodeFormat.getFormatById( sp.getInt(KEY_BARCODE_ID, DEFAULT_BARCODE_ID) )

            if( result.contents == null ){
                return null
            }
            return result
        }

        fun getSavedIsLocked(context: Context): Boolean {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            return sp.getBoolean(KEY_IS_LOCKED, false)
        }
    }
}