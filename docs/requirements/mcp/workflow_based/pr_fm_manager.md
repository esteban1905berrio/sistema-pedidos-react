vamos a trabajar en create_function_module, el siguiente es el flujo de llamdos que se deben hacer para crear un FM conociendo su grupo de funciono. Valida como esta la implementacion actual y que falta con relacion al flujo que retorna el eclipse

Lllamod 1:

POST /sap/bc/adt/functions/validation?objtype=FUGR%2FFF&objname=ZFI_DMEE_BANCOLOMBIA_R1&fugrname=ZFIDMEE_1&description=R1+Bancolombia HTTP/1.1
Header Key: Header Value
=======================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.StatusMessage

Response:

<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <SEVERITY>INFO</SEVERITY>
      <SHORT_TEXT>El nombre del módulo de funciones está dentro del área de nombres SAP</SHORT_TEXT>
      <LONG_TEXT/>
    </DATA>
  </asx:values>
</asx:abap>

Llamado 2:

POST /sap/bc/adt/cts/transportchecks HTTP/1.1

Header Key: Header Value
=====================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.transport.service.checkData
Content-Type: application/vnd.sap.as+xml; charset=UTF-8; dataname=com.sap.adt.transport.service.checkData

Body: <?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID/>
      <OBJECT/>
      <OBJECTNAME/>
      <DEVCLASS>ZFI</DEVCLASS>
      <SUPER_PACKAGE/>
      <OPERATION>I</OPERATION>
      <URI>/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1</URI>
    </DATA>
  </asx:values>
</asx:abap>

Response:

<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID>LIMU</PGMID>
      <OBJECT>FUNC</OBJECT>
      <OBJECTNAME>ZFI_DMEE_BANCOLOMBIA_R1</OBJECTNAME>
      <OPERATION>I</OPERATION>
      <DEVCLASS>ZFI</DEVCLASS>
      <CTEXT>Desarrollos FI</CTEXT>
      <KORRFLAG>X</KORRFLAG>
      <AS4USER>IGONZALEZ</AS4USER>
      <PDEVCLASS>ZCAD</PDEVCLASS>
      <DLVUNIT>HOME</DLVUNIT>
      <NAMESPACE>/0CUST/</NAMESPACE>
      <RESULT>S</RESULT>
      <RECORDING/>
      <EXISTING_REQ_ONLY/>
      <MESSAGES/>
      <REQUESTS/>
      <LOCKS>
        <CTS_OBJECT_LOCK>
          <OBJECT_KEY>
            <PGMID>LIMU</PGMID>
            <OBJECT>REPS</OBJECT>
            <OBJ_NAME>LZFIDMEE_1UXX</OBJ_NAME>
          </OBJECT_KEY>
          <LOCK_HOLDER>
            <REQ_HEADER>
              <TRKORR>CADK910827</TRKORR>
              <TRFUNCTION>K</TRFUNCTION>
              <TRSTATUS>D</TRSTATUS>
              <TARSYSTEM>/QASALL/</TARSYSTEM>
              <AS4USER>L_ABAPS_ITA</AS4USER>
              <AS4DATE>2025-10-30</AS4DATE>
              <AS4TIME>09:48:32</AS4TIME>
              <AS4TEXT>FI WB TRF005 Medios de pago Banco de Occidente V001SL</AS4TEXT>
              <CLIENT>200</CLIENT>
            </REQ_HEADER>
            <REQ_ATTRS/>
            <TASK_HEADERS>
              <CTS_TASK_HEADER>
                <TRKORR>CADK911140</TRKORR>
                <TRFUNCTION>S</TRFUNCTION>
                <TRSTATUS>D</TRSTATUS>
                <AS4USER>L_ABAPS_ITA</AS4USER>
                <AS4DATE>2025-10-27</AS4DATE>
                <AS4TIME>16:22:37</AS4TIME>
                <AS4TEXT>FI_TR_W_Medios de pago occidente</AS4TEXT>
              </CTS_TASK_HEADER>
            </TASK_HEADERS>
          </LOCK_HOLDER>
        </CTS_OBJECT_LOCK>
      </LOCKS>
      <TADIRDEVC>ZFI</TADIRDEVC>
      <URI>/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1</URI>
      <CTS_PROJECTS/>
    </DATA>
  </asx:values>
</asx:abap>


Llamado 3:

