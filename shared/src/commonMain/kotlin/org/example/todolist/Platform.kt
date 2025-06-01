package org.example.todolist

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform