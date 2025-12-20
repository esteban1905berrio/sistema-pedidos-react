CLASS zclcxr1002_util_ftp DEFINITION
  PUBLIC
*  FINAL
  CREATE PUBLIC .

  PUBLIC SECTION.

    TYPES:
      BEGIN OF gtp_comandos,
        comando TYPE char256,
      END OF gtp_comandos,

      gtp_ti_comandos TYPE STANDARD TABLE OF gtp_comandos.

    DATA:
      gti_comandos TYPE gtp_ti_comandos.

    METHODS:
      "! Abrir conexión y procesar comando, luego desconectar.
      "! @parameter i_user |Usuario
      "! @parameter i_pwd |Password
      "! @parameter i_host |Host
      "! @parameter i_destino |Destino lógico (será indicado al llamar la función)
      "! @parameter i_ti_comandos |Comandos a ejecutar
      "! @parameter i_dest |Destino lógico (será indicado al llamar la función)
      "! @parameter i_compress |Indicador de una posición
      "! @parameter e_subrc |Código retorno, código retorno tras sentencias ABAP
      procesar
        IMPORTING
          VALUE(i_user)        TYPE zzchar30
          VALUE(i_pwd)         TYPE zzchar30
          VALUE(i_host)        TYPE zzchar64
          VALUE(i_destino)     TYPE zzchar80 OPTIONAL
          VALUE(i_ti_comandos) TYPE gtp_ti_comandos
          VALUE(i_dest)        TYPE rfcdes-rfcdest DEFAULT 'SAPFTPA'
          VALUE(i_compress)    TYPE char1 DEFAULT 'N'
        EXPORTING
          !e_subrc             TYPE sysubrc,

      "! Procesar con Tabla Interna
      "! @parameter i_user |Usuario
      "! @parameter i_pwd |Password
      "! @parameter i_host |Host
      "! @parameter i_destino |Carpeta Destino - Validar si la carpeta ya existe
      "! @parameter i_dest |Destino lógico (será indicado al llamar la función)
      "! @parameter i_compress |Indicador de una posición
      "! @parameter i_full_path |Ruta con Nombre de Archivo
      "! @parameter i_ti_data |Tabla a Enviar al FTP
      "! @parameter e_subrc |Código retorno, código retorno tras sentencias ABAP
      procesar_ti
        IMPORTING
          VALUE(i_user)      TYPE zzchar30
          VALUE(i_pwd)       TYPE zzchar30
          VALUE(i_host)      TYPE zzchar64
          VALUE(i_destino)   TYPE zzchar80 OPTIONAL
          VALUE(i_dest)      TYPE rfcdes-rfcdest DEFAULT 'SAPFTPA'
          VALUE(i_compress)  TYPE char1 DEFAULT 'N'
          VALUE(i_full_path) TYPE zzchar80
          !i_ti_data         TYPE ANY TABLE
        EXPORTING
          !e_subrc           TYPE sysubrc,

      "! Retornar mensaje / error
      "! @parameter e_es_mensaje |Mensaje / Error
      get_mensaje
        EXPORTING
          !e_es_mensaje TYPE string .
  PROTECTED SECTION.
  PRIVATE SECTION.

    TYPES:
      BEGIN OF gtp_result,
        line(100) TYPE c,
      END OF gtp_result.

    DATA:
      ges_mensaje  TYPE string.

    "! Ejecutar comando FTP
    "! @parameter i_hdl |Conexion FTP(Handler)
    "! @parameter i_cmd |Comando FTP
    "! @parameter i_compress |Indicador de una posición
    "! @parameter e_subrc |Código retorno, código retorno tras sentencias ABAP
    METHODS:
      ftp_command
        IMPORTING
          VALUE(i_hdl)      TYPE i
          VALUE(i_cmd)      TYPE char256
          VALUE(i_compress) TYPE char1 OPTIONAL
        EXPORTING
          !e_subrc          TYPE sysubrc,

      "! Conectar a servidor FTP
      "! @parameter i_user |Usuario
      "! @parameter i_pwd |Password
      "! @parameter i_host |Host
      "! @parameter i_dest |Destino lógico (será indicado al llamar la función)
      "! @parameter e_hdl |Conexion FTP(Handler)
      "! @parameter e_subrc |Código retorno, código retorno tras sentencias ABAP
      ftp_connect
        IMPORTING
          VALUE(i_user) TYPE zzchar30
          VALUE(i_pwd)  TYPE zzchar30
          VALUE(i_host) TYPE zzchar64
          VALUE(i_dest) TYPE rfcdes-rfcdest
        EXPORTING
          !e_hdl        TYPE i
          !e_subrc      TYPE sysubrc,

      "! Desconectar de servidor FTP
      "! @parameter i_hdl |Conexion FTP(Handler)
      "! @parameter i_dest |Destino lógico (será indicado al llamar la función)
      "! @parameter e_subrc |Código retorno, código retorno tras sentencias ABAP
      ftp_disconnect
        IMPORTING
          VALUE(i_hdl) TYPE i
          !i_dest      TYPE rfcdes-rfcdest OPTIONAL
        EXPORTING
          !e_subrc     TYPE sysubrc .
