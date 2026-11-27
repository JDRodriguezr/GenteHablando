package com.example.gentehablando.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R
import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.DemoMessage

@Composable
fun ChatMessage.displayText(): String = when (demoText) {
    null -> text
    DemoMessage.CONNECTED -> stringResource(R.string.demo_connected)
    DemoMessage.REPLY_1 -> stringResource(R.string.demo_reply_1)
    DemoMessage.REPLY_2 -> stringResource(R.string.demo_reply_2)
    DemoMessage.REPLY_3 -> stringResource(R.string.demo_reply_3)
    DemoMessage.REPLY_4 -> stringResource(R.string.demo_reply_4)
    DemoMessage.REPLY_5 -> stringResource(R.string.demo_reply_5)
}
