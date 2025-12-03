Tengo el analisis completo para recuperar los Dump:

Primero la recuperacion de los dumps esta correcta.
Para retornar el detalle vamos a tratar de utilizar los siguientes endpoint, con el ID retornado por el llamdo que se hace para retornar el listado de DUMPS. Si no se puede hacer el llamado para retornar el detalle, simplmente utiliza el summary del retorno inicial, si el agente requiere mas detalle utilizar el llamado al RFC ZCX_GET_DUMP_DETAIL. evalua el estado actual de la implmentacion.

Debes hacer los siguiente llamados para retornar el detalle siempre y cuando el sistema SAP lo soporte.

Llamdo 1
GET /sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207 HTTP/1.1

Header Key: Header Value
================================================================================================
Accept    : application/vnd.sap.adt.runtime.dump.v1+xml
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Response:

<?xml version="1.0" encoding="UTF-8"?><dump:dump xmlns:dump="http://www.sap.com/adt/categories/dump" title="Runtime Error: CALL_FUNCTION_NOT_REMOTE 01.12.2025 12:45:03 SEBLONDO (SEBASTIAN LONDONO SANCHEZ)" error="CALL_FUNCTION_NOT_REMOTE" author="SEBLONDO" exception="" terminatedProgram="SAPLZGFCX_1" serverInstance="vhs4dapci_S4D_00" datetime="2025-12-01T17:45:03Z" systemDate="01.12.2025" systemTime="12:45:03">
  <dump:links>
    <dump:link relation="contents" uri="/sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207/formatted" contentType="text/plain"/>
    <dump:link relation="http://www.sap.com/adt/relations/runtime/dump/unformatted" uri="/sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207/unformatted" contentType="text/plain"/>
    <dump:link relation="self" uri="/sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207" contentType="application/vnd.sap.adt.runtime.dump.v1+xml"/>
    <dump:link relation="alternate" uri="/sap/bc/adt/vit/runtime/dumps/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207" contentType="application/vnd.sap.adt.sapgui"/>
    <dump:link relation="http://www.sap.com/adt/relations/runtime/dump/summary" uri="/sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207/summary" contentType="text/html"/>
    <dump:link relation="http://www.sap.com/adt/relations/runtime/dump/termination" uri="adt://S4D/sap/bc/adt/programs/includes/sapmssy1?context=%2fsap%2fbc%2fadt%2ffunctions%2fgroups%2fzgfcx_1/source/main#start=185" contentType=""/>
    <dump:link relation="http://www.sap.com/adt/relations/runtime/dump/http" uri="https://VHSWDAPCI.CRYSTAL.COM.CO:44310/sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207" contentType="text/html"/>
  </dump:links>
  <dump:chapters>
    <dump:chapter name="kap5" title="Entorno sistema" category="System Environment" line="89" chapterOrder="6" categoryOrder="1"/>
    <dump:chapter name="kap6" title="Usuario y transacción" category="System Environment" line="135" chapterOrder="7" categoryOrder="1"/>
    <dump:chapter name="kap6a" title="Server-Side Connection Information" category="System Environment" line="154" chapterOrder="8" categoryOrder="1"/>
    <dump:chapter name="kap1" title="¿Qué ha sucedido?" category="User View" line="15" chapterOrder="2" categoryOrder="2"/>
    <dump:chapter name="kap2" title="¿Qué puede hacer?" category="User View" line="27" chapterOrder="3" categoryOrder="2"/>
    <dump:chapter name="kap0" title="Texto breve" category="ABAP Developer View" line="10" chapterOrder="1" categoryOrder="3"/>
    <dump:chapter name="kap3" title="Anál.errores" category="ABAP Developer View" line="43" chapterOrder="4" categoryOrder="3"/>
    <dump:chapter name="kap4" title="Notas para corregir errores" category="ABAP Developer View" line="59" chapterOrder="5" categoryOrder="3"/>
    <dump:chapter name="kap7" title="Info posición de cancelación" category="ABAP Developer View" line="189" chapterOrder="9" categoryOrder="3"/>
    <dump:chapter name="kap8" title="Detalle código fuente" category="ABAP Developer View" line="198" chapterOrder="10" categoryOrder="3"/>
    <dump:chapter name="kap9" title="Contenido campos sistema" category="ABAP Developer View" line="255" chapterOrder="11" categoryOrder="3"/>
    <dump:chapter name="kap11" title="Llamadas/Eventos activos" category="ABAP Developer View" line="286" chapterOrder="12" categoryOrder="3"/>
    <dump:chapter name="kap10" title="Variables seleccionadas" category="ABAP Developer View" line="298" chapterOrder="13" categoryOrder="3"/>
    <dump:chapter name="kap22" title="Llamadas de aplicación" category="ABAP Developer View" line="375" chapterOrder="14" categoryOrder="3"/>
    <dump:chapter name="kap14" title="Lista de programas ABAP implicados" category="ABAP Developer View" line="420" chapterOrder="17" categoryOrder="3"/>
    <dump:chapter name="kap12" title="Notas internas" category="BASIS Developer View" line="383" chapterOrder="15" categoryOrder="4"/>
    <dump:chapter name="kap13" title="Llamadas activas núcleo SAP" category="BASIS Developer View" line="393" chapterOrder="16" categoryOrder="4"/>
    <dump:chapter name="kap16" title="Directorio de tablas de aplicación" category="BASIS Developer View" line="430" chapterOrder="18" categoryOrder="4"/>
  </dump:chapters>
