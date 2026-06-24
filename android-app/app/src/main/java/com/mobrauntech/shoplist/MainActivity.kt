package com.mobrauntech.shoplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mobrauntech.shoplist.ui.ShopScreen
import com.mobrauntech.shoplist.ui.ShopViewModel
import com.mobrauntech.shoplist.ui.theme.Bg
import com.mobrauntech.shoplist.ui.theme.ShopListTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels {
        ShopViewModel.Factory((application as ShopListApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShopListTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Bg), color = Bg) {
                    ShopScreen(viewModel)
                }
            }
        }
    }
}
