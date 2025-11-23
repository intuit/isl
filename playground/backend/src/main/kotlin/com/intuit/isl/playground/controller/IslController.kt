package com.intuit.isl.playground.controller

import com.intuit.isl.playground.model.*
import com.intuit.isl.playground.service.IslService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class IslController(private val islService: IslService) {
    
    @PostMapping("/transform")
    fun transform(@RequestBody request: TransformRequest): ResponseEntity<TransformResponse> {
        val response = islService.transform(request)
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/validate")
    fun validate(@RequestBody request: ValidationRequest): ResponseEntity<ValidationResponse> {
        val response = islService.validate(request)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/examples")
    fun getExamples(): ResponseEntity<List<ExampleResponse>> {
        val examples = islService.getExamples()
        return ResponseEntity.ok(examples)
    }
    
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}