GET /sap/bc/adt/sscr/registration/objects?uri=%2Fsap%2Fbc%2Fadt%2Ffunctions%2Fgroups%2Fzfidmee_1%2Ffmodules%2Fzfi_dmee_bancolombia_r1 HTTP/1.1
Header Key: Header Value
====================================================
Accept    : application/vnd.sap.adt.registration+xml

Response:
<?xml version="1.0" encoding="UTF-8"?><reg:objectRegistrationResponse xmlns:reg="http://www.sap.com/adt/registration" reg:release="750" reg:installationNumber="0020141828">
  <reg:object reg:isRequired="false" reg:accessKey="" reg:transportPGMID="R3TR" reg:transportType="FUGR" reg:transportName="ZFIDMEE_1">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="https://support.sap.com/sscr" rel="http://www.sap.com/adt/relations/sscr/registration" type="text/html" title="Object Registration in SAP Support Portal is required"/>
  </reg:object>
  <reg:developer reg:isRequired="false" reg:name="L_ABAPS_ITA" reg:accessKey="33909874732164833806">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="https://support.sap.com/sscr" rel="http://www.sap.com/adt/relations/sscr/registration" type="text/html" title="User Registration in SAP Support Portal is required"/>
  </reg:developer>
</reg:objectRegistrationResponse>

Llamdo 4:

POST /sap/bc/adt/cts/transportchecks HTTP/1.1
Header Key: Header Value
=====================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.transport.service.checkData
Content-Type: application/vnd.sap.as+xml; charset=UTF-8; dataname=com.sap.adt.transport.service.checkData

Body 
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID/>
      <OBJECT/>
      <OBJECTNAME/>
      <DEVCLASS>ZFI</DEVCLASS>
      <SUPER_PACKAGE/>
      <OPERATION>I</OPERATION>
      <URI>/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1</URI>
    </DATA>
  </asx:values>
</asx:abap>

Response:

<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID>LIMU</PGMID>
      <OBJECT>FUNC</OBJECT>
      <OBJECTNAME>ZFI_DMEE_BANCOLOMBIA_R1</OBJECTNAME>
      <OPERATION>I</OPERATION>
      <DEVCLASS>ZFI</DEVCLASS>
      <CTEXT>Desarrollos FI</CTEXT>
      <KORRFLAG>X</KORRFLAG>
      <AS4USER>IGONZALEZ</AS4USER>
      <PDEVCLASS>ZCAD</PDEVCLASS>
      <DLVUNIT>HOME</DLVUNIT>
      <NAMESPACE>/0CUST/</NAMESPACE>
      <RESULT>S</RESULT>
      <RECORDING/>
      <EXISTING_REQ_ONLY/>
      <MESSAGES/>
      <REQUESTS/>
      <LOCKS>
        <CTS_OBJECT_LOCK>
          <OBJECT_KEY>
            <PGMID>LIMU</PGMID>
            <OBJECT>REPS</OBJECT>
            <OBJ_NAME>LZFIDMEE_1UXX</OBJ_NAME>
          </OBJECT_KEY>
          <LOCK_HOLDER>
            <REQ_HEADER>
              <TRKORR>CADK910827</TRKORR>
              <TRFUNCTION>K</TRFUNCTION>
              <TRSTATUS>D</TRSTATUS>
              <TARSYSTEM>/QASALL/</TARSYSTEM>
              <AS4USER>L_ABAPS_ITA</AS4USER>
              <AS4DATE>2025-10-30</AS4DATE>
              <AS4TIME>09:48:32</AS4TIME>
              <AS4TEXT>FI WB TRF005 Medios de pago Banco de Occidente V001SL</AS4TEXT>
              <CLIENT>200</CLIENT>
            </REQ_HEADER>
            <REQ_ATTRS/>
            <TASK_HEADERS>
              <CTS_TASK_HEADER>
                <TRKORR>CADK911140</TRKORR>
                <TRFUNCTION>S</TRFUNCTION>
                <TRSTATUS>D</TRSTATUS>
                <AS4USER>L_ABAPS_ITA</AS4USER>
                <AS4DATE>2025-10-27</AS4DATE>
                <AS4TIME>16:22:37</AS4TIME>
                <AS4TEXT>FI_TR_W_Medios de pago occidente</AS4TEXT>
              </CTS_TASK_HEADER>
            </TASK_HEADERS>
          </LOCK_HOLDER>
        </CTS_OBJECT_LOCK>
      </LOCKS>
      <TADIRDEVC>ZFI</TADIRDEVC>
      <URI>/sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1</URI>
      <CTS_PROJECTS/>
    </DATA>
  </asx:values>
