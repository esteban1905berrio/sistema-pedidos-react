@EndUserText.label: 'TF para ultimo Log de idoc'
@AccessControl.authorizationCheck: #NOT_REQUIRED
define table function ZTFI1008_2
returns
{
  mandt       : abap.clnt;
  numero_idoc : edi_docnum;
  mestyp      : edi_mestyp;
  status      : edi_status;
  statyp      : edi_symsty;
  statxt      : edi_statx_;
  stapa1      : edi_stapa1;
  stapa2      : edi_stapa2;
  stapa3      : edi_stapa3;
  stapa4      : edi_stapa4;
  stamid      : edi_stamid;
  stamno      : edi_stamno;
  uname       : edi_uname;
  logdat      : edi_logdat;
  logtim      : edi_logtim;
  countr      : edi_countr;

}
implemented by method
  zclfii1008_amdp_datos_idoc=>log_idoc;