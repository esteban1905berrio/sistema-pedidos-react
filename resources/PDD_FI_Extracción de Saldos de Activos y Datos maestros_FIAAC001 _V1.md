

**Extracción de Saldos de Activos y Datos maestros**

\<FIAAC001\>

1. # **REQUERIMIENTOS DEL NEGOCIO**

Se requiere realizar un programa Z para la extracción de activos fijos almacenados en SAP, capitalizado con su valoración homologando los campos de acuerdo a los catálogos definidos en la plantilla de carga de excel. 

El programa deberá extraer tanto el maestro de activos fijos como los saldos en el formato plantilla de Excel y homologando los datos principales de activos como centros de costo, clases de activos, criterios de clasificación, clave de depreciación. El sistema deberá extraer los saldos en periodo cerrado como interanuales de los bienes muebles e inmuebles, **por el área de valoración 01\.**

No se encuentra en el alcance la extracción automática de información del sistema ALMERA. 

Se partirá con la extracción de activos contables gestionados actualmente en SAP y a las plantillas de carga se adicionarán de manera manual los datos maestros y saldos de activos provenientes de ALMERA, sin embargo, la gobernación debe garantizar que estos saldos coincidan con los saldos contables de balance detallando de forma individual el valor de cada activo de acuerdo a la estructura de la plantilla de carga. 

no se incluirán activos fijos en curso. 

Se identifican los siguientes escenarios que deben conciliarse para garantizar que la extracción y carga de activos se encuentre conciliada de manera manual en la preparación de la plantilla posterior a la extracción y antes de la carga.

Extraer saldos y datos maestros de SAP y comparar con la data extraída de ALMERA (Manual) Determinando:

1. Qué activos están en SAP y no lo están en ALMERA  
2. Qué activos están en ALMERA y deben capitalizarse en SAP (Valor 1 peso por control o totalmente depreciados) garantizando el saldo contable por clase de activo y cuenta.   
3. Los que están en SAP y no están en ALMERA y deben darse de baja en SAP actualmente o posterior a la salida en vivo.   
   

Una vez realizada la depuración y conciliación contable de datos, se podrá realizar la carga mediante el programa de carga de activos fijos. 

2. # **DETALLES DE LA PANTALLA DE SELECCIÓN**

La transacción o programa Z contará con una pantalla de selección que permitirá al usuario definir los parámetros necesarios para la ejecución del proceso de extracción de saldos de Activos Fijos y maestro de Activos.

| Nombre | Parámetro/Opción de selección | Comentarios (Rangos, Selecciones múltiples/sencillas, obligatorio, etc)  | Valor por defecto |
| :---: | ----- | ----- | :---: |
| Sociedad | Tipo parámetro | Se selecciona para ejecutar la extracción de Datos Generales. | N/A |
| Fecha de corte | Tipo parámetro | La fecha de corte en la cual se deben extraer los datos de acuerdo a la fecha de contabilización. | N/A |
| Clase de activo | Select Option | Esta opción debe permitir la selección de uno o varios activos fijos o traer todos los activos, números y subnúmeros de la sociedad. | N/A |
| Área de Valoración | Tipo parámetro | Parámetro Obligatorio | 01 |
| Botón para tabla parámetros | Botón  | Permite el acceso a la tabla Z de parámetros donde se almacenan las constantes y las homologaciones. | N/A |
| Botón para tabla de homologación de cuentas | Botón | Permite el acceso a la tabla z donde se homologan las cuentas contables del plan de cuentas actual versus el plan de cuentas anterior.  | N/A |
| Archivo de Extracción | Tipo parámetro | Se debe seleccionar la ruta donde se va a descargar el archivo de extracción de acuerdo con la opción de procesamiento en formato Excel | N/A |

Tabla 2\. Pantalla de selección

Se requiere crear una pantalla con las opciones de procesamiento por sociedad a una fecha de corte y con acceso a las tablas Z de parametrización que controlan la ejecución del programa. 

**CREAR TABLA DE ZPARMETROS (BOTON PARAMETROS)**

|   Z TIPO P  | ZCAMPO SAP   | ZVALOR ORIGEN | ZA VALOR DESTINO  |
| :---- | :---- | :---- | :---- |
| K | BURKS |  200 | EC05 |

Nota: la información para el campo ZCAMPO SAP podrá tener entradas múltiples a la tabla. 

La tabla Z a crear se utilizará para que el programa pueda homologar los datos que deberán generarse en el archivo de salida.

* **Z TIPO P:** Indica tipo de procesamiento del parámetro de entrada  
* **ZCAMPO SAP**:  Campo SAP obtenido de las tablas que requiere ser homologado.   
* **ZVALOR ORIGEN:** Valor extraído del origen de la tabla SAP.  
* **ZVALOR DESTINO:** Valor el cual tendrá el dato homologado.    
  **EXPORT:** la extracción del documento debe ser formato Excel. 

3. # **CONDICIONES DE INICIO Y PREREQUISITOS** 

El proceso de extracción se ejecutará mediante el programa Z, el cual simulará las transacciones estándar AR01, AS01 y tablas ANLA, ANLC, ANLB ANLZ de activos fijos, subnúmeros y sus valoraciones por clase de activo y sociedad. 

