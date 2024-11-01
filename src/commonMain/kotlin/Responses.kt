package com.xemantic.anthropic

import kotlinx.serialization.Serializable

@Serializable
abstract class Response(val type: String)
