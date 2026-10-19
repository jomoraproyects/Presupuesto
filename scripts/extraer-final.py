"""Lee el catálogo de Ff.xlsx/final; no importa montos históricos ni modifica el Excel.

Uso: python scripts/extraer-final.py C:/ruta/Ff.xlsx docs/catalogo-final.json
Requiere openpyxl. El segundo argumento es un archivo JSON de configuración.
"""
import json
import re
import sys
from collections import OrderedDict
from openpyxl import load_workbook

book = load_workbook(sys.argv[1], data_only=False)
sheet = book['final']
source = []
for cells in sheet.iter_rows(min_row=3):
    code, name, level = [cell.value for cell in cells[:3]]
    if code is None:
        continue
    if not isinstance(name, str) or not isinstance(level, (int, float)):
        raise ValueError(f'Fila {cells[0].row}: catálogo incompleto')
    # Las tres cabeceras numéricas del Excel se guardan como el texto que muestran.
    if isinstance(code, (int, float)):
        code = format(code, 'g')
    if not isinstance(code, str):
        raise ValueError(f'Fila {cells[0].row}: código no interpretable')
    source.append(dict(fila=cells[0].row, codigo=code, nombre=name, nivelFuente=int(level)))

unique = OrderedDict()
for row in source:
    unique.setdefault(row['codigo'], dict(row))
for row in unique.values():
    # La columna Nivel y la posición definen la jerarquía, incluso si un código
    # contiene una coma o un guion inusual. El texto del código nunca se corrige.
    candidates = [r for r in source if r['fila'] < row['fila'] and r['nivelFuente'] < row['nivelFuente']]
    parent = candidates[-1] if candidates else None
    row['padre'] = parent['codigo'] if parent else None
    row['movimiento'] = row['nivelFuente'] == 11

# Las categorías siguen siendo hojas del formulario, separadas de los títulos finales.
# FORM.* es una clave interna reutilizando el modelo existente, nunca un código del Excel.
categories = []
seen = set()
group = []
services = set()
def flush():
    global group, services
    if group:
        first = group[0][0]
        parent = unique[first['codigo']]['padre']
        if parent and not first['codigo'].startswith('2.4.1.'):
            name = re.split(r'\b(?:Acueducto|Alcantarillado|Aseo)\b', first['nombre'], flags=re.I)[0].strip(' -/')
            categories.append(dict(codigo=f"FORM.XLSX.{first['fila']}", nombre=name or first['nombre'],
                padre=parent, fila=first['fila'], destinos=[dict(servicio=s, codigo=r['codigo']) for r, s in group]))
    group = []
    services = set()

for row in source:
    if row['nivelFuente'] != 11:
        flush()
        continue
    if row['codigo'] in seen:
        continue
    seen.add(row['codigo'])
    if row['codigo'].startswith('2.4.1.'):
        flush()
        continue
    match = re.search(r'[.,](21|22|23)(?:-|\s)', row['codigo'])
    if not match:
        raise ValueError(f"Fila {row['fila']}: servicio no identificado en {row['codigo']!r}")
    service = match.group(1)
    if service in services or (group and unique[group[0][0]['codigo']]['padre'] != unique[row['codigo']]['padre']):
        flush()
    group.append((row, service))
    services.add(service)
flush()
result = dict(archivo=book.properties.title or 'Ff.xlsx', hoja='final', filas=source,
              rubros=list(unique.values()), categorias=categories)
with open(sys.argv[2], 'w', encoding='utf-8') as target:
    json.dump(result, target, ensure_ascii=False, indent=2)
print(f'{len(source)} filas; {len(unique)} códigos distintos; {len(source)-len(unique)} repeticiones; {len(categories)} categorías sin nómina.')
