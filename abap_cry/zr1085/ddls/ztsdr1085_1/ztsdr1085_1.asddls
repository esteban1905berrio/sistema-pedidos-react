@EndUserText.label: 'TF documentos FI Ecommerce relacionados a orden de compra'
define table function ztsdr1085_1
returns
{
  key mandt                 : abap.clnt;
  key numero_idoc           : edi_docnum;
  key numero_documento_fi   : belnr_d;
  key pedido_cliente        : bstkd;
      tipo_documento        : blart;
      mestyp                : edi_mestyp;
      segnam                : edi_segnam;
      status                : edi_status;
      sociedad              : bukrs;
      periodo               : monat;
      ejercicio             : gjahr;
      fecha_contabilizacion : budat;
      referencia_1          : abap.char(12);

}
implemented by method
  zclsdr1085_monitor_pedidos=>datos_idoc_financiero;