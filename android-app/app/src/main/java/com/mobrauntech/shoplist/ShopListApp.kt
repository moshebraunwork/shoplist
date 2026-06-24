package com.mobrauntech.shoplist

import android.app.Application
import com.mobrauntech.shoplist.data.repo.ShopRepository

class ShopListApp : Application() {
    val repository: ShopRepository by lazy { ShopRepository(this) }
}
