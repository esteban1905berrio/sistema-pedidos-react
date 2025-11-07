**Carga de Saldos de Activos y Datos maestros**

\<FIAAC002\>

El proceso de carga de activos fijos busca asegurar que SAP FI-AA inicie operaciones con los datos correctos de los bienes, manteniendo la trazabilidad contable, técnica y fiscal. La carga se efectuará mediante una plantilla Excel que contendrá los datos maestros, subnúmeros, valores de capitalización y depreciaciones acumuladas. El sistema deberá procesar tanto cargas en periodo cerrado como interanuales de los bienes muebles e inmuebles, para las áreas de valoración 01, 17, 18 (Valorización, Deterioro y avalúo técnico)

Se partirá con la extracción de activos contables gestionados actualmente en SAP y las plantillas de de carga se adicionarán de manera manual los saldos provenientes de ALMERA, sin embargo estos saldos deben coincidir con los saldos contables de balance detallando valor por valor del activo de acuerdo a la estructura de la plantilla de carga. 

1. # **DETALLES DE LA PANTALLA DE SELECCIÓN**

El programa debe contener una ruta para cargar un archivo tipo Excel, un botón en ejecución en modo test y en ejecución real el cual debe generar un log de resultados del proceso tanto en test como en real. 

Al realizar el procesamiento, el programa permitirá la descarga del log de resultados en Excel como resultado de la ejecución, también permitirá visualizar en pantalla

El programa Z permitirá al usuario cargar la plantilla en formato Excel definida la cual cuenta con todos los parámetros necesarios para la carga de maestro de activos fijos y su respectiva valoración.


La plantilla de Excel no debe contener caracteres especiales, y las clases de activos estarán homologados desde la extracción. 

| Nombre | Parámetro/Opción de selección | Comentarios (Rangos, Selecciones múltiples/sencillas, obligatorio, etc)  | Valor por defecto |
| :---: | ----- | ----- | :---: |
| Fich.Entrada |  | Permite acceso a una ruta del PC | N/A |
| Ejecución modo test | Check box | Obligatorio | X |
| Análisis de errores | Check box | Opcional | N/A |

2. # **CONDICIONES DE INICIO Y PREREQUISITOS** 

El proceso de carga se ejecutará mediante el programa Z, el cual simulará las transacciones estándar AS91 y AS94 para la creación de activos fijos, subnúmeros y sus valorizaciones por clase de activo y sociedad. Se implementará utilizando la BAPI estándar BAPI\_FIXEDASSET\_CREATE1.

Se podrá cargar en periodo cerrado, es decir, con corte al 31 de Diciembre, y el programa también permitirá la carga interanual

• Carga en periodo cerrado: se realizará con corte al 31 de diciembre, incluyendo valores históricos de capitalización y depreciación acumulada.

• Carga interanual: incluirá, además de los valores anteriores, las altas, valorizaciones especiales y sus depreciaciones por cada área de valoración ocurridas entre enero y la fecha de carga con valores acumulados.

El sistema debe considerar la carga de los datos maestros y los campos Z ampliados al dato maestro. 

Los datos deben venir homologados, sin caracteres especiales, y no se incluirán activos fijos en curso, estos activos se cargarán a través de la liquidación de proyectos, por lo tanto se excluyen de este proceso de carga. 

## **4.1 Validaciones previas del sistema**

A partir del archivo seleccionado, el sistema deberá ejecutar las siguientes validaciones:

* En modo **Simulación**, el sistema no generará documentos contables, únicamente el log de resultados con los errores y advertencias encontradas.  
* En modo **Ejecución Real**, el sistema deberá:  
  * Relacionar los documentos FI Contabilizados, saldo del activo y números de activos.  
  * Registrar los resultados en la **ruta de log de resultados** tanto en test como en real.  
  * Procesar los registros en estado correcto y excluir los que están en estado de error.   
  * El programa debe permitir el procesamiento de forma visible y en proceso de fondo. 

La estructura del archivo compone la carga de datos maestros y su valoración por cada área de valoración

Datos Maestro:

![][image3] 

