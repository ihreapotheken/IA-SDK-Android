package de.ihreapotheke.iasdkexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.ihreapotheke.iasdkexample.ui.theme.IASDKExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            IASDKExampleTheme {
                AppScaffold()
            }
        }
    }
}
