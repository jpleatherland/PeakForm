package dev.jpleatherland.peakform

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text =
                    """
                    Why we request Health Connect permissions:
                    
                    We use your health data (body weight and nutrition information) only to sync with your weight tracking app and display it to you. We never share or sell your data, and all information stays on your device. For more information, see our Privacy Policy at https://yourdomain.com/privacy.
                    """.trimIndent()
                textSize = 18f
                setPadding(32, 32, 32, 32)
            },
        )
    }
}
