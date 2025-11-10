"""
Script para copiar módulos de función desde CRY hacia GDC.

Requerimiento: docs/requirements/pr_object_copy.md
- Sistema fuente: CRY (grupo ZFIE1017_1)
- Sistema destino: GDC (grupo ZFIAAC002_1)
- Transporte: CADK910827
- Paquete: ZFI
"""

import os
from dotenv import load_dotenv
from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.navigation_service import NavigationService
from app.services.creation_service import CreationService

load_dotenv()

# Definir los módulos a copiar con sus nuevos nombres
MODULES_TO_COPY = [
    {
        "source": "ZFIE1017_DMEE_VALOR_TOTAL_2DEC",
        "target": "ZFIAAC002_DMEE_VALOR_TOTAL_2DEC",
        "description": "Valor total con 2 decimales"
    },
    {
        "source": "ZFIE1017_DMEE_CONSECUTIVO_PAGO",
        "target": "ZFIAAC002_DMEE_CONSECUTIVO_PAGO",
        "description": "Consecutivo de pago"
    },
    {
        "source": "ZFIE1017_DMEE_CONSEC_OCCIDENT",
        "target": "ZFIAAC002_DMEE_CONSEC_OCCIDENT",
        "description": "Consecutivo Occidental"
    },
    {
        "source": "ZFIE1017_DMEE_NIT_CC",
        "target": "ZFIAAC002_DMEE_NIT_CC",
        "description": "NIT o Cédula"
    },
    {
        "source": "ZFIE1017_DMEE_FORMA_DE_PAGO_OC",
        "target": "ZFIAAC002_DMEE_FORMA_DE_PAGO_OC",
        "description": "Forma de pago Occidental"
    },
    {
        "source": "ZFIE1017_DMEE_VALOR_POSIC_2DEC",
        "target": "ZFIAAC002_DMEE_VALOR_POSIC_2DEC",
        "description": "Valor posición con 2 decimales"
    },
    {
        "source": "ZFIE1017_DMEE_COMPROBANTE",
        "target": "ZFIAAC002_DMEE_COMPROBANTE",
        "description": "Número de comprobante"
    },
    {
        "source": "ZFIE1017_DMEE_TP_CUENT_DESTINO",
        "target": "ZFIAAC002_DMEE_TP_CUENT_DESTI",  # Truncado a 30 caracteres
        "description": "Tipo cuenta destino"
    }
]

TRANSPORT = "CADK910827"
PACKAGE = "ZFI"
SOURCE_FG = "ZFIE1017_1"
TARGET_FG = "ZFIAAC002_1"


def test_copy_function_modules():
    """Test para copiar módulos de función entre sistemas."""

    # Configuración para CRY (sistema fuente)
    cry_config = SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )

    # Configuración para GDC (sistema destino) - usar variables GDC_*
    gdc_config = SAPConfig(
        ashost=os.getenv("GDC_ASHOST", os.getenv("SAP_ASHOST", "")),
        sysnr=os.getenv("GDC_SYSNR", os.getenv("SAP_SYSNR", "")),
        client=os.getenv("GDC_CLIENT", os.getenv("SAP_CLIENT", "")),
        user=os.getenv("GDC_USER", os.getenv("SAP_USER", "")),
        passwd=os.getenv("GDC_PASSWD", os.getenv("SAP_PASSWD", "")),
        lang=os.getenv("GDC_LANG", "EN"),
        saprouter=os.getenv("GDC_ROUTER", os.getenv("SAP_ROUTER")),
    )

    # Crear pools de conexión
    cry_pool = RfcConnectionPool(cry_config, pool_size=1)
    gdc_pool = RfcConnectionPool(gdc_config, pool_size=1)

    # Crear servicios
    cry_nav_service = NavigationService(cry_pool)
    gdc_creation_service = CreationService(gdc_pool)

    print(f"\n{'='*80}")
    print(f"INICIO DE COPIA DE MÓDULOS DE FUNCIÓN")
    print(f"{'='*80}")
    print(f"Sistema fuente: CRY - Grupo: {SOURCE_FG}")
    print(f"Sistema destino: GDC - Grupo: {TARGET_FG}")
    print(f"Transporte: {TRANSPORT}")
    print(f"Paquete: {PACKAGE}")
    print(f"Módulos a copiar: {len(MODULES_TO_COPY)}")
    print(f"{'='*80}\n")

    success_count = 0
    error_count = 0

    for idx, module in enumerate(MODULES_TO_COPY, 1):
        source_name = module["source"]
        target_name = module["target"]
        description = module["description"]

        print(f"\n[{idx}/{len(MODULES_TO_COPY)}] Procesando: {source_name}")
        print(f"    → Destino: {target_name}")
        print(f"    → Descripción: {description}")

        try:
            # 1. Obtener código fuente desde CRY
            print(f"    [1/3] Obteniendo código fuente desde CRY...")
            source_uri = f"/sap/bc/adt/functions/groups/{SOURCE_FG.lower()}/fmodules/{source_name.lower()}/source/main"
            source_result = cry_nav_service.get_object_source(source_uri)

            if not source_result.get("source"):
                print(f"    ❌ ERROR: No se pudo obtener el código fuente")
                error_count += 1
                continue

            source_code = source_result["source"]
            print(f"    ✓ Código fuente obtenido ({len(source_code)} caracteres)")

            # 2. Reemplazar nombre del módulo en el código fuente
            print(f"    [2/3] Renombrando referencias en el código...")
            modified_source = source_code.replace(
                f"FUNCTION {source_name.lower()}",
                f"FUNCTION {target_name.lower()}"
            ).replace(
                f"FUNCTION {source_name.upper()}",
                f"FUNCTION {target_name.upper()}"
            )
            print(f"    ✓ Código renombrado")

            # 3. Crear módulo en GDC
            print(f"    [3/3] Creando módulo en GDC...")
            result = gdc_creation_service.create_function_module(
                function_module_name=target_name,
                function_group_name=TARGET_FG,
                package=PACKAGE,
                description=description,
                transport=TRANSPORT
            )

            if result.get("success"):
                print(f"    ✅ ÉXITO: Módulo {target_name} creado correctamente")
                success_count += 1
            else:
                print(f"    ⚠️  ADVERTENCIA: {result.get('message', 'Error desconocido')}")
                error_count += 1

        except Exception as e:
            print(f"    ❌ ERROR: {str(e)}")
            error_count += 1

    # Resumen final
    print(f"\n{'='*80}")
    print(f"RESUMEN DE COPIA")
    print(f"{'='*80}")
    print(f"✅ Exitosos: {success_count}/{len(MODULES_TO_COPY)}")
    print(f"❌ Errores: {error_count}/{len(MODULES_TO_COPY)}")
    print(f"{'='*80}\n")

    # Assert para pytest
    assert success_count > 0, "No se pudo copiar ningún módulo"


if __name__ == "__main__":
    test_copy_function_modules()
