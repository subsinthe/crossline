package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import com.example.subsinthe.crossline.network.ISocketFactory
import com.example.subsinthe.crossline.soulseek.Credentials
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.loggerFor
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.android.Main
import org.jetbrains.anko.AnkoComponent
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.button
import org.jetbrains.anko.dip
import org.jetbrains.anko.padding
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.setContentView
import org.jetbrains.anko.toast
import org.jetbrains.anko.verticalLayout
import kotlin.coroutines.experimental.CoroutineContext
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.soulseek.Client as SoulseekClient

private class UiScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main
}

private class IoScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO
}

class MainActivity : AppCompatActivity() {
    private val uiScope = UiScope()
    private val ioScope = IoScope()
    private val socketFactory = SocketFactory(ioScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())
        MainActivityUI(uiScope, ioScope, socketFactory).setContentView(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        socketFactory.close()
    }
}

class MainActivityUI(
    private var uiScope: CoroutineScope,
    private val ioScope: CoroutineScope,
    private val socketFactory: ISocketFactory
) : AnkoComponent<MainActivity> {
    private val customStyle = { v: Any ->
        when (v) {
            is Button -> v.textSize = 26f
            is EditText -> v.textSize = 24f
        }
    }

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            padding = dip(32)

            button("Log in") {
                onClick {
                    try {
                        val client = SoulseekClient.build(uiScope, socketFactory)
                        client.login(
                            Credentials(
                                username = "username",
                                password = "password"
                            )
                        )
                        toast("Successfully logged in")
                    } catch (ex: Throwable) {
                        toast("Error logging in: $ex")
                    }
                }
            }
        }.applyRecursively(customStyle)
    }

    private companion object {
        val LOG = loggerFor<MainActivityUI>()
    }
}