Para las cargas interanuales se deben llenan los siguientes campos de la plantilla: (Valor acumulado de adquisición, Revalorización acumulada en valor reposición, Amortización normal acumulada, Revalorización acumulada de amortización normal)

Valorización del activo:

![][image4]

*En esta versión 6.8 sap, como se cargan los saldos contables de activos versión nueva de activos fijos*

*Para la carga de saldos contables de activos fijos para esta versión de producto se hacen por cuenta e mayor o se hacen directamente en el modulo considerando que existe la nueva contabilidad de activos y está activa para este cliente.* 

3. # **SUPUESTOS**

Los siguientes supuestos se consideran válidos para la correcta ejecución del proceso de carga inicial de activos en SAP:

* La Gobernación entrega la información de activos fijos de forma consolidada y revisada antes de la carga.

* Los archivos de carga cumplen con el formato estándar definido (columnas, tipos de dato, campos obligatorios).  
    
* Los catálogos externos (ubicaciones, clases de activo, centros de costo, dependencias) están previamente homologados.  
    
* **Homologación de datos:** Esta homologación se realizará en el proceso de extracción y/o depuración manual de los datos, por lo tanto se entiende que los valores a cargar vienen previamente homologados con las nuevas estructuras o listas definidas en sap

4. # **REGLAS DE NEGOCIO**

Las siguientes reglas aplican para garantizar consistencia, control y trazabilidad durante el proceso de carga

• El número de activo y subnúmero se generan de acuerdo con la secuencia interna definida por SAP.

• Los valores monetarios deben estar expresados en COP.

• La fecha de capitalización debe ser anterior a la fecha del año fiscal vigente.

• El sistema debe validar que el centro de costo y las áreas de valoración existan en SAP.

• Debe generarse un log detallado de activos creados, errores y tiempos de ejecución

5. # **UNIDADES DE MEDIDA Y MONEDA**

COP (Peso Colombiano).  
UN (Unidad)

6. # **MAPEO DE DATOS**

El mapeo de datos asegura que cada campo del archivo de origen (inventario físico, contabilidad, catastros, avalúos) se asigne correctamente a los campos de SAP FI-AA, cumpliendo reglas de obligatoriedad, formato y consistencia.

La estructura de datos corresponde a los campos requeridos por la BAPI\_FIXEDASSET\_CREATE1, Ver detalle de los campos en la plantilla adjunta:

7. # **REQUERIMIENTOS DE SEGURIDAD / AUTORIZACIONES**

El usuario ejecutor deberá contar con los **roles y autorizaciones FI/AA y GL**  necesarias para contabilizar documentos 

| Objeto de autorización | Campo autorización | Actividad | Observaciones |
| :---: | :---: | :---: | :---: |
| BKPF – BURKS | Sociedad | Contabilizar |  |
| T093 \- AFAPL  | Plan de Valoración | Contabilizar |  |

Tabla 6\. Requerimientos de seguridad

8. # **LOG DE RESULTADOS Y MANEJO DE ERRORES** 

**Log de ejecución en modo prueba**:

* Cantidad total de registros en la plantilla.  
* Cantidad de registros con estructura correcta.  
* Identificación de registros con errores (incluyendo los campos con error), con opción de exportar a Excel.

**Log de resultados**:

* Si la carga es exitosa, mostrar cantidad de registros creados/actualizados relacionando el documento de activos y número de activo fijo.   
* Si se rechaza el archivo, listar los registros con error y los campos afectados.

**Manejo de errores**:

* Si la plantilla presenta errores en su estructura o posiciones, procesar los registros exitosos y relacionar el documento de activos. 

9. # **TABLAS ESTRUCTURAS DE DATOS**

Ver plantilla de excel

| Nombre tabla | ANLA  |
| :---- | :---- |
| **Descripción** | Tabla de datos maestros de activos (números de activo, clase, sociedad, etc.) |
| **Tamaño de datos** | Variable (depende de número de activos a migrar, estimado: 10.000 registros) |
| **Clase de datos** | Maestros de activos fijos |
| **Vista mantenimiento** | AS02 (Modificación de activos), AS03 (Visualización de activos) |