package com.kokodi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KokodiApplication

fun main(args: Array<String>) {
    runApplication<KokodiApplication>(*args)
} 