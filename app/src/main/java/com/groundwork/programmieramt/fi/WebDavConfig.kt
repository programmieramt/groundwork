package com.groundwork.programmieramt.fi

data class WebDavConfig(
    val url: String,
    val username: String,
    val password: String,
    val trustAllCerts: Boolean = false
)
