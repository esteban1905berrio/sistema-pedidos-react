@AbapCatalog.sqlViewName: 'ZPSDR1085_4'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Información IDOC generador documento FI'
define view ZISDR1085_IDOC_FI
  as select distinct from ztsdr1085_1
{
  key     max( numero_idoc )         as numero_idoc,
  key     max( numero_documento_fi ) as numero_documento_fi,
  key     pedido_cliente,
          sociedad,
          ejercicio
}
group by
  sociedad,
  ejercicio,
  pedido_cliente
