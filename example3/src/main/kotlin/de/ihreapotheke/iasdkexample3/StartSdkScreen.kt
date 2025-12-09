package de.ihreapotheke.iasdkexample3

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.ihreapotheke.iasdkexample.R
import de.ihreapotheken.sdk.core.api.PresentationMode
import de.ihreapotheken.sdk.core.api.listener.CheckoutListener
import de.ihreapotheken.sdk.integrations.api.IaSdk
import de.ihreapotheken.sdk.integrations.api.TransferPrescriptionRequest
import de.ihreapotheken.sdk.integrations.api.view.IaSdkActivity
import java.io.ByteArrayOutputStream

@Composable
fun StartSdkScreen() {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Text(
            text = "Welcome to the Host App Start Screen",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )

        val context = LocalContext.current
        Button(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            onClick = {
                val drawable = ContextCompat.getDrawable(context, R.drawable.dummy_prescription)
                val bitmap = (drawable as BitmapDrawable).bitmap
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArrayImage = outputStream.toByteArray()

                val inputStreamPdf = context.resources.openRawResource(R.raw.test_prescription)
                val byteArrayPdf = inputStreamPdf.readBytes()

                val eprescription1 = "\n" +
                        "{\"urls\":[\"Task/test9ba2fee0d07e4ef2b6205f8012e1445b/\$accept?ac=5e24cc059ff244bdbb01efcccf834a6329bdac67a4a64733938fe1b799ac19a9\",\"Task/test6ffbb0e6a9d449ceb8c168be8d105403/\$accept?ac=b64b434f3a874c0a9bc110205e2d8d8a7283e8cfbd1b496f807fef7cc8299cb3\",\"Task/test6b7f0170fbc24ec7a467b3d23444f5d9/\$accept?ac=a7c07835565d48138d810f138e685252fa8580ee14ba4594879d6fa426bdb7c8\"]}"
                val eprescription2 =
                    "{\"urls\":[\"Task/test9ba2fee0d07e4ef2b6205f8012e1445b/\$accept?ac=5e24cc059ff244bdbb01efcccf834a6329bdac67a4a64733938fe1b799ac19a9\"]}"


                val listener = object : CheckoutListener {
                    override fun onCheckoutCompleted(hostOrderId: String, sdkOrderId: String) {
                        Toast.makeText(
                            context,
                            "onCheckoutCompleted: $hostOrderId : $sdkOrderId",
                            Toast.LENGTH_SHORT
                        ).show()
                        IaSdkActivity.finishAllActivities()
                    }
                }

                context.findActivity()?.let { activity ->
                    IaSdk.ordering
                        .setCheckoutListener(listener)
                        .transferPrescriptions(
                            context = activity,
                            transferPrescriptionRequest = TransferPrescriptionRequest(
                                images = listOf(byteArrayImage),
                                pdfs = listOf(byteArrayPdf),
                                codes = listOf(eprescription1, eprescription2),
                                orderId = "ORDER-123",
                            ),
                            presentationMode = PresentationMode.FULL_FLOW,
                        )
                }

            }) {
            Text(
                text = "Transfer Prescription Data and open Cart",
                textAlign = TextAlign.Center
            )
        }
    }
}

fun Context.findActivity(): Activity? = generateSequence(this as Context?) { ctx ->
    (ctx as? ContextWrapper)?.baseContext
}.filterIsInstance<Activity>()
    .firstOrNull()