</dump:dump>

Llamado 2

GET /sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207/summary HTTP/1.1
/sap/bc/adt/runtime/dump/sap/bc/adt/vit/runtime/dumps/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207/summary

Header Key: Header Value
================================================================================================
Accept    : text/html
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Response
<h4 id="OVERVIEW">Contents</h4><a href="#HEADERX">Header Information</a><br><a href="#WHATHAPPENED">What happened?</a><br><a href="#ERROR">Error analysis</a><br><a href="#TERMINATION">Information on where terminated</a><br><a href="#SOURCE">Source Code Extract</a><br><a href="#STACK">Active Calls/Events</a><h4 id="HEADERX">Header Information</h4><table cellspacing="3"><tr><td><b>Short Text&nbsp;</b></td><td nowrap> Function module "ZCX_GETDDICSOURCE" cannot be called 'remotely'. </td></tr><tr><td><b>Runtime Error&nbsp;</b></td><td nowrap> CALL_FUNCTION_NOT_REMOTE </td></tr><tr><td><b>Program&nbsp;</b></td><td nowrap> SAPLZGFCX_1 </td></tr><tr><td><b>Date/Time&nbsp;</b></td><td nowrap> 01.12.2025 12:45:03 (System) </td></tr><tr><td><b>User&nbsp;</b></td><td nowrap> SEBLONDO (SEBASTIAN LONDONO SANCHEZ) </td></tr><tr><td><b>Client&nbsp;</b></td><td nowrap> 100 </td></tr><tr><td><b>Host&nbsp;</b></td><td nowrap> vhs4dapci_S4D_00 </td></tr></table><h4 id="WHATHAPPENED">What happened?</h4>A function module is called via a CALL FUNCTION with one of the<br>following additions: DESTINATION, STARTING NEW TASK, IN BACKGROUND. The<br>function module is not flagged as capable of being called remotely<br>however.<br>Error in the ABAP application program.<br><br>The current ABAP program "SAPLZGFCX_1" had to be terminated because it found a<br>statement that could not be executed.<h4 id="ERROR">Error analysis</h4>An attempt was made to call function module "ZCX_GETDDICSOURCE" via RFC, but this<br>function module has not been released for 'remote' calls.<br><br>For the following call types, a module must be released for 'remote'<br>calls:<br><br>a) Call Function 'ZCX_GETDDICSOURCE' Destination ....<br>b) Call Function 'ZCX_GETDDICSOURCE' In Background ...<br>c) Call Function 'ZCX_GETDDICSOURCE' Starting New Task ...<br><br>For further information, see SAP Note 725951.<h4 id="TERMINATION">Information on where terminated</h4>The termination occurred in ABAP program or include "SAPLZGFCX_1", in "???". The<br>main program was "SAPMSSY1".<br><br>In the source code, the termination point is in line 185 of include "SAPMSSY1".<br>include "SAPMSSY1".<br><h4 id="SOURCE">Source Code Extract</h4><style> .showInRuntimeViewerLink { font-size:140%;  color: grey; }  .showInRuntimeViewerLink:link { font-size:140%;  color: grey; }   .keyword { color: blue }.comment { color: grey }.literal { color: green;}.number  { color: #3399ff;}.sourceline { padding-left: 3px; font-family: Consolas, "Liberation Mono", Courier, monospace;}.linenumber { display:block; color: grey; border-right: 1px solid; padding-left: 5px; padding-right: 3px; text-align: right; }.indicator { color: blue; font-weight: bold; }span { white-space: pre; font-family: Consolas, "Liberation Mono", Courier, monospace; }#sourcetable {  }#sourcetablecolumn { padding: 0px; }.highlight { padding-left: 3px; background-color: gold; }</style><table id="sourcetable" cellspacing="0"><tr><td id="sourcetablecolumn" style="vertical-align:top;"><span class="linenumber">169</span><span class="linenumber">170</span><span class="linenumber">171</span><span class="linenumber">172</span><span class="linenumber">173</span><span class="linenumber">174</span><span class="linenumber">175</span><span class="linenumber">176</span><span class="linenumber">177</span><span class="linenumber">178</span><span class="linenumber">179</span><span class="linenumber">180</span><span class="linenumber">181</span><span class="linenumber">182</span><span class="linenumber">183</span><span class="linenumber">184</span><span class="linenumber"><a title="Show where terminated" href="adt://S4D/sap/bc/adt/programs/includes/sapmssy1?context=%2fsap%2fbc%2fadt%2ffunctions%2fgroups%2fzgfcx_1/source/main#start=185"><span class="indicator">>>></span></a></span><span class="linenumber">186</span><span class="linenumber">187</span><span class="linenumber">188</span><span class="linenumber">189</span><span class="linenumber">190</span><span class="linenumber">191</span><span class="linenumber">192</span><span class="linenumber">193</span><span class="linenumber">194</span><span class="linenumber">195</span></td><td id="sourcetablecolumn"><div class="sourceline"><span><span class="keyword">ENDFORM.</span></span></div><div class="sourceline"><span><span>&nbsp;</span></span></div><div class="sourceline"><span><span class="comment">*&amp;---------------------------------------------------------------------*</span></span></div><div class="sourceline"><span><span class="comment">*&amp; Form remote_function_call</span></span></div><div class="sourceline"><span><span class="comment">*&amp;---------------------------------------------------------------------*</span></span></div><div class="sourceline"><span><span class="comment">* text</span></span></div><div class="sourceline"><span><span class="comment">*----------------------------------------------------------------------*</span></span></div><div class="sourceline"><span><span class="comment">* --&gt;VALUE text</span></span></div><div class="sourceline"><span><span class="comment">* --&gt;(TYPE) text</span></span></div><div class="sourceline"><span><span class="comment">*----------------------------------------------------------------------*</span></span></div><div class="sourceline"><span><span class="keyword">FORM</span> remote_function_call <span class="keyword">USING VALUE(</span>type<span class="keyword">).</span></span></div><div class="sourceline"><span>&nbsp;&nbsp;<span class="keyword">DATA</span> rc <span class="keyword">TYPE</span> i <span class="keyword">VALUE</span> <span class="number">0</span><span class="keyword">.</span></span></div><div class="sourceline"><span>&nbsp;&nbsp;<span class="keyword">DATA:</span> l_syxform <span class="keyword">TYPE</span> syxform<span class="keyword">,</span></span></div><div class="sourceline"><span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;l_syxprog <span class="keyword">TYPE</span> syxprog<span class="keyword">.</span></span></div><div class="sourceline"><span><span>&nbsp;</span></span></div><div class="sourceline"><span>&nbsp;&nbsp;<span class="keyword">DO.</span></span></div><div class="sourceline highlight"><a title="Show where terminated" href="adt://S4D/sap/bc/adt/programs/includes/sapmssy1?context=%2fsap%2fbc%2fadt%2ffunctions%2fgroups%2fzgfcx_1/source/main#start=185" style="text-decoration:none;"><span>&nbsp;&nbsp;&nbsp;&nbsp;<span class="keyword">CALL</span> <span class="literal">'RfcImport'</span> <span class="keyword">ID</span> <span class="literal">'Type'</span> <span class="keyword">FIELD</span> type</span></a></div><div class="sourceline"><span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="keyword">ID</span> <span class="literal">'SYXForm'</span> <span class="keyword">FIELD</span> l_syxform</span></div><div class="sourceline"><span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="keyword">ID</span> <span class="literal">'SYXProg'</span> <span class="keyword">FIELD</span> l_syxprog<span class="keyword">.</span></span></div><div class="sourceline"><span><span>&nbsp;</span></span></div><div class="sourceline"><span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="keyword">PERFORM (</span>l_syxform<span class="keyword">) IN PROGRAM (</span>l_syxprog<span class="keyword">).</span></span></div><div class="sourceline"><span><span>&nbsp;</span></span></div><div class="sourceline"><span>&nbsp;&nbsp;<span class="keyword">ENDDO.</span> <span class="comment">"#EC DO_OK</span></span></div><div class="sourceline"><span><span class="keyword">ENDFORM.</span> <span class="comment">"remote_function_call</span></span></div><div class="sourceline"><span><span>&nbsp;</span></span></div><div class="sourceline"><span><span class="comment">* RFC *from* sapgui, called from sapmssyd</span></span></div><div class="sourceline"><span><span class="keyword">FORM</span> remote_function_fromsaptemu<span class="keyword">.</span></span></div></td></tr></table><h4 id="STACK">Active Calls/Events</h4><style>code { font-family: Consolas, "Liberation Mono", Courier, monospace; }</style><table cellspacing="5"><tr><th align="left">No.</th><th align="left">Event</th><th align="left">Program</th><th align="left">Include</th><th align="left">Line</th></tr><tr><td><code><a href="adt://S4D/sap/bc/adt/programs/programs/sapmssy1/source/main#start=185">2</a></code></td><td><code>REMOTE_FUNCTION_CALL</code></td><td><code>SAPMSSY1</code></td><td><code>SAPMSSY1</code></td><td><code>185</code></td></tr><tr><td><code><a href="adt://S4D/sap/bc/adt/programs/programs/sapmssy1/source/main#start=35">1</a></code></td><td><code>%_RFC_START</code></td><td><code>SAPMSSY1</code></td><td><code>SAPMSSY1</code></td><td><code>35</code></td></tr></table>

Llamdo 3

GET /sap/bc/adt/runtime/dump/20251201124503vhs4dapci_S4D_00%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20SEBLONDO%20%20%20%20100%20%20%20%20%20%20%20%207/formatted HTTP/1.1

Header Key: Header Value
================================================================================================
Accept    : text/plain
User-Agent: Eclipse/4.36.0.v20250528-1830 (macosx; aarch64; Java 21.0.9) ADT/3.50.0 (devedition)

Response:

----------------------------------------------------------------------------------------------------
Categoría              Error de programación ABAP                                                 
Err.tmpo.ejec.         CALL_FUNCTION_NOT_REMOTE                                                     
Programa ABAP          SAPLZGFCX_1                                                                  
Application Component  Not assigned                                                                 
Fecha y hora           01.12.2025 12:45:03 (UTC-5)                                                  
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Texto breve                                                                                       |
|    Function module "ZCX_GETDDICSOURCE" cannot be called 'remotely'.                              |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|¿Qué ha sucedido?                                                                                 |
|    A function module is called via a CALL FUNCTION with one of the                               |
|    following additions: DESTINATION, STARTING NEW TASK, IN BACKGROUND. The                       |
|    function module is not flagged as capable of being called remotely                            |
|    however.                                                                                      |
|    Error in the ABAP application program.                                                        |
|                                                                                                  |
|    The current ABAP program "SAPLZGFCX_1" had to be terminated because it found a                |
|    statement that could not be executed.                                                         |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|¿Qué puede hacer?                                                                                 |
|    Note which actions and entries caused the error to occur.                                     |
|                                                                                                  |
|    Consult your SAP administrator.                                                               |
|                                                                                                  |
|    Using transaction ST22 for ABAP dump analysis, you can view, manage,                          |
|    and retain termination messages for longer periods.                                           |
|    Note which actions and entries caused the error to occur.                                     |
|                                                                                                  |
|    Consult your SAP administrator.                                                               |
|                                                                                                  |
|    Using transaction ST22 for ABAP dump analysis, you can view, manage,                          |
|    and retain termination messages for longer periods.                                           |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Anál.errores                                                                                      |
|    An attempt was made to call function module "ZCX_GETDDICSOURCE" via RFC, but                  |
|     this                                                                                         |
|    function module has not been releasedfor 'remote' calls.                                      |
|                                                                                                  |
|    For the following call types, a module must be released for 'remote'                          |
|    calls:                                                                                        |
|                                                                                                  |
|    a) Call Function 'ZCX_GETDDICSOURCE' Destination ....                                         |
|    b) Call Function 'ZCX_GETDDICSOURCE' In Background ...                                        |
|    c) Call Function 'ZCX_GETDDICSOURCE' Starting New Task ...                                    |
|                                                                                                  |
|    For further information, see SAP Note 725951.                                                 |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Notas para corregir errores                                                                       |
|    If the module is a function module of your own,                                               |
|    please do the following:                                                                      |
|    Release module "ZCX_GETDDICSOURCE" in the function library for RFC calls by                   |
|     setting                                                                                      |
|    the button to allow it to be called remotely.                                                 |
|                                                                                                  |
|    If you cannot solve the problem yourself, please send the following                           |
|    information to SAP:                                                                           |
|                                                                                                  |
|    1. The description of the problem (short dump)                                                |
|    To do this, choose  System -> List -> Save -> Local File (unconverted)                        |
|    on the screen you are in now.                                                                 |
|                                                                                                  |
|    2. The relevant system log                                                                    |
|    To do this, call the system log in transaction SM21. Restrict the time                        |
|    interval to ten minutes before the short dump and five minutes after                          |
|    it. In the display, choose System -> List -> Save -> Local File                               |
|    (unconverted).                                                                                |
|                                                                                                  |
|    3. If these are programs of your own, or modified SAP programs: The                           |
|    source text of the programs                                                                   |
|    To do this, please choose                                                                     |
|    "Other Utilties -> Upload/Download -> Download".                                              |
|                                                                                                  |
|    4. Details regarding the conditions under which the error occurred or                         |
|    which actions and input caused the error.                                                     |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Entorno sistema                                                                                   |
|    SAP Release..... 755                                                                          |
|    SAP Basis level 0001                                                                          |
|                                                                                                  |
|    Application server... vhs4dapci                                                               |
|    Instance name....... vhs4dapci_S4D_00                                                         |
|    Network address...... 172.27.154.8                                                            |
|    Operating system... Linux                                                                     |
|    Release.............. 6.4.0-150700.53.19-d                                                    |
|    Hardware type....... x86_64                                                                   |
|    Character length..... 16 Bits                                                                 |
|    Pointer length........ 64 Bits                                                                |
|    Work process number... 7                                                                      |
|    Work process ID....... 41825                                                                  |
|    ABAP load version.... 2736                                                                    |
|    Shortdump setting. full                                                                       |
|                                                                                                  |
|    Database server... vhshddb00                                                                  |
|    Database type..... HDB                                                                        |
|    Database name..... S4D                                                                        |
|    Database user ID SAPHANADB                                                                    |
|                                                                                                  |
|    Terminal.......... 192.168.1.237                                                              |
|                                                                                                  |
|    Character set C                                                                               |
|                                                                                                  |
|    SAP kernel....... 781                                                                         |
|    Created on....... Jul 25 2022 12:37:40                                                        |
|    Created at....... Linux GNU SLES-12 x86_64 cc8.2.1 use-pr220722                               |
|    Database version SQLDBC 2.07.017.1607722875                                                   |
|    Patch level....... 300                                                                        |
|    Patch text.......                                                                             |
|                                                                                                  |
|    Database............. HANA 1.0, HANA 2.0                                                      |
|    SAP database version. 781                                                                     |
|    Operating system... Linux                                                                     |
|                                                                                                  |
|    Memory consumption                                                                            |
|    EM...... 2393064                                                                              |
|    Heap.... 0                                                                                    |
|    Page.... 0                                                                                    |
|    MM used. 1251024                                                                              |
|    MM free. 1138504                                                                              |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Usuario y transacción                                                                             |
|    Client................. 100                                                                   |
|    User.................. SEBLONDO                                                               |
|    Language key.......... S                                                                      |
|    Transaction.........                                                                          |
|    Transaction ID...... E730B5C779AA0070E0069295DC35EB44                                         |
|                                                                                                  |
|    EPP Root Context ID.... 36393244443033303639324444303330                                      |
|    EPP Connection ID...... 00000000000000000000000000000000                                      |
|    EPP Connection Counter. 5                                                                     |
|    EPP Component Name..... S4D/vhs4dapci_S4D_00                                                  |
|                                                                                                  |
|    Program....................... SAPLZGFCX_1                                                    |
|    Screen......................... SAPMSSY1                                3004                  |
|    Screen line....................2                                                              |
|    Debugger interpretor loop... "none"                                                           |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Server-Side Connection Information                                                                |
|    Information on caller of Remote Function Call (RFC):                                          |
|    System.............. ########                                                                 |
|    Installation number ##########                                                                |
|    Database Release..... 753                                                                     |
|    Kernel Release...... 753                                                                      |
|    Connection type..... "E"  (2=R/2, 3=ABAP-System, E=External,                                  |
|    R=Reg.External) call type....... synchronous and non-transactional (emode 0,                  |
|     imode 0)                                                                                     |
|    Inbound TID..........                                                                         |
|    Inbound queue name...                                                                         |
|    Outbound TID.........                                                                         |
|    Outbound queue name..                                                                         |
|                                                                                                  |
|    Client................. ###                                                                   |
|    User.................. ############                                                           |
|    Transaction.........                                                                          |
|    Call program...........SAPJCo31                                                               |
|    Function module..... ZCX_GETDDICSOURCE                                                        |
|    Call destination..... SAP_SYSTEM                                                              |
|    Source server...... Sebastians-MacBook-Air                                                    |
|    Source IP address.. 192.168.1.237                                                             |
|                                                                                                  |
|    Additional information on RFC logon:                                                          |
|    Trusted relationship..                                                                        |
|    Logon return code.... 0                                                                       |
|    Trusted return code.. 0                                                                       |
|                                                                                                  |
|    Remarks:                                                                                      |
|    In Releases prior to 4.0, information about the RFC caller might be                           |
|    missing or incomplete.                                                                        |
|    - The installation number is provided in caller Release 700 and higher.                       |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Info posición de cancelación                                                                      |
|    The termination occurred in ABAP program or include "SAPLZGFCX_1", in " ". The                |
|    main program was "SAPMSSY1".                                                                  |
|                                                                                                  |
|    In the source code, the termination point is in line 185 of include "SAPMSSY1".               |
|    include "SAPMSSY1".                                                                           |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Detalle código fuente                                                                             |
----------------------------------------------------------------------------------------------------
|Lín. |Txt.fte.                                                                                    |
----------------------------------------------------------------------------------------------------
|  155|      ENDIF.                       "Otherwise callback is allowed.                          |
|  156|*                                                                                           |
|  157|    WHEN OTHERS.                                                                            |
|  158|      CLEAR <out_result>.                      "Unexpected branch: Callback is forbidden.   |
|  159|  ENDCASE.                                                                                  |
|  160|                                                                                            |
|  161|                                                                                            |
|  162|  CALL 'AB_SET_C_PARMS' ID 'P4' FIELD <out_result>. "#EC CI_CCALL CI_CCALL                  |
|  163|                                                                                            |
|  164|ENDFORM.                                                                                    |
|  165|                                                                                            |
|  166|                                                                                            |
|  167|FORM AFTER_LOGON_SCREEN.                                                                    |
|  168|*  MESSAGE a000(s_unified_con) with 'test'.                                                 |
|  169|ENDFORM.                                                                                    |
|  170|                                                                                            |
|  171|*&---------------------------------------------------------------------*                    |
|  172|*&      Form  remote_function_call                                                          |
|  173|*&---------------------------------------------------------------------*                    |
|  174|*       text                                                                                |
|  175|*----------------------------------------------------------------------*                    |
|  176|*      -->VALUE      text                                                                   |
|  177|*      -->(TYPE)     text                                                                   |
|  178|*----------------------------------------------------------------------*                    |
|  179|FORM remote_function_call USING VALUE(type).                                                |
|  180|  DATA rc             TYPE i VALUE 0.                                                       |
|  181|  DATA: l_syxform          TYPE syxform,                                                    |
|  182|        l_syxprog          TYPE syxprog.                                                    |
|  183|                                                                                            |
|  184|  DO.                                                                                       |
|>>>>>|    CALL 'RfcImport' ID'Type'        FIELD type                                             |
|  186|                     ID 'SYXForm'     FIELD l_syxform                                       |
|  187|                     ID 'SYXProg'     FIELD l_syxprog.                                      |
|  188|                                                                                            |
|  189|      PERFORM (l_syxform) IN PROGRAM (l_syxprog).                                           |
|  190|                                                                                            |
|  191|  ENDDO.                                                    "#EC DO_OK                      |
|  192|ENDFORM.                    "remote_function_call                                           |
|  193|                                                                                            |
|  194|* RFC *from* sapgui, called from sapmssyd                                                   |
|  195|FORM remote_function_fromsaptemu.                                                           |
|  196|  "do. only *one* call                                                                      |
|  197|  CALL 'RfcImport' ID 'Type' FIELD rfctype_saptemu.                                         |
|  198|  PERFORM (sy-xform) IN PROGRAM (sy-xprog).                                                 |
|  199|  "rsyn >scont sysc 00011111 0. (wenn überhaupt, dann 10 oder 11?)                          |
|  200|  "but we leave sapmssyd the normal way.                                                    |
|  201|  "enddo.                                                                                   |
|  202|ENDFORM ##CALLED.                    "remote_function_fromsaptemu                           |
|  203|                                                                                            |
|  204|*&---------------------------------------------------------------------*                    |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Contenido campos sistema                                                                          |
----------------------------------------------------------------------------------------------------
|Nom.    |Val.                                                                                     |
----------------------------------------------------------------------------------------------------
|SY-SUBRC|0                                                                                        |
|SY-INDEX|1                                                                                        |
|SY-TABIX|0                                                                                        |
|SY-DBCNT|0                                                                                        |
|SY-FDPOS|0                                                                                        |
|SY-LSIND|0                                                                                        |
|SY-PAGNO|0                                                                                        |
|SY-LINNO|1                                                                                        |
|SY-COLNO|1                                                                                        |
|SY-PFKEY|                                                                                         |
|SY-UCOMM|                                                                                         |
|SY-TITLE|Control CPI-C y RFC                                                                      |
|SY-MSGTY|                                                                                         |
|SY-MSGID|                                                                                         |
|SY-MSGNO|000                                                                                      |
|SY-MSGV1|                                                                                         |
|SY-MSGV2|                                                                                         |
|SY-MSGV3|                                                                                         |
|SY-MSGV4|                                                                                         |
|SY-MODNO|0                                                                                        |
|SY-DATUM|20251201                                                                                 |
|SY-UZEIT|124503                                                                                   |
|SY-XPROG|SAPLZGFCX_1                                                                              |
|SY-XFORM|ZCX_GETDDICSOURCE                                                                        |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Llamadas/Eventos activos                                                                          |
----------------------------------------------------------------------------------------------------
|Nº    Cl.          Programa                            Include                             Lín.   |
|      Nom.                                                                                        |
----------------------------------------------------------------------------------------------------
|    2 FORM         SAPMSSY1                            SAPMSSY1                185                |
|      REMOTE_FUNCTION_CALL                                                                        |
|    1 MODULE (PBO) SAPMSSY1                            SAPMSSY1                               35  |
|      %_RFC_START                                                                                 |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Variables seleccionadas                                                                           |
----------------------------------------------------------------------------------------------------
|Nom.                                                                                              |
|    Val.                                                                                          |
----------------------------------------------------------------------------------------------------
|Nº        2 Cl.          FORM                                                                     |
|Nom.  REMOTE_FUNCTION_CALL                                                                        |
----------------------------------------------------------------------------------------------------
|%_DUMMY$$                                                                                         |
|                                                                                                  |
|    2222                                                                                          |
|    0000                                                                                          |
|    0000                                                                                          |
|    0000                                                                                          |
|    2000200020002000                                                                              |
|SY-REPID                                                                                          |
|    SAPMSSY1                                                                                      |
|    5454555322222222222222222222222222222222                                                      |
|    310D339100000000000000000000000000000000                                                      |
|    0000000000000000000000000000000000000000                                                      |
|    0000000000000000000000000000000000000000                                                      |
|    5300410050004D00530053005900310020002000200020002000200020002000200020002000200020002000200020|
|SYST-REPID                                                                                        |
|    SAPMSSY1                                                                                      |
|    5454555322222222222222222222222222222222                                                      |
|    310D339100000000000000000000000000000000                                                      |
|    0000000000000000000000000000000000000000                                                      |
|    0000000000000000000000000000000000000000                                                      |
|    5300410050004D00530053005900310020002000200020002000200020002000200020002000200020002000200020|
|COUNT                                                                                             |
|    0                                                                                             |
|    0000                                                                                          |
|    0000                                                                                          |
|    00000000                                                                                      |
|TYPE                                                                                              |
|    3                                                                                             |
|    0000                                                                                          |
|    3000                                                                                          |
|    03000000                                                                                      |
|L_SYXFORM                                                                                         |
|    ZCX_GETDDICSOURCE                                                                             |
|    545544544445455442222222222222                                                                |
|    A38F75444933F52350000000000000                                                                |
|    000000000000000000000000000000                                                                |
|    000000000000000000000000000000                                                                |
|    5A00430058005F00470045005400440044004900430053004F00550052004300450020002000200020002000200020|
|L_SYXPROG                                                                                         |
|    SAPLZGFCX_1                                                                                   |
|    5454544455322222222222222222222222222222                                                      |
|    310CA7638F100000000000000000000000000000                                                      |
|    0000000000000000000000000000000000000000                                                      |
|    0000000000000000000000000000000000000000                                                      |
|    5300410050004C005A0047004600430058005F00310020002000200020002000200020002000200020002000200020|
|%_SPACE                                                                                           |
|                                                                                                  |
|    2                                                                                             |
|    0                                                                                             |
|    0                                                                                             |
|    0                                                                                             |
|    2000                                                                                          |
----------------------------------------------------------------------------------------------------
|Nº        1 Cl.          MODULE (PBO)                                                             |
|Nom.  %_RFC_START                                                                                 |
----------------------------------------------------------------------------------------------------
|SY                                                                                                |
|    {1;0;0;0;0;0;0;0;0;0;0;0;1;0;1;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;22;84;0;0;0;18000|
|    0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000|
|    1000000000000000000000000000000000000000000000001000000010000000000000000000000000000000000000|
|    0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000|
|RFCTYPE_INTERNAL                                                                                  |
|    3                                                                                             |
|    0000                                                                                          |
|    3000                                                                                          |
|    03000000                                                                                      |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Llamadas de aplicación                                                                            |
----------------------------------------------------------------------------------------------------
|Índ. |Llam.                                                                                       |
----------------------------------------------------------------------------------------------------
|    1|R=4 T=S S=Sebastians-MacBook-Air I=SAPJCo31 F=SADT_REST_RFC_ENDPOINT C= U=                  |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Notas internas                                                                                    |
|    The termination was triggered in function "ab_rfcimport" of the SAP kernel, in                |
|    line 5205 of module "//bas/781_STACK/src/krn/rfc/abrfcfun.c#4".                               |
|    The internal operation just processed is "CALY".                                              |
|    Internal mode started at 20251201124502.                                                      |
|    Parameter........: "-", "-", -                                                                |
|    Other parameters: "-", "-"                                                                    |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Llamadas activas núcleo SAP                                                                       |
----------------------------------------------------------------------------------------------------
|Líneas de pila C en núcleo (estructura diferente según la plataforma)                             |
----------------------------------------------------------------------------------------------------
|dw.sapS4D_D00[S](LinStackBacktrace(void**, int, int)+0xa6)[0x55719f0b904c,0x8b904c]               |
|dw.sapS4D_D00[S](LinStack(_IO_FILE*)+0x44)[0x55719f0bca8f,0x8bca8f]                               |
|dw.sapS4D_D00[S](CTrcStack2+0x4b)[0x55719f0b8ae9,0x8b8ae9]                                        |
|dw.sapS4D_D00[S](rabax_CStackSave()+0x9e)[0x55719f34aaa4,0xb4aaa4]                                |
|dw.sapS4D_D00[S](rabax(char16_t const*, char16_t const*, int, char16_t const*, void const*) [clone|
|dw.sapS4D_D00[S](ab_rfcimport.cold.33+0x996)[0x55719f870f2d,0x1070f2d]                            |
|dw.sapS4D_D00[S](ab_jcaly()+0x11d)[0x5571a0ff2d5d,0x27f2d5d]                                      |
|dw.sapS4D_D00[S](ab_extri()+0x2a4)[0x5571a0f96e24,0x2796e24]                                      |
|dw.sapS4D_D00[S](ab_xevent(char16_t const*)+0x31)[0x5571a0fc57a1,0x27c57a1]                       |
|dw.sapS4D_D00[S](ab_dstep+0xfc)[0x5571a0f94fdc,0x2794fdc]                                         |
|dw.sapS4D_D00[S](dynprctl(DINFDUMY*)+0x68e)[0x5571a0f7ad5e,0x277ad5e]                             |
|dw.sapS4D_D00[S](dynpen00+0x3a8)[0x5571a0f79bf8,0x2779bf8]                                        |
|dw.sapS4D_D00[S](ThrtCallAbapVm.cold.10+0xaf)[0x55719f041f72,0x841f72]                            |
|dw.sapS4D_D00[S](RfcHandler::handleRequest(REQUEST_BUF*, bool)+0x1fc)[0x5571a0f682dc,0x27682dc]   |
|dw.sapS4D_D00[S](ThHandleRequest(REQUEST_BUF*, unsigned char, unsigned char)+0x227)[0x5571a0f59f27|
|dw.sapS4D_D00[S](ThStart()+0x5a0)[0x5571a127be90,0x2a7be90]                                       |
|dw.sapS4D_D00[S](DpMain+0x41f)[0x5571a124663f,0x2a4663f]                                          |
|dw.sapS4D_D00[S](main+0x2d)[0x5571a0efe2ed,0x26fe2ed]                                             |
|libc.so.6[S](__libc_start_call_main+0x7e)[0x7f8d42240e6c,0x40e6c]                                 |
|libc.so.6[S](__libc_start_main_alias_2+0x87)[0x7f8d42240f35,0x40f35]                              |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Lista de programas ABAP implicados                                                                |
----------------------------------------------------------------------------------------------------
|Índ.  |Tp.|Programa                                |Grupo |Fecha     |Tmpo    |Tam.     |Idioma   |
----------------------------------------------------------------------------------------------------
|     0|Prg|SAPMSSY1                                |     0|26.11.2018|11:04:02|    33792|S        |
|     1|Prg|SAPLZGFCX_1                             |     1|24.11.2025|23:52:19|    98304|S        |
|     2|Typ|SYST                                    |     0|10.10.2014|18:27:32|    32768|         |
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
|Directorio detablas de aplicación                                                                 |
----------------------------------------------------------------------------------------------------
|Nom.                                     Fecha      Tmpo       Long.                              |
|    Val.                                                                                          |
----------------------------------------------------------------------------------------------------
|Programa -                                                                                        |
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
|Programa SAPMSSY1                                                                                 |
----------------------------------------------------------------------------------------------------
|SYST                             .  .       :  :     00004612                                     |
|    \x0001\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\x0001                                    |
|SY                                         .  .       :  :     00004612                           |
|    \x0001\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\x0001                                    |
----------------------------------------------------------------------------------------------------