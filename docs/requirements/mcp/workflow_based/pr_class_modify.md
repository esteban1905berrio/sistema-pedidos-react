
 Vamos ajustar el tool modify_class, te dejo el orden de los llamados que funcionan: 

 Llmado 1:

 POST /sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij?_action=LOCK&accessMode=MODIFY HTTP/1.1
 Header Key: Header Value
=============================================================================================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result;q=0.8, application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.lock.result2;q=0.9

Response:
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <LOCK_HANDLE>8BB0F835AA8C80F143BD7A5A04EB908AE456996E</LOCK_HANDLE>
      <CORRNR>CADK911122</CORRNR>
      <CORRUSER>L_ABAPS_ITA</CORRUSER>
      <CORRTEXT>FI WB AAC002 Carga Saldos de activo y Datos Maestros V001SL</CORRTEXT>
      <IS_LOCAL/>
      <IS_LINK_UP/>
      <MODIFICATION_SUPPORT>NoModification</MODIFICATION_SUPPORT>
      <LINK_UP_MODE/>
      <CORR_LOCKS/>
      <CORR_CONTENTS/>
    </DATA>
  </asx:values>
</asx:abap>

Llamdo 2:

GET /sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/objectstructure?version=active&withShortDescriptions=true HTTP/1.1

Header Key: Header Value
====================================================================================================================================
Accept    : application/xml;q=0.8, application/vnd.sap.adt.objectstructure+xml;q=0.9, application/vnd.sap.adt.objectstructure.v2+xml

Response:

