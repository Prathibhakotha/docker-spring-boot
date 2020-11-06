package com.springboot.docker.dockerspringboot.resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/docker")
public class Hello {

    @Value("${testMessage.value}")
    String username;

    @GetMapping("/hello")
    public String hello() {
        return username;
    }
}
