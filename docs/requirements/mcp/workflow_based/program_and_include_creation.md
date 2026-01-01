Como desarrollador de SAP, quiero agregar una tool para crear programas y includes en SAP, para que pueda automatizar el proceso de creación de programas y includes en SAP.

Criterios de aceptación:

- Primero planeamos y luego implementamos.
- Se debe crear una tool para crear programas y otra para los include.
- Evaluar si existe un service existente para agregar estas dos nuevas funcionalidades.
- Crear un unico test manual para ambos escenarios.
- La tool debe solicitar: nombre objeto, paquete y OT.
- Considerar que tambien se pueden crear programas y includes en el paquete temporal.
- Estos son los endpoint que se llaman desde eclipse ADT para crear un programa:
Request:
POST /sap/bc/adt/programs/validation?objname=ZHCM03&packagename=ZHCM&description=ZHCM03&objtype=PROG%2FP HTTP/1.1
Header Key: Header Value
================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.programs.validation
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Response:
<?xml version="1.0" encoding="UTF-8"?><asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <CHECK_RESULT>X</CHECK_RESULT>
    </DATA>
  </asx:values>
</asx:abap>

Request:
GET /sap/bc/adt/sscr/registration/objects?uri=%2Fsap%2Fbc%2Fadt%2Fprograms%2Fprograms%2Fzhcm03 HTTP/1.1
Header Key: Header Value
================================================================================================
Accept    : application/vnd.sap.adt.registration+xml
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Response:
<?xml version="1.0" encoding="UTF-8"?><reg:objectRegistrationResponse xmlns:reg="http://www.sap.com/adt/registration" reg:release="750" reg:installationNumber="0020141828">
  <reg:object reg:isRequired="false" reg:accessKey="" reg:transportPGMID="R3TR" reg:transportType="PROG" reg:transportName="ZHCM03">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="https://support.sap.com/sscr" rel="http://www.sap.com/adt/relations/sscr/registration" type="text/html" title="Object Registration in SAP Support Portal is required"/>
  </reg:object>
  <reg:developer reg:isRequired="false" reg:name="L_ABAPS_ITA" reg:accessKey="33909874732164833806">
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" href="https://support.sap.com/sscr" rel="http://www.sap.com/adt/relations/sscr/registration" type="text/html" title="User Registration in SAP Support Portal is required"/>
  </reg:developer>
</reg:objectRegistrationResponse>

Request:
POST /sap/bc/adt/programs/programs?corrNr=CADK911892 HTTP/1.1

Body:
<?xml version="1.0" encoding="UTF-8"?><program:abapProgram xmlns:program="http://www.sap.com/adt/programs/programs" xmlns:adtcore="http://www.sap.com/adt/core" adtcore:description="ZHCM03" adtcore:language="ES" adtcore:name="ZHCM03" adtcore:type="PROG/P" adtcore:masterLanguage="ES" adtcore:masterSystem="CAD" adtcore:responsible="L_ABAPS_ITA">
    
  <adtcore:packageRef adtcore:name="ZHCM"/>
  
</program:abapProgram>

- Estos son los endpoint para cerar include:
Request:
POST /sap/bc/adt/includes/validation?objname=ZHCM03_1P&packagename=ZHCM&description=Test&objtype=PROG%2FI HTTP/1.1

Header Key: Header Value
================================================================================================
Accept    : application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.programs.validation
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Request:
POST /sap/bc/adt/programs/includes?corrNr=CADK911892 HTTP/1.1

Header Key  : Header Value
==================================================================================================
Content-Type: application/vnd.sap.adt.programs.includes.v2+xml
User-Agent  : Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Body:
<?xml version="1.0" encoding="UTF-8"?><include:abapInclude xmlns:include="http://www.sap.com/adt/programs/includes" xmlns:adtcore="http://www.sap.com/adt/core" adtcore:description="Test" adtcore:language="ES" adtcore:name="ZHCM03_1P" adtcore:type="PROG/I" adtcore:masterLanguage="ES" adtcore:masterSystem="CAD" adtcore:responsible="L_ABAPS_ITA">
    
  <adtcore:packageRef adtcore:name="ZHCM"/>
  
</include:abapInclude>