<?xml version="1.0" encoding="UTF-8"?><abapsource:objectStructureElement xmlns:abapsource="http://www.sap.com/adt/abapsource" xml:base="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/objectstructure?version=active&amp;withShortDescriptions=true" adtcore:name="ZCLFIAAC002_CARGA_ACTIVOS_FIJ" visibility="public" final="true" adtcore:type="CLAS/OC" xmlns:adtcore="http://www.sap.com/adt/core" xmlns:atom="http://www.w3.org/2005/Atom">
  <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=1,6;end=1,35"/>
  <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=314,6;end=314,35"/>
  <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=1,0;end=310,8"/>
  <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=314,0;end=882,8"/>
  <abapsource:objectStructureElement adtcore:type="CLAS/OA" constant="true" adtcore:name="GC_CLS_MSG" level="static" readOnly="true" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=8,15;end=8,25"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=8,4;end=8,61"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OA" constant="true" adtcore:name="GC_CLS_MSG_CX" level="static" readOnly="true" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=9,15;end=9,28"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=8,4;end=9,61"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OA" constant="true" adtcore:name="GC_CLS_MOV_ANULACION" level="static" readOnly="true" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=10,15;end=10,35"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=8,4;end=10,80"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OA" constant="true" adtcore:name="GC_MOV_FI" level="static" readOnly="true" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=11,15;end=11,24"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=8,4;end=11,59"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OA" constant="true" adtcore:name="GC_MOV_AF" level="static" readOnly="true" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=12,15;end=12,24"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=8,4;end=12,59"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_TI_BAPI1022_DEP_AREAS" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=14,10;end=14,34"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=14,4;end=14,91"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_TI_BAPI1022_DEP_AREASX" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=15,10;end=15,35"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=15,4;end=15,93"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_TI_BAPI1022_CUMVAL" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=16,10;end=16,31"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=16,4;end=16,85"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_TI_BAPI1022_POSTVAL" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=17,10;end=17,32"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=17,4;end=17,87"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_ES_LOG" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=19,20;end=19,29"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=19,4;end=22,34"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_TI_LOG" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=24,11;end=24,20"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=24,69"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_R_ANLN1" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=25,11;end=25,21"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=25,46"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_R_ANLN2" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=26,11;end=26,21"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=26,46"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_R_FLNAM" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=27,11;end=27,21"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=27,55"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_R_FECHA" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=28,11;end=28,21"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=28,54"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_R_HORA" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=29,11;end=29,20"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=29,53"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_R_TIPO" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=30,11;end=30,20"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=24,4;end=30,53"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_ES_MOV_AF" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=32,20;end=32,32"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=32,4;end=44,30"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OT" adtcore:name="TP_TI_MOV_AF" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=46,11;end=46,23"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=32,4;end=46,97"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="CREAR_MODIFICAR_ACTIVO_FIJO" level="instance" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=53,6;end=53,33" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=411,9;end=411,36"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=48,4;end=58,57"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=411,2;end=617,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="PRESENTAR_LOG_CREA_ACTIVOFIJO" level="static" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=66,6;end=66,35" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=869,9;end=869,38"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=59,4;end=73,45"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=869,2;end=881,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MOSTRAR_RESULTADOS_LOG" level="static" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=74,6;end=74,28" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=708,9;end=708,31"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=59,4;end=84,45"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=708,2;end=740,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="CONSULTAR_TI_MOVIMIENTOS_AF" level="instance" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=87,6;end=87,33" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=397,9;end=397,36"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=86,4;end=94,46"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=397,2;end=408,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="ANULAR_MOVIMIENTOS" level="instance" visibility="public">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=99,6;end=99,24" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=317,9;end=317,27"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=86,4;end=103,52"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=317,2;end=379,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="EJECUTAR_BAPI_CREAR" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=134,6;end=134,25" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=620,9;end=620,28"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=107,4;end=161,53"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=620,2;end=651,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="EJECUTAR_BAPI_MODIFICAR" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=190,6;end=190,29" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=654,9;end=654,32"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=163,4;end=217,53"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=654,2;end=705,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="CONSULTAR_MOVIMIENTOS_AF" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=225,6;end=225,30" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=382,9;end=382,33"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=219,4;end=231,46"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=382,2;end=394,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_KEY" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=234,6;end=234,12" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=809,9;end=809,15"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=233,4;end=237,51"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=809,2;end=814,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_GENERALDATA" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=240,6;end=240,20" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=787,9;end=787,23"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=239,4;end=245,51"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=787,2;end=795,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_INVENTORY" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=248,6;end=248,18" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=798,9;end=798,21"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=247,4;end=253,49"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=798,2;end=806,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_POSTINGINFORMATION" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=256,6;end=256,27" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=844,9;end=844,30"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=255,4;end=261,58"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=844,2;end=852,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_TIMEDEPENDENTDATA" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=264,6;end=264,26" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=855,9;end=855,29"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=263,4;end=269,57"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=855,2;end=866,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_ALLOCATIONS" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=272,6;end=272,20" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=743,9;end=743,23"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=271,4;end=277,51"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=743,2;end=751,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_ORIGIN" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=280,6;end=280,15" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=817,9;end=817,18"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=279,4;end=285,49"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=817,2;end=830,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_DEPRECIATIONAREAS" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=288,6;end=288,26" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=762,9;end=762,29"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=287,4;end=293,64"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=762,2;end=784,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_CUMULATEDVALUES" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=296,6;end=296,24" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=754,9;end=754,27"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=295,4;end=300,57"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=754,2;end=759,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OM" adtcore:name="MP_POSTEDVALUES" level="instance" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=303,6;end=303,21" type="CLAS/OM"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationIdentifier" href="./source/main#start=833,9;end=833,24"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=302,4;end=307,55"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/implementationBlock" href="./source/main#start=833,2;end=841,11"/>
  </abapsource:objectStructureElement>
  <abapsource:objectStructureElement adtcore:type="CLAS/OA" adtcore:name="O_LOG" level="static" visibility="private">
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionIdentifier" href="./source/main#start=309,16;end=309,21"/>
    <atom:link rel="http://www.sap.com/adt/relations/source/definitionBlock" href="./source/main#start=309,4;end=309,54"/>
  </abapsource:objectStructureElement>
</abapsource:objectStructureElement>

Llamdo 3:

PUT /sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/source/main?lockHandle=8BB0F835AA8C80F143BD7A5A04EB908AE456996E&corrNr=CADK911122 HTTP/1.1
Header Key: Header Value
========================
Accept    : text/plain
Content-Type: text/plain; charset=utf-8

