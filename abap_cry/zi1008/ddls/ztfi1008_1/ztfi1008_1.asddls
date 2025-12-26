@EndUserText.label: 'TF pra datos de idoc financieros'
@AccessControl.authorizationCheck: #NOT_REQUIRED
define table function ZTFI1008_1
returns
{
  mandt       : abap.clnt;
  numero_idoc : edi_docnum;
  mestyp      : edi_mestyp;
  segnam      : edi_segnam;
  status      : edi_status;
  sociedad    : bukrs;
  belnr       : belnr_d;
  gjahr       : gjahr;
  tipo_doc    : abap.char(4);
  bldat       : bldat;
  budat       : budat;

}
implemented by method
  zclfii1008_amdp_datos_idoc=>datos_general_financiero;