</asx:abap>

llamado 5:

POST /sap/bc/adt/functions/groups/zfidmee_1/fmodules?corrNr=CADK910827 HTTP/1.1
Header Key  : Header Value
===============================================================
Content-Type: application/vnd.sap.adt.functions.fmodules.v2+xml


Body: <?xml version="1.0" encoding="UTF-8"?><fmodule:abapFunctionModule xmlns:fmodule="http://www.sap.com/adt/functions/fmodules" xmlns:adtcore="http://www.sap.com/adt/core" adtcore:description="R1 Bancolombia" adtcore:name="ZFI_DMEE_BANCOLOMBIA_R1" adtcore:type="FUGR/FF">
    
  <adtcore:containerRef adtcore:name="ZFIDMEE_1" adtcore:type="FUGR/F" adtcore:uri="/sap/bc/adt/functions/groups/zfidmee_1"/>
  
</fmodule:abapFunctionModule>

llamado 6:
GET /sap/bc/adt/functions/groups/zfidmee_1/fmodules/zfi_dmee_bancolombia_r1/source/main HTTP/1.1

Llamado 7:

POST /sap/bc/adt/repository/nodestructure?parent_name=ZFIDMEE_1&parent_tech_name=SAPLZFIDMEE_1&parent_type=FUGR%2FF&withShortDescriptions=true HTTP/1.1
Header Key: Header Value
=====================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.RepositoryObjectTreeContent
Content-Type: application/vnd.sap.as+xml; charset=UTF-8; dataname=null

Body: 
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <TV_NODEKEY>000000</TV_NODEKEY>
    </DATA>
  </asx:values>
</asx:abap>

Response: 
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <TREE_CONTENT/>
      <CATEGORIES>
        <SEU_ADT_OBJECT_CATEGORY_INFO>
          <CATEGORY>source_library</CATEGORY>
          <CATEGORY_LABEL>Source Code Library</CATEGORY_LABEL>
        </SEU_ADT_OBJECT_CATEGORY_INFO>
      </CATEGORIES>
      <OBJECT_TYPES>
        <SEU_ADT_OBJECT_TYPE_INFO>
          <OBJECT_TYPE>FUGR/FF</OBJECT_TYPE>
          <CATEGORY_TAG>source_library</CATEGORY_TAG>
          <OBJECT_TYPE_LABEL>Módulos funciones</OBJECT_TYPE_LABEL>
          <NODE_ID>000002</NODE_ID>
        </SEU_ADT_OBJECT_TYPE_INFO>
        <SEU_ADT_OBJECT_TYPE_INFO>
          <OBJECT_TYPE>FUGR/I</OBJECT_TYPE>
          <CATEGORY_TAG>source_library</CATEGORY_TAG>
          <OBJECT_TYPE_LABEL>Includes</OBJECT_TYPE_LABEL>
          <NODE_ID>000064</NODE_ID>
        </SEU_ADT_OBJECT_TYPE_INFO>
        <SEU_ADT_OBJECT_TYPE_INFO>
          <OBJECT_TYPE>FUGR/PD</OBJECT_TYPE>
          <CATEGORY_TAG>source_library</CATEGORY_TAG>
          <OBJECT_TYPE_LABEL>Campos</OBJECT_TYPE_LABEL>
          <NODE_ID>000053</NODE_ID>
        </SEU_ADT_OBJECT_TYPE_INFO>
        <SEU_ADT_OBJECT_TYPE_INFO>
          <OBJECT_TYPE>FUGR/PU</OBJECT_TYPE>
          <CATEGORY_TAG>source_library</CATEGORY_TAG>
          <OBJECT_TYPE_LABEL>Subrutinas</OBJECT_TYPE_LABEL>
          <NODE_ID>000058</NODE_ID>
        </SEU_ADT_OBJECT_TYPE_INFO>
        <SEU_ADT_OBJECT_TYPE_INFO>
          <OBJECT_TYPE>FUGR/PY</OBJECT_TYPE>
          <CATEGORY_TAG>source_library</CATEGORY_TAG>
          <OBJECT_TYPE_LABEL>Tipos</OBJECT_TYPE_LABEL>
          <NODE_ID>000051</NODE_ID>
        </SEU_ADT_OBJECT_TYPE_INFO>
      </OBJECT_TYPES>
    </DATA>
  </asx:values>
</asx:abap>

