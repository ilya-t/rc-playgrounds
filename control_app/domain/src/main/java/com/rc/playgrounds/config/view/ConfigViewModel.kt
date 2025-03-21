package com.rc.playgrounds.config.view

class ConfigViewModel(
    val title: String,
    val rawJson: String,

    val saveEnabled: Boolean,
    val save: () -> Unit,

    val nextEnabled: Boolean,
    val next: () -> Unit,

    val prevEnabled: Boolean,
    val prev: () -> Unit,
)