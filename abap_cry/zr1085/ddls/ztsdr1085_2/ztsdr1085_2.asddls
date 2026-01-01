@EndUserText.label: 'TF inventario entrega'
define table function ZTSDR1085_2
returns
{
  key mandt            : abap.clnt;
  key matnr            : matnr;
  key werks            : werks_d;
  key lgort            : lgort_d;
  key charg            : charg_d;
      vrkme            : vrkme;
      @Semantics.quantity.unitOfMeasure : 'lips.vrkme'
      cantidad_entrega : lfimg;

}
implemented by method
  zclsdr1085_monitor_pedidos=>inventario_entrega;