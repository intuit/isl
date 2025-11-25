package com.intuit.isl.playground

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IslPlaygroundApplication

fun main(args: Array<String>) {
    runApplication<IslPlaygroundApplication>(*args)
}