Body: Codigo modificado

Llamdo 4:

GET /sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij?version=inactive HTTP/1.1
Header Key: Header Value
===================================================================================================================================================================================
Accept    : application/vnd.sap.adt.oo.classes.v4+xml, application/vnd.sap.adt.oo.classes.v3+xml, application/vnd.sap.adt.oo.classes.v2+xml, application/vnd.sap.adt.oo.classes+xml
If-None-Match: 20251106210559001000184

Response:
<?xml version="1.0" encoding="UTF-8"?><class:abapClass xmlns:class="http://www.sap.com/adt/oo/classes" class:final="true" class:abstract="false" class:visibility="public" class:category="generalObjectType" class:sharedMemoryEnabled="false" abapoo:modeled="false" abapsource:fixPointArithmetic="true" abapsource:activeUnicodeCheck="true" adtcore:responsible="L_ABAPS_ITA" adtcore:masterLanguage="ES" adtcore:masterSystem="CAD" adtcore:name="ZCLFIAAC002_CARGA_ACTIVOS_FIJ" adtcore:type="CLAS/OC" adtcore:changedAt="2025-11-10T22:42:03Z" adtcore:version="inactive" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:createdBy="L_ABAPS_ITA" adtcore:description="carga activos fijos" adtcore:descriptionTextLimit="60" adtcore:language="ES" xmlns:abapoo="http://www.sap.com/adt/oo" xmlns:abapsource="http://www.sap.com/adt/abapsource" xmlns:adtcore="http://www.sap.com/adt/core">
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="objectstructure" rel="http://www.sap.com/adt/relations/objectstructure"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/vit/wb/object_type/clasocx/object_name/ZCLFIAAC002_CARGA_ACTIVOS_FIJ" rel="http://www.sap.com/adt/relations/sources/textelements" type="application/vnd.sap.sapgui" title="Text elements"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/source/main?withAbapDocFromShortTexts=true" rel="http://www.sap.com/adt/relations/sources/withabapdocfromshorttexts" type="text/plain" title="Source with ABAP Doc"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/transports" rel="http://www.sap.com/adt/relations/transport" type="application/vnd.sap.as+xml;charset=utf-8;dataname=com.sap.adt.lock.result2" title="Related Transport Requests"/>
  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/classifications?uri=%2fsap%2fbc%2fadt%2foo%2fclasses%2fzclfiaac002_carga_activos_fij" rel="http://www.sap.com/adt/categories/classifications" type="application/vnd.sap.adt.classifications+xml" title="Classifications"/>
  <adtcore:packageRef adtcore:uri="/sap/bc/adt/vit/wb/object_type/devck/object_name/ZFI" adtcore:type="DEVC/K" adtcore:name="ZFI"/>
  <abapsource:syntaxConfiguration>
    <abapsource:language>
      <abapsource:version>X</abapsource:version>
      <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/abapsource/parsers/rnd/grammar" rel="http://www.sap.com/adt/relations/abapsource/parser" type="text/plain" etag="750"/>
    </abapsource:language>
  </abapsource:syntaxConfiguration>
  <class:include class:includeType="definitions" abapsource:sourceUri="includes/definitions" adtcore:name="" adtcore:type="CLAS/I" adtcore:changedAt="2025-10-26T20:47:10Z" adtcore:version="active" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:createdBy="L_ABAPS_ITA">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/definitions/versions" rel="http://www.sap.com/adt/relations/versions"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/definitions" rel="http://www.sap.com/adt/relations/source" type="text/plain" etag="202510262047100011"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/definitions" rel="http://www.sap.com/adt/relations/source" type="text/html" etag="202510262047100011"/>
  </class:include>
  <class:include class:includeType="implementations" abapsource:sourceUri="includes/implementations" adtcore:name="" adtcore:type="CLAS/I" adtcore:changedAt="2025-10-26T20:47:10Z" adtcore:version="active" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:createdBy="L_ABAPS_ITA">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/implementations/versions" rel="http://www.sap.com/adt/relations/versions"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/implementations" rel="http://www.sap.com/adt/relations/source" type="text/plain" etag="202510262047100011"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/implementations" rel="http://www.sap.com/adt/relations/source" type="text/html" etag="202510262047100011"/>
  </class:include>
  <class:include class:includeType="macros" abapsource:sourceUri="includes/macros" adtcore:name="" adtcore:type="CLAS/I" adtcore:changedAt="2025-10-26T20:47:10Z" adtcore:version="active" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:createdBy="L_ABAPS_ITA">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/macros/versions" rel="http://www.sap.com/adt/relations/versions"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/macros" rel="http://www.sap.com/adt/relations/source" type="text/plain" etag="202510262047100011"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/macros" rel="http://www.sap.com/adt/relations/source" type="text/html" etag="202510262047100011"/>
  </class:include>
  <class:include class:includeType="testclasses" abapsource:sourceUri="includes/testclasses" adtcore:name="" adtcore:type="CLAS/I" adtcore:changedAt="2025-10-26T20:47:10Z" adtcore:version="active" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:createdBy="L_ABAPS_ITA">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/testclasses/versions" rel="http://www.sap.com/adt/relations/versions"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/testclasses" rel="http://www.sap.com/adt/relations/source" type="text/plain" etag="202510262047100011"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/testclasses" rel="http://www.sap.com/adt/relations/source" type="text/html" etag="202510262047100011"/>
  </class:include>
  <class:include class:includeType="main" abapsource:sourceUri="source/main" adtcore:name="" adtcore:type="CLAS/I" adtcore:changedAt="2025-11-10T22:42:03Z" adtcore:version="inactive" adtcore:createdAt="2025-10-23T00:00:00Z" adtcore:changedBy="L_ABAPS_ITA" adtcore:createdBy="L_ABAPS_ITA">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="includes/main/versions" rel="http://www.sap.com/adt/relations/versions"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="source/main" rel="http://www.sap.com/adt/relations/source" type="text/plain" etag="20251110224203000000181"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="source/main" rel="http://www.sap.com/adt/relations/source" type="text/html" etag="20251110224203000000181"/>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/source/main?version=active" rel="http://www.sap.com/adt/relations/objectstates" title="Reference to active or inactive version"/>
  </class:include>
