package com.nj031.onetask.data.task

import java.util.UUID

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val completed: Boolean = false
)
