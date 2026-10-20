package com.serviciudad.presupuesto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/", "/login", "/dashboard", "/necesidades", "/necesidades/**",
        "/formulacion", "/crear-presupuesto", "/presupuesto-general",
        "/presupuesto-detallado", "/consolidado/**", "/configuracion-vigencia", "/admin/**"})
    public String application() {
        return "forward:/index.html";
    }
}