</class:abapClass>

Llamdo 5:

POST /sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij?_action=UNLOCK&lockHandle=8BB0F835AA8C80F143BD7A5A04EB908AE456996E HTTP/1.1

Llamdo 6:

POST /sap/bc/adt/activation?method=activate&preauditRequested=true HTTP/1.1
Header Key: Header Value
===========================
Accept    : application/xml
Content-Type: application/xml

Body:

<?xml version="1.0" encoding="UTF-8"?><adtcore:objectReferences xmlns:adtcore="http://www.sap.com/adt/core">
  <adtcore:objectReference adtcore:uri="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij" adtcore:name="ZCLFIAAC002_CARGA_ACTIVOS_FIJ"/>
</adtcore:objectReferences>

Llamado 7:

POST /sap/bc/adt/activation?method=activate&preauditRequested=false HTTP/1.1

Header Key: Header Value
===========================
Accept    : application/xml
Content-Type: application/xml

Body:

<?xml version="1.0" encoding="UTF-8"?><adtcore:objectReferences xmlns:adtcore="http://www.sap.com/adt/core">
  <adtcore:objectReference adtcore:uri="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij" adtcore:type="CLAS/OC" adtcore:name="ZCLFIAAC002_CARGA_ACTIVOS_FIJ" adtcore:packageName="ZFI"/>
  <adtcore:objectReference adtcore:uri="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij/source/main#type=CLAS%2FOM;name=EJECUTAR_BAPI_CREAR" adtcore:type="CLAS/OM/private" adtcore:name="ZCLFIAAC002_CARGA_ACTIVOS_FIJ EJECUTAR_BAPI_CREAR" adtcore:parentUri="/sap/bc/adt/oo/classes/zclfiaac002_carga_activos_fij"/>
</adtcore:objectReferences>