Se podrá extraer en periodo cerrado, es decir, con corte al 31 de Diciembre, y el programa también permitirá la carga interanual

• Extracción en periodo cerrado: se realizará con corte al 31 de diciembre, incluyendo valores históricos de capitalización y depreciación acumulada.

• Extracción interanual: incluirá, además de los valores anteriores, las altas, valorizaciones especiales y sus depreciaciones por cada área de valoración ocurridas entre enero y la fecha de carga con valores acumulados.

Detalle de los campos de valoración que se deben extraer para cada escenario: 

En la extracción, los siguientes campos serán homologados automáticamente por el programa Z y no deberán contener caracteres especiales:

- Centros de costo  
- Clases de activos  
- Criterios de clasificación  
- Clave de depreciación. 

no se incluirán activos fijos en curso. 

**Selección de Activos:** 

Se debe realizar la extracción de datos maestros por sociedad para los activos capitalizados y que no estén dados de baja y obtener la información de depreciación (métodos de depreciación y vida útil) del área de valoración 01\.

**Valoración de activos**:

De acuerdo con los datos maestros de opción selección de datos determinados en la opción se debe obtener la valoración de activos clasificada en:

Valores acumulados: Son los valores de activos de costo y depreciación  por cada área de valoración para activos capitalizados en ejercicios anteriores(antes del 2024).

**Movimientos:**

Son los valores de costo y depreciación para activos nuevos (capitalizados para el ejercicio 2025\) y depreciaciones contabilizadas para activos antiguos durante el ejercicio, estos valores se deben extraer por área de valoración.

El archivo de Excel de salida debe contener los campos relacionados en el Excel adjunto, respetando el orden.


Datos Maestro:

 

Valorización del activo:

**Generalidades del mapeo:**

Cuando se ejecuta el programa Z para la extracción de datos maestros y saldos de activos se deben realizar las siguientes búsquedas:

**Selección de Activos:**

Selección de datos maestros de activos a extraer : Consultar la tabla ANLA  y ANLZ con sociedad BURKS para activos ANLA-ANLN1 y subnumero ANLA-ANLN2 capitalizados el AKTIV \= diferentes de vacío y que no estén dados de baja DEAKT \= vacío , aplicar la misma búsqueda para obtener el centro de costo en ANLZ.

*Ver detalle de campos a diligencias en la plantilla de activos.*

**Valoración de activos:**

Selección de datos de depreciación para los datos maestros por área de valoración ; con los datos obtenidos en la selección de datos maestros consultar ANLB con sociedad BURKS para activos ANLA-ANLN1 y subnumero ANLA-ANLN2y obtener la información de depreciación para las áreas de valoración definidas en la tabla ZPARAMETROS.  

Ver detalle de campos a diligenciar en la plantilla de activos.

**Movimientos:**

A partir de estos datos obtener la valoración de activos por cada número de activo ANLA-ANLN1 y subnumero ANLA-ANLN2 por cada área de valoración consultado ANLC con sociedad BURKS para las áreas de valoración ANLC – AFABE \= definidas  en la tabla  ZFI\_PARAMETROS  para las áreas de valoración a extraer (01)

Ver detalle de campos a diligencias en la plantilla de activos

Se podrá usar la misma tabla de parámetros creada para la extracción de datos maestros de activos. E identificar el tipo de procesamiento activo. 

4. # **SUPUESTOS**

Los siguientes supuestos se consideran válidos para la correcta ejecución del proceso de extracción de activos en SAP:

* La extracción no debe traer datos duplicados  
* No se incluirán activos en curso   
* La información se consulta directamente en las tablas ANLA, ANLB, ANLC, ANLZ, ANEP, ANEK.  
* Los saldos contables deben coincidir con los valores por clase de actiuvo y cuenta contable.   
* Los campos que no puedan homologarse porque no se encuentren en SAP o estén desactualizados, deberán ser actualizados directamente en la plantilla de manera manual previa a la carga.

5. # **REGLAS DE NEGOCIO**

Las siguientes reglas aplican para garantizar consistencia, control y trazabilidad durante el proceso de extracción: 

* Solo se extraen activos capitalizados y vigentes, que no estén dados de baja.  
* La extracción incluirá tanto datos maestros (identificación, clase, centro de costo, ubicación) como valores contables (costo histórico, revalorizaciones, depreciaciones acumuladas).  
* Para cargas interanuales se deben calcular los valores acumulados de adquisición y depreciación hasta la fecha de corte.  
* Los valores se expresan en moneda local (COP).  
* El archivo de salida debe seguir la estructura de la plantilla estándar definida en el proyecto en excel.

6. #  **CRITERIOS DE ACEPTACIÓN** 

* El archivo de excel extraído debe cumplir con la estructura de la plantilla de carga.   
* Los datos extraídos coinciden con el reporte estándar de SAP AR01  
* El programa permite seleccionar y extraer correctamente los datos según los filtros definidos.  
* Se garantiza que no se extraen activos duplicados o con datos incompletos.  
* El proceso finaliza sin dumps ni bloqueos.

