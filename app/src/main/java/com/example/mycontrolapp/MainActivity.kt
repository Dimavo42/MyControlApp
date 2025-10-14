package com.example.mycontrolapp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mycontrolapp.ui.componentes.MainLayout
import com.example.mycontrolapp.ui.theme.MyControlAppTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyControlAppTheme {
                MainLayout()
            }
        }
    }
}