ENDCLASS.



CLASS zclcxr1002_util_ftp IMPLEMENTATION.
  METHOD ftp_command.
*----------------------------------------------------------------------*
*   Definición de tablas internas locales
*----------------------------------------------------------------------*
    DATA: ti_result TYPE STANDARD TABLE OF gtp_result.

    ".Ejecutar comando
    CALL FUNCTION 'FTP_COMMAND'
      EXPORTING
        handle        = i_hdl
        command       = i_cmd
        compress      = i_compress
      TABLES
        data          = ti_result
      EXCEPTIONS
        tcpip_error   = 1
        command_error = 2
        data_error    = 3
        OTHERS        = 4.
    e_subrc = sy-subrc.
    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
              WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4 INTO ges_mensaje.
      CONCATENATE ges_mensaje cl_abap_char_utilities=>cr_lf
             INTO ges_mensaje.
    ENDIF.

    LOOP AT ti_result INTO DATA(es_result).
      CONCATENATE ges_mensaje es_result-line
                  cl_abap_char_utilities=>cr_lf
             INTO ges_mensaje.
    ENDLOOP.
  ENDMETHOD.

  METHOD ftp_connect.
*----------------------------------------------------------------------*
*.Definición de variables locales
*----------------------------------------------------------------------*
    DATA: slen TYPE i,
          pwd  TYPE zzchar30.

    CONSTANTS: c_key  TYPE i VALUE 26101957.

    pwd = i_pwd.
    SET EXTENDED CHECK OFF.
    slen = strlen( pwd ).

    ".Password Encryption
    CALL FUNCTION 'HTTP_SCRAMBLE'
      EXPORTING
        source      = pwd
        sourcelen   = slen
        key         = c_key
      IMPORTING
        destination = pwd.

    ".Conectar al servidor FTP
    CALL FUNCTION 'FTP_CONNECT'
      EXPORTING
        user            = i_user
        password        = pwd
        host            = i_host
        rfc_destination = i_dest
      IMPORTING
        handle          = e_hdl
      EXCEPTIONS
        not_connected   = 1
        OTHERS          = 2.
    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
              WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4 INTO ges_mensaje.
    ENDIF.

    e_subrc = sy-subrc.
  ENDMETHOD.

  METHOD ftp_disconnect.
    ".Desconectar FTP
    CALL FUNCTION 'FTP_DISCONNECT'
      EXPORTING
        handle = i_hdl.

    ".Desconectar RFC
    CALL FUNCTION 'RFC_CONNECTION_CLOSE'
      EXPORTING
        destination          = i_dest
      EXCEPTIONS
        destination_not_open = 1
        OTHERS               = 2.
    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
              WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4 INTO ges_mensaje.
    ENDIF.

    e_subrc = sy-subrc.
  ENDMETHOD.

  METHOD procesar.
*----------------------------------------------------------------------*
*.Definicion de variable locales
*----------------------------------------------------------------------*
    DATA: hdl TYPE i.

    ".Abrir conexión FTP
    CALL METHOD me->ftp_connect
      EXPORTING
        i_user  = i_user
        i_pwd   = i_pwd
        i_host  = i_host
        i_dest  = i_dest
      IMPORTING
        e_hdl   = hdl
        e_subrc = e_subrc.
    IF e_subrc <> 0.
      RETURN.
    ENDIF.

    ". Validar si la carpeta a crear ya existe
