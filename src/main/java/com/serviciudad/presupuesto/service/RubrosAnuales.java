package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.entity.RubroPresupuestal;
import java.util.*;

/** Los códigos propios del año reemplazan la plantilla global para nuevas solicitudes. */
final class RubrosAnuales {
    private RubrosAnuales() {}
    static boolean concepto(RubroPresupuestal rubro) { return rubro.getCodigo().startsWith("FORM.CONCEPTO."); }
    static boolean plantillaAnterior(RubroPresupuestal rubro) { return rubro.getCodigo().startsWith("FORM.") && !concepto(rubro); }
    /** Claves internas del formulario existente; no son códigos presupuestales del Excel. */
    static boolean categoriaAuxiliar(RubroPresupuestal rubro) {
        return rubro.isCategoriaFormulacion() && rubro.getCodigo().startsWith("FORM.")
            && rubro.getRubroPadre() != null && !rubro.getRubroPadre().getCodigo().startsWith("FORM");
    }
    static Set<String> codigosPropios(List<RubroPresupuestal> catalogo, Long vigenciaId) {
        Set<String> result = new HashSet<>();
        catalogo.stream().filter(r -> r.getVigencia() != null && r.getVigencia().getId().equals(vigenciaId))
            .forEach(r -> result.add(r.getCodigo()));
        return result;
    }
    static List<RubroPresupuestal> efectivos(List<RubroPresupuestal> catalogo, Long vigenciaId) {
        Set<String> propios = codigosPropios(catalogo, vigenciaId);
        return catalogo.stream().filter(r -> r.getVigencia() == null ? !propios.contains(r.getCodigo())
            : r.getVigencia().getId().equals(vigenciaId)).toList();
    }
}
