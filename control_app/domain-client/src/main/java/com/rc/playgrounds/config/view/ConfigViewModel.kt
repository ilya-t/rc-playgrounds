package com.rc.playgrounds.config.view

class ConfigViewModel(
    val title: String,
    val rawJson: String,
    val saveError: String?,

    val saveEnabled: Boolean,
    val okBtn: suspend () -> Boolean,
    val saveBtn: () -> Unit,

    val nextEnabled: Boolean,
    val next: () -> Unit,

    val prevEnabled: Boolean,
    val prev: () -> Unit,
)