*    CALL METHOD me->validar_dir_exist
*      EXPORTING
*        i_destino  = i_destino
*        i_hdl      = hdl
*        i_compress = i_compress
*      EXCEPTIONS
*        dir_exit   = 1
*        OTHERS     = 2.
*    IF sy-subrc = 1.
*      CLEAR i_cmd1.
*    ENDIF.

    ".Validamos si existe informacion de comandos a ejcutar
    CHECK lines( i_ti_comandos ) GT 0.

    ".Recorremos los comandos a ejecutar
    LOOP AT i_ti_comandos ASSIGNING FIELD-SYMBOL(<fs_comando>).

      IF <fs_comando>-comando IS INITIAL.
        CONTINUE.
      ENDIF.

      ".Ejecutar comando FTP
      CALL METHOD me->ftp_command
        EXPORTING
          i_hdl      = hdl
          i_cmd      = <fs_comando>-comando
          i_compress = i_compress
        IMPORTING
          e_subrc    = e_subrc.
      IF e_subrc <> 0.
        RETURN.
      ENDIF.

    ENDLOOP.

    ".Desconectar FTP y RFC
    CALL METHOD me->ftp_disconnect
      EXPORTING
        i_hdl   = hdl
        i_dest  = i_dest
      IMPORTING
        e_subrc = e_subrc.
  ENDMETHOD.

  METHOD procesar_ti.

*----------------------------------------------------------------------*
    ".Definicion de Tipos
*----------------------------------------------------------------------*
    TYPES:
      tp_text(20000) TYPE c.
*----------------------------------------------------------------------*
    ".Definicion de tablas internas
*----------------------------------------------------------------------*
    DATA:
      ti_text  TYPE STANDARD TABLE OF tp_text,
      ti_xtext TYPE crd_t_1024.
*----------------------------------------------------------------------*
    ".Definicion de Estructuras
*----------------------------------------------------------------------*
    DATA:
      es_text  TYPE tp_text,
      es_xtext TYPE tbl1024.
*----------------------------------------------------------------------*
    ".Definicion de variable locales
*----------------------------------------------------------------------*
    DATA:
      hdl TYPE i.
*----------------------------------------------------------------------*
    ".Definicion de Objetos de Referencia
*----------------------------------------------------------------------*
    DATA:
      o_dref TYPE REF TO data .
*----------------------------------------------------------------------*
    ".Definicion de Field Symbols
*----------------------------------------------------------------------*
    FIELD-SYMBOLS:
                   <fs_es_data> TYPE any .

    CREATE DATA o_dref LIKE LINE OF i_ti_data.
    ASSIGN o_dref->* TO <fs_es_data> .

    LOOP AT i_ti_data ASSIGNING <fs_es_data> .
      MOVE <fs_es_data> TO es_text.
      APPEND es_text TO ti_text.
    ENDLOOP.

    ".Abrir conexión FTP
    CALL METHOD me->ftp_connect
      EXPORTING
        i_user  = i_user
        i_pwd   = i_pwd
        i_host  = i_host
        i_dest  = i_dest
      IMPORTING
        e_hdl   = hdl
        e_subrc = e_subrc.
    IF e_subrc <> 0.
      RETURN.
    ENDIF.
*----------------------------------------------------------------------*
    "Validar si la carpeta a crear ya existe
*----------------------------------------------------------------------*
*    CALL METHOD me->validar_dir_exist
*      EXPORTING
*        i_destino  = i_destino
*        i_hdl      = hdl
*        i_compress = i_compress
*      EXCEPTIONS
*        dir_exit   = 1
*        OTHERS     = 2.
*    IF sy-subrc = 1.
**    CLEAR i_c_cmd1.
*    ENDIF.

    ".FTP_R3_TO_SERVER:used to transfer the internal table data as a file to other system in the character mode.
    CALL FUNCTION 'FTP_R3_TO_SERVER'
      EXPORTING
        handle         = hdl
        fname          = i_full_path           "file path of destination system
        character_mode = 'X'
      TABLES
        text           = ti_text
      EXCEPTIONS
        tcpip_error    = 1
        command_error  = 2
        data_error     = 3
        OTHERS         = 4.
    IF sy-subrc <> 0.
      MESSAGE ID sy-msgid TYPE sy-msgty NUMBER sy-msgno
              WITH sy-msgv1 sy-msgv2 sy-msgv3 sy-msgv4 INTO ges_mensaje.
      CONCATENATE ges_mensaje cl_abap_char_utilities=>cr_lf
             INTO ges_mensaje.
    ENDIF.

    ".Desconectar FTP y RFC
    CALL METHOD me->ftp_disconnect
      EXPORTING
        i_hdl   = hdl
        i_dest  = i_dest
      IMPORTING
        e_subrc = e_subrc.

  ENDMETHOD.

  METHOD get_mensaje.
    e_es_mensaje = ges_mensaje.
  ENDMETHOD.

ENDCLASS.