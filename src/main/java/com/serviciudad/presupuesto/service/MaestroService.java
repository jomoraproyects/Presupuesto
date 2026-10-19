package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class MaestroService {
    private final VigenciaRepository vigencias;
    private final RubroRepository rubros;
    private final NecesidadRepository necesidades;
    private final DistribucionRepository distribuciones;
    private final MapeoRubroServicioRepository mapeos;
    public MaestroService(VigenciaRepository vigencias, RubroRepository rubros, NecesidadRepository necesidades,
        DistribucionRepository distribuciones, MapeoRubroServicioRepository mapeos) {
        this.vigencias = vigencias; this.rubros = rubros; this.necesidades = necesidades;
        this.distribuciones = distribuciones; this.mapeos = mapeos;
    }

    public MaestroResponse get(Long vigenciaId) {
        vigencias.findById(vigenciaId).orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        Map<Long, Node> nodes = new LinkedHashMap<>();
        List<RubroPresupuestal> catalog = rubros.findAllByOrderByOrdenAscIdAsc();
        Set<String> scopedCodes = RubrosAnuales.codigosPropios(catalog, vigenciaId);
        catalog.stream()
            .filter(r -> r.getVigencia() == null || r.getVigencia().getId().equals(vigenciaId))
            .forEach(r -> nodes.put(r.getId(), new Node(r)));
        record Key(Long rubroId, Long servicioId) {}
        Map<Key, Long> destinations = new HashMap<>();
        mapeos.findByVigenciaIdOrderByIdAsc(vigenciaId).stream().filter(MapeoRubroServicio::isActivo)
            .forEach(m -> destinations.put(new Key(m.getRubroOrigen().getId(), m.getServicio().getId()), m.getRubroDestino().getId()));
        List<MapeoPendienteResponse> pending = new ArrayList<>();
        for (var share : distribuciones.byRubroServicio(vigenciaId, ConsolidadoService.INCLUDED)) {
            BigDecimal requested = zero(share.getSolicitado()), approved = zero(share.getAprobado());
            Long destination = destinations.get(new Key(share.getRubroId(), share.getServicioId()));
            if (destination == null && (requested.signum() != 0 || approved.signum() != 0)) {
                pending.add(new MapeoPendienteResponse(share.getRubroId(), share.getRubro(), share.getServicioId(), share.getServicio(), requested, approved));
            }
            // Un mapeo pendiente conserva su importe en el origen; nunca se descarta dinero.
            add(nodes, destination == null ? origenGeneral(nodes, share.getRubroId()) : destination, requested, approved);
        }
        for (var item : necesidades.findByVigenciaIdAndActivaTrueOrderByIdAsc(vigenciaId)) {
            if (!ConsolidadoService.INCLUDED.contains(item.getEstado())) continue;
            BigDecimal requestedGeneral = item.getValorTotalSolicitado();
            BigDecimal approvedGeneral = zero(item.getValorTotalAprobado());
            for (var d : item.getDistribuciones()) {
                requestedGeneral = requestedGeneral.subtract(d.getValorSolicitadoDistribuido());
                approvedGeneral = approvedGeneral.subtract(d.getValorAprobadoDistribuido());
            }
            add(nodes, origenGeneral(nodes, item.getRubroPresupuestal().getId()), requestedGeneral, approvedGeneral);
        }
        List<Node> roots = new ArrayList<>();
        for (Node n : nodes.values()) {
            if (n.rubro.getRubroPadre() == null) roots.add(n);
            else {
                Node parent = nodes.get(n.rubro.getRubroPadre().getId());
                if (parent == null) throw new BusinessException("Un rubro del maestro tiene un padre ajeno a la vigencia");
                parent.children.add(n);
            }
        }
        List<MaestroRubroResponse> tree = roots.stream().map(n -> build(n, scopedCodes)).filter(Objects::nonNull).toList();
        BigDecimal requested = tree.stream().map(MaestroRubroResponse::solicitado).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        BigDecimal approved = tree.stream().map(MaestroRubroResponse::aprobado).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new MaestroResponse(vigenciaId, requested, approved, approved.subtract(requested), pending.isEmpty(), pending, tree);
    }
    private Long origenGeneral(Map<Long, Node> nodes, Long id) {
        Node node = nodes.get(id);
        return node != null && RubrosAnuales.categoriaAuxiliar(node.rubro)
            ? node.rubro.getRubroPadre().getId() : id;
    }
    private void add(Map<Long, Node> nodes, Long id, BigDecimal requested, BigDecimal approved) {
        Node node = nodes.get(id);
        if (node == null) throw new BusinessException("Hay solicitudes o mapeos con rubros ajenos a esta vigencia");
        node.requested = node.requested.add(requested); node.approved = node.approved.add(approved);
    }
    private MaestroRubroResponse build(Node node, Set<String> scopedCodes) {
        // El importe directo ya se asignó al rubro general. No se muestra otra fila de movimiento.
        if (RubrosAnuales.categoriaAuxiliar(node.rubro)) return null;
        List<MaestroRubroResponse> children = node.children.stream().map(n -> build(n, scopedCodes)).filter(Objects::nonNull).toList();
        BigDecimal requested = node.requested.add(children.stream().map(MaestroRubroResponse::solicitado).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal approved = node.approved.add(children.stream().map(MaestroRubroResponse::aprobado).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (!node.rubro.isActivo() && children.isEmpty() && requested.signum() == 0 && approved.signum() == 0) return null;
        if (node.rubro.getVigencia() == null && scopedCodes.contains(node.rubro.getCodigo())
            && children.isEmpty() && requested.signum() == 0 && approved.signum() == 0) return null;
        return new MaestroRubroResponse(node.rubro.getId(), node.rubro.getCodigo(), node.rubro.getNombre(), node.rubro.getNivel(),
            requested, approved, approved.subtract(requested), children, node.requested, node.approved);
    }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO.setScale(2) : value; }
    private static class Node {
        final RubroPresupuestal rubro;
        final List<Node> children = new ArrayList<>();
        BigDecimal requested = BigDecimal.ZERO.setScale(2), approved = BigDecimal.ZERO.setScale(2);
        Node(RubroPresupuestal rubro) { this.rubro = rubro; }
    }
}
