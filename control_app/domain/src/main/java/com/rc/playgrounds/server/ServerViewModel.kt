package com.rc.playgrounds.server

class ServerViewModel(
    val title: String,
    val target: String,
    val onTargetChanged: (String) -> Unit,
)
