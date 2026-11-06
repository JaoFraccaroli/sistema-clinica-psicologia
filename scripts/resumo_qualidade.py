"""Monta o resumo de qualidade (testes, cobertura e mutação) a partir dos relatórios do Maven.

Usado no GitHub Actions: python3 scripts/resumo_qualidade.py >> $GITHUB_STEP_SUMMARY
"""
import csv
import glob
import xml.etree.ElementTree as ET
from pathlib import Path


def testes():
    total = falhas = erros = pulados = 0
    for arquivo in glob.glob("target/surefire-reports/TEST-*.xml"):
        raiz = ET.parse(arquivo).getroot()
        total += int(raiz.get("tests", 0))
        falhas += int(raiz.get("failures", 0))
        erros += int(raiz.get("errors", 0))
        pulados += int(raiz.get("skipped", 0))
    return total, falhas + erros, pulados


def cobertura():
    arquivo = Path("target/site/jacoco/jacoco.csv")
    if not arquivo.exists():
        return None
    cobertas = perdidas = 0
    with arquivo.open() as f:
        for linha in csv.DictReader(f):
            cobertas += int(linha["LINE_COVERED"])
            perdidas += int(linha["LINE_MISSED"])
    return 100 * cobertas / max(1, cobertas + perdidas)


def mutacao():
    arquivo = Path("target/pit-reports/mutations.xml")
    if not arquivo.exists():
        return None
    mutantes = ET.parse(arquivo).getroot().findall("mutation")
    mortos = [m for m in mutantes if m.get("detected") == "true"]
    vivos = [m for m in mutantes if m.get("detected") != "true"]
    return len(mutantes), len(mortos), vivos


total, problemas, pulados = testes()
print("## Resumo de qualidade\n")
print("| Métrica | Resultado |")
print("|---|---|")
print(f"| Testes executados | {total} ({'✅ todos passaram' if problemas == 0 else f'❌ {problemas} com falha'}) |")
if pulados:
    print(f"| Testes pulados | {pulados} |")
cob = cobertura()
print(f"| Cobertura de linhas (JaCoCo) | {f'{cob:.1f}%' if cob is not None else 'não gerada'} |")
mut = mutacao()
if mut:
    qtd, mortos, vivos = mut
    print(f"| Mutation score (PIT) | {100 * mortos / max(1, qtd):.1f}% ({mortos} de {qtd} mutantes mortos) |")
    if vivos:
        print("\n### Mutantes que sobreviveram\n")
        for m in vivos:
            print(f"- `{m.findtext('mutatedClass')}.{m.findtext('mutatedMethod')}` linha {m.findtext('lineNumber')}: {m.findtext('description')}")
else:
    print("| Mutation score (PIT) | não gerado |")
