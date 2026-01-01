@EndUserText.label: 'TF Conciliación inventarios Legacy'
define table function ztmmi1204_1
returns
{
  key mandt             : abap.clnt;
      centro            : werks_d;
      almacen           : zed_almacen_inventario_legacy;
      material_variante : matnr;
      lote              : charg_d;
      stock             : abap.quan(13,3);
      material_generico : satnr;
      talla             : wrf_size1;
      color             : wrf_color;
      unidad_medida     : meins;

}
implemented by method
  zclmmi1204_inventarios_legado=>consultar_inventario;